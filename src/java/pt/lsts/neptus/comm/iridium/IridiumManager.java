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
 * Mar 11, 2014
 */
package pt.lsts.neptus.comm.iridium;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.AssetReport;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.LogBookEntry;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * This class will handle Iridium communications
 * 
 * @author zp
 */
public class IridiumManager {

    private static IridiumManager instance = null;
    private DuneIridiumMessenger duneMessenger;
    private RockBlockIridiumMessenger rockBlockMessenger;
    private HubIridiumMessenger hubMessenger;
    private SimulatedMessenger simMessenger;
    private ScheduledExecutorService service = null;
    //private IridiumMessenger currentMessenger;

    private Date lastCall;
    private boolean running = false;

    public static final int IRIDIUM_MTU = 270;
    public static final int IRIDIUM_HEADER = 6;

    private long timeSinceLastUpdateVoiceWarning = -1;
    
    public enum IridiumMessengerEnum {
        DuneIridiumMessenger,
        RockBlockIridiumMessenger,
        HubIridiumMessenger,
        SimulatedMessenger,
    }
    
    private IridiumManager() {
        duneMessenger = new DuneIridiumMessenger();
        rockBlockMessenger = new RockBlockIridiumMessenger();
        hubMessenger = new HubIridiumMessenger();
        simMessenger = new SimulatedMessenger();
    }
    
    public IridiumMessenger getCurrentMessenger() {
        switch (GeneralPreferences.iridiumMessenger) {
            case DuneIridiumMessenger:
                return duneMessenger;
            case HubIridiumMessenger:
                return hubMessenger;
            case RockBlockIridiumMessenger:
                return rockBlockMessenger;
            default:
                return simMessenger;
        }
    }

