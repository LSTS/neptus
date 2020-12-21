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
 * Author: Paulo Dias
 * 8/10/2011
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionPoolTimeoutException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;


/**
 * @author pdias
 *
 */
public abstract class TileHttpFetcher extends Tile {

    private static final long serialVersionUID = 536559879996297467L;
    private static final long TIME_MILLIS_TO_RETIRE_THREAD_WORKER = 10000;

    protected static String tileClassId = TileHttpFetcher.class.getSimpleName();
    
    protected static Random rnd = new Random(System.currentTimeMillis());
    
    private static final int MAX_LEVEL_OF_DETAIL = 18;
    private static final int MAX_RETRIES = 4;

    protected static final long MAX_WAIT_TIME_MILLIS = 30000;
    
    private static boolean isInStateForbidden = false;
    
    protected int retries = 0;

    protected static HttpClientConnectionHelper httpComm;
//    protected static DefaultHttpClient client;
//    protected static PoolingClientConnectionManager httpConnectionManager; // was ThreadSafeClientConnManager

//    {
//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(
//                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
//        schemeRegistry.register(
//                new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
//        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
//        httpConnectionManager.setMaxTotal(50);
//        httpConnectionManager.setDefaultMaxPerRoute(10);
//
//        HttpParams params = new BasicHttpParams();
//        HttpConnectionParams.setConnectionTimeout(params, 30000);
//        
////        HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1092.0 Safari/536.6");
//        
//        client = new DefaultHttpClient(httpConnectionManager, params);
//        
//        ProxyInfoProvider.setRoutePlanner((AbstractHttpClient) client);
//
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//            @Override
//            public void run() {
//                client.getConnectionManager().shutdown();
//                httpConnectionManager.shutdown();
//            }
//        });
//    }

    public TileHttpFetcher(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
        
        initialize();
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileHttpFetcher(String id) throws Exception {
        super(id);
        initialize();
    }

    private final synchronized void initialize() {
        if (httpComm != null)
            return;
        
        httpComm = new HttpClientConnectionHelper("NMaps v1");
        httpComm.initializeComm();

//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(
//                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
//        schemeRegistry.register(
//                new Scheme("https", 443, PlainSocketFactory.getSocketFactory()));
//        httpConnectionManager = new PoolingClientConnectionManager(schemeRegistry);
//        httpConnectionManager.setMaxTotal(50);
//        httpConnectionManager.setDefaultMaxPerRoute(10);
//
//        HttpParams params = new BasicHttpParams();
//        HttpConnectionParams.setConnectionTimeout(params, 30000);
//
//        //            HttpProtocolParams.setUserAgent(params, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1092.0 Safari/536.6");
//
//        client = new DefaultHttpClient(httpConnectionManager, params);
//
//        ProxyInfoProvider.setRoutePlanner((AbstractHttpClient) client);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
//                client.getConnectionManager().shutdown();
//                httpConnectionManager.shutdown();
                httpComm.cleanUp();
            }
        });
    }
    
    // "Overrided" but is static
    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#retryLoadingTile()
     */
    @Override
    public void retryLoadingTile() {
        retries = 0;
        super.retryLoadingTile();
    }
    
