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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * 7 de Jul de 2012
 */
package pt.lsts.neptus.plugins.odss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingWorker;

import org.mbari.tracking.Tracking;
import org.mbari.tracking.Tracking.PlatformReport;
import org.mbari.trex.Sensor.SensorMessage;

import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.odss.track.PlatformReportType;
import pt.lsts.neptus.plugins.odss.track.PlatformReportType.PlatformType;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.AISMmsiUtil;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "ODSS RabbitMQ Track Fetcher", author = "Paulo Dias", version = "0.1",
 icon = "pt/lsts/neptus/plugins/odss/odss.png", category = CATEGORY.WEB_PUBLISHING)
public class OdssRabbitMQTrackFetcher extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {
    private static final long serialVersionUID = 1L;

    private static final String EXCHANGE_AUV = "auvs_pb";
    private static final String EXCHANGE_GLIDER = "gliders_pb";
    private static final String EXCHANGE_DRIFTERS = "drifters_pb";
    private static final String EXCHANGE_SHIP = "ships_pb";
    private static final String EXCHANGE_AIS = "ais_pb";
    
//    private static final String EXCHANGE_SENSOR_TREX = "SensorMessagesFromTrex";
//    private static final String EXCHANGE_SENSOR_WAVEGLIDER = "SensorMessagesFromWaveGlider";
//    private static final String EXCHANGE_SENSOR_FEUP = "SensorMessagesFromFEUP";
    
    @NeptusProperty(name = "Host", category = "Communication")
    public String host = "messaging.shore.mbari.org"; // 134.89.10.43

    @NeptusProperty(name = "Port", category = "Communication")
    public int port = 5672;

    @NeptusProperty(name = "AUV", category = "System Filter")
    public boolean fetchAUVType = true;

    @NeptusProperty(name = "Glider", category = "System Filter")
    public boolean fetchGliderType = true;

    @NeptusProperty(name = "Drifter", category = "System Filter")
    public boolean fetchDrifterType = true;

    @NeptusProperty(name = "Ship", category = "System Filter")
    public boolean fetchShipType = true;

    @NeptusProperty(name = "AIS", category = "System Filter")
    public boolean fetchAISType = false;

    @NeptusProperty(name = "WaveGlider Sensor Data", category = "System Filter")
    public boolean fetchSensorWaveGlider = false;

    @NeptusProperty(name = "Publishing")
    public boolean activeOn = false;

    // @NeptusProperty(functionality = "Web Publishing")
    public boolean publishRemoteSystemsLocally = true;

    // @NeptusProperty(functionality = "Web Publishing")
    public boolean debugOn = false;

//    private long lastFetchPosTimeMillis = System.currentTimeMillis();
    
    private final HashMap<String, PlatformReportType> sysRMQLocations = new HashMap<String, PlatformReportType>();

    private ConnectionFactory factory = null;
    private Connection connection = null;

    private Channel channelAUVs = null;
    private Channel channelGliders = null;
    private Channel channelDrifters = null;
    private Channel channelShips = null;
    private Channel channelAISs = null;
    private boolean stopAllThreads = false;
    
    private Thread auvWorkingThread = null;
    private Thread gliderWorkingThread = null;
    private Thread drifterWorkingThread = null;
    private Thread shipWorkingThread = null;
    private Thread aisWorkingThread = null;
    private JCheckBoxMenuItem startStopCheckItem = null;

    public OdssRabbitMQTrackFetcher(ConsoleLayout console) {
        super(console);
        initializeComm();
        initialize();
    }

    /**
     * 
     */
    private void initialize() {
        setVisibility(false);
    }

    /**
     * 
     */
    private void initializeComm() {
        factory = new ConnectionFactory();
        // IP address for messaging.shore.mbari.org is (134.89.10.43)
        factory.setHost(host);
        factory.setPort(port);
        factory.setVirtualHost("trackingvhost");
        factory.setUsername("tracking-subscriber");
        factory.setPassword("tsubro");
    }

