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
 * 2010/05/09
 */
package pt.up.fe.dceg.neptus.util.comm.transports;

import java.net.InetSocketAddress;

import pt.up.fe.dceg.neptus.util.comm.transports.DeliveryListener.ResultEnum;

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
	private DeliveryListener deliveryListener = null;
	
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
     * @param reception
     * @param socketAddress
     * @param recBytes
     * @param currentTimeMillis
     */
    public Notification(boolean isReception, InetSocketAddress address, byte[] buffer, long timeMillis) {
        this.address = address;
        this.buffer = buffer;
        this.timeMillis = timeMillis;
        setReception(isReception);
    }

//	public Notification(boolean isReception, InetSocketAddress address,
//			boolean eos, long timeMillis) {
//		this.address = address;
//		this.eosReceived = eos;
//		this.buffer = new byte[0];
//		this.timeMillis = timeMillis;
//		setReception(isReception);
//	}

//	/**
//	 * @return the eosReceived
//	 */
//	public boolean isEosReceived() {
//		return eosReceived;
//	}
//	
//	/**
//	 * @param eosReceived the eosReceived to set
//	 */
//	public void setEosReceived(boolean eosReceived) {
//		this.eosReceived = eosReceived;
//	}
	
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
	 * @param reception the reception to set
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
    public DeliveryListener getDeliveryListener() {
        return deliveryListener;
    }
    
    /**
     * @param deliveryListener the deliveryListener to set
     */
    public void setDeliveryListener(DeliveryListener deliveryListener) {
        this.deliveryListener = deliveryListener;
    }
}
