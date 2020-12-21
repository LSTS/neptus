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
 * 6 de Jul de 2012
 */
package pt.lsts.neptus.plugins.odss;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBoxMenuItem;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.odss.track.PlatformReportType;
import pt.lsts.neptus.plugins.odss.track.PlatformReportType.PlatformType;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.NMEAUtils;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 *
 */
@SuppressWarnings({"serial"})
@PluginDescription(name = "ODSS STOQS Track Fetcher", author = "Paulo Dias", version = "0.1",
        icon="pt/lsts/neptus/plugins/odss/odss.png")
public class OdssStoqsTrackFetcher extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {

    /*
     * http://beach.mbari.org/trackingdb/position/seacon-5/last/24h/data.html
     * http://beach.mbari.org/trackingdb/positionOfType/auv/last/24h/data.html
     * http://beach.mbari.org/trackingdb/positionOfType/glider/last/24h/data.html
     */

//    private static final String POS_URL_FRAGMENT = "position/";
    private static final String POS_OF_URL_FRAGMENT = "positionOfType/";
    private static final String LAST_URL_FRAGMENT = "last/";
    private static final String AUV_URL_FRAGMENT = "auv/";
    private static final String AIS_URL_FRAGMENT = "uav/";
    private static final String GLIDER_URL_FRAGMENT = "glider/";
    private static final String DRIFTER_URL_FRAGMENT = "drifter/";
    private static final String SHIP_URL_FRAGMENT = "ship/";
    
    @NeptusProperty(name = "Web address")
    public String fetchURL = "http://odss.mbari.org/trackingdb/";

    @NeptusProperty(name = "AUV", category = "System Filter")
    public boolean fetchAUVType = true;

    @NeptusProperty(name = "Glider", category = "System Filter")
    public boolean fetchGliderType = true;

    @NeptusProperty(name = "Drifter", category = "System Filter")
    public boolean fetchDrifterType = true;

    @NeptusProperty(name = "Ship", category = "System Filter")
    public boolean fetchShipType = true;

    @NeptusProperty(name = "AIS", category = "System Filter")
    public boolean fetchAISType = true;
    
    @NeptusProperty(name = "Period to fetch (hours)")
    public int periodHoursToFetch = 3;

    @NeptusProperty(name = "Update period (ms)", description = "The period to fetch the systems' positions. "
            + "Zero means disconnected.")
    public int updatePeriodMillis = 1000;

    @NeptusProperty(name = "Publishing")
    public boolean fetchOn = false;

    public boolean publishRemoteSystemsLocally = true;

    public boolean debugOn = false;

//    private long lastFetchPosTimeMillis = System.currentTimeMillis();

    private JCheckBoxMenuItem startStopCheckItem = null;
//    private ToolbarButton sendEnableDisableButton;

    private HttpClientConnectionHelper httpComm;
    private HttpGet getHttpRequestState;

    private Timer timer = null;
    private TimerTask ttask = null;

//    private LinkedHashMap<String, Long> timeSysList = new LinkedHashMap<String, Long>();
//    private LinkedHashMap<String, CoordinateSystem> locSysList = new LinkedHashMap<String, CoordinateSystem>();
    private final HashMap<String, PlatformReportType> sysStokesLocations = new HashMap<String, PlatformReportType>();

