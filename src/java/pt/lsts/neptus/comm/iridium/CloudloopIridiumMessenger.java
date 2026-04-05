/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * Apr 2, 2026
 */
package pt.lsts.neptus.comm.iridium;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.comm.protocol.CloudloopArgs;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import static pt.lsts.neptus.comm.iridium.HubIridiumMessenger.updateVehicleWithLastSeenImei;

/**
 * This class uses the Cloudloop HTTP API (directly) to send and receive messages to Iridium destinations.
 * 
 * @see https://api.cloudloop.com/swagger/index.html
 * @author Paulo Dias
 */
@IridiumProvider(id="cloudloop", name="Cloudloop Messenger", description="Sends and receives Iridium messages directly via Cloudloop web service")
public class CloudloopIridiumMessenger implements IridiumMessenger {

    private static final Pattern p = Pattern.compile("(\\((.)\\) )?\\((.*)\\) (.*) / (.*), (.*) / .*");
    private static final TimeZone tz = TimeZone.getTimeZone("UTC");
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    static { dateFormat.setTimeZone(tz); }

    protected HttpClientConnectionHelper httpComm;

    protected boolean available = true;
    protected static String serverUrl = "https://api.cloudloop.com";
    protected static String endpointPlatformPing = "/Platform/Ping";
    protected static String endpointDataSendSbdMessage = "/Data/DoSendSbdMessage";
    protected static String endpointDataGetMessageRecords = "/Data/GetMessageRecords";
    protected static String endpointDataGetMessageRecordsLongPoll = "/Data/GetMessageRecordsLongPoll";
    protected static String endpointDataGetLingoMo = "/Data/GetLingoMo";
    protected static String endpointDataGetLingoMt = "/Data/GetLingoMt";

    protected Set<IridiumMessageListener> listeners = new HashSet<>();
    private static long lastSuccess = -1;

    private final Pattern patternText = Pattern.compile("TEXT/PLAIN; charset=([a-zA-Z0-9-]+)");
    private final Pattern patternAttach = Pattern.compile("APPLICATION/OCTET-STREAM; name=(\\d+)-(\\d+)\\.bin");

    @NeptusProperty
    private boolean alwaysAskForToken = false;

    @NeptusProperty
    private String cloudloopToken = null;

    private boolean askCloudloopToken = true;

    public static final String CONF_CLOUDLOOP_PROPS = ".cache/cloudloop.props";

    {
        try {
            PluginUtils.loadProperties(CONF_CLOUDLOOP_PROPS, this);
        }
        catch (Exception ignored) {
        }
        httpComm = new HttpClientConnectionHelper(HttpClientConnectionHelper.MAX_TOTAL_CONNECTIONS,
                HttpClientConnectionHelper.DEFAULT_MAX_CONNECTIONS_PER_ROUTE, 1000, true);
        httpComm.setRegistryNoHostNameVerifier();
        httpComm.initializeComm();
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

    private String getCloudloopToken() {
        if (cloudloopToken == null)
            return "";
        return StringUtils.newStringUtf8(DatatypeConverter.parseBase64Binary(cloudloopToken));
    }

    private void setCloudloopToken(String password) {
        if (password == null) {
            this.cloudloopToken = null;
            return;
        }

        this.cloudloopToken = DatatypeConverter.printBase64Binary(password.getBytes(StandardCharsets.UTF_8));
    }

    private boolean askCredentials() throws IOException {
        if (askCloudloopToken || cloudloopToken == null) {
            String tokenSecret = GuiUtils.askSecretCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter Cloudloop token", "token", getCloudloopToken());
            if (tokenSecret == null)
                return true;
            setCloudloopToken(tokenSecret);
            PluginUtils.saveProperties(CONF_CLOUDLOOP_PROPS, this);
            askCloudloopToken = false;
        }
        return false;
    }

    private void checkResponseFromServer(String result) throws Exception {
        JsonValue arrVal = Json.parse(result);
        if (arrVal.isArray()) {
            JsonArray arr = arrVal.asArray();
            // should be ok
        } else if (arrVal.isObject()) {
            JsonObject jsonObject = arrVal.asObject();
            // May be the result
            JsonValue errorNode = jsonObject.get("error");
            if (errorNode != null) {
                String error = errorNode.asString();
                if ("DispatcherAuthenticationException".equalsIgnoreCase(error)) {
                    // 'FAILED,10,Invalid login credentials'
                    askCloudloopToken = true;
                }
                throw new Exception("Cloudloop server failed to deliver the message: '" + result + "'");
            }
        }
    }

    @Override
    public void sendMessage(IridiumMessage msg) throws Exception {
        VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(msg.getDestination()));
        if (vt == null) {
            throw new Exception("Cannot send message to an unknown destination");
        }
        CloudloopArgs args = (CloudloopArgs) vt.getProtocolsArgs().get("cloudloop");

        if (askCredentials())
            return;

        String destThingId = getThingIdToUse(args);
        String result = sendToCloudloopHttp(destThingId, getCloudloopToken(), msg.serialize());
        checkResponseFromServer(result);
    }

