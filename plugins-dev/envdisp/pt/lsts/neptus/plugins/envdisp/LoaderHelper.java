/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 6 de Jul de 2013
 */
package pt.lsts.neptus.plugins.envdisp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class LoaderHelper {

    private static final String NETCDF_ATT_STANDARD_NAME = "standard_name";
    private static final String NETCDF_ATT_FILL_VALUE = "_FillValue";
    private static final String NETCDF_ATT_UNITS = "units";

    public static final HashMap<String, HFRadarDataPoint> processTUGHFRadar(Reader readerInput, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
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
                        dateStart = HFRadarVisualization.dateTimeFormaterSpacesUTC.parse(line,
                                new ParsePosition(tsStr.length() - 1));
                    }
                    
                    line = reader.readLine();
                    continue;
                }

                if (dateStart == null) {
                    NeptusLog.pub().warn("No start date found on TUV CODAR file.");
                    return hfdp;
                }

                if (!ignoreDateLimitToLoad && dateStart.before(dateLimit)) {
                    return hfdp;
                }


                String[] tokens = line.trim().split("[\\t ,]+");
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

    public static final HashMap<String, HFRadarDataPoint> processNetCDFHFRadar(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing Currents netCDF file '" + fileName + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        
        NetcdfFile dataFile = null;

        Date fromDate = null;
        Date toDate = null;
        
        try {
          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Pair<String, Variable> searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the u (north) wind velocity Variables.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "x_wind", "grid_eastward_wind", "u");
          @SuppressWarnings("unused")
          String xWindName = searchPair == null ? null : searchPair.first();
          Variable uVar = searchPair == null ? null : searchPair.second();

          // Get the v (east) wind velocity Variables.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "y_wind", "grid_northward_wind", "v");
          @SuppressWarnings("unused")
          String yWindName = searchPair == null ? null : searchPair.first();
          Variable vVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array depthOrAltitudeArray; //ArrayFloat.D1
          Array uArray;    // ArrayFloat.D?
          Array vArray;    // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          depthOrAltitudeArray = depthOrAltitudeVar == null ? null : depthOrAltitudeVar.read(); 

          if (uVar == null || vVar == null)
              return hfdp;
          
          uArray = uVar == null ? null : uVar.read();
          vArray = vVar == null ? null : vVar.read();
          
          double[] multAndOffset = getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          String uUnits = "cm/s";
          Attribute uUnitsAtt = uVar.findAttribute(NETCDF_ATT_UNITS);
          if (uUnitsAtt != null)
              uUnits = (String) uUnitsAtt.getValue(0);
          String vUnits = "cm/s";
          Attribute vUnitsAtt = uVar.findAttribute(NETCDF_ATT_UNITS);
          if (vUnitsAtt != null)
              vUnits = (String) vUnitsAtt.getValue(0);

          double uFillValue = findFillValue(uVar);
          double vFillValue = findFillValue(vVar);
          
          int[] shape = uVar.getShape();
          int[] counter = new int[shape.length];
          Arrays.fill(counter, 0);
          String dimStr = uVar.getDimensionsString();
          Map<String, Integer> collumsIndexMap = getIndexesForVar(dimStr, timeName, latName, lonName,
                  depthOrAltitudeName);

          do {
              Date dateValue = null;
              Date[] timeVals = getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
                      toDate, ignoreDateLimitToLoad, dateLimit);
              if (timeVals == null) {
                  continue;
              }
              else {
                  dateValue = timeVals[0];
                  fromDate = timeVals[1];
                  toDate = timeVals[2];
              }

              double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[collumsIndexMap.get(latName)]));
              double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[collumsIndexMap.get(lonName)]));

              // We don't do anything yet with depth or alt
              @SuppressWarnings("unused")
              boolean depthOrAltIndicator = depthOrAltitudeName != null
                      && depthOrAltitudeName.equalsIgnoreCase("altitude") ? false : true;
              @SuppressWarnings("unused")
              double depthOrAlt = Double.NaN;
              if (depthOrAltitudeName != null && collumsIndexMap.get(depthOrAltitudeName) > 0)
                  depthOrAlt = depthOrAltitudeArray.getDouble(counter[collumsIndexMap.get(depthOrAltitudeName)]);

              Index uIndex = uArray.getIndex();
              uIndex.set(counter);
              Index vIndex = vArray.getIndex();
              vIndex.set(counter);
              
              double u = uArray == null ? Double.NaN : uArray.getDouble(uIndex);
              double v = vArray == null ? Double.NaN : vArray.getDouble(vIndex);

              if (!Double.isNaN(u) && !Double.isNaN(v)
                      && u != uFillValue && v != vFillValue) {
                  
                  u = u * getMultiplierForCmPerSecondsFromSpeedUnits(uUnits);
                  v = v * getMultiplierForCmPerSecondsFromSpeedUnits(vUnits);
                  double speedCmS = Math.sqrt(u * u + v * v) * 100;
                  double heading = Math.atan2(v, u);

                  HFRadarDataPoint dp = new HFRadarDataPoint(lat, lon);
                  dp.setSpeedCmS(speedCmS);
                  dp.setHeadingDegrees(Math.toDegrees(heading));
                  dp.setDateUTC(dateValue);

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
              }
          } while (nextShapeStage(shape, counter) != null);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            NeptusLog.pub().info("Ending processing Currents netCDF file '" + fileName + "'. Reading from date '"
                    + fromDate + "' till '" + toDate + "'.");
        }
        
        return hfdp;
    }

    public static final HashMap<?, ?>[] processMeteo(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing Meteo (wind and SST) netCDF file '" + fileName + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, SSTDataPoint> sstdp = new HashMap<>();
        HashMap<String, WindDataPoint> winddp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        Date fromDate = null;
        Date toDate = null;
        
        try {
          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Pair<String, Variable> searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the u (north) wind velocity Variables.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "x_wind", "grid_eastward_wind", "u");
          @SuppressWarnings("unused")
          String xWindName = searchPair == null ? null : searchPair.first();
          Variable uVar = searchPair == null ? null : searchPair.second();

          // Get the v (east) wind velocity Variables.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "y_wind", "grid_northward_wind", "v");
          @SuppressWarnings("unused")
          String yWindName = searchPair == null ? null : searchPair.first();
          Variable vVar = searchPair == null ? null : searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "sea_surface_temperature", "sst");
          @SuppressWarnings("unused")
          String sstName = searchPair == null ? null : searchPair.first();
          Variable sstVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array depthOrAltitudeArray; //ArrayFloat.D1
          Array uArray;    // ArrayFloat.D?
          Array vArray;    // ArrayFloat.D?
          Array sstArray;  // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          depthOrAltitudeArray = depthOrAltitudeVar == null ? null : depthOrAltitudeVar.read(); 
          uArray = uVar == null ? null : uVar.read();
          vArray = vVar == null ? null : vVar.read();
          sstArray = sstVar == null ? null : sstVar.read();
          
          double[] multAndOffset = getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          // Let us process SST
          if (sstVar != null) {
              try {
                  String sstUnits = "K";
                  Attribute sstUnitsAtt = sstVar == null ? null : sstVar.findAttribute(NETCDF_ATT_UNITS);
                  if (sstUnitsAtt != null)
                      sstUnits = (String) sstUnitsAtt.getValue(0);

                  double sstFillValue = findFillValue(sstVar);
                  
                  int[] shape = sstVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = sstVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
                              toDate, ignoreDateLimitToLoad, dateLimit);
                      if (timeVals == null) {
                          continue;
                      }
                      else {
                          dateValue = timeVals[0];
                          fromDate = timeVals[1];
                          toDate = timeVals[2];
                      }

                      double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[collumsIndexMap.get(latName)]));
                      double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[collumsIndexMap.get(lonName)]));

                      // We don't do anything yet with depth or alt
                      @SuppressWarnings("unused")
                      boolean depthOrAltIndicator = depthOrAltitudeName != null
                              && depthOrAltitudeName.equalsIgnoreCase("altitude") ? false : true;
                      @SuppressWarnings("unused")
                      double depthOrAlt = Double.NaN;
                      if (depthOrAltitudeName != null && collumsIndexMap.get(depthOrAltitudeName) > 0)
                          depthOrAlt = depthOrAltitudeArray.getDouble(counter[collumsIndexMap.get(depthOrAltitudeName)]);

                      Index index = sstArray.getIndex();
                      index.set(counter);

                      double sst = sstArray == null ? Double.NaN : sstArray.getDouble(index);

                      if (!Double.isNaN(sst) && sst != sstFillValue) {
                          SSTDataPoint dp = new SSTDataPoint(lat, lon);
                          sst = getValueForDegreesCelciusFromTempUnits(sst, sstUnits);
                          dp.setSst(sst);
                          dp.setDateUTC(dateValue);

                          SSTDataPoint dpo = sstdp.get(SSTDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              sstdp.put(SSTDataPoint.getId(dp), dp);
                          }

                          ArrayList<SSTDataPoint> lst = dpo.getHistoricalData();
                          boolean alreadyIn = false;
                          for (SSTDataPoint tmpDp : lst) {
                              if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                                  alreadyIn = true;
                                  break;
                              }
                          }
                          if (!alreadyIn) {
                              dpo.getHistoricalData().add(dp);
                          }
                      }
                  } while (nextShapeStage(shape, counter) != null);
              }
              catch (Exception e) {
                  e.printStackTrace();
              }
          }

          // Let us process Wind
          if (uVar != null && vVar != null) {
              try {
                  String uUnits = "cm/s";
                  Attribute uUnitsAtt = uVar == null ? null : uVar.findAttribute(NETCDF_ATT_UNITS);
                  if (uUnitsAtt != null)
                      uUnits = (String) uUnitsAtt.getValue(0);
                  String vUnits = "cm/s";
                  Attribute vUnitsAtt = vVar == null ? null : vVar.findAttribute(NETCDF_ATT_UNITS);
                  if (vUnitsAtt != null)
                      vUnits = (String) vUnitsAtt.getValue(0);

                  double uFillValue = findFillValue(uVar);
                  double vFillValue = findFillValue(vVar);

                  int[] shape = uVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = uVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
                              toDate, ignoreDateLimitToLoad, dateLimit);
                      if (timeVals == null) {
                          continue;
                      }
                      else {
                          dateValue = timeVals[0];
                          fromDate = timeVals[1];
                          toDate = timeVals[2];
                      }

                      double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[collumsIndexMap.get(latName)]));
                      double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[collumsIndexMap.get(lonName)]));

                      // We don't do anything yet with depth or alt
                      @SuppressWarnings("unused")
                      boolean depthOrAltIndicator = depthOrAltitudeName != null
                              && depthOrAltitudeName.equalsIgnoreCase("altitude") ? false : true;
                      @SuppressWarnings("unused")
                      double depthOrAlt = Double.NaN;
                      if (depthOrAltitudeName != null && collumsIndexMap.get(depthOrAltitudeName) > 0)
                          depthOrAlt = depthOrAltitudeArray.getDouble(counter[collumsIndexMap.get(depthOrAltitudeName)]);

                      Index uIndex = uArray.getIndex();
                      uIndex.set(counter);
                      Index vIndex = vArray.getIndex();
                      vIndex.set(counter);
                      
                      double u = uArray == null ? Double.NaN : uArray.getDouble(uIndex);
                      double v = vArray == null ? Double.NaN : vArray.getDouble(vIndex);

                      if (!Double.isNaN(u) && !Double.isNaN(v)
                              && u != uFillValue && v != vFillValue) {
                          WindDataPoint dp = new WindDataPoint(lat, lon);
                          u = u * getMultiplierForCmPerSecondsFromSpeedUnits(uUnits);
                          v = v * getMultiplierForCmPerSecondsFromSpeedUnits(vUnits);
                          dp.setU(u);
                          dp.setV(v);
                          dp.setDateUTC(dateValue);

                          WindDataPoint dpo = winddp.get(WindDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              winddp.put(HFRadarDataPoint.getId(dp), dp);
                          }

                          ArrayList<WindDataPoint> lst = dpo.getHistoricalData();
                          boolean alreadyIn = false;
                          for (WindDataPoint tmpDp : lst) {
                              if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                                  alreadyIn = true;
                                  break;
                              }
                          }
                          if (!alreadyIn) {
                              dpo.getHistoricalData().add(dp);
                          }
                      }
                  } while (nextShapeStage(shape, counter) != null);
              }
              catch (Exception e) {
                  e.printStackTrace();
              }
          }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            NeptusLog.pub().info("Ending processing Meteo (wind and SST) netCDF file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }

        return new HashMap[] { sstdp, winddp };
    }

    public static final HashMap<String, WavesDataPoint> processWavesFile(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing Waves netCDF file '" + fileName + "'." + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, WavesDataPoint> wavesdp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        Date fromDate = null;
        Date toDate = null;

        try {
          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Pair<String, Variable> searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the significant height Variable.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true,
                  "sea_surface_wave_significant_height", "significant_height_of_wind_and_swell_waves", "hs");
          Variable hsVar = searchPair == null ? null : searchPair.second();

          // Get the peak period Variable.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true,
                  "sea_surface_wave_period_at_variance_spectral_density_maximum", "tp");
          Variable tpVar = searchPair == null ? null : searchPair.second();

          // Get the peak direction Variable.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "sea_surface_wave_to_direction",
                  "pdir");
          Variable pdirVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; // ArrayFloat.D1
          Array depthOrAltitudeArray; //ArrayFloat.D1
          Array hsArray;   // ArrayFloat.D?
          Array tpArray;   // ArrayFloat.D?
          Array pdirArray; // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          depthOrAltitudeArray = depthOrAltitudeVar == null ? null : depthOrAltitudeVar.read(); 
          hsArray = hsVar.read();
          tpArray = tpVar.read();
          pdirArray = pdirVar.read();
          
          double[] multAndOffset = getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          double hsFillValue = findFillValue(hsVar);
          double tpFillValue = findFillValue(tpVar);
          double pdirFillValue = findFillValue(pdirVar);

          int[] shape = hsVar.getShape();
          int[] counter = new int[shape.length];
          Arrays.fill(counter, 0);
          String dimStr = hsVar.getDimensionsString();
          Map<String, Integer> collumsIndexMap = getIndexesForVar(dimStr, timeName, latName, lonName,
                  depthOrAltitudeName);

          do {
              Date dateValue = null;
              Date[] timeVals = getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
                      toDate, ignoreDateLimitToLoad, dateLimit);
              if (timeVals == null) {
                  continue;
              }
              else {
                  dateValue = timeVals[0];
                  fromDate = timeVals[1];
                  toDate = timeVals[2];
              }

              double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[collumsIndexMap.get(latName)]));
              double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[collumsIndexMap.get(lonName)]));

              // We don't do anything yet with depth or alt
              @SuppressWarnings("unused")
              boolean depthOrAltIndicator = depthOrAltitudeName != null
                      && depthOrAltitudeName.equalsIgnoreCase("altitude") ? false : true;
              @SuppressWarnings("unused")
              double depthOrAlt = Double.NaN;
              if (depthOrAltitudeName != null && collumsIndexMap.get(depthOrAltitudeName) > 0)
                  depthOrAlt = depthOrAltitudeArray.getDouble(counter[collumsIndexMap.get(depthOrAltitudeName)]);

              Index index = hsArray.getIndex();
              index.set(counter);
              double hs = hsArray.getDouble(index);
              index = hsArray.getIndex();
              index.set(counter);
              double tp = tpArray.getDouble(index);
              index = hsArray.getIndex();
              index.set(counter);
              double pdir = pdirArray.getDouble(index);


              if (!Double.isNaN(hs) && !Double.isNaN(tp) && !Double.isNaN(pdir)
                      && hs != hsFillValue && tp != tpFillValue && pdir != pdirFillValue) {
                  WavesDataPoint dp = new WavesDataPoint(lat, lon);
                  dp.setSignificantHeight(hs);
                  dp.setPeakPeriod(tp);
                  dp.setPeakDirection(pdir);
                  dp.setDateUTC(dateValue);

                  WavesDataPoint dpo = wavesdp.get(WavesDataPoint.getId(dp));
                  if (dpo == null) {
                      dpo = dp.getACopyWithoutHistory();
                      wavesdp.put(HFRadarDataPoint.getId(dp), dp);
                  }

                  ArrayList<WavesDataPoint> lst = dpo.getHistoricalData();
                  boolean alreadyIn = false;
                  for (WavesDataPoint tmpDp : lst) {
                      if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                          alreadyIn = true;
                          break;
                      }
                  }
                  if (!alreadyIn) {
                      dpo.getHistoricalData().add(dp);
                  }
              }
          } while (nextShapeStage(shape, counter) != null);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            NeptusLog.pub().info("Ending processing Waves netCDF file '" + fileName + "'. Reading from date '"
                    + fromDate + "' till '" + toDate + "'.");
        }

        return wavesdp;
    }
    
    public static final HashMap<String, ChlorophyllDataPoint> processChlorophyll(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing ChlorophyllnetCDF file '" + fileName + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, ChlorophyllDataPoint> chlorophylldp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        Date fromDate = null;
        Date toDate = null;
        
        try {
          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Pair<String, Variable> searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the chlorophyll Variable.
          searchPair = findVariableForStandardNameOrName(dataFile, fileName, true,
                  "mass_concentration_of_chlorophyll_in_sea_water", "chlorophyll_concentration_in_sea_water",
                  "concentration_of_chlorophyll_in_sea_water", "chlorophyll");
          @SuppressWarnings("unused")
          String chlorophyllName = searchPair == null ? null : searchPair.first();
          Variable chlorophyllVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array depthOrAltitudeArray; //ArrayFloat.D1
          Array chlorophyllArray;    // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          depthOrAltitudeArray = depthOrAltitudeVar == null ? null : depthOrAltitudeVar.read(); 
          chlorophyllArray = chlorophyllVar == null ? null : chlorophyllVar.read();
          
          double[] multAndOffset = getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          // Let us process SST
          if (chlorophyllVar != null) {
              try {
                  String chlorophyllUnits = "kg m-3";
                  Attribute chlorophyllUnitsAtt = chlorophyllVar == null ? null : chlorophyllVar.findAttribute(NETCDF_ATT_UNITS);
                  if (chlorophyllUnitsAtt != null)
                      chlorophyllUnits = (String) chlorophyllUnitsAtt.getValue(0);

                  double chlorophyllFillValue = findFillValue(chlorophyllVar);
                  
                  int[] shape = chlorophyllVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = chlorophyllVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
                              toDate, ignoreDateLimitToLoad, dateLimit);
                      if (timeVals == null) {
                          continue;
                      }
                      else {
                          dateValue = timeVals[0];
                          fromDate = timeVals[1];
                          toDate = timeVals[2];
                      }

                      double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[collumsIndexMap.get(latName)]));
                      double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[collumsIndexMap.get(lonName)]));

                      // We don't do anything yet with depth or alt
                      @SuppressWarnings("unused")
                      boolean depthOrAltIndicator = depthOrAltitudeName != null
                              && depthOrAltitudeName.equalsIgnoreCase("altitude") ? false : true;
                      @SuppressWarnings("unused")
                      double depthOrAlt = Double.NaN;
                      if (depthOrAltitudeName != null && collumsIndexMap.get(depthOrAltitudeName) > 0)
                          depthOrAlt = depthOrAltitudeArray.getDouble(counter[collumsIndexMap.get(depthOrAltitudeName)]);

                      Index index = chlorophyllArray.getIndex();
                      index.set(counter);

                      double chlorophyll = chlorophyllArray == null ? Double.NaN : chlorophyllArray.getDouble(index);

                      if (!Double.isNaN(chlorophyll) && chlorophyll != chlorophyllFillValue) {
                          ChlorophyllDataPoint dp = new ChlorophyllDataPoint(lat, lon);
                          
                          chlorophyll = getValueForMilliGPerM3FromTempUnits(chlorophyll, chlorophyllUnits);
                          
                          dp.setChlorophyll(chlorophyll);
                          dp.setDateUTC(dateValue);

                          ChlorophyllDataPoint dpo = chlorophylldp.get(ChlorophyllDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              chlorophylldp.put(ChlorophyllDataPoint.getId(dp), dp);
                          }

                          ArrayList<ChlorophyllDataPoint> lst = dpo.getHistoricalData();
                          boolean alreadyIn = false;
                          for (ChlorophyllDataPoint tmpDp : lst) {
                              if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                                  alreadyIn = true;
                                  break;
                              }
                          }
                          if (!alreadyIn) {
                              dpo.getHistoricalData().add(dp);
                          }
                      }
                  } while (nextShapeStage(shape, counter) != null);
              }
              catch (Exception e) {
                  e.printStackTrace();
              }
          }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            NeptusLog.pub().info("Ending processing Chlorophyll netCDF file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }

        return chlorophylldp;
    }

    /**
     * @param timeArray
     * @param timeIdx
     * @param timeMultiplier
     * @param timeOffset
     * @param fromDate
     * @param toDate
     * @param ignoreDateLimitToLoad
     * @param dateLimit
     * @return
     */
    private static Date[] getTimeValues(Array timeArray, int timeIdx, double timeMultiplier, double timeOffset,
            Date fromDate, Date toDate, boolean ignoreDateLimitToLoad, Date dateLimit) {
        double timeVal = timeArray.getDouble(timeIdx); // get(timeIdx);
        Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
        
        if (!ignoreDateLimitToLoad && dateValue.before(dateLimit))
            return null;
        
        if (fromDate == null) {
            fromDate = dateValue;
        }
        else {
            if (dateValue.before(fromDate))
                fromDate = dateValue;
        }
        if (toDate == null) {
            toDate = dateValue;
        }
        else {
            if (dateValue.after(toDate))
                toDate = dateValue;
        }
        
        return new Date[] {dateValue, fromDate, toDate};
    }

    /**
     * @param var
     * @return
     * @throws NumberFormatException
     */
    private static double findFillValue(Variable var) throws NumberFormatException {
        Attribute fillValueAtt = var == null ? null : var.findAttribute(NETCDF_ATT_FILL_VALUE);
        if (fillValueAtt != null) {
            try {
                return ((Number) fillValueAtt.getValue(0)).doubleValue();
            }
            catch (ClassCastException e) {
                return Double.parseDouble((String) fillValueAtt.getValue(0));
            }
        }
        return Double.NaN;
    }

    /**
     * @param timeVar
     * @param fileNameForErrorString
     * @return
     * @throws Exception
     */
    private static double[] getTimeMultiplierAndOffset(Variable timeVar, String fileNameForErrorString)
            throws Exception {
        String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
        Attribute timeUnitsAtt = timeVar.findAttribute(NETCDF_ATT_UNITS);
        if (timeUnitsAtt != null)
            timeUnits = (String) timeUnitsAtt.getValue(0);
        double[] multAndOffset = getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
        if (multAndOffset == null) {
            throw new Exception("Aborting. Can't parse units for variable 'time' (was '" + timeUnits
                    + "') for netCDF file '" + fileNameForErrorString + "'.");
        }
        return multAndOffset;
    }

    /**
     * @param dataFile
     * @param fileNameForErrorString
     * @param failIfNotFound
     * @param varStName
     * @return
     */
    private static Pair<String, Variable> findVariableForStandardNameOrName(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varStName) {
        Pair<String, Variable> ret = findVariableForStandardName(dataFile, fileNameForErrorString, false, varStName);
        if (ret == null)
            ret = findVariableFor(dataFile, fileNameForErrorString, failIfNotFound, varStName);
        
        return ret;
    }

    /**
     * @param dataFile
     * @param fileNameForErrorString
     * @param failIfNotFound
     * @param varName
     * @return
     */
    private static Pair<String, Variable> findVariableFor(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varName) {
        String name = "";
        Variable latVar = null;
        for (String st : varName) {
            latVar = dataFile.findVariable(st);
            if (latVar != null) {
                name = st;
                break;
            }
        }
        if (latVar == null) {
            String message = "Can't find variable '" + Arrays.toString(varName) + "' for netCDF file '"
                    + fileNameForErrorString + "'.";
            if (failIfNotFound) {
                new Exception("Aborting. " + message);
            }
            else {
                NeptusLog.pub().error(message);
                return null;
            }
        }
        return new Pair<String, Variable>(name, latVar);
    }

    private static Pair<String, Variable> findVariableForStandardName(NetcdfFile dataFile, String fileNameForErrorString,
            boolean failIfNotFound, String... varName) {
        String name = "";
        Variable latVar = null;
        for (String st : varName) {
            latVar = dataFile.findVariableByAttribute(null, NETCDF_ATT_STANDARD_NAME, st);
            if (latVar != null) {
                name = latVar.getShortName();
                break;
            }
        }
        if (latVar == null) {
            String message = "Can't find variable standard name '" + Arrays.toString(varName) + "' for netCDF file '"
                    + fileNameForErrorString + "'.";
            if (failIfNotFound) {
                new Exception("Aborting. " + message);
            }
            else {
                NeptusLog.pub().error(message);
                return null;
            }
        }
        return new Pair<String, Variable>(name, latVar);
    }

    /**
     * @param dimStr
     * @param name
     * @return
     */
    private static Map<String, Integer> getIndexesForVar(String dimStr, String... name) {
        HashMap<String, Integer> ret = new HashMap<>();
        if (dimStr == null || dimStr.length() == 0)
            return ret;
        
        Arrays.stream(name).filter(n -> n != null && !n.isEmpty()).forEach(n -> ret.put(n.trim(), -1));
        
        String[] tk = dimStr.split("[, \t]");
        for (int i = 0; i < tk.length; i++) {
            for (String n : ret.keySet()) {
                if (n.equalsIgnoreCase(tk[i].trim())) {
                    ret.put(n, i);
                    break;
                }
            }
        }

        return ret;
    }

    /**
     * Time Coordinate in the NetCDF Climate and Forecast (CF) Metadata Conventions v1.6
     * indicated the format as: <br/>
     *   
     *   <ul>
     *      <li>"seconds since 1992-10-8 15:15:42.5 -6:00"</li>
     *   </ul>
     * 
     * From http://coastwatch.pfeg.noaa.gov/erddap/convert/time.html:<br/><br/>
     * 
     * The first word can be (upper or lower case): <br/>
     *   <ul>
     *      <li>ms, msec, msecs, millis, millisecond, milliseconds,</li>
     *      <li>ms, msec, msecs, millis, millisecond, milliseconds,</li> 
     *      <li>s, sec, secs, second, seconds,</li>
     *      <li>m, min, mins, minute, minutes,</li>
     *      <li>h, hr, hrs, hour, hours,</li>
     *      <li>d, day, days,</li>
     *      <li>week, weeks, (not support)</li>
     *      <li>mon, mons, month, months, (not support)</li>
     *      <li>yr, yrs, year, or years (not support).</li>
     *   </ul>
     * 
     * "since" is required. <br/><br/>
     * 
     * The time can be any time in the format yyyy-MM-ddTHH:mm:ss.SSSZ, 
     * where Z is 'Z' or a ±hh or ±hh:mm offset from the Zulu/GMT time zone. 
     * If you omit Z and the offset, the Zulu/GMT time zone is used. 
     * Separately, if you omit .SSS, :ss.SSS, :mm:ss.SSS, or Thh:mm:ss.SSS, the 
     * missing fields are assumed to be 0.<br/><br/>
     * 
     * So another example is "hours since 0001-01-01".<br/><br/>
     * 
     * Technically, ERDDAP does NOT follow the UDUNITS standard when converting "years since" 
     * and "months since" time values to "seconds since". The UDUNITS standard defines a 
     * year as a fixed, single value: 3.15569259747e7 seconds. And UDUNITS defines a month 
     * as year/12. Unfortunately, most/all datasets that we have seen that use 
     * "years since" or "months since" clearly intend the values to be calendar years 
     * or calendar months. For example, "3 months since 1970-01-01" is usually intended 
     * to mean 1970-04-01. So, ERDDAP interprets "years since" and "months since" as 
     * calendar years and months, and does not strictly follow the UDUNITS standard.
     * 
     * @param timeStr
     * @return
     */
    public static double[] getMultiplierAndMillisOffsetFromTimeUnits(String timeStr) {
        if ("days since 00-01-00 00:00:00".equalsIgnoreCase(timeStr) || "days since 00-01-00".equalsIgnoreCase(timeStr)) {
            // Reference time in year zero has special meaning
            return new double[] { DateTimeUtil.DAY, - DateTimeUtil.DAYS_SINCE_YEAR_0_TILL_1970 * DateTimeUtil.DAY};
        }
        else {
            String[] tk = timeStr.trim().split("[ ]");
            if (tk.length < 3) {
                return null;
            }
            else {
                double mult = 1;
                double off = 1;
                switch (tk[0].trim().toLowerCase().replace(".", "")) {
                    case "days":
                    case "day":
                    case "d":
                        mult = DateTimeUtil.DAY;
                        break;
                    case "hours":
                    case "hour":
                    case "hr":
                    case "h":
                        mult = DateTimeUtil.HOUR;
                        break;
                    case "minutes":
                    case "minute":
                    case "min":
                        mult = DateTimeUtil.MINUTE;
                        break;
                    case "seconds":
                    case "second":
                    case "sec":
                    case "s":
                        mult = DateTimeUtil.SECOND;
                        break;
                }
                
                String dateTkStr = tk[2];
                String timeTkStr = tk.length > 3 ? tk[3] : "0:0:0";
                String timeZoneTkStr = tk.length > 4 ? tk[4] : "";
                if (tk[2].contains("T")) { // Then is a ISO 8601, e.g. 1970-01-01T00:00:00Z 
                    String[] sp1 = tk[2].split("T");
                    dateTkStr = sp1[0];
                    if (sp1[1].contains("+")) {
                        String[] sp2 = sp1[1].split("\\+");
                        timeTkStr = sp2[0];
                        timeZoneTkStr = "+" + sp2[1];
                    }
                    else if (sp1[1].contains("\u2212") || sp1[1].contains("-")) {
                        String[] sp2 = sp1[1].split("[-\u2212]");
                        timeTkStr = sp2[0];
                        timeZoneTkStr = "-" + sp2[1];
                    }
                    else if (sp1[1].endsWith("Z")) {
                        timeTkStr = sp1[1].replaceAll("Z", "");
                        timeZoneTkStr = "";
                    }
                }
                
                try {
                    Date date = HFRadarVisualization.dateTimeFormaterUTC.parse(dateTkStr + " " + timeTkStr);
                    off = date.getTime();
                    
                    // Let us see if milliseconds are present
                    String[] mSplitArray = timeTkStr.split("\\.");
                    if (mSplitArray.length > 1) {
                        String millisStr = mSplitArray[1];
                        int millisSize = millisStr.length();
                        switch (millisSize) {
                            case 1:
                                off += Integer.parseInt(millisStr) * 100;
                                break;
                            case 2:
                                off += Integer.parseInt(millisStr) * 10;
                                break;
                            case 3:
                                off += Integer.parseInt(millisStr);
                                break;
                            default:
                                off += Integer.parseInt(millisStr.substring(0, 3));
                                break;
                        }
                    }
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }
                
                try {
                    if (!timeZoneTkStr.isEmpty()) { // So we have a time zone and so it's not UTC
                        // The time zone specification
                        // can also be written without a colon using one or two-digits
                        // (indicating hours) or three or four digits (indicating hours
                        // and minutes)
                        timeZoneTkStr = timeZoneTkStr.replace("\u2212",  "-"); //Replace unicode "-"
                        String[] tzStrs = timeZoneTkStr.split(":");
                        if (tzStrs.length > 1) { // Has colon
                            int hrTzNb = Integer.parseInt(tzStrs[0]); 
                            off -= hrTzNb * DateTimeUtil.HOUR;
                            off -= Integer.parseInt(tzStrs[1]) * Math.signum(hrTzNb) * DateTimeUtil.MINUTE;
                        }
                        else {
                            String tzSt = timeZoneTkStr.replace(":", "");
                            int tzNb = Integer.parseInt(tzSt);
                            if (Math.abs(tzNb) < 100) { // It's hours
                                off -= tzNb * DateTimeUtil.HOUR;
                            }
                            else { // It's hours plus minutes
                                int hrTzNb = tzNb / 100;
                                off -= hrTzNb * DateTimeUtil.HOUR;
                                int minTzNb = tzNb - hrTzNb * 100;
                                off -= minTzNb * DateTimeUtil.MINUTE;
                            }
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                return new double[] { mult, off };
            }
        }
    }
    
    // FIXME better control of unit for speed
    private static double getMultiplierForCmPerSecondsFromSpeedUnits(String speedUnits) {
        double mult = 1;
        switch (speedUnits.trim().toLowerCase()) {
            case "cm/s":
            case "cm s-1":
            case "cm s^-1":
            case "cm.s^-1":
                mult = 1;
                break;
            case "m/s":
            case "m s-1":
            case "m s^-1":
            case "m.s^-1":
                mult = 100;
                break;
            case "ft/s":
            case "ft s-1":
            case "ft s^-1":
            case "ft.s^-1":
                mult = 0.3048 * 100;
        }
        
        return mult;
    }

    /**
     * @param value
     * @param units
     * @return
     */
    private static double getValueForDegreesCelciusFromTempUnits(double value, String units) {
        double ret = value;
        switch (units.trim()) {
            case "K":
                ret = value + SSTDataPoint.KELVIN_TO_CELSIUS;
                break;
            case "\u00B0F":
            case "ºF":
                ret = (value - 32) / 1.8;
                break;
        }

        return ret;
    }

    private static double getValueForMilliGPerM3FromTempUnits(double value, String units) {
        double ret = value;
        switch (units.trim()) {
            case "kg m-3":
            case "Kg m-3":
                ret = value * 1E3 * 1E3;
                break;
            case "g m-3":
                ret = value * 1E3;
                break;
            case "ug m-3":
            case "\u03BCg m-3":
                ret = value / 1E3;
                break;
        }

        return ret;
    }

    /**
     * @param shape
     * @param counter
     * @return The next stage of the for loops or null if reach the end for all loops.
     */
    private static int[] nextShapeStage(final int[] shape, int[] counter) {
        for (int i : shape) {
            if (i < 1)
                return null;
        }
        for (int i = counter.length - 1; i >= 0; i--) {
            if (i >= counter.length - 1)
                counter[i]++;
            else {
                if (counter[i + 1] >= shape[i + 1]) {
                    counter[i]++;
                    counter[i + 1] = 0;
                }
            }
            if (i == 0 && counter[i] >= shape[i]) {
                return null;
            }
        }
        return counter;
    }

    private static void test() {
        int[] shape = {2, 1, 2, 3};
        int[] counter = new int[shape.length];
        Arrays.fill(counter, 0);
        for (int i = 0; i < Arrays.stream(shape).reduce(1, (x, y) -> (x+1) * y); i++) {
            System.out.println(Arrays.toString(counter));
            System.out.flush();
            counter = nextShapeStage(shape, counter);
            if (counter == null)
                break;
        }
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String str = "    -7.3280918  36.4298738  -24.540   15.514          0      15.440       2.180     -25.700     10.5000    -48.0000   49.1357   167.7     29.033     302.3      1   1";
        String[] tokens = str.trim().split("[\\t ,]+");
        System.out.println(tokens.length);
        

        try {
            double[] val = getMultiplierAndMillisOffsetFromTimeUnits("days since 00-01-00 00:00:00");
            System.out.println(val[0] + "    " + val[1]);
            val = getMultiplierAndMillisOffsetFromTimeUnits("seconds since 2013-07-04 00:00:00");
            System.out.println(val[0] + "    " + val[1]);
        }
        catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        try {
            Pattern timeStringPattern = Pattern.compile("^(\\w+?)\\ssince\\s(\\w+?)");
            String timeUnits = "days since 00-01-00 00:00:00";
            Matcher matcher = timeStringPattern.matcher(timeUnits);

            System.out.println(matcher.group(1));
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
        try {
            Date date = HFRadarVisualization.dateTimeFormaterUTC.parse("0001-01-01 00:00:00");
            System.out.println(date.getTime());
            
            Date ndate = new Date(date.getTime() + DateTimeUtil.DAYS_SINCE_YEAR_0_TILL_1970 * DateTimeUtil.DAY);
            System.out.println(ndate + "           " + ndate.getTime());
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        System.out.println("------ Test time load -----");
        String dateT1Str = "2013-07-04 00:00:00";
        Date dateT1 = HFRadarVisualization.dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());

        dateT1Str = "2013-7-4 0:0:0";
        dateT1 = HFRadarVisualization.dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());

        dateT1Str = "2013-7-4 13:3:4.32";
        dateT1 = HFRadarVisualization.dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());
        
        dateT1Str = "2013-7-4 13:3:4";
        dateT1 = HFRadarVisualization.dateTimeFormaterUTC.parse(dateT1Str);
        System.out.printf("%22s  ==  %s \t%s\n", dateT1Str, dateT1, dateT1.getTime());
        
        String dateT2Str = "2013-7-4 13:3:4.32";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4.";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));
        dateT2Str = "2013-7-4 13:3:4.262626";
        System.out.println(Arrays.toString(dateT2Str.split("\\.")));

        
        String dateT3Str = "seconds since 2013-7-4 13:3:4.32";
        double[] multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        Date dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        System.out.println("\nEvery resulting date should result in the same values!");
        dateT3Str = "seconds since 2013-7-4 13:3:4";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 +1:0";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 1";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 12:3:4 -100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 12:3:4 \u2212100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:4 +130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:04 +1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        System.out.println("\nOther tests!");

        dateT3Str = "seconds since 2013-7-4 14:33:04.67 +1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 1970-01-01T00:00:00Z";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2017-11-04T3:10:00.33+1:20";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4T14:33:04+1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%40s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        for (String sstFileName : new String[] {"../../Lab/MBARI/SST/erdATssta3day_9c53_2021_f180.nc",
                "plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/meteo_20130705.nc"}) {
            try {
                // String sstFileName = "../../Lab/MBARI/SST/erdATssta3day_9c53_2021_f180.nc";
                System.out.println("\n-----------------------------------");
                HashMap<?, ?>[] dataRet = processMeteo(sstFileName, new java.sql.Date(0));
                for (HashMap<?, ?> hashMap : dataRet) {
                    System.out.println("Size=" + hashMap.size());
//                for (Object nO : hashMap.keySet()) {
//                    Object dt = hashMap.get(nO);
//                    System.out.println(dt);
//                }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        test();
        
        for (String sstFileName : new String[] {"plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/waves_S_20130704.nc",
                "plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/waves_SW_20130704.nc"}) {
            try {
                System.out.println("\n-----------------------------------");
                HashMap<String, WavesDataPoint> hashMap = processWavesFile(sstFileName, new java.sql.Date(0));
                System.out.println("Size=" + hashMap.size());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }        

        for (String sstFileName : new String[] {"../../Lab/MBARI/SST/chlorophyll-erdMWchla8day_455c_156d_5bed.nc"}) {
            try {
                // String sstFileName = "../../Lab/MBARI/SST/erdATssta3day_9c53_2021_f180.nc";
                System.out.println("\n-----------------------------------");
                HashMap<String, ChlorophyllDataPoint> hashMap = processChlorophyll(sstFileName, new java.sql.Date(0));
                System.out.println("Size=" + hashMap.size());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
