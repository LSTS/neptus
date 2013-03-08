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
 * 13 de Mar de 2011
 * $Id:: MessageListenerQueueProvider.java 9615 2012-12-30 23:08:28Z pdias      $:
 */
package pt.up.fe.dceg.neptus.util.comm.manager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.messages.IMessage;
import pt.up.fe.dceg.neptus.messages.MessageFilter;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;

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
                    NeptusLog.pub().warn("Message queue is full [" + messageList.size() + "] :: " + this + " for " + listener);
                    lastFullQueueWarning = time;
                }
                return false;
            }
            return messageList.offer(pac, 100, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
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
            Thread listenerThread = new Thread(MessageListenerQueueProvider.class.getSimpleName()
                    + ": Dispacher Thread " + Integer.toHexString(this.hashCode()) + "for " + listener) {
                public synchronized void start() {
                    NeptusLog.pub().info(this + "Dispacher Thread Started");
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
                                try { Thread.sleep(50); } catch (Exception e) { }
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        NeptusLog.pub().info(this + " Thread interrupted");
                    }
                    
                    NeptusLog.pub().info(this + " Thread Stopped");
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
