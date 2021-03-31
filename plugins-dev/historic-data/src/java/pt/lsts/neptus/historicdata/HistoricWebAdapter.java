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
 * 06/05/2016
 */
package pt.lsts.neptus.historicdata;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import pt.lsts.imc.HistoricData;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.RemoteCommand;
import pt.lsts.imc.historic.DataSample;
import pt.lsts.imc.historic.DataStore;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import javax.net.ssl.SSLContext;

/**
 * @author zp
 *
 */
public class HistoricWebAdapter {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String getURL = GeneralPreferences.ripplesUrl + "/datastore/lsf";
    private String postURL = GeneralPreferences.ripplesUrl + "/datastore";
    private long lastPoll = System.currentTimeMillis() - 1200 * 1000;
    private HistoricDataInteraction interaction;
    private DataStore localStore, uploaded;

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

    public HistoricWebAdapter(HistoricDataInteraction interaction) {
        this.interaction = interaction;
        localStore = new DataStore();
        uploaded = new DataStore();
    }
    
    public Future<Notification> command(String system, IMCMessage msg, double timeout) {
        HistoricData data = new HistoricData();
        RemoteCommand cmd = new RemoteCommand();
        cmd.setDestination(ImcSystemsHolder.getSystemWithName(system).getId().intValue());
        cmd.setOriginalSource(ImcMsgManager.getManager().getLocalId().intValue());
        cmd.setCmd(msg);
        cmd.setTimeout(timeout);
        data.setData(Arrays.asList(cmd));
        return upload(data);
    }

    public Future<Notification> upload(HistoricData data) {
        return executor.submit(new Callable<Notification>() {
            @Override
            public Notification call() throws Exception {
                try (CloseableHttpClient client = HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .setConnectionManager(cm)
                        .build()) {
                    HttpPost post = new HttpPost(postURL);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IMCOutputStream out = new IMCOutputStream(baos);
                    out.writeMessage(data);
                    out.close();

                    InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(baos.toByteArray()),
                            -1, ContentType.APPLICATION_OCTET_STREAM);
                    reqEntity.setChunked(true);
                    post.setEntity(reqEntity);

                    NeptusLog.pub().info("Uploading message to the web");
                    try (CloseableHttpResponse response = client.execute(post)) {
                        NeptusLog.pub().debug(response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                        HttpEntity entity = response.getEntity();
                        EntityUtils.consume(entity);
                        if (response.getStatusLine().getStatusCode()  != 200) {
                            throw new Exception("HTTP Status " + response + ": " + response.getStatusLine().getReasonPhrase());
                        }

                        return Notification.success(I18n.text("Message upload"),
                                I18n.text("Message uploaded successfully"));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NeptusLog.pub().error(e);
                        return Notification.error(I18n.text("Message upload"),
                                I18n.textf("Error uploading to web: %error", e.getMessage()));
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NeptusLog.pub().error(e);
                    return Notification.error(I18n.text("Message upload"),
                            I18n.textf("Error uploading to web: %error", e.getMessage()));
                }
            }
        });
    }

    public Future<Notification> upload() {
        return executor.submit(new Callable<Notification>() {
            @Override
            public Notification call() throws Exception {
                int samplesBefore = localStore.numSamples();
                HistoricData data = localStore.pollData(0, 64000);

                try (CloseableHttpClient client = HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .setConnectionManager(cm)
                        .build()) {

                    HttpPost post = new HttpPost(postURL);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IMCOutputStream out = new IMCOutputStream(baos);
                    out.writeMessage(data);
                    out.close();

                    InputStreamEntity reqEntity = new InputStreamEntity(new ByteArrayInputStream(baos.toByteArray()),
                            -1, ContentType.APPLICATION_OCTET_STREAM);
                    reqEntity.setChunked(true);
                    post.setEntity(reqEntity);

                    NeptusLog.pub().info("Uploading local data to the web");
                    try(CloseableHttpResponse response = client.execute(post)) {
                        NeptusLog.pub().debug(response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                        HttpEntity entity = response.getEntity();
                        EntityUtils.consume(entity);
                        if (response.getStatusLine().getStatusCode()  != 200) {
                            throw new Exception("HTTP Status " + response + ": " + response.getStatusLine().getReasonPhrase());
                        }

                        NeptusLog.pub().info("Uploaded local data to the web. Local dataStore contained "
                                + samplesBefore + " and now contains " + localStore.numSamples() + " samples.");
                        uploaded.addData(data);
                        return Notification.info(I18n.text("Historic Data Upload"),
                                I18n.text("Historic Data uploaded successfully"));
                    }
                    catch (Exception e) {
                        throw e;
                    }
                }
                catch (Exception e) {
                    localStore.addData(data);
                    e.printStackTrace();
                    NeptusLog.pub().error(e);
                    return Notification.warning(I18n.text("Historic Data Upload"),
                            I18n.textf("Error uploading historic data: %error", e.getMessage()));
                }
            } 
        });
    }

    public void addLocalData(HistoricData data) {
        localStore.addData(data);
    }

    public Future<Boolean> download() {
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {

                try (CloseableHttpClient client = HttpClients.custom()
                        .setSSLSocketFactory(sslsf)
                        .setConnectionManager(cm)
                        .build()) {
                    String query = "?since="+lastPoll;
                    HttpGet get = new HttpGet(getURL + query);

                    NeptusLog.pub().info("Polling web server for data since " + new Date(lastPoll));

                    try (CloseableHttpResponse response = client.execute(get)) {
                        HttpEntity entity = response.getEntity();
                        if (response.getStatusLine().getStatusCode()  != 200) {
                            EntityUtils.consume(entity);
                            throw new Exception("HTTP Status " + response + ": " + response.getStatusLine().getReasonPhrase());
                        }

                        IMCInputStream iis = new IMCInputStream(response.getEntity().getContent(),
                                IMCDefinition.getInstance());
                        IMCMessage msg = iis.readMessage();
                        iis.close();

                        EntityUtils.consume(entity);

                        if (msg.getMgid() == HistoricData.ID_STATIC) {
                            HistoricData data = HistoricData.clone(msg);
                            DataStore downloaded = new DataStore();
                            for (DataSample s : DataSample.parseSamples(data)) {
                                if (!uploaded.contains(s))
                                    downloaded.addSample(s);
                            }
                            lastPoll = (long) (data.getBaseTime() * 1000);
                            NeptusLog.pub().info("Downloaded "+downloaded.numSamples()+" new samples from the web. Last sample from "+new Date(lastPoll));
                            if (downloaded.numSamples() > 0)
                                interaction.process(downloaded.pollData(0, 65000));
                            return true;
                        }
                        else {
                            throw new Exception("Unexpected message type: " + msg.getAbbrev());
                        }
                    }
                    catch (Exception e) {
                        throw e;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    NeptusLog.pub().error(e);
                    return false;
                }
            }                        
        });
    }

}
