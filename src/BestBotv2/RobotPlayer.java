package BestBotv2;
import battlecode.common.*;
import java.lang.Math;
import java.util.ArrayList;

//Long term goals
//TODO: The goal of this bot is to be as simple and clean as possible to enable future versions to be even better
//Todo: Comment any uncommented piece of code to ensure we know what each section generally does

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

    static Direction[] allDirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    //Blockchain Stuff
    static final int HQID = 0;
    static final int DESIGNSCHOOL = 123;
    static int turnCount; // number of turns since creation
    static int numMiners = 0;
    static int numDesignSchools = 0;
    static int numLandscapers = 0;
    static int lastCheckedBlock = 0;
    static boolean shouldMakeBuilders = false;
    static final int NOTHINGID = 404;
    static final MapLocation closestRefineryLoc = null;

    //Map Locations
    static MapLocation myLoc;
    static MapLocation hqLoc;
    static ArrayList<MapLocation> soupLocations = new ArrayList<>();
    static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    static ArrayList<MapLocation> amazonLocations = new ArrayList<>();

    // used in blockchain transactions
    static final int teamSecret = 232232332;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        BestBotv2.RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            turnCount++;
            myLoc = rc.getLocation();
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                findHQ();
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    //case REFINERY:           runRefinery();          break;
                    //case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    //case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    //Todo: Get drones to swarm enemy base
                    //Todo: Get drones to dive in all at once
                    //Todo: Get drones to drop enemy landscapers into water
                    //Todo: Bonus: Get our landscapers on their wall and bury their HQ
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

    //Todo: set a hard limit of landscapers to make
    //Creates 5 when we have the resources to
    static void runDesignSchool() throws GameActionException {
        if (rc.getTeamSoup()>=(5*RobotType.LANDSCAPER.cost)){
            shouldMakeBuilders = true;
        }
        if (shouldMakeBuilders){
            for (Direction dir: directions){
                tryBuild(RobotType.LANDSCAPER,dir);
            }
        }
    }

    //Currently builds up our wall evenly
    static void runLandscaper() throws GameActionException {
        if(rc.getDirtCarrying() == 0){
            tryDig();
        }

        //Wait 15 turns to build
        if (turnCount > 50) {
            for (int i = 0; i < 8; i++){
                MapLocation bestPlaceToBuildWall = null;
                // find best place to build
                if (hqLoc != null) {
                    int lowestElevation = 9999999;
                    for (Direction dir : directions) {
                        MapLocation tileToCheck = hqLoc.add(dir);
                        if (rc.getLocation().distanceSquaredTo(tileToCheck) < 4
                                && rc.canDepositDirt(rc.getLocation().directionTo(tileToCheck))) {
                            if (rc.senseElevation(tileToCheck) < lowestElevation) {
                                lowestElevation = rc.senseElevation(tileToCheck);
                                bestPlaceToBuildWall = tileToCheck;
                            }
                        }
                    }
                }

                if (Math.random() < 0.4) {
                    // build the wall
                    if (bestPlaceToBuildWall != null) {
                        rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
                        rc.setIndicatorDot(bestPlaceToBuildWall, 0, 255, 0);
                        System.out.println("building a wall");
                    }
                }
            }
        }

        // otherwise try to get to the hq
        if(hqLoc != null){
            System.out.println("Can See hq");

            //Runs from the school
            RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
            MapLocation nextPlace = rc.getLocation();
            for (RobotInfo robot:robots){
                if (robot.type == RobotType.DESIGN_SCHOOL){
                    nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                }
            }
            if(robots.length == 0){
                nextPlace = nextPlace.add(randomDirection());
            }
            if(nextPlace != rc.getLocation()) {
                if(myLoc.add(myLoc.directionTo(nextPlace)).distanceSquaredTo(hqLoc) < 3) { //Only move in directions where you end up on the wall
                    tryMove(rc.getLocation().directionTo(nextPlace));
                }
            }
            //Else move random (uses move limits to not go random every line)
            Direction rand = randomDirection();
            if (myLoc.add(rand).distanceSquaredTo(hqLoc) < 3){ //Only move in directions where you end up on the wall
                tryMove(rand);
            }
        } else {
            System.out.println("Can't see hq");
            tryMove(randomDirection());
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
            if(hqLoc == null) {
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
        if(turnCount == 1) {
            sendHqLoc(rc.getLocation());
        }
        if(numMiners < 6) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }

        //Request a school next to base
        boolean seeDesignSchool = false;
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared,rc.getTeam());
        for (RobotInfo robot:robots){
            if(robot.type == RobotType.DESIGN_SCHOOL){
                seeDesignSchool = true;
            }
        }
        if(!seeDesignSchool){
            if(rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.MINER.cost){
                tryBuild(RobotType.MINER,Direction.SOUTHWEST);
            }
        }
        if (seeDesignSchool && rc.getRoundNum() > 300){
            for (Direction dir: directions){
                tryBuild(RobotType.MINER,randomDirection());
            }
        }
    }

    //Todo: get miners to build one-two drone-centers to begin the swarm
    //Todo: Find the first made miner
    static void runMiner() throws GameActionException {
        //Update Stuff
        updateUnitLocations();
        updateSoupLocations();
        checkIfSoupGone();

        //Build 1 school when summoned into a specific position by HQ and after move away
        System.out.println(turnCount);
        if(turnCount <= 11){
            System.out.println(hqLoc.distanceSquaredTo(myLoc));
            if(myLoc.directionTo(hqLoc) == Direction.NORTHEAST && myLoc.distanceSquaredTo(hqLoc) == 2){
                System.out.println("Trybuild school");
                tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH);
            }
        }

        // Better to deposit soup while you can
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
                if (hqLoc.distanceSquaredTo(soupLoc) > 15) {
                    if (tryBuild(RobotType.REFINERY, randomDirection())) {
                        broadcastUnitCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                    }
                }
                if (!soupLocations.contains(soupLoc)) {
                    broadcastSoupLocation(soupLoc);
                }
            }

        //lastly, move

        // if at soup limit, go to nearest refinery or hq.
        // if hq or refinery is far away, build a refinery.
        // if there are less than MINER LIMIT miners, tell hq to pause building miners????
        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup");


            //find closest refinery (including hq, should change that tho since HQ will become unreachable)
            MapLocation closestRefineryLoc = hqLoc;

            //Find Closest Refinery
            if (refineryLocations.size() != 0) {
                for (MapLocation refinery : refineryLocations) {
                    if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                        closestRefineryLoc = refinery;
                    }
                }
            }

            // a refinery, they just sit there and wait for passive soup income.

            // how far away is enough to justify a new refinery?
            if (rc.getLocation().distanceSquaredTo(closestRefineryLoc) > 35) {
                if(!tryBuild(RobotType.REFINERY, randomDirection())){ // if a new refinery can't be built go back to hq
                    System.out.println("moved towards HQ");
                    goTo(closestRefineryLoc);
                    rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
                }
            } else {
                System.out.println("moved towards HQ");
                goTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
            }
        }

        else {
            if (soupLocations.size() > 0) {
                System.out.println("I'm moving to soupLocation[0]");
                goTo(soupLocations.get(0));
            } else {
                System.out.println("I'm searching for soup, moving away from other miners");
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
                MapLocation nextPlace = rc.getLocation();
                for (RobotInfo robot:robots){
                    if (robot.type == RobotType.MINER){
                        nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                    }
                }
                if(robots.length == 0){
                    nextPlace.add(randomDirection());
                }
                System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
                if(nextPlace != rc.getLocation()){
                    goTo(rc.getLocation().directionTo(nextPlace));
                } else{
                    goTo(randomDirection());
                }
            }
        }
    }

    // tries to move in the general direction of dir
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

    //Removes soup location from memory if there is no soup
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

    static Direction randomAllDirection() {
        return allDirections[(int) (Math.random() * allDirections.length)];
    }

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if(rc.canDigDirt(dir)){
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
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            broadcastUnitCreation(type, rc.getLocation().add(dir));
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

    //Jamie's Blockchain stuff
    //TODO: add round number to the team secret to prevent enemy spamming

    public static void broadcastSoupLocation(MapLocation loc ) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 2;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            System.out.println("new soup!" + loc);
        }
    }

    public static void updateSoupLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 2){
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
        if (rc.canSubmitTransaction(message, 1))
            rc.submitTransaction(message, 1);
    }

    public static void getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == HQID){
                    System.out.println("found the HQ!");
                    hqLoc = new MapLocation(mess[2], mess[3]);
                }
            }
        }
    }

    public static void broadcastUnitCreation(RobotType type, MapLocation loc) throws GameActionException {
        int typeNumber;
        switch (type) {
            case COW:                     typeNumber = 1;     break;
            case DELIVERY_DRONE:          typeNumber = 2;     break;
            case DESIGN_SCHOOL:           typeNumber = 3;     break;
            case FULFILLMENT_CENTER:      typeNumber = 4;     break;
            case HQ:                      typeNumber = 5;     break;
            case LANDSCAPER:              typeNumber = 6;     break;
            case MINER:                   typeNumber = 7;     break;
            case NET_GUN:                 typeNumber = 8;     break;
            case REFINERY:                typeNumber = 9;     break;
            case VAPORATOR:               typeNumber = 10;    break;
            default:                      typeNumber = 0;     break;
        }

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 4;
        message[2] = loc.x; // x coord of unit
        message[3] = loc.y; // y coord of unit
        message[4] = typeNumber;
        if (rc.canSubmitTransaction(message, 1)) {
            rc.submitTransaction(message, 1);
            System.out.println("new refinery!" + loc);
        }
    }

    public static void updateUnitLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 4){
                System.out.println("heard about a new unit");
                switch (mess[4]) {
                    case 3:     designSchoolLocations.add(new MapLocation(mess[2], mess[3]));   break;
                    case 4:     amazonLocations.add(new MapLocation(mess[2], mess[3]));         break;
                    case 9:     refineryLocations.add(new MapLocation(mess[2], mess[3]));       break;
                    case 10:    vaporatorLocations.add(new MapLocation(mess[2], mess[3]));      break;
                    default: break;
                }

            }
        }
    }
}
