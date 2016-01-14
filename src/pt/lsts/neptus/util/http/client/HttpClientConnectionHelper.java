/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Jun 22, 2013
 */
package pt.lsts.neptus.util.http.client;

import org.apache.http.HttpResponse;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import pt.lsts.neptus.comm.proxy.ProxyInfoProvider;

/**
 * This helper class encapsulates the use of Apache's HttpComponents.
 * It uses a {@link DefaultHttpClient} and {@link PoolingClientConnectionManager}.
 * Additionally it allows the use of proxy connection with the call of {@link ProxyInfoProvider}.
 * 
 * To start the comms. call {@link #initializeComm()} and at the end {@link #cleanUp()} to close the comms.
 * 
 * @author pdias
 *
 */
@SuppressWarnings("deprecation")
public class HttpClientConnectionHelper {

    private DefaultHttpClient client;
    private PoolingClientConnectionManager httpConnectionManager; // old ThreadSafeClientConnManager
    // private HashSet<HttpRequestBase> listActiveHttpMethods = new HashSet<HttpRequestBase>();

    private int maxTotalConnections = 4;
    private int defaultMaxConnectionsPerRoute = 50;
    private int connectionTimeout = 5000;
    private boolean initializeProxyRoutePlanner = true;
    
    public HttpClientConnectionHelper() {
    }

    /**
     * @param maxTotalConnections
     * @param defaultMaxConnectionsPerRoute
     * @param connectionTimeout
     * @param initializeProxyRoutePlanner
     */
    public HttpClientConnectionHelper(int maxTotalConnections, int defaultMaxConnectionsPerRoute,
            int connectionTimeout, boolean initializeProxyRoutePlanner) {
        super();
        this.maxTotalConnections = maxTotalConnections;
        this.defaultMaxConnectionsPerRoute = defaultMaxConnectionsPerRoute;
        this.connectionTimeout = connectionTimeout;
        this.initializeProxyRoutePlanner = initializeProxyRoutePlanner;
    }

    /**
     * Call this to initialize the comms.
     */
    public void initializeComm() {
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
        httpConnectionManager.setMaxTotal(maxTotalConnections);
        httpConnectionManager.setDefaultMaxPerRoute(defaultMaxConnectionsPerRoute);

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
        client = new DefaultHttpClient(httpConnectionManager, params);

        if (initializeProxyRoutePlanner)
            ProxyInfoProvider.setRoutePlanner(client);
    }
    
    /**
     * This has to be called to cleanup.
     * After calling this no more connections are feasible.
     */
    public void cleanUp() {
        if (client != null) {
        }
        if (httpConnectionManager != null) {
            httpConnectionManager.shutdown();
        }
    }

    /**
     * @return the client
     */
    public DefaultHttpClient getClient() {
        return client;
    }
    
    /**
     * @return the httpConnectionManager
     */
    public PoolingClientConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }
    
    
    /**
     * @return the maxTotalConnections
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    /**
     * @return the defaultMaxConnectionsPerRoute
     */
    public int getDefaultMaxConnectionsPerRoute() {
        return defaultMaxConnectionsPerRoute;
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @return the initializeProxyRoutePlanner
     */
    public boolean isInitializeProxyRoutePlanner() {
        return initializeProxyRoutePlanner;
    }

    public void autenticateProxyIfNeeded(HttpResponse iGetResultCode) {
        autenticateProxyIfNeeded(iGetResultCode, null);
    }

    /**
     * Call this after calling client.execute in order to execute proxy authentication if needed.
     * @param iGetResultCode
     * @param localContext
     */
    public void autenticateProxyIfNeeded(HttpResponse iGetResultCode, HttpContext localContext) {
        if (localContext == null)
            localContext = new BasicHttpContext();
        
        ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
    }
}
