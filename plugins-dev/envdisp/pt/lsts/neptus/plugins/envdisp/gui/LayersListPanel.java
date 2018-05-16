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
 * Apr 25, 2018
 */
package pt.lsts.neptus.plugins.envdisp.gui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXBusyLabel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.envdisp.loader.NetCDFLoader;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LayersListPanel extends JPanel {

    private static final ImageIcon LOGOIMAGE_ICON = new ImageIcon(
            ImageUtils.getScaledImage("pt/lsts/neptus/plugins/envdisp/netcdf-radar.png", 32, 32));
    static final ImageIcon VIEW_IMAGE_ICON = ImageUtils.createImageIcon("images/menus/view.png");

    static enum UpOrDown {
        UP,
        DOWN
    }

    private AtomicLong plotCounter = new AtomicLong();
    private File recentFolder = new File(".");

    // GUI
    private Window parentWindow = null;
    private JPanel holder;
    private JPanel buttonBarPanel;
    private JScrollPane scrollHolder;
    private JXBusyLabel busyPanel;
    
    private JButton addButton;
    private AbstractAction addAction;
    private JButton hideAllButton;
    private AbstractAction hideAllAction;

    public LayersListPanel() {
        this(null);
    }
    
    public <W extends Window> LayersListPanel(W parentWindow) {
        this.parentWindow = parentWindow;
        this.setPreferredSize(new Dimension(600, 400));
        initializeActions();
        initialize();
    }

    private void initialize() {
        setLayout(new MigLayout("ins 0, wrap 1"));

        buttonBarPanel = new JPanel(new MigLayout("ins 10"));
        
        JLabel logoLabel = new JLabel(LOGOIMAGE_ICON);
        buttonBarPanel.add(logoLabel);
        
        busyPanel = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(20);
        busyPanel.setVisible(false);
        buttonBarPanel.add(busyPanel);
        
        Dimension buttonDimension = new Dimension(80, 30);
        addButton = new JButton(addAction);
        addButton.setSize(buttonDimension);
        buttonBarPanel.add(addButton, "sg button");

        hideAllButton = new JButton(hideAllAction);
        buttonBarPanel.add(hideAllButton, "sg button");
        
        add(buttonBarPanel, "w 100%");

        holder = new JPanel();
        holder.setLayout(new MigLayout("ins 5, flowy", "grow, fill", ""));
        holder.setSize(400, 600);
        
        scrollHolder = new JScrollPane(holder);
        add(scrollHolder, "w 100%, h 100%");
    }
    
    private void initializeActions() {
        addAction = new AbstractAction(I18n.text("Add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                File fx = NetCDFLoader.showChooseANetCDFToOpen(parentWindow, recentFolder);
                if (fx == null)
                    return;
                
                JButton source = (JButton) e.getSource();
                source.setEnabled(false);
                setBusy(true);
                
                try {
                    NetcdfFile dataFile = NetcdfFile.open(fx.getPath());
                    
                    Variable choiceVarOpt = NetCDFLoader.showChooseVar(fx.getName(), dataFile, parentWindow);
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
                                        // PluginUtils.editPluginProperties(viz, parentWindow, true);
                                        addVisualizationLayer(viz);
                                    }
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e.getMessage(), e);
                                    GuiUtils.errorMessage(parentWindow,
                                            I18n.textf("Loading netCDF variable %s", choiceVarOpt.getShortName()),
                                            e.getMessage());
                                }
                                NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                                source.setEnabled(true);
                                setBusy(false);
                            }
                        };
                        sw.execute();
                    }
                    else {
                        source.setEnabled(true);
                        NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                        setBusy(false);
                    }
                    
                    recentFolder = fx;
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    source.setEnabled(true);
                    NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                    setBusy(false);
                }
            }
        };

        hideAllAction = new AbstractAction(I18n.text("Hide All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                getAllVizConfigPanels().forEach(p -> p.setVizVisible(false));
            }
        };
    }

    private void setBusy(boolean busy) {
        busyPanel.setVisible(busy);
        busyPanel.setBusy(busy);;
    }

    /**
     * @return the parentWindow
     */
    public Window getParentWindow() {
        return parentWindow;
    }
    
    /**
     * @param parentWindow the parentWindow to set
     */
    public <W extends Window> void setParentWindow(W parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    /**
     * This list is ordered by paint in most foreground to the less.
     * (So painting in reverse order is necessary.)
     * 
     * @return the varLayersList
     */
    public List<GenericNetCDFDataPainter> getVarLayersList() {
        return getAllVizConfigPanels().map(c -> c.getViz()).collect(Collectors.toList());
    }
    
    /**
     * @return
     */
    private Stream<VizConfigPanel> getAllVizConfigPanels() {
        return Stream.of(holder.getComponents()).filter(c -> c instanceof VizConfigPanel).map(c -> (VizConfigPanel) c);
    }

    private void addVisualizationLayer(GenericNetCDFDataPainter viz) {
        new VizConfigPanel(holder, viz);
    }

    public static void main(String[] args) {
        GuiUtils.testFrame(new LayersListPanel(), "", 620, 350);
    }
}
