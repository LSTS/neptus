/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 17, 2015
 */
package pt.lsts.neptus.plugins.trex;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.TrexAttribute;
import pt.lsts.imc.TrexAttribute.ATTR_TYPE;
import pt.lsts.imc.TrexOperation;
import pt.lsts.imc.TrexOperation.OP;
import pt.lsts.imc.TrexToken;
import pt.lsts.imc.net.UDPTransport;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.iridium.ActivateSubscription;
import pt.lsts.neptus.comm.iridium.DeactivateSubscription;
import pt.lsts.neptus.comm.iridium.DuneIridiumMessenger;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.europtus.TokenHistory;
import pt.lsts.neptus.plugins.trex.goals.AUVDrifterSurvey;
import pt.lsts.neptus.plugins.trex.goals.AUVDrifterSurvey.PathType;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 */
@PluginDescription(name="Europtus Interface", icon="pt/lsts/neptus/plugins/trex/hal9000.png")
public class Europtus extends ConsoleInteraction implements MessageDeliveryListener, DeliveryListener {

    @NeptusProperty(category="Real Vehicles", name="First AUV (smaller surveys)")
    public String auv1 = "lauv-xplore-1";

    @NeptusProperty(category="Real Vehicles", name="Second AUV (bigger surveys)")
    public String auv2 = "lauv-xplore-2";

    @NeptusProperty(category="Europtus", name="Host and Port for local europtus server")
    public String europtus = "127.0.0.1:7030";

    @NeptusProperty(category="Europtus", name="IMC ID of Europtus")
    public int europtus_id = 24575;

    @NeptusProperty(category="Simulated Vehicles", name="First Simulator")
    public String sim_auv1 = "trex-sim-1";

    @NeptusProperty(category="Simulated Vehicles",name="Second Simulator")
    public String sim_auv2 = "trex-sim-2";

    @NeptusProperty(category="Survey Parameters", name="Smaller survey side length, in meters")
    public double survey_size = 300;

    @NeptusProperty(category="Survey Parameters", name="Ground Speed, in meters")
    public double ground_speed = 1.25;

    @NeptusProperty(category="Survey Parameters", name="Time spent at surface during popups, in seconds")
    public int popup_secs = 30;

    @NeptusProperty(category="Survey Parameters", name="Path rotation, in degrees")
    public double rotation = 0;

    @NeptusProperty(category="Survey Parameters", name="Maximum depth, in meters")
    public double max_depth = 20;    

    @NeptusProperty(category="Real Vehicles", name="Connection to be used to send Goals")
    public Connection connection_type = Connection.IMC;

    @NeptusProperty(category="Europtus", name="Forward goals to Europtus")
    public boolean forwardToEuroptus = true;

    private String europtus_host = null;
    private int europtus_port = -1;

    private TokenHistory history = new TokenHistory();
    private PlanType plan1 = null, plan2 = null;

    private HubIridiumMessenger hubMessenger = null;
    private DuneIridiumMessenger duneMessenger = null;
    private UDPTransport imcTransport = null;
    private LinkedHashMap<String, TrexToken> trex_state = new LinkedHashMap<String, TrexToken>();

    private GpsFix asFix(EstimatedState state) {
        LocationType loc = IMCUtils.getLocation(state);
        loc.convertToAbsoluteLatLonDepth();
        Calendar cal = GregorianCalendar.getInstance();

        GpsFix fix = GpsFix.create( 
                "validity", 0xFFFF, 
                "type", "MANUAL_INPUT", 
                "utc_year", cal.get(Calendar.YEAR),
                "utc_month", cal.get(Calendar.MONTH)+1,
                "utc_day", cal.get(Calendar.DATE),
                "utc_time", cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND),
                "lat", loc.getLatitudeRads(),
                "lon", loc.getLongitudeRads(),
                "height", 0,
                "satellites", 4,
                "cog", Math.toDegrees(state.getPsi()),
                "sog", 0,
                "hdop", 1,
                "vdop", 1,
                "hacc", 2,
                "vacc", 2                                    
                );

