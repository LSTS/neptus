/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * May 11, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
public class PositionHistory {

    private static final String positions_url = GeneralPreferences.ripplesUrl + "/api/v1/csvTag/";
    private static DateFormat fmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void downloadCsv(String day, boolean force) throws Exception {
        File tmp = new File("positions/"+day);
        if (!force && tmp.exists()) {
            NeptusLog.pub().info("Downloading positions for day "+day+" not necessary.");   
            return;
        }
        NeptusLog.pub().info("Downloading positions for day "+day+"...");
        URL urlData = new URL(positions_url+day);        
        FileUtils.copyURLToFile(urlData, tmp);  
    }
    
    public static void downloadData() throws Exception {
        Date d = new Date();
        SimpleDateFormat dayFormat = new SimpleDateFormat("YYYY-MM-dd");
        downloadCsv(dayFormat.format(d), true);
        for (int i = 0; i < 30; i++) {
            d.setTime(d.getTime() - 1000 * 24 * 3600);
            String day = dayFormat.format(d);
            if (i == 0)
                downloadCsv(day, true);
            else
                downloadCsv(day, false);
        }
    }
    
    public static Collection<AssetPosition> getHistory() throws Exception {
        
        try {
            downloadData();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        Vector<AssetPosition> positions = new Vector<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("YYYY-MM-dd");
        Date current = new Date();
        Date d = new Date();
        for (int i = 30; i >= 0; i--) {
            d.setTime(current.getTime() - i * (1000 * 24 * 3600));
            String day = dayFormat.format(d);
            downloadCsv(day, false);      
            BufferedReader reader = new BufferedReader(new FileReader("positions/"+day));
            String line;
            
            while ((line = reader.readLine()) != null) {
                String parts[] = line.split(",");
                if (parts.length < 4)
                    continue;
                try {
                    Date time = fmt2.parse(parts[0]);
                    String id = parts[1].trim().toLowerCase();
                    double lat_degs = Double.parseDouble(parts[2].trim());
                    double lon_degs = Double.parseDouble(parts[3].trim());
                    AssetPosition position = new AssetPosition(id, lat_degs, lon_degs);
                    position.setTimestamp(time.getTime());
                    if (id.toLowerCase().startsWith("spot"))
                        position.setType("Spot Tag");
                    else if (id.toLowerCase().startsWith("argos"))
                        position.setType("Argos Tag");
                    
                    positions.add(position);
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error parsing history line: "+line);
                }
            }
            reader.close();
        }
        return positions;
    }
 
    public static void main(String args[]) throws Exception {
        System.out.println(PositionHistory.getHistory().size());
    }
    
}
