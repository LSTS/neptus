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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
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
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

import static pt.lsts.neptus.comm.iridium.HubIridiumMessenger.updateVehicleWithLastSeenImei;

/**
 * This class uses the RockBlock HTTP API (directly) to send messages to Iridium destinations and a gmail inbox to poll
 * for incoming messages
 * 
 * @see https://www.groundcontrol.com/wp-content/uploads/2022/02/RockBLOCK-Web-Services-User-Guide.pdf
 * @author zp
 */
@IridiumProvider(id="rock7", name="RockBlock Messenger", description="Sends Iridium messages directly via RockBlock web service and receives new messages by polling a gmail address")
public class RockBlockIridiumMessenger implements IridiumMessenger {

    protected HttpClientConnectionHelper httpComm;

    protected boolean available = true;
    protected static String serverUrl = "https://secure.rock7mobile.com/rockblock/MT";
    protected Set<IridiumMessageListener> listeners = new HashSet<>();
    private static long lastSuccess = -1;

    private final Pattern patternText = Pattern.compile("TEXT/PLAIN; charset=([a-zA-Z0-9-]+)");
    private final Pattern patternAttach = Pattern.compile("APPLICATION/OCTET-STREAM; name=(\\d+)-(\\d+)\\.bin");

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

    public static final String CONF_ROCKBLOCK_PROPS = ".cache/rockblock.props";

    {
        try {
            PluginUtils.loadProperties(CONF_ROCKBLOCK_PROPS, this);
        }
        catch (Exception e) {
        }
        askGmailPassword = askRockBlockPassword = alwaysAskForPassword;

        httpComm = new HttpClientConnectionHelper(HttpClientConnectionHelper.MAX_TOTAL_CONNECTIONS,
                HttpClientConnectionHelper.DEFAULT_MAX_CONNECTIONS_PER_ROUTE, 1000, true);
        httpComm.setRegistryNoHostNameVerifier();
        httpComm.initializeComm();
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

        this.rockBlockPassword = DatatypeConverter.printBase64Binary(password.getBytes(StandardCharsets.UTF_8));
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

        this.gmailPassword = DatatypeConverter.printBase64Binary(password.getBytes(StandardCharsets.UTF_8));
    }

    private void setGmailUsername(String username) {
        if (username == null)
            this.gmailUsername = null;
        this.gmailUsername = username;
    }

    private boolean askCredentials() throws IOException {
        if (askRockBlockPassword || rockBlockPassword == null || rockBlockUsername == null) {
            Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter RockBlock Credentials", getRockBlockUsername(), getRockBlockPassword());
            if (credentials == null)
                return true;
            setRockBlockUsername(credentials.first());
            setRockBlockPassword(credentials.second());
            PluginUtils.saveProperties(CONF_ROCKBLOCK_PROPS, this);
            askRockBlockPassword = false;
        }
        return false;
    }

    private void checkResponseFromServer(String result) throws Exception {
        if (!result.split(",")[0].equals("OK")) {
            String[] errorCode = result.split(",");
            if (errorCode[0].equalsIgnoreCase("FAILED") && errorCode[1].equalsIgnoreCase("10")) {
                // 'FAILED,10,Invalid login credentials'
                askRockBlockPassword = true;
            }
            throw new Exception("RockBlock server failed to deliver the message: '" + result + "'");
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(msg.getDestination()));
        if (vt == null) {
            throw new Exception("Cannot send message to an unknown destination");
        }
        IridiumArgs args = (IridiumArgs) vt.getProtocolsArgs().get("iridium");

        if (askCredentials())
            return;

        String result = sendToRockBlockHttp(args.getLastSeenImei(), getRockBlockUsername(), getRockBlockPassword(),
                msg.serialize());
        checkResponseFromServer(result);
    }

    @Override
    public void sendMessageRaw(String destinationName, String imeiAddr, byte[] data) throws Exception {
        if (imeiAddr == null || imeiAddr.trim().isEmpty()) {
            VehicleType vt = VehiclesHolder.getVehicleById(destinationName);
            if (vt == null) {
                throw new Exception("Cannot send message to an unknown destination");
            }
            IridiumArgs args = (IridiumArgs) vt.getProtocolsArgs().get("iridium");
            imeiAddr = args.getLastSeenImei();
        }

        if (askCredentials())
            return;

        String result = sendToRockBlockHttp(imeiAddr, getRockBlockUsername(), getRockBlockPassword(), data);
        checkResponseFromServer(result);
    }

