package BestBotv3;
import battlecode.common.*;

import java.util.ArrayList;

public class Miner extends Unit {

    int numDesignSchools = 0;
    ArrayList<MapLocation> soupLocations = new ArrayList<MapLocation>();

    public Miner(RobotController r) {
        super(r);
    }

    /*
     * FIRST: mine updates the map, unitlocation txs, numminers txs, souplocation txs, and checkIfSoupGone
     * SECOND: check if need to build amazon. build amazon.
     * THIRD: deposit soup in all directions
     * FOURTH: mine soup in all directions
     * FIFTH: move. see below (should turn it into a separate func)
     */

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        //Update Stuff
        updateUnitLocations();
        comms.updateSoupLocations(soupLocations);
        checkIfSoupGone();

        // TODO: 1/19/2020 Fix how the miners build design schools so its foolproof
        // Build 1 school when summoned into a specific position by HQ and after move away
        System.out.println(turnCount);
        if(turnCount <= 11){
            System.out.println(hqLoc.distanceSquaredTo(myLoc));
            if(myLoc.directionTo(hqLoc) == Direction.NORTHEAST && myLoc.distanceSquaredTo(hqLoc) == 2){
                System.out.println("Trybuild school");
                tryBuild(RobotType.DESIGN_SCHOOL,Direction.NORTH);
                numDesignSchools = 1;
            }
        }

        // Better to deposit soup while you can
        for (Direction dir : Util.directions) {
            tryRefine(dir);
        }

        // then, try to mine soup in all directions
        for (Direction dir : Util.directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (refineryLocations.size() != 0){
                    if (refineryLocations.get(refineryLocations.size()-1).distanceSquaredTo(soupLoc) > 10) {
                        if (tryBuild(RobotType.REFINERY, Util.randomDirection())) {
                            comms.broadcastUnitCreation(RobotType.REFINERY, rc.adjacentLocation(dir.opposite()));
                        }
                    }
                }
                if(soupLocations.size() == 0) {
                    comms.broadcastSoupLocation(soupLoc);
                } else{
                    if (!soupLocations.contains(soupLoc)) {
                        comms.broadcastSoupLocation(soupLoc);
                    }
                }


            }

        //lastly, move

        // if at soup limit, go to nearest refinery or hq.
        // if hq or refinery is far away, build a refinery.
        // if there are less than MINER LIMIT miners, tell hq to pause building miners????

        //find closest refinery (including hq)
        MapLocation closestRefineryLoc = hqLoc;

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            System.out.println("I'm full of soup");
            //Find Closest Refinery
            if (refineryLocations.size() != 0) {
                for (MapLocation refinery : refineryLocations) {
                    if (myLoc.distanceSquaredTo(refinery) < myLoc.distanceSquaredTo(closestRefineryLoc)) {
                        closestRefineryLoc = refinery;
                    }
                }


                // If there is a design school or landscaper of same team, then the miner needs to go away and find another
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.MINER.sensorRadiusSquared,rc.getTeam());
                for (RobotInfo robot:robots){
                    if ((robot.type == RobotType.DESIGN_SCHOOL || robot.type == RobotType.LANDSCAPER) && robot.team == rc.getTeam()) {
                        System.out.println("I see the design school");
                        if (rc.getLocation().distanceSquaredTo(hqLoc)<=2){
                            System.out.println("I am going to try to move away now");
                            closestRefineryLoc = null;
                            System.out.println(refineryLocations);
                            rc.move(rc.getLocation().directionTo(hqLoc).opposite());
                        }
                    }
                }
            }

            // a refinery, they just sit there and wait for passive soup income.

            // how far away is enough to justify a new refinery?
            if (rc.getLocation().distanceSquaredTo(closestRefineryLoc) > 35) {
                if(!tryBuild(RobotType.REFINERY, Util.randomDirection())){ // if a new refinery can't be built go back to hq
                    System.out.println("moved towards HQ");
                    nav.goTo(closestRefineryLoc);
                    rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
                }
            } else {
                System.out.println("moved towards HQ");
                nav.goTo(closestRefineryLoc);
                rc.setIndicatorLine(rc.getLocation(), closestRefineryLoc, 255, 0, 255);
            }
        }

        else {
            if (soupLocations.size() > 0) {
                System.out.println("I'm moving to soupLocation[0]");
                nav.goTo(soupLocations.get(0));
            } else {
                System.out.println("I'm searching for soup, moving away from other miners");
                MapLocation nextPlace = rc.getLocation();
                for (RobotInfo robot:robots){
                    if (robot.type == RobotType.MINER){
                        nextPlace = nextPlace.add(rc.getLocation().directionTo(robot.location).opposite());
                    }
                }
                if(robots.length == 0){
                    nextPlace.add(Util.randomDirection());
                }
                System.out.println("Trying to go: " + rc.getLocation().directionTo(nextPlace));
                if(nextPlace != rc.getLocation()){
                    nav.goTo(rc.getLocation().directionTo(nextPlace));
                } else{
                    nav.goTo(Util.randomDirection());
                }
            }
        }
    }



    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    boolean tryMine(Direction dir) throws GameActionException {
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
    boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

    void checkIfSoupGone() throws GameActionException {
        if (soupLocations.size() > 0) {
            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(targetSoupLoc)
                    && rc.senseSoup(targetSoupLoc) == 0) {
                soupLocations.remove(0);
            }
        }
    }
}
