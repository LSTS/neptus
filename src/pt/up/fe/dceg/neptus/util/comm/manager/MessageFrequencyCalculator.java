/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrot?cnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: pdias
 * Jun 2, 2012
 */
package pt.up.fe.dceg.neptus.util.comm.manager;

/**
 * @author pdias
 * 
 */
public class MessageFrequencyCalculator {

    private long msgCount = 0;
    
    // Processing Freq. variables
    protected double timeMillisLastMsg = -1;
    protected long msgsInLastSec = 0;
    protected double messageFreq = -1;
    protected double lastSecondMsgTime = 0;
    protected double deltaTxRxTimeNanosLastMsg = -1;

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the timeLastMsgReceived
     */
    public final double getTimeMillisLastMsg() {
        return timeMillisLastMsg;
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
    public final void setTimeMillisLastMsg(double timeMillisLastMsgReceived) {
        this.timeMillisLastMsg = timeMillisLastMsgReceived;
        
        msgCount++;
        if (msgCount < 0)
            msgCount = 0;

        long time = System.currentTimeMillis();
        if (time - lastSecondMsgTime > 1000) {
            double hz = ((double) msgsInLastSec) * ((time - lastSecondMsgTime) / 1000.0);
            if (lastSecondMsgTime > 0)
                messageFreq = hz;
            lastSecondMsgTime = time;
            msgsInLastSec = 0;
        }
        else {
            ++msgsInLastSec;
        }
    }

    /**
     * @return the msgCount
     */
    public long getMsgCount() {
        return msgCount;
    }
    
    /**
     * @return the processDeltaTxRxTimeNanosLastMsg
     */
    public final double getDeltaTxRxTimeNanosLastMsg() {
        return deltaTxRxTimeNanosLastMsg;
    }

    /**
     * @param processDeltaTxRxTimeNanosLastMsg the processDeltaTxRxTimeNanosLastMsg to set
     */
    public final void setDeltaTxRxTimeNanosLastMsg(double deltaTxRxTimeNanosLastMsg) {
        // this.processDeltaTxRxTimeNanosLastMsg = processDeltaTxRxTimeNanosLastMsg;
        long time = System.currentTimeMillis();
        if (time - timeMillisLastMsg > 1000) {
            this.deltaTxRxTimeNanosLastMsg = deltaTxRxTimeNanosLastMsg;
        }
        else {
            this.deltaTxRxTimeNanosLastMsg = (this.deltaTxRxTimeNanosLastMsg + deltaTxRxTimeNanosLastMsg) / 2.0;
        }
    }

    /**
     * <p style="color='ORANGE'">
     * Don't need to override this method.
     * </p>
     * 
     * @return the messageProcessFreq
     */
    public final double getMessageFreq() {
        double tmpMillis = getTimeMillisLastMsg();
        double timeSeconds = (System.currentTimeMillis() - (long) tmpMillis) / 1000.0;
        if (timeSeconds < 2)
            return messageFreq;
        else
            return -1;
    }

    // /**
    // * <p style="color='ORANGE'">Don't need to override this method.</p>
    // *
    // * @param messageProcessFreq the messageProcessFreq to set
    // */
    // protected final void setMessageFreq(double messageProcessFreq) {
    // this.messageFreq = messageProcessFreq;
    // }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
}
