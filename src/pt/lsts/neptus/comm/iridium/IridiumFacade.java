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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.SwingWorker;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.imc.Abort;
import pt.lsts.imc.IMCMessage;

/**
 * @author zp
 *
 */
public class IridiumFacade implements IridiumMessenger, IPeriodicUpdates, IridiumMessageListener {

    private static IridiumFacade instance = null;    
    protected Vector<IridiumMessenger> messengers = new Vector<>();
    protected String iridiumSystemProvider = "any";
    
    protected HashSet<IridiumMessageListener> listeners = new HashSet<>();
    
    @Override
    public void addListener(IridiumMessageListener listener) {
        listeners.add(listener);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getFirstMessengerOfType(Class<T> type) {
        for (IridiumMessenger im : messengers) {
            if (im.getClass().equals(type)) {
                return (T)im;
            }
        }
        return null;
    }
    
    @Override
    public void removeListener(IridiumMessageListener listener) {
        listeners.remove(listener);       
    }
    
    private IridiumFacade() {
        
        NeptusLog.pub().info("Starting Iridium comms");
        updateMessengers();        
    }

    @Override
    public long millisBetweenUpdates() {
        return 60000;
    }

    @Override
    public boolean update() {        
        updateMessengers();        
        return true;
    }

    public static IridiumFacade getInstance() {
        if (instance == null)
            instance = new IridiumFacade();
        return instance;
    }
    
    @Override
    public void sendMessage(final IridiumMessage msg) throws Exception {
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String provider = null;
                for (IridiumMessenger m : messengers) {
                    System.out.println("trying to send iridium message using "+m.getName());
                    //if (m.isAvailable()) {
                        try {
                            m.sendMessage(msg);
                            provider = m.getName();
                            break;
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            NeptusLog.pub().error(e);
                        }
                    //}
                    //else {
                    //    System.err.println(m.getName()+" is not available");
                    //}
                }
                if (provider == null)
                    throw new Exception("Unable to send iridium message");
                NeptusLog.pub().info("Iridium message sent through "+provider);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
                    NeptusLog.pub().error(e);
                }
            }
        };
        worker.execute();
    }

    public void sendMessage(IMCMessage msg) throws Exception {
        ImcIridiumMessage imsg = new ImcIridiumMessage();
        imsg.setMsg(msg);
        imsg.setDestination(msg.getDst());
        imsg.setSource(msg.getSrc());
        imsg.setMessageType(msg.getMgid());
        sendMessage(imsg);
    }

    @Override
    public Collection<IridiumMessage> pollMessages(Date timeSince) throws Exception {
        Vector<IridiumMessage> msgs = new Vector<>();
        for (IridiumMessenger i : messengers)
            msgs.addAll(i.pollMessages(timeSince));

        Collections.sort(msgs);

        return msgs;
    }

    @Override
    public void messageReceived(IridiumMessage msg) {
        for (IridiumMessageListener listener : listeners)
            listener.messageReceived(msg);
    }

    public void updateMessengers() {
        System.out.println("updating messengers");
        ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("iridium",
                SystemTypeEnum.ALL, true);
        if (iridiumSystemProvider == null)
            iridiumSystemProvider = "any";
        
        for (ImcSystem sys : sysLst) {
            System.out.println("Found messenger: "+sys.getName());
        }
        
        if (!iridiumSystemProvider.equals("any")) {
            Vector<IridiumMessenger> toDelete = new Vector<>();
            
            for (IridiumMessenger m : messengers) {
                if (!m.getName().contains(iridiumSystemProvider)) {
                    toDelete.add(m);
                    m.cleanup();
                    NeptusLog.pub().info("Removed "+m);
                }
            }            
            messengers.removeAll(toDelete);
            
            
        }
        
        if (iridiumSystemProvider.equals("any") || (""+iridiumSystemProvider).equalsIgnoreCase("hub")) {
            boolean alreadyAdded = false;
            for (IridiumMessenger m : messengers) {
                if (m instanceof HubIridiumMessenger) {                    
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                HubIridiumMessenger m = new HubIridiumMessenger();
                messengers.add(m);
                NeptusLog.pub().info("Added "+m);
            }
        }
        else {
            
            boolean alreadyAdded = false;
            for (IridiumMessenger m : messengers) {
                if (m instanceof DuneIridiumMessenger && ((DuneIridiumMessenger) m).getMessengerName().equals(iridiumSystemProvider)) {                    
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                DuneIridiumMessenger m = new DuneIridiumMessenger(iridiumSystemProvider);
                messengers.add(m);
                NeptusLog.pub().info("Added "+m);
            }
        }
        
        if (iridiumSystemProvider.equals("any") || (""+iridiumSystemProvider).equalsIgnoreCase("sim")) {
            boolean alreadyAdded = false;
            for (IridiumMessenger m : messengers) {
                if (m instanceof SimulatedMessenger) {                    
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                SimulatedMessenger m = new SimulatedMessenger();
                messengers.add(m);
                NeptusLog.pub().info("Added "+m);
            }
        }     
        
        
        for (ImcSystem s : sysLst) {
            boolean alreadyAdded = false;
            for (IridiumMessenger m : messengers) {
                if (m instanceof DuneIridiumMessenger) {
                    if (((DuneIridiumMessenger)m).messengerName.equals(s.getName())) {
                        alreadyAdded = true;
                        break;
                    }
                }
            }
            if (!alreadyAdded) {
                if (iridiumSystemProvider.equals("any") || iridiumSystemProvider.equals(s.getName())) {
                    DuneIridiumMessenger m = new DuneIridiumMessenger(s.getName());
                    messengers.add(m);
                    m.addListener(this);
                    NeptusLog.pub().info("Added "+m);
                }
            }
        }
        
        for (IridiumMessenger m : messengers) {
            System.out.println("Stayed with: "+m.getName());
        }
        
        if (messengers.isEmpty()) {
            System.err.println("No messengers are available");
        }
    }
    
    @Override
    public String getName() {
        return "Iridium Communications";
    }

    @Override
    public boolean isAvailable() {
        for (IridiumMessenger m : messengers) {
            if (m.isAvailable())
                return true;
        }
        return false;
    }
    
    /**
     * @return the iridiumSystemProvider
     */
    public String getIridiumSystemProvider() {
        return iridiumSystemProvider;
    }

    /**
     * @param iridiumSystemProvider the iridiumSystemProvider to set
     */
    public void setIridiumSystemProvider(String iridiumSystemProvider) {
        this.iridiumSystemProvider = iridiumSystemProvider;
        updateMessengers();
    }

    /**
     * @return the messengers
     */
    public Vector<IridiumMessenger> getMessengers() {
        return messengers;
    }
    
    @Override
    public void cleanup() {
        for (IridiumMessenger m : messengers)
            m.cleanup();
    }

    public static void main(String[] args) throws Exception {
        ConfigFetch.initialize();
        ImcMsgManager.getManager().start();
        Thread.sleep(30000);
        IridiumFacade.getInstance().sendMessage(new Abort());
    }
}
