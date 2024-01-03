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
 * 2010/05/09
 */
package pt.lsts.neptus.comm.transports;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;

/**
 * @author pdias
 *
 */
public class Notification {

    public static final boolean RECEPTION = true;
	public static final boolean SEND = false;

//	private boolean eosReceived = false;
	private boolean isReception = true;
	
	private InetSocketAddress address;
	private byte[] buffer;	

	private long timeMillis = -1;
	
	// DeliveryListener vars
	private ResultEnum operationResult = ResultEnum.UnFinished;
	private Exception errorObject = null;
	private CompletableFuture<DeliveryResult> deliveryListener = null;
	
	/**
	 * @param isReception
	 * @param address
	 * @param buffer
	 */
    public Notification(boolean isReception, InetSocketAddress address, byte[] buffer) {
        this.address = address;
        this.buffer = buffer;
        setReception(isReception);
    }

    /**
     * @param isReception
     * @param address
     * @param buffer
     * @param timeMillis
     */
    public Notification(boolean isReception, InetSocketAddress address, byte[] buffer, long timeMillis) {
        this.address = address;
        this.buffer = buffer;
        this.timeMillis = timeMillis;
        setReception(isReception);
    }

	/**
	 * @return the buffer
	 */
	public byte[] getBuffer() {
		return buffer;
	}
	
	/**
	 * @param buffer the buffer to set
	 */
	public void setBuffer(byte[] buffer) {
		this.buffer = buffer;
	}

	/**
	 * @return the address
	 */
	public InetSocketAddress getAddress() {
		return address;
	}
	
	/**
	 * @param address the address to set
	 */
	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}
	
	/**
	 * @return the reception
	 */
	public boolean isReception() {
		return isReception;
	}

	/**
	 * @param value the reception to set
	 */
	public void setReception(boolean value) {
		isReception = value;
	}
	
	public boolean isSend() {
		return !isReception;
	}

	public void setSend(boolean value) {
		isReception = !value;
	}

	
	/**
	 * @return the timeMillis
	 */
	public long getTimeMillis() {
		return timeMillis;
	}
	
	/**
	 * @param timeMillis the timeMillis to set
	 */
	public void setTimeMillis(long timeMillis) {
		this.timeMillis = timeMillis;
	}
	
	/**
     * @return the operationResult
     */
    public ResultEnum getOperationResult() {
        return operationResult;
    }
    
    /**
     * @param operationResult the operationResult to set
     */
    public void setOperationResult(ResultEnum operationResult) {
        this.operationResult = operationResult;
    }
    
    /**
     * @return the errorObject
     */
    public Exception getErrorObject() {
        return errorObject;
    }
    
    /**
     * @param errorObject the errorObject to set
     */
    public void setErrorObject(Exception errorObject) {
        this.errorObject = errorObject;
    }
    
    /**
     * @return the deliveryListener
     */
    public CompletableFuture<DeliveryResult> getDeliveryListener() {
        return deliveryListener;
    }
    
    /**
     * @param deliveryListener the deliveryListener to set
     */
    public void setDeliveryListener(CompletableFuture<DeliveryResult> deliveryListener) {
        this.deliveryListener = deliveryListener;
    }
}
