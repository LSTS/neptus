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
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.neptus.NeptusLog;

import com.google.gson.Gson;

/**
 * @author zp
 *
 */
@IridiumProvider(id="hub", name="HUB Iridium Messenger", description="Uses the HUB web server to send and receive messages")
public class HubIridiumMessenger implements IridiumMessenger {

    protected boolean available = true;
    protected String serverUrl = "http://hub.lsts.pt/api/v1/";
    protected String systemsUrl = serverUrl+"systems";
    protected String activeSystemsUrl = systemsUrl+"/active";
    protected String messagesUrl = serverUrl+"iridium";
    protected int timeoutMillis = 10000;
    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();
    
    private static TimeZone tz = TimeZone.getTimeZone("UTC");
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static { dateFormat.setTimeZone(tz); }
    
    protected Thread t = null;

    // public HubIridiumMessenger() {
    //  startPolling();
    // }
    
    public DeviceUpdate pollActiveDevices() throws Exception {
        Gson gson = new Gson();
        URL url = new URL(activeSystemsUrl);        
        HubSystemMsg[] sys = gson.fromJson(new InputStreamReader(url.openStream()), HubSystemMsg[].class);
        
        DeviceUpdate up = new DeviceUpdate();
        for (HubSystemMsg s : sys) {
            DeviceUpdate.Position pos = new DeviceUpdate.Position();
            pos.id = s.imcid;
            pos.latitude = s.coordinates[0];
            pos.longitude = s.coordinates[1];
            pos.timestamp = stringToDate(s.updated_at).getTime() / 1000.0;
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
        OutputStream os = conn.getOutputStream();
        os.write(data);
        NeptusLog.pub().info("Sent "+msg.getClass().getSimpleName()+" through HTTP: "+conn.getResponseCode()+" "+conn.getResponseMessage());        
        if (conn.getResponseCode() != 201) {
            throw new Exception("Server returned "+conn.getResponseCode()+": "+conn.getResponseMessage());
        }
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        
        System.out.println("Polling messages since "+dateToString(timeSince));
        
        String since = null;
        if (timeSince != null)
            since = dateToString(timeSince);
        URL u = new URL(messagesUrl+"?since="+since);
        if (since == null)
            u = new URL(messagesUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod( "GET" );
        conn.setConnectTimeout(timeoutMillis);
        Gson gson = new Gson();  
        
        if (conn.getResponseCode() != 200)
            throw new Exception("Hub iridium server returned "+conn.getResponseCode()+": "+conn.getResponseMessage());
        HubMessage[] msgs = gson.fromJson(new InputStreamReader(conn.getInputStream()), HubMessage[].class);
        
        Vector<IridiumMessage> ret = new Vector<>();        
        
        for (HubMessage m : msgs)
            ret.add(m.message());
        
        System.out.println(msgs.length+" messages retrieved");
        
        return ret;
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
        return gson.fromJson(new InputStreamReader(url.openStream()), HubSystemMsg[].class);        
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
        
        public IridiumMessage message() throws Exception {
            char[] chars = new char[msg.length()];
            msg.getChars(0, msg.length(), chars, 0);
            byte[] data = Hex.decodeHex(chars);
            return IridiumMessage.deserialize(data);
        }
        
        public Date updatedAt() {
            return stringToDate(updated_at);
        }
    }
    
    public static class HubSystemMsg {
        
        int imcid;
        String name;
        String updated_at;
        Double[] coordinates;
        
        public Date updatedAt() {
            return stringToDate(updated_at);
        }
    }
    
    
    @Override
    public void cleanup() {
        listeners.clear();
        //stopPolling();
    }
    
    public static void main(String[] args) throws Exception {
        HubIridiumMessenger messenger = new HubIridiumMessenger();
        Date d = new Date(System.currentTimeMillis() - (1000 * 3600 * 60));
        System.out.println(dateToString(d));
        System.out.println(messenger.pollMessages(d).size());
    }
    
    @Override
    public String toString() {
        return getName();                
    }
}