    /**
     * 
     */
    private void startComms() {
        NeptusLog.pub().warn(OdssRabbitMQTrackFetcher.class.getSimpleName() + ": Starting comms");
        try {
            connection = factory.newConnection();
            connection.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException e) {
//                    connection = null;
                    stopComms();
                }
            });
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        
        stopAllThreads = false;
        long timeStmp =  System.currentTimeMillis();
        String queueName = "";
        if (fetchAUVType) {
            queueName = "porto_persist_" + EXCHANGE_AUV + "_" + timeStmp;
            channelAUVs = createChannel(queueName, EXCHANGE_AUV, true, false);
            auvWorkingThread = createNewThreadFetcherTrack(PlatformType.AUV, EXCHANGE_AUV, channelAUVs, queueName);
            if (auvWorkingThread != null)
                auvWorkingThread.start();
        }
        
        if (fetchGliderType) {
            queueName = "porto_persist_" + EXCHANGE_GLIDER + "_" + timeStmp;
            channelGliders = createChannel(queueName, EXCHANGE_GLIDER, true, false);
            gliderWorkingThread = createNewThreadFetcherTrack(PlatformType.GLIDER, EXCHANGE_GLIDER, channelGliders, queueName);
            if (gliderWorkingThread != null)
                gliderWorkingThread.start();
        }
        
        if (fetchDrifterType) {
            queueName = "porto_persist_" + EXCHANGE_DRIFTERS + "_" + timeStmp;
            channelDrifters = createChannel(queueName, EXCHANGE_DRIFTERS, true, false);
            drifterWorkingThread = createNewThreadFetcherTrack(PlatformType.DRIFTER, EXCHANGE_DRIFTERS, channelDrifters, queueName);
            if (drifterWorkingThread != null)
                drifterWorkingThread.start();
        }
        
        if (fetchShipType) {
            queueName = "porto_persist_" + EXCHANGE_SHIP + "_" + timeStmp;
            channelShips = createChannel(queueName, EXCHANGE_SHIP, true, false);
            shipWorkingThread = createNewThreadFetcherTrack(PlatformType.SHIP, EXCHANGE_SHIP, channelShips, queueName);
            if (shipWorkingThread != null)
                shipWorkingThread.start();
        }
        
        if (fetchAISType) {
            queueName = "porto_persist_" + EXCHANGE_AIS + "_" + timeStmp;
            channelAISs = createChannel(queueName, EXCHANGE_AIS, true, false);
            aisWorkingThread = createNewThreadFetcherTrack(PlatformType.AIS, EXCHANGE_AIS, channelAISs, queueName);
            if (aisWorkingThread != null)
                aisWorkingThread.start();
        }
        
        if (true) {
            
        }
    }

    /**
     * @return
     */
    private Channel createChannel(String queueName, String exchange, boolean declareQueue, boolean declareExchange) {
        Channel channel;
        try {
            channel = connection.createChannel();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (declareExchange) {
            try {
                channel.exchangeDeclare(exchange, "fanout");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (declareQueue) {
            try {
                // queueDeclare : Name, Durable (the queue will survive a broker restart), Exclusive (used by only one
                // connection and the queue will be deleted when that connection closes), Auto-delete (queue is deleted
                // when last consumer unsubscribes), Arguments
                channel.queueDeclare(queueName, true, false, true, null); 
                channel.queueBind(queueName, exchange, "");
            }
            catch (IOException e) {
                e.printStackTrace();
//            channel.abort();
                return null;
            }
        }
        return channel;
    }
    
    private void stopComms() {
        if (connection != null)
            NeptusLog.pub().warn(OdssRabbitMQTrackFetcher.class.getSimpleName() + ": Stopping comms");

        if (connection != null && connection.isOpen()) {
            connection.abort();
//            try {
//                if (connection.isOpen())
//                    connection.close();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
        }
        connection = null;
        if (channelAUVs != null && channelAUVs.isOpen()) {
            try {
                channelAUVs.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        channelAUVs = null;
        if (channelGliders != null && channelGliders.isOpen()) {
            try {
                channelGliders.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        channelGliders = null;
        if (channelDrifters != null && channelDrifters.isOpen()) {
            try {
                channelDrifters.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        channelDrifters = null;
        if (channelShips != null && channelShips.isOpen()) {
            try {
                channelShips.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        channelShips = null;
        if (channelAISs != null && channelAISs.isOpen()) {
            try {
                channelAISs.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        channelAISs = null;
        
        stopAllThreads = true;
        if (auvWorkingThread != null) {
            Thread tmpThr = auvWorkingThread;
            auvWorkingThread = null;
            tmpThr.interrupt();
        }
        if (gliderWorkingThread != null) {
            Thread tmpThr = gliderWorkingThread;
            gliderWorkingThread = null;
            tmpThr.interrupt();
        }
        if (drifterWorkingThread != null) {
            Thread tmpThr = drifterWorkingThread;
            drifterWorkingThread = null;
            tmpThr.interrupt();
        }
        if (shipWorkingThread != null) {
            Thread tmpThr = shipWorkingThread;
            shipWorkingThread = null;
            tmpThr.interrupt();
        }
        if (aisWorkingThread != null) {
            Thread tmpThr = aisWorkingThread;
            aisWorkingThread = null;
            tmpThr.interrupt();
        }
    }
    
    protected void publishToChannel(Channel channel, String exchange, com.google.protobuf.GeneratedMessage protobubMsg) {
        byte[] data = protobubMsg.toByteArray();
        publishToChannel(channel, exchange, data);
    }

    private void publishToChannel(Channel channel, String exchange, byte[] data) {
        try {
            channel.basicPublish(exchange, "", null, data);
            if (debugOn) {
                NeptusLog.pub().info("<###> [x] Sent to exchange '" + exchange + "':\n" + ByteUtil.dumpAsHexToString(data));
            }
        }
        catch (IOException e) {
            NeptusLog.pub().error(OdssRabbitMQTrackFetcher.class.getSimpleName() + ": " +
            		e.getClass().getSimpleName() + ": " + e.getMessage() + "  from Exchange '" + exchange + "'", e);
        }
    }
    
    @Override
    public void initSubPanel() {
        setVisibility(false);
        
        startStopCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
                + ">Start/Stop", null,
                new CheckMenuChangeListener() {
                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        startStopCheckItem.setEnabled(false);
                        activeOn = false;
                        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                try {
                                    stopComms();
                                }
                                catch (Exception e2) {
                                    e2.printStackTrace();
                                }
                                return null;
                            }
                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                startStopCheckItem.setEnabled(true);
                            }
                        };
                        sw.execute();
                    }
                    
                    @Override
                    public void menuChecked(ActionEvent e) {
                        startStopCheckItem.setEnabled(false);
                        activeOn = true;
                        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                try {
                                    startComms();
                                }
                                catch (Exception e2) {
                                    e2.printStackTrace();
                                }
                                return null;
                            }
                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                startStopCheckItem.setEnabled(true);
                            }
                        };
                        sw.execute();
                    }
                });

        addMenuItem(
                I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">" + I18n.text("Settings"),
                null,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PropertiesEditor.editProperties(OdssRabbitMQTrackFetcher.this, getConsole(), true);
                        // PropertiesEditor.createAggregatedPropertiesDialog(getConsole(), true);
                    }
                });
        
        startStopCheckItem.setState(activeOn);
        
        if (activeOn)
            startComms();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">Settings");
        
        stopComms();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        try {
            if (factory != null) {
                stopComms();
                initializeComm();
                if (activeOn)
                    startComms();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     */
    private Thread createNewThreadFetcherTrack(final PlatformReportType.PlatformType platformType, final String exchangeName,
            final Channel channel, final String queueName) {
        Thread thr = new Thread(OdssRabbitMQTrackFetcher.class.getSimpleName() + "["
                + OdssRabbitMQTrackFetcher.this.getClass().hashCode() + "]: " + exchangeName) {
            // Tracking.PlatformReport.Builder platformReportBuilder = Tracking.PlatformReport.newBuilder();
            // platformReportBuilder.mergeFrom(input).build();
            QueueingConsumer consumer;
            @Override
            public synchronized void start() {
                consumer = new QueueingConsumer(channel);
                try {
                    channel.basicConsume(queueName, true, consumer);
                    super.start();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void run() {
                while (!stopAllThreads) {
                    QueueingConsumer.Delivery delivery;
                    try {
                        delivery = consumer.nextDelivery(200);
                        if (delivery == null)
                            continue;
                        String message = new String(delivery.getBody());
                        try {
                            PlatformReport pr = Tracking.PlatformReport.parseFrom(delivery.getBody());
                            
                            HashMap<String, PlatformReportType> sysBag = new HashMap<String, PlatformReportType>();
                            if (debugOn)
                                NeptusLog.pub().info("<###>Arrived message: " + pr.toString() + pr.hasType()
                                        + "  from Exchange '" + exchangeName + "'");
                            if (!pr.hasName() && !pr.hasEpochSeconds() /*&& !pr.hasType()*/ && !pr.hasLatitude()
                                    && !pr.hasLongitude())
                                continue;
                            if (debugOn)
                                NeptusLog.pub().info("<###>Message accepted from: " + ">>>" + pr.getName() + " and with"
                                        + (pr.hasType() ? "" : "out") + " type" + "  from Exchange '" + exchangeName
                                        + "'");
                            PlatformType type = platformType;
                            if (pr.hasType()) {
                                switch (pr.getType()) {
                                    case AIS:
                                        type = PlatformType.AIS;
                                        break;
                                    case AUV:
                                        type = PlatformType.AUV;
                                        break;
                                    case DRIFTER:
                                        type = PlatformType.DRIFTER;
                                        break;
                                    case GLIDER:
                                        type = PlatformType.GLIDER;
                                        break;
                                    case MBARI_SHIP:
                                        type = PlatformType.SHIP;
                                        break;
                                    case MOORING:
                                        type = PlatformType.MOORING;
                                        break;
                                }
                            }
                            
                            String name = pr.getName();
                            if (pr.hasMmsi() && pr.getName().equalsIgnoreCase("" + pr.getMmsi())) {
                                String nameFromMmsi = AISMmsiUtil.queryNameFromMmsi(pr.getMmsi());
                                if (nameFromMmsi != null && !nameFromMmsi.isEmpty())
                                    name = nameFromMmsi;
                            }

                            PlatformReportType prType = new PlatformReportType(name, type);
                            prType.setLocation(pr.getLatitude(), pr.getLongitude(), pr.getEpochSeconds());
                            if (pr.hasMmsi())
                                prType.setMmsi(pr.getMmsi());
                            else
                                prType.setMmsi(-1);
                            if (pr.hasImei())
                                prType.setImei(pr.getImei());
                            else
                                prType.setImei(-1);
                            if (pr.hasSource())
                                prType.setSource(pr.getSource());
                            else
                                prType.setSource("");
                            
                            sysBag.put(prType.getName(), prType);
                            OdssStoqsTrackFetcher.filterAndAddToList(sysBag);
                            sysRMQLocations.putAll(sysBag);
                        }
                        catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        if (debugOn)
                            NeptusLog.pub().info("<###> [x] Received '" + message + "'  from Exchange '" + exchangeName + "'");
                    }
                    catch (ShutdownSignalException e) {
                        NeptusLog.pub().warn(OdssRabbitMQTrackFetcher.class.getSimpleName() + ": " + e + "  from Exchange '" + exchangeName + "'");
                        break;
                    }
                    catch (ConsumerCancelledException e) {
                        e.printStackTrace();
                        break;
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().warn("Thread " + this.getName() + " stopped");
            }
        };
        thr.setPriority(Thread.MIN_PRIORITY);
        return thr;
    }

    protected Thread createNewThreadFetcherSensor(final PlatformReportType.PlatformType platformType, final String exchangeName,
            final Channel channel, final String queueName) {
        Thread thr = new Thread(OdssRabbitMQTrackFetcher.class.getSimpleName() + "["
                + OdssRabbitMQTrackFetcher.this.getClass().hashCode() + "]: " + exchangeName) {
            QueueingConsumer consumer;
            @Override
            public synchronized void start() {
                consumer = new QueueingConsumer(channel);
                try {
                    channel.basicConsume(queueName, true, consumer);
                    super.start();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void run() {
                while (!stopAllThreads) {
                    QueueingConsumer.Delivery delivery;
                    try {
                        delivery = consumer.nextDelivery(200);
                        if (delivery == null)
                            continue;
                        String message = new String(delivery.getBody());
                        try {
                            SensorMessage pr = SensorMessage.parseFrom(delivery.getBody());
                            
                            new HashMap<String, PlatformReportType>();
                            if (debugOn)
                                NeptusLog.pub().info("<###>Arrived message: " + pr.toString()
                                        + "  from Exchange '" + exchangeName + "'");
//                            if (!pr.hasName() && !pr.hasEpochSeconds() /*&& !pr.hasType()*/ && !pr.hasLatitude()
//                                    && !pr.hasLongitude())
//                                continue;
//                            if (debugOn)
//                                NeptusLog.pub().info("<###>Message accepted from: " + ">>>" + pr.getName() + " and with"
//                                        + (pr.hasType() ? "" : "out") + " type" + "  from Exchange '" + exchangeName
//                                        + "'");

//                            sysBag.put(pr.getName(), prType);
//                            OdssStoqsTrackFetcher.filterAndAddToList(sysBag);
//                            sysRMQLocations.putAll(sysBag);
                        }
                        catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        if (debugOn)
                            NeptusLog.pub().info("<###> [x] Received '" + message + "'  from Exchange '" + exchangeName + "'");
                    }
                    catch (ShutdownSignalException e) {
                        NeptusLog.pub().warn(OdssRabbitMQTrackFetcher.class.getSimpleName() + ": " + e + " from Exchange '" + exchangeName + "'");
                        break;
                    }
                    catch (ConsumerCancelledException e) {
                        e.printStackTrace();
                        break;
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().warn("Thread " + this.getName() + " stopped");
            }
        };
        thr.setPriority(Thread.MIN_PRIORITY);
        return thr;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        if (startStopCheckItem != null)
            startStopCheckItem.setState(activeOn);

        if (!activeOn)
            stopComms();

        if (activeOn)
            processRemoteStates();
        
        return true;
    }

    private void processRemoteStates() {
        if (publishRemoteSystemsLocally) {
            OdssStoqsTrackFetcher.processRemoteStates(sysRMQLocations, DateTimeUtil.HOUR * 3);
        }
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
        OdssRabbitMQTrackFetcher rmq = new OdssRabbitMQTrackFetcher(null);
        rmq.debugOn = true;
        rmq.initializeComm();
        rmq.fetchAISType = true;
        rmq.startComms();
        
        try { Thread.sleep(30000); } catch (InterruptedException e) { e.printStackTrace();}
        rmq.stopComms();
        try { Thread.sleep(10000); } catch (InterruptedException e) { e.printStackTrace();}
        rmq.fetchAISType = false;
        rmq.startComms();
    }
}