    @Override
    public void sendMessageRaw(String destinationName, String thingIdAddr, byte[] data) throws Exception {
        if (thingIdAddr == null || thingIdAddr.trim().isEmpty()) {
            VehicleType vt = VehiclesHolder.getVehicleById(destinationName);
            if (vt == null) {
                throw new Exception("Cannot send message to an unknown destination");
            }
            CloudloopArgs args = (CloudloopArgs) vt.getProtocolsArgs().get("cloudloop");
            thingIdAddr = getThingIdToUse(args);
        }

        if (askCredentials()) {
            return;
        }

        String result = sendToCloudloopHttp(thingIdAddr, getCloudloopToken(), data);
        checkResponseFromServer(result);
    }

    private static String getThingIdToUse(CloudloopArgs args) {
        String destThingId = args.getLastSeenThingId();
        int thingIdIdx = GeneralPreferences.iridiumModemDefaultIndexToUse;
        if (thingIdIdx > 0) {
            switch (thingIdIdx) {
                case 1:
                    destThingId = args.getThingId();
                    break;
                case 2:
                default:
                    destThingId = args.getThingId1();
            }
        }
        return destThingId;
    }
    
    public String sendToCloudloopHttp(String destThingId, String token, byte[] data)
            throws IOException {

        // Construct the URL with query parameters
        String requestUrl = String.format("%s%s?thing=%s&message=%s&token=%s",
                serverUrl,
                endpointDataSendSbdMessage,
                destThingId,
                ByteUtil.encodeToHex(data),
                getCloudloopToken());
        HttpPost post = new HttpPost(requestUrl);

        HttpClientContext context = HttpClientContext.create();
        try (CloseableHttpResponse response = httpComm.getClient().execute(post, context)) {
            httpComm.autenticateProxyIfNeeded(response, context);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        }
        catch (Exception e) {
            NeptusLog.pub().warn("Error sending message to Cloudloop " +  e.getMessage());
            throw e;
        }
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {

        if (askCloudloopToken || cloudloopToken == null) {
            String credentials = GuiUtils.askSecretCredentials(ConfigFetch.getSuperParentFrame(),
                    "Enter Cloudloop Token Credentials", "token", getCloudloopToken());
            if (credentials == null)
                return null;
            setCloudloopToken(credentials);
            PluginUtils.saveProperties(CONF_CLOUDLOOP_PROPS, this);
            askCloudloopToken = false;
        }
        NeptusLog.pub().info("Polling messages since {}", dateToString(timeSince));

        // Construct the URL with query parameters
        String requestUrl = String.format("%s%s?from=%s&to=%s&token=%s",
                serverUrl,
                endpointDataGetMessageRecords,
                dateToString(timeSince),
                dateToString(new Date()),
                getCloudloopToken());
        HttpPost post = new HttpPost(requestUrl);

        HttpClientContext context = HttpClientContext.create();
        String jsonResult = null;
        try (CloseableHttpResponse response = httpComm.getClient().execute(post, context)) {
            httpComm.autenticateProxyIfNeeded(response, context);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            checkResponseFromServer(result.toString());
            jsonResult = result.toString();
        }
        catch (Exception e) {
            NeptusLog.pub().warn("Error getting message {} to Cloudloop {}",
                    endpointDataGetMessageRecords, e.getMessage());
            throw e;
        }

        if (jsonResult == null)
            return new ArrayList<>();

        JsonValue arrVal = Json.parse(jsonResult);
        if (!arrVal.isObject()) {
            NeptusLog.pub().warn("Error expecting a list of messages records from Cloudloop");
            return new ArrayList<>();
        }

        JsonObject jsonObject = arrVal.asObject();
        // Maybe the result
        JsonValue msgRecordsNode = jsonObject.get("messageRecords");
        if (msgRecordsNode == null || !msgRecordsNode.isArray()) {
            throw new Exception("Cloudloop server failed to deliver a proper message list '" + jsonResult + "'");
        }

        // more recent msgs on top
        JsonArray msgRecordsArray = msgRecordsNode.asArray();
        if (msgRecordsArray.isEmpty()) {
            return new ArrayList<>();
        }

        List<IridiumMessage> ret = new ArrayList<>();

        for (int i = msgRecordsArray.size() - 1; i >= 0; i--) {
            try {
                JsonObject msgRecord = msgRecordsArray.get(i).asObject();
                String msgId = msgRecord.get("id").asString();
                Date msgAtDate = stringToDate(msgRecord.get("at").asString());
                String msgThingId = msgRecord.get("thing").asString();
                String msgDirection = msgRecord.get("direction").asString();
                int msgSize = msgRecord.get("size").asInt();
                String msgSnippetHex = msgRecord.get("snippet").asString();
                byte[] msgSnippetBytes = ByteUtil.decodeHexString(msgSnippetHex);
                NeptusLog.pub().warn("Received message record with id {}, thing {}, direction {}, size {}, at {}",
                        msgId, msgThingId, msgDirection, msgSize, dateToString(msgAtDate));

                if (msgSnippetBytes.length != msgSize) {
                    // We need to get more detail
                    String detailResult = getMessageRecordFromServerAsJson(msgId, "MO".equalsIgnoreCase(msgDirection));
                    JsonValue arrDetailVal = Json.parse(detailResult);
                    if (!arrDetailVal.isObject()) {
                        NeptusLog.pub().warn("Error expecting a list detail of message {}} from Cloudloop", msgId);
                        continue;
                    }
                    JsonObject msgDetails = arrDetailVal.asObject();
                    String messageBase64 = msgDetails.get("message").asString();
                    msgSnippetBytes = Base64.decodeBase64(messageBase64);
                    if (msgSnippetBytes.length != msgSize) {
                        NeptusLog.pub().warn("Error expecting a message of size {} but got {} for message record with id {}, thing {}, direction {}, at {}",
                                msgSize, msgSnippetBytes.length, msgId, msgThingId, msgDirection, dateToString(msgAtDate));
                        continue;
                    }
                }

                Date now = new Date();
                IridiumMessage irMsg = IridiumMessage.deserialize(msgSnippetBytes);
                if (!"MO".equalsIgnoreCase(msgDirection)) {
                    // So it is possibly an MT sent by a C2 or other to the vehicle
                    irMsg.source = ImcId16.ANNOUNCE.intValue();
                } else if (irMsg.source == ImcId16.NULL_ID.intValue()) {
                    // Let us try to fill the source from imei
                    irMsg.source = findSystemIdByThingId(msgThingId);
                }

                // If not set, set the timestamp
                if (!new Date(irMsg.timestampMillis).before(now) && msgAtDate != null) {
                    irMsg.timestampMillis = msgAtDate.getTime();
                }

                ret.add(irMsg);
                updateVehicleWithLastSeenImei(msgThingId, msgAtDate);
                // it may not be needed the catch for text only message
            }
            catch (Exception e) {
                NeptusLog.pub().warn("Error parsing message record from Cloudloop {} \n >> {} >>",
                        e.getMessage(), msgRecordsArray.get(i).isNull() ? "null" : msgRecordsArray.get(i).toString());
            }
        }

        return ret;
    }

    private String getMessageRecordFromServerAsJson(String msgId, boolean moOrMt) throws Exception {
        NeptusLog.pub().info("Polling message {} detail", msgId);

        // Construct the URL with query parameters
        String requestUrl = String.format("%s%s?messageRecord=%s&token=%s",
                serverUrl,
                moOrMt ? endpointDataGetLingoMo : endpointDataGetLingoMt,
                msgId,
                getCloudloopToken());
        HttpPost post = new HttpPost(requestUrl);

        HttpClientContext context = HttpClientContext.create();
        String jsonResult = null;
        try (CloseableHttpResponse response = httpComm.getClient().execute(post, context)) {
            httpComm.autenticateProxyIfNeeded(response, context);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            checkResponseFromServer(result.toString());
            jsonResult = result.toString();
        }
        catch (Exception e) {
            NeptusLog.pub().warn("Error getting message {} to Cloudloop {}", endpointDataGetLingoMo, e.getMessage());
            throw e;
        }

        return jsonResult;
    }

    public static int findSystemIdByThingId(String thingId) {
        VehicleType vt = VehiclesHolder.getVehicleWithCloudloopThingId(thingId);
        if (vt == null) {
            return ImcId16.NULL_ID.intValue();
        }

        ImcSystem imcSys = ImcSystemsHolder.getSystemWithName(vt.getId());
        if (imcSys == null) {
            return vt.getImcId().intValue();
        }
        return imcSys.getId().intValue();
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
        return "Cloudloop Messenger";
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


    private static Future<Boolean> lastSuccessFuture = null;
    private static synchronized Future<Boolean> currentCloudloopIsReachable() {
        if (lastSuccessFuture != null && !lastSuccessFuture.isDone()) {
            return lastSuccessFuture;
        }

        lastSuccessFuture = new Future<Boolean>() {
            Boolean result = null;
            boolean canceled = false;
            long start = System.currentTimeMillis();
            {
                if (System.currentTimeMillis() - lastSuccess < 15000) {
                    result = true;
                }

                if (result == null) {
                    try {
                        // Construct the URL with query parameters
                        String requestUrl = String.format("%s%s",
                                serverUrl,
                                endpointPlatformPing);
                        URL url = new URI(requestUrl).toURL();
                        NeptusLog.pub().info("Checking Cloudloop server at {}", url);
                        int len = url.openConnection().getContentLength();
                        if (len > 0) {
                            lastSuccess = System.currentTimeMillis();
                        }
                        result = len > 0;
                        NeptusLog.pub().info("Cloudloop server is {}reachable", result ? "" : "NOT ");
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        result = false;
                    }
                }
            }

            @Override
            public Boolean get() throws InterruptedException, ExecutionException {
                while (result == null) {
                    //Thread.sleep(100);
                    Thread.yield();
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
                    //Thread.sleep(100);
                    Thread.yield();
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

        return lastSuccessFuture;
    }

    public static Future<Boolean> rockBlockIsReachable() {
        return currentCloudloopIsReachable();
    }
    
    @Override
    public String toString() {
        return getName();                
    }

    public static void main(String[] args) throws Exception {
        CloudloopIridiumMessenger messenger = new CloudloopIridiumMessenger();
        ConfigFetch.initialize();
        for (IridiumMessage msg : messenger.pollMessages(new Date(System.currentTimeMillis() - 3600000))) {
            System.out.println(msg);
        }
    }
}
