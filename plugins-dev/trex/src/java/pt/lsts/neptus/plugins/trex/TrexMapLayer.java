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
 * Author: José Pinto, Margarida Faria
 * May 28, 2012
 */
package pt.lsts.neptus.plugins.trex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.TrexCommand;
import pt.lsts.imc.TrexOperation;
import pt.lsts.imc.TrexOperation.OP;
import pt.lsts.imc.TrexToken;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.fileeditor.SyntaxDocument;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.trex.goals.AUVDrifterSurvey;
import pt.lsts.neptus.plugins.trex.goals.TrexGoal;
import pt.lsts.neptus.plugins.trex.goals.UavSpotterSurvey;
import pt.lsts.neptus.plugins.trex.goals.VisitLocationGoal;
import pt.lsts.neptus.plugins.trex.gui.PortEditor;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "TrexMapLayer", icon = "pt/lsts/neptus/plugins/trex/smallTrex.png")
public class TrexMapLayer extends SimpleRendererInteraction implements Renderer2DPainter, NeptusMessageListener {
    enum CommsChannel {
        IMC,
        IRIDIUM,
        REST;
    }

    @NeptusProperty(name = "Default depth (negative for altitude)")
    public double defaultDepth = 2;

    @NeptusProperty(name="T-Rex Shore IP", description="Ip of the machine where T-Rex is running whith shore configuration to follow a tag.")
    public String ipShore = "localhost";
    @NeptusProperty(name = "T-Rex Shore port", description = "Port of the machine where T-Rex is running whith shore configuration to follow a tag.", editorClass = PortEditor.class)
    public int portShore = 8888;
    @NeptusProperty(name = "Comms channel", description = "Choose if Dune communicates with T-Rex through a REST API or IP.")
    public CommsChannel trexDuneComms = CommsChannel.IMC;
    @NeptusProperty(name = "Name of Dune task")
    public String taskName = "TREX";

