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
 * 2007/05/19
 */
package pt.lsts.neptus.comm.manager;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Every Comm. Manager should be a singleton. So it MUST be a method called getManager() which SOULD BE public and
 * static.
 * 
 * @author Paulo Dias
 * 
 * @param <M> extends IMessage
 * @param <Mi> extends MessageInfo
 * @param <C> extends VehicleCommInfo<M, I>
 * @param <I> its the identifier
 * @param <L> extends CommManagerStatusChangeListener
 */
public abstract class CommBaseManager<M extends IMessage, Mi extends MessageInfo, C extends SystemCommBaseInfo<M, Mi, I>, I, L extends CommManagerStatusChangeListener>
        extends CommonCommBaseImplementation<M, Mi> implements MessageListener<Mi, M> {

    protected LinkedHashSet<L> statusListeners = new LinkedHashSet<L>();

    protected boolean started = false;

    // public static String CUCS_VSM_STRING = "CUCS-VSM";
    // public static String VSM_CUCS_STRING = "VSM-CUCS";

    // public static String CONNECTION4CUCS = "Connection4CUCS";

    /* ----------------------------- */
    // Public Constants
    public static final int MANAGER_START = 0;
    public static final int MANAGER_STOP = 1;
    public static final int MANAGER_ERROR = 2;

    public static final int SYS_COMM_ON = 0;
    public static final int SYS_COMM_OFF = 1;
    public static final int SYS_NEW = 2;
    public static final int SYS_CTRL_CHANGED = 3;

    /* ----------------------------- */

    protected Thread timerThread;

    protected LinkedHashMap<I, C> commInfo = new LinkedHashMap<I, C>();

    protected CommBaseManager() {
        // init();
    }

    /**
     * Initialize manager. Calls {@link #initManagerComms()} linking the created node to {@link #openNode} and calls
     * it's start method. Also starts {@link #messageProcessor} and starts it. Then sends a {@link #MANAGER_START}
     * event. Next calls a resume message processing to each started system comms.
     * <p style="color='GREEN'">
     * This one needs to be called if you override it.
     * </p>
     * 
     * @return
     */
    @Override
    public synchronized boolean start() {
        NeptusLog.pub().debug("Starting comms");
        if (started)
            return true; // do nothing

        // Just in case
        if (messageProcessor != null) {
            messageProcessor.stopProcessing();
            messageProcessor = null;
        }

        messageProcessor = new MessageProcessor(this.getClass().getSimpleName() + " [" + this.hashCode() + "] :: "
                + MessageProcessor.class.getSimpleName());
        messageProcessor.start();

        if (initManagerComms()) {
            if (!startManagerComms()) {
                messageProcessor.stopProcessing();
                messageProcessor = null;
                return false;
            }
        }
        else {
            messageProcessor.stopProcessing();
            messageProcessor = null;
            return false;
        }

        sendManagerStatusChanged(MANAGER_START, "");

        for (SystemCommBaseInfo<M, Mi, I> vic : commInfo.values()) {
            vic.resumeMsgProcessing();
        }

        super.start();

        if (timerThread == null) {
            timerThread = getTimerThread();
            timerThread.setDaemon(true);
            timerThread.start();
        }
        started = true;
        return true;
    }

    public Thread getTimerThread() {
        Thread t = new Thread(this.getClass().getSimpleName() + " [" + this.hashCode() + "] - Thread") {
            long timeControl = -1;
           
            @Override
            public void run() {
                try {
                    long prevTime = System.currentTimeMillis();
                    while (true) {
                        if ((System.currentTimeMillis() - prevTime) > 2000) {
                            if (timeControl - getArrivalTimeMillisLastMsg() > 10000)
                                setActive(false, null, null);
                            for (C cinf : commInfo.values()) {
                                if (timeControl - cinf.getArrivalTimeMillisLastMsg() > 10000) {
                                    cinf.setActive(false, null, null);
                                    // System.err.println("---------------------------------------------");
                                }
                            }
                            prevTime = System.currentTimeMillis();
                            timeControl = System.currentTimeMillis();
                        }
                        Thread.sleep(100);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        return t;
    }

    /**
     * Shutdown. Stops the {@link #openNode} and {@link #messageProcessor} and clears the messages queue. Calls the
     * stopMsgProcessing on the vehicle comms. Then sends a {@link #MANAGER_STOP} event.
     * <p style="color='GREEN'">
     * This one needs to be called if you override it.
     * </p>
     * 
     * @return
     */
    @Override
    public synchronized boolean stop() {
        boolean ret;
        try {
            if (!started)
                return true; // do nothing
            NeptusLog.pub().debug("Stopping comms");
            if(timerThread != null) {
                timerThread.interrupt();
                timerThread = null;
            }

            stopManagerComms();

            if (messageProcessor != null) {
                messageProcessor.stopProcessing();
                messageProcessor = null;
                msgQueue.clear();
                infoQueue.clear();
            }

            for (SystemCommBaseInfo<M, Mi, I> vic : commInfo.values()) {
                vic.stopMsgProcessing();
            }

            sendManagerStatusChanged(MANAGER_STOP, "");
            started = false;
            ret = true;
        }
        catch (Exception e) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), e);
            NeptusLog.pub().error("CommBase Manager init error!", e);
            ret = false;
        }
        super.stop();
        return ret;
    }

    // -- ------------------- --//
    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param status
     * @param msg
     */
    protected synchronized void sendManagerStatusChanged(int status, String msg) {
        synchronized (statusListeners) {
            for (L sl : getStatusListeners()) {
                sl.managerStatusChanged(status, msg);
            }
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param vehicle
     * @param status
     */
    public void sendManagerVehicleStatusChanged(VehicleType vehicle, int status) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerVehicleStatusChanged(vehicle, status);
            }
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param vehicle
     */
    protected void sendManagerVehicleAdded(VehicleType vehicle) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerVehicleAdded(vehicle);
            }

        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param vehicle
     */
    protected void sendManagerVehicleRemoved(VehicleType vehicle) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerVehicleRemoved(vehicle);
            }
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param commId
     * @param status
     */
    protected void sendManagerSystemStatusChanged(I commId, int status) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerSystemStatusChanged(commId.toString(), status);
            }
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param commId
     */
    protected void sendManagerSystemAdded(I commId) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerSystemAdded(commId.toString());
            }

        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param commId
     */
    protected void sendManagerSystemRemoved(I commId) {
        synchronized (statusListeners) {
            for (L sl : statusListeners) {
                sl.managerSystemRemoved(commId.toString());
            }
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return Returns the listeners.
     */
    protected LinkedHashSet<L> getStatusListeners() {
        synchronized (statusListeners) {
            return statusListeners;
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners}).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listeners The listeners to set.
     */
    protected void setStatusListeners(LinkedHashSet<L> listeners) {
        this.statusListeners = listeners;
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners} ).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return
     */
    public int getStatusListenersSize() {
        synchronized (statusListeners) {
            return statusListeners.size();
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners} ).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @return
     */
    public boolean addStatusListener(L listener) {
        synchronized (statusListeners) {
            boolean ret = statusListeners.add(listener);
            return ret;
        }
    }

    /**
     * This is related to the status change listeners ({@link #statusListeners} ).
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @return
     */
    public boolean removeStatusListener(L listener) {
        synchronized (statusListeners) {
            boolean ret = statusListeners.remove(listener);
            return ret;
        }
    }

    // -- ------------------- --//

    /**
     * @return
     */
    public String getStatusListenersAsHtmlFragment() {
        String ret = "";
        boolean first = true;
        for (L lst : getStatusListeners()) {
            if (!first)
                ret += "<br>";
            else
                first = false;
            ret += lst.getClass().getName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
            // ret += lst.getClass().getSimpleName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
        }
        return ret;
    }

    /**
     * @return
     */
    public String getListenersAsHtmlFragment() {
        String ret = "";
        boolean first = true;
        for (MessageListener<Mi, M> lst : getListeners()) {
            if (!first)
                ret += "<br>";
            else
                first = false;
            ret += lst.getClass().getName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
            // ret += lst.getClass().getSimpleName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
            if (useListenersQueues) {
                try {
                    ret += " {" + getListenersQueueProvider().get(lst).getMessageCount() + " msgs}";
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (lastListener == lst) {
                ret += " working";
            }
        }
        return ret;
    }

    /**
     * @param id
     * @return
     */
    public String getListenersAsHtmlFragment(I id) {
        String ret = "";
        C ci = commInfo.get(id);
        MessageListener<Mi, M> lastListenerForCom = ci.getLastListener();
        if (ci != null) {
            boolean first = true;
            for (MessageListener<Mi, M> lst : ci.getListeners()) {
                if (!first)
                    ret += "<br>";
                else
                    first = false;
                ret += lst.getClass().getName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
                // ret += lst.getClass().getSimpleName() + " [" + Integer.toHexString(lst.hashCode()) + "]";
                if (useListenersQueues) {
                    try {
                        ret += " {" + ci.getListenersQueueProvider().get(lst).getMessageCount() + " msgs}";
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(this.getClass().getSimpleName(), e);
                    }
                }
                if (lastListenerForCom == lst) {
                    ret += " working";
                }
            }
        }
        return ret;
    }

    // -- ------------------- --//

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param id
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    public C getCommInfoById(I id) {
        return getCommInfo().get(id);
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return Returns the commInfo.
     */
    public LinkedHashMap<I, C> getCommInfo() {
        return commInfo;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param commInfo The commInfo to set.
     */
    protected void setCommInfo(LinkedHashMap<I, C> commInfo) {
        this.commInfo = commInfo;
    }

    /**
     * Initialize a vehicle comms (as in {@link SystemCommBaseInfo}).
     * <p style="color='DARKVIOLET'">
     * This function HAS TO BE overwritten (return null as the default implementation).
     * </p>
     * This is not abstract to force "synchronized".
     * 
     * @param vIdS
     * @param inetAddress
     * @return
     */
    public synchronized C initSystemCommInfo(I vIdS, String inetAddress) {
        return null;
    }

    // -- ------------------ --//

    /**
     * Initialize the comms. but don't start.
     * @return
     */
    abstract protected boolean initManagerComms();

    /**
     * Start the comms.
     * @return
     */
    abstract protected boolean startManagerComms();

    /**
     * Stop the comms.
     * @return
     */
    abstract protected boolean stopManagerComms();

    // -- ------------------ --//

    /**
     * Should call {@link #sendMessage(MiddlewareMessage, Object)}.
     * 
     * @param message
     * @param vehicle
     * @param sendProperties Properties to allow decision on how to send the message. Implementation dependent but can
     *            be a list.
     * @return
     */
    abstract public boolean sendMessageToVehicle(M message, VehicleType vehicle, String sendProperties);

    /**
     * Should call {@link #sendMessage(MiddlewareMessage, Object)}.
     * 
     * @param message
     * @param vehicleID This id is the vehicle id as defined by {@link VehicleType}.
     * @param sendProperties Properties to allow decision on how to send the message. Implementation dependent but can
     *            be a list.
     * @return
     */
    abstract public boolean sendMessageToVehicle(M message, String vehicleID, String sendProperties);

    /**
     * @param message
     * @param vehicleCommId
     * @param sendProperties Properties to allow decision on how to send the message. Implementation dependent but can
     *            be a list.
     * @return
     */
    abstract public boolean sendMessage(M message, I vehicleCommId, String sendProperties);

    // -- ------------------ --//

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @param vehicleCommId
     * @return
     */
    public boolean addListener(MessageListener<Mi, M> listener, I vehicleCommId) {
        return addListener(listener, vehicleCommId, null);
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @param vehicleCommId
     * @param filter
     * @return
     */
    public boolean addListener(MessageListener<Mi, M> listener, I vehicleCommId, MessageFilter<Mi, M> filter) {
        C vci = null;
        I vIdS = vehicleCommId;

        if (vehicleCommId == null)
            return false;

        vci = commInfo.get(vIdS);
        if (vci == null) {
            // FIXME Ver o q preencher aqui no InetAdress
            initSystemCommInfo(vIdS, "");
            vci = commInfo.get(vIdS);
        }
        boolean ret = false;

        if (vci != null)
            ret = vci.addListener(listener, filter);

        NeptusLog.pub().debug("Add listener for vehicle: " + vIdS + ".");

        return ret;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @param vehicleCommId
     * @return
     */
    public boolean removeListener(MessageListener<Mi, M> listener, I vehicleCommId) {
        C vci = null;
        I vIdS = vehicleCommId;

        if (vehicleCommId == null)
            return false;

        vci = commInfo.get(vIdS);
        if (vci == null) {
            return false;
        }

        boolean ret = vci.removeListener(listener);

        return ret;
    }

    /**
     * Removes this listener from all systems excluding the common.
     * 
     * @param listener
     * @return
     */
    public final boolean removeListenerFromAllSystems(MessageListener<Mi, M> listener) {
        boolean ret = false;
        int r = 0;
        for (C vci : commInfo.values()) {
            boolean rt = vci.removeListener(listener); // This HAS TO be separated from the line of code bellow because
                                                       // it might not run if "ret" is already true
            ret = ret || rt;
            if (rt)
                r++;
        }
        NeptusLog.pub().debug("Removed " + listener.getClass().getName() + " | " + r + " | " + listener.hashCode());
        return ret;
    }

    // -- ------------------ --//

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return
     */
    public boolean isRunning() {
        return started;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return
     */
    public int getNumberOfSystems() {
        return commInfo.size();
    }

    // -- ------------------ --//

    // /**
    // * @param args
    // * @throws InterruptedException
    // */
    // public static void main(String[] args)
    // {
    // ConfigFetch.initialize();
    // //getManager().init();
    // GuiUtils.testFrame(new JButton(), "Teste");
    // }
}
