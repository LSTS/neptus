/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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

/**
 * @author zp
 *
 */
public class PositionHistory {

    private static final String positions_url = "http://hub.lsts.pt/api/v1/csvTag";
    private static DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat fmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void downloadDailyPositions() throws Exception {
        
        NeptusLog.pub().info("Downloading daily positions");
        URL urlData = new URL(positions_url+"/"+fmt.format(new Date()));
        File tmp = new File("positions/"+fmt.format(new Date()));
        FileUtils.copyURLToFile(urlData, tmp);        
    }
    
    public static Collection<AssetPosition> dailyPositions() throws Exception {
        try {
            downloadDailyPositions();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        BufferedReader reader = new BufferedReader(new FileReader("positions/"+fmt.format(new Date())));
        
        String line;
        Vector<AssetPosition> positions = new Vector<>();
        while ((line = reader.readLine()) != null) {
            String parts[] = line.split(",");
            Date time = fmt2.parse(parts[0]);
            String id = parts[1].trim();
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
        reader.close();
        return positions;
    }
 
    public static void main(String args[]) throws Exception {
        System.out.println(PositionHistory.dailyPositions().size());
    }
    
}
