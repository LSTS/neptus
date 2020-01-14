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
 * 7 de Jul de 2011
 */
package pt.lsts.neptus.plugins.webupdate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.w3c.dom.Document;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.SystemImcMsgCommInfo;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.plugins.CheckMenuChangeListener;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author pdias
 * 
 */
@SuppressWarnings({"serial"})
@PluginDescription(name = "Remote Position Fetcher Updater", author = "Paulo Dias", version = "0.2",
icon="pt/lsts/neptus/plugins/webupdate/webupdate-fetch-on.png")
@LayerPriority(priority = 178)
public class RemotePositionFetcherUpdater extends ConsolePanel implements IPeriodicUpdates,
        ConfigurationListener, Renderer2DPainter, SubPanelChangeListener {

    private final ImageIcon ICON_ENABLE = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/webupdate/webupdate-fetch-on.png", 32, 32);
    private final ImageIcon ICON_DISABLE = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/webupdate/webupdate-fetch-off.png", 32, 32);

    private final Color LIGHT_ORCHID = new Color(255, 161, 255);
    
    @NeptusProperty(name = "Publish web address")
    public String pubURL = "http://whale.fe.up.pt/neptleaves/";

    @NeptusProperty(name = "Update period (ms)", description = "The period to fetch the systems' positions. "
            + "Zero means disconnected.")
    public int updatePeriodMillis = 1000;

    @NeptusProperty
    public boolean fetchOn = false;

    @NeptusProperty (description = "If true will paint the web fetch postion in the render.")
    public boolean showSystem = true;

    @NeptusProperty (description = "If true will write age of data and depth on the render.")
    public boolean infoExtended = true;

    @NeptusProperty (description = "if true will also retreive remote messages from server.")
    public boolean publishWebReceivedMessages = true;

    @NeptusProperty(name = "Seconds to display ranges")
    public int secondsToDisplayRanges = 5;

    @NeptusProperty
    public boolean publishRemoteSystemsLocally = true;

    @NeptusProperty(category = "Advanced")
    public boolean debugOn = false;

    private JCheckBoxMenuItem publishCheckItem = null;

//    private long lastCalcPosTimeMillis = -1;
    private long lastFetchPosTimeMillis = System.currentTimeMillis();

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();
    private static GeneralPath arrowShape = null;

    private HttpClientConnectionHelper httpComm;
    private HttpGet getHttpRequestState, getHttpRequestImcMsgs;

    private Timer timer = null;
    private TimerTask ttask = null;

    private final LinkedHashMap<String, Long> timeSysList = new LinkedHashMap<String, Long>();
    private final LinkedHashMap<String, CoordinateSystem> locSysList = new LinkedHashMap<String, CoordinateSystem>();

    private DocumentBuilderFactory docBuilderFactory;

    private ToolbarButton sendEnableDisableButton;

    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
    }

    /**
     * 
     */
    public RemotePositionFetcherUpdater(ConsoleLayout console) {
        super(console);
        initializeComm();
        initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }

