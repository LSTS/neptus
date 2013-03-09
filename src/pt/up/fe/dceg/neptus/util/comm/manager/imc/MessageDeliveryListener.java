/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/10/23
 */
package pt.up.fe.dceg.neptus.util.comm.manager.imc;

import pt.up.fe.dceg.neptus.imc.IMCMessage;

/**
 * This interface is used by clients that wish to send messages reliably.
 * @author zp
 */
public interface MessageDeliveryListener {
	
	/**
	 * Message has been successfully delivered to target
	 * @param message The message that was sent for reliable delivery
	 */
	public void deliverySuccess(IMCMessage message);
	
	/**
	 * Delivery time out after some time. End point may be disconnected or network conditions are poor 
	 * @param message The message that was sent for reliable delivery
	 */
	public void deliveryTimeOut(IMCMessage message);
	
	/**
	 * Unable to reach end point. The end point may have disconnected or destination is invalid. 
	 * @param message The message that was sent for reliable delivery
	 */
	public void deliveryUnreacheable(IMCMessage message);
	
	/**
	 * Unexpected error while trying to deliver message
	 * @param message The message that was sent for reliable delivery
	 * @param error The error that was found or returned by the end point.
	 */
	public void deliveryError(IMCMessage message, Object error);

    /**
     * @param message
     * @param string
     */
    public void deliveryUncertain(IMCMessage message, Object msg);	
}
