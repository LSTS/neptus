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
 * Jun 2, 2012
 */
package pt.lsts.neptus.comm.manager;

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
            double hz = (msgsInLastSec) * ((time - lastSecondMsgTime) / 1000.0);
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
