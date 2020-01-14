/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 17/04/2011
 */
package pt.lsts.neptus.comm.manager;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

/**
 * This is an helper implementation for the {@link CommBaseManager} and {@link SystemCommBaseInfo}. Don't use it
 * directly.
 * 
 * @author pdias
 * 
 */
abstract class CommonCommBaseImplementation<M extends IMessage, Mi extends MessageInfo> implements
        MessageListener<Mi, M> {

    protected static boolean useListenersQueues = true;

    protected LinkedList<M> msgQueue = new LinkedList<M>();
    protected LinkedList<Mi> infoQueue = new LinkedList<Mi>();
    protected MessageProcessor messageProcessor = null;

    // Activity Indicators
    protected boolean isActive = false;

    // Processing Freq. variables
    protected double processTimeMillisLastMsg = -1;
    protected long processMsgsInLastSec = 0;
    protected double processMessageFreq = -1;
    protected double processLastSecondMsgTime = 0;
    protected double processDeltaTxRxTimeNanosLastMsg = -1;

    // Arriving Freq. variables
    protected double arrivalTimeMillisLastMsg = -1;
    protected long arrivalMsgsInLastSec = 0;
    protected double arrivalMessageFreq = -1;
    protected double arrivalLastSecondMsgTime = 0;
    protected double arrivalDeltaTxRxTimeNanosLastMsg = -1;

    // Freq. count related variables
    protected long counter = 0;
    private long lastFullQueueWarning = -1;

    protected LinkedHashSet<MessageListener<Mi, M>> listeners = new LinkedHashSet<MessageListener<Mi, M>>();
    protected MessageListener<Mi, M> lastListener = null;
    protected LinkedHashMap<MessageListener<Mi, M>, MessageListenerQueueProvider<Mi, M>> listenersQueueProvider = new LinkedHashMap<MessageListener<Mi, M>, MessageListenerQueueProvider<Mi, M>>();
    private Hashtable<String, MessagePackage<Mi, M>> lastMessagesOfType = new Hashtable<String, MessagePackage<Mi, M>>();

    protected int queueMaxSize = 1024;

    protected long seqInstanceNr = 0;

    /**
     * Is the separation time of each message type that is warn to the listeners. If -1 all messages are warned,
     */
    protected long minDelay = -1;
    protected LinkedHashMap<String, Long> lastReceivedTime = new LinkedHashMap<String, Long>();

    protected PreferencesListener preferencesListener = new PreferencesListener() {
        @SuppressWarnings("unchecked")
        public void preferencesUpdated() {
//            try {
//                queueMaxSize = GeneralPreferences.getPropertyInteger(GeneralPreferences.COMMS_QUEUE_SIZE);
//            }
//            catch (GeneralPreferencesException e) {
//            }
//            try {
//                minDelay = GeneralPreferences.getPropertyInteger(GeneralPreferences.COMMS_MSG_SEPARATION_MILLIS);
//            }
//            catch (GeneralPreferencesException e) {
//            }
            queueMaxSize = GeneralPreferences.commsQueueSize;
            minDelay = GeneralPreferences.commsMsgSeparationMillis;
            
            synchronized (listeners) {
                for (MessageListener<Mi, M> ml : listeners) {
                    if (ml instanceof MessageListenerQueueProvider)
                        ((MessageListenerQueueProvider<Mi, M>) ml).setQueueMaxSize(queueMaxSize);
                }
            }
        }
    };

    /**
     * 
     */
    protected boolean start() {
        NeptusLog.pub().info("Starting comm. " + this.getClass().getSimpleName());
        preferencesListener.preferencesUpdated();
        GeneralPreferences.addPreferencesListener(preferencesListener);
        return true;
    }

    /**
     * 
     */
    protected boolean stop() {
        NeptusLog.pub().warn("Stoping comm. " + this.getClass().getSimpleName());
        GeneralPreferences.removePreferencesListener(preferencesListener);
        return true;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return The messages queue length.
     */
    public final int getMsgQueueLength() {
        synchronized (msgQueue) {
            return msgQueue.size();
        }
    }

    /**
     * To force the clear of the messages queue.
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     */
    public final void clearMsgQueue() {
        synchronized (msgQueue) {
            msgQueue.clear();
            infoQueue.clear();
            try {
                for (MessageListener<Mi, M> msgListener : listenersQueueProvider.keySet())
                    listenersQueueProvider.get(msgListener).clearMessages();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage());
            }
            clearLastMessageOfType();
        }
    }

    // -- ------------------- --//

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the listeners
     */
    protected final LinkedHashSet<MessageListener<Mi, M>> getListeners() {
        return listeners;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return
     */
    public final int getListenersSize() {
        return getListeners().size();
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the lastListener
     */
    public final MessageListener<Mi, M> getLastListener() {
        return lastListener;
    }

    /**
     * @return the listenersQueueProvider
     */
    protected final LinkedHashMap<MessageListener<Mi, M>, MessageListenerQueueProvider<Mi, M>> getListenersQueueProvider() {
        return listenersQueueProvider;
    }

    /**
     * @return the isActive
     */
    public final boolean isActive() {
        return isActive;
    }

    /**
     * This will set activity flag and call {@link #triggerExtraActionOnSetActive(boolean, MessageInfo, IMessage)} to
     * trigger some extra action (by default the {@link #triggerExtraActionOnSetActive(boolean, MessageInfo, IMessage)}
     * does nothing). WARNING info and message can be null.
     * 
     * @return the isActive
     */
    protected final void setActive(boolean isActive, Mi info, M message) {
        this.isActive = isActive;
        triggerExtraActionOnSetActive(isActive, info, message);
    }

    /**
     * <p style="color='ORANGE'">
     * You need to override this method to implement extra actions when
     * {@link #setActive(boolean, MessageInfo, IMessage)} is called.
     * </p>
     * Please don't modify the info and message and take little time on it. WARNING info and message can be null.
     * 
     * @param isActive
     * @param info Can be null.
     * @param message Can be null.
     */
    protected void triggerExtraActionOnSetActive(boolean isActive, Mi info, M message) {

    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the timeLastMsgReceived
     */
    public final double getProcessTimeMillisLastMsg() {
        return processTimeMillisLastMsg;
    }

    /**
     * This function is used to set the last msg time arrival on processor and it will calculate the frequency of
     * arrival.
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * 
     * @param timeMillisLastMsgReceived the timeLastMsgReceived to set
     */
    protected final void setProcessTimeMillisLastMsg(double timeMillisLastMsgReceived) {
        this.processTimeMillisLastMsg = timeMillisLastMsgReceived;

        long time = System.currentTimeMillis();
        if (time - processLastSecondMsgTime > 1000) {
            double hz = ((double) processMsgsInLastSec) * ((time - processLastSecondMsgTime) / 1000.0);
            if (processLastSecondMsgTime > 0)
                processMessageFreq = hz;
            processLastSecondMsgTime = time;
            processMsgsInLastSec = 0;
        }
        else {
            ++processMsgsInLastSec;
        }
    }

    /**
     * @return the processDeltaTxRxTimeNanosLastMsg
     */
    public final double getProcessDeltaTxRxTimeNanosLastMsg() {
        return processDeltaTxRxTimeNanosLastMsg;
    }

    /**
     * @param processDeltaTxRxTimeNanosLastMsg the processDeltaTxRxTimeNanosLastMsg to set
     */
    protected final void setProcessDeltaTxRxTimeNanosLastMsg(double processDeltaTxRxTimeNanosLastMsg) {
        // this.processDeltaTxRxTimeNanosLastMsg = processDeltaTxRxTimeNanosLastMsg;
        long time = System.currentTimeMillis();
        if (time - processTimeMillisLastMsg > 1000) {
            this.processDeltaTxRxTimeNanosLastMsg = processDeltaTxRxTimeNanosLastMsg;
        }
        else {
            this.processDeltaTxRxTimeNanosLastMsg = (this.processDeltaTxRxTimeNanosLastMsg + processDeltaTxRxTimeNanosLastMsg) / 2.0;
        }
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the messageProcessFreq
     */
    public final double getProcessMessageFreq() {
        return processMessageFreq;
    }

    // /**
    // * <p style="color='ORANGE'">Don't need to override this method.</p>
    // *
    // * @param messageProcessFreq the messageProcessFreq to set
    // */
    // protected final void setProcessMessageFreq(double messageProcessFreq) {
    // this.processMessageFreq = messageProcessFreq;
    // }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the arrivalMessageFreq
     */
    public final double getArrivalMessageFreq() {
        return arrivalMessageFreq;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param arrivalMessageFreq the arrivalMessageFreq to set
     */
    protected final void setArrivalMessageFreq(double arrivalMessageFreq) {
        this.arrivalMessageFreq = arrivalMessageFreq;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the arrivalTimeLastMsg
     */
    public final double getArrivalTimeMillisLastMsg() {
        return arrivalTimeMillisLastMsg;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param arrivalTimeMillisLastMsg the arrivalTimeLastMsg to set
     */
    protected final void setArrivalTimeMillisLastMsg(double arrivalTimeMillisLastMsg) {
        this.arrivalTimeMillisLastMsg = arrivalTimeMillisLastMsg;

        long time = System.currentTimeMillis();
        if (time - arrivalLastSecondMsgTime > 1000) {
            double hz = ((double) arrivalMsgsInLastSec) * ((time - arrivalLastSecondMsgTime) / 1000.0);
            if (arrivalLastSecondMsgTime > 0)
                arrivalMessageFreq = hz;
            arrivalLastSecondMsgTime = time;
            arrivalMsgsInLastSec = 0;
        }
        else {
            ++arrivalMsgsInLastSec;
        }
    }

    /**
     * @return the arrivalDeltaTxRxTimeNanosLastMsg
     */
    public final double getArrivalDeltaTxRxTimeNanosLastMsg() {
        return arrivalDeltaTxRxTimeNanosLastMsg;
    }

    /**
     * @param arrivalDeltaTxRxTimeNanosLastMsg the arrivalDeltaTxRxTimeNanosLastMsg to set
     */
    public final void setArrivalDeltaTxRxTimeNanosLastMsg(double arrivalDeltaTxRxTimeNanosLastMsg) {
        // this.arrivalDeltaTxRxTimeNanosLastMsg = arrivalDeltaTxRxTimeNanosLastMsg;
        long time = System.currentTimeMillis();
        if (time - arrivalTimeMillisLastMsg > 1000) {
            this.arrivalDeltaTxRxTimeNanosLastMsg = arrivalDeltaTxRxTimeNanosLastMsg;
        }
        else {
            this.arrivalDeltaTxRxTimeNanosLastMsg = (this.arrivalDeltaTxRxTimeNanosLastMsg + arrivalDeltaTxRxTimeNanosLastMsg) / 2.0;
        }
    }

    // -- ------------- --//

    // -- ------------- --//
    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return
     */
    protected long getMinDelay() {
        return minDelay;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param minDelay
     */
    protected void setMinDelay(long minDelay) {
        this.minDelay = minDelay;
    }

    /**
     * @param i
     * @param msg
     * @return
     */
    private long getLastReceivedTime(Mi i, M msg) {
        String msgname = (msg instanceof IMCMessage) ? ((IMCMessage) msg).getMessageType().getShortName() : msg
                .getClass().getSimpleName();

        String compound = i.getPublisher() + ":" + msgname;

        if (lastReceivedTime.containsKey(compound))
            return lastReceivedTime.get(compound);
        return 0;
    }

    /**
     * @param i
     * @param msg
     */
    private void setLastReceivedTime(Mi i, M msg) {
        String msgname = (msg instanceof IMCMessage) ? ((IMCMessage) msg).getMessageType().getShortName() : msg
                .getClass().getSimpleName();

        String compound = i.getPublisher() + ":" + msgname;

        lastReceivedTime.put(compound, System.currentTimeMillis());
    }

    /**
     * For public channel
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param info
     * @param msg
     */
    @SuppressWarnings("unchecked")
    protected void warnListeners(Mi info, M msg) {

        if (minDelay > 0) {
            if (System.currentTimeMillis() - getLastReceivedTime(info, msg) < minDelay)
                return;
        }

        setLastReceivedTime(info, msg);
        // NeptusLog.pub().info("<###>    >>>>>>>>");
        LinkedHashSet<MessageListener<Mi, M>> listList;
        LinkedHashMap<MessageListener<Mi, M>, MessageListenerQueueProvider<Mi, M>> listQueueProvider;
        synchronized (listeners) {
            try {
                listList = (LinkedHashSet<MessageListener<Mi, M>>) getListeners().clone();
            }
            catch (Exception e1) {
                listList = getListeners();
                e1.printStackTrace();
            }
            try {
                listQueueProvider = (LinkedHashMap<MessageListener<Mi, M>, MessageListenerQueueProvider<Mi, M>>) getListenersQueueProvider()
                        .clone();
            }
            catch (Exception e) {
                listQueueProvider = getListenersQueueProvider();
                e.printStackTrace();
            }
        }
        for (MessageListener<Mi, M> lt : listList) {
            lastListener = lt;
            try {
                if (!useListenersQueues) {
                    lt.onMessage(info, msg);
                }
                else {
                    MessageListenerQueueProvider<Mi, M> lqp = listQueueProvider.get(lt);
                    if (lqp != null) {
                        lqp.addMessage(info, msg);
                    }
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(this + ": Error warning listener '" + lt + "'", e);
            }
            catch (Error e) {
                NeptusLog.pub().fatal(this + ": Fatal Error warning listener '" + lt + "'", e);
            }
            lastListener = null;
        }
        listList = null;
        listQueueProvider = null;
    }

    /**
     * For public channel
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @return
     */
    public boolean addListener(MessageListener<Mi, M> listener) {
        return addListener(listener, (MessageFilter<Mi, M>) null);
    }

    /**
     * Same as {@link #addListener(MessageListener)} but with a filter.
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @param filter If null means no filter.
     * @return
     */
    public boolean addListener(MessageListener<Mi, M> listener, MessageFilter<Mi, M> filter) {
        synchronized (listeners) {
            // NeptusLog.pub().info("<###>    +++++++"+listener);
            boolean ret = listeners.contains(listener);
            if (!ret)
                ret = listeners.add(listener);
            if (ret && useListenersQueues) {
                try {
                    MessageListenerQueueProvider<Mi, M> lqp = listenersQueueProvider.get(listener);
                    if (lqp != null) {
                        lqp.setFilter(filter);
                    }
                    else {
                        lqp = new MessageListenerQueueProvider<Mi, M>(listener, filter);
                        lqp.setQueueMaxSize(queueMaxSize);
                        listenersQueueProvider.put(listener, lqp);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return ret;
        }
    }

    /**
     * For public channel
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param listener
     * @return
     */
    public boolean removeListener(MessageListener<Mi, M> listener) {
        synchronized (listeners) {
            boolean ret = listeners.remove(listener);
            if (ret && useListenersQueues) {
                try {
                    MessageListenerQueueProvider<Mi, M> lqp = listenersQueueProvider.remove(listener);
                    if (lqp != null)
                        lqp.cleanup();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            if (!ret) {
                NeptusLog.pub().error(
                        this.getClass().getSimpleName() + ": Not possible to remove listener from " + listener); 
            }
            return ret;
        }
    }

    // -- ------------------- --//

    // -- -- Open Channel -- --//

    /**
     * This is from {@link #openNode}.
     * <p style="color='PINK'">
     * This method is to be called upon message arrival.
     * </p>
     * {@inheritDoc}
     * 
     * @see pt.lsts.neptus.messages.listener.MessageListener#onMessage(pt.lsts.neptus.messages.listener.MessageInfo,
     *      pt.lsts.neptus.messages.IMessage)
     */
    @Override
    public final void onMessage(Mi info, M msg) {
        if (messageProcessor == null) {
            NeptusLog.pub().error("Comms are down. Cannot use this call!");
            return;
        }
        // Call this one before setArrivalTimeMillisLastMsg
        setArrivalDeltaTxRxTimeNanosLastMsg(info.getTimeReceivedNanos() - info.getTimeSentNanos());
        setArrivalTimeMillisLastMsg(DateTimeUtil.timeStampSeconds() * 1E3);

        // varOpenRec.onMessage(info, msg);
        synchronized (msgQueue) {
            if (msgQueue.size() >= queueMaxSize) {
                long time = System.currentTimeMillis();
                if (time - lastFullQueueWarning > 1000) {
                    String errorMsg = "Message queue is full [" + msgQueue.size() + "] :: " + this;
                    if (messageProcessor.isRunning())
                        NeptusLog.pub().warn(errorMsg);
                    else
                        NeptusLog.pub().fatal(errorMsg);
                    lastFullQueueWarning = time;
                }
                // TODO check if this is necessary 
                // synchronized (messageProcessor) {
                // messageProcessor.notify();
                // }
                return;
            }

            msgQueue.add(msg);
            infoQueue.add(info);
        }
        // messageProcessor.newMessage();
        synchronized (messageProcessor) {
            messageProcessor.notify();
        }
        // NeptusLog.pub().info("<###>>>>>>>>>" + counter++ + "  " + msgQueue.size());
    }

    /**
     * This calls:
     * 
     * <pre>
     * if (processMsgLocally(info, msg))
     *     warnListeners(info, msg);
     * </pre>
     * 
     * <p style="color='DARKVIOLET'">
     * Normally this method is NOT needed to be overwritten.
     * </p>
     * 
     * @param info
     * @param msg
     */
    protected final void processMessage(Mi info, M msg) {
        addLastMessageOfType(info, msg);
        try {
            if (processMsgLocally(info, msg))
                warnListeners(info, msg);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // msg.dump(System.out);
        // NeptusLog.pub().info("<###>Counter: " + counter++);
        counter++;
    }

    /**
     * To be used to process the message locally.
     * 
     * @param info
     * @param msg
     * @return If the return is false the listeners are not warned.
     */
    protected abstract boolean processMsgLocally(Mi info, M msg);

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the seqInstanceNr
     */
    public final long getSeqInstanceNr() {
        return seqInstanceNr;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @param seqInstanceNr the seqInstanceNr to set
     */
    protected final void setSeqInstanceNr(long seqInstanceNr) {
        this.seqInstanceNr = seqInstanceNr;
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the next seqInstanceNr
     */
    public final long getNextSeqInstanceNr() {
        return ++seqInstanceNr;
    }

    // -- ------------- --//

    /**
     * @param type
     * @return
     */
    /*
     * public final MessagePackage<Mi,M> getLastMessageOfType(String type) { synchronized (lastMessagesOfType) {
     * MessagePackage<Mi, M> lastMsg = lastMessagesOfType.get(type); return lastMsg; } }
     */

    /**
     * 
     */
    private void addLastMessageOfType(Mi info, M message) {
        synchronized (lastMessagesOfType) {
            lastMessagesOfType.put(message.getAbbrev(), new MessagePackage<Mi, M>(info, message));
        }
    }

    /**
     * 
     */
    private void clearLastMessageOfType() {
        synchronized (lastMessagesOfType) {
            lastMessagesOfType.clear();
        }
    }

    // -- ------------------ --//

    /**
     * Message processor is a message queue to allow create nodes in a onMessage call
     * 
     * @author Paulo Dias
     * 
     */
    protected final class MessageProcessor extends Thread {
        private boolean running = true;
        private M msgNew;
        private Mi infoNew;

        public MessageProcessor(String name) {
            super(name);
            setDaemon(true);
        }

        public void stopProcessing() {
            running = false;
            synchronized (this) {
                this.notify();
            }
        }
        
        /**
         * @return the running
         */
        public boolean isRunning() {
            return running;
        }

        public void newMessage() {
            synchronized (this) {
                this.notify();
            }
        }

        @Override
        public void run() {
            while (running) {
                // NeptusLog.pub().info("<###>::::::::::");
                synchronized (msgQueue) {
                    msgNew = msgQueue.size() > 0 ? msgQueue.remove() : null; // msgQueue.peek() sometimes dispite the
                                                                             // msgQueue was full the peek return
                                                                             // null!!?!!
                    // NeptusLog.pub().info("<###>msgQueue " + CommonCommBaseImplementation.this.hashCode() + ":  size: " +
                    // msgQueue.size() + "  " + msgNew);
                    if (msgNew != null) {
                        // msgNew = msgQueue.remove();
                        infoNew = infoQueue.remove();
                    }
                    else {
                        infoNew = null;
                    }
                }
                if (msgNew == null) {
                    synchronized (this) {
                        try {
                            // NeptusLog.pub().info("<###>1zzzzzzzzzz" + this.hashCode());
                            wait(500);
                            // NeptusLog.pub().info("<###>1wakeeeeeee");
                            // System.out.flush();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().warn(this, e);
                            // e.printStackTrace();
                        }
                    }
                    continue;
                }
                // NeptusLog.pub().info("<###>1wwwwwwwwww" + this.hashCode());
                try {
                    setActive(true, infoNew, msgNew);
                    // double a1 = DateTimeUtil.timeStampSeconds() * 1E9;
                    // double a2 = infoNew.getTimeReceivedNanos();
                    // double a3 = DateTimeUtil.timeStampSeconds() * 1E9 - infoNew.getTimeReceivedNanos();
                    // Call this one before setArrivalTimeMillisLastMsg
                    setProcessDeltaTxRxTimeNanosLastMsg(DateTimeUtil.timeStampSeconds() * 1E9
                            - infoNew.getTimeReceivedNanos());
                    // - infoNew.getTimeSentNanos());
                    setProcessTimeMillisLastMsg(
                    // System.currentTimeMillis()
                    DateTimeUtil.timeStampSeconds() * 1E3);
                    processMessage(infoNew, msgNew);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(this + " error on child processing method", e);
                }
                // try { Thread.sleep(10); } catch (Exception e) { }
            }
        }
    }
}
