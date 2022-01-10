/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 *
 */
public class MedslikDataParser {

    // xppm = xppb / 1000
    // http://www.endmemo.com/convert/density.php
    
//gals of pollutant at level 5 between depths
//cu.m of pollutant at level 5 between depths
//    0.0  2000.0   metres below surface
//  3     : Hours after start of spill
//    48393.  gals of pollutant released so far
// 43.517  16.383  : Lat & Long of spill location
//  5.0    : Pixel size (m) for spill plotting
//100.0 ppm         0.0   : Concentration & density of active pollutant
//0.5 ppm         0.0   : Concentration & density of active pollutant
// 7657     : Number of data points
//   Lat      Lon      gals/sq km
//   Lat      Lon      cu.m/sq km
    
    
    private File file;
    private BufferedReader reader; 
    private ArrayList<BaseData> points = new ArrayList<>();
    
    private long numberOfDataLines = 0;
    
    private long millisPassedFromSpill = 0;
    private double latDegsSpillLocation = Double.NaN;
    private double lonDegsSpillLocation = Double.NaN;
    
    private double depthUpper = 0;
    private double depthLower = Double.MAX_VALUE;

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
    
    /**
     * @return the millisPassedFromSpill
     */
    public long getMillisPassedFromSpill() {
        return millisPassedFromSpill;
    }
    
    /**
     * @return the latDegsSpillLocation
     */
    public double getLatDegsSpillLocation() {
        return latDegsSpillLocation;
    }
    
    /**
     * @return the lonDegsSpillLocation
     */
    public double getLonDegsSpillLocation() {
        return lonDegsSpillLocation;
    }
    
    public boolean parse() {
        int counter = 0;
        long dataLines = 0;
        try {
            String line = reader.readLine(); 
            while (line != null) {
                if (counter == 2) {
                    processTimePassedLines(line);
                }
                else if (counter == 7) {
                    processNumberOfDataLines(line);
                }
                else if (counter == 4) {
                    processLatLonSpillLines(line);
                }
                else if (counter == 1) {
                    processDepthsLines(line);
                }
                else if (counter > 8) {
                    if (dataLines < numberOfDataLines) {
                        processData(line);
                        dataLines++;
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
        
        BaseData point = new BaseData(lat, lon, depthUpper, millisPassedFromSpill);
        point.setSourceSystem("medslik");
        point.setDepthLower(depthLower);
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

    private void processTimePassedLines(String line) throws Exception {
        String[] tokens = line.split(":");
        if (tokens.length == 0)
            throwUnexpectedException();
        
        double tk0 = Double.parseDouble(tokens[0].trim());
        String tklc = tokens[1].toLowerCase();
        if (tklc.contains("minute"))
            millisPassedFromSpill = (long) (tk0 * DateTimeUtil.MINUTE);
        else if (tklc.contains("second"))
            millisPassedFromSpill = (long) (tk0 * DateTimeUtil.SECOND);
        else if (tklc.contains("millisecond"))
            millisPassedFromSpill = (long) tk0;
        else if (tklc.contains("day"))
            millisPassedFromSpill = (long) (tk0 * DateTimeUtil.DAY);
        else
            millisPassedFromSpill = (long) (tk0 * DateTimeUtil.HOUR);
    }

    private void processLatLonSpillLines(String line) throws Exception {
        String[] tokens = line.split(":");
        if (tokens.length == 0)
            throwUnexpectedException();
        
        String tk0Str = tokens[0].trim();
        String[] tokensLatLon = tk0Str.split(" +");
        if (tokensLatLon.length < 2)
            return;
            
        double tkLat = Double.parseDouble(tokensLatLon[0].trim());
        double tkLon = Double.parseDouble(tokensLatLon[1].trim());
        latDegsSpillLocation = tkLat;
        lonDegsSpillLocation = tkLon;
    }

    private void processDepthsLines(String line) throws Exception {
        String[] tokens = line.trim().split(" +", 3);
        if (tokens.length < 3)
            return;
        
        String tk0Str = tokens[0].trim();
        String tk1Str = tokens[1].trim();
        String tk2Str = tokens[2].trim();
        
        if (!tk2Str.startsWith("metres"))
            return;
        
        double tkDepthUpper = Double.parseDouble(tk0Str);
        double tkDepthLower = Double.parseDouble(tk1Str);
        depthUpper = tkDepthUpper;
        depthLower = tkDepthLower;
    }
    
    public static void main(String[] args) throws FileNotFoundException {
        MedslikDataParser csv = new MedslikDataParser(new File("out0003.tot"));
        
        csv.parse();

        for (BaseData pt : csv.getPoints()) {
            System.out.println(">\t" + pt);
        }
    }
}
