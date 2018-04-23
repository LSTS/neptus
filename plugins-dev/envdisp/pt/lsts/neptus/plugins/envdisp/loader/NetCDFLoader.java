/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 21, 2018
 */
package pt.lsts.neptus.plugins.envdisp.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.util.AngleUtils;
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
public class NetCDFLoader {
    
    public static final String NETCDF_FILE_PATTERN = ".\\.nc(\\.gz)?$";
    
    /**
     * @param dataFile
     * @param varName
     * @param dateLimit If null no filter for time is done
     * @return
     */
    public static final HashMap<String, GenericDataPoint> processFileForVariable(NetcdfFile dataFile, String varName, Date dateLimit) {
        boolean ignoreDateLimitToLoad = false;
        if (dateLimit == null)
            ignoreDateLimitToLoad = true;
        
        String fileName = dataFile.getLocation();
        
        NeptusLog.pub().info("Starting processing " + varName + " file '" + dataFile.getLocation() + "'."
                + (ignoreDateLimitToLoad ? " ignoring dateTime limit" : " Accepting data after " + dateLimit + "."));

        HashMap<String, GenericDataPoint> dataDp = new HashMap<>();

        Date fromDate = null;
        Date toDate = null;
        
        try {
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

          // If varName is already depth, no need to use it
          searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "depth");
          String depthName = searchPair == null ? null : searchPair.first();;
          Variable depthVar = searchPair == null ? null : searchPair.second(); 

          // Get the Variable.
          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, varName);
          @SuppressWarnings("unused")
          String vName = searchPair == null ? null : searchPair.first();
          Variable vVar = searchPair == null ? null : searchPair.second();

          // Get the lat/lon data from the file.
          Array latArray;  // ArrayFloat.D1
          Array lonArray;  // ArrayFloat.D1
          Array timeArray; //ArrayFloat.D1
          Array depthArray; //ArrayFloat.D1
          Array vArray;    // ArrayFloat.D?

          latArray = latVar.read();
          lonArray = lonVar.read();
          timeArray = timeVar != null ? timeVar.read() : null;
          depthArray = depthVar != null ? depthVar.read() : null;
          vArray = vVar == null ? null : vVar.read();
          
