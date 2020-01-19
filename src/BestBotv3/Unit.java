package BestBotv3;
import battlecode.common.*;

import java.util.ArrayList;

public class Unit extends Robot {

    Navigation nav;

    MapLocation myLoc;
    MapLocation hqLoc;
    MapLocation EHqLoc = new MapLocation(-3,-3);
    ArrayList<MapLocation> soupLocations = new ArrayList<>();
    ArrayList<MapLocation> refineryLocations = new ArrayList<>();
    ArrayList<MapLocation> designSchoolLocations = new ArrayList<>();
    ArrayList<MapLocation> vaporatorLocations = new ArrayList<>();
    ArrayList<MapLocation> amazonLocations = new ArrayList<>();

    public Unit(RobotController r) {
        super(r);
        nav = new Navigation(rc);
        myLoc = rc.getLocation();
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        myLoc = rc.getLocation();
        findHQ();
    }

    public void findHQ() throws GameActionException {
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
                hqLoc = comms.getHqLocFromBlockchain();
            }
        }
    }

    void findEHQ() throws GameActionException {
        if (EHqLoc.x < 0 || EHqLoc.y < 0) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam().opponent()) {
                    EHqLoc = robot.location;
                    System.out.println("Sending Enemy Location");
                    comms.sendEHqLoc(EHqLoc);
                }
            }
            if(EHqLoc.x < 0 || EHqLoc.y < 0) {
                // if still null, search the blockchain
                comms.getEHqLocFromBlockchain();
            }
        }
    }

    public void updateUnitLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == comms.teamSecret && mess[1] == 4){
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

    public void getSoupLocations() throws GameActionException {
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == comms.teamSecret && mess[1] == 2){
                soupLocations.add(new MapLocation(mess[2], mess[3]));
            }
        }
    }
}