    private final Runnable pollMessages = new Runnable() {
        //Date lastTime = new Date(System.currentTimeMillis() - Duration.ofHours(1).toMillis());
        //Date lastTime = new GregorianCalendar(2024, Calendar.NOVEMBER, 6).getTime(); // new Date(System.currentTimeMillis() - Duration.ofHours(1).toMillis());
        Date lastTime = new GregorianCalendar(2025, Calendar.JANUARY, 14).getTime(); // new Date(System.currentTimeMillis() - Duration.ofHours(1).toMillis());

        @Override
        public void run() {
            try {
                if (running) {
                    return;
                }
                running = true;
                double pollIntervalMin = Math.max(0.17, Math.min(30, GeneralPreferences.iridiumMessengerPollMinutes));
                Duration pollInterval = pollIntervalMin >= 1 ? Duration.ofMinutes((long) pollIntervalMin)
                        : Duration.ofSeconds((long) (60 * pollIntervalMin));
                if (lastCall != null && System.currentTimeMillis() - lastCall.getTime() < pollInterval.toMillis()) {
                    return;
                }

                Date now = new Date();
                lastCall = now;
                NeptusLog.pub().info("Start polling messages from Iridium network.");
                Collection<IridiumMessage> msgs = getCurrentMessenger().pollMessages(lastTime);
                NeptusLog.pub().info("Polled {} messages from Iridium network.", msgs.size());
                if (!msgs.isEmpty()) {
                    speakUpdateEntityState();
                }
                for (IridiumMessage m : msgs) {
                    try {
                        processMessage(m);
                    } catch (Exception e) {
                        NeptusLog.pub().warn(e);
                    }
                }
                NeptusLog.pub().info("Processed polled {} messages from Iridium network. Took {}ms",
                        msgs.size(), System.currentTimeMillis() - now.getTime());
                
                lastTime = now;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            finally {
                running = false;
            }
        }
    };

    private synchronized void speakUpdateEntityState() {
        if (System.currentTimeMillis() - timeSinceLastUpdateVoiceWarning > Duration.ofSeconds(10).toMillis()) {
            timeSinceLastUpdateVoiceWarning = System.currentTimeMillis();
            String msg = I18n.text("Ireedeehum received"); // To be able to speak Iridium
            SpeechUtil.readSimpleText(msg);
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

    public boolean isAvailable() {
        return getCurrentMessenger().isAvailable();
    }
    
    public synchronized boolean isActive() {
        return service != null;
    }
    
    public void processMessage(IridiumMessage msg) {
        try {
            IridiumMsgTx transmission = new IridiumMsgTx();

            if (msg.getMessageType() < 0) {
                // This allows to send the original message bytes
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    IMCOutputStream ios = new IMCOutputStream(baos);
                    ios.setBigEndian(false);
                    int size = msg.serializeFields(ios);
                    transmission.setData(Arrays.copyOf(baos.toByteArray(), size));
                } catch (Exception e) {
                    NeptusLog.pub().warn(e.getMessage());
                    transmission.setData(msg.serialize());
                }
            } else {
                transmission.setData(msg.serialize());
            }

            transmission.setSrc(msg.getSource());
            transmission.setDst(msg.getDestination());
            transmission.setTimestamp(msg.timestampMillis/1000.0);
            ImcMsgManager.getManager().postInternalMessage("IridiumManager", transmission);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        
        Collection<IMCMessage> msgs = msg.asImc();
        
        for (IMCMessage m : msgs) {
            NeptusLog.pub().info("Posting resulting "+m.getAbbrev()+" message to bus.");
            ImcMsgManager.getManager().postInternalMessage("iridium", m);
        }

        if (msg instanceof PlainTextReportMessage) {
            processAndCreateAssetReportFrom((PlainTextReportMessage) msg);
            processAndCreateFuelAndBattVoltageFrom((PlainTextReportMessage) msg);
            processAndCreateOpModeFrom((PlainTextReportMessage) msg);
        }
    }

    private static GregorianCalendar parseReportTime(PlainTextReportMessage reportMsg) {
        // Get day month and year from date
        Date recDate = new Date(reportMsg.timestampMillis);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(recDate);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1; // Months are 0-based in Calendar
        int year = calendar.get(Calendar.YEAR);
        GregorianCalendar reportTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        String[] timeParts = reportMsg.timeOfDay.split(":");
        reportTime.set(Calendar.YEAR, year);
        reportTime.set(Calendar.MONTH, month);
        reportTime.set(Calendar.DAY_OF_MONTH, day);
        reportTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
        reportTime.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
        reportTime.set(Calendar.SECOND, Integer.parseInt(timeParts[2]));
        return reportTime;
    }

    private static void processAndCreateAssetReportFrom(PlainTextReportMessage reportMsg) {
        NeptusLog.pub().info("Posting resulting plain text report message to bus.");
        AssetReport report = new AssetReport();
        report.setSrc(reportMsg.getSource());
        report.setDst(reportMsg.getDestination());
        report.setTimestamp(reportMsg.timestampMillis / 1000.0);
        report.setMedium(AssetReport.MEDIUM.SATELLITE);

        report.setReportTime(report.getTimestamp());

        try {
            GregorianCalendar reportTime = parseReportTime(reportMsg);
            report.setReportTime(reportTime.getTimeInMillis() / 1000.0);
        } catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }

        report.setName(reportMsg.vehicle);
        report.setLat(Math.toRadians(reportMsg.latDeg));
        report.setLon(Math.toRadians(reportMsg.lonDeg));
        report.setDepth(-1);
        report.setAlt(-1);

        ImcMsgManager.getManager().postInternalMessage("iridium", report);
    }

    private void processAndCreateFuelAndBattVoltageFrom(PlainTextReportMessage reportMsg) {
        FuelLevel fuel = new FuelLevel();
        fuel.setSrc(reportMsg.getSource());
        fuel.setDst(reportMsg.getDestination());
        fuel.setTimestamp(reportMsg.timestampMillis / 1000.0);

        Voltage batteryVoltage = new Voltage();
        batteryVoltage.setSrc(reportMsg.getSource());
        batteryVoltage.setDst(reportMsg.getDestination());
        batteryVoltage.setTimestamp(reportMsg.timestampMillis / 1000.0);

        LogBookEntry logBookEntry = new LogBookEntry();
        logBookEntry.setSrc(reportMsg.getSource());
        logBookEntry.setDst(reportMsg.getDestination());
        logBookEntry.setTimestamp(reportMsg.timestampMillis / 1000.0);

        try {
            GregorianCalendar reportTime = parseReportTime(reportMsg);
            fuel.setTimestamp(reportTime.getTimeInMillis() / 1000.0);
            batteryVoltage.setTimestamp(reportTime.getTimeInMillis() / 1000.0);
            logBookEntry.setTimestamp(reportTime.getTimeInMillis() / 1000.0);
        } catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }

        fuel.setValue(reportMsg.fuelPercentage);
        fuel.setConfidence(reportMsg.batteryConfidencePercentage);

        int entBatt = EntitiesResolver.resolveId(reportMsg.vehicle, "Batteries");
        if (entBatt > 0)
            batteryVoltage.setSrcEnt(entBatt);
        batteryVoltage.setValue(reportMsg.batteryVoltage);

        logBookEntry.setHtime(logBookEntry.getTimestamp());
        logBookEntry.setContext("Text report from Iridium");
        logBookEntry.setText((reportMsg.batteryVoltage > 0 ? "Batteries voltage is "
                + MathMiscUtils.round(reportMsg.batteryVoltage, 1) + " V " : "")
                + (reportMsg.fuelPercentage > 0 ? "Fuel level is " + Math.round(reportMsg.fuelPercentage)
                + "% (confidence " + Math.round(reportMsg.batteryConfidencePercentage) + "%)" : ""));

        boolean sendLogBookEntry = false;
        if (reportMsg.fuelPercentage > 0) {
            NeptusLog.pub().info("Posting resulting fuel report message to bus.");
            ImcMsgManager.getManager().postInternalMessage("iridium", fuel);
            sendLogBookEntry = true;
        }
        if (reportMsg.batteryVoltage > 0) {
            NeptusLog.pub().info("Posting resulting battery voltage report message to bus.");
            ImcMsgManager.getManager().postInternalMessage("iridium", batteryVoltage);
            sendLogBookEntry = true;
        }
        if (sendLogBookEntry) {
            NeptusLog.pub().info("Posting resulting log book entry message to bus.");
            ImcMsgManager.getManager().postInternalMessage("iridium", logBookEntry);
        }
    }

    private void processAndCreateOpModeFrom(PlainTextReportMessage reportMsg) {
        if (reportMsg.statusIndicator.isEmpty())
            return;

        NeptusLog.pub().info("Posting resulting op. mode report message to bus.");
        LogBookEntry logBookEntry = new LogBookEntry();
        logBookEntry.setSrc(reportMsg.getSource());
        logBookEntry.setDst(reportMsg.getDestination());
        logBookEntry.setTimestamp(reportMsg.timestampMillis / 1000.0);

        try {
            GregorianCalendar reportTime = parseReportTime(reportMsg);
            logBookEntry.setTimestamp(reportTime.getTimeInMillis() / 1000.0);
        } catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }

        logBookEntry.setHtime(logBookEntry.getTimestamp());
        logBookEntry.setContext("Text report from Iridium");
        logBookEntry.setType(LogBookEntry.TYPE.INFO);

        switch (reportMsg.statusIndicator) {
            case "S":
                logBookEntry.setType(LogBookEntry.TYPE.INFO);
                logBookEntry.setText("Vehicle is in service mode");
                break;
            case "B":
                logBookEntry.setType(LogBookEntry.TYPE.WARNING);
                logBookEntry.setText("Vehicle is in boot mode");
                break;
            case "C":
                logBookEntry.setType(LogBookEntry.TYPE.INFO);
                logBookEntry.setText("Vehicle is in calibration mode");
                break;
            case "E":
                logBookEntry.setType(LogBookEntry.TYPE.ERROR);
                logBookEntry.setText("Vehicle is in error mode");
                break;
            case "X":
                logBookEntry.setType(LogBookEntry.TYPE.WARNING);
                logBookEntry.setText("Vehicle is in external mode");
                break;
            case "M":
                logBookEntry.setType(LogBookEntry.TYPE.INFO);
                logBookEntry.setText("Vehicle is in maneuver mode");
                break;
            default:
                logBookEntry.setType(LogBookEntry.TYPE.INFO);
                logBookEntry.setText("Vehicle is in " + reportMsg.statusIndicator + " mode");
                break;
        }

        ImcMsgManager.getManager().postInternalMessage("iridium", logBookEntry);
    }

