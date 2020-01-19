package BestBotv3;

import battlecode.common.*;

public class HQ extends Shooter {
    static int numMiners = 0;

    public HQ(RobotController r) throws GameActionException {
        super(r);
        comms.sendHqLoc(rc.getLocation());
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        if(turnCount == 1) {
            comms.sendHqLoc(rc.getLocation());
        }

        //Every 3 turns repeat messages
        if(turnCount % 3 == 0){
            comms.jamEnemyComms();
        }

        if(numMiners < 6) {
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