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
 * May 24, 2010
 * $Id:: VehicleStateProvider.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author zp
 *
 */
public class VehicleStateProvider implements MessageListener<MessageInfo, IMCMessage> {

	public static long minimumTimeBetweenCalculationsMillis = 100;

	private static LinkedHashMap<String, IMCMessage> estimatedStates = new LinkedHashMap<String, IMCMessage>();
	
	private VehicleStateProvider() {		
		ImcMsgManager.getManager().addListener(this);		
	}
	
	private static VehicleStateProvider instance = null;
	
	public static void init() {
		if (instance == null)
			instance = new VehicleStateProvider();		
	}
	
	@Override
	public void onMessage(MessageInfo info, IMCMessage msg) {
		String src_id = msg.getHeaderValue("src").toString();
		estimatedStates.put(src_id, msg);
	}
}
