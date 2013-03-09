/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/27
 */
package pt.up.fe.dceg.neptus.console.plugins;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import pt.up.fe.dceg.neptus.util.comm.CommUtil;

/**
 * @author ZP
 *
 */
public class SerialCommMonitor {

	public static void main(String[] args) {
		for (CommPortIdentifier cid : CommUtil.enumerateComPorts()) {
			if (cid.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				try {
					SerialPort serial = (SerialPort)cid.open("test", 2000);
					serial.getOutputStream().write("at\n\r".getBytes());
					BufferedReader reader = new BufferedReader(new InputStreamReader(serial.getInputStream()));
					System.out.println(reader.readLine());
					serial.close();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println(cid.getName());
			}
		}
	}

}
