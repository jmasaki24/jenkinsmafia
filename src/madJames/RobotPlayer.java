package madJames;

import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;

// BEHAVIOR:    1. makes a fcenter and drone asap.
//              2. flies drone around whole map, uploading certain locations to chain
//              3. make map of locations in each unit
//              4. tests pathfinding for specific locations
//              5.



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
    static int turnCount; // number of turns since creation

    static int numCows = 0; // simple blockchain id 1
    static int numDrones = 0; // simple blockchain id 2
    static int numLandscapers = 0; // simple blockchain id 6
    static int numMiners = 0; // simple blockchain id 7

    static int lastCheckedBlock = 0;
    static final int NOTHINGID = 404;

    static MapLocation myLoc;
    static MapLocation hqLoc;
    static ArrayList<MapLocation> soupLocations = new ArrayList<>();
    static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    static ArrayList<MapLocation> amazonLocations = new ArrayList<>();

    // used in blockchain transactions
    static final int teamSecret = 444444444;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        madJames.RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount++;
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
                    //case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    //case LANDSCAPER:         runLandscaper();        break;
                    //case DELIVERY_DRONE:     runDeliveryDrone();     break;
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

    static void runHQ() throws GameActionException {
        if (turnCount >= 1) {
            updateArmyInfo();
        }
        if (turnCount == 1) {
            sendHqLoc(rc.getLocation());
        }
        System.out.println("numMiners: " + numMiners);

        // don't build miners until we get a fullfillment center
        if (numMiners >= 3 && amazonLocations.size() == 0) {
            // do nothing
            System.out.println("do nothing");
        } else if (numMiners < 15) {
            for (Direction dir : directions) {
                tryBuild(RobotType.MINER, dir, numMiners);
            }
        }

        hqBroadcastNumMiners(numMiners);

    }

    static void hqBroadcastNumMiners(int numMiners) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 3;
        message[2] = 7;
        message[3] = numMiners;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("broadcast miner!");
        }
    }


    static void runMiner() throws GameActionException {
        updateArmyInfo();
        updateSoupLocations();
        checkIfSoupGone();
        System.out.println("numMiners: " + numMiners);

        myLoc = rc.getLocation();
        System.out.println("numMiners from miner" + numMiners);
        if (myLoc.distanceSquaredTo(hqLoc) >= 16 && numMiners >= 3 && amazonLocations.size() <=1) {
            tryBuild(RobotType.FULFILLMENT_CENTER, randomDirection());
        }

        // TODO: 1/12/2020 maybe have the first priority be: run away from flood?
        // Better to deposit soup instead of refining
        for (Direction dir : directions) {
            if (rc.canDepositSoup(dir)) {
                rc.depositSoup(dir, rc.getSoupCarrying());
                System.out.println("Deposited soup into new refinery");
            }
        }

        // then, try to mine soup in all directions
        for (Direction dir : directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (hqLoc.distanceSquaredTo(soupLoc) > 25) {
                    if (tryBuild(RobotType.REFINERY, randomDirection())) {
                        broadcastBuildingCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                    }
                }
                if (!soupLocations.contains(soupLoc)) {
                    broadcastSoupLocation(soupLoc);
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
            if (rc.canSenseLocation(targetSoupLoc)
                    && rc.senseSoup(targetSoupLoc) == 0) {
                soupLocations.remove(0);
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
            System.out.println("trying to build " + type);
            rc.buildRobot(type, dir);
            // adds to array list in broadcastBuildingCreation
            broadcastBuildingCreation(type, rc.getLocation().add(dir));
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
     * @overload
     */
    static boolean tryBuild(RobotType type, Direction dir, int numUnits) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            System.out.println("trying to build " + type);
            rc.buildRobot(type, dir);
            broadcastUnitCreation(type, numUnits);
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

    /**************************************************************************************************
     * SIMPLE BLOCKCHAIN
     * soup has id of 2, hq has id of HQID, unit has id of 3, building has id of 4
     *
     */

    public static void broadcastSoupLocation(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 2;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new soup!" + loc);
        }
    }

    public static void updateSoupLocations() throws GameActionException {
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && mess[1] == 2) {
                System.out.println("heard about a tasty new soup location");
                soupLocations.add(new MapLocation(mess[2], mess[3]));
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

    // only used by hq, design center, and fulfillment center... until we decide how we're going to manage cows
    //
    public static void broadcastUnitCreation(RobotType type, int numUnits) throws GameActionException {
        int typeNumber;
        switch (type) {
            case COW:
                typeNumber = 1;
                numCows++;
                break;
            case DELIVERY_DRONE:
                typeNumber = 2;
                numDrones++;
                break;
            case LANDSCAPER:
                typeNumber = 6;
                numLandscapers++;
                break;
            case MINER:
                typeNumber = 7;
                numMiners++;
                break;
            default:
                typeNumber = 0;
                break;
        }
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 3;
        message[2] = typeNumber;
        message[3] = numUnits;
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            System.out.println("new unit!");
        }
    }

    // only used by miners
    public static void broadcastBuildingCreation(RobotType type, MapLocation loc) throws GameActionException {
        int typeNumber;
        switch (type) {
            case DESIGN_SCHOOL:
                typeNumber = 3;
                designSchoolLocations.add(loc);
                break;
            case FULFILLMENT_CENTER:
                typeNumber = 4;
                amazonLocations.add(loc);
                break;
            case HQ:
                typeNumber = 5;
                break;
            case NET_GUN:
                typeNumber = 8;
                break;
            case REFINERY:
                typeNumber = 9;
                refineryLocations.add(loc);
                break;
            case VAPORATOR:
                typeNumber = 10;
                vaporatorLocations.add(loc);
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
            System.out.println("new building!" + loc);
        }
    }

    // checks if message[1] is 3 (units) or 4 (buildings)
    public static void updateArmyInfo() throws GameActionException {
        for (Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if (mess[0] == teamSecret && mess[1] == 4) {
                System.out.println("heard about a new building");
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

            if (mess[0] == teamSecret && mess[1] == 3) {
                System.out.println("heard about a new unit");
                switch (mess[2]) {
                    case 1:
                        /* something about a cow. moo my ass */
                        break;
                    case 2:
                        numDrones = mess[3];
                        break;
                    case 6:
                        numLandscapers = mess[3];
                        break;
                    case 7:
                        numMiners = mess[3];
                        break;
                    default:
                        break;

                }
            }
        }
    }
}