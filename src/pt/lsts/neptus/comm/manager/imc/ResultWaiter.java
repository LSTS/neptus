/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 26/02/2014
 */
package pt.lsts.neptus.comm.manager.imc;

import java.util.concurrent.Callable;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;

class ResultWaiter implements Callable<SendResult>, MessageDeliveryListener {
    
    public SendResult result = null;
    private long timeoutMillis = 10000;
    private long start;
    
    public ResultWaiter(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        this.start = System.currentTimeMillis();
    }
    
    @Override
    public SendResult call() throws Exception {
        while (true) {
            synchronized (this) {
                if (result != null) {
                    return result;
                }
                if (System.currentTimeMillis() - start > timeoutMillis) {                     
                    return SendResult.TIMEOUT;
                }
            }
            Thread.sleep(100);
        }
    }
    
    @Override
    public void deliveryError(IMCMessage message, Object error) {
        result = SendResult.ERROR;
    }
    
    @Override
    public void deliverySuccess(IMCMessage message) {
        result = SendResult.SUCCESS;
    }

    @Override
    public void deliveryTimeOut(IMCMessage message) {
        result = SendResult.TIMEOUT;
    }

    @Override
    public void deliveryUncertain(IMCMessage message, Object msg) {
        result = SendResult.UNCERTAIN_DELIVERY;
    }

    @Override
    public void deliveryUnreacheable(IMCMessage message) { 
        result = SendResult.UNREACHABLE;
    }        
}