    public void selectMessenger(Component parent) {
        Object op = JOptionPane.showInputDialog(parent, "Select Iridium provider", "Iridium Provider",
                JOptionPane.QUESTION_MESSAGE, ImageUtils.createImageIcon("images/satellite.png"), IridiumMessengerEnum.values(),
                GeneralPreferences.iridiumMessenger);

        if (op != null) {
            GeneralPreferences.iridiumMessenger = (IridiumMessengerEnum) op;
            GeneralPreferences.saveProperties();
        }
    }
    
    public synchronized void start() {
        if (service != null)
            stop();
        
        ImcMsgManager.getManager().registerBusListener(this);
        service = Executors.newScheduledThreadPool(1);
        lastCall = null;
        running = false;
        service.scheduleAtFixedRate(pollMessages, 1, 2, TimeUnit.SECONDS);
    }
    
    public synchronized void stop() {
        if (service != null) {
            service.shutdownNow();           
            service = null;
        }
        ImcMsgManager.getManager().unregisterBusListener(this);        
    }

    public static IridiumManager getManager() {
        if (instance == null)
            instance = new IridiumManager();
        return instance;
    }

    public static Collection<ImcIridiumMessage> iridiumEncode(IMCMessage msg) throws Exception {
        if (msg.getPayloadSize() < ImcIridiumMessage.MaxPayloadSize) {
            ImcIridiumMessage m = new ImcIridiumMessage();
            m.setSource(msg.getSrc());
            m.setDestination(msg.getDst());
            m.timestampMillis = msg.getTimestampMillis();
            m.msg = msg;
            return Arrays.asList(m);
        }
        else {
            MessagePart[] parts = new IMCFragmentHandler(IMCDefinition.getInstance()).fragment(msg,
                    ImcIridiumMessage.MaxPayloadSize+IMCDefinition.getInstance().headerLength());
            
            ArrayList<ImcIridiumMessage> ret = new ArrayList<ImcIridiumMessage>();
            for (MessagePart mp : parts) {
                ImcIridiumMessage m = new ImcIridiumMessage();
                m.setSource(msg.getSrc());
                m.setDestination(msg.getDst());
                m.timestampMillis = msg.getTimestampMillis();
                m.msg = mp;
                ret.add(m);
            }
            return ret;
        }
    }

