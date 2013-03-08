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
 * $Id:: WebServer.java 9616 2012-12-30 23:23:22Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.ws;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.logdb.SQLiteSerialization;

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
				System.out.println("Running server stopped");
			}
			catch (Exception e) {
				e.printStackTrace();				
			}
		}
		else {
			System.out.println("No server is running");
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
