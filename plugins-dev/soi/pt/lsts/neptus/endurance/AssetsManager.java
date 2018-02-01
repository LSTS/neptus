/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SoiCommand;
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
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class AssetsManager {

    private static AssetsManager instance = null;
    
    private Map<String, SoiSettings> settings = (Map<String, SoiSettings>) Collections
            .synchronizedMap(new LinkedHashMap<String, SoiSettings>());
    private Map<String, Plan> plans = (Map<String, Plan>) Collections
            .synchronizedMap(new LinkedHashMap<String, Plan>());
    private Map<String, Asset> assetsMap = (Map<String, Asset>) Collections
            .synchronizedMap(new LinkedHashMap<String, Asset>());
    
    private ReentrantLock lockAssets = new ReentrantLock();

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
    
    /**
     * @return the plans
     */
    public Map<String, Plan> getPlans() {
        return plans;
    }
    
    /**
     * @return the settings
     */
    public Map<String, SoiSettings> getSettings() {
        return settings;
    }
    
    public void sendCommand(String systemName, SoiCommand cmd, CommMean commMean, ConsoleLayout console) {
        if (commMean == CommMean.WiFi) {
            ImcMsgManager.getManager().sendMessageToSystem(cmd, systemName, createMessageDeliveryListener(console, systemName));
            NeptusLog.pub().warn("Command sent " + cmd.getCommandStr() + " sent over UDP to " + systemName + " :: " + cmd.asJSON());
            if (console != null)
                console.post(Notification.success(I18n.text("Command sent"),
                    I18n.textf("%cmd sent over UDP to %vehicle.", cmd.getCommandStr(), systemName)));
        }
        else if (commMean == CommMean.Iridium) {
            try {
                ImcSystem system = ImcSystemsHolder.lookupSystemByName(systemName);
                ImcIridiumMessage msg = new ImcIridiumMessage();
                msg.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                msg.setMsg(cmd);
                msg.setDestination(system.getId().intValue());
                IridiumManager.getManager().send(msg);
                if (console != null)
                    console.post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "
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
            
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                if (console != null)
                    console.post(Notification.error(I18n.text("Command Delivery Unreacheable"),
                        I18n.textf("%cmd sent over Wi-Fi to %system.", message, systemName)));
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                if (console != null)
                    console.post(Notification.success(I18n.text("Command Delivery Uncertain"),
                        I18n.textf("%cmd sent over Wi-Fi to %system with reason: %reason.", message, systemName, msg)));
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                if (console != null)
                    console.post(Notification.error(I18n.text("Command Delivery TimeOut"),
                        I18n.textf("%cmd sent over Wi-Fi to %system.", message, systemName)));
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
                if (console != null)
                    console.post(Notification.success(I18n.text("Command Delivery Success"),
                        I18n.textf("%cmd sent over Wi-Fi to %system.", message, systemName)));
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                if (console != null)
                    console.post(Notification.success(I18n.text("Command Delivery Error"),
                        I18n.textf("%cmd sent over Wi-Fi to %system with reason: %reason.", message, systemName, error)));
            }
        };
        return listener;
    }

    public void process(SoiCommand cmd, ConsoleLayout console) {
        if (cmd.getType() != SoiCommand.TYPE.SUCCESS)
            return;

        NeptusLog.pub().info("Processing SoiCommand: " + cmd.asJSON() + ", " + Thread.currentThread().getName() + ", "
                + cmd.hashCode());

        switch (cmd.getCommand()) {
            case GET_PARAMS:
                if (console != null)
                    console.post(Notification.success(I18n.text("SOI Settings"),
                            I18n.textf("Received settings from %vehicle.", cmd.getSourceName())));
                setParams(cmd.getSourceName(), cmd.getSettings());
                break;
            case GET_PLAN:
            case EXEC:
                if (console != null)
                    console.post(Notification.success(I18n.text("SOI Plan"),
                            I18n.textf("Received plan from %vehicle.", cmd.getSourceName())));
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
                    System.out.println("Class not recognized: " + p.getType());
                    break;
            }
        }

        PluginUtils.setPluginProperties(settings.get(vehicle), props);
    }

    public Asset getAssetFor(String systemName) {
        lockAssets.lock();
        Asset ret = assetsMap.get(systemName);
        lockAssets.unlock();
        return ret;
    }
    
    @Periodic(millisBetweenUpdates = 1000)
    private void updatePos() {
        Future<List<Asset>> sts = EnduranceWebApi.getSoiState();
        try {
            List<Asset> assetLst = sts.get(750, TimeUnit.MILLISECONDS);
            StringBuilder sb = new StringBuilder();
            for (Asset asset : assetLst) {
                sb.append(asset.getAssetName());
                sb.append(" :: ");
                sb.append(asset.toString());
                sb.append("\n");
                
                String id = asset.getAssetName();
                
                lockAssets.lock();
                if (assetsMap.containsKey(id)) {
                    assetsMap.put(id, asset);
                }
                else {
                    Asset curAsset = assetsMap.get(id);
                    curAsset.setState(asset.currentState());
                    curAsset.setPlan(asset.getPlan());
                    curAsset.getConfig().clear();
                    curAsset.getConfig().putAll(asset.getConfig());
                }
                lockAssets.unlock();
                
                plans.put(id, asset.getPlan());
            }
            if (sb.length() > 0)
                System.out.println(sb.toString());
        }
        catch (TimeoutException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