    private DocumentBuilderFactory docBuilderFactory;
    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
    }

    /**
     * 
     */
    public OdssStoqsTrackFetcher(ConsoleLayout console) {
        super(console);
        initializeComm();
        initialize();
    }

    /**
     * 
     */
    private void initializeComm() {
        httpComm = new HttpClientConnectionHelper();
        httpComm.initializeComm();
    }

    /**
     * 
     */
    private void initialize() {
        setVisibility(false);

//        removeAll();
//        setBackground(new Color(255, 255, 110));

        timer = new Timer(OdssStoqsTrackFetcher.class.getSimpleName() + " [" + OdssStoqsTrackFetcher.this.hashCode() + "]");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    private TimerTask getTimerTask() {
        if (ttask == null) {
            ttask = new TimerTask() {
                @Override
                public void run() {
                    if (!fetchOn)
                        return;
                    getStateRemoteData();
                }
            };
        }
        return ttask;
    }

    @Override
    public void initSubPanel() {
        setVisibility(false);
        
        startStopCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
                + ">Start/Stop", null,
                new CheckMenuChangeListener() {
                    @Override
                    public void menuUnchecked(ActionEvent e) {
                        fetchOn = false;
                    }
                    
                    @Override
                    public void menuChecked(ActionEvent e) {
                        fetchOn = true;
                    }
                });

        addMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass()) + ">Settings", null,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PropertiesEditor.editProperties(OdssStoqsTrackFetcher.this,
                                getConsole(), true);
                    }
                });
        
        startStopCheckItem.setState(fetchOn);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        httpComm.cleanUp();
        
        removeCheckMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Settings");
        
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 
     */
    protected void getStateRemoteData() {
        if (getHttpRequestState != null)
            getHttpRequestState.abort();
        getHttpRequestState = null;
        
        // http://beach.mbari.org/trackingdb/positionOfType/auv/last/24h/data.html
        HashMap<String, PlatformReportType.PlatformType> typeRqstLst = new LinkedHashMap<String, PlatformReportType.PlatformType>();
        if (fetchAUVType)
            typeRqstLst.put(AUV_URL_FRAGMENT, PlatformType.AUV);
        if (fetchGliderType)
            typeRqstLst.put(GLIDER_URL_FRAGMENT, PlatformType.GLIDER);
        if (fetchDrifterType)
            typeRqstLst.put(DRIFTER_URL_FRAGMENT, PlatformType.DRIFTER);
        if (fetchShipType)
            typeRqstLst.put(SHIP_URL_FRAGMENT, PlatformType.SHIP);
        if (fetchAISType)
            typeRqstLst.put(AIS_URL_FRAGMENT, PlatformType.AIS);

        for (String typeRqst : typeRqstLst.keySet()) {
            try {
                String endpoint = fetchURL;
                String uri = endpoint + POS_OF_URL_FRAGMENT + typeRqst + LAST_URL_FRAGMENT + periodHoursToFetch + "h/data.html";
                getHttpRequestState = new HttpGet(uri);

                HttpClientContext localContext = HttpClientContext.create();
                HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequestState, localContext);
//                ProxyInfoProvider.authenticateConnectionIfNeeded(iGetResultCode, localContext, client);
                httpComm.autenticateProxyIfNeeded(iGetResultCode, localContext);
                
                if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    NeptusLog.pub().info("<###> "+OdssStoqsTrackFetcher.this.getClass().getSimpleName() 
                            + "[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                            + iGetResultCode.getStatusLine().getReasonPhrase()
                            + " code was return from the server");
                    if (getHttpRequestState != null) {
                        getHttpRequestState.abort();
                    }
                    continue;
                }
                InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
                @SuppressWarnings("unused")
                long fullSize = iGetResultCode.getEntity().getContentLength();
                if (iGetResultCode.getEntity().isChunked())
                    fullSize = iGetResultCode.getEntity().getContentLength();
                DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
                Document docProfiles = builder.parse(streamGetResponseBody);
                
                HashMap<String, PlatformReportType> sysBag = processOdssStokesResponse(docProfiles, typeRqstLst.get(typeRqst));
                filterAndAddToList(sysBag);
                if (debugOn) {
                    for (String key : sysBag.keySet()) {
                        NeptusLog.pub().info("<###> "+sysBag.get(key));
                    }
                }

                sysStokesLocations.putAll(sysBag);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            finally {
                if (getHttpRequestState != null) {
                    getHttpRequestState.abort();
                    getHttpRequestState = null;
                }
            }
        }
        
        processRemoteStates();
        
        return;
    }

    /**
     * @param sysBag
     */
    static void filterAndAddToList(HashMap<String, PlatformReportType> sysBag) {
        if (sysBag.size() == 0)
            return;
        
        String[] vehicles = VehiclesHolder.getVehiclesArray();
        ImcSystem[] imcSyss = ImcSystemsHolder.lookupAllSystems();
        String[] imcReduced = new String[vehicles.length + imcSyss.length];
        for (int i = 0; i < vehicles.length; i++) {
            imcReduced[i] = IMCUtils.reduceSystemName(vehicles[i]);
        }
        for (int i = 0; i < imcSyss.length; i++) {
            imcReduced[i + vehicles.length] = IMCUtils.reduceSystemName(imcSyss[i].getName());
        }
        List<String> lstReducedLst = Arrays.asList(imcReduced);
        for (String key : sysBag.keySet().toArray(new String[0])) {
            if (ImcSystemsHolder.lookupSystemByName(key) != null)
                continue;
            else if (lstReducedLst.contains(key)) {
                int idx = lstReducedLst.indexOf(key);
                PlatformReportType pr = sysBag.get(key);
                sysBag.remove(key);
                String newName = idx < vehicles.length ? vehicles[idx] : imcSyss[idx].getName();
                pr = pr.cloneWithName(newName);
                sysBag.put(newName, pr);
            }
        }
    }

    /**
     * @param timeSysList2
     * @param locSysList2
     * @param docProfiles
     * @return
     */
    private HashMap<String, PlatformReportType> processOdssStokesResponse(Document docProfiles, PlatformReportType.PlatformType type) {
        HashMap<String, PlatformReportType> dataBag = new HashMap<String, PlatformReportType>();
        
        Element root = docProfiles.getDocumentElement();
        NodeList elem = root.getElementsByTagName("table");
        for (int i = 0; i < elem.getLength(); i++) {
            Node tableNd = elem.item(i);
            Node tbodyNd = null;
            Node bn = tableNd.getFirstChild();
            while (bn != null) {
                if ("tbody".equalsIgnoreCase(bn.getNodeName())) {
                    tbodyNd = bn;
                    break;
                }
                else if ("tr".equalsIgnoreCase(bn.getNodeName())) {
                    tbodyNd = tableNd;
                    break;
                }
                bn = bn.getNextSibling();
            }
            if (tbodyNd != null) {
                NodeList trNdLst = ((Element) tbodyNd).getElementsByTagName("tr");
                if (trNdLst.getLength() > 0) {
                    //find header
                    boolean hasHeader = false;
                    Node trNode = trNdLst.item(0);
                    NodeList trThNdLst = ((Element) trNode).getElementsByTagName("th");
                    if (trThNdLst.getLength() > 0) {
                        hasHeader = true;
                        // Platform Name, Epoch Seconds, Longitude, Latitude, ISO-8601
                        for (int j = 0; j < trThNdLst.getLength(); j++) {
                            Element thElm = (Element) trThNdLst.item(j);
                            String txt = thElm.getTextContent().trim();
                            if (debugOn)
                                System.out.print(txt + (j == trThNdLst.getLength() - 1 ? "\n" : "\t"));
                        }
                    }
                    for (int j = hasHeader ? 1 : 0; j < trNdLst.getLength(); j++) {
                        NodeList trTdNdLst = ((Element) trNdLst.item(j)).getElementsByTagName("td");
                        if (trTdNdLst.getLength() > 0) {
                            // Platform Name, Epoch Seconds, Longitude, Latitude, ISO-8601
                            String name = null;
                            long unixTimeSeconds = -1;
                            double lat = Double.NaN, lon = Double.NaN;
                            boolean allDataIn = false;
                            try {
                                for (int k = 0; k < trTdNdLst.getLength(); k++) {
                                    Element tdElm = (Element) trTdNdLst.item(k);
                                    String txt = tdElm.getTextContent().trim();
                                    if (debugOn)
                                        System.out.print(txt + (k == trTdNdLst.getLength() - 1 ? "\n" : "\t"));
                                    switch (k) {
                                        case 0:
                                            name = txt;
                                            break;
                                        case 1:
                                            unixTimeSeconds = (long) Double.parseDouble(txt);
                                            break;
                                        case 2:
                                            lon = Double.parseDouble(txt);
                                            break;
                                        case 3:
                                            lat = Double.parseDouble(txt);
                                            allDataIn = true;
                                            break;
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (allDataIn) {
                                PlatformReportType dfe = dataBag.get(name);
                                if (dfe == null || dfe.getEpochSeconds() <= unixTimeSeconds) {
                                    PlatformReportType pr = new PlatformReportType(name, type);
                                    pr.setLocation(lat, lon, unixTimeSeconds);
                                    dataBag.put(name, pr);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (debugOn) {
            for (String key : dataBag.keySet()) {
                NeptusLog.pub().info("<###> "+dataBag.get(key));
            }
        }
        
        return dataBag;
    }

    private void processRemoteStates() {
        if (publishRemoteSystemsLocally) {
            processRemoteStates(sysStokesLocations, DateTimeUtil.HOUR * periodHoursToFetch);
        }
    }

    static void processRemoteStates(HashMap<String, PlatformReportType> sysStokesLocations, long timeMillisToDiscart) {
        for (String key : sysStokesLocations.keySet().toArray(new String[0])) {
            String id = key;
            PlatformReportType pr = sysStokesLocations.get(key);
            if (pr == null)
                continue;

            long time = Math.round(pr.getEpochSeconds() * 1000d);
            CoordinateSystem coordinateSystem = new CoordinateSystem();
            coordinateSystem.setLocation(pr.getHasLocation());
//            NeptusLog.pub().info("<###> "+key + " :: " +coordinateSystem);

            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);
            if (sys != null) {
                if ((System.currentTimeMillis() - time < timeMillisToDiscart) && sys.getLocationTimeMillis() < time) {
                    if (coordinateSystem.getLatitudeDegs() != 0d && coordinateSystem.getLongitudeDegs() != 0d) {
                        sys.setLocation(coordinateSystem, time);

                        if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                && coordinateSystem.getYaw() != 0d) {
                            sys.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                    coordinateSystem.getYaw(), time);
                        }
                        sys.storeData(SystemUtils.WEB_UPDATED_KEY, true, time, true);
                    }
                }
            }
            else {
                ExternalSystem ext;
                if (pr.getMmsi() > 0)
                    ext = NMEAUtils.getAndRegisterExternalSystem((int) pr.getMmsi(), pr.getName());
                else
                    ext = ExternalSystemsHolder.lookupSystem(id);
                
                boolean registerNewExternal = false;
                if (ext == null) {
                    registerNewExternal = true;
                    ext = new ExternalSystem(id);
                    SystemTypeEnum type = SystemTypeEnum.UNKNOWN;
                    if (pr.getType() == PlatformType.AUV) {
                        ext.setType(SystemTypeEnum.VEHICLE);
                        ext.setTypeVehicle(VehicleTypeEnum.UUV);
                    }
                    else if (pr.getType() == PlatformType.DRIFTER) {
                        ext.setType(SystemTypeEnum.MOBILESENSOR);
                    } 
                    else if (pr.getType() == PlatformType.GLIDER) {
                        ext.setType(SystemTypeEnum.VEHICLE);
                        ext.setTypeVehicle(VehicleTypeEnum.UUV);
                    } 
                    else if (pr.getType() == PlatformType.MOORING) {
                        ext.setType(SystemTypeEnum.STATICSENSOR);
                    } 
                    else if (pr.getType() == PlatformType.AIS) {
                        ext.setType(SystemTypeEnum.UNKNOWN);
                        ext.setTypeExternal(ExternalTypeEnum.MANNED_SHIP);
                    } 
                    else if (pr.getType() == PlatformType.SHIP) {
                        ext.setType(SystemTypeEnum.UNKNOWN);
                        ext.setTypeExternal(ExternalTypeEnum.MANNED_SHIP);
                    } 
                    ext.setType(type);
                    
                    if (pr.getMmsi() > 0)
                        ext.storeData(SystemUtils.MMSI_KEY, pr.getMmsi(), time, true);
                    
                    // See better because this should not be here
                    ext.setLocation(coordinateSystem, time);
                    ExternalSystemsHolder.registerSystem(ext);
                }
                if ((System.currentTimeMillis() - time < timeMillisToDiscart) && ext.getLocationTimeMillis() < time) {
                    if (coordinateSystem.getLatitudeDegs() != 0d && coordinateSystem.getLongitudeDegs() != 0d) {
                        ext.setLocation(coordinateSystem, time);

                        if (coordinateSystem.getRoll() != 0d && coordinateSystem.getPitch() != 0d
                                && coordinateSystem.getYaw() != 0d) {
                            ext.setAttitudeDegrees(coordinateSystem.getRoll(), coordinateSystem.getPitch(),
                                    coordinateSystem.getYaw(), time);
                        }
                        
                        if (registerNewExternal)
                            ExternalSystemsHolder.registerSystem(ext);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (!fetchURL.endsWith("/"))
            fetchURL += "/";

        abortAllActiveConnections();
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    public String validateFetchURL(String value) {
        if (!fetchURL.endsWith("/"))
            fetchURL += "/";
        try {
            new URL(fetchURL);
            return null;
        }
        catch (MalformedURLException e) {
            return e.getMessage();
        }
    }
    
    public String validatePeriodHoursToFetch(int value) {
        String ret = new IntegerMinMaxValidator(1, 300, true, true).validate(value);
        if (value < 1)
            value = periodHoursToFetch = 1;
        if (value > 300)
            value = periodHoursToFetch = 300;
        
        return ret;
    }
    
    public String validateUpdatePeriodMillis(int value) {
        String ret = new IntegerMinMaxValidator(500, (int) (DateTimeUtil.MINUTE * 10), true, true).validate(value);
        if (value < 500)
            value = updatePeriodMillis = 500;
        if (value > DateTimeUtil.MINUTE * 10)
            value = updatePeriodMillis = (int) (DateTimeUtil.MINUTE * 10);
        
        return ret;
    }

    private void abortAllActiveConnections() {
        try {
            if (getHttpRequestState != null)
                getHttpRequestState.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates
     * ()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
//        refreshUI();

        if (startStopCheckItem != null)
            startStopCheckItem.setState(fetchOn);

        if (!fetchOn)
            abortAllActiveConnections();
        
        return true;
    }
    
    // Use for debug
    private Document loadXml(String xml) throws Exception {
        DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
        Document docProfiles = 
                //builder.parse("http://beach.mbari.org/trackingdb/positionOfType/auv/last/300h/data.html");
                builder.parse(bais);
        return docProfiles;
    }

    public static void main(String[] args) {
        try {
            VehiclesHolder.loadVehicles();
            ImcMsgManager.getManager().start();
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-seacon-1", "127.0.0.1");

            NeptusLog.pub().info("<###>ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            NeptusLog.pub().info("<###>ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);

            OdssStoqsTrackFetcher osf = new OdssStoqsTrackFetcher(null);
            osf.debugOn = true;
            osf.publishRemoteSystemsLocally = true;
            osf.periodHoursToFetch = 300;
            
            
            NeptusLog.pub().info("<###>\n\n-------------- Use remote requests  -------------- \n");
            osf.getStateRemoteData();
            NeptusLog.pub().info("<###>ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            NeptusLog.pub().info("<###>ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);

            NeptusLog.pub().info("<###>\n\n-------------- Use local test files -------------- \n");
            
            String xml = FileUtil.getFileAsString("srcTests/mbari/ODSS-Position-2.html");
            Document docProfiles = osf.loadXml(xml);
            
            HashMap<String, PlatformReportType> sysBag = osf.processOdssStokesResponse(docProfiles, PlatformType.AUV);
            filterAndAddToList(sysBag);
            for (String key : sysBag.keySet()) {
                NeptusLog.pub().info("<###> "+sysBag.get(key));
            }
            osf.sysStokesLocations.putAll(sysBag);
            osf.processRemoteStates();
            
            NeptusLog.pub().info("<###>ImcSystemsHolder:   " + ImcSystemsHolder.lookupAllSystems().length);
            NeptusLog.pub().info("<###>ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);
            
            xml = FileUtil.getFileAsString("srcTests/mbari/ODSS-Position-glider.html");
            docProfiles = osf.loadXml(xml);
            
            sysBag = osf.processOdssStokesResponse(docProfiles, PlatformType.GLIDER);
            filterAndAddToList(sysBag);
            for (String key : sysBag.keySet()) {
                NeptusLog.pub().info("<###> "+sysBag.get(key));
            }
            osf.sysStokesLocations.putAll(sysBag);
            osf.processRemoteStates();
            
            NeptusLog.pub().info("<###>ImcSystemsHolder:     " + ImcSystemsHolder.lookupAllSystems().length);
            NeptusLog.pub().info("<###>ExternalSystemsHolder: " + ExternalSystemsHolder.lookupAllSystems().length);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
