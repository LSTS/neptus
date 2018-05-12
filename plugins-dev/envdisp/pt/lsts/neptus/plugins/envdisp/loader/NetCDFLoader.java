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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Type;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info.ScalarOrLogPreference;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUnitsUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
public class NetCDFLoader {
    
    public static final String NETCDF_FILE_PATTERN = ".+\\.nc(\\.gz)?$";
    
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
            // Get the Variable.
            Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, varName);
            String vName = searchPair == null ? null : searchPair.first();
            Variable vVar = searchPair == null ? null : searchPair.second();

            if (vName == null || vVar == null) {
                NeptusLog.pub().debug(String.format("Variable %s not found in data fiel %s", varName, fileName));
                return null;
            }

            String dimStr = vVar.getDimensionsString();
            List<Dimension> dimDim = vVar.getDimensions();

            @SuppressWarnings("unused")
            String latName = null;
            Variable latVar = null;
            @SuppressWarnings("unused")
            String lonName = null;
            Variable lonVar = null;
            @SuppressWarnings("unused")
            String timeName = null;
            Variable timeVar = null;
            @SuppressWarnings("unused")
            String depthName = null;
            Variable depthVar = null;

            // Find the vars for dims
            List<String> dimDimStrLst = Arrays.asList(dimDim.stream().flatMap(d -> Stream.of(d.getShortName())).toArray(String[]::new));
            Map<String, Variable> dimVars = new LinkedHashMap<>();
            for (Dimension d : dimDim) {
                Pair<String, Variable> sPair = NetCDFUtils.findVariableFor(dataFile, fileName, false, dimDimStrLst, d.getShortName());
                if (sPair == null || sPair.second() == null) {
                    dimVars.put(d.getShortName(), null);
                }
                else {
                    dimVars.put(d.getShortName(), sPair.second());
                    Variable v = sPair.second();
                    Attribute vSNAtt = v.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
                    if (vSNAtt != null) {
                        switch (vSNAtt.getStringValue().toLowerCase()) {
                            case "latitude":
                                latName = v.getShortName();
                                latVar = v;
                                continue;
                            case "longitude":
                                lonName = v.getShortName();
                                lonVar = v;
                                continue;
                            case "time":
                            case "ocean_time":
                                timeName = v.getShortName();
                                timeVar = v;
                                continue;
                            case "depth":
                                depthName = v.getShortName();
                                depthVar = v;
                                continue;
                            default:
                                break;
                        }
                    }
                    switch (v.getShortName().toLowerCase()) {
                        case "latitude":
                        case "lat":
                            latName = v.getShortName();
                            latVar = v;
                            continue;
                        case "longitude":
                        case "lon":
                            lonName = v.getShortName();
                            lonVar = v;
                            continue;
                        case "time":
                        case "ocean_time":
                            timeName = v.getShortName();
                            timeVar = v;
                            continue;
                        case "depth":
                            depthName = v.getShortName();
                            depthVar = v;
                            continue;
                        default:
                            break;
                    }
                }
            }

            // Get the latitude and longitude Variables.
            Group navDataGroup = dataFile.findGroup("navigation_data");
            if (latVar == null) {
                searchPair = null;
                if (navDataGroup != null) {
                    Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "latitude", "lat");
                    if (varL == null)
                        varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                                NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "latitude", "lat");
                    if (varL != null)
                        searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
                }
                if (searchPair == null)
                    searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, dimDimStrLst,
                            "latitude", "lat");
                latName = searchPair.first();
                latVar = searchPair.second(); 
            }

            if (lonVar == null) {
                searchPair = null;
                if (navDataGroup != null) {
                    Variable varL = NetCDFUtils.findVariableForGroup(navDataGroup, null, "longitude", "lon");
                    if (varL == null)
                        varL = NetCDFUtils.findVariableWithAttributeForGroup(dataFile, navDataGroup, null,
                                NetCDFUtils.NETCDF_ATT_STANDARD_NAME, "longitude", "lon");
                    if (varL != null)
                        searchPair = new Pair<String, Variable>(varL.getShortName(), varL);
                }
                if (searchPair == null)
                    searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, true, dimDimStrLst,
                            "longitude", "lon");
                lonName = searchPair.first();
                lonVar = searchPair.second();
            }

            if (timeVar == null) {
                searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "time", "ocean_time");
                timeName = searchPair == null ? null : searchPair.first();
                timeVar = searchPair == null ? null : searchPair.second();
            }

            if (depthVar == null) {
                // If varName is already depth, no need to use it
                searchPair = "depth".equalsIgnoreCase(varName) ? null : NetCDFUtils.findVariableForStandardNameOrName(dataFile, fileName, false, dimDimStrLst, "depth");
                depthName = searchPair == null ? null : searchPair.first();;
                depthVar = searchPair == null ? null : searchPair.second(); 
            }

            if (latVar == null || lonVar == null) {
                NeptusLog.pub().debug(String.format("Variable %s IS NOT georeference in data fiel %s", varName, fileName));
                return null;
            }
            
            // Get the lat/lon data from the file.
            Array latArray;  // ArrayFloat.D?
            Array lonArray;  // ArrayFloat.D?
            Array timeArray; //ArrayFloat.D?
            Array depthArray; //ArrayFloat.D?
            Array vArray;    // ArrayFloat.D?

            latArray = latVar.read();
            lonArray = lonVar.read();
            timeArray = timeVar != null ? timeVar.read() : null;
            depthArray = depthVar != null ? depthVar.read() : null;
            vArray = vVar.read();
          
            double[] multAndOffset = timeVar != null ? NetCDFUtils.getTimeMultiplierAndOffset(timeVar, fileName) : null;
            double timeMultiplier = timeVar != null ? multAndOffset[0] : 1;
            double timeOffset = timeVar != null ? multAndOffset[1] : 0;

            Info info = createInfoBase(vVar);
          
            // Let us process
            try {
                double varFillValue = NetCDFUtils.findFillValue(vVar);
                Pair<Double, Double> varValidRange = NetCDFUtils.findValidRange(vVar);
                Pair<Double, Double> varScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(vVar);

                double latFillValue = NetCDFUtils.findFillValue(latVar);
                Pair<Double, Double> latValidRange = NetCDFUtils.findValidRange(latVar);
                Pair<Double, Double> latScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(latVar);

                double lonFillValue = NetCDFUtils.findFillValue(lonVar);
                Pair<Double, Double> lonValidRange = NetCDFUtils.findValidRange(lonVar);
                Pair<Double, Double> lonScaleFactorAndAddOffset = NetCDFUtils.findScaleFactorAnfAddOffset(lonVar);

                double depthFillValue = depthVar != null ? NetCDFUtils.findFillValue(depthVar) : 0;
                Pair<Double, Double> depthValidRange = depthVar != null ? NetCDFUtils.findValidRange(depthVar)
                        : new Pair<Double, Double>(0., 0.);
                Pair<Double, Double> depthScaleFactorAndAddOffset = depthVar != null
                        ? NetCDFUtils.findScaleFactorAnfAddOffset(depthVar)
                        : new Pair<Double, Double>(1., 0.);

                int[] shape = vVar.getShape();
                int[] counter = new int[shape.length];
                Arrays.fill(counter, 0);

                // The null values are ignored
                Map<String, Integer> timeCollumsIndexMap = timeVar == null ? new HashMap<>()
                        : NetCDFUtils.getIndexesForVar(dimStr, timeVar.getDimensionsString().split(" "));
                Map<String, Integer> latCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                        latVar.getDimensionsString().split(" "));
                Map<String, Integer> lonCollumsIndexMap = NetCDFUtils.getIndexesForVar(dimStr,
                        lonVar.getDimensionsString().split(" "));
                Map<String, Integer> depthCollumsIndexMap = depthVar == null ? new HashMap<>()
                        : NetCDFUtils.getIndexesForVar(dimStr, depthVar.getDimensionsString().split(" "));

                if (timeCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                    timeCollumsIndexMap.clear();
                if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                    latCollumsIndexMap = NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(latCollumsIndexMap,
                            vVar.getDimensions(), latVar.getDimensions());
                    if (latCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                        latCollumsIndexMap.clear();
                }
                if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0)) {
                    lonCollumsIndexMap = NetCDFUtils.getMissingIndexesForVarTryMatchDimSize(lonCollumsIndexMap,
                            vVar.getDimensions(), lonVar.getDimensions());
                    if (lonCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                        lonCollumsIndexMap.clear();
                }
                if (depthCollumsIndexMap.values().stream().anyMatch(i -> i < 0))
                    depthCollumsIndexMap.clear();

                do {
                    Date dateValue = null;
                    Date[] timeVals = !timeCollumsIndexMap.isEmpty()
                            ? NetCDFUtils.getTimeValues(timeArray, buildConterFrom(counter, timeCollumsIndexMap),
                                    timeMultiplier, timeOffset, fromDate, toDate, ignoreDateLimitToLoad, dateLimit)
                            : null;
                    
                    if (timeVals == null)
                        timeVals = NetCDFUtils.getTimeValuesByGlobalAttributes(dataFile, fromDate, toDate,
                                ignoreDateLimitToLoad, dateLimit);
                    
                    if (timeVals == null)
                        timeVals = NetCDFUtils.getDatesAndDateLimits(new Date(0), fromDate, toDate);

                    dateValue = timeVals[0];
                    fromDate = timeVals[1];
                    toDate = timeVals[2];

                    double lat = latArray.getDouble(buildIndexFrom(latArray, counter, latCollumsIndexMap));
                    double lon = lonArray.getDouble(buildIndexFrom(lonArray, counter, lonCollumsIndexMap));

                    if (!NetCDFUtils.isValueValid(lat, latFillValue, latValidRange)
                            || !NetCDFUtils.isValueValid(lon, lonFillValue, lonValidRange)) {
                        NeptusLog.pub().debug(
                                String.format("While processing %s found invalid values for lat or lon!", varName));
                        continue;
                    }
                    
                    lat = lat * latScaleFactorAndAddOffset.first() + latScaleFactorAndAddOffset.second();
                    lon = lon * lonScaleFactorAndAddOffset.first() + lonScaleFactorAndAddOffset.second();
                    lat = AngleUtils.nomalizeAngleDegrees180(lat);
                    lon = AngleUtils.nomalizeAngleDegrees180(lon);
                    
                    double depth = !depthCollumsIndexMap.isEmpty()
                            ? depthArray.getDouble(buildIndexFrom(depthArray, counter, depthCollumsIndexMap))
                            : Double.NaN;
                    if (!Double.isNaN(depth) && NetCDFUtils.isValueValid(depth, depthFillValue, depthValidRange)) {
                        depth = depth * depthScaleFactorAndAddOffset.first() + depthScaleFactorAndAddOffset.second();
                        depth = NetCDFUnitsUtils.getValueForMetterFromTempUnits(depth, depthVar.getUnitsString());
                    }

                    Index index = vArray.getIndex();
                    index.set(counter);

                    double v = vArray.getDouble(index);

                    if (NetCDFUtils.isValueValid(v, varFillValue, varValidRange)) {
                        GenericDataPoint dp = new GenericDataPoint(lat, lon);
                        dp.setInfo(info);

                        v = v * varScaleFactorAndAddOffset.first() + varScaleFactorAndAddOffset.second();
                        if (info.scalarOrLogPreference == ScalarOrLogPreference.LOG10)
                            v = Math.pow(10, v); // let us unlog

                        // Doing nothing with units, just using what it is
                        // v = NetCDFUnitsUtils.getValueForMetterFromTempUnits(v, vUnits);

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

                        dp.setDepth(depth); // See better this!!
                        if (Double.isFinite(depth)) {
                            if (!Double.isFinite(dp.getInfo().minDepth) || dp.getInfo().minDepth == Double.MIN_VALUE
                                    || depth < dp.getInfo().minDepth)
                                dp.getInfo().minDepth = depth;
                            if (!Double.isFinite(dp.getInfo().maxDepth) || dp.getInfo().maxDepth == Double.MAX_VALUE
                                    || depth > dp.getInfo().maxDepth)
                                dp.getInfo().maxDepth = depth;
                        }

                        GenericDataPoint dpo = dataDp.get(dp.getId());
                        if (dpo == null) {
                            switch (info.type) {
                                case GEO_TRAJECTORY:
                                    dp.setIndexesXY(Arrays.copyOf(counter, counter.length));
                                    break;
                                case GEO_2D:
                                    dp.setIndexesXY(Arrays.copyOfRange(counter, counter.length - 2, counter.length));
                                    break;
                                case UNKNOWN:
                                default:
                                    break;
                            }

                            dpo = dp.getACopyWithoutHistory();
                            dataDp.put(dpo.getId(), dpo);
                        }

                        ArrayList<GenericDataPoint> lst = dpo.getHistoricalData();
                        boolean alreadyIn = false;
                        for (GenericDataPoint tmpDp : lst) {
                            // Check also depth and see if no time
                            if (tmpDp.getDateUTC().equals(dp.getDateUTC()) && tmpDp.getDepth() == dp.getDepth()) {
                                alreadyIn = true;
                                break;
                            }
                        }
                        if (!alreadyIn)
                            dpo.getHistoricalData().add(dp);
                    }
                } while (NetCDFUtils.advanceLoopCounter(shape, counter) != null);
            }
            catch (Exception e) {
                e.printStackTrace();
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
     * @param varArray
     * @param counter
     * @param collumsIndexMap
     * @return
     */
    private static Index buildIndexFrom(Array varArray, int[] counter, Map<String, Integer> collumsIndexMap) {
        int[] idxCounter = buildConterFrom(counter, collumsIndexMap);
        Index ret = varArray.getIndex();
        ret.set(idxCounter);
        return ret;
    }

    /**
     * @param counter
     * @param collumsIndexMap
     * @return
     */
    private static int[] buildConterFrom(int[] counter, Map<String, Integer> collumsIndexMap) {
        int[] ret = new int[collumsIndexMap.size()];
        Iterator<Integer> ci = collumsIndexMap.values().iterator();
        for (int i = 0; i < ret.length; i++)
            ret[i] = counter[ci.next()];
        return ret;
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
        if (info.unit == null) {
            info.unit = "";
        }
        else {
            Pattern pattern = Pattern.compile("^ *?lo?g\\((.*)\\) *?$");
            Matcher matcher = pattern.matcher(info.unit);
            if (matcher.matches()) {
                info.unit = matcher.group(1);
                info.scalarOrLogPreference = ScalarOrLogPreference.LOG10;
            }
        }
        
        vAtt = vVar.findAttribute(NetCDFUtils.NETCDF_ATT_COMMENT);
        info.comment = vAtt == null ? "" : vAtt.getStringValue();
        
        if (vVar.getDimensions().size() == 1)
            info.type = Type.GEO_TRAJECTORY;
        else if (vVar.getDimensions().size() >= 2)
            info.type = Type.GEO_2D;
        else
            info.type = Type.UNKNOWN;
        
        return info;
    }

    /**
     * Deletes the netCDF unzipped file if any. The file passed has to be of the form "*.nc.gz".
     * 
     * @param fx
     */
    public static void deleteNetCDFUnzippedFile(File fx) {
        // Deleting the unzipped file
        String absPath = fx.getAbsolutePath();
        if (absPath.matches(".+\\.nc\\.gz$")) {
            absPath = absPath.replaceAll("\\.nc\\.gz$", ".nc");
        }
        else if (absPath.matches(".+\\.nc\\.zip$")) {
            absPath = absPath.replaceAll("\\.nc\\.zip$", ".nc");
        }
        else {
            absPath = null;
        }

        if (absPath != null) {
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
        List<Dimension> dimsRoot = dataFile.getDimensions();
        
        Map<String, Group> groupList = new HashMap<>();
        
        Map<String, Variable> varToConsider = NetCDFUtils.getVariables(dataFile, 1); // Choose variables with dim > 2
        
        for (String varStr : varToConsider.keySet().toArray(new String[varToConsider.size()])) {
            boolean isRemoved = false;

            // Remove vars that are root dimensions
            for (Dimension dimStr : dimsRoot) {
                if (varStr.equalsIgnoreCase(dimStr.getShortName())) {
                    varToConsider.remove(varStr);
                    isRemoved = true;
                    break;
                }
            }
            
            if (isRemoved)
                continue;

            // Collect groups
            Variable var = varToConsider.get(varStr);
            Group grp = var.getGroup();
            if (grp != null && !grp.getShortName().isEmpty() && !groupList.containsKey(grp.getShortName()))
                groupList.put(grp.getShortName(), grp);

            // Remove if an axis attribute
            if (var.findAttribute(NetCDFUtils.NETCDF_ATT_AXIS) != null) {
                varToConsider.remove(varStr);
                continue;
            }

            for (String sn : new String[] {"lat", "latitude", "lon", "longitude", "time", "lat_bnds", "lon_bnds"}) {
                if (sn.equalsIgnoreCase(var.getShortName())) {
                    varToConsider.remove(varStr);
                    isRemoved = true;
                    break;
                }
            }

            Attribute stdName = var.findAttribute(NetCDFUtils.NETCDF_ATT_STANDARD_NAME);
            if (stdName != null) {
                for (String sn : new String[] {"latitude", "longitude", "time"}) {
                    if (sn.equalsIgnoreCase(stdName.getStringValue())) {
                        varToConsider.remove(varStr);
                        isRemoved = true;
                        break;
                    }
                }
            }
        }
        
        boolean geoVarGroupExist = groupList.keySet().stream()
                .anyMatch(s -> s.equalsIgnoreCase(NetCDFUtils.NETCDF_GRP_GEOPHYSICAL_DATA));
        if (geoVarGroupExist) {
            // Remove all that not on the group
            for (String varStr : varToConsider.keySet().toArray(new String[varToConsider.size()])) {
                Variable v = varToConsider.get(varStr);
                if (!NetCDFUtils.NETCDF_GRP_GEOPHYSICAL_DATA.equalsIgnoreCase(v.getGroup().getShortName()))
                    varToConsider.remove(varStr);
            }
        }
        
        if (varToConsider.isEmpty()) {
            GuiUtils.infoMessage(parentWindow, I18n.text("Error loading"), I18n.text("Missing variables in data"));
            return null;
        }
        
        // Removing the ones that don't have location info
        // TODO
        
        ArrayList<JLabel> choicesVarsLbl = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>(varToConsider.keySet());
        Collections.sort(keys, (e1, e2) -> Collator.getInstance().compare(varToConsider.get(e1).getFullName(),
                varToConsider.get(e2).getFullName()));
        for (String vName : keys) {
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
    
    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        
        if (false) {
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
        
        if (true) {

            Pattern pattern = Pattern.compile("^ *?lo?g\\((.*)\\) *?$");
            Matcher matcher = pattern.matcher("log(re mg.m-3)");
            if (matcher.matches()) {
                System.out.println(matcher.group(1));
            }

            NetcdfFile dataFile = null;
            
            String baseFolder = "../NetCDF/";
//            baseFolder = "../../../netCDF/";
            
//            String fileName = baseFolder + "A2018116215500.L2_LAC.S3160_SOI.nc";
            String fileName = baseFolder + "nrt_global_allsat_phy_l4_latest.nc.gz";
            dataFile = NetcdfFile.open(fileName, null);
            
            List<Dimension> dims = dataFile.getDimensions();
            for (Dimension d : dims) {
                System.out.println("dim " + d.getShortName() + "   " + d.getLength());
            }
            
           Group rootGroup = dataFile.getRootGroup();
           System.out.println(String.format("root group: ", rootGroup.getShortName()));
           
           List<Attribute> globalAtt = dataFile.getGlobalAttributes();
           for (Attribute att : globalAtt) {
               System.out.println(String.format("Global Att: %s == %s", att.getShortName(), att.getValues()));
           }
           
           System.out.println(dataFile.getDetailInfo());
           
           List<Variable> varsL = dataFile.getVariables();
           for (Variable v : varsL) {
                System.out.println(String.format("Variable: %s :: %s >> Group='%s'::'%s' ParentGroup='%s'  Dim='%s'::'%s'", v.getShortName(), v.getFullName(),
                        v.getGroup().getShortName(), v.getGroup().getFullName(), v.getParentGroup().getShortName(),
                        v.getDimensions(), v.getDimensionsString()));
                for (Attribute att : v.getAttributes()) {
                    System.out.println(String.format("     Var Att: %s == %s", att.getShortName(), att.getValues()));
                }
           }
           
                      
           fileName = baseFolder + "A2018116215500.L2_LAC.S3160_SOI.nc";
           Variable choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));
           
           fileName = baseFolder + "nrt_global_allsat_phy_l4_latest.nc.gz";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));
           
           fileName = baseFolder + "20180416184057-MAR-L2P_GHRSST-SSTskin-SLSTRA-20180416204859-v02.0-fv01.0.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

           fileName = baseFolder + "marmenor_his.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));
           
           fileName = baseFolder + "temperature_salinity_STF_10042018.nc";
           dataFile = NetcdfFile.open(fileName, null);
           choiceVar = showChooseVar(fileName, dataFile, null);
           System.out.println(String.format("Choice is '%s'", choiceVar.getShortName()));

        }
    }
}
