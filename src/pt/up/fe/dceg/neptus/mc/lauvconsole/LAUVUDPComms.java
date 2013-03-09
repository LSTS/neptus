/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.mc.lauvconsole;

import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.messages.Message;


public class LAUVUDPComms {

	private LinkedHashMap<Integer, Vector<LAUVMessageListener>> messageListeners = new LinkedHashMap<Integer, Vector<LAUVMessageListener>>();

	public void sendMessage(Message message) {
		disseminate(message);
	}
	
	private void disseminate(Message message) {
		Vector<LAUVMessageListener> listeners = messageListeners.get(message.serialId());
		if (listeners != null) {
			for (LAUVMessageListener listener : listeners)
				listener.messageReceived(message);
		}
	}	
	
	public void addMessageListener(LAUVMessageListener listener, int messageID) {
		Vector<LAUVMessageListener> v = messageListeners.get(messageID);
		if (v == null)
			v = new Vector<LAUVMessageListener>();
		
		if (!v.contains(listener))
			v.add(listener);
	}
	
	public void removeMessageListener(LAUVMessageListener listener) {
		for (Integer i : messageListeners.keySet()) {
			if (messageListeners.get(i).contains(listener)) {
				messageListeners.get(i).remove(listener);
				if (messageListeners.get(i).isEmpty())
					messageListeners.remove(i);
			}
		}
	}
	
}
