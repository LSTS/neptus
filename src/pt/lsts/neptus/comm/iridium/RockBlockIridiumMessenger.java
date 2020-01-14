/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.comm.protocol.IridiumArgs;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This class uses the RockBlock HTTP API (directly) to send messages to Iridium destinations and a gmail inbox to poll
 * for incoming messages
 * 
 * @see http://rockblock.rock7mobile.com/downloads/RockBLOCK-Web-Services-User-Guide.pdf
 * @author zp
 */
@IridiumProvider(id="rock7", name="RockBlock Messenger", description="Sends Iridium messages directly via RockBlock web service and receives new messages by polling a gmail address")
public class RockBlockIridiumMessenger implements IridiumMessenger {

    protected boolean available = true;
    protected String serverUrl = "https://secure.rock7mobile.com/rockblock/MT";
    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();
    private static long lastSuccess = -1;

    @NeptusProperty
    private boolean alwaysAskForPassword = false;

    @NeptusProperty
    private String rockBlockPassword = null;

    @NeptusProperty
    private String rockBlockUsername = null;

    @NeptusProperty
    private String gmailUsername = null;

    @NeptusProperty
    private String gmailPassword = null;

    @NeptusProperty
    private String gmailAccount = "lsts.iridium";

    private boolean askRockBlockPassword = true;
    private boolean askGmailPassword = true;

    {
        try {
            PluginUtils.loadProperties("conf/rockblock.props", this);
        }
        catch (Exception e) {
        }
        askGmailPassword = askRockBlockPassword = alwaysAskForPassword;
    }

    private String getRockBlockUsername() {
        if (rockBlockUsername == null)
            return "";
        return rockBlockUsername;
    }

    private String getRockBlockPassword() {
        if (rockBlockPassword == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(rockBlockPassword));
    }

    private void setRockBlockPassword(String password) {
        if (password == null)
            this.rockBlockPassword = null;

        this.rockBlockPassword = DatatypeConverter.printBase64Binary(password.getBytes(Charset.forName("UTF8")));
    }

    private void setRockBlockUsername(String username) {
        if (username == null)
            this.rockBlockUsername = null;
        this.rockBlockUsername = username;
    }

    private String getGmailUsername() {
        if (gmailUsername == null)
            return "";
        return gmailUsername;
    }

    private String getGmailPassword() {
        if (gmailPassword == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(gmailPassword));
    }

    private void setGmailPassword(String password) {
        if (password == null)
            this.gmailPassword = null;

        this.gmailPassword = DatatypeConverter.printBase64Binary(password.getBytes(Charset.forName("UTF8")));
    }

    private void setGmailUsername(String username) {
        if (username == null)
            this.gmailUsername = null;
        this.gmailUsername = username;
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(msg.getDestination()));
        if (vt == null) {
            throw new Exception("Cannot send message to an unknown destination");
        }
        IridiumArgs args = (IridiumArgs) vt.getProtocolsArgs().get("iridium");
        if (askRockBlockPassword || rockBlockPassword == null || rockBlockUsername == null) {
            Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter RockBlock Credentials", getRockBlockUsername(), getRockBlockPassword());
            if (credentials == null)
                return;
            setRockBlockUsername(credentials.first());
            setRockBlockPassword(credentials.second());
            PluginUtils.saveProperties("conf/rockblock.props", this);
            askRockBlockPassword = false;
        }
        
        String result = sendToRockBlockHttp(args.getImei(), getRockBlockUsername(), getRockBlockPassword(),
                msg.serialize());