          double[] multAndOffset = timeVar != null ? NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName) : null;
          double timeMultiplier = timeVar != null ? multAndOffset[0] : 1;
          double timeOffset = timeVar != null ? multAndOffset[1] : 0;
          
          Info info = new Info();
          info.name = vVar.getShortName();
          Attribute vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_LONG_NAME);
          info.fullName = vAtt == null ? vVar.getFullName() : vAtt.getStringValue();
          vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
          info.standardName = vAtt == null ? "" : vAtt.getStringValue();
          info.unit = vVar.getUnitsString();
          vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_COMMENT);
          info.comment = vAtt == null ? "" : vAtt.getStringValue();
          
          // Let us process
          if (vVar != null) {
              try {
//                  String vUnits = "";
//                  Attribute vUnitsAtt = vVar == null ? null : vVar.findAttribute(NetCDFUtils.NETCDF_ATT_UNITS);
//                  if (vUnitsAtt != null)
//                      vUnits = (String) vUnitsAtt.getValue(0);

                  double slaFillValue = NetCDFUtils.findFillValue(vVar);
                  Pair<Double, Double> slaValidRange = NetCDFUtils.findValidRange(vVar);
                  
                  int[] shape = vVar.getShape();
                  int[] counter = new int[shape.length];
                  Arrays.fill(counter, 0);
                  String dimStr = vVar.getDimensionsString();
                  
                  // The null values are ignored
                  Map<String, Integer> collumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr, timeName, latName, lonName, depthName);

                  int timeIndexPos = collumsIndexMap.containsKey(timeName) ? collumsIndexMap.get(timeName) : -1;
                  int latIndexPos = collumsIndexMap.containsKey(latName) ? collumsIndexMap.get(latName) : -1;
                  int lonIndexPos = collumsIndexMap.containsKey(lonName) ? collumsIndexMap.get(lonName) : -1;
                  int depthIndexPos = collumsIndexMap.containsKey(depthName) ? collumsIndexMap.get(depthName) : -1;
                  do {
                      Date dateValue = null;
                      Date[] timeVals = timeIndexPos >= 0 ? NetCDFUtils.getTimeValues(timeArray, counter[timeIndexPos], timeMultiplier, timeOffset, fromDate,
                              toDate, ignoreDateLimitToLoad, dateLimit) : null;
                      if (timeVals == null) {
                          continue; // Check if we bail if no time exists
                      }
                      else {
                          dateValue = timeVals[0];
                          fromDate = timeVals[1];
                          toDate = timeVals[2];
                      }

                      double lat = AngleUtils.nomalizeAngleDegrees180(latArray.getDouble(counter[latIndexPos]));
                      double lon = AngleUtils.nomalizeAngleDegrees180(lonArray.getDouble(counter[lonIndexPos]));

                      double depth = depthIndexPos >= 0 ? AngleUtils.nomalizeAngleDegrees180(depthArray.getDouble(counter[depthIndexPos])) : Double.NaN;

                      Index index = vArray.getIndex();
                      index.set(counter);

                      double v = vArray == null ? Double.NaN : vArray.getDouble(index);

                      if (NetCDFUtils.isValueValid(v, slaFillValue, slaValidRange)) {
                          GenericDataPoint dp = new GenericDataPoint(lat, lon);
                          dp.setInfo(info);
                          dp.setDepth(depth); // See better this!!
                          
                          Pair<Double, Double> scaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);
                          v = v * scaleFactorAndAddOffset.first() + scaleFactorAndAddOffset.second();
                          
                          // Not doing nothing with units, just using what it is
                          // sla = NetCDFUnitsUtils.getValueForMetterFromTempUnits(sla, slaUnits);
                          
                          dp.setValue(v);
                          if (dp.getInfo().minVal == Double.MIN_VALUE || v < dp.getInfo().minVal)
                              dp.getInfo().minVal = v;
                          if (dp.getInfo().maxVal == Double.MAX_VALUE || v > dp.getInfo().maxVal)
                              dp.getInfo().maxVal = v;
                          dp.setDateUTC(dateValue);
                          if (dp.getInfo().minDate.getTime() == 0 || dp.getInfo().minDate.after(dateValue))
                              dp.getInfo().minDate = dateValue;
                          if (dp.getInfo().maxDate.getTime() == 0 || dp.getInfo().maxDate.before(dateValue))
                              dp.getInfo().maxDate = dateValue;

                          GenericDataPoint dpo = dataDp.get(GenericDataPoint.getId(dp));
                          if (dpo == null) {
                              dpo = dp.getACopyWithoutHistory();
                              dataDp.put(GenericDataPoint.getId(dp), dp);
                          }

                          ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
                          boolean alreadyIn = false;
                          for (GenericDataPoint tmpDp : lst) {
                              if (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth()) { // Check also depth and see if no time
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
            NeptusLog.pub().info("Ending processing " + varName + " netCDF file '" + fileName
                    + "'. Reading from date '" + fromDate + "' till '" + toDate + "'.");
        }

        return dataDp;
    }

    public static void main(String[] args) throws Exception {
        NetcdfFile dataFile = null;
        
        String fileName = "../nrt_global_allsat_phy_l4_latest.nc.gz";
        dataFile = NetcdfFile.open(fileName, null);
        
        List<Variable> vars = dataFile.getVariables();
        for (Variable v : vars) {
            System.out.println(String.format("'%s'  '%s' '%s' '%d' '%s'  '%s'", v.getShortName(), v.getFullName(), v.getDescription(), v.getDimensions().size(),
                    v.getDimensionsString(), v.getRanges()));
        }
        System.out.println();
        
        HashMap<String, GenericDataPoint> data = processFileForVariable(dataFile, "sla", null);
        // data.keySet().stream().forEachOrdered(k -> System.out.println(data.get(k)));
        String[] keys = data.keySet().toArray(new String[0]);
        for (int i = 0; i < Math.min(10, data.size()); i++) {
            System.out.println(keys[i] + " -> " + data.get(keys[i]));
        }
    }
}
