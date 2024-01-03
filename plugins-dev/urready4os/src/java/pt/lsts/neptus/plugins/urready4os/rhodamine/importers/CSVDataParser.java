/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 24, 2014
 */
package pt.lsts.neptus.plugins.urready4os.rhodamine.importers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;

import pt.lsts.neptus.plugins.urready4os.rhodamine.BaseData;

/**
 * @author pdias
 *
 */
public class CSVDataParser {

//    % lauv-xplore-1, Cyclops 7, Rhodamine, 1 Hz
//    % 22/09/2014 13:11
//    % -1
//    % Time, Latitude, Longitude, Depth, Rhodamine,Rhodamine, Temperature
//    % seconds, degrees, degrees, meter, ppb, raw, Celsius
    
    private File file;
    private BufferedReader reader; 
    private ArrayList<BaseData> points = new ArrayList<>();
    private double invalidValue = -99999;
    
    private long millisMaxAge = -1;
    private long curTimeMillis = System.currentTimeMillis();
    
    private String system;
    
    // Indexes
    int numberOfElements = 0;
    int timeIdx = -1;
    int latIdx = -1;
    int lonIdx = -1;
    int depthIdx = -1;
    int rhodamineIdx = -1;
    int rhodamineRawIdx = -1;
    int crudeOilIdx = -1;
    int refineOilIdx = -1;
    int temperatureIdx = -1;
    
    public CSVDataParser(File file) throws FileNotFoundException {
        this.file = file;
        
        FileReader fileReader = new FileReader(this.file);
        reader = new BufferedReader(fileReader);
    }

    /**
     * @return the millisMaxAge
     */
    public long getMillisMaxAge() {
        return millisMaxAge;
    }
    
    /**
     * @param millisMaxAge the millisMaxAge to set
     */
    public void setMillisMaxAge(long millisMaxAge) {
        this.millisMaxAge = millisMaxAge;
    }
    
    /**
     * @return the system
     */
    public String getSystem() {
        return system;
    }
    
    /**
     * @return the points
     */
    public ArrayList<BaseData> getPoints() {
        return points;
    }
    
