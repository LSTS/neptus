/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: LAUVUDPComms.java 9616 2012-12-30 23:23:22Z pdias                $:
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
