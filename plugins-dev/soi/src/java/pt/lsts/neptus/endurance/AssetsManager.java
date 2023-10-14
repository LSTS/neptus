/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 09/01/2018
 */
package pt.lsts.neptus.endurance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.lsts.imc.Announce;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SoiCommand;
import pt.lsts.imc.StateReport;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 * @author zp
 */
public class AssetsManager {

    @SuppressWarnings("serial")
    public static final SimpleDateFormat dateFormatterXMLNoMillisUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") {{setTimeZone(TimeZone.getTimeZone("UTC"));}};  // This one should be UTC (Zulu)
    
    private static AssetsManager instance = null;
    
    private ConcurrentHashMap<String, SoiSettings> settings = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, DripSettings> dSettings = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Plan> plans = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Asset> assetsMap = new ConcurrentHashMap<>();

    /**
     * @param console
     */
    private AssetsManager() {
        PeriodicUpdatesService.registerPojo(this);
    }

    public static AssetsManager getInstance() {
        if (instance == null)
            instance = new AssetsManager();
        
        return instance;
    }
    
    public List<Asset> getAssets() {
        ArrayList<Asset> ret = new ArrayList<>();
        ret.addAll(assetsMap.values());
        return ret;
    }
    
    /**
     * @return the plans
     */
    public ConcurrentHashMap<String, Plan> getPlans() {
        return plans;
    }
    
    /**
     * @return the settings
     */
    public ConcurrentHashMap<String, SoiSettings> getSettings() {
        return settings;
    }
    
    public void sendCommand(String systemName, SoiCommand cmd, CommMean commMean, ConsoleLayout console) {
        if (commMean == CommMean.WiFi) {
            ImcMsgManager.getManager().sendMessageToSystem(cmd, systemName, createMessageDeliveryListener(console, systemName));
            NeptusLog.pub().warn("Command sent " + cmd.getCommandStr() + " sent over UDP to " + systemName + " :: " + cmd.asJSON());
        }
        else if (commMean == CommMean.Iridium) {
            try {
                ImcSystem system = ImcSystemsHolder.lookupSystemByName(systemName);
                ImcIridiumMessage msg = new ImcIridiumMessage();
                msg.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                cmd.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
                cmd.setDst(system.getId().intValue());
                msg.setMsg(cmd);
                msg.setDestination(system.getId().intValue());
                IridiumManager.getManager().send(msg);
                if (console != null)
                    console.post(Notification.success(cmd.getCommandStr()+" sent to "+systemName, cmd.getCommandStr()+" sent using "
                            + IridiumManager.getManager().getCurrentMessenger().getName()));
            }
            catch (Exception e) {
                GuiUtils.errorMessage(console != null ? console : null, e);
            }
        }
    }