    public String sendToRockBlockHttp(String destImei, String username, String password, byte[] data)
            throws IOException {

        try {
            HttpPost post = new HttpPost(serverUrl);
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair("imei", destImei));
            urlParameters.add(new BasicNameValuePair("username", username));
            urlParameters.add(new BasicNameValuePair("password", password));
            urlParameters.add(new BasicNameValuePair("data", ByteUtil.encodeToHex(data)));

            post.setEntity(new UrlEncodedFormEntity(urlParameters));
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpClientContext context = HttpClientContext.create();
            try (CloseableHttpResponse response = httpComm.getClient().execute(post, context)) {
                httpComm.autenticateProxyIfNeeded(response, context);

                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line = "";
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
            catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        catch (Exception e) {
            throw e;
        }
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {

        if (askGmailPassword || gmailPassword == null || gmailUsername == null) {
            Pair<String, String> credentials = GuiUtils.askCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter Gmail Credentials", getGmailUsername(), getGmailPassword());
            if (credentials == null)
                return null;
            setGmailUsername(credentials.first());
            setGmailPassword(credentials.second());
            PluginUtils.saveProperties(CONF_ROCKBLOCK_PROPS, this);
            askGmailPassword = false;
        }

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        ArrayList<IridiumMessage> messages = new ArrayList<>();
        Store store = null;
        try {
            Session session = Session.getDefaultInstance(props, null);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", getGmailUsername(), getGmailPassword());

            Folder inbox = store.getFolder("Inbox");
            inbox.open(Folder.READ_ONLY);
            int numMsgs = inbox.getMessageCount();

            for (int i = numMsgs; i > 0; i--) {
                Message m = inbox.getMessage(i);
                Date transmiteDate = null;
                String fromImei = "";
                String seqNumber = "";
                byte[] data = null;
                if (m.getReceivedDate().before(timeSince)) {
                    break;
                }

                if (m.getContent() instanceof String) {
                    // No attach data so empty msg
                    String text = (String) m.getContent();
                    String[] partsList = text.split("\n");
                    for (String prt : partsList) {
                        if (prt.trim().isEmpty())
                            continue;
                        prt = prt.trim();
                        if (prt.startsWith("Transmit Time:")) {
                            String trmTime = prt.split("e: ")[1].trim();
                            transmiteDate = getTransmitDateToDate(trmTime);
                        } else if (prt.startsWith("IMEI:")) {
                            fromImei = prt.split(":")[1].trim();
                        } else if (prt.startsWith("MOMSN:")) {
                            seqNumber = prt.split(":")[1].trim();
                        }
                    }
                }
                else if (m.getContent() instanceof MimeMultipart) {
                    MimeMultipart mime = (MimeMultipart) m.getContent();
                    for (int j = 0; j < mime.getCount(); j++) {
                        BodyPart p = mime.getBodyPart(j);
                        Matcher matcher = patternText.matcher(p.getContentType());
                        if (matcher.matches()) {
                            String text = (String) p.getContent();
                            String[] partsList = text.split("\n");
                            for (String prt : partsList) {
                                if (prt.trim().isEmpty())
                                    continue;
                                prt = prt.trim();
                                if (prt.startsWith("Transmit Time:")) {
                                    String trmTime = prt.split("e: ")[1].trim();
                                    transmiteDate = getTransmitDateToDate(trmTime);
                                } else if (prt.startsWith("IMEI:")) {
                                    fromImei = prt.split(":")[1].trim();
                                } else if (prt.startsWith("MOMSN:")) {
                                    seqNumber = prt.split(":")[1].trim();
                                }
                            }
                            continue;
                        }
                        matcher = patternAttach.matcher(p.getContentType());
                        if (matcher.matches()) {
                            InputStream stream = (InputStream) p.getContent();
                            data = IOUtils.toByteArray(stream);
                            fromImei = matcher.group(1);
                            seqNumber = matcher.group(2);
                        }
                    }
                }

                if (fromImei == null || fromImei.isEmpty())
                    continue;
                IridiumMessage msg = process(data, fromImei, seqNumber,
                        transmiteDate != null ? transmiteDate
                                : (m.getSentDate() == null ? m.getReceivedDate() : m.getSentDate()));
                if (msg != null)
                    messages.add(msg);
            }
        }
        catch (AuthenticationFailedException ex) {
            askGmailPassword = true;
            ex.printStackTrace();
            return new ArrayList<>();
        }
        catch (NoSuchProviderException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
        catch (MessagingException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try {
                if (store != null)
                    store.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            store = null;
        }

        messages.sort((m1, m2) -> Long.compare(m1.timestampMillis, m2.timestampMillis));
        return messages;
    }

    private static Date getTransmitDateToDate(String trmTime) {
        // Parse date in format: 2025-01-20T14:16:03Z UTC
        trmTime = trmTime.replaceFirst(" UTC", "");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME;
        ZonedDateTime dateTime = ZonedDateTime.parse(trmTime, formatter);
        return Date.from(dateTime.toInstant());
    }

    private IridiumMessage process(byte[] data, String fromImei, String seqNumber, Date sentDate) {
        try {
            updateVehicleWithLastSeenImei(fromImei, sentDate);
            if (data == null || data.length == 0)
                return null;

            Date now = new Date();
            IridiumMessage irMsg = IridiumMessage.deserialize(data);
            if (irMsg.source == ImcId16.NULL_ID.intValue()) {
                // Let us try to fill the source from imei
                irMsg.source = HubIridiumMessenger.HubMessage.findSystemIdByImei(fromImei);
            }

            // If not set, set the timestamp
            if (!new Date(irMsg.timestampMillis).before(now) && sentDate != null) {
                irMsg.timestampMillis = sentDate.getTime();
            }

            return irMsg;
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
        httpComm.cleanUp();
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
                    URL url = new URL("http://secure.rock7mobile.com/rockblock");
                    NeptusLog.pub().info("Checking RockBlock server at {}", url);
                    int len = url.openConnection().getContentLength();
                    if (len > 0)
                        lastSuccess = System.currentTimeMillis();
                    result = len > 0;
                    NeptusLog.pub().info("RockBlock server is {}reachable", result ? "" : "NOT ");
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
