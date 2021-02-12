/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Dec 21, 2015
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JFileChooser;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.imc.DevDataText;
import pt.lsts.imc.RSSI;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.airos.Station;
import pt.lsts.neptus.console.plugins.airos.StationList;
import pt.lsts.neptus.console.plugins.airos.WiFiMacAddresses;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.credentials.Credentials;
import pt.lsts.neptus.util.ssh.SSHUtil;

/**
 * @author zp
 *
 */
@PluginDescription(name="AirOS Peers", icon="images/airos.png")
@Popup(accelerator='9',pos=POSITION.CENTER,height=400,width=400)
public class AirOSPeers extends ConsolePanel {

    private static final long serialVersionUID = 2622193661006634171L;
    private ChartPanel cpanel;
    private JFreeChart chart;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private long lastUpdateMillis = 0;
    
    @NeptusProperty(name="AirOS hostname", userLevel=LEVEL.REGULAR)
    String host = "10.0.30.1";
    
    @NeptusProperty(name="AirOS SSH port", userLevel=LEVEL.REGULAR)
    int port = 22;
    
    @NeptusProperty(name="Seconds between updates", userLevel=LEVEL.REGULAR)
    int seconds = 2;
    
    @NeptusProperty(name="Credentials", userLevel=LEVEL.REGULAR)
    Credentials credentials = new Credentials(new File("conf/AirOS.conf"));
    
    @NeptusProperty(name="Log to disk", userLevel=LEVEL.ADVANCED)
    boolean logToDisk = true;    
    
    private LinkedHashMap<String, String> ipToNames = new LinkedHashMap<>(); 
    private LinkedHashMap<String, Integer> namesToIds = new LinkedHashMap<>();
    
    @Periodic(millisBetweenUpdates=5000)
    private void updateIpAddresses() {
        for (ImcSystem s : ImcSystemsHolder.lookupAllSystems()) {
            if (s.getHostAddress() != null)
                ipToNames.put(s.getHostAddress(), s.getName());
        }        
    }
    
    @Periodic
    private void update() {       
        if (System.currentTimeMillis() - lastUpdateMillis < seconds * 1000)
            return;
        lastUpdateMillis = System.currentTimeMillis();
        
        Future<String> result = SSHUtil.exec(host, port, credentials.getUsername(), credentials.getPassword(), "wstalist");
        try {
            String json = result.get(5, TimeUnit.SECONDS);
            
            if (logToDisk) {
                DevDataText txt = new DevDataText();
                txt.setValue(json);
                txt.setSrcEnt(ImcMsgManager.getManager().getEntityId());
                txt.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
                LsfMessageLogger.log(txt);
            }
            
            HashSet<String> series = new HashSet<>();
            for (Object o : tsc.getSeries()) {
                TimeSeries tseries = (TimeSeries)o;
                series.add(""+tseries.getKey());
            }
            
            for (Station station : new StationList(json).stations) {
                series.remove(nameOf(station));
                process(station);                
            }
            
            for (String name : series) {
                TimeSeries ts = tsc.getSeries(name);                
                if (ts != null)
                    ts.addOrUpdate(new Millisecond(new Date(System.currentTimeMillis())), null);
            }
        }
        catch (TimeoutException timeout) {
            NeptusLog.pub().error("Timeout while trying to connect to "+host+":"+port);
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().error(e);            
        }
    }
    
    private int entityOf(String name) {
        if (!namesToIds.containsKey(name)) {
            try {
                namesToIds.put(name, ImcMsgManager.getManager().registerEntity(name));
            }
            catch (Exception e) {
                namesToIds.put(name, 255);
            }
        }
        return namesToIds.get(name);
    }
    
    private String nameOf(Station station) {
        String name = WiFiMacAddresses.resolve(station.mac);
        if (name != null)
            return name;
        
        name = ipToNames.get(station.lastip);
        if (name != null)
            return name;
        
        return "IP("+station.lastip+")";
    }
    
    private void process(Station station) {
        String name = nameOf(station);
        
        TimeSeries ts = tsc.getSeries(name);
        if (ts == null) {
            ts = new TimeSeries(name);
            ts.setMaximumItemCount(250);
            tsc.addSeries(ts);
        }
        ts.addOrUpdate(new Millisecond(new Date(System.currentTimeMillis())), station.ccq);
        if (logToDisk) {
            RSSI rssi = new RSSI();
            rssi.setValue(station.ccq);
            rssi.setSrcEnt(entityOf(name+" wifi"));
            rssi.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
            rssi.setDst(65535);
            LsfMessageLogger.log(rssi);
        }
    }
    
    public AirOSPeers(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        chart = ChartFactory.createTimeSeriesChart(null, "Time of day", "Link Quality", tsc, true, true, true);
        chart.getPlot().setBackgroundPaint(Color.black);
        cpanel = new ChartPanel(chart);
        add (cpanel, BorderLayout.CENTER);
        cpanel.getPopupMenu().add(I18n.text("Load Addresses")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setFileFilter(GuiUtils.getCustomFileFilter("CSV Files", "csv"));
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int op = chooser.showOpenDialog(AirOSPeers.this);
                    if (op == JFileChooser.APPROVE_OPTION)
                        WiFiMacAddresses.parseAddresses(new FileReader(chooser.getSelectedFile()));  
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }
            }
        });
        cpanel.getPopupMenu().addSeparator();
        cpanel.getPopupMenu().add(I18n.text("Clear")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tsc.removeAllSeries();
            }
        });
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public void initSubPanel() {
        updateIpAddresses();        
    }
}
