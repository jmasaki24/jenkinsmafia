package zoerbjunk;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    // initialized to negative stuff because we don't know how to check against a null object (if that's a thing in java)
    static MapLocation lastSeenSoup = new MapLocation(-5, -5);
    static int LastRoundMined = 0;
    static MapLocation lastSeenRefinery = new MapLocation(-5, -5);
    static MapLocation lastSeenAmazon = new MapLocation(-5, -5);
    static MapLocation lastSeenSchool = new MapLocation(-5, -5);
    static Direction myHeading = Direction.WEST;
    static MapLocation lastSeenWater = new MapLocation(-5, -5);
    static MapLocation ourHQ = new MapLocation(-5, -5);
    static int AttacksSent = 0;
    static int a = 0;


    static Direction[] directions = {
            Direction.NORTH,
            Direction.SOUTH,

            Direction.NORTHEAST,
            Direction.SOUTHWEST,

            Direction.EAST,
            Direction.WEST,

            Direction.SOUTHEAST,
            Direction.NORTHWEST
    };
    static Direction[] alldirections = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };
    static Direction[] squareDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
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
        //testChain();
        PutItOnTheChain(1234);
        if(rc.getRoundNum() > 10){
            if(isOurMessage(10,1)){
                System.out.println("Can send messages!!!");
            }
        }
        if (rc.getRobotCount() <= 3 || rc.getRoundNum() < 35) {
            for (Direction dir : directions)
                tryBuild(RobotType.MINER, dir);
        }
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo target : targets) {
            if (rc.canShootUnit(target.ID)) {
                rc.shootUnit(target.ID);
            }
        }
    }


    static void runMiner() throws GameActionException {
        boolean gotSoup = rc.getSoupCarrying() > 0;
        boolean nearRef = lastSeenRefinery.distanceSquaredTo(rc.getLocation()) <= 8;
        boolean knowsSoup = lastSeenSoup.x >=0  && lastSeenSoup.y >=0;
        boolean safeToRef = !rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(lastSeenRefinery)));


        boolean moved = false;
        boolean mined = false;
        boolean inventoryFull = rc.getSoupCarrying() >= RobotType.MINER.soupLimit;
        boolean seesAmazon = false;
        boolean seesSchool = false;
        boolean seesHQ = false;

        //Refine Soup Everywhere
        for (Direction dir : directions) {
            if (rc.getSoupCarrying()>0){
                if (tryRefine(dir)) {
                    System.out.println("I refined soup! " + rc.getSoupCarrying() + " Soup left to refine");
                    lastSeenRefinery = rc.getLocation().add(dir);
                }
            }
        }

        //Mine Everywhere
        for (Direction dir : directions) {
            // If possible to mine, mine
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());

                moved = true;
                mined = true;
                LastRoundMined = rc.getRoundNum();
                lastSeenSoup = rc.getLocation().add(dir);
            }
            // If not possible to mine, but I'm next to last seen soup
            else if (rc.getLocation().distanceSquaredTo(lastSeenSoup)<=2){
                System.out.println("Theres no soup here! There is this much" + rc.senseSoup(rc.getLocation().add(dir)));
                lastSeenSoup = new MapLocation(-5, -5);
            }
        }



        //Vision and Memory
        RobotInfo[] robot = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared, rc.getTeam());
        for (RobotInfo robo : robot) {
            if (robo.team == rc.getTeam()) {
                //if we see a friendly refinery or hq
                if (robo.type == RobotType.HQ){
                    ourHQ = robo.location;
                    seesHQ = true;
                } if (robo.type == RobotType.REFINERY || robo.type == RobotType.HQ) {
                    lastSeenRefinery = robo.location;
                } else if(robo.type == RobotType.FULFILLMENT_CENTER){
                    seesAmazon = true;
                    lastSeenAmazon = robo.location;
                } else if (robo.type == RobotType.DESIGN_SCHOOL) {
                    seesSchool = true;
                   // lastSeenSchool = robo.location; - Not sure if this line is important or not yet
                }
            }
        }


        // build schools
        if (!seesSchool && ((lastSeenSchool.x < 0 || lastSeenSchool.y < 0) && seesHQ)) {
            if (ourHQ.distanceSquaredTo(rc.getLocation()) > 8) {
                moved = true;
                if (!tryMove(rc.getLocation().directionTo(ourHQ))) {
                    tryMove(randomDirection());
                }
            } else {
                if (!tryBuild(RobotType.DESIGN_SCHOOL, rc.getLocation().directionTo(ourHQ))) {
                    moved = false;
                }
            }
        }

