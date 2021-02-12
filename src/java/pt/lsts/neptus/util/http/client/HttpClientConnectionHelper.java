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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * Jun 22, 2013
 */
package pt.lsts.neptus.util.http.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import pt.lsts.neptus.comm.proxy.ProxyInfoProvider;

/**
 * This helper class encapsulates the use of Apache's HttpComponents.
 * It uses a {@link CloseableHttpClient} and {@link PoolingHttpClientConnectionManager}.
 * Additionally it allows the use of proxy connection with the call of {@link ProxyInfoProvider}.
 * 
 * To start the comms. call {@link #initializeComm()} and at the end {@link #cleanUp()} to close the comms.
 * 
 * @author pdias
 *
 */
public class HttpClientConnectionHelper {

    public static final int MAX_TOTAL_CONNECTIONS = 4;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 50;
    public static final int CONNECTION_TIMEOUT = 5000;
    
    private CloseableHttpClient client; // Usar v4.3 HttpClientBuilder para criar CloseableHttpClient WAS DefaultHttpClient
    private PoolingHttpClientConnectionManager httpConnectionManager; // Usar v4.3  PoolingHttpClientConnectionManager WAS PoolingClientConnectionManager
    // private HashSet<HttpRequestBase> listActiveHttpMethods = new HashSet<HttpRequestBase>();

    private int maxTotalConnections = MAX_TOTAL_CONNECTIONS;
    private int defaultMaxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;
    private int connectionTimeout = CONNECTION_TIMEOUT;
    private boolean initializeProxyRoutePlanner = true;
    private String userAgent = null;
    
    public HttpClientConnectionHelper() {
    }

    public HttpClientConnectionHelper(String userAgent) {
        this.userAgent = userAgent;
    }

    public HttpClientConnectionHelper(int maxTotalConnections, int defaultMaxConnectionsPerRoute,
            int connectionTimeout, boolean initializeProxyRoutePlanner, String userAgent) {
        this(maxTotalConnections, defaultMaxConnectionsPerRoute, connectionTimeout, initializeProxyRoutePlanner);
        this.userAgent = userAgent;
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
//        SchemeRegistry schemeRegistry = new SchemeRegistry(); //(4.3) use Registry
//        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
//        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
//        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
//        httpConnectionManager.setMaxTotal(maxTotalConnections);
//        httpConnectionManager.setDefaultMaxPerRoute(defaultMaxConnectionsPerRoute);
//
//        HttpParams params = new BasicHttpParams(); //(4.3) use configuration classes provided 'org.apache.http.config' and 'org.apache.http.client.config'
//        HttpConnectionParams.setConnectionTimeout(params, connectionTimeout); // (4.3) use configuration classes provided 'org.apache.http.config' and 'org.apache.http.client.config'
//        client = new DefaultHttpClient(httpConnectionManager, params);
//
//        if (initializeProxyRoutePlanner)
//            ProxyInfoProvider.setRoutePlanner(client);

        HttpClientBuilder clientBuilder = HttpClients.custom();

        // FIXME
//        SchemeRegistry schemeRegistry = new SchemeRegistry(); //(4.3) use Registry
//        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
//        schemeRegistry.register(new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
        httpConnectionManager = new PoolingHttpClientConnectionManager(); //schemeRegistry
        httpConnectionManager.setMaxTotal(maxTotalConnections);
        httpConnectionManager.setDefaultMaxPerRoute(defaultMaxConnectionsPerRoute);
        clientBuilder.setConnectionManager(httpConnectionManager);
        
//        HttpParams params = new BasicHttpParams(); //(4.3) use configuration classes provided 'org.apache.http.config' and 'org.apache.http.client.config'
//        HttpConnectionParams.setConnectionTimeout(params, connectionTimeout); // (4.3) use configuration classes provided 'org.apache.http.config' and 'org.apache.http.client.config'
        RequestConfig rqstCfg = RequestConfig.custom().setConnectTimeout(connectionTimeout).build();
        clientBuilder.setDefaultRequestConfig(rqstCfg);

        if (initializeProxyRoutePlanner)
            ProxyInfoProvider.setRoutePlanner(clientBuilder); // client

        if (userAgent != null && !userAgent.isEmpty())
            clientBuilder.setUserAgent(userAgent);
        
        client = clientBuilder.build();
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
    public CloseableHttpClient getClient() {
        return client;
    }
    
    /**
     * @return the httpConnectionManager
     */
    public PoolingHttpClientConnectionManager getHttpConnectionManager() {
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
    public void autenticateProxyIfNeeded(HttpResponse iGetResultCode, HttpClientContext localContext) {
        if (localContext == null)
            localContext = HttpClientContext.create();
        
        ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
    }
}