//    /**
//     *  [0.0, 1.0]
//     * @return
//     */
//    protected float getTransparencyToApplyToImage() {
//        return 0.4f;
//    }
    
    /**
     * @return the URL to use to fetch the image. 
     */
    protected abstract String createTileRequestURL();
    
    /**
     * This works with {@link #getWaitTimeLock()} in order to separate the connections.
     * @return 0
     */
    protected long getWaitTimeMillisToSeparateConnections() {
        return 0;
    }
    
    /**
     * This returns null. If you want to have one tile downloaded at a time and with 
     * random separation between them (e.g. for Google Maps) overwrite this method and
     * return a {@link ReentrantLock}. 
     */
    protected ReentrantLock getWaitTimeLock() {
        return null;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.WorldRenderPainter.Tile#createTileImage()
     */
    @Override
    protected void createTileImage() {
        if (getState() == TileState.DISPOSING || getState() == TileState.FATAL_ERROR)
            return;
        setState(TileState.LOADING);
        if (useThreadPool) {
            createTileImageAlt();
            return;
        }
        Thread t = new Thread(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "] ::LOD"
                + levelOfDetail + ":: " + "createTileImage") {
            @Override
            public void run() {
                String urlGet = createTileRequestURL();
                HttpGet get = new HttpGet(urlGet);

                // Mostly for GoogleMaps wait time between connections
                long waitTime = getWaitTimeMillisToSeparateConnections();
                if (waitTime > MAX_WAIT_TIME_MILLIS)
                    waitTime = MAX_WAIT_TIME_MILLIS;
                
                try {
                    HttpResponse resp;
                    double sleepT = 2000 + 15000 * rnd.nextDouble();
                    if (isInStateForbidden && retries == 0)
                        sleepT = 60000 + 30000 * rnd.nextDouble();
//                    NeptusLog.pub().info("<###> "+sleepT + "ms");
                    try { Thread.sleep((long) (sleepT)); } catch (Exception e) { }
                    if (TileHttpFetcher.this.getState() == TileState.DISPOSING
                            || TileHttpFetcher.this.getState() == TileState.FATAL_ERROR)
                        return;

//                    NeptusLog.pub().info("<###>Fetching " + id + ": " + urlGet + "    "  
//                            + DateTimeUtil.timeFormater.format(new Date(System.currentTimeMillis())) 
//                            + "  sleep:"  + sleepT + "ms  "+ retries 
//                            + " lastForbidden@ " + DateTimeUtil.timeFormater.format(new Date(lastForbiddenTimeMillis)));
                    
                    // Mostly for GoogleMaps wait time between connections
                    if (waitTime > 0) {
                        if (getWaitTimeLock() != null)
                            getWaitTimeLock().lock();
                    }
                    
                    HttpClientContext localContext = HttpClientContext.create();
                    resp = httpComm.getClient().execute(get, localContext);
//                    ProxyInfoProvider.authenticateConnectionIfNeeded(resp, localContext, client);
                    httpComm.autenticateProxyIfNeeded(resp, localContext);
                    
//                    NeptusLog.pub().info("<###> "+resp.getStatusLine().getStatusCode());
                    if (TileHttpFetcher.this.getState() != TileState.DISPOSING
                            && retries < MAX_RETRIES
                            && resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                        isInStateForbidden = true;                        

                        retries++;
                        sleepT = retries * 60000 + 30000 * rnd.nextDouble();
//                        NeptusLog.pub().info("<###>Retrying " + id + ": " + urlGet + "    "  
//                                + DateTimeUtil.timeFormater.format(new Date(System.currentTimeMillis())) 
//                                + "  in:"  + sleepT + "ms  "+ retries + "retries.");
                        try { Thread.sleep((long) (sleepT)); } catch (Exception e) { }
                        if (TileHttpFetcher.this.getState() == TileState.DISPOSING
                                || TileHttpFetcher.this.getState() == TileState.FATAL_ERROR)
                            return;

                        setState(TileState.RETRYING);
                        get.abort();
                        createTileImage();
                        return;
                    }
                    else if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                        lasErrorMessage = "Not able to fetch "
                                + this.getClass().getSimpleName() + " Image" + (retries < MAX_RETRIES?", retrying":"") + ": "
                                + resp.getStatusLine().getStatusCode();
                        NeptusLog.pub().error(lasErrorMessage);
                        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST)
                            setState(TileState.FATAL_ERROR);
                        else
                            setState(TileState.ERROR);
                        return;
                    }
                    retries = 0;
                    isInStateForbidden = false;
                    
                    InputStream is = resp.getEntity().getContent();
                    ImageInputStream iis = ImageIO.createImageInputStream(is);
                    
                    BufferedImage cache = ImageIO.read(iis);
//                    if (getTransparencyToApplyToImage() >= 0 && getTransparencyToApplyToImage() < 1 )
//                        cache = (BufferedImage) GuiUtils.applyTransparency(cache, getTransparencyToApplyToImage());
                    image = cache;
                    
                    setState(TileState.LOADED);
                    Thread tsave = new Thread(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "] :: Tile Saver") {
                        public void run() {
                            saveTile();
                        };
                    };
                    tsave.setDaemon(false);
                    tsave.start();
                    tsave.join();
                }
                catch (IllegalStateException e) {
//                  NeptusLog.pub().info("<###> "+e);
                    lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName()
                            + " Image, " + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                    setState(TileState.ERROR);
                }
                catch (ConnectionPoolTimeoutException e) {
//                    NeptusLog.pub().info("<###> "+e);
                    lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName()
                            + " Image, " + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                    setState(TileState.ERROR);
                }
                catch (UnknownHostException e) {
//                    double sleepT = 30000 + 10000 * rnd.nextDouble();
//                    NeptusLog.pub().info("<###>UnknownHostException Retrying " + id + ": " + urlGet + "    "  
//                            + DateTimeUtil.timeFormater.format(new Date(System.currentTimeMillis())) 
//                            + "  in:"  + sleepT + "ms  "+ retries + "retries.");
//                    try { Thread.sleep((long) (sleepT)); } catch (Exception e1) { }
//                    setState(TileState.RETRYING);
//                    get.abort();
//                    createTileImage();
                    lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName()
                            + " Image, " + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                    setState(TileState.ERROR);
                }
                catch (Exception e) {
                    lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName()
                            + " Image" + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                    if (retries > MAX_RETRIES)
                        NeptusLog.pub().info("<###> "+lasErrorMessage + " :: " + e);
                    setState(TileState.ERROR);
                }
                finally {
                    get.abort();
                    
                    // Mostly for GoogleMaps wait time between connections
                    if (waitTime > 0) {
                        if (getWaitTimeLock() != null) {
                            try { Thread.sleep(waitTime); } catch (InterruptedException e) { }
                            getWaitTimeLock().unlock();
                        }
                    }

                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private void createTileImageAlt() {
        final HttpFetcherWorker tf = getLoadingRunnable();
        if (workingThreadCounter.incrementAndGet() <= MAX_WORKING_THREADS) {
            Thread t = new Thread(tf.getIdStr()) {
                private long lastJobTimeMillis = -1;
                private long timeoutLastJobMillis = TIME_MILLIS_TO_RETIRE_THREAD_WORKER;
                
                @Override
                public void run() {
                    lastJobTimeMillis = System.currentTimeMillis();
                    tf.run();
                    if (tf.getTileLoadingState() != LoadHttpTileState.END) {
                        httpFetcherWorkerList.offer(tf);
                    }
                    while (System.currentTimeMillis() - lastJobTimeMillis < timeoutLastJobMillis) {
                        HttpFetcherWorker toFetch = null;
                        try {
                            toFetch = httpFetcherWorkerList.poll(1000, TimeUnit.MILLISECONDS);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (toFetch != null) {
                            lastJobTimeMillis = System.currentTimeMillis();
                            if (toFetch.getTimeToRun() < 0 || toFetch.getTimeToRun() - System.currentTimeMillis() < 0) {
//                                NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + toFetch.getIdStr());
                                setName(toFetch.getIdStr());
                                toFetch.run();
                                if (toFetch.getTileLoadingState() != LoadHttpTileState.END) {
                                    httpFetcherWorkerList.offer(toFetch);
                                }
                            }
                            else {
                                httpFetcherWorkerList.offer(toFetch);
                            }
                        }
                        setName(TileHttpFetcher.this.getClass().getSimpleName() + " [" + Integer.toHexString(TileHttpFetcher.this.hashCode()) + "] :: " + "createTileImage");
                        try { Thread.sleep(toFetch != null ? 10 : 3000); } catch (InterruptedException e) { e.printStackTrace(); }
                    }
                    workingThreadCounter.decrementAndGet();
                }
            };
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            t.start();
        }
        else {
            workingThreadCounter.decrementAndGet();
            httpFetcherWorkerList.offer(tf);
        }
    }

    // ---------------  Less threads for loading tiles code (test) ------------------------
    private static final boolean DEBUG = false;
    private static boolean useThreadPool = true;
    private static enum LoadHttpTileState {
        START, FETCH_HTTP, WAIT_FORBIDDEN, RETRY_FORBIDDEN, WAIT_BETWEEN_CONNECTIONS_END, WAIT_BETWEEN_CONNECTIONS_RETRY, DECIDE_TO_LOCK_OR_CONTINUE, GET_LOCK, END
    };

    private static AtomicLong workingThreadCounter = new AtomicLong(0);
    private static final int MAX_WORKING_THREADS = 10;
    private static LinkedBlockingQueue<HttpFetcherWorker> httpFetcherWorkerList = new LinkedBlockingQueue<HttpFetcherWorker>();

    private HttpFetcherWorker getLoadingRunnable() {
        return new HttpFetcherWorker(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "] ::LOD"
                + levelOfDetail + ":: " + tileX + "x" + tileY + " ::" + "createTileImage");
    }

    private class HttpFetcherWorker implements Runnable {

        private String idStr = "?";
        
        private LoadHttpTileState tileLoadingState = LoadHttpTileState.START;
        private long timeToRun = -1;

        /**
         * 
         */
        public HttpFetcherWorker(String id) {
            this.idStr = id;
        }
        
        /**
         * @return the idStr
         */
        public String getIdStr() {
            return idStr  + " :: " + tileLoadingState;
        }
        
        /**
         * @return the tileLoadingState
         */
        public LoadHttpTileState getTileLoadingState() {
            return tileLoadingState;
        }
        
        /**
         * @return the timeToRun
         */
        public long getTimeToRun() {
            return timeToRun;
        }
        
        @Override
        public void run() {
            switch (tileLoadingState) {
                case START: 
                    {
                        double sleepT = 2000 + 15000 * rnd.nextDouble();
                        if (isInStateForbidden && retries == 0)
                            sleepT = 60000 + 30000 * rnd.nextDouble();
                        // NeptusLog.pub().info("<###> "+sleepT + "ms");
    
                        if (sleepT == 0) {
                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.DECIDE_TO_LOCK_OR_CONTINUE;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        else {
                            timeToRun = System.currentTimeMillis() + (long) (sleepT);
                            tileLoadingState = LoadHttpTileState.WAIT_FORBIDDEN;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + "   " + (timeToRun - System.currentTimeMillis()));System.out.flush(); }
                        }
                    }
                    break;
                case WAIT_FORBIDDEN:
                case RETRY_FORBIDDEN:
                case WAIT_BETWEEN_CONNECTIONS_RETRY:
                case WAIT_BETWEEN_CONNECTIONS_END: 
                    {
                        // These calls came from "time sleeps"
                        if (TileHttpFetcher.this.getState() == TileState.DISPOSING
                                || TileHttpFetcher.this.getState() == TileState.FATAL_ERROR) {
                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                            return;
                        }
    
                        timeToRun = -1;
                        if (tileLoadingState == LoadHttpTileState.WAIT_FORBIDDEN) {
                            tileLoadingState = LoadHttpTileState.DECIDE_TO_LOCK_OR_CONTINUE;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        else if (tileLoadingState == LoadHttpTileState.RETRY_FORBIDDEN) {
                            setState(TileState.RETRYING);
                            tileLoadingState = LoadHttpTileState.START;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        else { // WAIT_BETWEEN_CONNECTIONS_RETRY || WAIT_BETWEEN_CONNECTIONS_END
                            if (getWaitTimeLock().isLocked() && getWaitTimeLock().isHeldByCurrentThread()) {
                                getWaitTimeLock().unlock();
                                timeToRun = -1;
                                tileLoadingState = (tileLoadingState == LoadHttpTileState.WAIT_BETWEEN_CONNECTIONS_END ? LoadHttpTileState.END :
                                    LoadHttpTileState.RETRY_FORBIDDEN);
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + " unlock");System.out.flush(); }
                            }
                            else {
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + " try unlock");System.out.flush(); }
                            }
                        }
                    }
                    break;
                case DECIDE_TO_LOCK_OR_CONTINUE:
                    {
                        // Mostly for GoogleMaps wait time between connections
                        long waitTime = getWaitTimeMillisToSeparateConnections();
                        if (waitTime > 0 && getWaitTimeLock() != null) {
                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.GET_LOCK;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + " try lock");System.out.flush(); }
                        }
                        else {
                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.FETCH_HTTP;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                    }
                    break;
                case GET_LOCK:
                    {
                        if (TileHttpFetcher.this.getState() == TileState.DISPOSING) {
                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + " disposing");System.out.flush(); }
                        }
                        else {
                            try {
                                if (getWaitTimeLock().tryLock(1000, TimeUnit.MILLISECONDS)) {
                                    timeToRun = -1;
                                    tileLoadingState = LoadHttpTileState.FETCH_HTTP;
                                    if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  lock" + getIdStr());System.out.flush(); }
                                }
                                else {
                                    timeToRun = -1;
                                    tileLoadingState = LoadHttpTileState.GET_LOCK;
                                    if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + " try lock");System.out.flush(); }
                                }
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                                TileHttpFetcher.this.setState(TileState.FATAL_ERROR);
                                timeToRun = -1;
                                tileLoadingState = LoadHttpTileState.END;
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                            }
                        }
                    }
                    break;
                case FETCH_HTTP: 
                    {
                        String urlGet = createTileRequestURL();
                        HttpGet get = new HttpGet(urlGet);
                        get.setHeader("User-Agent", "WorldMap (Java/" + System.getProperty("java.version") + ")");

                        try {
                            HttpResponse resp;

//                            // Mostly for GoogleMaps wait time between connections
//                            long waitTime = getWaitTimeMillisToSeparateConnections();
//                            // waitTime = MAX_WAIT_TIME_MILLIS;
//                            if (waitTime > 0) {
//                                if (getWaitTimeLock() != null)
//                                    getWaitTimeLock().lock();
//                            }

                            HttpClientContext localContext = new HttpClientContext();

                            resp = httpComm.getClient().execute(get, localContext);

//                            ProxyInfoProvider.authenticateConnectionIfNeeded(resp, localContext, client);
                            httpComm.autenticateProxyIfNeeded(resp, localContext);

                            // NeptusLog.pub().info("<###> "+resp.getStatusLine().getStatusCode());
                            if (TileHttpFetcher.this.getState() != TileState.DISPOSING && retries < MAX_RETRIES
                                    && resp.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                                isInStateForbidden = true;

                                retries++;
                                double sleepT = retries * 60000 + 30000 * rnd.nextDouble();
                                // NeptusLog.pub().info("<###>Retrying " + id + ": " + urlGet + "    "
                                // + DateTimeUtil.timeFormater.format(new Date(System.currentTimeMillis()))
                                // + "  in:" + sleepT + "ms  "+ retries + "retries.");

                                timeToRun = System.currentTimeMillis() + (long) (sleepT);
                                tileLoadingState = LoadHttpTileState.RETRY_FORBIDDEN;
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + "   " + (timeToRun - System.currentTimeMillis()));System.out.flush(); }
                                return;
                            }
                            else if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                                lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image"
                                        + (retries < MAX_RETRIES ? ", retrying" : "") + ": "
                                        + resp.getStatusLine().getStatusCode();
                                NeptusLog.pub().error(lasErrorMessage);
                                setState(TileState.FATAL_ERROR);

                                timeToRun = -1;
                                tileLoadingState = LoadHttpTileState.END;
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                                return;                                
                            }
                            else if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                                lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image"
                                        + (retries < MAX_RETRIES ? ", retrying" : "") + ": "
                                        + resp.getStatusLine().getStatusCode();
                                NeptusLog.pub().error(lasErrorMessage);
                                if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST)
                                    setState(TileState.FATAL_ERROR);
                                else
                                    setState(TileState.ERROR);

                                timeToRun = -1;
                                tileLoadingState = LoadHttpTileState.END;
                                if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                                return;
                            }

                            retries = 0;
                            isInStateForbidden = false;

                            InputStream is = resp.getEntity().getContent();
                            ImageInputStream iis = ImageIO.createImageInputStream(is);

                            BufferedImage cache = ImageIO.read(iis);
                            // if (getTransparencyToApplyToImage() >= 0 && getTransparencyToApplyToImage() < 1 )
                            // cache = (BufferedImage) GuiUtils.applyTransparency(cache, getTransparencyToApplyToImage());
                            image = cache;

                            setState(TileState.LOADED);
                            Thread tsave = new Thread(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "] :: Tile Saver") {
                                public void run() {
                                    saveTile();
                                };
                            };
                            tsave.setDaemon(false);
                            tsave.start();
                            tsave.join();

                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        catch (IllegalStateException e) {
                            // NeptusLog.pub().info("<###> "+e);
                            lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image, "
                                    + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                            setState(TileState.ERROR);

                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        catch (ConnectionPoolTimeoutException e) {
                            // NeptusLog.pub().info("<###> "+e);
                            lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image, "
                                    + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                            setState(TileState.ERROR);

                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        catch (UnknownHostException e) {
                            // double sleepT = 30000 + 10000 * rnd.nextDouble();
                            // NeptusLog.pub().info("<###>UnknownHostException Retrying " + id + ": " + urlGet + "    "
                            // + DateTimeUtil.timeFormater.format(new Date(System.currentTimeMillis()))
                            // + "  in:" + sleepT + "ms  "+ retries + "retries.");
                            // try { Thread.sleep((long) (sleepT)); } catch (Exception e1) { }
                            // setState(TileState.RETRYING);
                            // get.abort();
                            // createTileImage();
                            lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image, "
                                    + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                            setState(TileState.ERROR);

                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        catch (Exception e) {
                            lasErrorMessage = "Not able to fetch " + this.getClass().getSimpleName() + " Image"
                                    + (retries <= MAX_RETRIES ? ", retrying" : "") + ": " + e;
                            if (retries > MAX_RETRIES)
                                NeptusLog.pub().info("<###> "+lasErrorMessage + " :: " + e);
                            setState(TileState.ERROR);

                            timeToRun = -1;
                            tileLoadingState = LoadHttpTileState.END;
                            if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr());System.out.flush(); }
                        }
                        finally {
                            get.abort();

                            // Mostly for GoogleMaps wait time between connections
                            long waitTime = getWaitTimeMillisToSeparateConnections();
                            if (waitTime > MAX_WAIT_TIME_MILLIS)
                                waitTime = MAX_WAIT_TIME_MILLIS;
                            if (waitTime > 0) {
                                if (getWaitTimeLock() != null) {
                                    // try { Thread.sleep(waitTime); } catch (InterruptedException e) { }
                                    // getWaitTimeLock().unlock();
                                    timeToRun = System.currentTimeMillis() + waitTime;
                                    tileLoadingState = (tileLoadingState == LoadHttpTileState.END ? LoadHttpTileState.WAIT_BETWEEN_CONNECTIONS_END :
                                        LoadHttpTileState.WAIT_BETWEEN_CONNECTIONS_RETRY);
                                    if (DEBUG) { NeptusLog.pub().info("<###> "+httpFetcherWorkerList.size() + "  " + getIdStr() + "   " + (timeToRun - System.currentTimeMillis()));System.out.flush(); }
                                }
                            }
                        }
                    }
                    break;
                    default:
                        break;
            }
        }
    };
    // -------------------------- End -----------------------------------------------------

    public static boolean isFetchableOrGenerated() {
        return true;
    }
    
    /**
     * @return the isInStateForbidden
     */
    protected static boolean isInStateForbidden() {
        return isInStateForbidden;
    }
}
