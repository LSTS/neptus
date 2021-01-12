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
 * Author: pdias
 * 01/06/2015
 */
package com.inovaworks;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Inovaworks Uploader")
public class InovaWorksUploader extends ConsolePanel {

    @NeptusProperty
    private String url = "http://ec2-52-16-31-123.eu-west-1.compute.amazonaws.com:8080/GeoC2/api/sensing/observations";

    @NeptusProperty(name = "Publish period (ms)", description = "The period to fetch the systems' positions.")
    public int publishPeriodMillis = 60000;

    @NeptusProperty(category = "Advanced", description = "If true any simulated system will not be published.")
    public boolean ignoreSimulatedSystems = true;

    @NeptusProperty
    public boolean publishOn = false;

    private HttpClientConnectionHelper httpComm;
    private HttpPost postHttpRequest;

    private JCheckBoxMenuItem menuItem;
    private ImageIcon onIcon;
    private ImageIcon offIcon;

    private LinkedHashMap<String, SystemPositionAndAttitude> toSend = new LinkedHashMap<String, SystemPositionAndAttitude>();
    private long lastSent = -1;
    
    /**
     * @param console
     */
    public InovaWorksUploader(ConsoleLayout console) {
        super(console);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        onIcon = ImageUtils.getScaledIcon("com/inovaworks/inova-on.png", 16, 16);
        offIcon = ImageUtils.getScaledIcon("com/inovaworks/inova-off.png", 16, 16);
        menuItem = addCheckMenuItem(I18n.text("Tools") + ">" + I18n.text("Inovaworks") + ">" 
                    + I18n.text("Start Synch"), offIcon, new CheckMenuChangeListener() {

            @Override
            public void menuUnchecked(ActionEvent e) {
                menuItem.setText(I18n.text("Start Synch"));
                menuItem.setIcon(offIcon);
                stopSynch();
            }

            @Override
            public void menuChecked(ActionEvent e) {
                menuItem.setText(I18n.text("Stop Synch"));
                menuItem.setIcon(onIcon);
                startSynch();
            }
        });

        if (publishOn)
            menuItem.doClick();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        if (publishOn)
            stopSynch();
        
        removeCheckMenuItem(I18n.text("Tools") + ">" + I18n.text("Inovaworks") + ">" 
                + I18n.text("Start Synch"));
    }

    private void startSynch() {
        NeptusLog.pub().info("Started synch'ing with Inovaworks.");
        httpComm = new HttpClientConnectionHelper();
        httpComm.initializeComm();
        publishOn = true;
    }

    private void stopSynch() {
        publishOn = false;
        NeptusLog.pub().info("Stopped synch'ing with Inovaworks.");
        httpComm.cleanUp();
        lastSent = -1;
    }


    @Subscribe
    public void on(EstimatedState state) {
        if (!publishOn)
            return;
        SystemPositionAndAttitude pose = new SystemPositionAndAttitude(IMCUtils.parseLocation(state)
                .convertToAbsoluteLatLonDepth(), state.getPhi(), state.getTheta(), state.getPsi());
        pose.setTime(state.getTimestampMillis());
        synchronized (toSend) {
            toSend.put(state.getSourceName(), pose);
        }
    }

    @Subscribe
    public void on(Announce announce) {
        if (!publishOn)
            return;

        LocationType loc = new LocationType(Math.toDegrees(announce.getLat()), Math.toDegrees(announce.getLon()));
        SystemPositionAndAttitude pose = new SystemPositionAndAttitude(loc, 0, 0, 0);
        pose.setTime(announce.getTimestampMillis());

        synchronized (toSend) {
            // avoid sending announce when there are estimated states to be sent
            if (toSend.containsKey(announce.getSysName()))
                return;
            toSend.put(announce.getSysName(), pose);
        }
    }
    
    @Periodic(millisBetweenUpdates = 50)
    public void sendPositions() {
        if (!publishOn)
            return;
        
        long now = System.currentTimeMillis();
        
        if (now - lastSent < publishPeriodMillis)
            return;
        
        lastSent = now;
        
        LinkedHashMap<String, SystemPositionAndAttitude> copy = new LinkedHashMap<String, SystemPositionAndAttitude>();
        synchronized (toSend) {
            copy.putAll(toSend);
            toSend.clear();
        }
        
        SystemPositionAndAttitude mine = new SystemPositionAndAttitude(MyState.getLocation(), 0, 0, MyState.getHeadingInRadians());
        mine.setTime(System.currentTimeMillis());
        copy.put(GeneralPreferences.imcCcuName, mine);
        
        ArrayList<Observation> obsToSend = new ArrayList<>();
        for (Entry<String, SystemPositionAndAttitude> state : copy.entrySet()) {
            String type = "";
            if (state.getKey().equals(GeneralPreferences.imcCcuName)) {
                type = "CCU";            
            }
            else {
                type = IMCUtils.getSystemType(IMCDefinition.getInstance().getResolver().resolve(state.getKey()));
                
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(state.getKey());
                if (sys != null && ignoreSimulatedSystems && sys.isSimulated())
                    continue;
            }
            
            Observation obs = ObservationFactory.create(state.getKey(), type, state.getValue());
            if (obs != null)
                obsToSend.add(obs);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("["); //  [{},{},{},{}] 
        for (Observation observation : obsToSend) {
            String json = observation.toJSON();
            if (sb.length() > 1)
                sb.append(",");
            sb.append(json);
        }
        sb.append("]"); //  [{},{},{},{}]
        String toSend = sb.toString();
        
        postHttpRequest = new HttpPost(url);
        HttpResponse iGetResultCode = null;
        try {
            //    List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            //    nvps.add(new BasicNameValuePair("username", "vip"));
            //    nvps.add(new BasicNameValuePair("password", "secret"));
            //    httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            
            StringEntity  postingString = new StringEntity(toSend);
            postHttpRequest.setEntity(postingString);
            postHttpRequest.setHeader("Content-type", "application/json");
            
            long startMillis = System.currentTimeMillis();
            iGetResultCode = httpComm.getClient().execute(postHttpRequest);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, null);
            long endMillis = System.currentTimeMillis();
            
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>process [" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase() + " code was return from the server"
                        + " | took " + (endMillis - startMillis) + "ms");
            }
            else {
                NeptusLog.pub().info("<###>process sent "
                        + " | took " + (endMillis - startMillis) + "ms");
                System.out.println(toSend);
            }
            HttpEntity entity1 = iGetResultCode.getEntity();
            EntityUtils.consume(entity1);
            postHttpRequest.releaseConnection();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
