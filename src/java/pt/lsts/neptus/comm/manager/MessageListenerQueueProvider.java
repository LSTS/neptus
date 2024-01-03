/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 13 de Mar de 2011
 */
package pt.lsts.neptus.comm.manager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;

/**
 * This class implements a queue of messages used to provide a private queue
 * to each {@link MessageListener} used in {@link CommonCommBaseImplementation}.
 * @author pdias
 *
 */
public class MessageListenerQueueProvider<Mi extends MessageInfo, M extends IMessage> {

    private Thread dispacherThread = null;

    protected MessageListener<Mi, M> listener = null;
    protected MessageFilter<Mi, M> filter = null;
    protected final Object filterLock = new Object();
    protected LinkedBlockingQueue<MessagePackage> messageList = null;
    protected int queueMaxSize = 1024;
    private long lastFullQueueWarning = -1;
    
    /**
     * @param listener
     */
    public MessageListenerQueueProvider(MessageListener<Mi, M> listener) {
        initialize();
        this.listener = listener;
        start();
    }

    /**
     * @param listener
     * @param filter
     */
    public MessageListenerQueueProvider(MessageListener<Mi, M> listener, 
            MessageFilter<Mi, M> filter) {
        initialize();
        this.listener = listener;
        this.filter = filter;
        start();
    }

    /**
     * 
     */
    private void initialize() {
        messageList = new LinkedBlockingQueue<MessagePackage>();
    }
    
    /**
     * @return the queueMaxSize
     */
    public int getQueueMaxSize() {
        return queueMaxSize;
    }
    
    /**
     * @param queueMaxSize the queueMaxSize to set
     */
    public void setQueueMaxSize(int queueMaxSize) {
        this.queueMaxSize = queueMaxSize;
    }
    
    /**
     * @param filter the filter to set
     */
    public void setFilter(MessageFilter<Mi, M> filter) {
        synchronized (filterLock) {
            this.filter = filter;
        }
    }
    
    /**
     * Set the filter to none.
     */
    public void clearFilter() {
        setFilter(null);
    }
    
    private void start() {
        getDispacherThread();
    }
    
    private void stop() {
        clearMessages();
        if (dispacherThread != null) {
            dispacherThread.interrupt();
            dispacherThread = null;
        }
    }
    
    /**
     * This call invalidates the further use of this,
     * but should be called to cleanup.
     */
    public void cleanup() {
        stop();
        listener = null;
        messageList = null;
        clearFilter();
    }
    
    /**
     * 
     */
    public void clearMessages() {
        synchronized (messageList) {
            messageList.clear();
        }
    }
    
    /**
     * 
     */
    public int getMessageCount() {
        return messageList.size();
    }
    
    /**
     * @param info
     * @param message
     * @return
     */
    public boolean addMessage(Mi info, M message) {
        MessagePackage pac = new MessagePackage(info , message);
        try {
            if (messageList.size() > queueMaxSize) {
                long time = System.currentTimeMillis();
                if (time - lastFullQueueWarning > 1000) {
                    NeptusLog.pub().warn("Message queue provider is full [" + messageList.size() + "] :: " + this + " for " + listener);
                    lastFullQueueWarning = time;
                }
                return false;
            }
            return messageList.offer(pac, 100, TimeUnit.MILLISECONDS);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage());
            return false;
        }
    }
    
    /**
     * @param pac
     * @return
     */
    private boolean isDontFilterOutMessage(MessagePackage pac) {
        boolean ret = true;
        synchronized (filterLock) {
            if (filter == null)
                ret = true;
            else
                ret = filter.isMessageToListen(pac.messageInfo, pac.message);
        }
        return ret;
    }
    
    /**
     * @return
     */
    private Thread getDispacherThread() {
        if (dispacherThread == null) {
            Thread listenerThread = new Thread("Queue Dispacher " + Integer.toHexString(this.hashCode()) + "for " + listener.getClass().getSimpleName()) {
                public synchronized void start() {
                    NeptusLog.pub().debug(this + "Dispacher Thread Started");
                    super.start();              
                }
                
                public void run() {
                    try {
                        while (true) {
                            // messageList should never be null but in situations in the 
                            //  "2011-05-29 19:41:48,546 ERROR [SwingWorker-pool-1-thread-2] {Neptus.Pub} 
                            //  (ImcMsgManager.java:260) - Error initializing lauv-seacon-2  this will be null"
                            MessagePackage pac = (messageList != null ? messageList.take() : null);
                            if (listener != null && messageList != null) {
                                try {
                                    if (isDontFilterOutMessage(pac))
                                        listener.onMessage(pac.messageInfo, pac.message);
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(this + ": Error warning listener '" +
                                            listener + "'", e);
                                } 
                                catch (Error e) {
                                    NeptusLog.pub().fatal(this + ": Fatal Error warning listener '" +
                                            listener + "'", e);
                                }
                            }
                            else {
                                try { Thread.sleep(50); } catch (Exception e) { NeptusLog.pub().error(e.getMessage());}
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        NeptusLog.pub().debug(this + " Thread interrupted");
                    }
                    
                    NeptusLog.pub().debug(this + " Thread Stopped");
                }
            };
            listenerThread.setPriority(Thread.MIN_PRIORITY+1);
            listenerThread.setDaemon(true);
            listenerThread.start();
            dispacherThread = listenerThread;
        }
        return dispacherThread;
    }

    /**
     * Represents a message package with the message and message info.
     * @author pdias
     */
    private class MessagePackage {
        Mi messageInfo;
        M message;
        
        public MessagePackage(Mi info, M message) {
            this.messageInfo = info;
            this.message = message;
        }
    }
}