//        addMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Start", null,
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        fetchOn = true;
//                    }
//                });
//
//        addMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Stop", null,
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        fetchOn = false;
//                    }
//                });

        publishCheckItem = addCheckMenuItem(I18n.text("Settings") + ">" + PluginUtils.getPluginName(this.getClass())
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
                        PropertiesEditor.editProperties(RemotePositionFetcherUpdater.this,
                                getConsole(), true);
                    }
                });
        
        publishCheckItem.setState(fetchOn);
    }

    /**
     * 
     */
    private void initialize() {
        // setVisibility(false);

        removeAll();
        setBackground(new Color(255, 255, 110));

        sendEnableDisableButton = new ToolbarButton(new AbstractAction("Fetch on", ICON_ENABLE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String cmd = e.getActionCommand();
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendEnableDisableButton.setEnabled(false);
                        if ("Fetch on".equalsIgnoreCase(cmd)) {
                            fetchOn = true;
                            sendEnableDisableButton.setActionCommand("Fetch off");
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON,
                                    ICON_DISABLE);
                            sendEnableDisableButton.getAction().putValue(
                                    AbstractAction.SHORT_DESCRIPTION, "Fetch off");
                        }
                        else if ("Fetch off".equalsIgnoreCase(cmd)) {
                            fetchOn = false;
                            sendEnableDisableButton.setActionCommand("Fetch on");
                            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON,
                                    ICON_ENABLE);
                            sendEnableDisableButton.getAction().putValue(
                                    AbstractAction.SHORT_DESCRIPTION, "Fetch on");

                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        sendEnableDisableButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        sendEnableDisableButton.setActionCommand("Fetch on");
        add(sendEnableDisableButton);

        timer = new Timer("RemotePositionFetcher");
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    private void initializeComm() {
        httpComm = new HttpClientConnectionHelper();
        httpComm.initializeComm();
    }

    /**
     * 
     */
    private void refreshUI() {
        if (fetchOn && "Fetch on".equalsIgnoreCase(sendEnableDisableButton.getActionCommand())) {
            sendEnableDisableButton.setActionCommand("Fetch off");
            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_DISABLE);
            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION,
                    "Fetch off");
        }
        else if (!fetchOn
                && !"Fetch on".equalsIgnoreCase(sendEnableDisableButton.getActionCommand())) {
            sendEnableDisableButton.setActionCommand("Fetch on");
            sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_ENABLE);
            sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION,
                    "Fetch on");
        }
    }

    private TimerTask getTimerTask() {
        if (ttask == null) {
            ttask = new TimerTask() {
                @Override
                public void run() {
                    if (!fetchOn)
                        return;
                    getStateRemoteData();
                    getRemoteImcData();
                }
            };
        }
        return ttask;
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
        refreshUI();

        if (publishCheckItem != null)
            publishCheckItem.setState(fetchOn);

        if (!fetchOn)
            abortAllActiveConnections();
        
        return true;
    }

    private boolean getStateRemoteData() {
        if (getHttpRequestState != null)
            getHttpRequestState.abort();
        getHttpRequestState = null;
        try {
            String endpoint = pubURL; // GeneralPreferences.getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
            String uri = endpoint + "state/state.xml"; // + "/state.xml";
            getHttpRequestState = new HttpGet(uri);

            HttpClientContext localContext = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequestState, localContext);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, localContext);

            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase()
                        + " code was return from the server");
                if (getHttpRequestState != null) {
                    getHttpRequestState.abort();
                }
                return false;
            }
            InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document docProfiles = builder.parse(streamGetResponseBody);
            RemotePositionHelper.getRemoteState(timeSysList, locSysList, docProfiles);
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (getHttpRequestState != null) {
                getHttpRequestState.abort();
                getHttpRequestState = null;
            }
        }
        
        processRemoteStates();
        
        return true;
    }

    private void processRemoteStates() {
        if (publishRemoteSystemsLocally)
            RemotePositionHelper.publishRemoteStatesLocally(timeSysList, locSysList);
    }

    private boolean getRemoteImcData() {
        if (!publishWebReceivedMessages) {
            return true;
        }

        if (getHttpRequestImcMsgs != null)
            getHttpRequestImcMsgs.abort();
        getHttpRequestImcMsgs = null;
        long time = (lastFetchPosTimeMillis <= 0 ? 0 : lastFetchPosTimeMillis);
        try {
            String endpoint = pubURL; // GeneralPreferences.getProperty(GeneralPreferences.PUBLISH_WS_ADDRESS);
            String uri = endpoint + "imc?after=" + time; // + "/state.xml";
            getHttpRequestImcMsgs = new HttpGet(uri);
            @SuppressWarnings("unused")
            long reqTime = System.currentTimeMillis();
            
            HttpClientContext context = HttpClientContext.create();
            HttpResponse iGetResultCode = httpComm.getClient().execute(getHttpRequestImcMsgs, context);
            httpComm.autenticateProxyIfNeeded(iGetResultCode, context);
            
            if (iGetResultCode.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                NeptusLog.pub().info("<###>[" + iGetResultCode.getStatusLine().getStatusCode() + "] "
                        + iGetResultCode.getStatusLine().getReasonPhrase()
                        + " code was return from the server");
                if (getHttpRequestImcMsgs != null) {
                    getHttpRequestImcMsgs.abort();
                }
                return false;
            }
            try {
                long serverTime = Long.parseLong(iGetResultCode.getFirstHeader("server-time")
                        .getValue().trim());
                // NeptusLog.pub().info("<###>server time delta: " + (reqTime -
                // serverTime) + "ms");
                lastFetchPosTimeMillis = serverTime;
            }
            catch (Exception e) {
                lastFetchPosTimeMillis = System.currentTimeMillis();
            }
            InputStream streamGetResponseBody = iGetResultCode.getEntity().getContent();
            @SuppressWarnings("unused")
            long fullSize = iGetResultCode.getEntity().getContentLength();
            // PipedOutputStream pos = new PipedOutputStream();
            // PipedInputStream pis = new PipedInputStream(pos);
            // boolean streamRes =
            // StreamUtil.copyStreamToStream(streamGetResponseBody, pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            @SuppressWarnings("unused")
            boolean streamRes = StreamUtil.copyStreamToStream(streamGetResponseBody, baos);
            // ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
            // baos.flush();
            byte[] baa = baos.toByteArray();
            // pos.write(baa);
            // pos.flush();
            // pos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(baa);
            IMCMessage[] msgs = IMCUtils.parseLsf(bais);
            if (msgs.length > 0) {
                processWebMessages(msgs);
            }
        }
        catch (Exception e) {
         // e.printStackTrace();
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            if (getHttpRequestImcMsgs != null) {
                getHttpRequestImcMsgs.abort();
                getHttpRequestImcMsgs = null;
            }
        }
        return true;
    }

    /**
     * @param msgs
     */
    private void processWebMessages(IMCMessage[] msgs) {
        if (publishWebReceivedMessages) {
            for (IMCMessage msg : msgs) {
                try {
                    ImcId16 id = new ImcId16(msg.getHeader().getValue("src"));
                    if (!debugOn) {
                        if (!ImcId16.NULL_ID.equals(ImcMsgManager.getManager().getLocalId())
                                && ImcMsgManager.getManager().getLocalId().equals(id))
                            return;
                    }

                    MessageInfo info = new MessageInfoImpl();
                    info.setTimeSentNanos((long) (msg.getHeader().getTimestamp() * 1E9));
                    info.setTimeReceivedNanos(lastFetchPosTimeMillis * (long) 1E6);
                    info.setProperty(MessageInfo.NOT_TO_LOG_MSG_KEY, "true");
                    info.setProperty(MessageInfo.WEB_FETCH_MSG_KEY, "true");

                    ImcSystem sys = ImcSystemsHolder.lookupSystem(id);
                    if (sys == null) {
                        if (id.intValue() >= 0x4000 && id.intValue() < 0x5FFF) {
                            // So it's a CCU from web
                            processWebCCUMessage(info, msg);
                        }
                        continue;
                    }
                    if (!sys.isActive()) {
                        SystemImcMsgCommInfo comm = ImcMsgManager.getManager().getCommInfoById(sys.getId());
                        if (comm != null)
                            comm.onMessage(info, msg);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param info
     * @param msg
     */
    private void processWebCCUMessage(MessageInfo info, IMCMessage msg) {
        // NeptusLog.pub().info("<###> "+msg.asJSON());

        if ("PlanSpecification".equalsIgnoreCase(msg.getAbbrev())) {
            try {
                String planId = msg.getAsString("plan_id");
                ImcId16 imcId = null;
                try {
                    imcId = new ImcId16(msg.getHeaderValue("src"));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                String srcId = (imcId == null ? msg.getHeaderValue("src").toString() : imcId
                        .toString());

                int res = JOptionPane.showConfirmDialog(getConsole(), "Plan with id '" + planId
                        + "' just arrived from " + srcId + ". Want to accept it?",
                        PluginUtils.getPluginName(this.getClass()),
                        JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }

                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), msg);
                if (getConsole().getMission().getIndividualPlansList().containsKey(planId)) {
                    res = JOptionPane.showConfirmDialog(getConsole(), "Overwrite existing plan?",
                            "Plan editor", JOptionPane.YES_NO_OPTION);
                    if (res != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                getConsole().getMission().getIndividualPlansList().put(planId, plan);

                new Thread() {
                    @Override
                    public void run() {
                        getConsole().getMission().save(false);
                    }
                }.start();

                getConsole().updateMissionListeners();
                getConsole().setPlan(plan);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void cleanSubPanel() {

        removeCheckMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Start/Stop");
        removeMenuItem("Settings>" + PluginUtils.getPluginName(this.getClass()) + ">Settings");

        if (httpComm != null) {
            httpComm.cleanUp();;
        }
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timeSysList.clear();
        locSysList.clear();

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.removePostRenderPainter(this);
        }
    }

    private static GeneralPath getArrow() {
        if (arrowShape == null) {
            arrowShape = new GeneralPath();
            arrowShape.moveTo(-2, 0);
            arrowShape.lineTo(2, 0);
            arrowShape.lineTo(2, 0);
            arrowShape.lineTo(8, 0);
            arrowShape.lineTo(0, 8);
            arrowShape.lineTo(-8, 0);
            arrowShape.lineTo(-2, 0);
            arrowShape.closePath();
        }
        return arrowShape;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (!pubURL.endsWith("/"))
            pubURL += "/";

        abortAllActiveConnections();
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        ttask = getTimerTask();
        timer.scheduleAtFixedRate(ttask, 500, updatePeriodMillis);
    }

    private void abortAllActiveConnections() {
        try {
            if (getHttpRequestState != null)
                getHttpRequestState.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (getHttpRequestImcMsgs != null)
                getHttpRequestImcMsgs.abort();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
        if (panelChange == null)
            return;

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {
            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "GPS FIX");
                }
            }

            if (panelChange.removed()) {
                renderers.remove(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.removePostRenderPainter(this);

                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D
     * , pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        if (fetchOn && showSystem) {
            if (timeSysList.size() == 0)
                return;

            for (String id : timeSysList.keySet().toArray(new String[0])) {
                Color colorIcon = LIGHT_ORCHID;
                
                double alfaPercentage = 1.0;
                long deltaTimeMillis = System.currentTimeMillis() - timeSysList.get(id);
                if (deltaTimeMillis > DateTimeUtil.MINUTE * 5) {
                    alfaPercentage = 0.0;
                    continue;
                }
                else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 2.0) {
                    alfaPercentage = 0.5;
                }
                else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 4.0) {
                    alfaPercentage = 0.7;
                }

                double rotationAngle = renderer.getRotation();
                CoordinateSystem lt = locSysList.get(id);
                Point2D centerPos = renderer.getScreenPosition(new LocationType(lt));

                Color baseColor = new Color(173, 154, 79);

                Graphics2D g = (Graphics2D) g2.create();
                g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
                g.draw(new Ellipse2D.Double(centerPos.getX() - 10, centerPos.getY() - 10, 20, 20));

                // g.setColor(new Color(139,69,19 , (int) (255 *
                // alfaPercentage)));
                g.setColor(ColorUtils.setTransparencyToColor(LIGHT_ORCHID, // Color.YELLOW,
                        (int) (255 * alfaPercentage)));
                g.draw(new Ellipse2D.Double(centerPos.getX() - 12, centerPos.getY() - 12, 24, 24));
                g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
                g.draw(new Ellipse2D.Double(centerPos.getX() - 14, centerPos.getY() - 14, 28, 28));
                g.translate(centerPos.getX(), centerPos.getY());
                // g.setColor(new Color(255, 255, 0, (int) (200 *
                // alfaPercentage)).darker());
                Color color = baseColor.darker();
                g.setColor(ColorUtils.setTransparencyToColor(color, (int) (200 * alfaPercentage)));
                g.fill(new Ellipse2D.Double(-7, -7, 14, 14));
                // g.setColor(new Color(255, 255, 0, (int) (150 *
                // alfaPercentage)).brighter());
                color = new Color(139, 69, 19, 0).brighter();
                g.setColor(ColorUtils.setTransparencyToColor(color, (int) (150 * alfaPercentage)));
                g.setStroke(new BasicStroke(2));
                g.draw(new Ellipse2D.Double(-7, -7, 14, 14));
                g.setColor(new Color(0, 0, 0, (int) (150 * alfaPercentage)));
                g.fill(new Ellipse2D.Double(-2, -2, 4, 4));
                g.setColor(Color.BLACK);
                // if(getConsole()!=null && getConsole().getMainVehicle()!=null)
                String strMsg = "Web: " + id; //getConsole().getMainVehicle();
                String strMsg2 = "";
                if (infoExtended) {
                    strMsg += (deltaTimeMillis < DateTimeUtil.SECOND * 10 ? "" : " :: age:"
                            + DateTimeUtil.milliSecondsToFormatedString(deltaTimeMillis));
                    strMsg2 += " > "
                            + (lt.getAllZ() >= 0 ? "depth=" : "alt=")
                            + MathMiscUtils.round(
                                    (lt.getAllZ() >= 0 ? lt.getAllZ() : -lt.getAllZ()),
                                    1) + "m";
                }
                g.drawString(strMsg, 10 - 7, 15 + 12);
                g.drawString(strMsg2, 10 - 7, 15 + 12 + 14);
                g.setColor(LIGHT_ORCHID ); // Color.YELLOW);
                g.drawString(strMsg, 10 - 8, 15 + 13);
                g.drawString(strMsg2, 10 - 8, 15 + 13 + 14);

//                if (!Double.isNaN(lt.getYaw())) {
                    double newYaw = !Double.isNaN(lt.getYaw()) ? Math.toRadians(lt.getYaw()) : 0;
                    Shape shape = getArrow();
                    g.rotate(-rotationAngle);
                    if (newYaw != 0)
                        g.rotate(newYaw + Math.PI);
                    color = colorIcon; //Color.BLACK;
                    g.setColor(ColorUtils.setTransparencyToColor(color,
                            (int) (150 * alfaPercentage)));
                    g.setStroke(new BasicStroke(2));
                    g.fill(shape);
                    color = color.darker(); //Color.BLACK.darker();
                    g.setColor(ColorUtils.setTransparencyToColor(color,
                            (int) (150 * alfaPercentage)));
                    g.draw(shape);
                    g.setColor(Color.BLACK);
                    if (newYaw != 0)
                        g.rotate(-(newYaw + Math.PI));
                    g.rotate(rotationAngle);
//                }

                g.translate(-centerPos.getX(), -centerPos.getY());
            }
        }
    }

}
