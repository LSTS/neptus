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
 \* Alternatively, this file may be used under the terms of the Modified EUPL,
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.util.DateTimeUtil;
import ucar.ma2.Array;
import ucar.ma2.Index3D;
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
                        dateStart = HFRadarVisualization.dateTimeFormaterSpacesUTC.parse(line, new ParsePosition(tsStr.length() -1 ));
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
        
        NeptusLog.pub().info("Starting processing Currents netCDF file '" + fileName + "'." + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));
        
        HashMap<String, HFRadarDataPoint> hfdp = new HashMap<>();
        NetcdfFile dataFile = null;

        Date fromDate = null;
        Date toDate = null;
        
        try {

          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Variable latVar = dataFile.findVariable("lat");
          if (latVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lat' for netCDF file '" + fileName + "'.");
              return hfdp;
          }

          Variable lonVar = dataFile.findVariable("lon");
          if (lonVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lon' for netCDF file '" + fileName + "'.");
              return hfdp;
          }

          Variable timeVar = dataFile.findVariable("time");
          if (timeVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'time' for netCDF file '" + fileName + "'.");
              return hfdp;
          }

          // Get the latitude and longitude Variables.
          Variable uVar = dataFile.findVariable("u");
          if (uVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'u' for netCDF file '" + fileName + "'.");
              return hfdp;
          }

          // Get the latitude and longitude Variables.
          Variable vVar = dataFile.findVariable("v");
          if (vVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'v' for netCDF file '" + fileName + "'.");
              return hfdp;
          }

//          // see if float or double
//          String timeType = "float";
//          Attribute timeTypeAtt = timeVar.findAttribute("dataType");
//          if (timeTypeAtt != null)
//              timeType = (String) timeTypeAtt.getValue(0);

          
          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; // ArrayFloat.D1
          Array uArray;    // ArrayFloat.D3
          Array vArray;    // ArrayFloat.D3

//          latArray = (ArrayFloat.D1) latVar.read();
//          lonArray = (ArrayFloat.D1) lonVar.read();
//          timeArray = (ArrayFloat.D1) timeVar.read();
//          uArray = (ArrayFloat.D3) uVar.read();
//          vArray = (ArrayFloat.D3) vVar.read();
          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          uArray = uVar.read();
          vArray = vVar.read();
          
          int [] shape = uVar.getShape();

          String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
          Attribute timeUnitsAtt = timeVar.findAttribute("units");
          if (timeUnitsAtt != null)
              timeUnits = (String) timeUnitsAtt.getValue(0);
          double[] multAndOffset = getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
          if (multAndOffset == null) {
              NeptusLog.pub().error("Aborting. Can't parse units for variable 'time' (was '" + timeUnits + "') for netCDF file '" + fileName + "'.");
              return hfdp;
          }
          
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
          double uFillValue = Double.NaN;
          double vFillValue = Double.NaN;
          Attribute uFillValueAtt = uVar.findAttribute("_FillValue");
          if (uFillValueAtt != null) {
              try {
                  uFillValue = (double) uFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  uFillValue = Double.parseDouble((String) uFillValueAtt.getValue(0));
              }
          }
          Attribute vFillValueAtt = vVar.findAttribute("_FillValue");
          if (vFillValueAtt != null) {
              try {
                  vFillValue = (double) vFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  vFillValue = Double.parseDouble((String) vFillValueAtt.getValue(0));
              }
          }
          
          // ucar.ma2.MAMath.
          // ucar.ma2.DataType.

          String uUnits = "cm/s";
          Attribute uUnitsAtt = uVar.findAttribute("units");
          if (uUnitsAtt != null)
              uUnits = (String) uUnitsAtt.getValue(0);
          String vUnits = "cm/s";
          Attribute vUnitsAtt = uVar.findAttribute("units");
          if (vUnitsAtt != null)
              vUnits = (String) vUnitsAtt.getValue(0);

//          String uDimentions = "time,lat,lon";
//          Attribute uDimentionsAtt = uVar.findAttribute("dimentions");
//          if (uDimentionsAtt != null)
//              uDimentions = (String) uDimentionsAtt.getValue(0);

          Pair<Integer, Integer> latLonIndexOrder = getLatLonIndexOrder(uVar.getDimensionsString());
          
            // int count = 0;
          for (int timeIdx = 0; timeIdx < shape[0]; timeIdx++) {
              double timeVal = timeArray.getDouble(timeIdx); // get(timeIdx);
              Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
              
              if (!ignoreDateLimitToLoad && dateValue.before(dateLimit)) {
                  continue;
              }
              
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
              
              for (int latOrLonFirstIdx = 0; latOrLonFirstIdx < shape[1]; latOrLonFirstIdx++) {
                  for (int latOrLonSecondIdx = 0; latOrLonSecondIdx < shape[2]; latOrLonSecondIdx++) {
                      double lat = latArray.getDouble(latLonIndexOrder.first() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);
                      double lon = lonArray.getDouble(latLonIndexOrder.second() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);

                      Index3D idx3d = (Index3D) uArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double u = uArray.getDouble(idx3d); // (timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      idx3d = (Index3D) vArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double v = vArray.getDouble(idx3d); // (timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
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
                        // count++;
                  }
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
            NeptusLog.pub().info("Ending processing Currents netCDF file '" + fileName + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }
        
        return hfdp;
    }

    public static final HashMap<?, ?>[] processMeteo(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        NeptusLog.pub().info("Starting processing Meteo (wind and SST) netCDF file '" + fileName + "'." + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, SSTDataPoint> sstdp = new HashMap<>();
        HashMap<String, WindDataPoint> winddp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        Date fromDate = null;
        Date toDate = null;
        
        try {

          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Variable latVar = dataFile.findVariable("lat");
          if (latVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lat' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }

          Variable lonVar = dataFile.findVariable("lon");
          if (lonVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lon' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }

          Variable timeVar = dataFile.findVariable("time");
          if (timeVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'time' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }

          // Get the u (north) wind velocity Variables.
          Variable uVar = dataFile.findVariable("u");
          if (uVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'u' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }

          // Get the v (east) wind velocity Variables.
          Variable vVar = dataFile.findVariable("v");
          if (vVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'v' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }

          Variable sstVar = dataFile.findVariable("sst");
          if (sstVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'sst' for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }


          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array uArray;    // ArrayFloat.D3
          Array vArray;    // ArrayFloat.D3
          Array sstArray;  // ArrayFloat.D3

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          uArray = uVar.read();
          vArray = vVar.read();
          sstArray = sstVar.read();
          
          int [] shape = uVar.getShape();
//          int recLen = shape[0]; // number of times
//          int[] origin = new int[4];
////        shape[0] = 1; // only one rec per read
//          System.out.println("" + recLen);
//          System.out.println(timeArray.getSize());

          
//          Date dateBase = new Date(-1900, 1-1, 1);
//          System.out.println(dateBase + "   " + (dateBase.getTime() / 24. / 60. / 60. / 1000.));
//          Date dateF = new Date((long) (dateBase.getTime() + 735389. * 24. * 60. * 60. * 1000.));
//          System.out.println("    " + (735389. * 24. * 60. * 60. * 1000) + "\n    " +  ((new Date(2013-1900, 6-1, 3).getTime()-dateBase.getTime())));
//          System.out.println(dateF);
//
//          int DAYS_FROM_YEAR_0_TILL_1970 = 719530;
//          
//          System.out.println(new Date((long) ((735389. - DAYS_FROM_YEAR_0_TILL_1970) * 24. * 60. * 60. * 1000.)));

          String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
          Attribute timeUnitsAtt = timeVar.findAttribute("units");
          if (timeUnitsAtt != null)
              timeUnits = (String) timeUnitsAtt.getValue(0);
          double[] multAndOffset = getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
          if (multAndOffset == null) {
              NeptusLog.pub().error("Aborting. Can't parse units for variable 'time' (was '" + timeUnits + "') for netCDF file '" + fileName + "'.");
              return new HashMap[] { sstdp, winddp };
          }
              
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
//          SimpleDateFormat dateFormaterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//          dateFormaterXMLNoMillisUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
          
          double uFillValue = Double.NaN;
          double vFillValue = Double.NaN;
          double sstFillValue = Double.NaN;
          Attribute uFillValueAtt = uVar.findAttribute("_FillValue");
          if (uFillValueAtt != null) {
              try {
                  uFillValue = (double) uFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  uFillValue = Double.parseDouble((String) uFillValueAtt.getValue(0));
              }
          }
          Attribute vFillValueAtt = vVar.findAttribute("_FillValue");
          if (vFillValueAtt != null) {
              try {
                  vFillValue = (double) vFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  vFillValue = Double.parseDouble((String) vFillValueAtt.getValue(0));
              }
          }
          Attribute sstFillValueAtt = sstVar.findAttribute("_FillValue");
          if (sstFillValueAtt != null) {
              try {
                  sstFillValue = (double) sstFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  sstFillValue = Double.parseDouble((String) sstFillValueAtt.getValue(0));
              }
          }
          
          String uUnits = "cm/s";
          Attribute uUnitsAtt = uVar.findAttribute("units");
          if (uUnitsAtt != null)
              uUnits = (String) uUnitsAtt.getValue(0);
          String vUnits = "cm/s";
          Attribute vUnitsAtt = uVar.findAttribute("units");
          if (vUnitsAtt != null)
              vUnits = (String) vUnitsAtt.getValue(0);
          String sstUnits = "K";
          Attribute sstUnitsAtt = sstVar.findAttribute("units");
          if (sstUnitsAtt != null)
              sstUnits = (String) sstUnitsAtt.getValue(0);
          
//          String uDimentions = "time,lat,lon";
//          Attribute uDimentionsAtt = uVar.findAttribute("dimentions");
//          if (uDimentionsAtt != null)
//              uDimentions = (String) uDimentionsAtt.getValue(0);

          Pair<Integer, Integer> latLonIndexOrder = getLatLonIndexOrder(uVar.getDimensionsString());

            // int count = 0;
          for (int timeIdx = 0; timeIdx < shape[0]; timeIdx++) {
              double timeVal = timeArray.getDouble(timeIdx); // get(timeIdx);
              Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
              
              if (!ignoreDateLimitToLoad && dateValue.before(dateLimit)) {
                  continue;
              }
              
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

              for (int latOrLonFirstIdx = 0; latOrLonFirstIdx < shape[1]; latOrLonFirstIdx++) {
                  for (int latOrLonSecondIdx = 0; latOrLonSecondIdx < shape[2]; latOrLonSecondIdx++) {
                      double lat = latArray.getDouble(latLonIndexOrder.first() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);
                      double lon = lonArray.getDouble(latLonIndexOrder.second() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);

                      Index3D idx3d = (Index3D) uArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double u = uArray.getDouble(idx3d); // (timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      idx3d = (Index3D) vArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double v = vArray.getDouble(idx3d); // (timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      idx3d = (Index3D) sstArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double sst = sstArray.getDouble(idx3d);
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
                      if (!Double.isNaN(sst) && sst != sstFillValue) {
                          SSTDataPoint dp = new SSTDataPoint(lat, lon);
                          sst = getValueForDegreesCelciusFromTempUnits(sst, sstUnits);
                          dp.setSst(sst); //) + SSTDataPoint.KELVIN_TO_CELSIUS);
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
                        // count++;
                  }
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
            NeptusLog.pub().info("Ending processing Meteo (wind and SST) netCDF file '" + fileName + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
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
          Variable latVar = dataFile.findVariable("lat");
          if (latVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lat' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }

          Variable lonVar = dataFile.findVariable("lon");
          if (lonVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'lon' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }

          Variable timeVar = dataFile.findVariable("time");
          if (timeVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'time' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }

          // Get the significant height Variable.
          Variable hsVar = dataFile.findVariable("hs");
          if (hsVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'hs' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }

          // Get the peak period Variable.
          Variable tpVar = dataFile.findVariable("tp");
          if (tpVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'tp' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }

          // Get the peak direction Variable.
          Variable pdirVar = dataFile.findVariable("pdir");
          if (pdirVar == null) {
              NeptusLog.pub().error("Aborting. Can't find variable 'pdir' for netCDF file '" + fileName + "'.");
              return wavesdp;
          }


          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; // ArrayFloat.D1
          Array hsArray;   // ArrayFloat.D3
          Array tpArray;   // ArrayFloat.D3
          Array pdirArray; // ArrayFloat.D3

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar.read();
          hsArray = hsVar.read();
          tpArray = tpVar.read();
          pdirArray = pdirVar.read();
          
          int [] shape = hsVar.getShape();

          String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
          Attribute timeUnitsAtt = timeVar.findAttribute("units");
          if (timeUnitsAtt != null)
              timeUnits = (String) timeUnitsAtt.getValue(0);
          double[] multAndOffset = getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
          if (multAndOffset == null) {
              NeptusLog.pub().error("Aborting. Can't parse units for variable 'time' (was '" + timeUnits + "') for netCDF file '" + fileName + "'.");
              return wavesdp;
          }
              
          double timeMultiplier = multAndOffset[0];
          double timeOffset = multAndOffset[1];
          
//          SimpleDateFormat dateFormaterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//          dateFormaterXMLNoMillisUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
          
          double hsFillValue = Double.NaN;
          double tpFillValue = Double.NaN;
          double pdirFillValue = Double.NaN;
          Attribute uFillValueAtt = hsVar.findAttribute("_FillValue");
          if (uFillValueAtt != null) {
              try {
                  hsFillValue = (double) uFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  hsFillValue = Double.parseDouble((String) uFillValueAtt.getValue(0));
              }
          }
          Attribute vFillValueAtt = tpVar.findAttribute("_FillValue");
          if (vFillValueAtt != null) {
              try {
                  tpFillValue = (double) vFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  tpFillValue = Double.parseDouble((String) vFillValueAtt.getValue(0));
              }
          }
          Attribute sstFillValueAtt = pdirVar.findAttribute("_FillValue");
          if (sstFillValueAtt != null) {
              try {
                  pdirFillValue = (double) sstFillValueAtt.getValue(0);
              } 
              catch (ClassCastException e) {
                  // e.printStackTrace();
                  pdirFillValue = Double.parseDouble((String) sstFillValueAtt.getValue(0));
              }
          }
          
//          String uUnits = "cm/s";
//          Attribute uUnitsAtt = hsVar.findAttribute("units");
//          if (uUnitsAtt != null)
//              uUnits = (String) uUnitsAtt.getValue(0);
//          String vUnits = "cm/s";
//          Attribute vUnitsAtt = hsVar.findAttribute("units");
//          if (vUnitsAtt != null)
//              vUnits = (String) vUnitsAtt.getValue(0);
          
//          String uDimentions = "time,lat,lon";
//          Attribute uDimentionsAtt = hsVar.findAttribute("dimensions");
//          if (uDimentionsAtt != null)
//              uDimentions = (String) uDimentionsAtt.getValue(0);

          Pair<Integer, Integer> latLonIndexOrder = getLatLonIndexOrder(hsVar.getDimensionsString());

            // int count = 0;
          for (int timeIdx = 0; timeIdx < shape[0]; timeIdx++) {
              double timeVal = timeArray.getDouble(timeIdx); // get(timeIdx);
              Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
              
              if (!ignoreDateLimitToLoad && dateValue.before(dateLimit)) {
                  continue;
              }
              
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
              
              for (int latOrLonFirstIdx = 0; latOrLonFirstIdx < shape[1]; latOrLonFirstIdx++) {
                  for (int latOrLonSecondIdx = 0; latOrLonSecondIdx < shape[2]; latOrLonSecondIdx++) {
                      double lat = latArray.getDouble(latLonIndexOrder.first() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);
                      double lon = lonArray.getDouble(latLonIndexOrder.second() == 1 ? latOrLonFirstIdx : latOrLonSecondIdx);

                      Index3D idx3d = (Index3D) hsArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double hs = hsArray.getDouble(idx3d);
                      idx3d = (Index3D) tpArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double tp = tpArray.getDouble(idx3d);
                      idx3d = (Index3D) pdirArray.getIndex();
                      idx3d.set(timeIdx, latOrLonFirstIdx, latOrLonSecondIdx);
                      double pdir = pdirArray.getDouble(idx3d);
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
                        // count++;
                  }
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
            NeptusLog.pub().info("Ending processing Waves netCDF file '" + fileName + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }

        return wavesdp;
    }

    /**
     * @param uDimentions
     * @returns
     */
    private static Pair<Integer, Integer> getLatLonIndexOrder(String uDimentions) {
        Pair<Integer, Integer> ret = null;
        if (uDimentions == null || uDimentions.length() == 0)
            return ret;
        
        String[] tk = uDimentions.split("[, \t]");
        if (tk.length < 3)
            return ret;
        
        int latIdx= 1;
        int lonIdx= 2;
        if ("lon".equalsIgnoreCase(tk[1].trim())) {
            latIdx= 2;
            lonIdx= 1;
        }
        ret = new Pair<Integer, Integer>(latIdx, lonIdx);
        return ret;
    }

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
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        System.out.println("\nEvery resulting date should result in the same values!");
        dateT3Str = "seconds since 2013-7-4 13:3:4";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 +1:0";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 1";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:3:4 100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 12:3:4 -100";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 11:33:4 -1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:4 +130";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

        dateT3Str = "seconds since 2013-7-4 14:33:04 +1:30";
        multPlusOffset = getMultiplierAndMillisOffsetFromTimeUnits(dateT3Str);
        dateT3 = new Date((long) (0 * multPlusOffset[0] + multPlusOffset[1]));
        System.out.printf("%37s == %22s \t mult=%f off=%f\n", dateT3Str, dateT3, multPlusOffset[0], multPlusOffset[1]);

    }
}
