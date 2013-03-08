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
package pt.up.fe.dceg.neptus.plugins.web;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.servlet.Servlet;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
        System.out.println("Restarting Neptus Web Server Request (was " + (isRunning() ? "" : "not ") + "running)...");
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
                    System.out.println("DONE.\nNeptus Web Server listening on port "
                            + WebServer.port + ".");
                    server.join();
                }
                catch (Exception e) {
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

                System.out.println("DONE.");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("NO SERVER RUNNING.");
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
        catch (IOException e) {
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