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
 * Author: pdias
 * 02/03/2016
 */
package pt.lsts.neptus.comm.manager.imc;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;

/**
 * @author pdias
 *
 */
public class FutureMessageSenderResult implements Future<ImcMsgManager.SendResult>, MessageDeliveryListener {

    private boolean finished = false;
    private boolean cancelled = false;
    private SendResult result = null;

    public FutureMessageSenderResult() {
    }

    @Override
    public boolean isDone() {
        return finished;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public SendResult get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        long startTime = System.currentTimeMillis();
        while (result == null) {
            Thread.sleep(100);
            if (System.currentTimeMillis() - startTime > unit.toMillis(timeout))
                throw new TimeoutException("Timeout while waiting");
        }
        return result;
    }
    
    @Override
    public SendResult get() throws InterruptedException, ExecutionException {
        while (result == null) {
            Thread.sleep(100);
        }
        return result;
    }
    
    /**
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!cancelled) {
            if (mayInterruptIfRunning) {
                finished = true;
                cancelled = true;
            }
            else if (finished) {
                cancelled = true;
            }
        }
        return cancelled;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener#deliveryError(pt.lsts.imc.IMCMessage, java.lang.Object)
     */
    @Override
    public void deliveryError(IMCMessage message, Object error) {
        result = SendResult.ERROR;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener#deliverySuccess(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void deliverySuccess(IMCMessage message) {
        result = SendResult.SUCCESS;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener#deliveryTimeOut(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void deliveryTimeOut(IMCMessage message) {
        result = SendResult.TIMEOUT;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener#deliveryUncertain(pt.lsts.imc.IMCMessage, java.lang.Object)
     */
    @Override
    public void deliveryUncertain(IMCMessage message, Object msg) {
        result = SendResult.UNCERTAIN_DELIVERY;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener#deliveryUnreacheable(pt.lsts.imc.IMCMessage)
     */
    @Override
    public void deliveryUnreacheable(IMCMessage message) { 
        result = SendResult.UNREACHABLE;
    }
}
