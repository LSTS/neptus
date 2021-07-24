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
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.Position.PosType;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

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
    protected int timeoutMillis = 10000;
    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();
    private static Pattern p = Pattern.compile("\\((.)\\) \\((.*)\\) (.*) / (.*), (.*) / .*");
    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static { dateFormat.setTimeZone(tz); }
    
   // protected Thread t = null;

    // public HubIridiumMessenger() {
    //  startPolling();
    // }
    
    public DeviceUpdate pollActiveDevices() throws Exception {
        Gson gson = new Gson();
        URL url = new URL(activeSystemsUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (authKey != null && !authKey.isEmpty()) {
            con.setRequestProperty ("Authorization", authKey);
        }

        HubSystemMsg[] sys = gson.fromJson(new InputStreamReader(con.getInputStream()), HubSystemMsg[].class);
        
        DeviceUpdate up = new DeviceUpdate();
        for (HubSystemMsg s : sys) {
            if (s.imcid > Integer.MAX_VALUE)
                continue;
            Position pos = new Position();
            pos.id = (int) s.imcid;
            pos.latRads = s.coordinates[0];
            pos.lonRads = s.coordinates[1];
            pos.timestamp = stringToDate(s.updated_at).getTime() / 1000.0;
            pos.posType = PosType.Unknown;
            up.getPositions().put(pos.id, pos);
        }
        
        return up;
    }
    
//    public void startPolling() {
//        if (t != null)
//            stopPolling();
//        t = new Thread("Hub Iridium message updater") {
//            @Override
//            public void run() {
//                Date lastTime = null;
//                while (true) {
//                    try {
//                        Thread.sleep(60 * 1000);
//                        
//                        if (lastTime == null)
//                            lastTime = new Date(System.currentTimeMillis() - (3600 * 1000));
//                        
//                        Collection<IridiumMessage> newMessages = pollMessages(lastTime);
//                        lastTime = new Date();
//                        
//                        for (IridiumMessage m : newMessages)
//                            for (IridiumMessageListener listener : listeners)
//                                listener.messageReceived(m);                        
//                    }
//                    catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        };
//        t.setDaemon(true);
//        t.start();
//    }
    
//    public void stopPolling() {
//        if (t != null)
//            t.interrupt();
//        t = null;
//    }
//    
    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);       
    }
    
    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
     
        byte[] data = msg.serialize();
        data = new String(Hex.encodeHex(data)).getBytes();
        
        URL u = new URL(messagesUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "Content-Type", "application/hub" );
        conn.setRequestProperty( "Content-Length", String.valueOf(data.length * 2) );
        conn.setConnectTimeout(timeoutMillis);
        if (authKey != null && !authKey.isEmpty()) {
            conn.setRequestProperty ("Authorization", authKey);
        }
        
        OutputStream os = conn.getOutputStream();
        os.write(data);
        os.close();

        NeptusLog.pub().info(messagesUrl+" : "+conn.getResponseCode()+" "+conn.getResponseMessage());
        
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream incoming = new ByteArrayOutputStream();
        IOUtils.copy(is, incoming);
        is.close();
        
        NeptusLog.pub().info("Sent "+msg.getClass().getSimpleName()+" through HTTP: "+conn.getResponseCode()+" "+conn.getResponseMessage());        
        try {
            logHubInteraction(msg.getClass().getSimpleName()+" ("+msg.getMessageType()+")", messagesUrl, conn.getRequestMethod(), ""+conn.getResponseCode(), ByteUtil.encodeToHex(msg.serialize()), new String(incoming.toByteArray()));
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        
        if (conn.getResponseCode() != 200) {
            throw new Exception("Server returned "+conn.getResponseCode()+": "+conn.getResponseMessage());
        }
    }

    public synchronized void logHubInteraction(String message, String url, String method, String statusCode, String requestData, String responseData) throws Exception {
        if (! (new File("log/hub.log")).exists()) {
            BufferedWriter tmp = new BufferedWriter(new FileWriter(new File("log/hub.log"), false));
            tmp.write("Time of Day, Message Type, URL, Method, Status Code, Request Data (hex encoded), Response Data\n");
            tmp.close();
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
        
        NeptusLog.pub().info("Polling messages since "+dateToString(timeSince));
        
        String since = null;
        if (timeSince != null)
            since = dateToString(timeSince);
        URL u = new URL(messagesUrl+"?since="+(timeSince.getTime()/1000));
        if (since == null)
            u = new URL(messagesUrl);
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
        
        logHubInteraction("Iridium Poll", u.toString(), conn.getRequestMethod(), ""+conn.getResponseCode(), "", new String(baos.toByteArray()));
        
        if (conn.getResponseCode() != 200)
            throw new Exception("Hub iridium server returned "+conn.getResponseCode()+": "+conn.getResponseMessage());
        HubMessage[] msgs = gson.fromJson(baos.toString(), HubMessage[].class);
        
        Vector<IridiumMessage> ret = new Vector<>();        
        
        for (HubMessage m : msgs) {
            try {
                ret.add(m.message());
            }
            catch (Exception e) {
                
                String report = new String(Hex.decodeHex(m.msg.toCharArray()));
                Matcher matcher = p.matcher(report);
                if (matcher.matches()) {
                    String vehicle = matcher.group(2);
                    String timeOfDay = matcher.group(3);
                    String latMins = matcher.group(4);
                    String lonMins = matcher.group(5);
                    
                    String latParts[] = latMins.split(" ");
                    String lonParts[] = lonMins.split(" ");
                    double lat = getCoords(latParts);
                    double lon = getCoords(lonParts);
                    long timestamp = parseTimeString(timeOfDay).getTime();
                    
                    ImcSystem system = ImcSystemsHolder.getSystemWithName(vehicle);
                    
                    if (system != null) {
                        system.setLocation(new LocationType(lat, lon), timestamp);
                    }
                    
                    NeptusLog.pub().info("Text report: "+report);
                }
                
                IridiumCommand msg = new IridiumCommand();
                msg.command = new String(Hex.decodeHex(m.msg.toCharArray()));
                ret.add(msg);
            }
        }
        
        return ret;
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
        URL url = new URL(systemsUrl);
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
        try {
            return dateFormat.parse(d);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static class HubMessage {
        int type;
        String msg;
        String updated_at;
        boolean plaintext;
        
        public IridiumMessage message() throws Exception {
            byte[] data = Hex.decodeHex(msg.toCharArray());
            return IridiumMessage.deserialize(data);
        }
        
        public Date updatedAt() {
            return stringToDate(updated_at);
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
    
    public static void main(String[] args) throws Exception {
        GeneralPreferences.ripplesUrl = "https://ripples.lsts.pt";
        
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        Date d = new Date(System.currentTimeMillis() - (1000 * 3600 * 5));
        
        System.out.println(dateToString(d));
        System.out.println(d.getTime());
        Collection<IridiumMessage> msgs = messenger.pollMessages(d);
        System.out.println(msgs.size());
        msgs.stream().forEach(m -> {
            System.out.println(m.asImc());
        });
        
        
        
    }
    
    @Override
    public String toString() {
        return getName();                
    }
}