    /**
     * @param console 
     * @param systemName 
     * @return
     */
    private MessageDeliveryListener createMessageDeliveryListener(ConsoleLayout console, String systemName) {
        MessageDeliveryListener listener = new MessageDeliveryListener() {
            
            private String msgName(IMCMessage msg) {
                String ret = msg.getAbbrev();
                if (msg.getMgid() == SoiCommand.ID_STATIC) {
                    ret = "SOI / "+msg.getString("command");
                }
                return ret;
            }
            
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                if (console != null)
                    console.post(Notification.error(I18n.text("System is Unreacheable"),
                        I18n.textf("%cmd to %system failed.", msgName(message), systemName)));
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                if (console != null)
                    console.post(Notification.warning(I18n.text("Delivery Uncertain"),
                        I18n.textf("Delivery of %cmd is uncertain (%reason)", msgName(message),
                                ((Exception)msg).getMessage())));
                
                NeptusLog.pub().error(msg);
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                if (console != null)
                    console.post(Notification.error(I18n.text("Delivery Timed Out"),
                        I18n.textf("%cmd sent over Wi-Fi to %system.", msgName(message), systemName)));
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
                if (console != null)
                    console.post(Notification.success(I18n.text("Successfull Delivery"),
                        I18n.textf("Delivery of %cmd was successfull.", msgName(message), systemName)));
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                if (console != null)
                    console.post(Notification.error(I18n.text("Delivery Error"),
                        I18n.textf("%cmd to %system failed: %reason.", msgName(message),
                        systemName, ((Exception)error).getMessage())));
                NeptusLog.pub().error(error);
            }
        };
        return listener;
    }

    public void process(StateReport cmd) {
        Asset asset = getAssetFor(cmd.getSourceName());
        
        if (asset == null)
            asset = new Asset(cmd.getSourceName());
        
        AssetState state = AssetState.builder()
                .withLatitude(cmd.getLatitude())
                .withLongitude(cmd.getLongitude())
                .withTimestamp(cmd.getDate())
                .withHeading((cmd.getHeading()/65535.0)*Math.PI * 2)
                .build();
        System.out.println("Received report from "+asset.getAssetName());
        asset.setState(state);        
    }
    
    public void process(Announce ann) {
        Asset asset = getAssetFor(ann.getSourceName());
        
        if (asset == null && VehiclesHolder.getVehicleById(ann.getSysName()) != null)
            asset = new Asset(ann.getSysName());
        
        if (asset == null)
            return;
        
        AssetState state = AssetState.builder()
                .withLatitude(Math.toDegrees(ann.getLat()))
                .withLongitude(Math.toDegrees(ann.getLon()))
                .withTimestamp(ann.getDate())
                .build();
        asset.setState(state);
    }
    
    public void process(SoiCommand cmd, ConsoleLayout console) {
        if (cmd.getType() != SoiCommand.TYPE.SUCCESS)
            return;

        NeptusLog.pub().info("Processing SoiCommand: " + cmd.asJSON() + ", " + Thread.currentThread().getName() + ", "
                + cmd.hashCode() + " (at " + dateFormatterXMLNoMillisUTC.format(new Date(cmd.getTimestampMillis())) + ")");

        switch (cmd.getCommand()) {
            case GET_PARAMS:
                if (console != null)
                    console.post(Notification.success(I18n.text("Received Settings"),
                            I18n.textf("Received settings from %vehicle (at %time).", cmd.getSourceName(),
                                    dateFormatterXMLNoMillisUTC.format(new Date(cmd.getTimestampMillis()))))
                            .requireHumanAction(true));
                setParams(cmd.getSourceName(), cmd.getSettings());
                break;
            case GET_PLAN:
            case EXEC:
                if (console != null)
                    console.post(Notification.success(I18n.text("Received Plan"),
                            I18n.textf("Received plan from %vehicle (at %time).", cmd.getSourceName(),
                                    dateFormatterXMLNoMillisUTC.format(new Date(cmd.getTimestampMillis()))))
                            .requireHumanAction(true));
                if (cmd.getPlan() != null)
                    plans.put(cmd.getSourceName(), Plan.parse(cmd.getPlan()));
                break;
            case RESUME:
                break;
            case SET_PARAMS:
                break;
            case STOP:
                break;
            default:
                break;
        }
    }

    public synchronized void setParams(String vehicle, LinkedHashMap<String, String> params) {
        if (!settings.containsKey(vehicle))
            settings.put(vehicle, new SoiSettings());

        PluginProperty[] props = PluginUtils.getPluginProperties(settings.get(vehicle));

        for (PluginProperty p : props) {

            if (!params.containsKey(p.getName()))
                continue;

            switch (p.getType().getSimpleName()) {
                case "String":
                    p.setValue(params.get(p.getName()));
                    break;
                case "double":
                    p.setValue(Double.parseDouble(params.get(p.getName())));
                    break;
                case "float":
                    p.setValue(Float.parseFloat(params.get(p.getName())));
                    break;
                case "int":
                    p.setValue(Integer.parseInt(params.get(p.getName())));
                    break;
                case "boolean":
                    p.setValue(Boolean.parseBoolean(params.get(p.getName())));
                    break;
                default:
                    NeptusLog.pub().error("Class not recognized: " + p.getType());
                    break;
            }
        }

        PluginUtils.setPluginProperties(settings.get(vehicle), props);
    }

    public Asset getAssetFor(String systemName) {
        Asset ret = assetsMap.getOrDefault(systemName, null);
        return ret;
    }
    
    @Periodic(millisBetweenUpdates = 60_000)
    private void updateState() {
        Future<List<Asset>> sts = EnduranceWebApi.getSoiState();
        try {
            List<Asset> assetLst = sts.get(5000, TimeUnit.MILLISECONDS);
            StringBuilder sb = new StringBuilder();
            for (Asset received : assetLst) {
                sb.append(received.getAssetName());
                sb.append(" :: ");
                sb.append(received.toString());
                sb.append("\n");
                
                String id = received.getAssetName();
                
                Asset updated = assetsMap.getOrDefault(id, received);
                updated.setPlan(received.getPlan());
                if (received.receivedState() != null)
                    updated.setState(received.receivedState());
                updated.getConfig().clear();
                updated.getConfig().putAll(received.getConfig());
                assetsMap.put(id, updated);
                if (received.getPlan() != null)
                    plans.put(id, received.getPlan());
            }
            if (sb.length() > 0) {
                NeptusLog.pub().info(sb);
            }
        }
        catch (TimeoutException e) {
            NeptusLog.pub().warn(e);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
        }
    }

    /**
     * @return
     */
    public ConcurrentHashMap<String, DripSettings> getDripSettings() {
        return dSettings;
    }
}
