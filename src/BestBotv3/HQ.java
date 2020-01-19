package BestBotv3;

import battlecode.common.*;

/*
 * FIRST FEW ROUNDS STRATEGY
 * Build 4 miners, start refining soup in HQ
 * Build Amazon, drone. Drone is on standby (hover near HQ, wait for enemy units)
 * Build design school
 * Build refinery (if there isn’t already one)
 * build 2 landscapers
 */

public class HQ extends Shooter {
    static int numMiners = 0;
    public static final int MINER_LIMIT = 4;

    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.sendHqLoc(rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        System.out.println(rc.getLocation());

        super.takeTurn();
        System.out.println("turnCount: " + turnCount);
        if(RobotPlayer.turnCount == 1) {
            comms.sendHqLoc(rc.getLocation());
            MapLocation[] nearbySoupLocations = rc.senseNearbySoup(RobotType.HQ.sensorRadiusSquared);
            if (nearbySoupLocations.length > 0) {
                for (MapLocation nearbySoup : nearbySoupLocations) {
                    comms.broadcastSoupLocation(nearbySoup);
                }
            }
        }
        if(numMiners < MINER_LIMIT) {
            for (Direction dir : Util.directions)
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
            for (Direction dir: Util.directions){
                tryBuild(RobotType.MINER,Util.randomDirection());
            }
        }
    }
}