    @NeptusProperty(name = "Loiter height", description = "Height of waypoint for uav spotter plan.", category = "UAV Spotter", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int spotterHeight = 100;

    @NeptusProperty(name = "Type", category = "YoYo Survey")
    public AUVDrifterSurvey.PathType path = AUVDrifterSurvey.PathType.SQUARE;

    @NeptusProperty(name = "Survey Size", category = "YoYo Survey")
    public float size = 800;

    @NeptusProperty(name = "Lagrangian distortion", category = "YoYo Survey", description="True if you want to apply Lagrangian distortion.")
    public boolean lagrangin = true;

    @NeptusProperty(name = "Rotation", category = "YoYo Survey", description="In degrees, an offset to north in clockwise.")
    public float heading = 0;

    @NeptusProperty(name = "Water current Speed", category = "YoYo Survey", description="Speed, in mps of the surface current.")
    public float speed = 0;
    
    @NeptusProperty(name = "Notifications for received T-REX observations")
    public boolean postNotifications = true;
    

    private final CloseableHttpClient httpclient = HttpClientBuilder.create().build();

    private static final long serialVersionUID = 1L;
    private Maneuver lastManeuver = null;

    protected LinkedHashMap<String, TrexGoal> sentGoals = new LinkedHashMap<String, TrexGoal>();
    protected LinkedHashMap<String, TrexGoal> completeGoals = new LinkedHashMap<String, TrexGoal>();

    protected LocationType[] surveyArea = new LocationType[4];
    protected boolean surveyEdit = false;
    protected int surveyPos = 0;
    protected boolean active = false;

    protected Vector<LocationType> sentPoints;
    private LocationType currentRef;
    final String fixedTrexPlanId = "trex_plan";
    Image trex = null;
    private boolean trexActive;

    /**
     * @param console
     */
    public TrexMapLayer(ConsoleLayout console) {
        super(console);
        sentPoints = new Vector<LocationType>();
        trexActive = false;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        active = mode;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        if (event.getButton() == MouseEvent.BUTTON1 && surveyEdit) {

            if (surveyPos < 4) {
                LocationType loc = source.getRealWorldLocation(event.getPoint());
                loc.convertToAbsoluteLatLonDepth();
                surveyArea[surveyPos++] = loc;
            }
            if (surveyPos >= 4) {
                String s_transects = JOptionPane.showInputDialog(getConsole(), "Number of transects: ", "5");
                String s_depth = JOptionPane.showInputDialog(getConsole(), "Depth: ", "" + defaultDepth);
                double depth;
                int n_transects;
                try {
                    depth = Double.parseDouble(s_depth);
                    n_transects = Integer.parseInt(s_transects);
                    if (n_transects < 2)
                        throw new Exception("Number of transects should be bigger than 1");
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(getConsole(), e);
                    return;
                }

                final String goalId = "Neptus_" + System.currentTimeMillis();
                String xml = "<Goal id='" + goalId + "' on='surveys' pred='LawnMower'>\n";
                for (int i = 0; i < 4; i++) {
                    xml += "  <Variable name='lat_" + i + "'><float value='" + surveyArea[i].getLatitudeDegs()
                            + "'/></Variable>\n";
                    xml += "  <Variable name='lon_" + i + "'><float value='"
                            + surveyArea[i].getLongitudeDegs() + "'/></Variable>\n";
                }
                xml += "  <Variable name='depth'><float value='" + depth + "'/></Variable>\n";
                xml += "  <Variable name='n_slices'><int value='" + n_transects + "'/></Variable>\n";
                xml += "</Goal>";

                final JEditorPane editor = SyntaxDocument.getXmlEditorPane();

                editor.setText(xml);
                JFrame frame = new JFrame("Edit goal xml");
                frame.setContentPane(new JScrollPane(editor));
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {

                    @Override
                    public void windowClosed(WindowEvent e) {
                        TrexCommand cmd = new TrexCommand();
                        cmd.setCommand(TrexCommand.COMMAND.POST_GOAL);
                        cmd.setGoalXml(editor.getText());
                        cmd.setGoalId(goalId);
                        // ImcMsgManager.getManager().sendMessageToSystem(cmd, getConsole().getMainSystem());
                        cmd.dump(System.err);
                    }
                });

                frame.setSize(500, 500);
                GuiUtils.centerParent(frame, getConsole());
                frame.setVisible(true);

                for (int i = 0; i < 4; i++) {
                    NeptusLog.pub().info("<###> " + surveyArea[i].asXML());
                }

            }
        }

        else if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            final LocationType loc = source.getRealWorldLocation(event.getPoint());

            if (surveyEdit) {
                addSurveyPointsMenu(popup);
            }
            if (trexActive) {
                addDisableTrexMenu(popup);
                popup.addSeparator();
                addUAVSpotter(popup, loc);
                addClearNeptusGoalsMenu(popup);
            }
            else {
                addEnableTrexMenu(popup);
                popup.addSeparator();
                addClearNeptusGoalsMenu(popup);
            }
            popup.addSeparator();
            addVisitThisPointMenu(popup, loc);
            addAUVDrifter(popup, loc);
            addClearGoalMenu(popup);
            //addTagSimulation(popup, loc);

            //            for (String gid : sentGoals.keySet()) {
            //                Point2D screenPos = source.getScreenPosition(sentGoals.get(gid).getLocation()); // FIXME all goals have
            //                // location?
            //                if (screenPos.distance(clicked) < 5) {
            //                    final String goal = gid;
            //                    addRecallGoalMenu(popup, gid, goal);
            //                }
            //            }
            addSettingMenu(popup);
            popup.show(source, event.getPoint().x, event.getPoint().y);
        }
    }

    private void addSettingMenu(JPopupMenu popup) {
        popup.add("Settings").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(TrexMapLayer.this, getConsole(), true);
            }
        });
    }

    private void addEnableTrexMenu(JPopupMenu popup) {
        popup.add("Enable TREX").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                    startTrex();
                    return;
                }

                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("TREX");
                EntityParameter param = new EntityParameter("Active", "true");
                Vector<EntityParameter> p = new Vector<>();
                p.add(param);
                setParams.setParams(p);
                switch (trexDuneComms) {
                    case IMC:
                    case REST:
                        ImcMsgManager.getManager().sendMessageToSystem(setParams, getConsole().getMainSystem());
                        break;
                    case IRIDIUM:
                        try {
                            sendViaIridium(getConsole().getMainSystem(), setParams);
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(e);
                        }
                        break;
                }
                
                //ImcMsgManager.getManager().sendMessageToSystem(setParams, getConsole().getMainSystem());
            }
        });
    }

    private void addClearNeptusGoalsMenu(JPopupMenu popup) {
        popup.add("Clear all goals in Neptus").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sentPoints.removeAllElements();
            }
        });
    }

    private void addDisableTrexMenu(JPopupMenu popup) {
        popup.add("Disable TREX").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
                    stopTrex();
                    return;
                }
                SetEntityParameters setParams = new SetEntityParameters();
                setParams.setName("TREX");
                EntityParameter param = new EntityParameter("Active", "false");
                Vector<EntityParameter> p = new Vector<>();
                p.add(param);
                setParams.setParams(p);
                
                switch (trexDuneComms) {
                    case IMC:
                    case REST:
                        ImcMsgManager.getManager().sendMessageToSystem(setParams, getConsole().getMainSystem());
                        break;
                    case IRIDIUM:
                        try {
                            sendViaIridium(getConsole().getMainSystem(), setParams);
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(e);
                        }
                        break;
                }
                
                
            }
        });
    }

    private void addClearGoalMenu(JPopupMenu popup) {
        popup.add("Clear goals").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                sentGoals.clear();
                completeGoals.clear();
                lastManeuver = null;
            }
        });
    }

    // private void addSurvveyAreaMenu(JPopupMenu popup) {
    // popup.add("Survey an area").addActionListener(new ActionListener() {
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // surveyArea = new LocationType[4];
    // surveyEdit = true;
    // surveyPos = 0;
    // }
    // });
    // }

    private void addVisitThisPointMenu(JPopupMenu popup, final LocationType loc) {
        popup.add("Visit this point").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                VisitLocationGoal visitLocationGoal = new VisitLocationGoal(loc.getLatitudeRads(), loc
                        .getLongitudeRads());
                switch (trexDuneComms) {
                    case IMC:
                        send(visitLocationGoal.asIMCMsg());
                        break;
                    case IRIDIUM:
                        try {
                            sendViaIridium(getConsole().getMainSystem(), visitLocationGoal.asIMCMsg());
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(e);
                        }
                        break;
                    case REST:
                        httpPostTrex(visitLocationGoal);
                        break;
                }
            }
        });
    }

    private void addUAVSpotter(JPopupMenu popup, final LocationType loc) {
        popup.add("UAV spotter").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                UavSpotterSurvey going = new UavSpotterSurvey(loc.getLatitudeRads(), loc
                        .getLongitudeRads(), spotterHeight);
                sentPoints.add(loc);
                switch (trexDuneComms) {
                    case IMC:
                        send(going.asIMCMsg());
                        break;
                    case IRIDIUM:
                        try {
                            sendViaIridium(getConsole().getMainSystem(), going.asIMCMsg());
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(e);
                        }
                        break;
                    case REST:
                        httpPostTrex(going);
                        break;
                }
            }

