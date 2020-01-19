package BestBotv3;

import battlecode.common.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class Communications {
    RobotController rc;

    final int teamSecret = 444444444;

    final int HQID = 0;
    final int EHQID = 232455;
    final int DESIGNSCHOOL = 123;

    static final String[] messageType = {
        "HQ loc",
        "design school created",
        "soup location",
    };

    public Communications(RobotController r) {
        rc = r;
    }

    public void sendHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 0;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3))
            rc.submitTransaction(message, 3);
    }

    public void sendEHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = EHQID;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3))
            rc.submitTransaction(message, 3);
    }

    public MapLocation getHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == 0){
                    System.out.println("found the HQ!");
                    return new MapLocation(mess[2], mess[3]);
                }
            }
        }
        return null;
    }

    public MapLocation getEHqLocFromBlockchain() throws GameActionException {
        for (int i = 1; i < rc.getRoundNum(); i++){
            for(Transaction tx : rc.getBlock(i)) {
                int[] mess = tx.getMessage();
                if(mess[0] == teamSecret && mess[1] == EHQID){
                    return new MapLocation(mess[2], mess[3]);
                }
            }
        }
        return null;
    }

    public boolean broadcastedCreation = false;
    public void broadcastDesignSchoolCreation(MapLocation loc) throws GameActionException {
        if(broadcastedCreation) return; // don't re-broadcast

        int[] message = new int[7];
        message[0] = teamSecret;
        message[1] = 1;
        message[2] = loc.x; // x coord of HQ
        message[3] = loc.y; // y coord of HQ
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            broadcastedCreation = true;
        }
    }

    // check the latest block for unit creation messages
    public int getNewDesignSchoolCount() throws GameActionException {
        int count = 0;
        for(Transaction tx : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 1){
                System.out.println("heard about a cool new school");
                count += 1;
            }
        }
        return count;
    }

    public void broadcastSoupLocation(MapLocation loc ) throws GameActionException {
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

    // used by miners only, for now
    public void updateSoupLocations(ArrayList<MapLocation> soupLocations) throws GameActionException {
        // if its just been created, go through all of the blocks and transactions to find soup
        if (RobotPlayer.turnCount == 1) {
            for (int i = 1; i< rc.getRoundNum(); i++) {
                crawlBlockchainForSoupLocations(soupLocations, i);
            }
        } else {
            crawlBlockchainForSoupLocations(soupLocations, rc.getRoundNum() - 1);
        }

    }

    public void crawlBlockchainForSoupLocations(ArrayList<MapLocation> soupLocations, int roundNum) throws GameActionException {
        for(Transaction tx : rc.getBlock(roundNum)) {
            int[] mess = tx.getMessage();
            if(mess[0] == teamSecret && mess[1] == 2){
                // TODO: don't add duplicate locations
                System.out.println("heard about a tasty new soup location");
                soupLocations.add(new MapLocation(mess[2], mess[3]));
            }
        }
    }

    public void broadcastUnitCreation(RobotType type, MapLocation loc) throws GameActionException {
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
}
