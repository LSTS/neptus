/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.ws;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.logdb.SQLiteSerialization;
import pt.lsts.imc.IMCDefinition;

public class WebServer {
	
	private static Server server;
	private static int port = 8080;
	
	public static void start(int port) {
		if (server != null)
			stop();
		
		WebServer.port = port;
		
		Thread serverThread = new Thread() {
			@Override
			public void run() {
				server = new Server(WebServer.port);
				Context root = new Context(server,"/",Context.SESSIONS);
				//root.addServlet(new ServletHolder(new NeptusStateServlet()), "/");
				root.addServlet(new ServletHolder(new RESTServlet()), "/imc/*");
				try {
					server.start();
					server.join();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		serverThread.start();
		
	}
	
	public static void stop() {
		if (server != null) {
			try {
				server.stop();
				NeptusLog.pub().info("<###>Running server stopped");
			}
			catch (Exception e) {
				e.printStackTrace();				
			}
		}
		else {
			NeptusLog.pub().info("<###>No server is running");
		}
	}
	
	public static void main(String[] args) throws Exception {
		ConfigFetch.initialize();

		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					try {						
						Thread.sleep(333);
						for (int i = 0; i < 10; i++) {
							SQLiteSerialization.getDb().store(IMCDefinition.getInstance().create("EstimatedState", "x", Math.random() * 100));
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		});
		t2.start();
		
		start(8080);
	}

	public static int getPort() {
		return port;
	}

	public static void setPort(int port) {
		WebServer.port = port;
	}
}
