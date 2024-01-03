/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Nov 3, 2014
 */
package pt.lsts.neptus.console.bathymLayer;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

/**
 * @author zp
 *
 */
@PluginDescription(name="Tide panel")
@Popup(accelerator='6',pos=POSITION.CENTER,height=300,width=300)
public class TidePanel extends ConsolePanel implements PreferencesListener {
    private static final long serialVersionUID = 6517658675736342089L;

    private JFreeChart timeSeriesChart = null;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private TimeSeries ts;
    private ValueMarker marker = new ValueMarker(System.currentTimeMillis());
    private ValueMarker levelMarker = new ValueMarker(0);
    private JMenuItem tidesItem = null;
    private String storedMenuPath;
    
    /**
     * @param console
     */
    public TidePanel(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        add(new ChartPanel(timeSeriesChart), BorderLayout.CENTER);
        GeneralPreferences.addPreferencesListener(this);
    }

    @Override
    public void cleanSubPanel() {
        removeMenuItem(I18n.text("Tools") + ">" + I18n.text("Tides") + ">" + I18n.text("Update Predictions"));
        removeMenuItem(storedMenuPath);
    }

    @Override
    public void initSubPanel() {
        storedMenuPath = I18n.text("Tools") + ">" + I18n.text("Tides") + ">"+I18n.textf("Using '%file'",  GeneralPreferences.tidesFile.getName());
        tidesItem = addMenuItem(storedMenuPath, null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem menu = (JMenuItem) e.getSource();
                Thread t = new Thread("Tide chooser") {
                    @Override
                    public void run() {
                        try {
                            File usedTidesSource = GeneralPreferences.tidesFile;
                            String currentSource = usedTidesSource == null || !usedTidesSource.exists() ? null
                                    : usedTidesSource.getName();
                            Date startDate = new Date(System.currentTimeMillis() - 2 * DateTimeUtil.DAY);
                            Date endDate = new Date(System.currentTimeMillis() + 3 * DateTimeUtil.DAY);
                            String harbor = TidePredictionFactory.showTidesSourceChooserGuiPopup(getConsole(), currentSource,
                                    startDate, endDate);
                            if (harbor != null && !harbor.isEmpty()
                                    && TidePredictionFactory.getTidesSourceFileFrom(harbor).exists()) {
                                GeneralPreferences.tidesFile = TidePredictionFactory.getTidesSourceFileFrom(harbor);
                                GeneralPreferences.saveProperties();
                                preferencesUpdated();
                                // Force the tide file reload
                                TidePredictionFactory.getTideLevel(System.currentTimeMillis());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            menu.setEnabled(true);
                        }
                    }
                };
                t.setDaemon(true);
                menu.setEnabled(false);
                t.run();
            }
        });
//        tidesItem.setEnabled(false);
        
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("Tides") + ">" + I18n.text("Update Predictions"), null,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem menu = (JMenuItem) e.getSource();
                Thread t = new Thread("Tide fetcher") {
                    public void run() {
                        try {
                            String harbor = TidePredictionFactory.fetchData(getConsole());
                            if (harbor != null  && !harbor.isEmpty()) {
                                File used = GeneralPreferences.tidesFile;
                                File f = new File(ConfigFetch.getConfFolder() + "/tides/" + harbor + "."
                                        + TidePredictionFactory.defaultTideFormat);
                                if (f.exists() && !f.getAbsolutePath().equals(used.getAbsolutePath())) {
                                    int resp = GuiUtils.confirmDialog(getConsole(), I18n.text("Tide Predictions"),
                                            I18n.textf(
                                                    "The selected location does not match the current location in use (%harbor). Do you wish to set the current location as %selection?",
                                                    used.getName(), harbor + "." + TidePredictionFactory.defaultTideFormat),
                                            ModalityType.DOCUMENT_MODAL);
                                    
                                    if (resp == JOptionPane.YES_OPTION) {
                                        GeneralPreferences.tidesFile = f;
                                        GeneralPreferences.saveProperties();
                                        preferencesUpdated();
                                        // Force the tide file reload
                                        TidePredictionFactory.getTideLevel(System.currentTimeMillis());
                                    }
                                }
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {
                            menu.setEnabled(true);
                        }
                    };
                };
                t.setDaemon(true);
                menu.setEnabled(false);
                t.start();                
            }
        });  

        ts = new TimeSeries(I18n.text("Tide level"));
        tsc.addSeries(ts);

        for (double i = -12; i < 12; i+= 0.25) {
            Date d = new Date(System.currentTimeMillis() + (long)(i * 1000 * 3600));
            ts.addOrUpdate(new Millisecond(d), TidePredictionFactory.getTideLevel(d));
        }
        timeSeriesChart.getXYPlot().addDomainMarker(marker);
        levelMarker.setValue(TidePredictionFactory.getTideLevel(new Date()));
        timeSeriesChart.getXYPlot().addRangeMarker(levelMarker);
    }

    @Periodic(millisBetweenUpdates = 60000)
    public void updateMarker() {
        marker.setValue(System.currentTimeMillis());
        levelMarker.setValue(TidePredictionFactory.getTideLevel(new Date()));
        
        ts.clear();
        for (double i = -12; i < 12; i+= 0.25) {
            Date d = new Date(System.currentTimeMillis() + (long)(i * 1000 * 3600));
            ts.addOrUpdate(new Millisecond(d), TidePredictionFactory.getTideLevel(d));
        }
        ts.fireSeriesChanged();
    }

    // general preferences was updated
    public void preferencesUpdated() {
        storedMenuPath = I18n.text("Tools") + ">" + I18n.text("Tides") + ">"+I18n.textf("Using '%file'",  GeneralPreferences.tidesFile.getName());
        tidesItem.setText(I18n.textf("Using '%file'",  GeneralPreferences.tidesFile.getName()));
//        tidesItem.setEnabled(false);
    }
}
