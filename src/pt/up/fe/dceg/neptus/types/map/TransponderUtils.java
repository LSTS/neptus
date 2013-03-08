/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 26 de Jan de 2013
 * $Id:: TransponderUtils.java 9777 2013-01-28 14:43:48Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.types.map;

import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.PropertiesLoader;

/**
 * @author pdias
 *
 */
public class TransponderUtils {

    /**
     * @param transp
     * @return
     */
    public static LblBeacon getTransponderAsLblBeaconMessage(TransponderElement transp) {
        LocationType tPos = transp.getCenterLocation();
        LocationType absLoc = tPos.getNewAbsoluteLatLonDepth();

        LblBeacon msgLBLBeaconSetup = new LblBeacon();
        msgLBLBeaconSetup.setBeacon(transp.getId());
        msgLBLBeaconSetup.setLat(absLoc.getLatitudeAsDoubleValueRads());
        msgLBLBeaconSetup.setLon(absLoc.getLongitudeAsDoubleValueRads());
        msgLBLBeaconSetup.setDepth(absLoc.getDepth());

        short queryChannel = 0;
        short replyChannel = 0;
        short transponderDelay = 0;
        // int id = 0;
        PropertiesLoader propCong = transp.getPropConf();
        if (propCong == null) {
            return null;
        }
        String prop1 = propCong.getProperty("interrogation channel");
        String prop2 = propCong.getProperty("reply channel");
        String prop3 = propCong.getProperty("transponder delay (msecs.)");
        try {
            queryChannel = (short) Double.parseDouble(prop1);
            replyChannel = (short) Double.parseDouble(prop2);
            transponderDelay = (short) Double.parseDouble(prop3);
        }
        catch (NumberFormatException e2) {
            return null;
        }

        msgLBLBeaconSetup.setQueryChannel(queryChannel);
        msgLBLBeaconSetup.setReplyChannel(replyChannel);
        msgLBLBeaconSetup.setTransponderDelay(transponderDelay);
        return msgLBLBeaconSetup;
    }
}
