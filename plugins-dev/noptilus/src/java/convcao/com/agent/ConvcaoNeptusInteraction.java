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
 * Author: Thanasis
 * Sep 17, 2013
 */
package convcao.com.agent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;

import convcao.com.agent.model.InputData;
import convcao.com.agent.model.TransferData;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.Distance;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.controllers.ControllerManager;
import pt.lsts.neptus.plugins.controllers.IController;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;


/**
 * @author Thanasis, ZP
 *
 */
@PluginDescription(author="thanasis", category=CATEGORY.UNSORTED, name="Convcao Neptus Interaction")
@Popup(accelerator=KeyEvent.VK_N, pos=Popup.POSITION.CENTER, height=500, width=510, name="convcao Neptus Interaction")
public class ConvcaoNeptusInteraction extends ConsolePanel implements Renderer2DPainter, IController, ConfigurationListener {

    private static final long serialVersionUID = -1330079540844029305L;
    private static final double DVL_ANGLE = Math.toRadians(22.5);
    
    @NeptusProperty(name="Used vehicles", description="Identifiers of the vehicles to be used, separated by commas")
    public String controlledVehicles = "lauv-noptilus-1,lauv-noptilus-2";

    @NeptusProperty(name="First Depth", description="Depth for vehicle closer to the surface")
    public double firstVehicleDepth = 1.5;

    @NeptusProperty(name="Depth separation", description="Depth separation between vehicles")
    public double depthIncrements = 1.5;

    @NeptusProperty(name="Control Latency", description="Time, in seconds, between sending vehicle references")
    public int controlLatencySecs = 3;

    @NeptusProperty(name="Control Timeout", description="Time, in seconds, after which the vehicle will stop executing the plan if no references are received")
    public int controlTimeoutSecs = 120;

    @NeptusProperty(name="Use Acoustic Communications", description="Use acoustic communications to transmit desired control references")
    public boolean useAcousticComms = false;

    @NeptusProperty(name="Near distance", description="Distance, in meters, to consider that the vehicles have arrived at desired reference points")
    public double nearDistance = 12.5;

    @NeptusProperty(name="Subtract Tide Level", description="Subtract the tide height when communicating bathymetry data do CONVCAO.")
    public boolean subtractTide = true;

    // Variables declaration - do not modify
    protected int auvs ;
    protected String sessionID = "";
    protected boolean cancel = false;
    protected String report = "";

    //GUI
    protected JButton jButton1;
    protected JButton jButton2;
    protected JLabel jLabel1;
    protected JLabel jLabel2;
    protected JLabel jLabel3;
    protected JLabel jLabel4;
    protected JLabel jLabel5;
    protected JLabel jLabel7;
    protected JLabel jLabel8;
    protected JLabel jLabel9;
    protected JLabel jLabel10;
    protected JLabel jLabel11;
    protected JLabel jLabel12;
    protected JLabel jLabel6;
    protected JPanel jPanel1;
    protected JPanel jPanel2;
    protected JPanel jPanelMain;
    protected JPasswordField jPasswordField1;
    protected JScrollPane jScrollPane1;
    protected JScrollPane jScrollPane2;
    protected JTextArea jTextArea1;
    protected JTextField jTextField1;
    protected JTextPane jTextPane1;
    protected JButton renewButton;
    protected JButton connectButton;
    // End of variables declaration

    private boolean active = false;

    protected ImageIcon runIcon = ImageUtils.getIcon("images/checklists/run.png");
    protected ImageIcon appLogo = ImageUtils.getIcon("images/control-mode/externalApp.png");
    protected ImageIcon noptilusLogo = ImageUtils.getIcon("images/control-mode/noptilus.png");
    protected ControllerManager manager = new ControllerManager();

