/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto, Margarida Faria
 * May 28, 2012
 */
package pt.up.fe.dceg.neptus.plugins.trex;

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
import java.util.LinkedHashMap;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.fileeditor.SyntaxDocument;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.TrexCommand;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.trex.goals.Surveil;
import pt.up.fe.dceg.neptus.plugins.trex.goals.TagSimulation;
import pt.up.fe.dceg.neptus.plugins.trex.goals.TrexGoal;
import pt.up.fe.dceg.neptus.plugins.trex.goals.VisitLocationGoal;
import pt.up.fe.dceg.neptus.plugins.trex.gui.PortEditor;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "TrexMapLayer", icon = "pt/up/fe/dceg/neptus/plugins/trex/trex.png")
public class TrexMapLayer extends SimpleRendererInteraction implements Renderer2DPainter {
    enum CommsChannel {
        IMC,
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

//    @NeptusProperty(name = "Default speed (m/s)")
//    public double defaultSpeed = 1.25;
//
//    @NeptusProperty(name = "Default tolerance (meters)")
//    public double defaultTolerance = 15;

    private static final long serialVersionUID = 1L;
    Maneuver lastManeuver = null;

    protected LinkedHashMap<String, TrexGoal> sentGoals = new LinkedHashMap<String, TrexGoal>();
    protected LinkedHashMap<String, TrexGoal> completeGoals = new LinkedHashMap<String, TrexGoal>();

    protected LocationType[] surveyArea = new LocationType[4];
    protected boolean surveyEdit = false;
    protected int surveyPos = 0;
    protected boolean active = false;

    /**
     * @param console
     */
    public TrexMapLayer(ConsoleLayout console) {
        super(console);
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
                    xml += "  <Variable name='lat_" + i + "'><float value='" + surveyArea[i].getLatitudeAsDoubleValue()
                            + "'/></Variable>\n";
                    xml += "  <Variable name='lon_" + i + "'><float value='"
                            + surveyArea[i].getLongitudeAsDoubleValue() + "'/></Variable>\n";
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
            addVisitThisPointMenu(popup, loc);
            addTagSimulation(popup, loc);
            addSurvveyAreaMenu(popup);
            addClearGoalMenu(popup);
            popup.addSeparator();
            addDisableTrexMenu(popup);
            addEnableTrexMenu(popup);
            addUAVSurveillance(popup, loc);

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
                TrexCommand cmd = new TrexCommand();
                cmd.setCommand(TrexCommand.COMMAND.ENABLE);
                ImcMsgManager.getManager().sendMessageToSystem(cmd, getConsole().getMainSystem());
                cmd.dump(System.err);
            }
        });
    }

    private void addDisableTrexMenu(JPopupMenu popup) {
        popup.add("Disable TREX").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                TrexCommand cmd = new TrexCommand();
                cmd.setCommand(TrexCommand.COMMAND.DISABLE);
                ImcMsgManager.getManager().sendMessageToSystem(cmd, getConsole().getMainSystem());
                cmd.dump(System.err);
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

    private void addSurvveyAreaMenu(JPopupMenu popup) {
        popup.add("Survey an area").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                surveyArea = new LocationType[4];
                surveyEdit = true;
                surveyPos = 0;
            }
        });
    }

    private void addVisitThisPointMenu(JPopupMenu popup, final LocationType loc) {
        popup.add("Visit this point").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                VisitLocationGoal visitLocationGoal = new VisitLocationGoal(loc.getLatitudeAsDoubleValueRads(), loc
                        .getLongitudeAsDoubleValueRads());
                switch (trexDuneComms) {
                    case IMC:
                        send(visitLocationGoal.asIMCMsg());
                        break;
                    case REST:
                        httpPostTrex(visitLocationGoal);
                        break;
                }
            }
        });
    }

    private void addUAVSurveillance(JPopupMenu popup, final LocationType loc) {
        popup.add("Surveil this point").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                Surveil surveil = new Surveil(loc.getLatitudeAsDoubleValueRads(), loc
                        .getLongitudeAsDoubleValueRads());
                switch (trexDuneComms) {
                    case IMC:
                        send(surveil.asIMCMsg());
                        break;
                    case REST:
                        httpPostTrex(surveil);
                        break;
                }
            }
        });
    }

    private void addTagSimulation(JPopupMenu popup, final LocationType loc) {
        popup.add("Simulate a tag here").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loc.convertToAbsoluteLatLonDepth();
                TagSimulation tagSimulation = new TagSimulation(loc.getLatitudeAsDoubleValueRads(), loc
                        .getLongitudeAsDoubleValueRads());
                httpPostTrex(tagSimulation);
            }

        });
    }

    private void httpPostTrex(TrexGoal goal) {
        try {
            StringEntity message;
            message = new StringEntity(goal.toJson());
            HttpPost httppost = new HttpPost("http://" + ipShore + ":" + portShore + "/rest/goal");
            httppost.setHeader("Content-Type", "application/json");
            httppost.setEntity(message);
            // Execute
            HttpClient httpclient = new DefaultHttpClient();
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

    Image trex = null;

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

        if (active)
            g.drawImage(trex, 5, 50, 32, 32, this);
    }

    public static void main(String[] args) {

        System.out.printf("%x\n", 65094);

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
