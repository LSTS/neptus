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
 */
package pt.up.fe.dceg.neptus.junit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

public class CommunicationsTest extends TestCase {

	
	public static void main(String[] args) {
		
		ConfigFetch.initialize();

		IMCDefinition imcDef = IMCDefinition.getInstance();
		Collection<String> messages = imcDef.getMessageNames();

		
		LinkedList<byte[]> msgs = new LinkedList<byte[]>();
		
		for (String msg : messages) {
			System.out.print("Testing serialization of message '"+msg+"'...");
			IMCMessage m = imcDef.create(msg);

			try {
			    ByteArrayOutputStream baos = new ByteArrayOutputStream();
			    IMCOutputStream imcos = new IMCOutputStream(baos);
				int size = m.serialize(imcos);
				byte[] ser = baos.toByteArray();				
				msgs.add(ser);
				//FIXME Zé podes ver como fazer isto?? (pdias 20111019)
                assertEquals(m.getHeader().getPayloadSize() + m.getPayloadSize() + 2/* footer */,
                        size);
				System.out.println("OK (size is "+size+")");
			}
			catch (Exception e) {
				e.printStackTrace();
				assertEquals(false, true);
			}
		}
		
		System.out.println("\n * SERIALIZED ALL MESSAGES... NOW GOING FOR DESEREALIZATION * \n");
		
		for (byte[] buffer : msgs) {
			System.out.print("Testing deserialization of message...");
	        IMCMessage m;
			try {
				m = imcDef.unserialize(new ByteArrayInputStream(buffer));
                System.out.println("OK (type is " + m.getMessageType().getShortName() + ")");
			}
			catch (Exception e) {
				e.printStackTrace();
				assertEquals(false, true);
			}
		}
	}	
}
