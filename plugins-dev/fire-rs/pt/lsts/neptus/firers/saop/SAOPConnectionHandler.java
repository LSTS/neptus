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
 * Author: keila
 * 02/04/2018
 */
package pt.lsts.neptus.firers.saop;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.DevDataBinary;
import pt.lsts.imc.DevDataText;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.UTMCoordinates;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

@PluginDescription(name = "SAOP Server Interaction", description = "IMC Message exchange with SAOP IMC TCP Server", icon = "pt/lsts/neptus/firers/images/fire.png")
public class SAOPConnectionHandler extends ConsoleLayer {

    private final String prefix = "saop-";
    private volatile boolean sendHeartbeat = false, established = false;
    private ImcTcpTransport imctt;
    private MessageDeliveryListener deliveryListener;
    private MessageListener<MessageInfo, IMCMessage> msgListener;
    private Map<String, Integer> plans_reqId = Collections.synchronizedMap(new HashMap<>());
    private ArrayList<ContourLine> polygons = new ArrayList<>();
    private FireRaster raster;
    // ETR89/LAEA Europe Coordinates System params for WGS84 re-projection
    public final double ETR89_to_WGS84_lat = 52.0;
    public final double ETR89_to_WGS84_long = 10.0;
    public final double ETR89_to_WGS84_east = 4321000.0;
    public final double ETR89_to_WGS84_nort = 3210000.0;
    public final int WGS84_EPSG = 4326; 
    private InterpolationColorMap polyCm = ColorMapFactory.createGrayScaleColorMap();
    private InterpolationColorMap rasterCm = ColorMapFactory.createAutumnColorMap();
    private ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, rasterCm);

    @NeptusProperty(name = "Debug Mode", userLevel = LEVEL.REGULAR, description = "Request operators permission to start/stop plan")
    public boolean debugMode = true;
    @NeptusProperty(name = "IP ADDRESS", userLevel = LEVEL.REGULAR, description = "IP ADDRESS to SAOP server")
    public String ipAddr = "127.0.0.1";
    @NeptusProperty(name = "PORT", userLevel = LEVEL.REGULAR, description = "Port to SAOP server")
    public int serverPort = 8888;
    @NeptusProperty(name = "Bind PORT", userLevel = LEVEL.ADVANCED, description = "Neptus internal configuration")
    public int bindPort = 8000;

    /**
     * Send periodically heartbeat to keep connection open
     */
    @Periodic(millisBetweenUpdates = 3000)
    public void sendHeartbeat() {
        boolean isConnected = imctt.getTcpTransport().connectIfNotConnected(ipAddr, serverPort);
        if (isConnected && !established) {
            established = true;
            sendHeartbeat = true;
        }
        else if (!isConnected && established) {
            established = false;
            sendHeartbeat = false;
        }

        if (sendHeartbeat) {
            Heartbeat hb = new Heartbeat();
            imctt.sendMessage(ipAddr, serverPort, hb, deliveryListener);
        }
    }

    public void initializeListeners() {

        msgListener = new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                NeptusLog.pub().info("Received: " + msg.getAbbrev() + " from: " + info.getPublisher() + ":"
                        + info.getPublisherPort());
                // if (info.getPublisherInetAddress().equalsIgnoreCase(ipAddr) && info.getPublisherPort() == port) {
                if (msg.getMgid() == PlanControl.ID_STATIC) {
                    PlanControl pc = msg.cloneMessageTyped();
                    addNewPlanControl(pc);
                }
                // }
                else if (msg.getMgid() == DevDataBinary.ID_STATIC) {

                    DevDataBinary data = msg.cloneMessageTyped();

                    byte[] msgData = data.getValue();
                    ByteBuffer wrapper = ByteBuffer.wrap(msgData);
                    wrapper.order(ByteOrder.LITTLE_ENDIAN);
                    short magicNum = wrapper.getShort();
                    long EPSG_ID = wrapper.getLong();
                    Long xSize = wrapper.getLong();
                    Long ySize = wrapper.getLong();
                    double xOffset = wrapper.getDouble();
                    double yOffset = wrapper.getDouble();
                    double cellWidth = wrapper.getDouble();

                    if (magicNum != (short) 0x3EF1) {
                        return;
                    }
                    NeptusLog.pub().info(I18n.text("xoffset: "+(xOffset)+" yoffset: "+(yOffset)+" EPSG: "+EPSG_ID));
                    NeptusLog.pub().info(I18n.text("xSize: "+xSize+" ySize: "+ySize+" Cell Width: "+cellWidth));

                    raster = new FireRaster(xOffset, yOffset, xSize, ySize, cellWidth, EPSG_ID);
                    NeptusLog.pub().info(I18n.text("WGS84 coords for raster: "+raster.getLocation()));
                    byte[] rasterData;
                    rasterData = Arrays.copyOfRange(msgData, wrapper.position(), msgData.length);

                    Inflater decompressor = new Inflater();
                    decompressor.setInput(rasterData);
                    byte[] inputBuff = new byte[256];
                    ByteArrayInputStream inByteStream = new ByteArrayInputStream(inputBuff);
                    InflaterInputStream inStream = new InflaterInputStream(inByteStream, decompressor);

                    byte[] tmpDouble = new byte[8];
                    ByteBuffer tmpBB = ByteBuffer.wrap(tmpDouble);
                    tmpBB.order(ByteOrder.LITTLE_ENDIAN);
                    double cmMax = Double.MIN_VALUE, cmMin = Double.MAX_VALUE;
                    try {
                        while (inStream.read(tmpDouble) > 0) {
                            double tmp = tmpBB.getDouble();
                            if(tmp != Double.POSITIVE_INFINITY){
                                if(tmp > cmMax)
                                    cmMax = tmp;
                                if(tmp < cmMin)
                                    cmMin = tmp;
                            }
                            raster.rasterDataAppend(tmp);
                            tmpBB.rewind();
                        }
                        rasterCm.setValues(new double[]{cmMin, cmMax});
                        DataBufferDouble buffer = new DataBufferDouble(
                                ArrayUtils.toPrimitive(raster.getRasterData().toArray(new Double[] {})), 1);
                        fillRaster(xSize.intValue(), ySize.intValue(), buffer);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        e.printStackTrace();
                    }

                }

                else if (msg.getMgid() == DevDataText.ID_STATIC) {
                    //Parse Json Object from IMC msg
                    //polygons.clear();
                    double minTime = Double.MAX_VALUE;
                    double maxTime = Double.MIN_VALUE;
                    polygons.clear();

                    try {
                        JsonParser parser = new JsonParser();
                        DevDataText data = msg.cloneMessageTyped();
//                        JsonElement element = parser.parse(data.getValue());
                        JsonObject object = parser.parse(data.getValue()).getAsJsonObject();
                        for (JsonElement contourLineElem : object.get("wildfire_contours").getAsJsonArray()) {
                            JsonObject contourLine = contourLineElem.getAsJsonObject();
                            ContourLine clInstance = processContourLine(contourLine);
                            if(clInstance != null){
                                polygons.add(clInstance);
                            }
                        }
                        NeptusLog.pub().info(I18n.text("Received "+polygons.size()+" contour lines from SAOP"));
                        for (ContourLine contour :
                                polygons) {
                            if (contour.epoch > maxTime)
                                maxTime = contour.epoch;
                            if(contour.epoch < minTime)
                                minTime = contour.epoch;
                        }
                        polyCm.setValues(new double[]{minTime,maxTime});
                    } catch (Exception e){
                        NeptusLog.pub().error(e);
                        e.printStackTrace();
                    }
                }
            }
        };

        deliveryListener = new MessageDeliveryListener() {

            @Override
            public void deliveryUnreacheable(IMCMessage msg) {
                NeptusLog.pub().debug(I18n.text("Destination " + ipAddr + ":" + serverPort
                        + "unreacheable. Unable to delivery " + msg.getAbbrev() + " to SAOP IMC TCP Server."));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliveryTimeOut(IMCMessage msg) {
                if (checkMsgId(msg.getMgid())) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }

            @Override
            public void deliverySuccess(IMCMessage msg) {
                // TODO remove from hash
            }

            @Override
            public void deliveryError(IMCMessage msg, Object error) {
                NeptusLog.pub().debug(I18n.text("Error delivering " + msg.getAbbrev() + " to SAOP IMC TCP Server."));
            }
        };
    }

    private void fillRaster(int w, int h, DataBufferDouble buffer) {
        int dataType = DataBuffer.TYPE_INT;
        int numBands = 4;
       
        if (raster != null && raster.firemap == null) {
            raster.firemap = Raster.createWritableRaster(new SampleModel(dataType, w, h, numBands) {

                @Override
                public void setSample(int x, int y, int b, int s, DataBuffer data) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void setDataElements(int x, int y, Object obj, DataBuffer data) {
                    // TODO Auto-generated method stub

                }

                @Override
                public int getSampleSize(int band) {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public int[] getSampleSize() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public int getSample(int x, int y, int b, DataBuffer data) {
                    double value = data.getElemDouble(x + y * w);
                    switch (b) {
                        case 0://red
                            return rasterCm.getColor(value).getRed();
                        case 1://green
                            return rasterCm.getColor(value).getGreen();
                        case 2://blue
                            return rasterCm.getColor(value).getBlue();
                        case 3://alpha
                            if(value == Double.POSITIVE_INFINITY){
                                return 0;
                            }
                            else {
                                return 255;
                            }
                        default:
                            return 0;
                    }
                }

                @Override
                public int getNumDataElements() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public Object getDataElements(int x, int y, Object obj, DataBuffer data) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public SampleModel createSubsetSampleModel(int[] bands) {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public DataBuffer createDataBuffer() {
                    // TODO Auto-generated method stub
                    return null;
                }

                @Override
                public SampleModel createCompatibleSampleModel(int w, int h) {
                    // TODO Auto-generated method stub
                    return null;
                }
            }, buffer, new Point(0, 0));
        }
    }

    @Subscribe
    public void on(IMCMessage msg) {
        String system = IMCDefinition.getInstance().getResolver().resolve(msg.getSrc());
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(system);
        // Filter specific IMC Messages from UAVs
        if (imcSys != null
                && (imcSys.getType() == SystemTypeEnum.VEHICLE && imcSys.getTypeVehicle() == VehicleTypeEnum.UAV)) {
            if (established) {
                if (msg.getMgid() == PlanControl.ID_STATIC) {
                    sendHeartbeat = false;
                    PlanControl pc = (PlanControl) msg;
                    pc = (PlanControl) pc.cloneMessage();
                    if (!pc.getType().equals(pt.lsts.imc.PlanControl.TYPE.REQUEST)) { // REPLY SUCCESS or FAILURE or
                        // IN_PROGRESS

                        if (pc.getPlanId() != null) { // reset original SAOP data
                            String newName = pc.getPlanId();
                            String oldName = getOriginalName(newName);
                            pc.setPlanId(oldName);
                            pc.setRequestId(plans_reqId.get(newName));
                        }
                    }
                    imctt.sendMessage(ipAddr, serverPort, pc, deliveryListener);
                    // plans_reqId.remove(newName); plan Id is needed for PCS

                    sendHeartbeat = true;
                }
                else if (msg.getMgid() == PlanControlState.ID_STATIC) {
                    PlanControlState pcs = (PlanControlState) msg;
                    pcs = (PlanControlState) pcs.cloneMessage();
                    String newName = pcs.getPlanId();
                    String oldName = getOriginalName(newName);
                    if (plans_reqId.containsKey(pcs.getPlanId())) {
                        sendHeartbeat = false;
                        pcs.setPlanId(oldName);
                        imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                        sendHeartbeat = true;
                    }
                }
                else if (msg.getMgid() == DevDataBinary.ID_STATIC) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
                else if (msg.getMgid() == EstimatedState.ID_STATIC) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }
        }
    }

    private ContourLine processContourLine(JsonObject contourLineObj) {
//        String time = contourLineObj.getAsJsonPrimitive("time").getAsString();
        BigDecimal time = contourLineObj.getAsJsonPrimitive("time").getAsBigDecimal();
        JsonArray colorRGB = contourLineObj.getAsJsonArray("color");
        Color c = new Color(colorRGB.get(0).getAsInt(), colorRGB.get(1).getAsInt(), colorRGB.get(2).getAsInt());
        PolygonType poly = new PolygonType();
        poly.setFilled(false);
        poly.setColor(c);
        for (JsonElement vertexElem : contourLineObj.get("polygon").getAsJsonArray()) {
            JsonElement latObj = vertexElem.getAsJsonObject().get("lat"),
                    longObj = vertexElem.getAsJsonObject().get("lon");
            poly.addVertex(latObj.getAsDouble(), longObj.getAsDouble());
        }
        poly.addVertex(poly.getVertices().get(0).getLatitudeDegs(),poly.getVertices().get(0).getLongitudeDegs()); // adding first point to close the polygon
        String id = new StringBuilder("ContourLine").append(polygons.size()).toString();
        poly.setId(id);

        // Parse time to epoch
        DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'");
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        long tIme = time.longValue();
        Date dateTime = new Date(tIme*1000);
        return new ContourLine(dateTime.getTime(), utcFormat.format(dateTime), poly, c);
    }

    public boolean checkMsgId(int msgId) {

        return msgId == PlanControl.ID_STATIC || msgId == PlanControlState.ID_STATIC || msgId == DevDataBinary.ID_STATIC
                || msgId == DevDataText.ID_STATIC || msgId == EstimatedState.ID_STATIC;
    }

    /**
     * Add new incoming PlanControl from the SAOP server to mission plan tree. The added plan has the format:
     * saop-planId
     */
    public void addNewPlanControl(PlanControl pc) {
        String pcsName = null;
        PlanSpecification ps;
        if (pc.getPlanId() != null) {
            StringJoiner sj = new StringJoiner("", prefix, pc.getPlanId());
            pcsName = sj.toString();
            pc.setPlanId(pcsName);
            synchronized (plans_reqId) {
                plans_reqId.put(pcsName, pc.getRequestId());
            }
        }
        if (pc.getArg() != null) {
            if (pc.getArg().getClass().equals(PlanSpecification.class)) {
                ps = (PlanSpecification) pc.getArg();
                ps.setPlanId(pcsName);
                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), ps);
                plan.setVehicle("x8-02");
                synchronized (plans_reqId) {
                    plans_reqId.put(pcsName, pc.getRequestId());
                }
                pc.setArg(ps);

                getConsole().getMission().addPlan(plan);
                getConsole().getMission().save(true);
                getConsole().updateMissionListeners();
                getConsole().post(Notification.success(I18n.text("SAOP IMC TCP SERVER"),
                        I18n.textf("Received PlanSpecifition: %plan.", pcsName)));
            }

        }
        String system = IMCDefinition.getInstance().getResolver().resolve(pc.getDst());
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(system);
        if (debugMode) {
            if (pc.getOp().equals(OP.STOP) || pc.getOp().equals(OP.START) || pc.getOp().equals(OP.LOAD)) {
                String message = pc.getOpStr() + " from SAOP Server to " + imcSys;
                if (pc.getOp().equals(OP.START) && pcsName != null) {
                    message = message + " to plan: " + pc.getPlanId();
                }
                else if (pc.getOp().equals(OP.START) && pcsName != null) {
                    message = message + " to plan: " + pcsName;
                }
                else if (pc.getOp().equals(OP.START) && (pcsName == null && pc.getArg() == null))
                    return;
                int answer = GuiUtils.confirmDialog(this.getConsole(), "SAOP IMC TCP SERVER", "Allow " + message);
                if (answer == JOptionPane.OK_OPTION) {
                    ImcMsgManager.getManager().sendMessage(pc);
                }
                else if (answer == JOptionPane.NO_OPTION) {
                    getConsole().post(Notification.warning(I18n.text("SAOP IMC TCP SERVER"),
                            I18n.text("Intercepted " + message)));
                }
            }

        }
        else {
            ImcMsgManager.getManager().sendMessage(pc);
            getConsole().post(Notification.success(I18n.text("SAOP IMC TCP SERVER"),
                    I18n.textf("Forwarded PlanControl %op.", pc.getOpStr())));
        }
    }

    /**
     * @param newName in the form: saop_oldName which can contain ``-'' (underscore) DEPREAC: saop_`TIMESTAMP`_oldName
     * @return Retrieves original name from the plan
     */
    public String getOriginalName(String newName) {
        StringBuilder sb = new StringBuilder();
        String[] names = newName.split("-");
        for (int i = 1; i < names.length; i++)
            sb.append(names[i]);
        return sb.toString();
    }

    /**
     * Update plugins parameters
     */
    @Override
    public void setProperties(Property[] properties) {
        if ((boolean) properties[0].getValue() != debugMode) {
            debugMode = (boolean) properties[0].getValue();
        }
        if ((String) properties[1].getValue() != ipAddr) {
            ipAddr = (String) properties[1].getValue();
        }
        if ((int) properties[2].getValue() != serverPort) {
            serverPort = (int) properties[2].getValue();
        }
        if ((int) properties[3].getValue() != bindPort) {
            established = false;
            sendHeartbeat = false;
            bindPort = (int) properties[3].getValue();
            imctt.reStart();
            imctt.setBindPort(bindPort);
            imctt.addListener(msgListener);
            sendHeartbeat = true;
        }

        super.setProperties(properties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        initializeListeners();
        imctt = new ImcTcpTransport(bindPort, IMCDefinition.getInstance());
        imctt.addListener(msgListener);

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        established = false;
        sendHeartbeat = false;
        polygons.clear();
        imctt.removeListener(msgListener);
        imctt.purge();

    }

    @Override
    public void paint(Graphics2D go, StateRenderer2D renderer) {
        int length = polygons.size() > 1 ? polygons.size()*50+100: 200;
        int width  = 150;
        Graphics2D g = ((Graphics2D) go.create());
        g.setColor(new Color(255, 255, 255, 150));
        g.fillRect(20, 20, width, length);
        int x = 40;
        int y = 40;
        
        //Title
        g.setColor(Color.black);
        g.setFont(new Font("Helvetica", Font.BOLD, 12));
        g.drawString("Wildfire Contours", x, y);
        y+=20;

        // PAINT RASTER
        if (raster != null && raster.firemap != null) {
            // PAINT RASTER COLORMAP
            Graphics2D cbGraphics = ((Graphics2D) go.create());
            double[] oldValues = rasterCm.getValues();
            rasterCm.setValues(new double[] { 0, 1 });
            cb.setSize(15, 80);
            cbGraphics.setColor(Color.black);
            cbGraphics.setFont(new Font("Helvetica", Font.BOLD, 12));
            int yy = polygons.size() > 1 ? polygons.size() * 40 + y : 100;
            cbGraphics.drawString("Ignition Time", x, yy);
            cbGraphics.translate(x, yy + 10);
            cb.paint(cbGraphics);
            rasterCm.setValues(oldValues);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            // multiply by 1000 because java creates dates based on milliseconds
            long minVal = Double.valueOf(oldValues[0]).longValue() * 1000,
                    maxVal = Double.valueOf(oldValues[1]).longValue() * 1000;
            cbGraphics.setFont(new Font("Helvetica", Font.BOLD, 10));
            try {
                if (oldValues[0] != Double.MAX_VALUE)
                    cbGraphics.drawString(sdf.format(new Date((minVal + maxVal) / 2)), 15, 45);

                if (oldValues[0] == Double.MAX_VALUE) {
                    cbGraphics.drawString("-\u221E", 15, 80);
                    cbGraphics.drawString("+\u221E", 15, 10);
                }
                else {
                    cbGraphics.drawString(sdf.format(new Date(maxVal)), 15, 10);
                    cbGraphics.drawString(sdf.format(new Date(minVal)), 15, 80);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
  
        }
        // PAINT POLYGONS
        for (int i = 0; i < polygons.size(); i++) {
            ContourLine cl = polygons.get(i);
            //cl.polygon.setColor(polyCm.getColor(cl.epoch));
            cl.polygon.paint(g, renderer);
            y = printLabel(go,renderer,cl,x+10,y);
            g.setTransform(renderer.getIdentity());
        }
        
        // PAINT RASTER
        if (raster != null && raster.firemap != null) {
            BufferedImage imgbuff = new BufferedImage(raster.firemap.getWidth(), raster.firemap.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            imgbuff.setData(raster.firemap);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            LocationType topLeft = raster.getLocation();
 
            Point2D corner = renderer.getScreenPosition(topLeft);
            g.translate(corner.getX(), corner.getY());            
            g.scale(renderer.getZoom(), renderer.getZoom());
            g.rotate(-imgbuff.getHeight());//
            g.drawImage(imgbuff, 0, 0, new Double(raster.xSize*raster.getCellWidth()).intValue(), new Double(raster.ySize*raster.getCellWidth()).intValue(), renderer);
        }

    }

    private int printLabel(Graphics2D g, StateRenderer2D renderer, ContourLine c, int x, int y) {

        g.setColor(Color.BLACK);
        Polygon poly = new Polygon(new int[] { 8, 8, 16 }, new int[] { 0, 10, 5 }, 3);
        g.translate(20, y - 10);
        g.fill(poly);
        g.translate(-20, -(y - 10));

        g.setColor(c.color);
        g.fill(new Ellipse2D.Double(x - 10, y - 10, 12, 12));
        g.setColor(Color.BLACK);
        String parts[] = c.time.split(" ");
        String date = parts[0];
        String time = parts[1];
        g.setFont(new Font("Helvetica", Font.BOLD, 10));
        g.drawString(date, x+5, y-2);
        g.drawString(time, x+5, y + 10);
        y += 25;
        return y;
    }

private class ContourLine {
    public String time;
    public long epoch;
    public PolygonType polygon;
    public Color color;

    ContourLine(long epoch, String time, PolygonType polygon, Color c) {
        this.epoch = epoch;
        this.time = time;
        this.polygon = polygon;
        this.color = c;
    }
}

private class FireRaster {
    final long EPSG_ID;
    final long xSize;
    final long ySize;
    final double xOffset;
    final double yOffset;
    final double cellWidth;
    private final LocationType location;
    ArrayList<Double> rasterData;
    WritableRaster firemap;

    FireRaster(double xOffset, double yOffset, long xSize, long ySize, double cellWidth, long EPSG_ID) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.xSize = xSize;
        this.ySize = ySize;
        this.cellWidth = cellWidth;
        this.EPSG_ID = EPSG_ID;
        rasterData = new ArrayList<Double>();
//        if(EPSG_ID == 32629) {//WGS 84 / UTM zone 29N 
        double xNW  = xOffset-cellWidth/2;
        double yNW  = yOffset + (ySize*cellWidth) - cellWidth/2; //most south-most west --> most north west
        UTMCoordinates utm = new UTMCoordinates(xNW, yNW, 29, 'N'); // half-pixel translation
        utm.UTMtoLL();
        location = new LocationType(utm.getLatitudeDegrees(), utm.getLongitudeDegrees());
        NeptusLog.pub().info(I18n.text("Adjustment xoffset: "+xNW+" Adjustment yoffset: "+yNW+" EPSG: "+EPSG_ID));
    }

    /**
     * @return
     */
    public LocationType getLocation() {
        return location;
    }

    /**
     * @return the ePSG_ID
     */
    public long getEPSG_ID() {
        return EPSG_ID;
    }

    /**
     * @return the xSize
     */
    public long getxSize() {
        return xSize;
    }

    /**
     * @return the ySize
     */
    public long getySize() {
        return ySize;
    }

    /**
     * @return the xOffset
     */
    public double getxOffset() {
        return xOffset;
    }

    /**
     * @return the yOffset
     */
    public double getyOffset() {
        return yOffset;
    }

    /**
     * @return the cellWidth
     */
    public double getCellWidth() {
        return cellWidth;
    }

    /**
     * @return the rasterData
     */
    public ArrayList<Double> getRasterData() {
        return rasterData;
    }

    public void setRasterData(ArrayList<Double> rasterData) {
        this.rasterData = rasterData;
    }

    public void rasterDataAppend(double value) {
        this.rasterData.add(value);
    }

    public int[] getScaleFactor(int maxWidth, int maxHeight) {
        int[] res = new int[2];
        double imgRatio = (double) raster.xSize*raster.getCellWidth() / (double) raster.ySize*raster.getCellWidth();
        double desiredRatio = (double) maxWidth / (double) maxHeight;
        int width;
        int height;

        if (desiredRatio > imgRatio) {
            height = maxHeight;
            width = (int) (maxHeight * imgRatio);
        }
        else {
            width = maxWidth;
            height = (int) (maxWidth / imgRatio);
        }
        res[0] = width;
        res[1] = height;
        return res;
    }
}

    public static void main(String [] args) {
    UTMCoordinates utm = new UTMCoordinates(535047.210, 4572080.090, 29, 'N');
    utm.UTMtoLL();
    LocationType loc = new LocationType(utm.getLatitudeDegrees(), utm.getLongitudeDegrees()); 
    LocationType topLeft = new LocationType(41.29100, -8.57000);
    System.err.println("UTM/WGS84: "+loc);
    System.err.println("Hard Coded: "+topLeft);
    
}

}






