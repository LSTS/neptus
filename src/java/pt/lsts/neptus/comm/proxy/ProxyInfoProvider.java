/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 10 de Mai de 2012
 */
package pt.lsts.neptus.comm.proxy;

import java.awt.Window;
import java.io.File;
import java.net.InetAddress;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.ssh.SSHConnectionDialog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author pdias
 * 
 * FIXME 
 * pddias: Fix deprecated hc4.3 upgrade-
 */
public class ProxyInfoProvider {

    @NeptusProperty
    public static boolean enableProxy = false;
    @NeptusProperty
    public static String httpProxyHost = "localhost";
    @NeptusProperty
    public static short httpProxyPort = 8080;

    @NeptusProperty
    public static String username = "user";
    private static String password = null;

    private static final String ROOT_PREFIX;
    static {
        if (new File("../" + "conf").exists())
            ROOT_PREFIX = "../";
        else {
            ROOT_PREFIX = "";
            new File("conf").mkdir();
        }
    }

    static {
        try {
            String confFx = ROOT_PREFIX + "conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, ProxyInfoProvider.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase()
                    + ".properties\" : " + e.getMessage());
        }
    }

    private ProxyInfoProvider() {
        // Don't allow initialization
    }

    private static synchronized void savePropertiesToDisk() {
        try {
            PluginUtils.saveProperties(ROOT_PREFIX + "conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase() + ".properties",
                    ProxyInfoProvider.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/" + ProxyInfoProvider.class.getSimpleName().toLowerCase()
                    + ".properties\" : " + e.getMessage());
        }
    }

    /**
     * @return the enableProxy
     */
    public static boolean isEnableProxy() {
        return enableProxy;
    }

    /**
     * @param enableProxy the enableProxy to set
     */
    public static void setEnableProxy(boolean enableProxy) {
        ProxyInfoProvider.enableProxy = enableProxy;
        savePropertiesToDisk();
    }

    /**
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] showConfigurations() {
        return showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), true, null);
    }

    /**
     * @param parentWindow
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] showConfigurations(Window parentWindow) {
        return showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), true, parentWindow);
    }

    /**
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] getConfiguratons() {
        return enableProxy ? showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), false, null)
                : new String[0];
    }

    /**
     * @param parentWindow
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    public static String[] getConfiguratons(Window parentWindow) {
        return enableProxy ? showOrNotConfiguratonDialogAndReturnConfigurationWorker(I18n.text("Proxy Configuration"), false,
                parentWindow) : new String[0];
    }

    /**
     * @param title
     * @return [httpProxyHost, username, password, httpProxyPort]
     */
    private synchronized static String[] showOrNotConfiguratonDialogAndReturnConfigurationWorker(String title,
            boolean forceShow, Window parentWindow) {

        if (forceShow || password == null) {
            String[] ret = SSHConnectionDialog.showConnectionDialog(httpProxyHost, username, password == null ? ""
                    : password, httpProxyPort, title, parentWindow);
            if (ret.length == 0)
                return new String[0];

            httpProxyHost = ret[0];
            username = ret[1];
            password = ret[2];
            try {
                httpProxyPort = Short.parseShort(ret[3]);
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().debug(e.getMessage());
                httpProxyPort = (short) 80;
            }

            savePropertiesToDisk();
        }

        return new String[] { httpProxyHost, username, password, Short.toString(httpProxyPort) };
    }

    /**
     * @param client to add a route planner
     */
    public static void setRoutePlanner(final HttpClientBuilder client) {
        DefaultRoutePlanner drp = new DefaultRoutePlanner(null);
        client.setRoutePlanner(new HttpRoutePlanner() {
            @Override
            public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context)
                    throws HttpException {
                final HttpClientContext clientContext = HttpClientContext.adapt(context);
                final RequestConfig config = clientContext.getRequestConfig();
                final InetAddress local = config.getLocalAddress();
                String[] ret = getConfiguratons();
                if (ret.length == 0) {
                    HttpRoute ht = drp.determineRoute(target, request, clientContext);
                    return ht; //new HttpRoute(target, local, target, "https".equalsIgnoreCase(target.getSchemeName()));
                }

                String proxyHost = ret[0];
                short proxyPort = Short.parseShort(ret[3]);
                String username = ret[1];
                String password = ret[2];

                // BasicCredentialsProvider or SystemDefaultCredentialsProvider
                CredentialsProvider credProv = new BasicCredentialsProvider();
                credProv.setCredentials(new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(username, password)); // client.getCredentialsProvider()
                client.setDefaultCredentialsProvider(credProv);
                
                return new HttpRoute(target, local, new HttpHost(proxyHost, proxyPort), "https".equalsIgnoreCase(target
                        .getSchemeName()));
            }
        });
    }

    public static Credentials getProxyCredentials() {
        String[] ret = getConfiguratons();
        if (ret.length == 0)
            return new UsernamePasswordCredentials("", "");

        //        String proxyHost = ret[0];
        //        short proxyPort = Short.parseShort(ret[3]);
        String username = ret[1];
        String password = ret[2];

        return new UsernamePasswordCredentials(username, password);
    }

    /**
     * @param resp
     * @param localContext
     */
    public static void authenticateConnectionIfNeeded(HttpResponse resp, HttpClientContext localContext, HttpClient client) {
        {
            if (isEnableProxy()) {
                AuthState proxyAuthState = (AuthState) localContext
                        .getAttribute(HttpClientContext.PROXY_AUTH_STATE);
                if (proxyAuthState != null) {
                    // NeptusLog.pub().info("Proxy auth state: " + proxyAuthState.getState());
                    if (proxyAuthState.getAuthScheme() != null)
                        NeptusLog.pub().info("Proxy auth scheme: " + proxyAuthState.getAuthScheme());
                    if (proxyAuthState.getCredentials() != null)
                        NeptusLog.pub().info("Proxy auth credentials: " + proxyAuthState.getCredentials());
                }
                AuthState targetAuthState = (AuthState) localContext
                        .getAttribute(HttpClientContext.TARGET_AUTH_STATE);
                if (targetAuthState != null) {
                    // NeptusLog.pub().info("Target auth state: " + targetAuthState.getState());
                    if (targetAuthState.getAuthScheme() != null)
                        NeptusLog.pub().info("Target auth scheme: " + targetAuthState.getAuthScheme());
                    if (targetAuthState.getCredentials() != null)
                        NeptusLog.pub().info("Target auth credentials: " + targetAuthState.getCredentials());
                }
            }
        }

        { // New Authentication for httpcomponents-client-4.2
            if (isEnableProxy()) {
                int sc = resp.getStatusLine().getStatusCode();

                AuthState authState = null;
                HttpHost authhost = null;
                if (sc == HttpStatus.SC_UNAUTHORIZED) {
                    // Target host authentication required
                    authState = (AuthState) localContext.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
                    authhost = (HttpHost) localContext.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                }
                if (sc == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED) {
                    // Proxy authentication required
                    authState = (AuthState) localContext.getAttribute(HttpClientContext.PROXY_AUTH_STATE);
                    authhost = (HttpHost) localContext.getAttribute(HttpCoreContext.HTTP_TARGET_HOST); // was HTTP_PROXY_HOST not sure what to use
                }
                if (authState != null) {
                    AuthScheme authscheme = authState.getAuthScheme();
                    NeptusLog.pub().info("Using proxy for " + authscheme.getRealm() + " ...");
                    Credentials creds = getProxyCredentials();
                    CredentialsProvider credsProvider = localContext.getCredentialsProvider();
                    if (credsProvider == null)
                        credsProvider = new BasicCredentialsProvider();
                    credsProvider.setCredentials(new AuthScope(authhost), creds);
                }
            }
        }
    }

    public static void main(String[] args) {
        savePropertiesToDisk();

        NeptusLog.pub().info("<###> "+enableProxy);
        NeptusLog.pub().info("<###> "+httpProxyHost);
        NeptusLog.pub().info("<###> "+httpProxyPort);
        NeptusLog.pub().info("<###> "+username);
        NeptusLog.pub().info("<###> "+password);
    }
}
