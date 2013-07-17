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
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import ucar.ma2.ArrayFloat;
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

    public static final HashMap<?, ?>[] processMeteo(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        HashMap<String, SSTDataPoint> sstdp = new HashMap<>();
        HashMap<String, WindDataPoint> winddp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        try {

          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Variable latVar = dataFile.findVariable("lat");
          if (latVar == null) {
            System.out.println("Cant find Variable lat");
            return new HashMap[] { sstdp, winddp };
          }

          Variable lonVar = dataFile.findVariable("lon");
          if (lonVar == null) {
            System.out.println("Cant find Variable lon");
            return new HashMap[] { sstdp, winddp };
          }

          Variable timeVar = dataFile.findVariable("time");
          if (timeVar == null) {
            System.out.println("Cant find Variable time");
            return new HashMap[] { sstdp, winddp };
          }

          // Get the latitude and longitude Variables.
          Variable uVar = dataFile.findVariable("u");
          if (uVar == null) {
            System.out.println("Cant find Variable u");
            return new HashMap[] { sstdp, winddp };
          }

          // Get the latitude and longitude Variables.
          Variable vVar = dataFile.findVariable("v");
          if (vVar == null) {
            System.out.println("Cant find Variable v");
            return new HashMap[] { sstdp, winddp };
          }

          Variable sstVar = dataFile.findVariable("sst");
          if (sstVar == null) {
            System.out.println("Cant find Variable SST");
            return new HashMap[] { sstdp, winddp };
          }


          // Get the lat/lon data from the file.
          ArrayFloat.D1 latArray;
          ArrayFloat.D1 lonArray;
          ArrayFloat.D1 timeArray;
          ArrayFloat.D3 uArray;
          ArrayFloat.D3 vArray;
          ArrayFloat.D3 sstArray;

          latArray = (ArrayFloat.D1) latVar.read();
          lonArray = (ArrayFloat.D1) lonVar.read();
          timeArray = (ArrayFloat.D1) timeVar.read();
          uArray = (ArrayFloat.D3) uVar.read();
          vArray = (ArrayFloat.D3) vVar.read();
          sstArray = (ArrayFloat.D3) sstVar.read();
          
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
              System.out.println("Cant parse units for Variable time");
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
          
          int count = 0;
          for (int timeIdx = 0; timeIdx < shape[0]; timeIdx++) {
              float timeVal = timeArray.get(timeIdx);
              Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
              
              if (!ignoreDateLimitToLoad && dateValue.before(dateLimit)) {
                  continue;
              }
              
              for (int lonIdx = 0; lonIdx < shape[1]; lonIdx++) {
                  double lon = lonArray.getDouble(lonIdx);
                  for (int latIdx = 0; latIdx < shape[2]; latIdx++) {
                      double lat = latArray.getDouble(latIdx);

                      double u = uArray.get(timeIdx, lonIdx, latIdx);
                      double v = vArray.get(timeIdx, lonIdx, latIdx);
                      double sst = sstArray.get(timeIdx, lonIdx, latIdx);
                      if (!Double.isNaN(u) && !Double.isNaN(v)
                              && u != uFillValue && v != vFillValue) {
                          WindDataPoint dp = new WindDataPoint(lat, lon);
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
                          dp.setSst(sst + SSTDataPoint.KELVIN_TO_CELSIUS);
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
                      count++;
                  }
             }
          }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
          if (dataFile != null)
            try {
              dataFile.close();
            } catch (IOException ioe) {
              ioe.printStackTrace();
            }
        }
        System.out.println("*** SUCCESS reading file "+fileName);

        return new HashMap[] { sstdp, winddp };
    }

    public static final HashMap<String, WavesDataPoint> processWavesFile(String fileName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        HashMap<String, WavesDataPoint> wavesdp = new HashMap<>();

        NetcdfFile dataFile = null;
        
        try {

          dataFile = NetcdfFile.open(fileName, null);

          // Get the latitude and longitude Variables.
          Variable latVar = dataFile.findVariable("lat");
          if (latVar == null) {
            System.out.println("Cant find Variable lat");
            return wavesdp;
          }

          Variable lonVar = dataFile.findVariable("lon");
          if (lonVar == null) {
            System.out.println("Cant find Variable lon");
            return wavesdp;
          }

          Variable timeVar = dataFile.findVariable("time");
          if (timeVar == null) {
            System.out.println("Cant find Variable time");
            return wavesdp;
          }

          // Get the latitude and longitude Variables.
          Variable hsVar = dataFile.findVariable("hs");
          if (hsVar == null) {
            System.out.println("Cant find Variable hs");
            return wavesdp;
          }

          // Get the latitude and longitude Variables.
          Variable tpVar = dataFile.findVariable("tp");
          if (tpVar == null) {
            System.out.println("Cant find Variable tp");
            return wavesdp;
          }

          Variable pdirVar = dataFile.findVariable("pdir");
          if (pdirVar == null) {
            System.out.println("Cant find Variable pdir");
            return wavesdp;
          }


          // Get the lat/lon data from the file.
          ArrayFloat.D1 latArray;
          ArrayFloat.D1 lonArray;
          ArrayFloat.D1 timeArray;
          ArrayFloat.D3 hsArray;
          ArrayFloat.D3 tpArray;
          ArrayFloat.D3 pdirArray;

          latArray = (ArrayFloat.D1) latVar.read();
          lonArray = (ArrayFloat.D1) lonVar.read();
          timeArray = (ArrayFloat.D1) timeVar.read();
          hsArray = (ArrayFloat.D3) hsVar.read();
          tpArray = (ArrayFloat.D3) tpVar.read();
          pdirArray = (ArrayFloat.D3) pdirVar.read();
          
          int [] shape = hsVar.getShape();

          String timeUnits = "days since 00-01-00 00:00:00"; // "seconds since 2013-07-04 00:00:00"
          Attribute timeUnitsAtt = timeVar.findAttribute("units");
          if (timeUnitsAtt != null)
              timeUnits = (String) timeUnitsAtt.getValue(0);
          double[] multAndOffset = getMultiplierAndMillisOffsetFromTimeUnits(timeUnits);
          if (multAndOffset == null) {
              System.out.println("Cant parse units for Variable time");
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
          
          int count = 0;
          for (int timeIdx = 0; timeIdx < shape[0]; timeIdx++) {
              float timeVal = timeArray.get(timeIdx);
              Date dateValue = new Date((long) (timeVal * timeMultiplier + timeOffset));
              
              if (!ignoreDateLimitToLoad && dateValue.before(dateLimit)) {
                  continue;
              }
              
              for (int lonIdx = 0; lonIdx < shape[1]; lonIdx++) {
                  double lon = lonArray.getDouble(lonIdx);
                  for (int latIdx = 0; latIdx < shape[2]; latIdx++) {
                      double lat = latArray.getDouble(latIdx);

                      double hs = hsArray.get(timeIdx, lonIdx, latIdx);
                      double tp = tpArray.get(timeIdx, lonIdx, latIdx);
                      double pdir = pdirArray.get(timeIdx, lonIdx, latIdx);
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
                      count++;
                  }
             }
          }
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        } 
        finally {
          if (dataFile != null)
            try {
              dataFile.close();
            } catch (IOException ioe) {
              ioe.printStackTrace();
            }
        }
        System.out.println("*** SUCCESS reading file "+fileName);

        return wavesdp;
    }

    public static double[] getMultiplierAndMillisOffsetFromTimeUnits(String timeStr) {
        if ("days since 00-01-00 00:00:00".equalsIgnoreCase(timeStr)) {
            return new double[] { DateTimeUtil.DAY, - DateTimeUtil.DAYS_SINCE_YEAR_0_TILL_1970 * DateTimeUtil.DAY};
        }
        else {
            String[] tk = timeStr.trim().split("[ ]", 3);
            if (tk.length < 3) {
                return null;
            }
            else {
                double mult = 1;
                double off = 1;
                switch (tk[0].toLowerCase()) {
                    case "days":
                    case "day":
                        mult = DateTimeUtil.DAY;
                        break;
                    case "hours":
                    case "hour":
                        mult = DateTimeUtil.HOUR;
                        break;
                    case "minutes":
                    case "minute":
                        mult = DateTimeUtil.MINUTE;
                        break;
                    case "seconds":
                    case "second":
                        mult = DateTimeUtil.SECOND;
                        break;
                }
                
                try {
                    Date date = HFRadarVisualization.dateTimeFormaterUTC.parse(tk[2]);
                    off = date.getTime();
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }
                
                return new double[] { mult, off };
            }
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        String str = "    -7.3280918  36.4298738  -24.540   15.514          0      15.440       2.180     -25.700     10.5000    -48.0000   49.1357   167.7     29.033     302.3      1   1";
        String[] tokens = str.trim().split("[\\t ,]+");
        System.out.println(tokens.length);
        

        double[] val = getMultiplierAndMillisOffsetFromTimeUnits("days since 00-01-00 00:00:00");
        System.out.println(val[0] + "    " + val[1]);
        val = getMultiplierAndMillisOffsetFromTimeUnits("seconds since 2013-07-04 00:00:00");
        System.out.println(val[0] + "    " + val[1]);
        
        Pattern timeStringPattern = Pattern.compile("^(\\w+?)\\ssince\\s(\\w+?)");
        Date dateBase = new Date(-1900, 1-1, 1);
        String timeUnits = "days since 00-01-00 00:00:00";
        Matcher matcher = timeStringPattern.matcher(timeUnits);

        System.out.println(matcher.group(1));
    }

}
