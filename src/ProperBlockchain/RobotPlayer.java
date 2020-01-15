package ProperBlockchain;

import battlecode.common.*;

import java.util.ArrayList;

// Somehow this file got broken and there are multiple sections that don't work. I have commented them out with "// //" so anyone who wants to fix them can easily find them

// focused on pathfinding at the moment

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

    // from lectureplayer, could be deleted
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    //Blockchain variables
    static int lastCheckedBlock = 0;

    //Location MESSAGE Identifiers
    static final int ourHQID = 0;
    static final int enemyHQID = 1;
    static final int SCHOOLID = 1234;
    static final int SOUPID = 4395;
    static final int LANDSCAPPERID = 5739;
    static final int AMAZONID = 5947;
    static final int VAPORATORID = 4938;
    static final int NOTHINGID = 404; //used to remove locations

    //All Location Identifiers in an array used for updating all locations
    static final int[] MessageIDs = new int[] {ourHQID,enemyHQID,SCHOOLID,SOUPID,LANDSCAPPERID,AMAZONID,VAPORATORID};

    //Example Command ID
    static final int BUILDWALLID = 1945; //when heard, landscapers start wall

    //Practical number storage
    static int turnCount; // number of turns since creation
    static int numMiners = 0;
    static int numLandscapers = 0;
    static int hqToCheck = 0;

    //Map
    static MapLocation myLoc;
    static MapLocation hqLoc;
    static MapLocation EHqLoc = new MapLocation(-3,-3);
    static ArrayList<MapLocation> soupLocations = new ArrayList<>();
    static ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    static ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    static ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    static ArrayList<MapLocation> amazonLocations = new ArrayList<>();

    // used in not good blockchain transactions
    static final int teamSecret = 495839;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        turnCount = 0;

        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            turnCount++;
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

    static void runDesignSchool() throws GameActionException {
        if (numLandscapers < 5){
            for (Direction dir: directions){
                tryBuild(RobotType.LANDSCAPER,dir);
                numLandscapers++;
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        myLoc = rc.getLocation();
    }

    static void definitelyDigDirt() throws GameActionException {
        definitelyDigDirt(0);
    }

    static void definitelyDigDirt(int count) throws GameActionException {
        Direction dir = randomAllDirection();
        int distance = myLoc.add(dir).distanceSquaredTo(hqLoc);
        if ((distance > 4 && (distance < 9) && rc.canDepositDirt(dir))) {
            if(rc.canDepositDirt(dir)){
                rc.depositDirt(dir);
            } else{
                if (count < 10)
                    definitelyDigDirt(count + 1);
            }
        } else {
            if (rc.canDigDirt(dir)) {
                rc.digDirt(dir);
            } else{
                if (count < 10)
                    definitelyDigDirt(count + 1);
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
            if(hqLoc == null) {
                // if still null, search the blockchain

                // ISSUE BELOW
                // getHqfromblockchain doesnt exist.

                // // getHqLocFromBlockchain();
            }
        }
    }

    static void findEHQ() throws GameActionException {
        if (EHqLoc.x < 0 || EHqLoc.y < 0) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam().opponent()) {
                    EHqLoc = robot.location;
                    System.out.println("Sending Enemy Location");
                    // // sendEHqLoc(EHqLoc);
                }
            }
            if(EHqLoc.x < 0 || EHqLoc.y < 0) {
                // if still null, search the blockchain
                getEHqLocFromBlockchain();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if(turnCount == 1) {
            // // sendHqLoc(rc.getLocation());
        }
        if(numMiners < 15) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners++;
                }
        }

        System.out.println();

        //Request a school next to base
        boolean seeDesignSchool = false;
        boolean seeAmazon = false;
        RobotInfo[] robots = rc.senseNearbyRobots(RobotType.HQ.sensorRadiusSquared,rc.getTeam());
        for (RobotInfo robot:robots){
            if(robot.type == RobotType.DESIGN_SCHOOL){
                seeDesignSchool = true;
            }if(robot.type == RobotType.FULFILLMENT_CENTER){
                seeAmazon = true;
            }
        }
        if(!seeDesignSchool){
            if(rc.getTeamSoup() > RobotType.DESIGN_SCHOOL.cost + RobotType.MINER.cost){
                tryBuild(RobotType.MINER,Direction.SOUTHWEST);
            }
        }
        if(seeDesignSchool && !seeAmazon){
            if(rc.getTeamSoup() > RobotType.FULFILLMENT_CENTER.cost + RobotType.MINER.cost){
                tryBuild(RobotType.MINER,Direction.SOUTHEAST);
            }
        }
        if (seeDesignSchool && rc.getRoundNum() > 300){
            for (Direction dir: directions){
                tryBuild(RobotType.MINER,randomDirection());
            }
        }

        //HQ shoots drones
        RobotInfo targets[] = rc.senseNearbyRobots(RobotType.NET_GUN.sensorRadiusSquared, rc.getTeam().opponent());
        for (RobotInfo target : targets) {
            if (rc.canShootUnit(target.ID)) {
                rc.shootUnit(target.ID);
            }
        }
    }



    static void runMiner() throws GameActionException {
        updateUnitLocations();
        updateSoupLocations();
        checkIfSoupGone();

        boolean shouldMove = true;
        myLoc = rc.getLocation();

        findEHQ();



        MapLocation[] potentialHQ = new MapLocation[] {new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (hqLoc.y) - 1), new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (rc.getMapHeight() - hqLoc.y) - 1)};
        System.out.println(potentialHQ[0]);
        System.out.println(potentialHQ[1]);


        if(rc.getRoundNum() > 130){
            if (rc.getID()%2 == 0){
                if(EHqLoc.x > 0 || EHqLoc.y > 0){
                    System.out.println("Found ENEMY HQ");
                    if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                        System.out.println("Going to ENEMY HQ:" + EHqLoc);
                        goTo(EHqLoc);
                    } else{
                        System.out.println("Standing my gound at ENEMY HQ");
                        for (Direction dir: directions){
                            tryBuild(RobotType.NET_GUN,dir);
                        }
                        shouldMove = false;
                    }
                }

                if(myLoc.distanceSquaredTo(potentialHQ[hqToCheck]) > 5){
                    System.out.println("Going to a potential HQ:" + potentialHQ);
                    if(shouldMove)
                        goTo(myLoc.directionTo(potentialHQ[hqToCheck]));
                        rc.setIndicatorLine(rc.getLocation(),potentialHQ[hqToCheck],0,230,0);
                } else{
                    System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                    hqToCheck = 1;
                }
            }
        }

        if(shouldMove){
            //Build 1 school when summoned into a specific position by HQ
            System.out.println(turnCount);
            if(turnCount <= 13){
                System.out.println(hqLoc.distanceSquaredTo(myLoc));
                if(myLoc.directionTo(hqLoc) == Direction.NORTHEAST && myLoc.distanceSquaredTo(hqLoc) == 2){
                    System.out.println("Trybuild school");
                    tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH);
                }
            }

            //Build 1 school when summoned into a specific position by HQ
            System.out.println(turnCount);
            if(turnCount <= 13){
                System.out.println(hqLoc.distanceSquaredTo(myLoc));
                if(myLoc.directionTo(hqLoc) == Direction.NORTHWEST && myLoc.distanceSquaredTo(hqLoc) == 2){
                    System.out.println("Trybuild Amazon");
                    tryBuild(RobotType.FULFILLMENT_CENTER,Direction.NORTH);
                }
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



    public static void broadcastSoupLocation(MapLocation loc ) throws GameActionException {
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
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 2){
                System.out.println("heard about a tasty new soup location");
                soupLocations.add(new MapLocation(mess[2], mess[3]));
            }
        }
    }

    public static void sendLoc(int id,MapLocation loc) throws GameActionException {
        sendMessage(id,loc);
    }
