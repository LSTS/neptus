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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * 6 de Jul de 2013
 */
package pt.up.fe.dceg.neptus.plugins.envdisp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * @author pdias
 *
 */
public class LoaderHelper {

    public static final HashMap<String, HFRadarDataPoint> processTUGHFRadar(Reader readerInput, Date dateLimite) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimite == null)
            ignoreDateLimitToLoad = true;
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(readerInput);
            String line = reader.readLine();
            Date dateStart = null;
            for (; line != null; ) {
                if (line.startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }
                else if (line.startsWith("%")) {
                    String tsStr = "%TimeStamp: ";
                    if (line.startsWith(tsStr)) {
                        dateStart = HFRadarVisualization.dateTimeFormaterSpacesUTC.parse(line, new ParsePosition(tsStr.length() -1 ));
                    }
                    
                    line = reader.readLine();
                    continue;
                }

                if (dateStart == null) {
                    NeptusLog.pub().warn("No start date found on TUV CODAR file.");
                    return hfdp;
                }

                if (!ignoreDateLimitToLoad && dateStart.before(dateLimite)) {
                    return hfdp;
                }


                String[] tokens = line.trim().split("[\\t ,]+");;
                if (tokens.length < 4) {
                    line = reader.readLine();
                    continue;
                }
                int latIdx = 1;
                int lonIdx = 0;
                int uIdx = 2;
                int vIdx = 3;
                try {
                    double lat = Double.parseDouble(tokens[latIdx]);
                    double lon = Double.parseDouble(tokens[lonIdx]);
                    double u = Double.parseDouble(tokens[uIdx]);
                    double v = Double.parseDouble(tokens[vIdx]);
                    double speed = Math.sqrt(u * u + v * v);
                    double heading = Math.atan2(v, u);
                    HFRadarDataPoint dp = new HFRadarDataPoint(lat, lon);
                    dp.setSpeedCmS(speed);
                    dp.setHeadingDegrees(Math.toDegrees(heading));
                    dp.setDateUTC(dateStart);

                    HFRadarDataPoint dpo = hfdp.get(HFRadarDataPoint.getId(dp));
                    if (dpo == null) {
                        dpo = dp.getACopyWithoutHistory();
                        hfdp.put(HFRadarDataPoint.getId(dp), dp);
                    }

                    ArrayList<HFRadarDataPoint> lst = dpo.getHistoricalData();
                    boolean alreadyIn = false;
                    for (HFRadarDataPoint tmpDp : lst) {
                        if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                            alreadyIn = true;
                            break;
                        }
                    }
                    if (!alreadyIn) {
                        dpo.getHistoricalData().add(dp);
                    }
                    
//                    System.out.println(dp);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                line = reader.readLine();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return hfdp;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String str = "    -7.3280918  36.4298738  -24.540   15.514          0      15.440       2.180     -25.700     10.5000    -48.0000   49.1357   167.7     29.033     302.3      1   1";
        String[] tokens = str.trim().split("[\\t ,]+");
        System.out.println(tokens.length);
    }

}
