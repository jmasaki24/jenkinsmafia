package BestBotv3;

import battlecode.common.*;

public class Drone extends Unit{

    //Vars
    boolean shouldMove = true;
    int hqToCheck = 0;
    MapLocation[] potentialHQ;

    public Drone(RobotController r) {
        super(r);

        MapLocation[] potentialHQ = new MapLocation[] {new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (hqLoc.y) - 1),
                new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (rc.getMapHeight() - hqLoc.y) - 1),
                new MapLocation((hqLoc.x) - 1                   , (rc.getMapHeight() - hqLoc.y) - 1)};
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        shouldMove = true;

        findEHQ();

        MapLocation[] potentialHQ = new MapLocation[] {new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (hqLoc.y) - 1),
                new MapLocation((rc.getMapWidth() - hqLoc.x) - 1, (rc.getMapHeight() - hqLoc.y) - 1),
                new MapLocation((hqLoc.x) - 1                   , (rc.getMapHeight() - hqLoc.y) - 1)};
        for (MapLocation loc: potentialHQ){
            rc.setIndicatorDot(loc,0,200,200);
        }


        if (rc.getID()%2 == 0){
            if(EHqLoc.x > 0 || EHqLoc.y > 0){
                System.out.println("Found ENEMY HQ");
                if (myLoc.distanceSquaredTo(EHqLoc) > 5){
                    System.out.println("Going to ENEMY HQ:" + EHqLoc);
                    nav.tryMove(myLoc.directionTo(EHqLoc));
                } else{
                    System.out.println("Standing my gound at ENEMY HQ");
                    for (Direction dir: Util.directions){
                        tryBuild(RobotType.NET_GUN,dir);
                    }
                    shouldMove = false;
                }
            }

            if(myLoc.distanceSquaredTo(potentialHQ[hqToCheck]) > 5){
                System.out.println("Going to a potential HQ:" + potentialHQ);
                if(shouldMove)
                    nav.tryMove(myLoc.directionTo(potentialHQ[hqToCheck]));
                rc.setIndicatorLine(rc.getLocation(),potentialHQ[hqToCheck],0,230,0);
            } else{
                System.out.println("Nothing Here at potential HQ:" + potentialHQ);
                hqToCheck += 1;
            }
        }
    }

}
