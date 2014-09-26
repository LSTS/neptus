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
 * Author: pdias
 * Sep 26, 2014
 */
package pt.lsts.neptus.plugins.urready4os.rhodamine.importers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import pt.lsts.neptus.plugins.urready4os.rhodamine.BaseData;

/**
 * @author pdias
 *
 */
public class MedslikDataParser {

//gals of pollutant at level 5 between depths
//    0.0  2000.0   metres below surface
//  3     : Hours after start of spill
//    48393.  gals of pollutant released so far
// 43.517  16.383  : Lat & Long of spill location
//  5.0    : Pixel size (m) for spill plotting
//100.0 ppm         0.0   : Concentration & density of active pollutant
// 7657     : Number of data points
//   Lat      Lon      gals/sq km
    
    
    private File file;
    private BufferedReader reader; 
    private ArrayList<BaseData> points = new ArrayList<>();
    
    private long numberOfDataLines = 0;

    public MedslikDataParser(File file) throws FileNotFoundException {
        this.file = file;
        
        FileReader fileReader = new FileReader(this.file);
        reader = new BufferedReader(fileReader);
    }

    /**
     * @return the points
     */
    public ArrayList<BaseData> getPoints() {
        return points;
    }
    
    public boolean parse() {
        int counter = 0;
        long dataLines = 0;
        try {
            String line = reader.readLine(); 
            while (line != null) {
                //                    System.out.println(line);
                if (counter == 7) {
                    processNumberOfDataLines(line);
                }
                else if (counter > 8) {
                    if (dataLines < numberOfDataLines) {
                        processData(line);
                        dataLines--;
                    }
                    else {
                        return true;
                    }
                }

                line = reader.readLine();
                counter++;
            }
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void throwUnexpectedException() throws Exception {
        throw new Exception("Unexpected exception");
    }

    /**
     * @param line
     * @throws Exception 
     */
    private void processData(String line) throws Exception {
        String[] tokens = line.trim().split(" +");
        if (tokens.length < 3)
            return;

        double lat = Double.NaN;
        double lon = Double.NaN;
        double rhodamine = Double.NaN;

        int ct = -1;
        for (String tk : tokens) {
            ct++;
            if (ct == 0) {
                lat = Double.parseDouble(tk); 
            } 
            else if (ct == 1) {
                lon = Double.parseDouble(tk); 
            } 
            else if (ct == 2) {
                rhodamine = Double.parseDouble(tk);
            } 
        }
        
        if (Double.isNaN(lat) || Double.isNaN(lon))
            return;
        
        BaseData point = new BaseData(lat, lon, Double.NaN, 0);
        point.setRhodamineDyePPB(rhodamine);
        
        points.add(point);
    }

    /**
     * @param line
     * @throws Exception 
     */
    private void processNumberOfDataLines(String line) throws Exception {
        String[] tokens = line.split(":");
        if (tokens.length == 0)
            throwUnexpectedException();
        
        numberOfDataLines = Long.parseLong(tokens[0].trim());
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        MedslikDataParser csv = new MedslikDataParser(new File("out0003.tot"));
        
        csv.parse();

        for (BaseData pt : csv.getPoints()) {
            System.out.println(">\t" + pt);
        }
    }
}
