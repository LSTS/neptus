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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Oct 16, 2012
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.LblRangeAcceptance;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author noptilus
 */
@PluginDescription(name = "Transponder Location Estimation", author="Noptilus")
public class TransponderEstimation extends ConsolePanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    public static final int TIME = 0, X = 1, Y = 2, Z = 3, RANGE = 4;

    @NeptusProperty(name = "Logs folder", editable = true) 
    public String logsFolder = ".";

    @NeptusProperty(name = "Number of iterations")
    public int numIterations = 10000;
    
    @NeptusProperty(name = "Distance treshold")
    public double dropDistance = 100;

    protected LinkedHashMap<String, LocationType> estimations = new LinkedHashMap<String, LocationType>();
    protected JMenuItem calcMenu = null, settingsMenu = null;

    private JMenu menu;
    
    public TransponderEstimation(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (String name : estimations.keySet()) {
            LocationType loc = estimations.get(name);            
            Point2D pt = renderer.getScreenPosition(loc);
            g.setColor(Color.green.brighter());
            g.drawString(name, (int)pt.getX()+10, (int)pt.getY());
            g.draw(new Line2D.Double(pt.getX()-3, pt.getY()-3, pt.getX()+3, pt.getY()+3));
            g.draw(new Line2D.Double(pt.getX()+3, pt.getY()-3, pt.getX()-3, pt.getY()+3));
        }
    }    
    
    @Override
    public void initSubPanel() {
        calcMenu = addMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Calculate", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(logsFolder);
                chooser.setDialogTitle("Open surface log file");
                int option = chooser.showOpenDialog(getConsole());

                if (option == JFileChooser.APPROVE_OPTION) {
                    File selection = chooser.getSelectedFile();
                    logsFolder = selection.getParent();

                    try {
                        getRanges(selection);
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                    }
                }
            }
        });
        
        settingsMenu = addMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Settings", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               PropertiesEditor.editProperties(TransponderEstimation.this, true);               
            }
        });
        
    }

    public void getRanges(File lsfFile) throws Exception {
        LsfIndex index = new LsfIndex(lsfFile, new IMCDefinition(new FileInputStream(new File(lsfFile.getParent(),
                "IMC.xml"))));

        LinkedHashMap<Short, Vector<double[]>> ranges = new LinkedHashMap<Short, Vector<double[]>>();
        LinkedHashMap<Short, double[]> beaconLocations = new LinkedHashMap<Short, double[]>();
        LinkedHashMap<Short, String> beaconNames = new LinkedHashMap<Short, String>();

        int curPos = index.getFirstMessageOfType(LblRangeAcceptance.ID_STATIC);

        while (curPos != -1) {
            LblRangeAcceptance range = LblRangeAcceptance.clone(index.getMessage(curPos));
            if (!ranges.containsKey(range.getId()))
                ranges.put(range.getId(), new Vector<double[]>());
            int estate = index.getNextMessageOfType(EstimatedState.ID_STATIC, curPos);

            if (estate != -1) {
                EstimatedState state = EstimatedState.clone(index.getMessage(estate));
                double[] values = new double[] { range.getTimestamp(), state.getX(), state.getY(), state.getZ(),
                        range.getRange() };
                ranges.get(range.getId()).add(values);
            }
            curPos = index.getNextMessageOfType(LblRangeAcceptance.ID_STATIC, curPos);
        }

        int lblIndex = index.getFirstMessageOfType(LblConfig.ID_STATIC);
        if (lblIndex == -1)
            throw new Exception("No LBL configuration found in the log");

        int hrefIndex = index.getFirstMessageOfType(EstimatedState.ID_STATIC);
        if (hrefIndex == -1)
            throw new Exception("No HomeRef found in the log");
        
        EstimatedState state = EstimatedState.clone(index.getMessage(hrefIndex));
        LocationType homeLoc = new LocationType(Math.toDegrees(state.getLat()), Math.toDegrees(state.getLon()));
        LblConfig lblConfig = LblConfig.clone(index.getMessage(lblIndex));
        Vector<LblBeacon> beacons = lblConfig.getBeacons();
        
        for (short i = 0; i < beacons.size(); i++) {
            if (beacons.get(i) == null)
                continue;
            
            LblBeacon beacon = beacons.get(i);
            
            if (beacon != null) {
                LocationType loc = new LocationType(Math.toDegrees(beacon.getLat()), Math.toDegrees(beacon.getLon()));
                loc.setAbsoluteDepth(beacon.getDepth());
                beaconLocations.put( i, loc.getOffsetFrom(homeLoc));
                beaconNames.put( i, beacon.getBeacon());
            }
        }

        JTabbedPane tabs = new JTabbedPane();
        for (Short key : ranges.keySet()) {
            Vector<double[]> vec = ranges.get(key);

            Object[][] values = new Object[vec.size()][5];
            for (int i = 0; i < vec.size(); i++)
                for (int j = 0; j < 5; j++)
                    values[i][j] = vec.get(i)[j];

            DefaultTableModel model = new DefaultTableModel(values, new String[] { "time", "x", "y", "z", "range" });
            JTable table = new JTable(model);
            JScrollPane scroll = new JScrollPane(table);

            tabs.addTab("Beacon " + key, scroll);
        }

        Random rand = new Random(System.currentTimeMillis());

        for (short beacon : beaconNames.keySet()) {
            double[] estimate = Arrays.copyOf(beaconLocations.get(beacon), beaconLocations.get(beacon).length);
            double[] bestEstimate = Arrays.copyOf(estimate, estimate.length);
            
            Vector<double[]> measurements = ranges.get(beacon);

            if (measurements == null)
                continue;
            
            int n = measurements.size();
            double bestR = Double.MAX_VALUE;

            for (int k = 0; k < numIterations; k++) {
                double r = 0;

                for (int i = 0; i < measurements.size(); i++) {
                    double px = measurements.get(i)[X];
                    double py = measurements.get(i)[Y];
                    double pz = measurements.get(i)[Z];

                    double val1 = Math.sqrt(
                            (px - estimate[0]) * (px - estimate[0]) + 
                            (py - estimate[1]) * (py - estimate[1]) + 
                            (pz - estimate[2]) * (pz - estimate[2])
                        );

                    double val2 = measurements.get(i)[RANGE];

                    r += Math.abs(val1 - val2);
                }
                r = r / n;

                if (r < bestR) {
                    bestEstimate = Arrays.copyOf(estimate, estimate.length);
                    bestR = r;
                }

                for (int i = 0; i < 3; i++)
                    estimate[i] = bestEstimate[i] + rand.nextGaussian() * Math.sqrt(bestR);
            }
            
            LocationType loc = new LocationType(homeLoc);
            loc.translatePosition(bestEstimate);
            
            estimations.put(beaconNames.get(beacon), loc);
        }
        
        menu = getConsole().getOrCreateJMenu(new String[] {"Noptilus", "Transponder Estimation"});
        menu.removeAll();
        menu.add(calcMenu);
        menu.add(settingsMenu);
            
        for (String beaconName : estimations.keySet()) {
            final LocationType loc = estimations.get(beaconName);
            loc.setId(beaconName);
            JMenuItem item = new JMenuItem("Copy " + beaconName + " location");
            item.setToolTipText(loc.getLatitudeAsPrettyString() + " / "+loc.getLongitudeAsPrettyString()+" / "+loc.getAllZ());
            item.addActionListener(new ActionListener() {                
                @Override
                public void actionPerformed(ActionEvent e) {
                    ClipboardOwner owner = new ClipboardOwner() {
                        @Override
                        public void lostOwnership(java.awt.datatransfer.Clipboard clipboard, java.awt.datatransfer.Transferable contents) {};                       
                    };
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(loc.getClipboardText()), owner);
                }
            });
            item.setActionCommand("copy " + beaconName);
            menu.add(item);
        }
        
        JMenuItem clear = new JMenuItem("Clear");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenu menu = getConsole().getOrCreateJMenu(new String[] {"Noptilus", "Transponder Estimation"});
                estimations.clear();
                menu.removeAll();
                menu.add(calcMenu);
                menu.add(settingsMenu);
            }
        });
        menu.add(clear);
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        TransponderEstimation est = new TransponderEstimation(null);
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Open surface log file");
        int option = chooser.showOpenDialog(null);

        if (option == JFileChooser.APPROVE_OPTION) {
            File selection = chooser.getSelectedFile();

            try {
                est.getRanges(selection);
            }
            catch (Exception ex) {
                GuiUtils.errorMessage(null, ex);
                ex.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Calculate");
        removeMenuItem("Noptilus"+">"+"Transponder Estimation"+">"+"Settings");
        menu.removeAll();
        removeMenuItem("Noptilus>Transponder Estimation");
    }
}