    // ICONTROLLER METHODS //    
    protected LinkedHashMap<String, LocationType> positions = new LinkedHashMap<>();
    protected LinkedHashMap<String, LocationType> destinations = new LinkedHashMap<>();
    protected LinkedHashMap<String, Double> bathymetry = new LinkedHashMap<>();    
    protected LinkedHashMap<String, Boolean> arrived = new LinkedHashMap<>();
    protected LinkedHashMap<Integer, String> nameTable = new LinkedHashMap<>();
    protected LinkedHashMap<String, Double> depths = new LinkedHashMap<>();
    protected LinkedHashMap<EstimatedState, ArrayList<Distance>> dvlMeasurements = new LinkedHashMap<>();

    protected Thread controlThread = null;

    private int timestep = 1;

    protected NoptilusCoords coords = new NoptilusCoords();

    /**
     * @param console
     */
    public ConvcaoNeptusInteraction(ConsoleLayout console) {
        super(console);
    }

    protected void showText(String text) {
        jTextArea1.append("[" + timestep + "] " + text + "\n");
        while (jTextArea1.getRows() > 50 && jTextArea1.getText().contains("\n"))
            jTextArea1.setText(jTextArea1.getText().substring(jTextArea1.getText().indexOf('\n') + 1));
        jTextArea1.repaint();
        jTextArea1.scrollRectToVisible(new Rectangle(0, jTextArea1.getHeight() + 22, 1, 1));
    }

    @Override
    public void propertiesChanged() {
        manager.setUseAcousticComms(useAcousticComms);

        LinkedHashMap<String, Double> newDepths = new LinkedHashMap<>();
        double depth = firstVehicleDepth;
        for (String v : positions.keySet()) {         
            newDepths.put(v, depth);
            depth += depthIncrements;
        }

        depths = newDepths;
    }

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        Graphics2D g = (Graphics2D) g2.create();
        
        Point2D center = renderer.getScreenPosition(coords.squareCenter);
        double width = renderer.getZoom() * coords.cellWidth * coords.numCols;
        double height = renderer.getZoom() * coords.cellWidth * coords.numRows;
        g.setColor(new Color(0,0,255,64));
        g.translate(center.getX(), center.getY());
        g.rotate(-renderer.getRotation());
        g.fill(new Rectangle2D.Double(-width/2, -height/2, width, height));        
        g.rotate(renderer.getRotation());
        g.translate(-center.getX(), -center.getY());

        if (!active) {
            g.dispose();
            return;
        }

        g.setColor(Color.orange);
        int pos = 50;
        for (String v : nameTable.values()) {
            g.drawString(v+": "+depths.get(v)+"m", 15, pos);
            pos +=20;
        }

