/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 16, 2020
 */
package pt.lsts.neptus.plugins.bathym;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author zp
 *
 */
public class XyzSplitter {

    private XyzFolder out;
    private CoordTranslator translator;
    
    double lastX, lastY, sum = 0;
    int count = 0;
    
    public XyzSplitter(File srcFolder, File dstFolder, String srcCRS) {
        out = new XyzFolder(dstFolder);
        translator = new CoordTranslator(srcCRS, CoordTranslator.CRS_WGS84);
        process(srcFolder);
    }
    
    public XyzSplitter(File srcFolder, File dstFolder) {
        this(srcFolder, dstFolder, CoordTranslator.CRS_WGS84);
    }
    
    public void process(File f) {
        if (f.isDirectory())
            for (File sub : f.listFiles())
                process(sub);
        
        System.out.println("Processing "+f);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            
            String line = reader.readLine();
            
            while (line != null) {                
                String[] parts = line.split("\\s");                
                line = reader.readLine();
                double x = Double.valueOf(parts[0]);
                double y = Double.valueOf(parts[1]);
                double z = Double.valueOf(parts[2]);
                
                if (lastX == x && lastY == y) {
                    count++;
                    sum += z; 
                }
                else {
                    double[] ll = translator.translate(x, y);
                    out.addSample(ll[1], ll[0], sum/count);                    
                    lastX = x; lastY = y; count = 1; sum = z;                               
                }
            }
            reader.close();            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args) {
        new XyzSplitter(new File("/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Batimetria/IH"), new File("/home/zp/Desktop/Bathymetry/ih"));
    }
    
}
