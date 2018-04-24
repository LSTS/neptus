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
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.SwingWorker;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.gui.editor.FolderPropertyEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.envdisp.loader.NetCDFLoader;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.GuiUtils;
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
        
        File fx = NetCDFLoader.showChooseANetCDFToOpen(getConsole(), recentFolder);
        if (fx == null)
            return;
        
        try {
            NetcdfFile dataFile = NetcdfFile.open(fx.getPath());
            
            Variable choiceVarOpt = NetCDFLoader.showChooseVar(fx.getName(), dataFile, getConsole());
            if (choiceVarOpt != null) {
                Future<GenericNetCDFDataPainter> fTask = NetCDFLoader.loadNetCDFPainterFor(fx.getPath(), dataFile,
                        choiceVarOpt.getShortName(), plotCounter.getAndIncrement());
                SwingWorker<GenericNetCDFDataPainter, Void> sw = new SwingWorker<GenericNetCDFDataPainter, Void>() {
                    @Override
                    protected GenericNetCDFDataPainter doInBackground() throws Exception {
                        return fTask.get();
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            GenericNetCDFDataPainter viz = get();
                            if (viz != null) {
                                PluginUtils.editPluginProperties(viz, getConsole(), true);
                                NetCDFDataVisualization.this.gDataViz = viz;
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e.getMessage(), e);
                            GuiUtils.errorMessage(getConsole(),
                                    I18n.textf("Loading netCDF variable %s", choiceVarOpt.getShortName()),
                                    e.getMessage());
                        }
                        NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                    }
                };
                sw.execute();
            }
            
            recentFolder = fx;
        }
        catch (Exception e) {
            e.printStackTrace();
            NetCDFLoader.deleteNetCDFUnzippedFile(fx);
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