        return fix;
    }

    long lastSentPosition1 = System.currentTimeMillis(), lastSentPosition2 = System.currentTimeMillis();

    LinkedHashMap<String, Long> lastSentTimes = new LinkedHashMap<String, Long>();
    
    @Subscribe
    public void on(EstimatedState state) {
        
        if (state.getSourceName().equals(auv1) && System.currentTimeMillis() - lastSentPosition1 > 10000) {
            lastSentPosition1 = System.currentTimeMillis();
            try {
                if (!ImcMsgManager.getManager().sendMessageToSystem(asFix(state), sim_auv1));
                throw new Exception("Not able to send gps fix to simulator 1"); 
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            
            try {
                sendToEuroptus(translate("auv1.estate", state));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (state.getSourceName().equals(auv2)&& System.currentTimeMillis() - lastSentPosition2 > 10000) {
            lastSentPosition2 = System.currentTimeMillis();
            try {
                if (!ImcMsgManager.getManager().sendMessageToSystem(asFix(state), sim_auv2));
                throw new Exception("Not able to send gps fix to simulator 2");                     
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            
            try {
                sendToEuroptus(translate("auv2.estate", state));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }        
    }

    @Subscribe
    public void on(TrexOperation op) {

        if (op.getToken().getTimeline().equals("neptus") && op.getToken().getPredicate().equals("Message")) {
            for (TrexAttribute attr : op.getToken().getAttributes()) {
                if (attr.getName().equals("content")) {
                    getConsole().post(Notification.warning("Europtus", attr.getMin()).requireHumanAction(true));
                }
            }
        }
        history.store(op);

        if (op.getSrc() == europtus_id) {
            try {
                if (op.getToken().getTimeline().endsWith(".drifter")) {
                    double latRad = 0, lonRad = 0, survey_size = 0, rotationRads = 0;
                    AUVDrifterSurvey.PathType pathType = PathType.SQUARE;
                    boolean lagrangian = false;
                    double u = 0, v = 0;

                    Vector<TrexAttribute> attrs = op.getToken().getAttributes();

                    for (TrexAttribute attr : attrs) {
                        switch (attr.getName()) {
                            case "center_lat":
                                latRad = Double.parseDouble(attr.getMin()); 
                                break;
                            case "center_lon":
                                lonRad = Double.parseDouble(attr.getMin()); 
                                break;
                            case "heading":
                                rotationRads = Double.parseDouble(attr.getMin()); 
                                break;
                            case "size":
                                survey_size = Double.parseDouble(attr.getMin()); 
                                System.out.println("Received this size: "+survey_size);
                                break;
                            case "path":
                                pathType = PathType.valueOf(attr.getMin().toUpperCase());
                                break;
                            case "u":
                                u = Double.parseDouble(attr.getMin()); 
                                break;
                            case "v":
                                v = Double.parseDouble(attr.getMin()); 
                                break;
                            case "lagrangian":
                                lagrangian = attr.getMin().equals("true") || attr.getMin().equals("1");
                                break;
                            default:
                                NeptusLog.pub().warn("Unrecognized attribute: "+attr.getName());
                                break;
                        }
                    }

                    double speed = Math.sqrt(u * u + v * v);

                    AUVDrifterSurvey survey = new AUVDrifterSurvey(latRad, lonRad,
                            (float) survey_size, (float)speed, lagrangian, pathType, (float) rotationRads);

                    PlanType plan = asNeptusPlan(survey);
                    if (op.getToken().getTimeline().startsWith("auv1")) {
                        plan.setVehicle(auv1);
                        plan.setId("trex_"+auv1);
                    }
                    else if (op.getToken().getTimeline().startsWith("auv2")) {
                        plan.setVehicle(auv2);
                        plan.setId("trex_"+auv2);
                    } 

                    getConsole().getMission().addPlan(plan);
                    getConsole().getMission().save(false);
                    getConsole().warnMissionListeners();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                if (op.getToken().getTimeline().startsWith("auv1.")) {
                    op.getToken().setTimeline(op.getToken().getTimeline().substring(5));
                    sendToVehicle1(op);
                    getConsole().post(Notification.info("Europtus", "Forwarded goal of type "+op.getToken().getTimeline()+" to "+auv1));
                }

                else if (op.getToken().getTimeline().startsWith("auv2.")) {
                    op.getToken().setTimeline(op.getToken().getTimeline().substring(5));
                    sendToVehicle2(op);
                    getConsole().post(Notification.info("Europtus", "Forwarded goal of type "+op.getToken().getTimeline()+" to "+auv2));
                }
            }
            catch (Exception e) {
                getConsole().post(Notification.warning("Europtus", e.getClass().getSimpleName()+": "+e.getMessage()));
                e.printStackTrace();
            }

        }
        else {
            TrexToken token = op.getToken();
            token.setTimestamp(op.getTimestamp());
            token.setSrc(op.getSrc());
            System.out.println("Posting "+token);
            on(token);
        }
    }

    @Subscribe
    public void on(TrexToken token) {
        String src = token.getSourceName();
        
        /*
         * SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSS");
                TrexOperation inactive1 = new TrexOperation();
                inactive1.setOp(OP.POST_TOKEN);
                TrexToken tok = new TrexToken();
                tok.setTimeline("auv1.drifter");
                tok.setPredicate("Inactive");
                TrexAttribute attr = new TrexAttribute();
                attr.setName("start");
                attr.setMax(sdf.format(new Date()));          
                attr.setMin("");
                tok.setAttributes(Arrays.asList(attr));
                inactive1.setToken(tok);
         */
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
        
        TrexAttribute start = new TrexAttribute();
        start.setName("start");
        start.setMin(sdf.format(new Date(token.getTimestampMillis())));
        start.setMax(sdf.format(new Date(token.getTimestampMillis())));        
        start.setAttrType(ATTR_TYPE.STRING);
        
        try {
            if (src.equals(auv1)) {
                TrexToken copy = TrexToken.clone(token);
                Vector<TrexAttribute> attrs = copy.getAttributes();
                attrs.add(start);
                copy.setAttributes(attrs);
                copy.setTimeline("auv1."+token.getTimeline());
                trex_state.put("auv1."+token.getTimeline(), copy);
            }
            else if (src.equals(auv2)) {
                TrexToken copy = TrexToken.clone(token);
                Vector<TrexAttribute> attrs = copy.getAttributes();
                attrs.add(start);
                copy.setAttributes(attrs);                
                copy.setTimeline("auv2."+token.getTimeline());
                trex_state.put("auv2."+token.getTimeline(), copy);               
            }            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getTrexTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss");
        return sdf.format(date);
    }
    
    @Subscribe
    public void on(RemoteSensorInfo rsi) {
        
        if (rsi.getSourceName().equals(auv1) || rsi.getSourceName().equals(auv2)) {
            getConsole().post(Notification.info("AUV update", "Received update from "+rsi.getSourceName()));
            EstimatedState state = new EstimatedState();
            state.setLat(rsi.getLat());
            state.setLon(rsi.getLon());
            state.setAlt(rsi.getAlt());
            state.setPsi(rsi.getHeading());
            state.setTimestamp(rsi.getTimestamp());
            ImcMsgManager.getManager().postInternalMessage("Europtus", state);
            on(state);            
        }
    }
    
    private TrexOperation translate(String timeline, EstimatedState state) {
        TrexToken token = new TrexToken();
        TrexOperation op = new TrexOperation();
        
        token.setTimeline(timeline);
        token.setPredicate("Position");
        Vector<TrexAttribute> attrs = new Vector<TrexAttribute>();
        TrexAttribute lat = new TrexAttribute();
        lat.setName("latitude");
        LocationType loc = IMCUtils.getLocation(state);
        lat.setMin(""+loc.getLatitudeRads());
        lat.setMax(""+loc.getLatitudeRads());
        lat.setAttrType(ATTR_TYPE.FLOAT);        
        TrexAttribute lon = new TrexAttribute();
        lon.setName("longitude");
        lon.setMin(""+loc.getLongitudeRads());
        lon.setMax(""+loc.getLongitudeRads());
        lon.setAttrType(ATTR_TYPE.FLOAT);
        TrexAttribute depth = new TrexAttribute();
        depth.setName("depth");
        depth.setMin(""+state.getDepth());
        depth.setMax(""+state.getDepth());
        depth.setAttrType(ATTR_TYPE.FLOAT);
        
        TrexAttribute start = new TrexAttribute();
        start.setName("start");
        start.setMin(getTrexTime(new Date(state.getTimestampMillis())));
        start.setMax(getTrexTime(new Date(state.getTimestampMillis())));        
        start.setAttrType(ATTR_TYPE.STRING);
                
        attrs.add(depth);
        attrs.add(lon);
        attrs.add(lat);
        attrs.add(start);
        token.setAttributes(attrs);
        
        op.setToken(token);
        op.setOp(OP.POST_TOKEN);
        return op;
    }

    @Periodic(millisBetweenUpdates=10000)
    public void sendStateToEuroptus() {

        //for (String msg : new String[] {"auv1.drifter", "auv2.drifter", "auv1.estate", "auv2.estate"}) {
        for (String msg : new String[] {"auv1.drifter", "auv2.drifter"}) {
            if (trex_state.containsKey(msg)) {
                try {
                    TrexToken clone = new TrexToken(trex_state.get(msg));
                    TrexOperation op = new TrexOperation();
                    op.setToken(clone);
                    op.setOp(OP.POST_TOKEN);
                    NeptusLog.pub().info("Sending token '"+msg+"' to europtus ("+europtus_host+":"+europtus_port+")");         
                    sendToEuroptus(op);
                    System.out.println("SENDING POSITION TO EUROPTUS");
                    trex_state.remove(msg);                    
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        EstimatedState s1 = ImcMsgManager.getManager().getState(auv1).last(EstimatedState.class);
        EstimatedState s2 = ImcMsgManager.getManager().getState(auv2).last(EstimatedState.class);
                
        try {
            if (s1 != null)
                sendToEuroptus(translate("auv1.estate", s1));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        try {
            if (s2 != null)
                sendToEuroptus(translate("auv2.estate", s2));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        
    }

    public enum Connection {
        MantaIridium,
        HubIridium,
        IMC
    }

    public enum GoalsDestination {
        Vehicles,
        Europtus
    }

    // send to europtus server
    void sendToEuroptus(TrexOperation msg) throws Exception {

        System.out.println("Sending to europtus ("+europtus_host+": "+europtus_port+">>");
        System.out.println(msg.asJSON());

        if (europtus_host == null)
            throw new Exception("Europtus host and port not correctly set.");
        sendUdp(msg, europtus_host, europtus_port);

        history.store(msg);
    }

    // send to udp destination
    void sendUdp(IMCMessage msg, String host, int port) throws Exception {
        if (!getUdpTransport().sendMessage(host, port, msg))
            throw new Exception(host+":"+port+" is unreacheable.");
    }

    Collection<ImcIridiumMessage> wrap(IMCMessage msg) throws Exception {
        msg.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
        return IridiumManager.iridiumEncode(msg);
    }

    void sendToVehicle(String vehicle, IMCMessage msg) throws Exception {
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(vehicle);
        if (sys != null)
            msg.setDst(sys.getId().intValue());
        else {
            VehicleType vt = VehiclesHolder.getVehicleById(vehicle);
            if (vt != null)
                msg.setDst(vt.getImcId().intValue());
            else
                throw new Exception("Vehicle "+vehicle+" is unknown.");
        }

        NeptusLog.pub().info("Send "+msg.getAbbrev()+" to "+vehicle+" ("+msg.getDst()+") using "+connection_type);
        switch (connection_type) {
            case IMC:
                ImcMsgManager.getManager().sendMessageToSystem(msg, vehicle, this);
                break;
            case HubIridium:
                for (ImcIridiumMessage m : wrap(msg))                
                    getHubMessenger().sendMessage(m);
                break;
            case MantaIridium:
                for (ImcIridiumMessage m : wrap(msg))                
                    getDuneMessenger().sendMessage(m);
                break;
            default:
                break;
        }
    }

    void sendToVehicle1(IMCMessage msg) throws Exception {
        System.out.println("Sending to vehicle 1 ("+auv1+"): \n"+IMCUtil.getAsHtml(msg));
        sendToVehicle(auv1, msg);
        try {
            if (!ImcMsgManager.getManager().sendMessageToSystem(msg, sim_auv1));
            throw new Exception("Not able to send message to vehicle 1");
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    void sendToVehicle2(IMCMessage msg) throws Exception {
        System.out.println("Sending to vehicle 2 ("+auv2+"): "+msg);
        sendToVehicle(auv2, msg);

        try {
            if (!ImcMsgManager.getManager().sendMessageToSystem(msg, sim_auv2));
            throw new Exception("Not able to send message to vehicle 2");
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }

    }

    @Override
    public void deliveryError(IMCMessage message, Object error) {
        if (error instanceof Throwable)
            deliveryResult(ResultEnum.Error, new Exception((Throwable)error));
        else
            deliveryResult(ResultEnum.Error, new Exception(""+error));
    }

    @Override
    public void deliverySuccess(IMCMessage message) {
        deliveryResult(ResultEnum.Success, null);
    }

    @Override
    public void deliveryTimeOut(IMCMessage message) {
        deliveryResult(ResultEnum.TimeOut, new Exception("Time out while sending "+message.getAbbrev()));
    }

    @Override
    public void deliveryUncertain(IMCMessage message, Object msg) {
        deliveryResult(ResultEnum.Success, null);
    }

    @Override
    public void deliveryUnreacheable(IMCMessage message) {
        deliveryResult(ResultEnum.Unreachable, new Exception("Destination is unreacheable"));
    }

    @Override
    public void deliveryResult(ResultEnum result, Exception error) {

        //NeptusLog.pub().info("Delivery result: "+result, error);
    }

    /**
     * @return the hubMessenger
     */
    public synchronized HubIridiumMessenger getHubMessenger() {
        if (hubMessenger == null)
            hubMessenger = new HubIridiumMessenger();
        return hubMessenger;
    }

    /**
     * @return the duneMessenger
     */
    public synchronized DuneIridiumMessenger getDuneMessenger() {
        if (duneMessenger == null)
            duneMessenger = new DuneIridiumMessenger();
        return duneMessenger;
    }

    public synchronized UDPTransport getUdpTransport() {
        if (imcTransport == null)
            imcTransport = new UDPTransport();
        return imcTransport;
    }

    @Override
    public void propertiesChanged() {
        try {
            europtus_host = europtus.split("\\:")[0];
            europtus_port = Integer.parseInt(europtus.split("\\:")[1]);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error parsing Europtus host:port", e);
            europtus_host = null;
            europtus_port = -1;
        }
    }

    public void surveyLocation(final LocationType loc) {

        Runnable r = new Runnable() {

            @Override
            public void run() {

                if (forwardToEuroptus) {
                    TrexOperation op = new TrexOperation();
                    op.setOp(OP.POST_TOKEN);
                    TrexToken token = new TrexToken();
                    token.setTimeline("whale.estate");
                    token.setPredicate("Position");
                    ArrayList<TrexAttribute> attrs = new ArrayList<TrexAttribute>();
                    TrexAttribute lat = new TrexAttribute();
                    lat.setName("latitude");
                    lat.setMin(String.format(Locale.US, "%.8f", loc.getLatitudeRads()));
                    lat.setMax(String.format(Locale.US, "%.8f", loc.getLatitudeRads()));
                    lat.setAttrType(ATTR_TYPE.FLOAT);
                    attrs.add(lat);

                    TrexAttribute lon = new TrexAttribute();
                    lon.setName("longitude");
                    lon.setMin(String.format(Locale.US, "%.8f", loc.getLongitudeRads()));
                    lon.setMax(String.format(Locale.US, "%.8f", loc.getLongitudeRads()));
                    lon.setAttrType(ATTR_TYPE.FLOAT);
                    attrs.add(lon);
                    
                    TrexAttribute start = new TrexAttribute();
                    start.setName("start");
                    start.setMin(getTrexTime(new Date()));
                    start.setMax(getTrexTime(new Date()));        
                    start.setAttrType(ATTR_TYPE.STRING);

                    attrs.add(start);
                    
                    token.setAttributes(attrs);
                    op.setToken(token);
                    System.out.println(IMCUtil.getAsHtml(op));
                    try {
                        sendToEuroptus(op);                                 
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(
                                ex.getClass().getSimpleName() + " while sending whale position: " + ex.getMessage(), ex);
                        GuiUtils.errorMessage(getConsole(), "Error sending whale position",
                                ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }   
                }
                else {

                    AUVDrifterSurvey survey1 = new AUVDrifterSurvey(loc.getLatitudeRads(), loc.getLongitudeRads(),
                            (float) survey_size, 0f, false, AUVDrifterSurvey.PathType.SQUARE_TWICE, (float) Math
                            .toRadians(rotation));

                    AUVDrifterSurvey survey2 = new AUVDrifterSurvey(loc.getLatitudeRads(), loc.getLongitudeRads(),
                            (float) (survey_size * 2), 0f, false, AUVDrifterSurvey.PathType.SQUARE, (float) Math
                            .toRadians(rotation));

                    plan1 = asNeptusPlan(survey1);
                    plan1.setVehicle(auv1);
                    plan1.setId("trex_"+auv1);

                    plan2 = asNeptusPlan(survey2);
                    plan2.setVehicle(auv2);
                    plan2.setId("trex_"+auv2);

                    getConsole().getMission().addPlan(plan1);
                    getConsole().getMission().addPlan(plan2);
                    getConsole().getMission().save(false);
                    getConsole().warnMissionListeners();
                    
                    try {
                        sendToVehicle1(survey1.asIMCMsg());
                        sendToVehicle2(survey2.asIMCMsg());                
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(
                                ex.getClass().getSimpleName() + " while sending survey command: " + ex.getMessage(), ex);
                        GuiUtils.errorMessage(getConsole(), "Error sending survey command",
                                ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }   
                }                
            }
        };

        Thread t = new Thread(r, "Europtus Survey Request");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3)
            return;

        JPopupMenu popup = new JPopupMenu();
        final LocationType loc = source.getRealWorldLocation(event.getPoint());

        popup.add("Enable TREX").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("TREX");
                EntityParameter param = new EntityParameter("Active", "true");
                Vector<EntityParameter> p = new Vector<>();
                p.add(param);
                setParams.setParams(p);
                try {
                    sendToVehicle1(setParams.cloneMessage());
                    sendToVehicle2(setParams.cloneMessage());
                }
                catch (Exception ex) {
                    NeptusLog.pub().error(
                            ex.getClass().getSimpleName() + " while enabling T-REX: " + ex.getMessage(), ex);
                    GuiUtils.errorMessage(getConsole(), "Error enabling T-REX",
                            ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        });

        popup.add("Disable TREX").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("TREX");
                EntityParameter param = new EntityParameter("Active", "false");
                Vector<EntityParameter> p = new Vector<>();
                p.add(param);
                setParams.setParams(p);

                try {
                    sendToVehicle1(setParams.cloneMessage());
                    sendToVehicle2(setParams.cloneMessage());
                }
                catch (Exception ex) {
                    NeptusLog.pub().error(
                            ex.getClass().getSimpleName() + " while disabling T-REX: " + ex.getMessage(), ex);
                    GuiUtils.errorMessage(getConsole(), "Error disabling T-REX",
                            ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        });

        JMenu menu = new JMenu("Survey around...");
        menu.add("this location").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                surveyLocation(loc);
            }
        });

        menu.addSeparator();

        Vector<MarkElement> marks = MapGroup.getMapGroupInstance(getConsole().getMission()).getAllObjectsOfType(
                MarkElement.class);

        for (MarkElement elem : marks) {
            menu.add(elem.getId()).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    surveyLocation(new LocationType(elem.getCenterLocation().convertToAbsoluteLatLonDepth()));
                }
            });
        }

        popup.addSeparator();
        popup.add(menu);

        popup.add("Send initial observations").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSS");
                TrexOperation inactive1 = new TrexOperation();
                inactive1.setOp(OP.POST_TOKEN);
                TrexToken tok = new TrexToken();
                tok.setTimeline("auv1.drifter");
                tok.setPredicate("Inactive");
                TrexAttribute attr = new TrexAttribute();
                attr.setName("start");
                attr.setMax(sdf.format(new Date()));          
                attr.setMin("");
                tok.setAttributes(Arrays.asList(attr));
                inactive1.setToken(tok);

                try {
                    sendToEuroptus(inactive1);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }

                TrexOperation inactive2 = new TrexOperation();
                inactive1.setOp(OP.POST_TOKEN);
                TrexToken tok2 = new TrexToken();
                TrexAttribute attr2 = new TrexAttribute();
                attr2.setName("start");
                attr2.setMax(sdf.format(new Date()));               
                attr2.setMin("");
                tok2.setAttributes(Arrays.asList(attr2));
                tok2.setTimeline("auv2.drifter");
                tok2.setPredicate("Inactive");
                inactive2.setToken(tok2);

                try {
                    sendToEuroptus(inactive2);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        popup.add("Subscribe Iridium Updates").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ActivateSubscription activate = new ActivateSubscription();
                activate.setDestination(0xFF);
                activate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                try {
                    IridiumManager.getManager().send(activate);
                    getConsole().post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "+IridiumManager.getManager().getCurrentMessenger().getName()));
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }                
            }
        });
        
        popup.add("Unsubscribe Iridium Updates").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DeactivateSubscription deactivate = new DeactivateSubscription();
                deactivate.setDestination(0xFF);
                deactivate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                try {
                    IridiumManager.getManager().send(deactivate);
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }             
            }
        });
        popup.addSeparator();
        popup.add("Plug-in settings").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(Europtus.this, true);
            }
        });

        popup.show(event.getComponent(), event.getX(), event.getY());
    }

    private PlanType asNeptusPlan(AUVDrifterSurvey survey) {

        PlanCreator pc = new PlanCreator(getConsole().getMission());
        pc.setSpeed(new SpeedType(1.25, Units.MPS));
        pc.setLocation(survey.getLocation());
        AffineTransform transform = new AffineTransform();
        transform.rotate(survey.getRotationRads());
        PathIterator it = survey.getShape().getPathIterator(transform);
        double[] coords = new double[6];        

        while(!it.isDone()) {
            it.currentSegment(coords);
            
            LocationType loc = new LocationType(survey.getLocation());
            loc.translatePosition(-coords[1], coords[0], 0);
            loc.convertToAbsoluteLatLonDepth();
            pc.setLocation(loc);
            pc.setDepth(max_depth/2);
            pc.addManeuver("YoYo", "amplitude", max_depth/2 - 1, "pitchAngle", Math.toRadians(15));
            pc.setDepth(0);
            pc.addManeuver("Elevator", "startFromCurrentPosition", true);
            pc.addManeuver("StationKeeping", "duration", 30);
            it.next();
        }
        pc.addManeuver("StationKeeping", "duration", 0);

        return pc.getPlan();
    }


    @Override
    public void initInteraction() {
        propertiesChanged();
    }

    @Override
    public void cleanInteraction() {

    }
    
    public static void main(String[] args) {
        System.out.println(new Date(1437150669000l));
    }
}
