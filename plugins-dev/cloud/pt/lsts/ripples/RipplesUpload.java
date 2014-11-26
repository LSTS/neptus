/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Nov 9, 2014
 */
package pt.lsts.ripples;

import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ImageUtils;

import com.firebase.client.Firebase;
import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Uploader", icon = "pt/lsts/ripples/ripples_on.png")
public class RipplesUpload extends ConsolePanel {

    private JCheckBoxMenuItem menuItem;
    private ImageIcon onIcon, offIcon;
    private final String firebasePath = "https://neptus.firebaseio-demo.com/";
    private Firebase firebase = null;
    private LinkedHashMap<String, PlanControlState> planStates = new LinkedHashMap<String, PlanControlState>();

    private static final long serialVersionUID = -8036937519999303108L;

    @NeptusProperty
    private boolean synch = false;

    /**
     * @param console
     */
    public RipplesUpload(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        if (synch)
            stopSynch();
    }

    private LinkedHashMap<String, SystemPositionAndAttitude> toSend = new LinkedHashMap<String, SystemPositionAndAttitude>();

    @Subscribe
    public void on(EstimatedState state) {
        if (!synch)
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
        if (!synch)
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

    @Subscribe
    public void on(PlanControlState pcs) {
        if (!synch)
            return;

        synchronized (planStates) {
            planStates.put(pcs.getSourceName(), pcs);
        }
    }

    @Periodic(millisBetweenUpdates = 10000)
    public void sendPlans() {
        if (!synch)
            return;

        LinkedHashMap<String, PlanControlState> copy = new LinkedHashMap<String, PlanControlState>();
        synchronized (planStates) {
            copy.putAll(planStates);
            planStates.clear();
        }
        for (PlanControlState pcs : copy.values()) {
            Firebase planRef = firebase.child("assets/" + pcs.getSourceName() + "/plan").getRef();

            // no plan being executed
            if (pcs.getState() == STATE.BLOCKED || pcs.getState() == STATE.READY) {
                planRef.setValue(null);
            }
            else if (!pcs.getPlanId().isEmpty()) {
                planRef.child("id").setValue(pcs.getPlanId());
                planRef.child("progress").setValue(String.format("%.1f", pcs.getPlanProgress()));

                if (getConsole().getMission().getIndividualPlansList().containsKey(pcs.getPlanId())) {
                    PlanType pt = getConsole().getMission().getIndividualPlansList().get(pcs.getPlanId());
                    Vector<double[]> locs = new Vector<double[]>();
                    for (ManeuverLocation m : PlanUtil.getPlanWaypoints(pt)) {
                        LocationType loc = m.convertToAbsoluteLatLonDepth();
                        locs.add(new double[] { loc.getLatitudeDegs(), loc.getLongitudeDegs() });
                    }
                    planRef.child("path").setValue(locs);
                }
            }
        }
    }

    @Periodic(millisBetweenUpdates = 500)
    public void sendPositions() {
        if (!synch)
            return;
        LinkedHashMap<String, SystemPositionAndAttitude> copy = new LinkedHashMap<String, SystemPositionAndAttitude>();
        synchronized (toSend) {
            copy.putAll(toSend);
            toSend.clear();
        }

        for (Entry<String, SystemPositionAndAttitude> state : copy.entrySet()) {
            Map<String, Object> assetState = new LinkedHashMap<String, Object>();
            Map<String, Object> tmp = new LinkedHashMap<String, Object>();
            tmp.put("latitude", state.getValue().getPosition().getLatitudeDegs());
            tmp.put("longitude", state.getValue().getPosition().getLongitudeDegs());
            tmp.put("heading", Math.toDegrees(state.getValue().getYaw()));
            tmp.put("altitude", state.getValue().getAltitude());
            tmp.put("speed", state.getValue().getU());
            tmp.put("depth", state.getValue().getV());
            assetState.put("position", tmp);
            assetState.put("updated_at", state.getValue().getTime());
            assetState.put("type",
                    IMCUtils.getSystemType(IMCDefinition.getInstance().getResolver().resolve(state.getKey())));
            synchronized (firebase) {
                if (firebase != null) {
                    firebase.child("assets/" + state.getKey()).getRef().updateChildren(assetState);
                }
            }
        }
    }

    @Override
    public void initSubPanel() {
        onIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_on.png", 16, 16);
        offIcon = ImageUtils.getScaledIcon("pt/lsts/ripples/ripples_off.png", 16, 16);
        menuItem = addCheckMenuItem("Advanced>Ripples>Start synch", offIcon, new CheckMenuChangeListener() {

            @Override
            public void menuUnchecked(ActionEvent e) {
                menuItem.setText("Start synch");
                menuItem.setIcon(offIcon);
                stopSynch();
            }

            @Override
            public void menuChecked(ActionEvent e) {
                menuItem.setText("Stop synch");
                menuItem.setIcon(onIcon);
                startSynch();
            }
        });

        if (synch)
            menuItem.doClick();
    }

    private void startSynch() {
        NeptusLog.pub().info("Started synch'ing with ripples.");
        firebase = new Firebase(firebasePath);
        Firebase.goOnline();
        synch = true;
    }

    private void stopSynch() {
        NeptusLog.pub().info("Stopped synch'ing with ripples.");
        firebase = null;
        Firebase.goOffline();
        synch = false;
    }

}
