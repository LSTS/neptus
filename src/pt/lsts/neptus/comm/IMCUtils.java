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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto and pdias
 * 2007/07/05
 */
package pt.lsts.neptus.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.tree.DefaultAttribute;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCFieldType;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.types.PlanSpecificationAdapter;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.editor.AngleEditorDegs;
import pt.lsts.neptus.gui.editor.AngleEditorRadsShowDegrees;
import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.messages.Enumerated;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.CompassCalibration;
import pt.lsts.neptus.mp.maneuvers.Elevator;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.FollowSystem;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.HeadingSpeedDepth;
import pt.lsts.neptus.mp.maneuvers.IMCSerialization;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.PopUp;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.mp.maneuvers.Unconstrained;
import pt.lsts.neptus.mp.maneuvers.VehicleFormation;
import pt.lsts.neptus.mp.maneuvers.YoYo;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.ConditionType;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.PropertiesLoader;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LogUtils;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

public class IMCUtils {

    public static String translateImcIdToSystem(int imcId) {
        return translateImcIdToSystem(new ImcId16(imcId));
    }

    public static String translateImcIdToSystem(ImcId16 imcId) {
        ImcSystem sys = ImcSystemsHolder.lookupSystem(imcId);
        if (sys != null)
            return sys.getName();

        VehicleType veh = VehiclesHolder.getVehicleWithImc(imcId);
        if (veh != null)
            return veh.getName();

        return "";
    }

    public static ImcId16 translateSystemToImcId(String system) {
        ImcSystem sysList = ImcSystemsHolder.lookupSystemByName(system);
        if (sysList != null)
            return sysList.getId();

        VehicleType veh = VehiclesHolder.getVehicleById(system);
        if (veh != null)
            return (veh.getImcId() != ImcId16.NULL_ID) ? veh.getImcId() : ImcId16.NULL_ID;

        return ImcId16.NULL_ID;
    }

    /**
     * @param name 
     * @return 
     * 
     */
    public static String reduceSystemName(String name) {
        if (name.length() <= 9)
            return name;

        name = name.replaceAll("^lauv-", "");
        if (name.length() <= 9)
            return name;

        name = name.replaceAll("[- _|]", "");
        if (name.length() <= 9)
            return name;

        String ret = name.substring(Math.max(0, name.length() - 9));
        ret = ret.replaceAll("^[- _|]", "");
        return ret;
    }

