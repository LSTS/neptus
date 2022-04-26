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
 * 6 de Jul de 2013
 */
package pt.lsts.neptus.plugins.envdisp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.envdisp.datapoints.ChlorophyllDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.HFRadarDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SLADataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.SSTDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WavesDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.WindDataPoint;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUnitsUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
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
                        dateStart = EnvironmentalDataVisualization.dateTimeFormaterSpacesUTC.parse(line,
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
                        hfdp.put(HFRadarDataPoint.getId(dpo), dpo);
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
          Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the u (north) wind velocity Variables.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "x_wind", "grid_eastward_wind", "u");
          @SuppressWarnings("unused")
          String xWindName = searchPair == null ? null : searchPair.first();
          Variable uVar = searchPair == null ? null : searchPair.second();

          // Get the v (east) wind velocity Variables.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "y_wind", "grid_northward_wind", "v");
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
          
          double[] multAndOffset = NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          String uUnits = "cm/s";
          Attribute uUnitsAtt = uVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
          if (uUnitsAtt != null)
              uUnits = (String) uUnitsAtt.getValue(0);
          String vUnits = "cm/s";
          Attribute vUnitsAtt = uVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
          if (vUnitsAtt != null)
              vUnits = (String) vUnitsAtt.getValue(0);

          double uFillValue = NetCDFUtils.findFillValue(uVar);
          Pair<Double, Double> uValidRange = NetCDFUtils.findValidRange(uVar);
          double vFillValue = NetCDFUtils.findFillValue(vVar);
          Pair<Double, Double> vValidRange = NetCDFUtils.findValidRange(vVar);
          
          int[] shape = uVar.getShape();
          int[] counter = new int[shape.length];
          Arrays.fill(counter, 0);
          String dimStr = uVar.getDimensionsString();
          Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName,
                  depthOrAltitudeName);

          do {
              Date dateValue = null;
              Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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

                if (NetCDFUtils.isValueValid(u, uFillValue, uValidRange)
                        && NetCDFUtils.isValueValid(v, vFillValue, vValidRange)) {

                  Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(uVar);
                  u = u * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                  scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);
                  v = v * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                  
                  u = u * NetCDFUnitsUtils.getMultiplierForCmPerSecondsFromSpeedUnits(uUnits);
                  v = v * NetCDFUnitsUtils.getMultiplierForCmPerSecondsFromSpeedUnits(vUnits);
                  double speedCmS = Math.sqrt(u * u + v * v) * 100;
                  double heading = Math.atan2(v, u);

                  HFRadarDataPoint dp = new HFRadarDataPoint(lat, lon);
                  dp.setSpeedCmS(speedCmS);
                  dp.setHeadingDegrees(Math.toDegrees(heading));
                  dp.setDateUTC(dateValue);

                  HFRadarDataPoint dpo = hfdp.get(HFRadarDataPoint.getId(dp));
                  if (dpo == null) {
                      dpo = dp.getACopyWithoutHistory();
                      hfdp.put(HFRadarDataPoint.getId(dpo), dpo);
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
          } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
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
          Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the u (north) wind velocity Variables.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "x_wind", "grid_eastward_wind", "u");
          @SuppressWarnings("unused")
          String xWindName = searchPair == null ? null : searchPair.first();
          Variable uVar = searchPair == null ? null : searchPair.second();

          // Get the v (east) wind velocity Variables.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "y_wind", "grid_northward_wind", "v");
          @SuppressWarnings("unused")
          String yWindName = searchPair == null ? null : searchPair.first();
          Variable vVar = searchPair == null ? null : searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "sea_surface_temperature", "sst");
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
          
          double[] multAndOffset = NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          // Let us process SST
          if (sstVar != null) {
              try {
                  String sstUnits = "K";
                  Attribute sstUnitsAtt = sstVar == null ? null : sstVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
                  if (sstUnitsAtt != null)
                      sstUnits = (String) sstUnitsAtt.getValue(0);

                  double sstFillValue = NetCDFUtils.findFillValue(sstVar);
                  Pair<Double, Double> sstValidRange = NetCDFUtils.findValidRange(sstVar);
                  
                  int[] shape = sstVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = sstVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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

                      if (NetCDFUtils.isValueValid(sst, sstFillValue, sstValidRange)) {
                          SSTDataPoint dp = new SSTDataPoint(lat, lon);
                          
                          Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(sstVar);
                          sst = sst * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          
                          sst = NetCDFUnitsUtils.getValueForDegreesCelciusFromTempUnits(sst, sstUnits);
                          dp.setSst(sst);
                          dp.setDateUTC(dateValue);

                          SSTDataPoint dpo = sstdp.get(SSTDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              sstdp.put(SSTDataPoint.getId(dpo), dpo);
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
                  } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
              }
              catch (Exception e) {
                  e.printStackTrace();
              }
          }

          // Let us process Wind
          if (uVar != null && vVar != null) {
              try {
                  String uUnits = "cm/s";
                  Attribute uUnitsAtt = uVar == null ? null : uVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
                  if (uUnitsAtt != null)
                      uUnits = (String) uUnitsAtt.getValue(0);
                  String vUnits = "cm/s";
                  Attribute vUnitsAtt = vVar == null ? null : vVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
                  if (vUnitsAtt != null)
                      vUnits = (String) vUnitsAtt.getValue(0);

                  double uFillValue = NetCDFUtils.findFillValue(uVar);
                  double vFillValue = NetCDFUtils.findFillValue(vVar);

                  int[] shape = uVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = uVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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
                          Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(uVar);
                          u = u * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);
                          v = v * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          
                          u = u * NetCDFUnitsUtils.getMultiplierForCmPerSecondsFromSpeedUnits(uUnits);
                          v = v * NetCDFUnitsUtils.getMultiplierForCmPerSecondsFromSpeedUnits(vUnits);
                          dp.setU(u);
                          dp.setV(v);
                          dp.setDateUTC(dateValue);

                          WindDataPoint dpo = winddp.get(WindDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              winddp.put(HFRadarDataPoint.getId(dpo), dpo);
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
                  } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
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
          Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the significant height Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true,
                  "sea_surface_wave_significant_height", "significant_height_of_wind_and_swell_waves", "hs");
          Variable hsVar = searchPair == null ? null : searchPair.second();

          // Get the peak period Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true,
                  "sea_surface_wave_period_at_variance_spectral_density_maximum", "tp");
          Variable tpVar = searchPair == null ? null : searchPair.second();

          // Get the peak direction Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "sea_surface_wave_to_direction",
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
          
          double[] multAndOffset = NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          double hsFillValue = NetCDFUtils.findFillValue(hsVar);
          Pair<Double, Double> hsValidRange = NetCDFUtils.findValidRange(hsVar);
          double tpFillValue = NetCDFUtils.findFillValue(tpVar);
          Pair<Double, Double> tpValidRange = NetCDFUtils.findValidRange(tpVar);
          double pdirFillValue = NetCDFUtils.findFillValue(pdirVar);
          Pair<Double, Double> pdirValidRange = NetCDFUtils.findValidRange(pdirVar);

          int[] shape = hsVar.getShape();
          int[] counter = new int[shape.length];
          Arrays.fill(counter, 0);
          String dimStr = hsVar.getDimensionsString();
          Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName,
                  depthOrAltitudeName);

          do {
              Date dateValue = null;
              Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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


              if (NetCDFUtils.isValueValid(hs, hsFillValue, hsValidRange)
                      && NetCDFUtils.isValueValid(tp, tpFillValue, tpValidRange)
                      && NetCDFUtils.isValueValid(pdir, pdirFillValue, pdirValidRange)) {

                  WavesDataPoint dp = new WavesDataPoint(lat, lon);

                  Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(hsVar);
                  hs = hs * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                  dp.setSignificantHeight(hs);

                  scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(tpVar);
                  tp = tp * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                  dp.setPeakPeriod(tp);

                  scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(pdirVar);
                  pdir = pdir * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                  dp.setPeakDirection(pdir);
                  
                  dp.setDateUTC(dateValue);

                  WavesDataPoint dpo = wavesdp.get(WavesDataPoint.getId(dp));
                  if (dpo == null) {
                      dpo = dp.getACopyWithoutHistory();
                      wavesdp.put(HFRadarDataPoint.getId(dpo), dpo);
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
          } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
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
    
    public static final HashMap<String, ChlorophyllDataPoint> processChlorophyllFile(String fileName, Date dateLimit) {
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
          Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "depth", "altitude");
          String depthOrAltitudeName = searchPair == null ? null : searchPair.first();
          Variable depthOrAltitudeVar = searchPair == null ? null : searchPair.second();

          // Get the chlorophyll Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true,
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
          
          double[] multAndOffset = NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          // Let us process
          if (chlorophyllVar != null) {
              try {
                  String chlorophyllUnits = "kg m-3";
                  Attribute chlorophyllUnitsAtt = chlorophyllVar == null ? null : chlorophyllVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
                  if (chlorophyllUnitsAtt != null)
                      chlorophyllUnits = (String) chlorophyllUnitsAtt.getValue(0);

                  double chlorophyllFillValue = NetCDFUtils.findFillValue(chlorophyllVar);
                  Pair<Double, Double> chlorophyllValidRange = NetCDFUtils.findValidRange(chlorophyllVar);
                  
                  int[] shape = chlorophyllVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = chlorophyllVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName,
                          depthOrAltitudeName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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

                      if (NetCDFUtils.isValueValid(chlorophyll, chlorophyllFillValue, chlorophyllValidRange)) {
                          ChlorophyllDataPoint dp = new ChlorophyllDataPoint(lat, lon);
                          
                          Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(chlorophyllVar);
                          chlorophyll = chlorophyll * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          
                          chlorophyll = NetCDFUnitsUtils.getValueForMilliGPerM3FromTempUnits(chlorophyll, chlorophyllUnits);
                          
                          dp.setChlorophyll(chlorophyll);
                          dp.setDateUTC(dateValue);

                          ChlorophyllDataPoint dpo = chlorophylldp.get(ChlorophyllDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              chlorophylldp.put(ChlorophyllDataPoint.getId(dpo), dpo);
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
                  } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
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

    public static final HashMap<String, SLADataPoint> processSLAFile(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing SLAnetCDF file '" + fileName + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, SLADataPoint> sladp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        Date fromDate = null;
        Date toDate = null;
        
        try {
          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
          String latName = searchPair.first();
          Variable latVar = searchPair.second(); 

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
          String lonName = searchPair.first();
          Variable lonVar = searchPair.second();

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          // Get the SLA Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "sea_surface_height_above_sea_level", "sla");
          @SuppressWarnings("unused")
          String slaName = searchPair == null ? null : searchPair.first();
          Variable slaVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array slaArray;    // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          slaArray = slaVar == null ? null : slaVar.read();
          
          double[] multAndOffset = NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName);
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          // Let us process
          if (slaVar != null) {
              try {
                  String slaUnits = "m";
                  Attribute slaUnitsAtt = slaVar == null ? null : slaVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
                  if (slaUnitsAtt != null)
                      slaUnits = (String) slaUnitsAtt.getValue(0);

                  double slaFillValue = NetCDFUtils.findFillValue(slaVar);
                  Pair<Double, Double> slaValidRange = NetCDFUtils.findValidRange(slaVar);
                  
                  int[] shape = slaVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = slaVar.getDimensionsString();
                  Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName);

                  do {
                      Date dateValue = null;
                      Date[] timeVals = NetCDFUtils.getTimeValues(timeArray, counter[0], timeMultiplier, timeOffset, fromDate,
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

                      Index index = slaArray.getIndex();
                      index.set(counter);

                      double sla = slaArray == null ? Double.NaN : slaArray.getDouble(index);

                      if (NetCDFUtils.isValueValid(sla, slaFillValue, slaValidRange)) {
                          SLADataPoint dp = new SLADataPoint(lat, lon);
                          
                          Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(slaVar);
                          sla = sla * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          
                          sla = NetCDFUnitsUtils.getValueForMetterFromTempUnits(sla, slaUnits);
                          
                          dp.setSLA(sla);
                          dp.setDateUTC(dateValue);

                          SLADataPoint dpo = sladp.get(SLADataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              sladp.put(SLADataPoint.getId(dpo), dpo);
                          }

                          ArrayList<SLADataPoint> lst = dpo.getHistoricalData();
                          boolean alreadyIn = false;
                          for (SLADataPoint tmpDp : lst) {
                              if (tmpDp.getDateUTC().equals(dp.getDateUTC())) {
                                  alreadyIn = true;
                                  break;
                              }
                          }
                          if (!alreadyIn) {
                              dpo.getHistoricalData().add(dp);
                          }
                      }
                  } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
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
            NeptusLog.pub().info("Ending processing SLA netCDF file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }

        return sladp;
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String str = "    -7.3280918  36.4298738  -24.540   15.514          0      15.440       2.180     -25.700     10.5000    -48.0000   49.1357   167.7     29.033     302.3      1   1";
        String[] tokens = str.trim().split("[\\t ,]+");
        System.out.println(tokens.length);
        

        for (String sstFileName : new String[] {"../../Lab/MBARI/SST/erdATssta3day_9c53_2021_f180.nc",
                "plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/meteo_20130705.nc"}) {
            try {
                // String sstFileName = "../../Lab/MBARI/SST/erdATssta3day_9c53_2021_f180.nc";
                System.out.println("\n-----------------------------------");
                HashMap<?, ?>[] dataRet = processMeteo(sstFileName, new Date(0));
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
        
        for (String sstFileName : new String[] {"plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/waves_S_20130704.nc",
                "plugins" + "-dev/envdisp/pt/lsts/neptus/plugins/envdisp/waves_SW_20130704.nc"}) {
            try {
                System.out.println("\n-----------------------------------");
                HashMap<String, WavesDataPoint> hashMap = processWavesFile(sstFileName, new Date(0));
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
                HashMap<String, ChlorophyllDataPoint> hashMap = processChlorophyllFile(sstFileName, new Date(0));
                System.out.println("Size=" + hashMap.size());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (String sstFileName : new String[] {"IHData/SLA/nrt_global_allsat_phy_l4_latest.nc.gz"}) {
            try {
                System.out.println("\n-----------------------------------");
                HashMap<String, SLADataPoint> hashMap = processSLAFile(sstFileName, new Date(0));
                System.out.println("Size=" + hashMap.size());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