        for (String vehicle : nameTable.values()) {
            LocationType src = positions.get(vehicle);
            LocationType dst = destinations.get(vehicle);

            if (!arrived.get(vehicle))
                g.setColor(Color.red.darker());
            else
                g.setColor(Color.green.darker());
            float dash[] = { 4.0f };
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,5.0f, dash, 0.0f));
            g.draw(new Line2D.Double(renderer.getScreenPosition(src), renderer.getScreenPosition(dst)));

            Point2D dstPt = renderer.getScreenPosition(dst);

            if (!arrived.get(vehicle))
                g.setColor(Color.red.darker());
            else
                g.setColor(Color.green.darker());

            g.fill(new Ellipse2D.Double(dstPt.getX()-4, dstPt.getY()-4, 8, 8));
        }
        
        g.dispose();
    }

    public TransferData localState() {
        TransferData data = new TransferData();
        data.timeStep = this.timestep;
        data.SessionID = sessionID;
        data.Bathymeter = new double[auvs];
        data.Location = new double[auvs][3];

        for (int AUV = 0; AUV < auvs; AUV++) {
            String auvName = nameTable.get(AUV);
            double noptDepth = coords.convertWgsDepthToNoptilusDepth(positions.get(auvName).getDepth());
            data.Bathymeter[AUV] = noptDepth-bathymetry.get(auvName);
            double[] nopCoords = coords.convert(positions.get(auvName));
            if (nopCoords == null) {
                GuiUtils.errorMessage(getConsole(), "ConvCAO", auvName+" is outside operating region");
                return null;
            }

            data.Location[AUV][0] = Math.round(nopCoords[0]);
            data.Location[AUV][1] = Math.round(nopCoords[1]);
            data.Location[AUV][2] = (int)noptDepth;                                   
        }

        LinkedHashMap<EstimatedState, ArrayList<Distance>> samples = new LinkedHashMap<>();

        synchronized (dvlMeasurements) {
            samples.putAll(dvlMeasurements);
            dvlMeasurements.clear();
        }

        Vector<double[]> locations = new Vector<>();
        Vector<Double> bathymetry = new Vector<>();

        for (Entry<EstimatedState, ArrayList<Distance>> beams : samples.entrySet() ) {
            for (Distance d : beams.getValue()) {
                double measurement[] = getBeamMeasurement(beams.getKey(), d);
                locations.add(new double[] {measurement[0], measurement[1], measurement[2]});
                bathymetry.add(measurement[3]);
            }
        }
        
        data.MBSamples = new double[bathymetry.size()];
        data.SampleLocations = new double[bathymetry.size()][3];
        
        for (int i = 0; i < bathymetry.size(); i++) {
            data.MBSamples[i] = bathymetry.get(i);
            data.SampleLocations[i] = locations.get(i);
        }
        
        return data;
    }
    
    private double[] getBeamMeasurement(EstimatedState state, Distance d) {
        double[] ret = new double[4];
        
        // H² = Ca² + Cb²
        double varH = d.getValue();
        double varCa = Math.cos(DVL_ANGLE) * varH;
        double varCb = Math.sin(DVL_ANGLE) * varH;
        
        String beamId = d.getEntityName();
        
        if (beamId == null)
            beamId = "DVL Filtered";
        
        double tide = 0;
        
        if (subtractTide) {
            try {
                tide = TidePredictionFactory.getTideLevel(state.getDate());
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }
        }
         
        double depth = state.getDepth();
        double heading = state.getPsi();
        
        /* DVL Beams       
         *   (0) | 
         *       |
         *(3) ---+--- (1)
         *       |
         *       | (2)         
         */
        
        switch (beamId) {
            // pointing down
            case "DVL Filtered":
                varCb = 0;
                varCa = varH;
                break;
            case "DVL Beam 1":
                heading += Math.toRadians(90);
                break;
            case "DVL Beam 2":
                heading += Math.toRadians(180);                
                break;
            case "DVL Beam 3":
                heading += Math.toRadians(270);
                break;
            default:
                break;
        }
        
        LocationType ground = IMCUtils.getLocation(state);
        ground.setOffsetDistance(varCb);
        ground.setAzimuth(Math.toDegrees(heading));
        ground.convertToAbsoluteLatLonDepth();
        
        double c[] = coords.convert(ground);
        ret[0] = c[0];
        ret[1] = c[1];
        ret[2] = state.getDepth();
        ret[3] = varCa + depth - tide;
        
        return ret;
    }

    /**
     * This method is called whenever the AUVs have reached their destinations and new data is to be sent to the server.<br/>
     * As a result, new destinations will be received and vehicles will travel to the new destinations.
     * @throws Exception If there is a problem communicating with the server.
     */
    public void controlLoop() throws Exception {
        TransferData send = localState();
        if (send == null)
            throw new Exception("Unable to compute local state");

        TransferData receive = new TransferData();

        for (int AUV = 0; AUV < auvs; AUV++) {
            showText(nameTable.get(AUV)+" is at "+send.Location[AUV][0]+", "+send.Location[AUV][1]+", "+send.Location[AUV][2]);
        }

        Gson gson = new Gson();
        String json = gson.toJson(send);

        PrintWriter writer = null;
        writer = new PrintWriter(sessionID + "_Data.txt", "UTF-8");
        writer.write(json);
        writer.close();

        NeptusLog.pub().info("uploading to convcao..." + json.toString());
        upload("www.convcao.com", "NEPTUS", "", jTextField1.getText(), new String(jPasswordField1.getPassword()),
                sessionID + "_Data.txt");

        showText("Upload complete, downloading new AUV destinations");

        int receivedTimestep = 0;
        while (!cancel && receivedTimestep < timestep) {            
            Thread.sleep(100);

            try {
                URL url = new URL("http://www.convcao.com/caoagile/FilesFromAgent/NEPTUS/" + sessionID + "_NewActions.txt");
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                conn.connect();
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String jsonClient = in.readLine();
                receive = new Gson().fromJson(jsonClient,TransferData.class);    
                receivedTimestep = receive.timeStep;
            }
            catch(IOException ex) {
                NeptusLog.pub().error(ex);
            }
        }

        showText("Received updated positions from convcao");

        timestep++;

        for (int AUV = 0; AUV < receive.Location.length; AUV++) {
            String name = nameTable.get(AUV);
            LocationType loc = coords.convert(receive.Location[AUV][0], receive.Location[AUV][1]);
            loc.setDepth(firstVehicleDepth+AUV*depthIncrements);//coords.convertNoptilusDepthToWgsDepth(receive.Location[AUV][2]));
            destinations.put(name, loc);
            showText(name+" is being sent to "+receive.Location[AUV][0]+", "+receive.Location[AUV][1]);                        
        }
        // reset arrived states
        for (String auvName : nameTable.values()) {
            arrived.put(auvName, false);
        }

        myDeleteFile(sessionID + "_Data.txt");
    }

    public Thread auvMonitor() {
        Thread auvMon = new Thread("AuvMonitor") {
            public void run() {
                while (!cancel) {
                    updateLocalStructures();
                    boolean arrivedState = true;
                    for (boolean arr : arrived.values())
                        if (!arr)
                            arrivedState = false;

                    try {
                        if (arrivedState) {
                            showText("All vehicles arrived at desired positions. Starting new control sequence");                        
                            try {
                                controlLoop();
                            }
                            catch (InterruptedException e) {
                                NeptusLog.pub().warn("Control thread interrupted");
                                manager.stop();
                                return;
                            }
                            catch (Exception e) {
                                GuiUtils.errorMessage(getConsole(), e);
                                e.printStackTrace();
                                return;
                            }
                        }
                        else {
                            Thread.sleep(1000);
                        }
                    }
                    catch (InterruptedException e) {
                        showText("Control thread interrupted");
                        manager.stop();
                        return;
                    }
                }                
            };
        };
        auvMon.setDaemon(true);
        
        return auvMon;
    }

    private String generateID() {
        Random rnd = new Random(System.currentTimeMillis());
        String ID = "Neptus_User_" + Integer.toString(rnd.nextInt(10000));
        return ID;
    }

    private void renewButtonActionPerformed(ActionEvent evt) {
        sessionID = generateID();

        jTextField1.setEditable(true);
        jPasswordField1.setEditable(true);

        jLabel1.setVisible(true);
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);

        jTextPane1.setText(sessionID);
        connectButton.setEnabled(true);
        jLabel6.setVisible(true);
    }

    private void stopButtonActionPerformed(ActionEvent evt) {
        jLabel10.setText("Please Wait...");
        report = "Canceled";

        cancel = true;
        if (controlThread != null)
            controlThread.interrupt();

        manager.stop();
        jLabel9.setVisible(false);
        jButton1.setEnabled(false);
        jButton2.setEnabled(false);
        connectButton.setEnabled(false);
        renewButton.setEnabled(true);
        report = "";
        jLabel1.setVisible(false);
        jTextArea1.setText("");
        jLabel10.setText("");
        jLabel6.setText("Please Renew your ID to start again");
        timestep = 1;

        active = false;
    }


    private void startButtonActionPerformed(ActionEvent evt) {
        cancel = false;

        jLabel9.setVisible(true);
        jButton2.setEnabled(true);

        jButton1.setEnabled(false);
        renewButton.setEnabled(false);
        connectButton.setEnabled(false);
        timestep = 1;

        showText("Starting FollowReference plans on vehicles");
        active = true;
        try {            
            for (String v : nameTable.values())
                manager.associateControl(this, VehiclesHolder.getVehicleById(v), controlLatencySecs, controlTimeoutSecs);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
            return;
        }

        controlThread = auvMonitor();
        controlThread.start();
    }

    @Subscribe
    public void on(Distance msg) {
        EstimatedState state = ImcMsgManager.getManager().getState(msg.getSourceName()).last(EstimatedState.class);
        
        synchronized (dvlMeasurements) {
            if (!dvlMeasurements.containsKey(state))
                dvlMeasurements.put(state, new ArrayList<Distance>());
            dvlMeasurements.get(state).add(msg);
        }
    }

    private void updateLocalStructures() {
        for (String auvName : nameTable.values()) {            
            EstimatedState state = ImcMsgManager.getManager().getState(auvName).last(EstimatedState.class);
            LocationType auvPosition = IMCUtils.getLocation(state);
            positions.put(auvName, auvPosition);
            double tideOffset = subtractTide ? TidePredictionFactory.currentTideLevel() : 0;
            bathymetry.put(auvName, state.getDepth() + state.getAlt() - tideOffset);
            double dist = auvPosition.getHorizontalDistanceInMeters(destinations.get(auvName));
            if (dist < nearDistance)
                arrived.put(auvName, true);                     
        }   
    }

    private void startLocalStructures(String[] vehicles) throws Exception {
        positions.clear();
        destinations.clear();
        bathymetry.clear();
        nameTable.clear();
        arrived.clear();

        for (String auvName : vehicles) {            
            EstimatedState state = ImcMsgManager.getManager().getState(auvName).last(EstimatedState.class);
            if (state == null)
                throw new Exception("Not able to get initial position for vehicle "+auvName);
            LocationType auvPosition = IMCUtils.getLocation(state);
            positions.put(auvName, auvPosition);
            destinations.put(auvName, auvPosition);
            bathymetry.put(auvName, state.getDepth() + state.getAlt()); // FIXME tide offsets
            arrived.put(auvName, true);
            startControlling(VehiclesHolder.getVehicleById(auvName), ImcMsgManager.getManager().getState(auvName).last(EstimatedState.class));
        }
        int i = 0;
        double depth = firstVehicleDepth;
        for (String v : positions.keySet()) {
            nameTable.put(i++, v);
            depths.put(v, depth);
            depth += depthIncrements;
        }
    }

    private void connectButtonActionPerformed(ActionEvent evt) throws SocketException, IOException {
        String[] vehicles = controlledVehicles.split(",");
        jTextArea1.setText("");
        jTextArea1.repaint();
        showText("Initializing Control Structures");

        try {
            startLocalStructures(vehicles);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
            return;
        }

        auvs = positions.keySet().size();

        showText("Initializing server connection");

        FTPClient client = new FTPClient();

        boolean PathNameCreated=false;
        try {
            client.connect("www.convcao.com", 21);
            client.login(jTextField1.getText(), new String(jPasswordField1.getPassword()));
            PathNameCreated = client.makeDirectory("/NEPTUS/" + sessionID);
            client.logout();

        }
        catch (IOException e) {
            jLabel6.setText("Connection Error");
            throw e;
        }

        showText("Sending session data");
        InputData initialState = new InputData();
        initialState.DateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        initialState.SessionID = sessionID;
        initialState.DemoMode = "1";
        initialState.AUVs = ""+positions.keySet().size();
        String fileName = sessionID + ".txt";

        Gson gson = new Gson();
        String json = gson.toJson(initialState);

        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.write(json);
        writer.close();

        if (PathNameCreated) {
            jLabel6.setText("Connection Established");
            jLabel1.setVisible(true);
            System.out.println("Uploading first file");
            upload("www.convcao.com", "NEPTUS", "", jTextField1.getText(), new String(jPasswordField1.getPassword()),
                    fileName); // send first file
            System.out.println("Uploading second file");
            String mapFxStr = FileUtil.getResourceAsFileKeepName("convcao/com/caoAgent/mapPortoSparse.txt");
            File mapFx = new File(mapFxStr);
            upload("www.convcao.com", "NEPTUS/" + sessionID, mapFx.getParent(),
                    jTextField1.getText(), new String(jPasswordField1.getPassword()), "mapPortoSparse.txt"); // send sparse map
            System.out.println("Both files uploaded");
            jButton1.setEnabled(true);
            jButton2.setEnabled(true);
            jTextPane1.setEditable(false);
            jTextField1.setEditable(false);
            jPasswordField1.setEditable(false);
            connectButton.setEnabled(false);
            renewButton.setEnabled(false);            
        }
        else {
            jLabel6.setText(client.getReplyString());
            jLabel1.setVisible(false);
        }

        myDeleteFile(fileName);      

        showText("ConvCAO control has started");
    }

    private void myDeleteFile(String fileName) {
        File f = new File(fileName);
        //delete File with safety

        // Make sure the file or directory exists and isn't write protected
        if (!f.exists())
            throw new IllegalArgumentException(
                    "Delete: no such file or directory: " + fileName);

        if (!f.canWrite())
            throw new IllegalArgumentException("Delete: write protected: "
                    + fileName);

        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0)
                throw new IllegalArgumentException(
                        "Delete: directory not empty: " + fileName);
        }

        // Attempt to delete it
        boolean success = f.delete();

        if (!success)
            throw new IllegalArgumentException("Delete: deletion failed");

        //end of deleting
    }

    private void upload(String ftpServer, String pathDirectory, String sourcePathDirectory, String userName,
            String password, String filename) {

        FTPClient client = new FTPClient();
        FileInputStream fis = null;

        try {
            client.connect(ftpServer);
            client.login(userName, password);
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            fis = new FileInputStream(sourcePathDirectory+filename);
            client.changeWorkingDirectory("/"+pathDirectory);

            client.storeFile(filename, fis);

            System.out.println("The file "+sourcePathDirectory+" was stored to "+"/"+pathDirectory+"/"+filename);
            client.logout();


            //Report = "File: " + filename + " Uploaded Successfully ";
        }
        catch (Exception exp) {
            exp.printStackTrace();
            report = "Server Error";
            jLabel6.setText(report);
        }
        finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                client.disconnect();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void jLabel1MouseClicked(MouseEvent evt) throws URISyntaxException, IOException {
        if(Desktop.isDesktopSupported() ) {
            Desktop desktop = Desktop.getDesktop();

            if(desktop.isSupported(Desktop.Action.BROWSE) ) {
                URI uri = new URI("http://www.noptilus-fp7.eu/default.asp?node=page&id=321&lng=2");
                desktop.browse(uri);
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        jPanelMain = new JPanel();    
        jPanel1 = new JPanel();
        jPanel2 = new JPanel();
        jLabel2 = new JLabel();
        jScrollPane1 = new JScrollPane();
        jTextPane1 = new JTextPane();
        renewButton = new JButton();
        jLabel4 = new JLabel();
        jTextField1 = new JTextField();
        jLabel5 = new JLabel();
        jPasswordField1 = new JPasswordField();
        connectButton = new JButton();
        jScrollPane2 = new JScrollPane();
        jTextArea1 = new JTextArea();
        jLabel7 = new JLabel();
        jLabel8 = new JLabel();
        jButton1 = new JButton();
        jButton2 = new JButton();
        jLabel1 = new JLabel();
        jLabel9 = new JLabel();
        jLabel10 = new JLabel();
        jLabel11 = new JLabel();
        jLabel12 = new JLabel();
        jLabel6 = new JLabel();
        jLabel3 = new JLabel();

        jLabel11.setIcon(noptilusLogo);

        jLabel12.setHorizontalAlignment(SwingConstants.LEFT);
        jLabel12.setText("<html>www.convcao.com<br>version 0.01</html>");
        jLabel12.setToolTipText("");
        jLabel12.setHorizontalTextPosition(SwingConstants.RIGHT);

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(jLabel11, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 19, Short.MAX_VALUE)
                        .addComponent(jLabel12, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel11, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel12, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                );

        jLabel2.setFont(new Font("Tahoma", 0, 10)); // NOI18N
        jLabel2.setText("Unique ID");

        jTextPane1.setEditable(true);
        jScrollPane1.setViewportView(jTextPane1);
        //jTextPane1.getAccessibleContext().setAccessibleName("");

        renewButton.setText("RENEW");
        renewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                renewButtonActionPerformed(evt);
            }
        });

        jLabel4.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        jLabel4.setText("Username");

        jTextField1.setText("FTPUser");

        jLabel5.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        jLabel5.setText("Password");

        jPasswordField1.setText("FTPUser123");

        connectButton.setText("Connect");
        connectButton.setEnabled(false);
        connectButton.setActionCommand("connect");
        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                try {
                    connectButtonActionPerformed(evt);
                }
                catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane2.setViewportView(jTextArea1);

        jLabel7.setFont(new Font("Tahoma", 0, 12)); // NOI18N
        jLabel7.setText("Command Monitor");

        jButton1.setFont(new Font("Tahoma", 1, 12)); // NOI18N
        jButton1.setText("START");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        jButton2.setFont(new Font("Tahoma", 1, 12)); // NOI18N
        jButton2.setText("STOP");
        jButton2.setEnabled(false);
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        jLabel1.setForeground(new Color(255, 0, 0));
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText("<html>Click HERE to activate the web service using your ID<br>When the web application is ready, press Start </html>");
        jLabel1.setCursor(new Cursor(Cursor.HAND_CURSOR));
        jLabel1.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                try {
                    jLabel1MouseClicked(evt);
                }
                catch (URISyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }   
        });

        //jLabel9.setText("Working...");
        jLabel9.setIcon(runIcon);
        jLabel9.setVisible(false);

        jLabel10.setText("---");

        jLabel6.setForeground(new Color(0, 204, 0));
        jLabel6.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel6.setText("---");

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addContainerGap()
                                        .addComponent(jLabel6, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                .addGap(126, 126, 126)
                                                                .addComponent(jLabel7, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE))
                                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                                        .addGap(23, 23, 23)
                                                                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                                                                .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, 56, GroupLayout.PREFERRED_SIZE)
                                                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                                                .addComponent(jButton1, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                                                                .addGap(29, 29, 29)
                                                                                                .addComponent(jButton2, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE))
                                                                                                .addComponent(jScrollPane2, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 308, GroupLayout.PREFERRED_SIZE))
                                                                                                .addComponent(jLabel10, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 103, GroupLayout.PREFERRED_SIZE)
                                                                                                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 299, GroupLayout.PREFERRED_SIZE))))
                                                                                                .addGap(0, 0, Short.MAX_VALUE))
                                                                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                                                                        .addGap(0, 0, Short.MAX_VALUE)
                                                                                                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                                                                .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                                                                                        .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                                                                                        .addGap(18, 18, 18)
                                                                                                                        .addComponent(jScrollPane1, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                                                                                                        .addGap(18, 18, 18)
                                                                                                                        .addComponent(renewButton))
                                                                                                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                                                                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                                                                                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                                                                                                                .addComponent(jLabel4, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                                                                                                                                .addGap(18, 18, 18)
                                                                                                                                                .addComponent(jTextField1, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE))
                                                                                                                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                                                                                                                        .addComponent(jLabel5, GroupLayout.PREFERRED_SIZE, 64, GroupLayout.PREFERRED_SIZE)
                                                                                                                                                        .addGap(18, 18, 18)
                                                                                                                                                        .addComponent(jPasswordField1)))
                                                                                                                                                        .addGap(14, 14, 14)
                                                                                                                                                        .addComponent(connectButton)))))
                                                                                                                                                        .addContainerGap())
                );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                .addComponent(renewButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jScrollPane1)
                                .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jTextField1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jPasswordField1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(connectButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jLabel6, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel1)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel7, GroupLayout.PREFERRED_SIZE, 20, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jScrollPane2, GroupLayout.PREFERRED_SIZE, 113, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(jButton2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jButton1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(jLabel10, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE)
                                                        .addGap(5, 5, 5))
                );

        jLabel1.getAccessibleContext().setAccessibleName("jLabel1");

        jLabel3.setFont(new Font("Tahoma", 1, 22)); // NOI18N
        jLabel3.setText("Real Time Navigation");

        jLabel8.setIcon(appLogo);

        GroupLayout layout = new GroupLayout(jPanelMain);
        jPanelMain.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel3)
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel8, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 331, GroupLayout.PREFERRED_SIZE)
                                        .addContainerGap())
                );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel8, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addContainerGap())
                );


        addMenuItem("Settings>Noptilus>Coordinate Settings", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(coords, true);
                coords.saveProps();                
            }
        });

        addMenuItem("Settings>Noptilus>ConvCAO Settings", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(ConvcaoNeptusInteraction.this, true);                               
            }
        });

        addMenuItem("Settings>Noptilus>Force vehicle depth", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (positions.isEmpty()) {
                    GuiUtils.errorMessage(getConsole(), "Force vehicle depth", "ConvCAO control is not active");
                    return;
                }
                String[] choices = nameTable.values().toArray(new String[0]);

                String vehicle = (String) JOptionPane.showInputDialog(getConsole(), "Force vehicle depth", "Choose vehicle",
                        JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]); 

                if (vehicle != null) {
                    double depth = depths.get(vehicle);
                    String newDepth = JOptionPane.showInputDialog(getConsole(), "New depth", ""+depth);
                    try {
                        double dd = Double.parseDouble(newDepth);
                        depths.put(vehicle, dd);
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                    }
                }
            }
        });        

        add(jPanelMain);

        renewButtonActionPerformed(null);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeMenuItem("Settings>Noptilus>Coordinate Settings");
        removeMenuItem("Settings>Noptilus>ConvCAO Settings");
        removeMenuItem("Settings>Noptilus>Force vehicle depth");
    }

    @Override
    public String getControllerName() {
        return "convcao";
    }

    @Override
    public Reference guide(VehicleType vehicle, EstimatedState estate, FollowRefState frefState) {

        if (estate.getAlt() != -1) {
            bathymetry.put(vehicle.getId(), estate.getDepth()+estate.getAlt());            
        }
        positions.put(vehicle.getId(), IMCUtils.parseLocation(estate));
        if (!destinations.containsKey(vehicle.getId())) {
            destinations.put(vehicle.getId(), positions.get(vehicle.getId()));
        }

        Reference ref = new Reference();
        ref.setFlags((short)(Reference.FLAG_LOCATION | Reference.FLAG_Z));
        LocationType dest = destinations.get(vehicle.getId());
        float depth = depths.get(vehicle.getId()).floatValue();
        dest.convertToAbsoluteLatLonDepth();
        //System.out.println("depth for "+vehicle.getId()+" is "+depth);
        ref.setLat(dest.getLatitudeRads());
        ref.setLon(dest.getLongitudeRads());
        DesiredZ desZ = new DesiredZ(depth, ZUnits.DEPTH);

        if (depth > 0)
            ref.setRadius(10);

        ref.setZ(desZ);
        System.out.println("Sending this reference to "+vehicle.getId()+":");
        ref.dump(System.out);
        return ref;
    }

    @Override
    public void startControlling(VehicleType vehicle, EstimatedState state) {
        positions.put(vehicle.getId(), IMCUtils.getLocation(state));
        if (state.getAlt() != -1)
            bathymetry.put(vehicle.getId(), state.getDepth()+state.getAlt());
    }

    @Override
    public void stopControlling(VehicleType vehicle) {
        //this.positions.remove(vehicle.getId());
        //this.destinations.remove(vehicle.getId());
        //this.bathymetry.remove(vehicle.getId());
    }

    @Override
    public boolean supportsVehicle(VehicleType vehicle, EstimatedState state) {
        return vehicle.getType().equalsIgnoreCase("uuv") || vehicle.getType().equalsIgnoreCase("auv"); 
    }

    @Override
    public void vehicleTimedOut(VehicleType vehicle) {
        stopControlling(vehicle);
    }
}
