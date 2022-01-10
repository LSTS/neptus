/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/10/23
 */
package pt.lsts.neptus.comm.manager.imc;

import pt.lsts.imc.IMCMessage;

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
