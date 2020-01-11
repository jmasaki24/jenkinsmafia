package MafiaBot;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static MapLocation lastSeenSoup = new MapLocation(-5,-5);
    static int LastRoundMined = 0;
    static MapLocation lastSeenRefinery = new MapLocation(-5,-5);
    static MapLocation lastSeenAmazon = new MapLocation(-5,-5);
    static MapLocation lastSeenSchool = new MapLocation(-5,-5);
    static Direction myHeading = Direction.WEST;
    static MapLocation lastSeenWater = new MapLocation(-5, -5);
    static MapLocation ourHQ = new MapLocation(-5,-5);

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
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (rc.getRobotCount() <= 10  || rc.getRoundNum() < 35) {
            for (Direction dir : directions)
                tryBuild(RobotType.MINER, dir);
        }
    }

    static void runMiner() throws GameActionException {
        tryBlockchain();
        boolean moved = false;
        boolean inventoryFull = rc.getSoupCarrying() >= RobotType.MINER.soupLimit;
        boolean seesAmazon = false;
        boolean seesSchool = false;
        boolean seesHQ = false;
        boolean mined = false;

        //Refine Soup Everywhere
        for (Direction dir : directions)
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());

        //Mine Everywhere
        for (Direction dir : directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                moved = true;
                mined = true;
                LastRoundMined = rc.getRoundNum();
                lastSeenSoup = rc.getLocation().add(dir);
            }
        }



        //Vision and Memory
        RobotInfo[] robot = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robo : robot) {
            if (robo.team == rc.getTeam()) {
                //if we see a friendly refinery or hq
                 if(robo.type == RobotType.HQ){
                    seesHQ = true;
                    ourHQ = robo.location;
                }
                if (robo.type == RobotType.REFINERY || robo.type == RobotType.HQ) {
                    inventoryFull = false;
                    lastSeenRefinery = robo.location;
                    System.out.println("Found a refinery!");
                } else if(robo.type == RobotType.FULFILLMENT_CENTER){
                    seesAmazon = true;
                    lastSeenAmazon = robo.location;
                } else if(robo.type == RobotType.DESIGN_SCHOOL){
                    seesSchool = true;
                    lastSeenSchool = robo.location;
                }
            }
        }

        //build some soup storage
        if (inventoryFull) {
            if (lastSeenRefinery.distanceSquaredTo(rc.getLocation()) > 60) {
                //build one refinery in any direction you can if the last one is too far
                boolean built = false;
                for (Direction dir : directions) {
                    if (!built) {
                        if (tryBuild(RobotType.REFINERY, dir)) {
                            System.out.println("I need to build a refinery!");
                            built = true;
                        }
                    }
                }
                if (built) {
                    for (Direction dir : directions) {
                        if (rc.canDepositSoup(dir)) {
                            rc.depositSoup(dir, rc.getSoupCarrying());
                            System.out.println("Deposited soup into new refinery");
                        }
                    }
                }
            }
        }
        /*   //
        if (!seesAmazon && (lastSeenAmazon.x < 0 || lastSeenAmazon.y < 0)) {
            boolean built = false;
            for (Direction dir : directions) {
                if (!built) {
                    if (tryBuild(RobotType.FULLFILLMENTCENTER, randomDirection())) {
                        built = true;
                    }
                }
            }
        }

        // build schools

*/      System.out.println("Sees HQ" + seesHQ);
        System.out.println("Sees School" + seesSchool);
        System.out.println("Doesn't remember school" + (lastSeenSchool.x < 0 || lastSeenSchool.y < 0));
        if (!seesSchool && ((lastSeenSchool.x < 0 || lastSeenSchool.y < 0) && seesHQ)) {
            if(ourHQ.distanceSquaredTo(rc.getLocation()) > 8){
                moved = true;
                if(!tryMove(rc.getLocation().directionTo(ourHQ))){
                    tryMove(randomDirection());
                }
            } else{
                if (!tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(ourHQ))) {
                    moved = false;
                }
            }
        }

        //Where to move and when
        if (!moved) {
            if (inventoryFull) {
                //Is the refinery real?
                if (lastSeenRefinery.x >= 0 && lastSeenRefinery.y >= 0) {
                    System.out.println("Heading to a refinery!");
                    if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(lastSeenRefinery)))) {
                        if(!tryMove(rc.getLocation().directionTo(lastSeenRefinery))){
                            tryMove(randomDirection());
                        }
                        System.out.println("Going to a refinery!!" + lastSeenRefinery);
                    }
                } else{
                    //build one refinery in any direction you can if the last one is too far
                    boolean built = false;
                    for (Direction dir : directions) {
                        if (!built) {
                            if (tryBuild(RobotType.REFINERY, dir)) {
                                System.out.println("I need to build a refinery!");
                                built = true;
                            }
                        }
                    }
                    if (built) {
                        for (Direction dir : directions) {
                            if (rc.canDepositSoup(dir)) {
                                rc.depositSoup(dir, rc.getSoupCarrying());
                                System.out.println("Deposited soup into new refinery");
                            }
                        }
                    }
                }
            } else {
                if((lastSeenSoup.x >= 0 && lastSeenSoup.y >= 0)){
                    //if 10 rounds has passed since we mined soup
                    if (lastSeenSoup.distanceSquaredTo(rc.getLocation()) <= 4) {

                        System.out.println("I got lost somehow???" + lastSeenSoup);
                        lastSeenSoup = new MapLocation(-5,-5);
                    } else{
                        //Scour for soup
                        if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(lastSeenSoup)))) {
                            if(!tryMove(rc.getLocation().directionTo(lastSeenSoup))){
                                tryMove(randomDirection());
                            }
                            System.out.println("Going to old soup" + lastSeenSoup);
                        }
                    }
                } else{
                    Direction rand = randomDirection();
                    if (!rc.senseFlooding(rc.getLocation().add(rand))) {
                        tryMove(rand);
                        System.out.println("Scouting for more soup");
                    }
                }

            }
        }
    }

    static void runRefinery() throws GameActionException {
        System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction dir : directions) {
            tryBuild(RobotType.LANDSCAPER, dir);
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions) {
            tryBuild(RobotType.DELIVERY_DRONE, dir);
        }
    }

    static void runLandscaper() throws GameActionException {
        
        RobotInfo[] robot = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robo : robot) {
            if (robo.team == rc.getTeam()) {
                //if we see a friendly refinery or hq
                if (robo.type == RobotType.HQ) {
                    ourHQ = robo.location;
                }
            }
        }

        if (ourHQ.x >= 0 && ourHQ.y >= 0) {
            //if far move close
            if(ourHQ.distanceSquaredTo(rc.getLocation()) > 8){
                if(!tryMove(rc.getLocation().directionTo(ourHQ))){
                    tryMove(randomDirection());
                }
            } else if(ourHQ.distanceSquaredTo(rc.getLocation()) < 4){ //if too close move away
                if(!tryMove(rc.getLocation().directionTo(ourHQ).opposite())){
                    tryMove(randomDirection());
                }
            } else{

            }
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        boolean moved = false;
        if(rc.senseFlooding(rc.getLocation())){
            lastSeenWater = rc.getLocation();
        }
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robot = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            RobotInfo[] cows = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, Team.NEUTRAL);
            //Combines the two arrays into one
            RobotInfo[] robots = new RobotInfo[robot.length + cows.length];
            System.arraycopy(robot, 0, robots, 0, robot.length);
            System.arraycopy(cows, 0, robots, robot.length, cows.length);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
            tryMove(randomDirection());
        } else {
            if(rc.getLocation() == lastSeenWater){
                for (Direction dir: directions){
                    if (tryMove(dir)){
                        rc.dropUnit(Direction.CENTER.opposite());
                    }
                }
            } else{
                if(lastSeenWater.x < 0 || lastSeenWater.y < 0){
                    tryMove(randomDirection());
                } else{
                    if(!tryMove(rc.getLocation().directionTo(lastSeenWater))){
                        tryMove(randomDirection());
                    }
                }
            }
        }
    }

    static void runNetGun() throws GameActionException {
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared,rc.getTeam().opponent());
        for(RobotInfo target:targets){
            if(rc.canShootUnit(target.ID)){
                rc.shootUnit(target.ID);
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

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
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

    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 1234567890;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}