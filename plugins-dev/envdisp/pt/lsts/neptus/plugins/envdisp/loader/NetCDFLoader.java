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

import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StringUtils;
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

          searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "time");
          String timeName = searchPair == null ? null : searchPair.first();
          Variable timeVar = searchPair == null ? null : searchPair.second();

          // If varName is already depth, no need to use it
          searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, "depth");
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
          
          Info info = createInfoBase(vVar);
          
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

    /**
     * @param vVar
     * @return
     */
    public static Info createInfoBase(Variable vVar) {
        Info info = new Info();
        info.name = vVar.getShortName();
        Attribute vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_LONG_NAME);
        info.fullName = vAtt == null ? vVar.getFullName() : vAtt.getStringValue();
        vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
        info.standardName = vAtt == null ? "" : vAtt.getStringValue();
        info.unit = vVar.getUnitsString();
        vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_COMMENT);
        info.comment = vAtt == null ? "" : vAtt.getStringValue();
        return info;
    }

    /**
     * Deletes the netCDF unzipped file if any. The file passed has to be of the form "*.nc.gz".
     * 
     * @param fx
     */
    public static void deleteNetCDFUnzippedFile(File fx) {
        // Deleting the unzipped file
        if ("gz".equalsIgnoreCase(FileUtil.getFileExtension(fx))) {
            String absPath = fx.getAbsolutePath();
            absPath = absPath.replaceAll("\\.nc\\.gz$", ".nc");
            File unzipedFile = new File(absPath);
            if (unzipedFile.exists()) {
                try {
                    FileUtils.forceDelete(unzipedFile);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Shows a {@link JFileChooser} for the operator to choose a netCDF file.
     * 
     * @param parentWindow
     * @param recentFolder
     * @return The selected file or null if canceled.
     */
    public static <W extends Window> File showChooseANetCDFToOpen(W parentWindow, File recentFolder) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileView(new NeptusFileView());
        chooser.setCurrentDirectory(recentFolder);
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("netCDF files"), "nc", "nc.gz"));
        chooser.setApproveButtonText(I18n.text("Open file"));
        chooser.showOpenDialog(parentWindow);
        if (chooser.getSelectedFile() == null)
            return null;
        
        return chooser.getSelectedFile();
    }
    
    /**
     * Shows a GUI for the operator to choose a variable to use. That variable needs to use lat, lon.
     * 
     * @param fileName
     * @param dataFile
     * @param parentWindow
     * @return The variable chosen or null if cancelled.
     */
    public static <W extends Window> Variable showChooseVar(String fileName, NetcdfFile dataFile, W parentWindow) {
        Map<String, Variable> varToConsider = NetCDFUtils.getMultiDimensionalVariables(dataFile);
        
        Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "latitude", "lat");
        String latName = searchPair.first();
        searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, "longitude", "lon");
        String lonName = searchPair.first();
        // searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fx.getName(), false, "time");
        // String timeName = searchPair == null ? null : searchPair.first();
        if (varToConsider.isEmpty() || latName == null || lonName == null /*|| timeName == null*/) {
            GuiUtils.infoMessage(parentWindow, I18n.text("Error loading"), I18n.textf("Missing variables in data (%s)", "lat; lon"));
            return null;
        }
        
        // Removing the ones that don't have location info
        NetCDFUtils.filterForVarsWithDimentionsWith(varToConsider, latName, lonName/*, timeName*/);
        
        ArrayList<JLabel> choicesVarsLbl = new ArrayList<>();
        for (String vName : varToConsider.keySet()) {
            Variable var = varToConsider.get(vName);
            StringBuilder sb = new StringBuilder("<html><b>");
            Info info = NetCDFLoader.createInfoBase(var);
            sb.append(vName);
            sb.append(" :: ");
            sb.append(info.fullName);
            sb.append("</b><br/>");
            sb.append("std='");
            sb.append(info.standardName);
            sb.append("'");
            sb.append("<br/>");
            sb.append("unit='");
            sb.append(info.unit);
            sb.append("'");
            sb.append("<br/>");
            sb.append("comment='");
            String cmt = StringUtils.wrapEveryNChars(info.comment, (short) 60);
            cmt = cmt.replaceAll("\n", "<br/>");
            sb.append(cmt);
            sb.append("'");
            sb.append("<html>");

            @SuppressWarnings("serial")
            JLabel l = new JLabel(vName) {
                @Override
                public String toString() {
                    return sb.toString();
                }
            };
            choicesVarsLbl.add(l);
        }
        if (choicesVarsLbl.isEmpty()) {
            GuiUtils.infoMessage(parentWindow, I18n.text("Info"),
                    I18n.textf("No valid variables in data with dimentions (%s)", "<time>; lat; lon; <depth>"));
            return null;
        }
        Object choiceOpt = JOptionPane.showInputDialog(parentWindow, I18n.text("Choose one of the vars"),
                I18n.text("Chooser"), JOptionPane.QUESTION_MESSAGE, null,
                choicesVarsLbl.toArray(new JLabel[choicesVarsLbl.size()]), 0);
    
        return choiceOpt == null ? null : varToConsider.get(((JLabel) choiceOpt).getText());
    }
    
    /**
     * This method will load a varName into a {@link GenericNetCDFDataPainter} and fill in the properties of it.
     * 
     * This runs on a separated thread returning a {@link Future}.
     *
     * @param filePath
     * @param dataFile
     * @param varName
     * @param plotUniqueId
     * @return
     */
    public static Future<GenericNetCDFDataPainter> loadNetCDFPainterFor(String filePath, NetcdfFile dataFile, String varName, long plotUniqueId) {
        FutureTask<GenericNetCDFDataPainter> fTask = new FutureTask<>(() -> {
            try {
                HashMap<String, GenericDataPoint> dataPoints = NetCDFLoader.processFileForVariable(dataFile, varName, null);
                GenericNetCDFDataPainter gDataViz = new GenericNetCDFDataPainter(plotUniqueId, dataPoints);
                gDataViz.setNetCDFFile(filePath);
                return gDataViz;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage(), e);
                return null;
            }
        });
        Thread t = new Thread(() -> fTask.run(), NetCDFLoader.class.getSimpleName() + ":: loadNetCDFPainterFor " + varName);
        t.setDaemon(true);
        t.start();
        return fTask;
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