    public boolean parse() {
        curTimeMillis = System.currentTimeMillis();
        int counter = 0;
        try {
            String line = reader.readLine(); 
            while (line != null) {
                //                    System.out.println(line);
                int systemLineNumber = 0;
                int invalidValueLineNumber = 2;
                int colsLineNumber = 3;

                if (counter == systemLineNumber) {
                    processSystem(line);
                }
                else if (counter == invalidValueLineNumber) {
                    processNaNValue(line);
                } 
                else if (counter == colsLineNumber) {
                    processHeaderColOrder(line);
                }
                else {
                    // Process data
                    processData(line);
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

    /**
     * @param line
     * @throws Exception 
     */
    private void processSystem(String line) throws Exception {
        String ln = checkIfCommentLineRemoveMarkerOrThrowException(line);
        String[] tokens = ln.split(",");
        if (tokens.length == 0)
            throwUnexpectedException();
        
        String systemName = tokens[0].trim();
        if (systemName.isEmpty())
            throwUnexpectedException();
        
        system = systemName.toLowerCase();
    }

    /**
     * @param line
     * @throws Exception 
     */
    private void processNaNValue(String line) throws Exception {
        String ln = checkIfCommentLineRemoveMarkerOrThrowException(line);
        try {
            invalidValue = Double.parseDouble(ln);
        }
        catch (Exception e) {
            //% Not valid value (-1)
            ln = ln.replaceAll("[a-zA-Z \\(\\),]", "").trim();
            invalidValue = Double.parseDouble(ln);
        }
    }

    /**
     * @param line
     * @throws Exception 
     */
    private void processHeaderColOrder(String line) throws Exception {
        String ln = checkIfCommentLineRemoveMarkerOrThrowException(line);
        
        String[] tokens = ln.split(",");
        if (tokens.length < 6)
            throwUnexpectedException();
        
        int tkCount = -1;
        for (String tk : tokens) {
            tkCount++;
            tk = tk.trim();
            if (tk.isEmpty())
                continue;
            
            String[] varTokens = tk.split("[\\(\\)]");
            if("time".equalsIgnoreCase(varTokens[0].trim())) {
                timeIdx = tkCount;
                numberOfElements++;
            }
            else if("latitude".equalsIgnoreCase(varTokens[0].trim()) || "lat".equalsIgnoreCase(varTokens[0].trim())) {
                latIdx = tkCount;
                numberOfElements++;
            }
            else if("longitude".equalsIgnoreCase(varTokens[0].trim()) || "lon".equalsIgnoreCase(varTokens[0].trim())) {
                lonIdx = tkCount;
                numberOfElements++;
            }
            else if("depth".equalsIgnoreCase(varTokens[0].trim())) {
                depthIdx = tkCount;
                numberOfElements++;
            }
            else if("rhodamine".equalsIgnoreCase(varTokens[0].trim())) {
                if (varTokens.length == 1) {
                    rhodamineIdx = tkCount;
                    numberOfElements++;
                }
                else {
                    if ("PPB".equalsIgnoreCase(varTokens[1].trim())) {
                        rhodamineIdx = tkCount;
                        numberOfElements++;
                    }
                    else if ("raw".equalsIgnoreCase(varTokens[1].trim())) {
                        rhodamineRawIdx = tkCount;
                        numberOfElements++;
                    }
                }
            }
            else if(varTokens[0].trim().toLowerCase().startsWith("crude")) {
                crudeOilIdx = tkCount;
                numberOfElements++;
            }
            else if(varTokens[0].trim().toLowerCase().startsWith("refine")) {
                refineOilIdx = tkCount;
                numberOfElements++;
            }
            else if("temperature".equalsIgnoreCase(varTokens[0].trim())) {
                temperatureIdx = tkCount;
                numberOfElements++;
            }
        }
        
        if (timeIdx < 0)
            throwUnexpectedException();
        else if (latIdx < 0)
            throwUnexpectedException();
        else if (lonIdx < 0)
            throwUnexpectedException();
        else if (lonIdx < 0)
            throwUnexpectedException();
        else if (rhodamineIdx < 0)
            throwUnexpectedException();
//        else if (rhodamineRawIdx < 0)
//            throwUnexpectedException();
        else if (temperatureIdx < 0)
            throwUnexpectedException();
    }

    /**
     * @param line
     */
    private void processData(String line) {
        if (line.startsWith("%") || numberOfElements < 1)
            return;
        
        String[] tokens = line.trim().split(",");
        if (tokens.length < numberOfElements)
            return;

        double timeSecs = Double.NaN;
        double lat = Double.NaN;
        double lon = Double.NaN;
        double depth = Double.NaN;
        double rhodamine = Double.NaN;
        double rhodamineRaw = Double.NaN;
        double crudeOil = Double.NaN;
        double refineOil = Double.NaN;
        double temperature = Double.NaN;

        int ct = -1;
        for (String tk : tokens) {
            ct++;
            if (ct == timeIdx) {
                timeSecs = Double.parseDouble(tk); 
            }
            else if (ct == latIdx) {
                lat = Double.parseDouble(tk); 
            } 
            else if (ct == lonIdx) {
                lon = Double.parseDouble(tk); 
            } 
            else if (ct == depthIdx) {
                depth = Double.parseDouble(tk); 
            }
            else if (ct == rhodamineIdx) {
                rhodamine = Double.parseDouble(tk);
                if (invalidValue == rhodamine)
                    rhodamine = Double.NaN;
            } 
            else if (ct == rhodamineRawIdx) {
                rhodamineRaw = Double.parseDouble(tk); 
                if (invalidValue == rhodamineRaw)
                    rhodamineRaw = Double.NaN;
            } 
            else if (ct == crudeOilIdx) {
                crudeOil = Double.parseDouble(tk); 
                if (invalidValue == crudeOil)
                    crudeOil = Double.NaN;
            } 
            else if (ct == refineOilIdx) {
                refineOil = Double.parseDouble(tk); 
                if (invalidValue == refineOil)
                    refineOil = Double.NaN;
            } 
            else if (ct == temperatureIdx) {
                temperature = Double.parseDouble(tk); 
                if (invalidValue == temperature)
                    temperature = Double.NaN;
            } 
        }

        if (Double.isNaN(timeSecs) || Double.isNaN(lat) || Double.isNaN(lon))
            return;
        
        if (millisMaxAge > 0) {
            if (curTimeMillis - (long) (timeSecs * 1000) > millisMaxAge)
                return;
        }
        
        BaseData point = new BaseData(lat, lon, depth, (long) (timeSecs * 1000));
        point.setSourceSystem(getSystem());
        point.setRhodamineDyePPB(checkForInvalidValue(rhodamine));
        point.setTemperature(checkForInvalidValue(temperature));
        point.setCrudeOilPPB(checkForInvalidValue(crudeOil));
        point.setRefineOilPPB(checkForInvalidValue(refineOil));
        
        points.add(point);
    }

    /**
     * @param value
     * @return
     */
    private double checkForInvalidValue(double value) {
        if (!Double.isFinite(value))
            return Double.NaN;
        if (Double.compare(value, invalidValue) == 0)
            return Double.NaN;
        return value;
    }

    /**
     * @throws Exception
     */
    private void throwUnexpectedException() throws Exception {
        throw new Exception("Unexpected exception");
    }

    /**
     * @param line
     * @return 
     * @throws Exception
     */
    private String checkIfCommentLineRemoveMarkerOrThrowException(String line) throws Exception {
        if (!line.startsWith("%") && !line.startsWith("\"%"))
            throw new Exception("Expected a commment line");
        
        return line.replaceFirst("^\"?%", "").trim();
    }

    /**
     * @param args
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException {
        CSVDataParser csv = new CSVDataParser(new File("test.csv"));
//        CSVDataParser csv = new CSVDataParser(new File("log_2014-09-24_22-15.csv"));
        
        csv.parse();

        System.out.println(csv.getSystem());
        for (BaseData pt : csv.getPoints()) {
            System.out.println(">\t" + pt);
            System.out.println(new Date(pt.getTimeMillis()));
        }
        
        System.out.println("Not valid value (-1)".replaceAll("[a-zA-Z \\(\\)]", ""));
    }

}