//            private void activateTrex() {
//                SetEntityParameters message = new SetEntityParameters();
//                message.setName(taskName);
//                EntityParameter entityParameter = new EntityParameter();
//                entityParameter.setName("Active");
//                entityParameter.setValue("true");
//                ArrayList<EntityParameter> params = new ArrayList<EntityParameter>();
//                params.add(entityParameter);
//                message.setParams(params);
//                send(message);
//            }
        });
    }

    private void addAUVDrifter(JPopupMenu popup, final LocationType loc) {
        popup.add("YoYo Survey").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                AUVDrifterSurvey going = new AUVDrifterSurvey(loc.getLatitudeRads(), loc
                        .getLongitudeRads(), size, speed, lagrangin, path, (float)Math.toRadians(heading));
                switch (trexDuneComms) {
                    case IMC:
                        // Send goal
                        send(going.asIMCMsg());
                        break;
                    case IRIDIUM:
                        try {
                            sendViaIridium(getConsole().getMainSystem(), going.asIMCMsg());
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(e);
                        }
                        break;
                    case REST:
                        httpPostTrex(going);
                        break;
                }
                sentGoals.put(""+going.hashCode(), going);
            }

        });
    }

    private void startTrex() {
        TrexOperation op = new TrexOperation(OP.REQUEST_PLAN, "", null);
        send(op);
    }

    private void stopTrex() {
        TrexOperation op = new TrexOperation(OP.REPORT_PLAN, "", null);
        send(op);
    }

    /*private void addTagSimulation(JPopupMenu popup, final LocationType loc) {
        popup.add("Simulate a tag here").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                TagSimulation tagSimulation = new TagSimulation(loc.getLatitudeAsDoubleValueRads(), loc
                        .getLongitudeAsDoubleValueRads());
                httpPostTrex(tagSimulation);
            }

        });
    }*/

    private void httpPostTrex(TrexGoal goal) {
        try {
            StringEntity message;
            message = new StringEntity(goal.toJson());
            HttpPost httppost = new HttpPost("http://" + ipShore + ":" + portShore + "/rest/goal");
            httppost.setHeader("Content-Type", "application/json");
            httppost.setEntity(message);
            // Execute

            HttpResponse response = httpclient.execute(httppost);

            // Get the response
            HttpEntity entity = response.getEntity();
            String textReceived = "";

            if (entity != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        textReceived += line + "\n";
                    }
                }
                finally {
                    reader.close();                    
                }
            }
            if (response.getStatusLine().getStatusCode() != 200) {
                GuiUtils.errorMessage(TrexMapLayer.this, "POST Goal", textReceived);
            }

        }
        catch (HttpHostConnectException e1) {
            GuiUtils.errorMessage(TrexMapLayer.this, "Unable to reach T-Rex", "Cannot reach T-Rex:" + e1.getMessage()
                    + ". Is it running and listening on this port?");
        }
        catch (IllegalStateException | IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    private void addSurveyPointsMenu(JPopupMenu popup) {
        popup.add("Clear survey points").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                surveyEdit = false;
                surveyPos = 0;
                surveyArea = new LocationType[4];
            }
        });
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (trex == null)
            trex = ImageUtils.getImage(PluginUtils.getPluginIcon(this.getClass()));

        if (surveyEdit) {
            for (int i = 0; i < surveyPos; i++) {
                LocationType loc = surveyArea[i];
                Point2D pt = renderer.getScreenPosition(loc);
                g.translate(pt.getX(), pt.getY());
                g.setColor(Color.orange);
                g.fill(new Ellipse2D.Double(-5, -5, 10, 10));
                g.setColor(Color.red.darker().darker());
                g.drawString("" + i, 8, 8);
                g.translate(-pt.getX(), -pt.getY());
            }
        }

        if (lastManeuver != null) {
            if (lastManeuver instanceof LocatedManeuver) {
                LocationType loc = ((LocatedManeuver) lastManeuver).getManeuverLocation();
                Point2D pt = renderer.getScreenPosition(loc);
                g.translate(pt.getX(), pt.getY());
                g.setColor(Color.red);
                g.fill(new Ellipse2D.Double(-5, -5, 10, 10));
                g.setColor(Color.red.darker().darker());
                g.drawString("TREX: " + lastManeuver, 10, 10);
                g.translate(-pt.getX(), -pt.getY());
            }
        }

        g.setColor(Color.black);
        for (String goal : sentGoals.keySet()) {
            if (sentGoals.get(goal) instanceof Renderer2DPainter) {
                ((Renderer2DPainter)sentGoals.get(goal)).paint((Graphics2D)g.create(), renderer);
            }
        }
        //            LocationType loc = sentGoals.get(goal).getLocation();
        //            long secs = sentGoals.get(goal).secs;
        //            Point2D pt = renderer.getScreenPosition(loc);
        //            g.translate(pt.getX(), pt.getY());
        //            g.draw(new Ellipse2D.Double(-5, -5, 10, 10));
        //            g.drawString("(" + loc.getAllZ() + "m, " + secs + "s)", 8, 8);
        //            g.translate(-pt.getX(), -pt.getY());
        //            double radius = sentGoals.get(goal).tolerance * renderer.getZoom();
        //
        //            g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
        //        }
        //
        //        g.setColor(Color.green);
        //        for (String goal : completeGoals.keySet()) {
        //            Point2D pt = renderer.getScreenPosition(completeGoals.get(goal).getLocation());
        //            g.translate(pt.getX(), pt.getY());
        //            g.draw(new Ellipse2D.Double(-5, -5, 10, 10));
        //            g.translate(-pt.getX(), -pt.getY());
        //        }

        paintUavGoals(g, renderer);

        if (active)
            g.drawImage(trex, 5, 5, 32, 32, this);
    }

    private void paintUavGoals(Graphics2D g, StateRenderer2D renderer) {
        LocationType loc;
        Point2D pt;
        int i = 1;
        Iterator<LocationType> iterator = sentPoints.iterator();
        while (iterator.hasNext()) {
            loc = iterator.next();
            pt = renderer.getScreenPosition(loc);
            g.translate(pt.getX(), pt.getY());
            g.setColor(new Color(173, 94, 255));
            g.fill(new Ellipse2D.Double(-5, -5, 10, 10));
            g.setColor(Color.black);
            g.drawString(i + " Goal", 10, 10);
            g.translate(-pt.getX(), -pt.getY());
            i++;
        }
        if (currentRef != null) {
            pt = renderer.getScreenPosition(currentRef);
            g.translate(pt.getX(), pt.getY());
            g.setColor(Color.green);
            g.drawOval(-5, -5, 10, 10);
            g.translate(-pt.getX(), -pt.getY());
        }
    }

    public static void main(String[] args) {

        System.out.printf("%x\n", 65094);

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        try {
            httpclient.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // @Subscribe
    // private void on(PathControlState pathState) {
    // NeptusLog.pub().warn("PathControlState");
    // NeptusLog.pub().warn("sentPoints.size()" + sentPoints.size());
    // if (sentPoints.size() == 0) {
    // return;
    // }
    // NeptusLog.pub().warn(
    // "Removing goal. [" + pathState.getEndLat() + "," + pathState.getEndLon() + "] different from ["
    // + sentPoints.get(0).getLatitudeRads() + "," + sentPoints.get(0).getLongitudeRads() + "]");
    // if (pathState.getEndLat() != sentPoints.get(0).getLatitudeRads()
    // || pathState.getEndLon() != sentPoints.get(0).getLongitudeRads()) {
    // sentPoints.remove(0);
    // }
    // }

    @Override
    public String[] getObservedMessages() {
        String msgs[] = { "PathControlState", "PlanControlState" };
        return msgs;
    }
    
    @Subscribe
    public void on(TrexOperation msg) {
        if (!postNotifications)
            return;
            
        TrexToken token = msg.getToken();
        if (token == null)
            return;
        String pred, timeline;
        switch(msg.getOp()) {
            case POST_TOKEN:
                pred = token.getPredicate();
                timeline = token.getTimeline();
                NeptusLog.pub().info("T-REX Observation: "+timeline+"."+pred);
                break;
            case POST_GOAL:
                pred = token.getPredicate();
                timeline = token.getTimeline();
                NeptusLog.pub().info("T-REX Goal: "+timeline+"."+pred);
                break;
            default:
                break;
        }        
    }

    @Override
    public void messageArrived(IMCMessage message) {
        if (message instanceof PathControlState) {
            PathControlState pathState = (PathControlState) message;
            currentRef = new LocationType();
            currentRef.setLatitudeRads(pathState.getEndLat());
            currentRef.setLongitudeRads(pathState.getEndLon());
        }
        else if (message instanceof PlanControlState) {
            PlanControlState planState = (PlanControlState) message;
            // NeptusLog.pub().warn("trexActive " + trexActive);
            // T-Rex already inactive here
            if (!trexActive){
                // NeptusLog.pub().warn(
                // "Not active. EXECUTING? " + (planState.getState() == PlanControlState.STATE.EXECUTING)
                // + ", planid: " + planState.getPlanId());
                // DUNE is running the t-rex plan so it's active there
                if(planState.getState() == PlanControlState.STATE.EXECUTING
                        && planState.getPlanId().equals(fixedTrexPlanId)) {
                    // NeptusLog.pub().warn("Activating");
                    trexActive = true;
                }
            }
            // T-Rex already active here
            else {
                // NeptusLog.pub().warn(
                // "Not active. EXECUTING? " + (planState.getState() == PlanControlState.STATE.EXECUTING)
                // + ", planid: " + planState.getPlanId());
                // DUNE isn't running the t-rex plan so it's inactive there
                if(planState.getState() != PlanControlState.STATE.EXECUTING
                        || !planState.getPlanId().equals(fixedTrexPlanId)) {
                    // NeptusLog.pub().warn("deactivating");
                    trexActive = false;
                }
            }
        }

    }
}