/*
        if (!seesAmazon && (lastSeenAmazon.x < 0 || lastSeenAmazon.y < 0)) {
            boolean built = false;
            for (Direction dir : directions) {
                if (!built) {
                    if (tryBuild(RobotType.FULFILLMENT_CENTER, randomDirection())) {
                        built = true;
                    }
                }
            }
        }*/

        //Movement and Building
        // If has soup, go to refine it v
        if (gotSoup) {
            System.out.println("Have soup");
            // If near Refinery, check if safe v
            if (lastSeenRefinery.distanceSquaredTo(rc.getLocation()) <= 13) {
                System.out.println("Near refinery");
                // If its safe to go to refinery, try to go
                if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(lastSeenRefinery)))) {
                    System.out.println("Safe to go");
                    // Try to go directly, or circle around
                    if(!tryMove(rc.getLocation().directionTo(lastSeenRefinery))){
                        tryMove(rc.getLocation().directionTo(lastSeenRefinery).rotateRight());
                    }
                    System.out.println("Going!" + lastSeenRefinery);
                }
                // If not safe to go, try to go left, right, or opposite
                if(!tryMove(rc.getLocation().directionTo(lastSeenRefinery).rotateLeft())){
                    System.out.println("Not safe to go");

                    if (!tryMove(rc.getLocation().directionTo(lastSeenRefinery).rotateRight())){
                        tryMove(randomDirection());
                    }
                    System.out.println("Relocating");
                }
            }
            // If not near refinery, build one v
            else {
                System.out.println("Not near a refinery");
                for (Direction dir : directions) {
                    if (rc.getLocation().add(dir).distanceSquaredTo(ourHQ)>8){
                        if (tryBuild(RobotType.REFINERY, dir)) {
                            System.out.println("I built a refinery!");
                        }
                    }
                }
            }
        }
        // If has no soup, try to get it v
        else {
            System.out.println("Have no soup");
            // If knows where soup is/was, try to go to it
            if (knowsSoup){
                System.out.println("Know where soup is");
                // If its safe to go to soup
                if (!rc.senseFlooding(rc.getLocation().add(rc.getLocation().directionTo(lastSeenSoup)))) {
                    System.out.println("Going to soup");
                    if (!tryMove(rc.getLocation().directionTo(lastSeenSoup))){
                        Direction rand = randomDirection();
                        if (!rc.senseFlooding(rc.getLocation().add(rand))) {
                            tryMove(rand);
                            System.out.println("But taking random detour to get there");
                        }
                    }
                }
            }
            // If doesn't know where soup is, go randomly
            else {
                System.out.println("Dont know where soup is");
                // If its safe to go randomly
                Direction rand = randomDirection();
                if (!rc.senseFlooding(rc.getLocation().add(rand))){
                    tryMove(rand);
                    System.out.println("Going randomly to find soup");
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
        if (rc.getRoundNum() > 300) {
            for (Direction dir : directions) {
                tryBuild(RobotType.LANDSCAPER, dir);
            }
        }

    }

    static void runFulfillmentCenter() throws GameActionException {
        if (a < 10) {
            for (Direction dir : directions) {
                tryBuild(RobotType.DELIVERY_DRONE, dir);
            }
            a++;
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
            //we are in position, prepare to deliver load
            if (((ourHQ.distanceSquaredTo(rc.getLocation()) == 8) || (ourHQ.distanceSquaredTo(rc.getLocation()) == 4) || rc.senseElevation(rc.getLocation())>=7 || rc.senseElevation(rc.getLocation())<=0) || rc.getRoundNum()>500) {
                MapLocation ourPos = rc.getLocation();
                Direction dir = randomallDirection();
                int distance = (ourPos.add(dir).distanceSquaredTo(ourHQ));
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LANDSCAPER.sensorRadiusSquared, rc.getTeam());
                int count = 0;
                for (RobotInfo robo : robots) {
                    if (robo.type == RobotType.LANDSCAPER) {
                        count++;
                    }
                }
                // If the ring is not full of landscapers AKA not complete
                if (count >= 8) {
                    System.out.println("I'm building");
                    if ((distance > 1) && (distance < 9) && rc.canDepositDirt(dir)) {
                        rc.depositDirt(dir);
                    } else {
                        if (rc.canDigDirt(dir)) {
                            rc.digDirt(dir);
                        }
                    }
                }
            }
            if (ourHQ.distanceSquaredTo(rc.getLocation()) > 8) {
                if (!tryMove(rc.getLocation().directionTo(ourHQ))) {
                    if (!tryMove(rc.getLocation().directionTo(ourHQ).rotateLeft())) {
                        tryMove(randomDirection());
                    }
                }
            } else if (ourHQ.distanceSquaredTo(rc.getLocation()) == 5){
                if (!tryMove(rc.getLocation().directionTo(ourHQ))) {
                    if (!tryMove(rc.getLocation().directionTo(ourHQ).rotateLeft())) {
                        tryMove(rc.getLocation().directionTo(ourHQ).rotateRight());
                    }
                }
            }
            else if (((ourHQ.distanceSquaredTo(rc.getLocation()) != 4) && (ourHQ.distanceSquaredTo(rc.getLocation()) != 8))) { //if in the wrong spot relocate
                if (!tryMove(rc.getLocation().directionTo(ourHQ))){
                    if (!tryMove(rc.getLocation().directionTo(ourHQ).opposite())) {
                        tryMove(randomDirection());
                    }
                }

            }
        }
    }

        /*if ((rc.getDirtCarrying()>=RobotType.LANDSCAPER.dirtLimit)&&(AttacksSent<2)){
            tryBlockchain(new int[]{1, rc.getLocation().x, rc.getLocation().y});


        }*/

    static void runDeliveryDrone() throws GameActionException {
        boolean moved = false;
        System.out.println(rc.getBlock(1));
        System.out.println("test");
//        if(rc.getBlock(1)[1]==1){
//
//        }
        if (rc.senseFlooding(rc.getLocation())) {
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
            if (rc.getLocation() == lastSeenWater) {
                for (Direction dir : directions) {
                    if (tryMove(dir)) {
                        rc.dropUnit(Direction.CENTER.opposite());
                    }
                }
            } else {
                if (lastSeenWater.x < 0 || lastSeenWater.y < 0) {
                    tryMove(randomDirection());
                } else {
                    if (!tryMove(rc.getLocation().directionTo(lastSeenWater))) {
                        tryMove(randomDirection());
                    }
                }
            }
        }
    }

    static void runNetGun() throws GameActionException {
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo target : targets) {
            if (rc.canShootUnit(target.ID)) {
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

    static Direction randomallDirection() {
        return alldirections[(int) (Math.random() * alldirections.length)];
    }
    static Direction randomsqDirection() {
        return squareDirections[(int) (Math.random() * alldirections.length)];
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


    static void PutItOnTheChain(int a) throws GameActionException {
        if(rc.getRoundNum() > 3) {
            int[] message = new int[2];
            String messageF = "";
            for (int i = 0; i < message.length; i++) {
                if (i == 0) {
                    //000 when entered becomes 0, this corrects for that
                    if (a < 1000) {
                        messageF += "0";
                        if (a < 100) {
                            messageF += "0";
                            if (a < 10) {
                                messageF += "0";
                            }
                        }
                    }
                    messageF += Integer.toString(a); //a needs to be a 4 digit integer
                } else {
                    int x = rc.getLocation().x;
                    int y = rc.getLocation().y;
                    String tX = "";
                    String tY = "";

                    if (x < 10) {
                        tX += "0";
                    }
                    if (y < 10) {
                        tY += "0";
                    }
                    messageF = (tX + (x + "" + tY) + y);//location x added to location
                }
                //add round number
                //Protect against loss of zeroes
                a = rc.getRoundNum();
                if (a < 1000) {
                    messageF += "0";
                    if (a < 100) {
                        messageF += "0";
                        if (a < 10) {
                            messageF += "0";
                        }
                    }
                }
                messageF = messageF + (rc.getRoundNum() % 1000); // add last 4 digits of round number
                int sum = checkSum(messageF);
                String addZero = "";
                if (sum < 10) {
                    addZero = "0";
                }
                messageF = messageF + addZero + sum;
                message[i] = stringToInt(messageF);
                System.out.println(messageF);
            }
            if (rc.canSubmitTransaction(message, 1)) {
                rc.submitTransaction(message, 1);
            }
        }
    }

    static void testChain() throws GameActionException {
        int[] a = {999999999};
        if (rc.canSubmitTransaction(a, 1)) {
            rc.submitTransaction(a, 1);
        }
    }

    static void PutItOnTheChain(String a) throws GameActionException {
        PutItOnTheChain(stringToInt(a));
    }

    static int stringToInt(String a){
        if (a.length() > 7){ // if string is too long for Integer.parseInt() cut it in half and do it on a smaller piece of the string
            String aa = a.substring(0,a.length()/2);
            String ab = a.substring(a.length()/2);

            //recombine the smaller strings
            int aaa = (int) (stringToInt(aa) * Math.pow(10,ab.length()));
            int aba = stringToInt(ab);
            return (aaa + aba);
        } else{
            return Integer.parseInt(a);
        }
    }


    //Checks the checksum of each message
    static boolean isOurMessage(int[] x){

        boolean[] messageValid = new boolean[x.length];
        int count = 0;
        for(int a: x) {
            int sum = a%100;
            if(checkSum(a/100) == sum){
                messageValid[count] = true;
            } else{
                messageValid[count] = false;
            }
            count++;
        }
        //check if all messages are valid
        for(boolean a: messageValid){
            if (!a){
                return false;
            }
        }
        return true;
    }

    static boolean isOurMessage(int block,int message) throws GameActionException {
        Transaction[] thisBlock = rc.getBlock(block);
        int[] x = thisBlock[message - 1].getMessage();
        return isOurMessage(x);
    }

    //gets initial message from encrypted message
    static int getMessage(int a){
        return a/1000000;
    }

    //Use this to get a list of messages from a block
    static int[] getMessages(int block) throws GameActionException {
        Transaction[] Block = rc.getBlock(block);
        int[] ourMessages = new int[Block.length];

        int count = 0;
        for(Transaction submission: Block){
            if(isOurMessage(submission.getMessage())){
                ourMessages[count] = getMessage(submission.getMessage()[0]);
                count++;
            }
        }
        return ourMessages;
    }

    //Gets the coordinates of a message we sent to the block chain
    static int[] getMessageCoordinates(int block) throws GameActionException {
        Transaction[] Block = rc.getBlock(block);
        int[] ourMessages = new int[Block.length];

        int count = 0;
        for(Transaction submission: Block){
            if(isOurMessage(submission.getMessage())){
                ourMessages[count] = getMessage(submission.getMessage()[1]);
                count++;
            }
        }
        return ourMessages;
    }

    static int checkSum(int a){
        int sum = 0;
        int temp = a;
        //Add up all the digits
        while (temp > 9) {
            sum += temp % 10;
            temp = temp / 10;
        }
        sum += temp;
        return sum;
    }

    static int checkSum(String a){
        return checkSum(stringToInt(a));
    }
}
