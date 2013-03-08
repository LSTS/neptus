/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 30/10/2010
 * $Id:: MultiSystemIMCMessageListener.java 10012 2013-02-21 14:23:45Z pdias    $:
 */
package pt.up.fe.dceg.neptus.plugins;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.loader.NeptusMain;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.CommManagerStatusChangeListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * This Class will hook itself to the {@link ImcMsgManager} to filter messages and systems of interest controlled by
 * {@link #setMessagesToListen(String...)}, {@link #setSystemToListen(ImcId16...)} or
 * {@link #setSystemToListenStrings(String...)}. The {@link #messageArrived(ImcId16, IMCMessage)} must be implemented.
 * You MUST call {@link #clean()} in order to detach the listeners when you want to dispose of this.
 * 
 * @author Paulo Dias
 */
public abstract class MultiSystemIMCMessageListener {
    private String realListenerClientStr;
    private Vector<String> messagesToListen = new Vector<String>();
    private boolean listenToAllSystems = false;
    private MessageListener<MessageInfo, IMCMessage> allSystemsListener = null;
    private Map<ImcId16, MessageListener<MessageInfo, IMCMessage>> systemsListen = Collections
            .synchronizedMap(new HashMap<ImcId16, MessageListener<MessageInfo, IMCMessage>>());
    private CommManagerStatusChangeListener comStatusListener = null;

    /**
     * @param realListenerClientStr Used in {@link #toString()} just for easy debug on who's the real listener.
     */
    public MultiSystemIMCMessageListener(String realListenerClientStr) {
        this.realListenerClientStr = realListenerClientStr;
        initialize();
    }

    private void initialize() {
        initAllSystemsListener();
        initComStatusListener();
        ImcMsgManager.getManager().addStatusListener(comStatusListener);
    }

    /**
     * This will remove all the listeners. To make it working again call the {@link #setSystemToListen(ImcId16...)}
     */
    public final void clean() {
        ImcMsgManager.getManager().removeStatusListener(comStatusListener);
        HashMap<ImcId16, MessageListener<MessageInfo, IMCMessage>> systemsListen = new HashMap<ImcId16, MessageListener<MessageInfo, IMCMessage>>();
        synchronized (this.systemsListen) {
            systemsListen.putAll(this.systemsListen);
            this.systemsListen.clear();
        }
        for (ImcId16 id : systemsListen.keySet())
            ImcMsgManager.getManager().removeListener(systemsListen.get(id), id);
        systemsListen.clear();
    }

    private void initAllSystemsListener() {
        if (allSystemsListener != null)
            return;
        allSystemsListener = new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                ImcId16 id;
                try {
                    id = new ImcId16(msg.getHeader().getValue("src"));
                }
                catch (Exception e) {
                    id = ImcId16.NULL_ID;
                }
                newMessageFromSystem(id, info, msg);
            }
        };
    }

    private void initComStatusListener() {
        if (comStatusListener != null)
            return;
        comStatusListener = new CommManagerStatusChangeListener() {
            @Override
            public void managerStatusChanged(int status, String msg) {
            }

            @Override
            public void managerSystemAdded(String systemId) {
                if (!listenToAllSystems)
                    return;
                ImcId16 id = new ImcId16(systemId);
                boolean ret = ImcMsgManager.getManager().addListener(allSystemsListener, id);
                synchronized (systemsListen) {
                    if (ret)
                        systemsListen.put(id, allSystemsListener);
                }
            }

            @Override
            public void managerSystemRemoved(String systemId) {
            }

            @Override
            public void managerSystemStatusChanged(String systemId, int status) {
            }

            @Override
            public void managerVehicleAdded(VehicleType vehicle) {
                if (!listenToAllSystems)
                    return;
                ImcId16 id = vehicle.getImcId();
                boolean ret = ImcMsgManager.getManager().addListener(allSystemsListener, id);
                synchronized (systemsListen) {
                    if (ret)
                        systemsListen.put(id, allSystemsListener);
                }
            }

            @Override
            public void managerVehicleRemoved(VehicleType vehicle) {
            }

            @Override
            public void managerVehicleStatusChanged(VehicleType vehicle, int status) {
            }
        };
    }

    /**
     * @return the messagesToListen
     */
    public final String[] getMessagesToListen() {
        synchronized (messagesToListen) {
            return messagesToListen.toArray(new String[messagesToListen.size()]);
        }
    }

    /**
     * @param messages The list of messages to listen or empty for all.
     */
    public final void setMessagesToListen(String... messages) {
        synchronized (messagesToListen) {
            messagesToListen.clear();
            if (messages != null) {
                for (String msg : messages) {
                    if (msg != null && !"".equalsIgnoreCase(msg))
                        messagesToListen.add(msg);
                }
            }
        }
    }

    public final ImcId16[] getSystemToListen() {
        synchronized (systemsListen) {
            return systemsListen.keySet().toArray(new ImcId16[systemsListen.size()]);
        }
    }

    /**
     * @param idsOrNames A list of String representation of IMC IDs or name IDs.
     */
    public final void setSystemToListenStrings(String... idsOrNames) {
        Vector<ImcId16> ids = new Vector<ImcId16>();
        for (String str : idsOrNames) {
            ImcId16 id;
            try {
                id = ImcId16.valueOf(str);
                ids.add(id);
            }
            catch (NumberFormatException e) {
                ImcSystem res = ImcSystemsHolder.lookupSystemByName(str);
                if (res != null) {
                    if (res.getId() != null)
                        ids.add(res.getId());
                }
                else {
                    VehicleType veh = VehiclesHolder.getVehicleById(str);
                    if (veh != null) {
                        if (veh.getImcId() != null)
                            ids.add(veh.getImcId());
                    }
                }
            }
        }
        setSystemToListen(ids.toArray(new ImcId16[ids.size()]));
    }

    /**
     * @param ids The ids of system to listen or empty for all systems.
     */
    @SuppressWarnings("unchecked")
    public final void setSystemToListen(ImcId16... ids) {
        Vector<ImcId16> systemsToListen = new Vector<ImcId16>();
        Vector<ImcId16> systemsToRemove = new Vector<ImcId16>();

        synchronized (systemsListen) {
            initialize();
            if (ids == null || ids.length == 0)
                listenToAllSystems = true;
            else
                listenToAllSystems = false;
            systemsToListen.addAll(systemsListen.keySet());
        }

        systemsToRemove = (Vector<ImcId16>) systemsToListen.clone();
        if (ids != null && ids.length != 0) {
            systemsToListen.clear();
            for (ImcId16 id : ids)
                systemsToListen.add(id);
        }
        for (ImcId16 id : systemsToRemove.toArray(new ImcId16[systemsToRemove.size()])) {
            if (systemsToListen.contains(id))
                systemsToRemove.remove(id);
            else {
                MessageListener<MessageInfo, IMCMessage> list = systemsListen.get(id);
                if (list != null) {
                    boolean ret = ImcMsgManager.getManager().removeListener(list, id);
                    if (ret) {
                        synchronized (systemsToListen) {
                            systemsListen.remove(id);
                        }
                    }
                }
            }
        }

        if (ids == null || ids.length == 0) {
            systemsToListen.addAll(ImcMsgManager.getManager().getCommInfo().keySet());
        }
        for (final ImcId16 id : systemsToListen) {
            MessageListener<MessageInfo, IMCMessage> list = systemsListen.get(id);
            if (list == null) {
                list = new MessageListener<MessageInfo, IMCMessage>() {
                    @Override
                    public void onMessage(MessageInfo info, IMCMessage msg) {
                        newMessageFromSystem(id, info, msg);
                    }
                };
                boolean ret = ImcMsgManager.getManager().addListener(list, id);
                if (ret) {
                    synchronized (systemsToListen) {
                        systemsListen.put(id, list);
                    }
                }
            }
        }

    }

    private void newMessageFromSystem(ImcId16 id, MessageInfo info, IMCMessage msg) {
        synchronized (messagesToListen) {
            if (messagesToListen.size() == 0 || messagesToListen.contains(msg.getAbbrev()))
                messageArrived(id, msg);
        }
    }

    /**
     * Override this in order to process the system(s) message(s).
     * 
     * @param id
     * @param msg
     */
    public abstract void messageArrived(ImcId16 id, IMCMessage msg);

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + Integer.toHexString(hashCode()) + "] working for "
                + realListenerClientStr;
    }

    /**
     * @return the realListenerClientStr
     */
    public String getRealListenerClientStr() {
        return realListenerClientStr;
    }

    public static void main(String[] args) {
        NeptusMain.main(new String[0]);
        MultiSystemIMCMessageListener msl = new MultiSystemIMCMessageListener("Test") {
            @Override
            public void messageArrived(ImcId16 id, IMCMessage msg) {
                System.out.print(id + ":  ");
                msg.dump(System.out);
            }
        };
        try {
            Thread.sleep(20000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setSystemToListen(new ImcId16("00:16"));
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setMessagesToListen("EstimatedState", "Abort");
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setSystemToListen(new ImcId16("00:15"));
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setMessagesToListen();
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setSystemToListenStrings();
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-------------- Change");
        msl.setSystemToListenStrings("00:16", "lauv-seacon-3");
    }
}