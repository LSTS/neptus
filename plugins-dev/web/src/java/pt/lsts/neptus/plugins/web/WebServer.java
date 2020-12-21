/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.plugins.web;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.servlet.Servlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class WebServer {

    private static int counter = 0;
    
    private static Server server = null;//new Server(WebServer.port);
    //private static int[] ports = new int[] {8080, 8081, 8083, 8082, 8084};
    static Integer port = 8080;

    protected static LinkedHashMap<String, Servlet> installedServlets = new LinkedHashMap<String, Servlet>();

    public static Servlet addServlet(String path, Class<?> servletClass) {
        try {
            Servlet s = (Servlet) servletClass.newInstance();
            installedServlets.put(path, s);
            return s;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // restart();
    }

    public static Servlet install(Class<?> servlet) {
        String path = "/" + servlet.getSimpleName();
        NeptusServlet s = servlet.getAnnotation(NeptusServlet.class);
        if (s != null && !s.path().equals("")) {
            path = s.path();
        }

        return addServlet(path, servlet);
    }

    public static void restart() {
        NeptusLog.pub().info("<###>Restarting Neptus Web Server Request (was " + (isRunning() ? "" : "not ") + "running)...");
        start(port);
    }

    public static void start(int port) {

        // new Exception().printStackTrace();

        if (server != null) {
            stop();
            try {
                Thread.sleep(300);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        WebServer.port = port;

        Thread serverThread = new Thread(WebServer.class.getSimpleName() + " :: Start Thread :: " + counter++) {
            @Override
            public void run() {
                //synchronized (WebServer.port) {

                while (!available(WebServer.port))
                    WebServer.port++;

                server = new Server(WebServer.port);
                Context root = new Context(server, "/", Context.SESSIONS);

                registerUnregisterPath(true, "");
                for (String path : installedServlets.keySet()) {
                    root.addServlet(new ServletHolder(installedServlets.get(path)), path);
                    registerUnregisterPath(true, path);
                }
                try {
                    System.out.print("Starting Neptus Web Server...");
                    server.start();
                    NeptusLog.pub().info("<###>DONE.\nNeptus Web Server listening on port "
                            + WebServer.port + ".");
                    server.join();
                }
                catch (Exception e) { // $codepro.audit.disable logExceptions
                    NeptusLog.pub().error("Error while binding web server to port "
                            + WebServer.port);
                }
            }

        };

        serverThread.start();
    }

    public static void stop() {
        System.out.print("Stopping Neptus Web Server... ");

        //synchronized (WebServer.port) {
        if (server != null) {
            try {
                server.stop();
                registerUnregisterPath(false, "");
                for (String path : installedServlets.keySet()) {
                    registerUnregisterPath(false, path);
                }

                NeptusLog.pub().info("<###>DONE.");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            NeptusLog.pub().info("<###>NO SERVER RUNNING.");
        }
        server = null;
    }

    public static boolean isRunning() {
        if (server == null)
            return false;
        return server.isRunning();
    }
    
    public static int getPort() {
        return port;
    }

    public static void setPort(int port) {
        WebServer.port = port;
        // restart();
    }

    /**
     * Checks to see if a specific port is available.
     * @param port the port to check for availability
     * @see http://stackoverflow.com/questions/434718/sockets-discover-port-availability-using-java
     */
    public static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        }
        catch (IOException e) { // $codepro.audit.disable logExceptions
            e.printStackTrace();
        }
        finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * @param path
     */
    protected static void registerUnregisterPath(boolean registerOrUnregister, String path) {
        try {
            URL serURL = new URL("http://localhost:" + WebServer.port + 
                    (path.startsWith("/") ? "" : "/") + 
                    (path.endsWith("*") ? path.replaceAll("\\*$", "") : path));
            if (registerOrUnregister)
                ImcMsgManager.getManager().registerService(serURL);
            else
                ImcMsgManager.getManager().unRegisterService(serURL);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ConfigFetch.initialize();
        for (Class<?> c : ReflectionUtil.getClassesForPackage(WebServer.class.getPackage()
                .getName())) {
            if (c.getAnnotation(NeptusServlet.class) != null)
                install(c);
        }
        WebServer.restart();
        // WebServer.install(LogBookServlet.class);
    }
}