// // // // // // // This whole thing below doesnt work
/*    public static void getLocFromBlockchain(int id,MapLocation loc) throws GameActionException {
        if (id == ourHQID) {

        } else if (id == enemyHQID) {

        } else {
            MapLocation[] listToUpdate;
            switch (id) {
                case SCHOOLID:
                    listToUpdate = 3;
                    break;
                case AMAZONID:
                    listToUpdate = 4;
                    break;
                case LANDSCAPPERID:
                    listToUpdate = 5;
                    break;
                case NOTHINGID:
                    listToUpdate = 6;
                    break;
                case MINER:
                    listToUpdate = 7;
                    break;
                case NET_GUN:
                    listToUpdate = 8;
                    break;
                case REFINERY:
                    listToUpdate = 9;
                    break;
                case VAPORATOR:
                    listToUpdate = 10;
                    break;
                default:
                    listToUpdate = 0;
                    break;
            }
        }
    }*/

    public static void getEHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                // // if(mess[0] == teamSecret && mess[1] == EHQID){
                    EHqLoc = new MapLocation(mess[2], mess[3]);
                // // }
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
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
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

    //Cam's pretty lame blockchain stuff here until end of doc

    static ArrayList<MapLocation> queueBlockchain(int id,ArrayList<MapLocation> currArray) throws GameActionException {
        ArrayList<MapLocation> answer = currArray;
        int block = lastCheckedBlock + 1;
        for (int i = block; i < rc.getRoundNum(); i++){
            int[][] messages = getMessages(i);
            for (int e = 0; e < messages.length; e++){
                if (messages[0][e] == id){
                    answer.add(getMessageLocation(messages[1][e]));
                } else if (messages[0][e] == NOTHINGID){
                    while(answer.contains(getMessageLocation(NOTHINGID))){
                        answer.remove(getMessageLocation(NOTHINGID));
                    }
                }
            }
        }
        lastCheckedBlock = rc.getRoundNum() - 1;
        System.out.println(answer);
        return answer;
    }

    static void sendMessage(int a) throws GameActionException {
        sendMessage(a,rc.getLocation());
    }

    static void sendMessage(String a) throws GameActionException {
        sendMessage(stringToInt(a));
    }

    static void sendMessage(int a,MapLocation pos) throws GameActionException {
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
                int x = pos.x;
                int y = pos.y;
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


    static MapLocation getMessageLocation(int a){
        return new MapLocation (getMessage(a)/100,getMessage(a)%100);
    }

    //Use this to get a list of messages from a block
    static int[][] getMessages(int block) throws GameActionException {
        Transaction[] Block = rc.getBlock(block);
        int[][] ourMessages = new int[Block.length][2];

        int count = 0;
        for(Transaction submission: Block){
            if(isOurMessage(submission.getMessage())){
                ourMessages[count][0] = getMessage(submission.getMessage()[0]);
                ourMessages[count][0] = getMessage(submission.getMessage()[1]);
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
