/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.Position.PosType;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.IridiumArgs;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zp
 *
 */
@IridiumProvider(id="hub", name="HUB Iridium Messenger", description="Uses the HUB web server to send and receive messages")
public class HubIridiumMessenger implements IridiumMessenger {

    protected boolean available = true;
    protected String serverUrl = GeneralPreferences.ripplesUrl + "/api/v1/";
    private final String authKey = GeneralPreferences.ripplesApiKey;
    // protected String serverUrl = "http://lsts-hub/api/v1/";
    protected String systemsUrl = serverUrl+"systems";
    protected String activeSystemsUrl = systemsUrl+"/active";
    protected String messagesUrl = serverUrl+"iridium";
    protected String messagesRawUrl = serverUrl+"iridium/raw";
    protected int timeoutMillis = 10000;
    protected Set<IridiumMessageListener> listeners = new HashSet<>();
    private static final Pattern p = Pattern.compile("(\\((.)\\) )?\\((.*)\\) (.*) / (.*), (.*) / .*");
    private static final TimeZone tz = TimeZone.getTimeZone("UTC");
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static { dateFormat.setTimeZone(tz); }
    
    public DeviceUpdate pollActiveDevices() throws Exception {
        Gson gson = new Gson();
        URL url = new URI(activeSystemsUrl).toURL();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (authKey != null && !authKey.isEmpty()) {
            con.setRequestProperty ("Authorization", authKey);
        }

        HubSystemMsg[] sys = gson.fromJson(new InputStreamReader(con.getInputStream()), HubSystemMsg[].class);
        
        DeviceUpdate up = new DeviceUpdate();
        for (HubSystemMsg s : sys) {
            if (s.imcid > Integer.MAX_VALUE)
                continue;

            Date date = stringToDate(s.updated_at);
            if (date == null)
                date = new Date();
            Position pos = new Position();
            pos.id = (int) s.imcid;
            pos.latRads = s.coordinates[0];
            pos.lonRads = s.coordinates[1];
            pos.timestamp = date.getTime() / 1000.0;
            pos.posType = PosType.Unknown;
            up.getPositions().put(pos.id, pos);
        }
        
        return up;
    }
    
    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);       
    }

    private HttpURLConnection getHttpURLConnection(String url) throws IOException, URISyntaxException {
        URL u = new URI(url).toURL();
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/hub" );
        conn.setConnectTimeout(timeoutMillis);
        if (authKey != null && !authKey.isEmpty()) {
            conn.setRequestProperty ("Authorization", authKey);
        }
        return conn;
    }

    private void checkResponseFromServer(String url, String msgLabel, String msgType, String sentHexData, HttpURLConnection conn) throws Exception {
        NeptusLog.pub().info("{} : {} {}", url, conn.getResponseCode(), conn.getResponseMessage());

        try (InputStream is = conn.getInputStream(); ByteArrayOutputStream incoming = new ByteArrayOutputStream()) {
            IOUtils.copy(is, incoming);

            NeptusLog.pub().info("Sent {} through HTTP: {} {}", msgLabel,
                    conn.getResponseCode(), conn.getResponseMessage());

            logHubInteraction(msgLabel + " (" + msgType + ")", url,
                    conn.getRequestMethod(), String.valueOf(conn.getResponseCode()),
                    sentHexData, incoming.toString());
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }

        if (conn.getResponseCode() != 200) {
            throw new Exception("Server returned "+ conn.getResponseCode()+": "+ conn.getResponseMessage());
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        byte[] data = msg.serialize();
        data = new String(Hex.encodeHex(data)).getBytes();

        HttpURLConnection conn = getHttpURLConnection(messagesUrl);
        conn.setRequestProperty( "Content-Length", String.valueOf(data.length * 2) );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }

        checkResponseFromServer(messagesUrl, msg.getClass().getSimpleName(), String.valueOf(msg.getMessageType()),
                ByteUtil.encodeToHex(msg.serialize()), conn);
    }

    /**
     * Send this raw message across
     * @param destinationName The name of the destination
     *                        (e.g. the name of the vehicle that should receive the message)
     * @param imeiAddr The address of the destination, in this case the imei of the Iridium
     *                 device that should receive the message, leave empty if not known
     * @param data  The raw data to be sent
     */
    @Override
    public void sendMessageRaw(String destinationName, String imeiAddr, byte[] data) throws Exception {
        HttpURLConnection conn = getHttpURLConnection(messagesRawUrl);

        conn.setRequestProperty( "source", GeneralPreferences.imcCcuName.toLowerCase(Locale.ROOT));
        conn.setRequestProperty( "destination", destinationName);
        if (imeiAddr != null && !imeiAddr.trim().isEmpty())
            conn.setRequestProperty( "imei", imeiAddr);

        conn.setRequestProperty( "Content-Length", String.valueOf(data.length * 2) );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(new String(Hex.encodeHex(data)).getBytes());
        }

        checkResponseFromServer(messagesRawUrl, "raw data", "-1",
                ByteUtil.encodeToHex(data), conn);
    }

    public synchronized void logHubInteraction(String message, String url, String method, String statusCode, String requestData, String responseData) throws Exception {
        if (! (new File("log/hub.log")).exists()) {
            try (BufferedWriter tmp = new BufferedWriter(new FileWriter(new File("log/hub.log"), false))) {
                tmp.write("Time of Day, Message Type, URL, Method, Status Code, Request Data (hex encoded), Response Data\n");
            }
        }
        
        BufferedWriter postWriter = new BufferedWriter(new FileWriter(new File("log/hub.log"), true));
        String out = dateFormat.format(new Date());
        out += ", "+message;
        out += ", "+url;
        out += ", "+method;
        out += ", "+statusCode;
        out += ", "+requestData;
        out += ", "+responseData;
        NeptusLog.pub().info(out);

        postWriter.write(out+"\n");
        postWriter.close();
    }
    
    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        NeptusLog.pub().info("Polling messages since {}", dateToString(timeSince));
        
        URL u;
        if (timeSince != null) {
            u = new URI(messagesUrl + "?since=" + (timeSince.getTime() / 1000)).toURL();
        } else {
            u = new URI(messagesUrl).toURL();
        }
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod( "GET" );
        conn.setConnectTimeout(timeoutMillis);
        if (authKey != null && !authKey.isEmpty()) {
            conn.setRequestProperty ("Authorization", authKey);
        }
        Gson gson = new Gson();  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        
        logHubInteraction("Iridium Poll", u.toString(), conn.getRequestMethod(), ""
                + conn.getResponseCode(), "", baos.toString());
        
        if (conn.getResponseCode() != 200)
            throw new Exception("Hub iridium server returned "+conn.getResponseCode()+": "+conn.getResponseMessage());

        HubMessage[] msgs = gson.fromJson(baos.toString(), HubMessage[].class);
        
        List<IridiumMessage> ret = new ArrayList<>();
        
        for (HubMessage m : msgs) {
            try {
                ret.add(m.message());
                updateVehicleWithLastSeenImei(m.imei, m.createdAt());
            }
            catch (Exception e) {
                String report = new String(Hex.decodeHex(m.msg.toCharArray()));
                Matcher matcher = p.matcher(report);
                if (matcher.matches()) {
                    String vehicle = matcher.group(3);
                    String timeOfDay = matcher.group(4);
                    String latMins = matcher.group(5);
                    String lonMins = matcher.group(6);
                    
                    String[] latParts = latMins.split(" ");
                    String[] lonParts = lonMins.split(" ");
                    double lat = getCoords(latParts);
                    double lon = getCoords(lonParts);
                    long timestamp = parseTimeString(timeOfDay).getTime();
                    
                    ImcSystem system = ImcSystemsHolder.getSystemWithName(vehicle);

                    if (system != null) {
                        system.setLocation(new LocationType(lat, lon), timestamp);
                    }

                    updateVehicleWithLastSeenImei(m.imei, m.createdAt() != null ? m.createdAt() : m.updatedAt());
                    NeptusLog.pub().info("Text report: {}", report);
                }
                
                IridiumCommand msg = new IridiumCommand();
                msg.command = new String(Hex.decodeHex(m.msg.toCharArray()));
                ret.add(msg);
            }
        }
        
        return ret;
    }

    public static void updateVehicleWithLastSeenImei(String imei, Date date) {
        VehicleType veh = VehiclesHolder.getVehicleWithImei(imei);
        if (veh != null) {
            IridiumArgs iridiumArgs = (IridiumArgs) veh.getProtocolsArgs().get(CommMean.IRIDIUM);
            if (iridiumArgs != null) {
                String lastSeen = iridiumArgs.getLastSeenImei();
                iridiumArgs.setLastSeenImei(imei, date);
                String lastSeenNew = iridiumArgs.getLastSeenImei();
                String imei0 = iridiumArgs.getImei();
                if (!lastSeen.equals(lastSeenNew)) {
                    NeptusLog.pub().info("Updated vehicle {} to last seen IMEI {} {} from " +
                                    "the old {} {} with date {}", veh.getId(), imei0.equals(lastSeenNew) ? "M1" : "M2",
                                    lastSeenNew, imei0.equals(lastSeenNew) ? "M2" : "M1", lastSeen, date);
                    NeptusEvents.post(Notification.warning(
                            I18n.textf("Iridium IMEI Change for %s", veh.getId()),
                            I18n.textf("Updated vehicle %vehicle to last seen IMEI %imeiOrdinal %newImei from " +
                                    "the old %oldImeiOrdinal %oldImei with date %date", veh.getId(), imei0.equals(lastSeenNew) ? "M1" : "M2",
                                    lastSeenNew, imei0.equals(lastSeenNew) ? "M2" : "M1", lastSeen, date)));
                }
            }
        }
    }

    public static Date parseTimeString(String timeOfDay) {
        GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        String[] timeParts = timeOfDay.split(":");
        date.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        date.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        date.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
        return date.getTime();
    }
    
    private double getCoords(String[] coordParts) {
        double coord = Double.parseDouble(coordParts[0]);
        coord += (coord > 0) ? Double.parseDouble(coordParts[1]) / 60.0 : -Double.parseDouble(coordParts[1]) / 60.0;
        return coord;
    }
    
    @Override
    public String getName() {
        return "HUB Iridium Messenger";
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    public HubSystemMsg[] retrieveSystems() throws Exception {
        Gson gson = new Gson();
        URL url = new URI(systemsUrl).toURL();
        URLConnection con = url.openConnection();
        if (authKey != null && !authKey.isEmpty()) {
            con.setRequestProperty ("Authorization", authKey);
        }
        return gson.fromJson(new InputStreamReader(con.getInputStream()), HubSystemMsg[].class);
    }
      
    
    public static String dateToString(Date d) {
        return dateFormat.format(d);
    }
    
    public static Date stringToDate(String d) {
        if (d == null || d.isEmpty())
            return null;

        try {
            return dateFormat.parse(d);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static class HubMessage {
        int type;
        String msg;
        String updated_at;
        boolean plaintext;

        // New fields for RockBlock messages
        String imei;
        int source = ImcId16.NULL_ID.intValue(); // imc id
        int destination = ImcId16.NULL_ID.intValue();; // imc id
        String created_at;

        public IridiumMessage message() throws Exception {
            byte[] data = Hex.decodeHex(msg.toCharArray());
            Date now = new Date();
            IridiumMessage irMsg = IridiumMessage.deserialize(data);
            if (source != ImcId16.NULL_ID.intValue())
                irMsg.source = source;
            if (destination != ImcId16.NULL_ID.intValue())
                irMsg.destination = destination;
            if (irMsg.source == ImcId16.NULL_ID.intValue()) {
                // Let us try to fill the source from imei
                irMsg.source = findSystemIdByImei(imei);
            }
            if (!new Date(irMsg.timestampMillis).before(now) &&
                    created_at != null && stringToDate(created_at) != null) {
                irMsg.timestampMillis = stringToDate(created_at).getTime();
            }
            return irMsg;
        }

        public static int findSystemIdByImei(String imei) {
            VehicleType vt = VehiclesHolder.getVehicleWithImei(imei);
            if (vt == null) {
                return ImcId16.NULL_ID.intValue();
            }

            ImcSystem imcSys = ImcSystemsHolder.getSystemWithName(vt.getId());
            if (imcSys == null) {
                return vt.getImcId().intValue();
            }
            return imcSys.getId().intValue();
        }

        public byte[] messageRaw() {
            try {
                return Hex.decodeHex(msg.toCharArray());
            } catch (Exception e) {
                return null;
            }
        }

        public Date updatedAt() {
            return stringToDate(updated_at);
        }

        public Date createdAt() {
            return stringToDate(created_at);
        }
    }
    
    public static class HubSystemMsg {
        public long imcid;
        public String name;
        public String updated_at;
        public String created_at;
        public Double[] coordinates;
        public String pos_error_class;
        
        public Date updatedAt() {
            return stringToDate(updated_at);
        }
        
        public Date createdAt() {
            return stringToDate(created_at);
        }
    }
    
    @Override
    public void cleanup() {
        listeners.clear();
        //stopPolling();
    }

    @Override
    public String toString() {
        return getName();
    }

    public static void main(String[] args) throws Exception {
        GeneralPreferences.ripplesUrl = "https://ripples.lsts.pt";

        HubIridiumMessenger messenger = new HubIridiumMessenger();
        Date d = new Date(System.currentTimeMillis() - (1000 * 3600 * 5));

        System.out.println(dateToString(d));
        System.out.println(d.getTime());
        Collection<IridiumMessage> msgs = messenger.pollMessages(d);
        System.out.println(msgs.size());
        msgs.forEach(m -> System.out.println(m.asImc()));

        byte[] msg = new byte[]{0x24, 0x01, 0x00, 0x2a, (byte) 0xa5};
        IridiumManager.getManager().sendRaw("caravel",
                "300125060492800", msg);
    }
}