        if (result != null) {
            if (!result.split(",")[0].equals("OK")) {
                throw new Exception("RockBlock server failed to deliver the message: '" + result + "'");
            }
        }
    }

    static final SSLConnectionSocketFactory sslsf;
    static final Registry<ConnectionSocketFactory> registry;
    static final PoolingHttpClientConnectionManager cm;
    
    static {
        
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(),
                    NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();
        cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(100);
    }
    
    public static String sendToRockBlockHttp(String destImei, String username, String password, byte[] data)
            throws HttpException, IOException {

        CloseableHttpClient client = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .build();
                

        HttpPost post = new HttpPost("https://secure.rock7mobile.com/rockblock/MT");
        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("imei", destImei));
        urlParameters.add(new BasicNameValuePair("username", username));
        urlParameters.add(new BasicNameValuePair("password", password));
        urlParameters.add(new BasicNameValuePair("data", ByteUtil.encodeToHex(data)));
        
        

        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse response = client.execute(post);

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        
        try {
            client.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return result.toString();
    }

    private Pattern pattern = Pattern.compile("APPLICATION/OCTET-STREAM; name=(\\d+)-(\\d+)\\.bin");

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {

        if (askGmailPassword || gmailPassword == null || gmailUsername == null) {
            Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter Gmail Credentials", getGmailUsername(), getGmailPassword());
            if (credentials == null)
                return null;
            setGmailUsername(credentials.first());
            setGmailPassword(credentials.second());
            PluginUtils.saveProperties("conf/rockblock.props", this);
            askGmailPassword = false;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        ArrayList<IridiumMessage> messages = new ArrayList<>();
        try {
            Session session = Session.getDefaultInstance(props, null);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", getGmailUsername(), getGmailPassword());

            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_ONLY);
            int numMsgs = inbox.getMessageCount();

            for (int i = numMsgs; i > 0; i--) {
                Message m = inbox.getMessage(i);
                if (m.getReceivedDate().before(timeSince)) {
                    break;
                }
                else {
                    MimeMultipart mime = (MimeMultipart) m.getContent();
                    for (int j = 0; j < mime.getCount(); j++) {
                        BodyPart p = mime.getBodyPart(j);
                        Matcher matcher = pattern.matcher(p.getContentType());
                        if (matcher.matches()) {
                            InputStream stream = (InputStream) p.getContent();
                            byte[] data = IOUtils.toByteArray(stream);
                            IridiumMessage msg = process(data, matcher.group(1));
                            if (msg != null)
                                messages.add(msg);
                        }
                    }
                }
            }
        }
        catch (NoSuchProviderException ex) {
            ex.printStackTrace();
            return new Vector<>();
        }
        catch (MessagingException ex) {
            ex.printStackTrace();
            return new Vector<>();
        }

        return messages;
    }

    private IridiumMessage process(byte[] data, String from) {
        try {
            return IridiumMessage.deserialize(data);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return rockBlockIsReachable().get();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getName() {
        return "RockBlock Messenger";
    }

    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cleanup() {
        // TODO Auto-generated method stub

    }

    public static Future<Boolean> rockBlockIsReachable() {
        return new Future<Boolean>() {
            Boolean result = null;
            boolean canceled = false;
            long start = System.currentTimeMillis();
            {

                if (System.currentTimeMillis() - lastSuccess < 15000) {
                    result = true;
                }

                try {
                    URL url = new URL("https://secure.rock7mobile.com/rockblock");
                    int len = url.openConnection().getContentLength();
                    if (len > 0)
                        lastSuccess = System.currentTimeMillis();
                    result = len > 0;
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    result = false;
                }
            }

            @Override
            public Boolean get() throws InterruptedException, ExecutionException {
                while (result == null) {
                    Thread.sleep(100);
                }
                return result;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                canceled = true;
                return false;
            }

            @Override
            public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                while (result == null) {
                    Thread.sleep(100);
                    if (System.currentTimeMillis() - start > unit.toMillis(timeout))
                        throw new TimeoutException("Time out while connecting");
                }
                return result;
            }

            @Override
            public boolean isCancelled() {
                return canceled;
            }

            @Override
            public boolean isDone() {
                return result != null;
            }
        };
    }
    
    @Override
    public String toString() {
        return getName();                
    }

    public static void main(String[] args) throws Exception {
        RockBlockIridiumMessenger messenger = new RockBlockIridiumMessenger();
        ConfigFetch.initialize();
        for (IridiumMessage msg : messenger.pollMessages(new Date(System.currentTimeMillis() - 3600000))) {
            System.out.println(msg);
        }
    }
}