    // LSF
    public static void writeAsLsf(IMCMessage message, OutputStream os) {
        IMCOutputStream ios = new IMCOutputStream(os);
        try {
            message.serialize(ios);
            //            os.write(sb.getBuffer());
            //            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LSF
    public static IMCMessage[] parseLsf(InputStream is) throws IOException {
        Vector<IMCMessage> msgs = new Vector<IMCMessage>();
        IMCDefinition imcDef = IMCDefinition.getInstance();
        while (is.available() > 0) {
            try {
                IMCMessage m = imcDef.nextMessage(is);
                msgs.add(m);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return msgs.toArray(new IMCMessage[0]);
    }

    // TXT
    public static void writeAsTxt(IMCMessage message, OutputStream os) {
        try {
            message.dump(os);
            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    public static Vector<IMCMessage> extractMessagesFromMessageListMsg(IMCMessage list) {
        Vector<IMCMessage> ret = new Vector<IMCMessage>();
        if (list == null)
            return ret;
        while (true) {
            IMCMessage inner = list.getMessage("msg");
            if (inner != null)
                ret.add(inner);
            list = list.getMessage("next");
            if (list == null)
                break;
        }
        return ret;
    }

    /**
     * 
     */
    public static IMCMessage packAsMessageList(Collection<IMCMessage> messages) {
        IMCMessage m = IMCDefinition.getInstance().create("MessageList");
        IMCMessage prev = null, first = m;

        for (IMCMessage msg : messages) {
            m.setValue("msg", msg);
            m.setValue("next", null);

            if (prev != null)
                prev.setValue("next", m);

            prev = m;
            m = IMCDefinition.getInstance().create("MessageList");
        }
        return first;
    }

    // IMC-XML
    public static Document getAsImcXml(IMCMessage[] messages) {
        Document doc = DocumentHelper.createDocument();
        Element el = DocumentHelper.createElement("imc");
        doc.setRootElement(el);

        el.addAttribute("version", IMCDefinition.getInstance().getVersion());

        for (IMCMessage msg : messages) {			
            Element msgEl = el.addElement("message");

            fillInMessage(msg, msgEl);

            try {
                msgEl.addAttribute("time", msg.getHeader().getString("timestamp"));
                msgEl.addAttribute("src", msg.getHeader().getString("src"));
                msgEl.addAttribute("dst", msg.getHeader().getString("dst"));				
            }
            catch (Exception e) {
                // e.printStackTrace();
                NeptusLog.pub().error(e.getStackTrace());
            }
        }
        return doc;
    }

    public static String getAsHtml(IMCMessage message) {
        return "<html><h1>" + message.getAbbrev() + "</h1>" + getAsInnerHtml(message) + "</html>";
    }

    private static String getAsInnerHtml(IMCMessage msg) {
        if (msg == null)
            return "null";
        String ret = "<table border=1><tr bgcolor='blue'><th>" + msg.getAbbrev() + "</th><th>"
                + msg.getFieldNames().length + " fields</th></tr>";

        for (String fieldName : msg.getFieldNames()) {
            String value = msg.getString(fieldName);	        
            if (msg.getTypeOf(fieldName).equalsIgnoreCase("message") && msg.getValue(fieldName) != null)
                value = getAsInnerHtml(msg.getMessage(fieldName));

            ret += "<tr><td>" + fieldName + "=</td><td>" + value + "</td></tr>";
        }

        return ret + "</table>";
    }

    private static void fillInMessage(IMCMessage msg, Element el) {
        el.addAttribute("name", msg.getMessageType().getShortName());
        for (String fieldName : msg.getMessageType().getFieldNames()) {
            Element fieldEl = el.addElement("field");

            fieldEl.addAttribute("name", fieldName);
            fieldEl.addAttribute("type", msg.getMessageType().getFieldType(fieldName).getTypeName());

            if (!msg.getMessageType().getFieldType(fieldName).equals("message")) {
                fieldEl.setText(msg.getString(fieldName));
            }
            else {
                fillInMessage(msg.getMessage(fieldName), el.addElement("inline"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static IMCMessage parseMessage(Element elem) {
        String name = elem.selectSingleNode("@name").getText();
        IMCMessage msg = IMCDefinition.getInstance().create(name);
        Node timeEl = elem.selectSingleNode("@time");
        Node srcEl = elem.selectSingleNode("@src");
        Node dstEl = elem.selectSingleNode("@dst");
        long time = System.currentTimeMillis();
        if (timeEl != null) {
            time = (long) (Double.parseDouble(timeEl.getText()) * 1000.0);
            msg.getHeader().setTimestamp(time/1000);
        }
        if (srcEl != null)
            msg.getHeader().setValue("src", srcEl.getText());

        if (dstEl != null)
            msg.getHeader().setValue("dst", dstEl.getText());

        List<Object> fieldElems = elem.selectNodes("field");
        for (Object mo : fieldElems) {
            Element fieldElem = (Element) mo;
            String ftype = fieldElem.selectSingleNode("@type").getText();
            String fname = fieldElem.selectSingleNode("@name").getText();
            if (!ftype.equals("message")) {
                if (msg.getMessageType().getFieldMeanings(fname) != null) {
                    long val = msg.getMessageType().getFieldMeanings(fname).get(fieldElem.getText());
                    msg.setValue(fname, val);	
                }
                //                else {
                //                    NativeType val = NativeTypeFactory.valueOf(ftype, fieldElem.getText());
                //                    msg.setValue(fname, val);	
                //                }
            }
            else {
                IMCMessage inline = parseMessage((Element)fieldElem.selectSingleNode("inline"));                
                msg.setValue(fname, inline);
            }
        }
        return msg;
    }

    @SuppressWarnings("unchecked")
    public static IMCMessage[] parseImcXml(String xml) throws DocumentException{
        Vector<IMCMessage> messages = new Vector<IMCMessage>();

        Document doc = DocumentHelper.parseText(xml);

        List<Object> msgs = doc.selectNodes("imc/message");
        for (Object mo : msgs)
            messages.add(parseMessage((Element) mo));

        return messages.toArray(new IMCMessage[0]);
    }

    public static IMCMessage generateReportedState(LocationType lt, String sid, double roll, double pitch, double yaw, double time) {
        double[] lld = lt.getAbsoluteLatLonDepth();

        IMCMessage msg = IMCDefinition.getInstance().create("ReportedState");
        msg.setValue("lat", Math.toRadians(lld[0]));
        msg.setValue("lon", Math.toRadians(lld[1]));
        msg.setValue("depth", lld[2]);
        sid = (sid != null) ? sid : "null";
        msg.setValue("sid", sid);
        msg.setValue("s_type", 254);

        msg.setValue("roll", roll);
        msg.setValue("pitch", pitch);
        msg.setValue("yaw", yaw);

        msg.setValue("rcpTime", time);

        return msg;
    }

    public static IMCMessage generateHomeReferenceMessage(MissionType mt) {
        MissionType miss = mt;
        if (miss == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send HomeRef", "Missing attached mission!");
            return null;
        }

        HomeReference homeRef = miss.getHomeRef();

        LocationType absLoc = new LocationType();
        absLoc.setLocation(homeRef);
        absLoc = absLoc.getNewAbsoluteLatLonDepth();

        IMCMessage msgHomeRef = IMCDefinition.getInstance().create("HomeRef");
        msgHomeRef.setValue("lat", absLoc.getLatitudeRads());

        msgHomeRef.setValue("lon", absLoc.getLongitudeRads());
        msgHomeRef.setValue("depth", absLoc.getDepth());

        return msgHomeRef;
    }

    public static IMCMessage[] generateLblBeaconSetup(MissionType mt) {
        MissionType miss = mt;

        LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();
        LinkedHashMap<String, MapMission> mapList = miss.getMapsList();
        Vector<IMCMessage> msgs = new Vector<IMCMessage>();

        for (MapMission mpm : mapList.values()) {
            // LinkedHashMap traList = mpm.getMap().getTranspondersList();
            LinkedHashMap<String, TransponderElement> transList = mpm.getMap().getTranspondersList();
            for (TransponderElement tmp : transList.values()) {
                transpondersList.add(tmp);
            }
        }

        for (int i = 0; i < transpondersList.size(); i++) {
            TransponderElement transp = transpondersList.get(i);
            LocationType tPos = transp.getCenterLocation();
            LocationType absLoc = tPos.getNewAbsoluteLatLonDepth();

            IMCMessage msgBeaconSetup = IMCDefinition.getInstance().create("LblBeaconSetup");
            // msgBeaconSetup.setValue("id", new NativeUINT8(i));
            String nStr = transp.getId()+"";

            msgBeaconSetup.setValue("beacon", nStr);

            msgBeaconSetup.setValue("lat", absLoc.getLatitudeRads());
            msgBeaconSetup.setValue("lon", absLoc.getLongitudeRads());
            msgBeaconSetup.setValue("depth",absLoc.getDepth());

            int queryChannel = 0, replyChannel = 0, transponderDelay = 0, id;
            PropertiesLoader propConf = transp.getPropConf();
            if (propConf == null) {
                GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send transponders",
                        "Bad configuration for transponder " + transp.getId() + "!");
                return null;
            }
            String propId = propConf.getProperty("id");
            String prop1 = propConf.getProperty("interrogation channel");
            String prop2 = propConf.getProperty("reply channel");
            String prop3 = propConf.getProperty("transponder delay (msecs.)");
            try {
                id = (int) Double.parseDouble(propId);
                queryChannel = (int) Double.parseDouble(prop1);
                replyChannel = (int) Double.parseDouble(prop2);
                transponderDelay = (int) Double.parseDouble(prop3);
            }
            catch (NumberFormatException e2) {
                GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send transponders",
                        "Bad configuration parsing for transponder " + transp.getId() + "!");
                NeptusLog.pub().error(e2.getStackTrace());
                return null;
            }

            msgBeaconSetup.setValue("id", id);

            msgBeaconSetup.setValue("query_channel", queryChannel);
            msgBeaconSetup.setValue("reply_channel", replyChannel);
            msgBeaconSetup.setValue("transponder_delay", transponderDelay);

            msgs.add(msgBeaconSetup);

        }

        return msgs.toArray(new IMCMessage[0]);
    }

    /**
     * @param plan
     * @return
     */
    public static IMCMessage generatePlanSpecification(PlanType plan) {
        if (plan == null || !(plan instanceof PlanType))
            return null;

        PlanType iPlan = plan;
        IMCMessage msgPlanSpecification = iPlan.asIMCPlan();
        return msgPlanSpecification;        
    }

    public static PlanType parsePlanSpecification(MissionType mission, IMCMessage msg) {
        PlanType plan = new PlanType(mission);
        PlanSpecificationAdapter adapter = new PlanSpecificationAdapter(msg);

        plan.setId(adapter.getPlanId());

        Vector<IMCMessage> planStartActions = new Vector<>();
        Vector<IMCMessage> planEndActions = new Vector<>();

        long vid = msg.getSrc();
        ImcId16 imcid = new ImcId16(vid);
        VehicleType sender = VehiclesHolder.getVehicleWithImc(imcid);
        if (sender != null)
            plan.setVehicle(sender);
        else {
            vid = msg.getDst();
            imcid = new ImcId16(vid);
            sender = VehiclesHolder.getVehicleWithImc(imcid);
            if (sender != null)
                plan.setVehicle(sender);
        }

        //Parse plan actions
        Vector<IMCMessage> sact = adapter.getPlanStartActions();
        Vector<IMCMessage> eact = adapter.getPlanEndActions();
        if (sact != null)
            planStartActions.addAll(sact);
        if (eact != null)
            planEndActions.addAll(eact);
        if (!planStartActions.isEmpty())
            plan.getStartActions().parseMessages(planStartActions);
        if (!planEndActions.isEmpty())
            plan.getEndActions().parseMessages(planEndActions);

        Map<String, IMCMessage> msgManeuvers = adapter.getAllManeuvers();

        for (Entry<String, IMCMessage> entry : msgManeuvers.entrySet()) {
            Maneuver maneuver = parseManeuver(entry.getValue());
            maneuver.setId(entry.getKey());
            Vector<IMCMessage> startActions = adapter.getManeuverStartActions(entry.getKey());
            if (startActions != null)
                maneuver.getStartActions().parseMessages(startActions);
            Vector<IMCMessage> endActions = adapter.getManeuverEndActions(entry.getKey());
            if (endActions != null)
                maneuver.getEndActions().parseMessages(endActions);
            plan.getGraph().addManeuver(maneuver);
        }

        for (PlanSpecificationAdapter.Transition trans : adapter.getAllTransitions())
            plan.getGraph().addTransition(trans.getSourceManeuver(), trans.getDestManeuver(), trans.getConditions());

        plan.getGraph().setInitialManeuver(adapter.getFirstManeuverId());

        return plan;
    }

    /**
     * See {@link LogUtils#parseManeuver}
     */
    public static Maneuver parseManeuver(IMCMessage message) {
        Maneuver m = null;

        if (message.getAbbrev().equalsIgnoreCase("goto"))
            m = new Goto();
        if (message.getAbbrev().equalsIgnoreCase("loiter"))
            m = new Loiter();
        else if (message.getAbbrev().equalsIgnoreCase("teleoperation"))
            m = new Unconstrained();
        else if (message.getAbbrev().equalsIgnoreCase("elementalmaneuver"))
            m = new HeadingSpeedDepth();
        else if (message.getAbbrev().equalsIgnoreCase("yoyo"))
            m = new YoYo();
        else if (message.getAbbrev().equalsIgnoreCase("popup"))
            m = new PopUp();
        else if (message.getAbbrev().equalsIgnoreCase("stationkeeping"))
            m = new StationKeeping();
        else if (message.getAbbrev().equalsIgnoreCase("rowsmaneuver") || message.getAbbrev().equalsIgnoreCase("rows"))
            m = new RowsManeuver();
        else if (message.getAbbrev().equalsIgnoreCase("followpath")) //TODO handle RIPattern and alike
            m = FollowPath.createFollowPathOrPattern(message);
        else if (message.getAbbrev().equalsIgnoreCase("followtrajectory"))
            m = new FollowTrajectory();
        else if (message.getAbbrev().equalsIgnoreCase("followsystem"))
            m = new FollowSystem();
        else if (message.getAbbrev().equalsIgnoreCase("vehicleformation"))
            m = new VehicleFormation();
        else if (message.getAbbrev().equalsIgnoreCase("elevator"))
            m = new Elevator();
        else if (message.getAbbrev().equalsIgnoreCase("compasscalibration"))
            m = new CompassCalibration();

        if (m != null)
            ((IMCSerialization) m).parseIMCMessage(message);
        else
            m = new Unconstrained();
        return m;
    }

    public static void parseManeuverTransition(String sourceManeuver, IMCMessage message, Vector<TransitionType> transitions) {
        if (message == null)
            return;
        String destMan = message.getAsString("dest_man");
        if (!"_done_".equalsIgnoreCase(destMan)) {
            TransitionType tt = new TransitionType(sourceManeuver, message.getAsString("dest_man"));
            ConditionType condition = new ConditionType();
            condition.setCondition(message.getAsString("conditions"));
            tt.setCondition(condition);
            transitions.add(tt);
        }
        IMCMessage next = message.getMessage("next");
        if (next != null)
            parseManeuverTransition(sourceManeuver, next, transitions);
    }

    public static IMCMessage generateLoadMissionCmd(PlanType plan) {
        IMCMessage specs = generatePlanSpecification(plan);
        IMCMessage msgPlanCommand = IMCDefinition.getInstance().create("PlanCommand");
        boolean isPlan = true;
        if (msgPlanCommand == null) {
            msgPlanCommand = IMCDefinition.getInstance().create("MissionCommand");
            isPlan = false;
        }
        if (isPlan)
            msgPlanCommand.setValue("plan_id", plan.getId());
        else
            msgPlanCommand.setValue("mission_id", plan.getId());
        msgPlanCommand.setValue("maneuver_id", "");

        msgPlanCommand.setValue("command","LOAD");

        msgPlanCommand.setValue("argument", specs);

        return msgPlanCommand;
    }

    public static LocationType lookForStartPosition(MissionType mt) {
        MarkElement startEl = null;
        Vector<MarkElement> marks = MapGroup.getMapGroupInstance(mt).getAllObjectsOfType(MarkElement.class);

        for (MarkElement el : marks) {
            if (el.getId().equals("start")) {
                startEl = el;
                break;
            }
        }
        LocationType absLoc = new LocationType();

        if (startEl == null && mt != null) {
            NeptusLog.pub().warn("Couldn't find startup position. Returning homeref position");
            HomeReference homeRef = mt.getHomeRef();
            absLoc = homeRef.getNewAbsoluteLatLonDepth();
            return null;
        }
        else if (startEl != null) {
            NeptusLog.pub().warn("Unable to retrieve homeref from mission type.");
            
            absLoc.setLocation(startEl.getCenterLocation());
            absLoc = absLoc.getNewAbsoluteLatLonDepth();
        }
        return absLoc;
    }

    public static IMCMessage generateNavStartupMessage(MissionType mt) {
        LocationType absLoc = lookForStartPosition(mt);

        IMCMessage msgNavStartPoint = IMCDefinition.getInstance().create("NavigationStartupPoint");
        msgNavStartPoint.setValue("lat", absLoc.getLatitudeRads());
        msgNavStartPoint.setValue("lon",absLoc.getLongitudeRads());
        msgNavStartPoint.setValue("depth", absLoc.getDepth());

        return msgNavStartPoint;
    }

    public static boolean sendMessage(IMCMessage message, InetSocketAddress target) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream sb = new IMCOutputStream(baos);
        try {
            message.serialize(sb);
            sendUdpMsg(target, baos.toByteArray(),baos.size());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }

        return true;
    }

    public static boolean sendMissionConfiguration(MissionType mt, String planId, InetSocketAddress destination) {

        if (!mt.getIndividualPlansList().containsKey(planId)) {
            NeptusLog.pub().error("Couldn't find the plan '" + planId + "' in the given mission");
            return false;
        }

        Vector<IMCMessage> msgs = new Vector<IMCMessage>();

        IMCMessage msg = generateHomeReferenceMessage(mt);
        msgs.add(msg);

        msg = generateNavStartupMessage(mt);
        msgs.add(msg);

        IMCMessage[] transps = generateLblBeaconSetup(mt);
        for (IMCMessage m : transps)
            msgs.add(m);

        NeptusLog.pub().info("<###>Sending the mission spec (plan) to " + destination.getHostName() + ":"
                + destination.getPort() + "...");
        msg = generatePlanSpecification(mt.getIndividualPlansList().get(planId));
        msgs.add(msg);

        msg = generateLoadMissionCmd(mt.getIndividualPlansList().get(planId));
        msgs.add(msg);

        sendMsgs(destination, msgs);
        return true;
    }

    public static boolean sendMsgs(InetSocketAddress destination, Vector<IMCMessage> msgs) {
        try {
            DatagramSocket sock = new DatagramSocket();

            sock.connect(destination);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream ios = new IMCOutputStream(baos);
            try {
                for (IMCMessage message : msgs) {
                    message.serialize(ios);

                    sock .send(new DatagramPacket(baos.toByteArray(), baos.size()));
                    message.dump(System.out);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                sock.close();
                return false;
            }

            sock.close();
            return true;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    public static boolean sendUdpMsg(InetSocketAddress destination, byte[] msg, int size) {

        try {
            DatagramSocket sock = new DatagramSocket();

            sock.connect(destination);
            sock.send(new DatagramPacket(msg, size));

            // int auxH=msg[3]>=0?msg[3]:Character.MAX_VALUE+1+msg[3];
            // System.err.println("MSG on the network "+aux);
            // ByteUtil.dumpAsHex(msg, System.out);
            sock.close();

            return true;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return false;
        }
    }

    public static IMCMessage sendHomeReference(VehicleType vt, MissionType mt) {

        IMCMessage msg = generateHomeReferenceMessage(mt);

        if (msg == null)
            return null;

        String vid = vt.getId();

        if (!ImcMsgManager.getManager().isRunning()) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send HomeRef", "IMC comms. are not running!");
            return null;
        }

        boolean ret = ImcMsgManager.getManager().sendMessageToVehicle(msg, vid, null);

        if (!ret)
            GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send HomeRef", "Error sending HomeRef message!");
        // HomeReference homeRef = mt.getHomeRef();
        // getSentConfiguration(vid).setHomeRef(homeRef);

        return msg;
    }

    public static boolean sendTransponderConfigurations(VehicleType vt, MissionType mt) {
        if (!ImcMsgManager.getManager().isRunning()) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send Transponders",
                    "IMC comms. are not running!");
            return false;
        }

        String vid = vt.getId();
        IMCMessage[] lblSetupMsgs = generateLblBeaconSetup(mt);

        for (IMCMessage msg : lblSetupMsgs) {
            boolean ret = ImcMsgManager.getManager().sendMessageToVehicle(msg, vid, null);

            if (!ret)
                GuiUtils.errorMessage(ConfigFetch.getSuperParentAsFrame(), "Send Start Mission Command",
                        "Error sending MissionCommand message!");
            return false;
        }

        if (lblSetupMsgs.length > 0) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            mt.asZipFile(missionlog, true);
        }
        return true;
    }

    static MissionType mt;
    static Vector<String> plansList = new Vector<String>();

    // -Services---------------------------------------------------------------

    /**
     * This one calls {@link #getServiceProvided(String, String, String)} and see if the return is empty.
     * 
     * @param services The services ';' separated string as come from Announce message.
     * @param scheme '*' for any. The match in this case is the complete string.
     * @param name '*' for any. The match in this case is the start with string.
     * @return
     */
    public static boolean isServiceProvided(String services, String scheme, String name) {
        Vector<URI> ret = getServiceProvided(services, scheme, name);
        if (ret.size() == 0)
            return false;
        else
            return true;
    }

    /**
     * @param services The services ';' separated string as come from Announce message.
     * @param scheme '*' for any. The match in this case is the complete string.
     * @param name '*' for any. The match in this case is the start with string.
     * @return
     */
    public static Vector<URI> getServiceProvided(String services, String scheme, String name) {
        String[] servicesList = services.split(";");
        Vector<URI> uriList = new Vector<URI>();
        if (name == null || "".equalsIgnoreCase(name))
            return uriList;
        for (String str : servicesList) {
            try {
                URI url1 = URI.create(str.trim());
                if (scheme != null && !"".equalsIgnoreCase(scheme)) {
                    if (!scheme.equalsIgnoreCase("*") && !scheme.equalsIgnoreCase(url1.getScheme()))
                        continue;
                }
                String path = url1.getPath();
                if (path == null || "".equalsIgnoreCase(path))
                    continue;
                if (path.startsWith("/" + name.trim()) || name.equalsIgnoreCase("*")) {
                    uriList.add(url1);
                }
                // if (path.substring(1).matches(name) || name.equalsIgnoreCase("*")) {
                // uriList.add(url1);
                // }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uriList;
    }

    /**
     * Extracts the UID from Dune (dune:///uid/<uid>) or Neptus (neptus:///uid/<uid>).
     * 
     * @param services
     * @return the UID
     */
    public static final String getUidFromServices(String services) {
        String ret = null;
        Vector<URI> strLst = getServiceProvided(services, "*", "uid");
        if (strLst.size() == 0) {
            strLst = getServiceProvided(services, "dune", "*");
        }
        if (strLst.size() != 0) {
            String ud = strLst.get(0).toString();
            if (ud != null)
                ret = ud;
        }
        return ret;
    }

    public static IMCMessage asMessageList(Collection<IMCMessage> list) {
        IMCMessage first = null, previous = null;
        for (IMCMessage m : list) {
            IMCMessage cur = IMCDefinition.getInstance().create("MessageList", "msg", m);

            if (previous != null)
                previous.setValue("next", cur);
            else
                first = cur;

            previous = cur;
        }
        return first;
    }

    /**
     * @return
     * 
     */
    public static IMCMessage getEmptyMessageList() {
        return IMCDefinition.getInstance().create("MessageList");
    }

    public static LocationType parseLocation(IMCMessage imcEstimatedState) {
        LocationType loc = new LocationType();
        loc.setLatitudeDegs(Math.toDegrees(imcEstimatedState.getDouble("lat")));
        loc.setLongitudeDegs(Math.toDegrees(imcEstimatedState.getDouble("lon")));
        loc.setOffsetNorth(imcEstimatedState.getDouble("x"));
        loc.setOffsetEast(imcEstimatedState.getDouble("y"));
        loc.setOffsetDown(imcEstimatedState.getDouble("z"));

        double depth = imcEstimatedState.getDouble("depth");
        double height = imcEstimatedState.getDouble("height");
        double altitude = imcEstimatedState.getDouble("alt");
        if (depth != -1) {
            loc.setDepth(depth);        
        }
        else if (altitude != -1) {
            loc.setDepth(-altitude);            
        }
        else {
            loc.setDepth(-height);
        }        
        return loc;
    }

    public static SystemPositionAndAttitude parseState(IMCMessage imcEstimatedState) {
        return new SystemPositionAndAttitude(IMCUtils.parseLocation(imcEstimatedState), imcEstimatedState.getDouble("phi"),
                imcEstimatedState.getDouble("theta"), imcEstimatedState.getDouble("psi"));
    }

    public static IMCMessage getLblConfig(MissionType mt) {
        IMCMessage lblConfig = IMCDefinition.getInstance().create("LblConfig");

        LinkedList<TransponderElement> transpondersList = new LinkedList<TransponderElement>();
        LinkedHashMap<String, MapMission> mapList = mt.getMapsList();
        for (MapMission mpm : mapList.values()) {
            // LinkedHashMap traList = mpm.getMap().getTranspondersList();
            LinkedHashMap<String, TransponderElement> transList = mpm.getMap().getTranspondersList();
            for (TransponderElement tmp : transList.values()) {
                transpondersList.add(tmp);
            }
        }

        Vector<IMCMessage> beaconMessages = new Vector<IMCMessage>();

        for (int i = 0; i < transpondersList.size(); i++) {
            TransponderElement transp = transpondersList.get(i);
            LocationType absLoc = transp.getCenterLocation().getNewAbsoluteLatLonDepth();
            try {
                // int id = (int) Double.parseDouble(transp.getPropConf().getProperty("id"));
                int queryChannel = (int) Double.parseDouble(transp.getPropConf().getProperty("interrogation channel"));
                int replyChannel = (int) Double.parseDouble(transp.getPropConf().getProperty("reply channel"));
                int transpDelay = (int) Double.parseDouble(transp.getPropConf().getProperty(
                        "transponder delay (msecs.)"));

                IMCMessage lblBeacon = IMCDefinition.getInstance().create("LblBeacon",
                        "beacon", transp.getId(),
                        "lat", absLoc.getLatitudeRads(),
                        "lon", absLoc.getLongitudeRads(),
                        "depth", absLoc.getDepth(),
                        "query_channel", queryChannel,
                        "reply_channel", replyChannel,
                        "transponder_delay", transpDelay
                        );

                beaconMessages.add(lblBeacon);
            }
            catch (NumberFormatException e2) {
                NeptusLog.pub().error(e2.getStackTrace());
                return null;
            }
        }

        Collections.sort(beaconMessages, new Comparator<IMCMessage>() {
            @Override
            public int compare(IMCMessage o1, IMCMessage o2) {
                return ("" + o1.getValue("beacon")).compareToIgnoreCase("" + o2.getValue("beacon"));
            }
        });

        for (int i = 0; i < 6; i++) {
            if (i < beaconMessages.size())
                lblConfig.setValue("beacon" + i, beaconMessages.get(i));
        }

        return lblConfig;
    }

    /**
     * @param estimatedStateEntry
     * @return
     */
    public static LocationType getLocation(IMCMessage estimatedStateMsg) {

        LocationType loc = new LocationType();
        loc.setLatitudeRads(estimatedStateMsg.getDouble("lat"));
        loc.setLongitudeRads(estimatedStateMsg.getDouble("lon"));
        loc.setDepth(estimatedStateMsg.getDouble("depth"));

        loc.translatePosition(
                estimatedStateMsg.getDouble("x"),
                estimatedStateMsg.getDouble("y"),
                estimatedStateMsg.getDouble("z")
                );

        return loc;        
    }


    public static void setProperties(Property[] properties, IMCMessage message) {
        //FIXME Enumerated e BitMask edition
        for (Property p : properties) {
            try {

                if (p.getCategory() != null && p.getCategory().equals("header")) {
                    try {
                        message.getHeader().setValue(p.getName(), p.getValue());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (message.getMessageType().getFieldUnits(p.getName()) != null) {
                    if (message.getMessageType().getFieldUnits(p.getName()).equalsIgnoreCase("Enumerated"))
                        message.setValue(p.getName(), p.getValue());
                    else if (message.getMessageType().getFieldUnits(p.getName()).equalsIgnoreCase("Bitmask"))
                        message.setValue(p.getName(), p.getValue());
                    else if (message.getMessageType().getFieldUnits(p.getName()).equalsIgnoreCase("TupleList"))
                        message.setValue(p.getName(), p.getValue());
                    else
                        message.setValue(p.getName(), p.getValue());
                }
                else if (message.getMessageType().getFieldType(p.getName()) == IMCFieldType.TYPE_PLAINTEXT){
                    message.setValue(p.getName(), p.getValue());
                }
                else 
                    message.setValue(p.getName(), p.getValue());
            }
            catch (Exception e) {
                NeptusLog.pub().error(e,e);
            }
        }
    }


    public static Vector<PluginProperty> getProperties(final IMCMessage message) {
        return getProperties(message, false);
    }
    @SuppressWarnings("serial")
    public static Vector<PluginProperty> getProperties(final IMCMessage message, boolean ignoreHeaderFields) {
        //FIXME Enumerated e BitMask edition

        Vector<PluginProperty> properties = new Vector<PluginProperty>();

        if (!ignoreHeaderFields) {
            IMCMessage headerType = IMCDefinition.getInstance().createHeader();

            for (String hf : headerType.getFieldNames()) {
                try {
                    Object fieldValue;
                    fieldValue = message.getHeader().getValue(hf);
                    
                    PluginProperty ap = null;

                    if(fieldValue == null) {
                        continue;
                    }
                    else {
                        if (hf.equalsIgnoreCase("src") || hf.equalsIgnoreCase("dst")) {
                            fieldValue = new ImcId16(fieldValue);
                        }
                        ap = new PluginProperty(hf, fieldValue.getClass(), fieldValue);
                        ap.setDisplayName(headerType.getLongFieldName(hf));
                    }
                    
                    ap.setCategory("header");
                    if (hf.equalsIgnoreCase("time") || hf.equalsIgnoreCase("mgid") || hf.equalsIgnoreCase("sync") ||
                            hf.equalsIgnoreCase("size")) {
                        ap.setEditable(false);
                    }
                    if (hf.equalsIgnoreCase("sync")) {
                        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(ap, 
                                new DefaultCellRenderer() {
                            {
                                setShowOddAndEvenRows(false);
                            }
                            @Override
                            protected String convertToString(Object value) {
                                try {
                                    Number nb = (Number) value;
                                    return "0x" + Long.toHexString(nb.longValue()) + " (" + nb + ")";
                                }
                                catch (Exception e) {
                                    return super.convertToString(value);
                                }
                            }
                        });
                    }

                    properties.add(ap);

                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getStackTrace());
                }
            }
        }

        for (final String fi : message.getMessageType().getFieldNames()) {           
            try {
                Object fieldValue = message.getValue(fi);

                if (message.getMessageType().getFieldUnits(fi) != null) {
                    if (message.getMessageType().getFieldUnits(fi).equalsIgnoreCase("Enumerated")) {
                        Enumerated em = new Enumerated(message.getMessageType().getFieldPossibleValues(fi), 
                                message.getLong(fi));
                        fieldValue = em;
                    }
                    else if (message.getMessageType().getFieldUnits(fi).equalsIgnoreCase("Bitmask")
                            || message.getMessageType().getFieldUnits(fi).equalsIgnoreCase("Bitfield")) {
                        Bitmask bm = new Bitmask(message.getMessageType().getFieldPossibleValues(fi),
                                message.getLong(fi));
                        fieldValue = bm;
                    }
                }                

                PluginProperty ap = null;
                if (fieldValue != null) {
                    ap = new PluginProperty(fi, fieldValue.getClass(), fieldValue);
                    ap.setDisplayName(message.getMessageType().getFullFieldName(fi));
                }
                //                    ap = PropertiesEditor.getPropertyInstance(fi, "payload", fieldValue.getClass(), fieldValue, true);
                else {
                    try {
                        Class<?> type = message.getMessageType().getFieldType(fi).getJavaType();
                        ap = new PluginProperty(fi, type, type.newInstance());
                        ap.setDisplayName(message.getMessageType().getFullFieldName(fi));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
                ap.setCategory("payload");

                String fieldName = fi;
                fieldName = fieldName.substring(0, 1).toUpperCase()+fieldName.substring(1);
                String desc = "<html>"+fi;
                if ((message.getMessageType().getFieldUnits(fi) != null) && 
                        !"".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi)))
                    desc = desc.concat(" (" + message.getMessageType().getFieldUnits(fi) + ")");

                try {
                    Object min = message.getClass().getField(fieldName+"_MIN").get(message);
                    Object max = message.getClass().getField(fieldName+"_MAX").get(message);
                    String d = desc + "<br>[ " + min + " .. " + max + " ]</html>";
                    ap.setShortDescription(d);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                    ap.setShortDescription(desc);
                }

                if ("rad".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))
                        || "radians".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))) {
                    PropertiesEditor.getPropertyEditorRegistry().registerEditor(
                            ap, AngleEditorRadsShowDegrees.class);
                    PropertiesEditor.getPropertyRendererRegistry().registerRenderer(ap, 
                            new DefaultCellRenderer() {
                        {
                            setShowOddAndEvenRows(false);
                        }
                        @Override
                        protected String convertToString(Object value) {
                            try {
                                Number nb = (Number) value;
                                return Math.toDegrees(nb.doubleValue()) + "\u00B0 deg";
                            }
                            catch (Exception e) {
                                return super.convertToString(value);
                            }
                        }
                    });
                }
                else if ("deg".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))
                        || "degrees".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))) {
                    PropertiesEditor.getPropertyEditorRegistry().registerEditor(
                            ap, AngleEditorDegs.class);
                    PropertiesEditor.getPropertyRendererRegistry().registerRenderer(ap, 
                            new DefaultCellRenderer() {
                        {
                            setShowOddAndEvenRows(false);
                        }
                        @Override
                        protected String convertToString(Object value) {
                            try {
                                Number nb = (Number) value;
                                return nb.doubleValue() + "\u00B0 deg";
                            }
                            catch (Exception e) {
                                return super.convertToString(value);
                            }
                        }
                    });
                }
                else if ("Enumerated".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))
                        || "Bitmask".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))
                        || "Bitfield".equalsIgnoreCase(message.getMessageType().getFieldUnits(fi))) {
                    // Do nothing
                }
                else {
                    PropertiesEditor.getPropertyRendererRegistry().registerRenderer(ap, new DefaultCellRenderer() {
                        {
                            setShowOddAndEvenRows(false);
                        }
                        @Override
                        protected String convertToString(Object value) {
                            String funits = message.getMessageType().getFieldUnits(fi);
                            return super.convertToString(value) + " " + (funits != null ? funits : "");
                        }
                    });
                }

                //if (message.getType().getFieldType(fi).equalsIgnoreCase("message")) {
                //  PropertiesEditor.getPropertyEditorRegitry().registerEditor(
                //          ap, MessageEditorImc.class);
                //}

                properties.add(ap);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                GuiUtils.errorMessage(null, e);
            }
        }
        return properties;
    }
    
    public static void dumpPayloadBytes(IMCMessage message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream ios = new IMCOutputStream(baos);
        IMCDefinition.getInstance().serializeFields(message, ios);
        byte[] data = baos.toByteArray();
        
        for (int i = 0; i < data.length; i++) {
            if (i % 10 == 0)
                System.out.print(" ");
            System.out.printf("%02X", data[i]);
        }       
    }
    
    
    
    /**
     * Given an IMC ID, this method returns the system type.
     * @see https://github.com/LSTS/imc/blob/master/IMC_Addressing_Scheme.txt
     * @param imcId The IMC id (uint16_t)
     * @return The type of the system. One of "UUV", "ROV", "USV", "UAV", "UXV", "CCU", "Sensor" or "Unknown".
     */
    public static String getSystemType(int imcId) {
        int sys_selector = 0xE000;
        int vtype_selector = 0x1800;
        
        int sys_type = (imcId & sys_selector) >> 13;
        
        switch (sys_type) {
            case 0:
            case 1:
                switch ((imcId & vtype_selector) >> 11) {
                    case 0:
                        return "UUV";
                    case 1:
                        return "ROV";
                    case 2:
                        return "USV";
                    case 3:
                        return "UAV";
                    default:
                        return "UXV";
                }
            case 2:
                return "CCU";
            default:
                break;
        }
        
        String name = IMCDefinition.getInstance().getResolver().resolve(imcId).toLowerCase();
        if (name.contains("ccu"))
            return "CCU";
        if (name.contains("argos"))
            return "Argos Tag";
        if (name.contains("spot"))
            return "SPOT Tag";
        if (name.contains("manta"))
            return "Gateway";
        return "Unknown";
    }
    
    public static void testSysTypeResolution() throws Exception {
        String address_url = "file:///home/zp/Desktop/IMC_Addresses.xml";
        
        URLConnection conn = new URL(address_url).openConnection();
        Document doc = DocumentHelper.parseText(IOUtils.toString(conn.getInputStream()));
        List<?> nodes = doc.getRootElement().selectNodes("address/@id");
        for (int i = 0; i < nodes.size(); i++) {
            DefaultAttribute addrElem = (DefaultAttribute) nodes.get(i);
            int id = Integer.parseInt(addrElem.getText().replaceAll("0x", ""), 16);
            String name = IMCDefinition.getInstance().getResolver().resolve(id);
            System.out.println(addrElem.getText() + ","+name+" --> "+getSystemType(id));
        }
    }
}
