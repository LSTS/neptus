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
 * Apr 22, 2018
 */
package pt.lsts.neptus.plugins.envdisp;

import java.awt.Font;
import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint;
import pt.lsts.neptus.plugins.envdisp.datapoints.GenericDataPoint.Info;
import pt.lsts.neptus.plugins.envdisp.loader.NetCDFLoader;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.netcdf.NetCDFUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "netCDF Data Visualization", icon = "pt/lsts/neptus/plugins/envdisp/netcdf-radar.png")
public class NetCDFDataVisualization extends ConsoleLayer implements ConfigurationListener {
    
    private static final String CATEGORY_TEST = "Test";
    private static final String CATEGORY_DATA_UPDATE = "Data Update";
    private static final String CATEGORY_VISIBILITY_VAR = "Visibility";

    @NeptusProperty(name = "Show var", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    public boolean showVar = true;
    @NeptusProperty(name = "Show var legend", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    public boolean showVarLegend = true;
    @NeptusProperty(name = "Show var legend from zoom level bigger than", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    public int showVarLegendFromZoomLevel = 13;
    @NeptusProperty(name = "Show var colorbar", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR,
            description = "Show the color scale bar. Only one will show.")
    public boolean showVarColorbar = false;
    @NeptusProperty(name = "Colormap for var", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    private ColorMap colorMapVar = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Data limit validity (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateLimitHours = 30;
    @NeptusProperty(name = "Use data x hour in the future (hours)", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public int dateHoursToUseForData = 1;
    @NeptusProperty(name = "Ignore data limit validity to load data", userLevel=LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE)
    public boolean ignoreDateLimitToLoad = true;

    @NeptusProperty(name = "Base Folder For netCDF Files", userLevel = LEVEL.REGULAR, category = CATEGORY_DATA_UPDATE, 
            description = "The folder to look for netCDF files. Admissible files '*.nc or *.nc.gz'. NetCDF variables used: lat, lon, time, <depth>.",
            editorClass = FolderPropertyEditor.class)
    public File baseFolderForVarNetCDFFiles = new File("netCDF/");
    
    @NeptusProperty(name = "Show visible data date-time interval", userLevel = LEVEL.ADVANCED, category = CATEGORY_TEST, 
            description = "Draws the string with visible curents data date-time interval.")
    public boolean showDataDebugLegend = false;

    @NeptusProperty(name = "Var min", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    private double minVar = -1.0;
    @NeptusProperty(name = "Var max", userLevel = LEVEL.REGULAR, category = CATEGORY_VISIBILITY_VAR)
    private double maxVar = 1.0;

    private final Font font8Pt = new Font("Helvetica", Font.PLAIN, 9);

    @SuppressWarnings("serial")
    static final SimpleDateFormat dateTimeFormaterUTC = new SimpleDateFormat("yyyy-MM-dd HH':'mm':'ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};
    @SuppressWarnings("serial")
    static final SimpleDateFormat dateTimeFormaterSpacesUTC = new SimpleDateFormat("yyyy MM dd  HH mm ss") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};

    private AtomicLong plotCounter = new AtomicLong();
    
    private File recentFolder = null;
    
    private GenericNetCDFDataPainter gDataViz = null;
    
    public NetCDFDataVisualization() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
    }

    @NeptusMenuItem("Tools > netCDF Data Visualization > Add new")
    public void configMenuAction() {
        if (recentFolder == null)
            recentFolder = baseFolderForVarNetCDFFiles;
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileView(new NeptusFileView());
        chooser.setCurrentDirectory(recentFolder);
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("netCDF files"), "nc", "nc.gz"));
        chooser.setApproveButtonText(I18n.text("Open file"));
        chooser.showOpenDialog(getConsole());
        if (chooser.getSelectedFile() == null)
            return;

        File fx = chooser.getSelectedFile();
        
        NetcdfFile dataFile = null;
        
        try {
            dataFile = NetcdfFile.open(fx.getPath());
            
            Map<String, Variable> varToConsider = NetCDFUtils.getMultiDimensionalVariables(dataFile);
            
            Pair<String, Variable> searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fx.getName(), true, "latitude", "lat");
            String latName = searchPair.first();
            searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fx.getName(), true, "longitude", "lon");
            String lonName = searchPair.first();
            // searchPair = NetCDFUtils.findVariableForStandardNameOrName(dataFile, fx.getName(), false, "time");
            // String timeName = searchPair == null ? null : searchPair.first();
            if (latName == null || lonName == null /*|| timeName == null*/) {
                GuiUtils.infoMessage(getConsole(), I18n.text("Error loading"), I18n.textf("Missing variables in data (%s)", "lat; lon"));
                return;
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
            Object choiceOpt = JOptionPane.showInputDialog(getConsole(), I18n.text("Choose one of the vars"),
                    I18n.text("Chooser"), JOptionPane.QUESTION_MESSAGE, null,
                    choicesVarsLbl.toArray(new JLabel[choicesVarsLbl.size()]), 0);
            
            if (choiceOpt != null) {
                String vn = ((JLabel) choiceOpt).getText();
                varToConsider.get(vn);
                HashMap<String, GenericDataPoint> dataPoints = NetCDFLoader.processFileForVariable(dataFile, vn, null);
                GenericNetCDFDataPainter gDataViz = new GenericNetCDFDataPainter(plotCounter.getAndIncrement(), dataPoints);
                this.gDataViz = gDataViz;
                
                PluginUtils.editPluginProperties(gDataViz, true);
            }
            
            recentFolder = fx;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        // Deleting the unzipped file
        if ("gz".equalsIgnoreCase(FileUtil.getFileExtension(fx))) {
            String absPath = fx.getAbsolutePath();
            absPath = absPath.replaceAll("\\.gz$", "");
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
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (gDataViz != null)
            gDataViz.paint(g, renderer, ignoreDateLimitToLoad, dateLimitHours, showDataDebugLegend);
    }
}
