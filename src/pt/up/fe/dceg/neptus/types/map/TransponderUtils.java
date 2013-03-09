/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 26 de Jan de 2013
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