    public static void testMessageSerialization() {
        IMCDefinition defs = IMCDefinition.getInstance();

        for (String abbrev: defs.getMessageNames()) {
            IMCMessage m = defs.create(abbrev);
            IMCUtil.fillWithRandomData(m);
            System.out.println("Message of type "+m.getAbbrev()+" and size "+(m.getPayloadSize()));
            System.out.println(m);
            try {
                Collection<ImcIridiumMessage> msgs = iridiumEncode(m);
                System.out.println(" ==> "+msgs.size()+" messages");
                for (ImcIridiumMessage msg : msgs) {
                    ByteUtil.dumpAsHex("Iridium message of type "+msg.getMessageType(), msg.serialize(), System.out);
                    for (byte b : msg.serialize()) {
                        System.out.printf("%02X",b);
                    }
                    System.out.println();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * This method will send the given message using the currently selected messenger
     * 
     * @param msg
     */
    public void send(IridiumMessage msg) throws Exception {
        NeptusLog.pub().info("Sending iridium message via "+getCurrentMessenger().getName()+": "+ByteUtil.encodeToHex(msg.serialize()));
        getCurrentMessenger().sendMessage(msg);
    }

    /**
     * This method will send the given raw message using the currently selected messenger
     *
     * @param destinationName The name of the destination
     *                        (e.g. the name of the vehicle that should receive the message)
     * @param destinationAddr The address of the destination, this depends on the messenger
     *                        (e.g. the IMC address of the vehicle that should receive the message,
     *                        or the imei of the Iridium device that should receive the message)
     *                        This can be empty or null, the messenger will try its best to find the
     *                        missing information.
     * @param data The data to be sent
     */
    public void sendRaw(String destinationName, String destinationAddr, byte[] data) throws Exception {
        NeptusLog.pub().info("Sending iridium raw message via "+getCurrentMessenger().getName()+": "+ByteUtil.encodeToHex(data));
        getCurrentMessenger().sendMessageRaw(destinationName, destinationAddr, data);
    }
    
    public static void main(String[] args) throws Exception {
       String hexText = "ffff0000db07002247545653000000000000000041002147545653000000000000000042000147545653000000000000000031000047545653000000000000000032000047545653000000000000000030";
       byte[] data = Hex.decodeHex(hexText.toCharArray());
       
       ExtendedDeviceUpdate devupd = (ExtendedDeviceUpdate) IridiumMessage.deserialize(data);
       for (Position p : devupd.positions.values()) {
           System.out.println(p.posType);
       }
    }
}
