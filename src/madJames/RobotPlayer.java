package madJames;

import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

// BEHAVIOR:    1. miner scans surrounding

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    // from lectureplayer, could be deleted
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};


    static final int HQID = 0;
    static final int DESIGNSCHOOL = 123;
    static int turnCount = 0; // number of turns since creation
    static int numMiners = 0; // used by HQ only, at the moment
    static int numAmazons = 0; // used by HQ only, at the moment
    static int numDrones = 0; // used by amazons only, at the moment
    static int numDesignSchools = 0;
    static int numLandscapers = 0;
    static int lastCheckedBlock = 0;

    // as an int, takes up 0.015 mb. if it were a long, it would take up a max of 64*64*8 = 32768 bytes, 0.03 mb.
    // from left to right, first 32 bits are elevation (cuz its an int anyway), then 1 bit for flooded, then 1 bit for accessible. still have 30 empty bits
    static long[][] map;

    static MapLocation myLoc;
    static MapLocation hqLoc;
    static ArrayList<MapLocation> soupLocations = new ArrayList<>();
    static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    static ArrayList<MapLocation> amazonLocations = new ArrayList<>();

    static final int MINER_LIMIT = 5;

    // used in blockchain transactions
    static final int teamSecret = 555555555;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        madJames.RobotPlayer.rc = rc;
        map = new long[rc.getMapHeight()][rc.getMapWidth()]; // should be a square array, i.e. number of rows and columns are the same.


        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount++;

            myLoc = rc.getLocation();

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                findHQ();
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    //case REFINERY:           runRefinery();          break;
                    //case VAPORATOR:          runVaporator();         break;
                    //case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    //case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    //case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }

    }

    static void findHQ() throws GameActionException {
        if (hqLoc == null) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
            if (hqLoc == null) {
                // if still null, search the blockchain
                getHqLocFromBlockchain();
            }
        }
        //HQ now shoots drones
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo target : targets) {
            if (rc.canShootUnit(target.ID)) {
                rc.shootUnit(target.ID);
            }
        }

    }

    // use myLoc, not hqLoc
    static RobotInfo[] nearbyRobots;

    static void runHQ() throws GameActionException {
        if (turnCount == 1) {
            sendHqLoc(rc.getLocation());

            MapLocation[] nearbySoupLocations = rc.senseNearbySoup();
            if (nearbySoupLocations.length > 0) {
                for (MapLocation nearbySoup : nearbySoupLocations) {
                    broadcastSoupLocation(nearbySoup, true);
                }
            }
     }



        if (numMiners < MINER_LIMIT) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                    System.out.println("built miner");
                    numMiners++;
                    //nearbyRobots = rc.senseNearbyRobots(2);
                    //for (RobotInfo r : nearbyRobots) {
                    //    if (r != null && r.location.equals(myLoc.add(dir))) {
                    //        System.out.println("FOUND MINDER" + r.getID());
                    //    }
                    //}
                }
            }
        }
        broadcastNumMiners();
    }

    static void updateMinerLocalMap() throws GameActionException {
        // building map. take all locations in sight, sense elevation and flooded, put info in map
        // miner can sense 109 loc, including its own loc. 109 = 7+9+(11*7)+9+7
        int myX = myLoc.x;
        int myY = myLoc.y;
        // gonna traverse vision radius, starting at top left.
        // TODO: 1/19/2020 stop wasting 12 executions of the loop in the corners outside the vision radius
        int startCheckLocX = myLoc.x - 5;
        int startCheckLocY = myLoc.y + 5;
        int checkLocX;
        int checkLocY = startCheckLocY; // don't need to reset this one like with checkLocX, but maybe we will?
        MapLocation checkThisMapLoc;
        int elevation;
        long pieceOfMap = 0; // 0b 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000 L

        for (int row = 0; row < 11; row++) {
            checkLocX = startCheckLocX; // gotta reset and bring it back to the left side.

            for (int col = 0; col < 11; col++) {
                pieceOfMap = 0;

                // for now, let's check it once and then not check it again
                if (map[checkLocY][checkLocX] == 0) {
                    checkThisMapLoc = new MapLocation(checkLocX, checkLocY);

                    if (rc.canSenseLocation(checkThisMapLoc)) {
                        // flip first bit. as base ten, looks like -9223372036954775808
                        pieceOfMap |= 0b1000000000000000000000000000000000000000000000000000000000000000L;
                        if (rc.senseFlooding(checkThisMapLoc)) {    // 31st (from left) bit gets flipped on.
                            pieceOfMap |= 0b0000000000000000000000000000000100000000000000000000000000000000L;
                        }

                        elevation = rc.senseElevation(checkThisMapLoc);
                        System.out.println("elevation: " + elevation + " on [" + checkLocX + ", " + checkLocY);

                        //  so we need 32 zeroes to precede the elevation bit-string, and this makes it work
                        pieceOfMap |= (0x00000000ffffffffL & elevation);
                        // System.out.println("[" + checkLocX + ", " + checkLocY + "] is 0b" + Long.toBinaryString(pieceOfMap));
                    }
                    map[checkLocY][checkLocX] = pieceOfMap;
                }
                checkLocX ++; // IMPORTANT
            }
            checkLocY--; // IMPORTANT
        }
    }

    /*
     * FIRST: mine updates the map, unitlocation txs, numminers txs, souplocation txs, and checkIfSoupGone
     * SECOND: check if need to build amazon. build amazon.
     * THIRD: deposit soup in all directions
     * FOURTH: mine soup in all directions
     * FIFTH: move. see below (should turn it into a separate func)
     */
    static void runMiner() throws GameActionException {
        updateMinerLocalMap();
        updateUnitLocations();
        updateNumMiners();
        updateSoupLocations();
        checkIfSoupGone();


        boolean amIAmazonBuilder = false;
        //first miner gets to build the Amazon
        if (numMiners == 1) {
            amIAmazonBuilder = true;
        }

        if (numMiners >= MINER_LIMIT && amIAmazonBuilder) {
            for (Direction dir : directions) {
                tryBuild(RobotType.FULFILLMENT_CENTER, dir);
            }
        }

        // TODO: 1/12/2020 maybe have the first priority be: run away from flood?

        // TODO: 1/19/2020 take up less bytecode with math. canDepositSoup is 10 bytecode 
        for (Direction dir : directions) {
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                System.out.println("Deposited soup into new refinery");
            }
        }

        // then, try to mine soup in all directions
        // TODO: 1/19/2020 same thing, canMineSoup (in tryBuild) takes up 5 bytecode
        for (Direction dir : directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (hqLoc.distanceSquaredTo(soupLoc) > 25) {
                    if (tryBuild(RobotType.REFINERY, randomDirection())) {
                        broadcastUnitCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                    }
                }
                if (!soupLocations.contains(soupLoc)) {
                    broadcastSoupLocation(soupLoc, true);
                }
            }

        //lastly, move

        // if at soup limit, go to nearest refinery or hq.
        // if hq or refinery is far away, build a refinery.
        // if there are less than MINERLIMIT miners, tell hq to pause building miners????

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup");


            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            MapLocation closestRefineryLoc = hqLoc;

            // will we ever have so many refineries that this is ineffective and we should rather sort the ArrayList
            // by distance/accessibility all the time? idfk.
            if (refineryLocations.size() != 0) {
                for (MapLocation refinery : refineryLocations) {
                    if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                        closestRefineryLoc = refinery;
                    }
                }
            }

            // TODO: 1/12/2020 an edge case: when all of the miners are far away and there isn't enough soup to make
            // a refinery, they just sit there and wait for passive soup income.

            // how far away is enough to justify a new refinery?
            if (rc.getLocation().distanceSquaredTo(closestRefineryLoc) > 35) {
                if (!tryBuild(RobotType.REFINERY, randomDirection())) { // if a new refinery can't be built go back to hq
                    System.out.println("moved towards HQ");
                    goTo(closestRefineryLoc);
                    rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
                }
            } else {
                System.out.println("moved towards HQ");
                goTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);

            }
        } else {
            if (soupLocations.size() > 0) {
                System.out.println("I'm moving to soupLocation[0]");
                goTo(soupLocations.get(0));
            } else {
                System.out.println("I'm searching for soup, moving away from other miners");
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
                MapLocation nextPlace = rc.getLocation();
                for (RobotInfo robot : robots) {
                    if (robot.type == RobotType.MINER) {
                        nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                    }
                }
                if (robots.length == 0) {
                    nextPlace.add(randomDirection());
                }
                System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
                if (nextPlace != rc.getLocation()) {
                    goTo(rc.getLocation().directionTo(nextPlace));
                } else {
                    goTo(randomDirection());
                }
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        if (numDrones < 1) {
            for (Direction dir : directions) {
                if (tryBuild(RobotType.DELIVERY_DRONE, dir)) {
                    numDrones++;
                }
            }
        }
    }

    static MapLocation somePlace;

    static void runDeliveryDrone() throws GameActionException {
        if (somePlace == null) {
            System.out.println("finding somePlace");

            boolean onLeftSide = hqLoc.x < (rc.getMapWidth() / 2);
            boolean onLowerSide = hqLoc.y < (rc.getMapHeight() / 2);

            if (onLeftSide) {
                somePlace = new MapLocation(rc.getMapWidth() - hqLoc.x, hqLoc.y);
            } else {
                somePlace = new MapLocation(hqLoc.x - rc.getMapWidth(), hqLoc.y);
            }
        }
        if (!tryMove(myLoc.directionTo(hqLoc).opposite())) {
            System.out.println("cant move");
        }
    }

    // tries to move in the general direction of dir
    // THIS IS WHERE I WORK ON PATHFINDING - jm
    static boolean goTo(Direction dir) throws GameActionException {

        // while, I cannot move, rotate the direction to the right.
        for (int i = 0; i < 8; i++) {
            if (!tryMove(dir)) {
                dir = dir.rotateRight();
            } else {
                return true;
            }
        }
        return false;

//        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
//        for (Direction d : toTry){
//            if(tryMove(d))
//                return true;
//        }
//        return false;
    }

    // navigate towards a particular location
    static boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }

    static void checkIfSoupGone() throws GameActionException {
        if (soupLocations.size() > 0) {
            MapLocation targetSoupLoc = soupLocations.get(0);
            // TODO: 1/19/2020 we could make this less bytecode by not running canSenseLocation and doing math instead
            // unless, of course, the math takes up more bytecode (math = if (-5 <= targetSoupLoc.x - myLoc.x <= 5) and same for y
            if (rc.canSenseLocation(targetSoupLoc) && rc.senseSoup(targetSoupLoc) == 0) {
                soupLocations.remove(0);
                broadcastSoupLocation(targetSoupLoc, false);
            }
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if (rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            rc.setIndicatorDot(rc.getLocation().add(dir), 255, 0, 0);
            return true;
        }
        return false;
    }

    /**
     * Attempts to move in every direction
     */
    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir  The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    public static void broadcastSoupLocation(MapLocation loc, boolean present) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 2;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        message[4] = present ? 1 : 0;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new soup!" + loc);
        }
    }

    // used by miners only, for now
    public static void updateSoupLocations() throws GameActionException {
        // if its just been created, go through all of the blocks and transactions to find soup
        if (turnCount == 1) {
            for (int i = 1; i < rc.getRoundNum(); i++) {
                crawlBlockForSoupLocations(i);
            }
        }
        crawlBlockForSoupLocations(rc.getRoundNum() - 1);
    }

    public static void crawlBlockForSoupLocations(int roundNum) throws GameActionException {
        for (Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && mess[1] == 2 && mess[4] == 1) {
                System.out.println("new soup [" + mess[2] + ", " + mess[3] + "]");
                soupLocations.add(new MapLocation(mess[2], mess[3]));
            } else if (mess[0] == teamSecret && mess[1] == 2 && mess[4] == 0) {
                System.out.println("removing soupLoc [" + mess[2] + ", " + mess[3] + "]");
                soupLocations.removeIf(currentSoupLoc -> currentSoupLoc.x == mess[2] && currentSoupLoc.y == mess[3]);
            }
        }
    }


    public static void sendHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = HQID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3))
            rc.submitTransaction(message, 3);
    }

    public static void getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++) {
            for (Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if (mess[0] == teamSecret && mess[1] == HQID) {
                    System.out.println("found the HQ!");
                    hqLoc = new MapLocation(mess[2], mess[3]);
                }
            }
        }
    }

    // to be only used by hq
    public static void broadcastNumMiners() throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 5;
        message[2] = numMiners;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("broadcast mines num: " + numMiners);
        }
    }

    public static void updateNumMiners() throws GameActionException {
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && mess[1] == 5) {
                numMiners = mess[2];
                System.out.println("update numMiners " + numMiners);
            }
        }
    }

    public static void broadcastUnitCreation(RobotType type, MapLocation loc) throws GameActionException {
        int typeNumber;
        switch (type) {
            case COW:
                typeNumber = 1;
                break;
            case DELIVERY_DRONE:
                typeNumber = 2;
                break;
            case DESIGN_SCHOOL:
                typeNumber = 3;
                break;
            case FULFILLMENT_CENTER:
                typeNumber = 4;
                break;
            case HQ:
                typeNumber = 5;
                break;
            case LANDSCAPER:
                typeNumber = 6;
                break;
            case MINER:
                typeNumber = 7;
                break;
            case NET_GUN:
                typeNumber = 8;
                break;
            case REFINERY:
                typeNumber = 9;
                break;
            case VAPORATOR:
                typeNumber = 10;
                break;
            default:
                typeNumber = 0;
                break;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 4;
        message[2] = loc.x; // x coord of unit
        message[3] = loc.y; // y coord of unit
        message[4] = typeNumber;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new refinery!" + loc);
        }
    }


    public static void updateUnitLocations() throws GameActionException {
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && mess[1] == 4) {
                System.out.println("heard about a new unit");
                switch (mess[4]) {
                    case 3:
                        designSchoolLocations.add(new MapLocation(mess[2], mess[3]));
                        break;
                    case 4:
                        amazonLocations.add(new MapLocation(mess[2], mess[3]));
                        break;
                    case 9:
                        refineryLocations.add(new MapLocation(mess[2], mess[3]));
                        break;
                    case 10:
                        vaporatorLocations.add(new MapLocation(mess[2], mess[3]));
                        break;
                    default:
                        break;
                }

            }
        }
    }
}
