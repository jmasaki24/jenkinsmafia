package BestBotv3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

//Todo: set a hard limit of landscapers to make
//Todo: Create Broadcast Design School Creation

public class Amazon extends Building {
    private boolean shouldMakeBuilders;

    public Amazon(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // will only actually happen if we haven't already broadcasted the creation
        //comms.broadcastDesignSchoolCreation(rc.getLocation());


        if (rc.getTeamSoup()>=(6*RobotType.DELIVERY_DRONE.cost)){
            shouldMakeBuilders = true;
        }
        if (shouldMakeBuilders){
            for (Direction dir: Util.directions){
                tryBuild(RobotType.DELIVERY_DRONE,dir);
            }
        }
    }
}
