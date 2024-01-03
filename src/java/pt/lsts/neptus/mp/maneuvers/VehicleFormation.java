/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * May 30, 2011
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.TrajectoryPoint;
import pt.lsts.imc.VehicleFormationParticipant;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.gui.VehicleChooser;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class VehicleFormation extends FollowTrajectory {

    /**
     * list of participants
     */
    protected Vector<VehicleType> participants = new Vector<VehicleType>();

    /**
     * Offsets to main trajectory, in meters:
     * <ul>
     * <li>[0] Along-track offset</li>
     * <li>[1] Cross-track offset</li>
     * <li>[2] Depth (positive) offset</li>
     * </ul>
     */
    protected Vector<Double[]> participantOffsets = new Vector<Double[]>();

    /**
     * Start time (required only for rendezvous) in milliseconds since 01-01-1970
     */
    protected long startTime = 0;   



    // GETTERS AND SETTERS //


    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the participantOffsets
     */
    public Vector<Double[]> getParticipantOffsets() {
        return participantOffsets;
    }

    /**
     * @param participantOffsets the participantOffsets to set
     */
    public void setParticipantOffsets(Vector<Double[]> participantOffsets) {
        this.participantOffsets = participantOffsets;
    }

    /**
     * @return the participants
     */
    public Vector<VehicleType> getParticipants() {
        return participants;
    }

    /**
     * @param participants the participants to set
     */
    public void setParticipants(Vector<VehicleType> participants) {
        this.participants = participants;
    }


    @Override
    public Object clone() {
        VehicleFormation clone = new VehicleFormation();
        super.clone(clone);
        clone.speed = getSpeed();
        clone.setManeuverLocation(getManeuverLocation());
        clone.points.addAll(points);                
        clone.setStartTime(getStartTime());
        clone.getParticipants().addAll(getParticipants());
        clone.getParticipantOffsets().addAll(getParticipantOffsets());

        return clone;
    }

    // IMC METHODS //


    /**
     * @return list of participants as an IMC Message
     */
    private Vector<VehicleFormationParticipant> getParticipantsIMC() {
        Vector<VehicleFormationParticipant> msgParticipants = new Vector<>();

        if (getManeuverLocation() == null || points.isEmpty() || participants.size() != participantOffsets.size())
            return msgParticipants;

        for (int i = 0; i < participants.size(); i++) {
            VehicleType participant = participants.get(i);
            Double[] offsets = participantOffsets.get(i);
            if (offsets.length < 3)
                continue;
            VehicleFormationParticipant msg = new VehicleFormationParticipant();
            msg.setVid(participant.getImcId().intValue());
            msg.setOffX(offsets[0]);
            msg.setOffY(offsets[1]);
            msg.setOffZ(offsets[2]);
            msgParticipants.add(msg);
        }

        return msgParticipants;
    }   

    @Override
    public IMCMessage serializeToIMC() {
        double[] lld = getManeuverLocation().getAbsoluteLatLonDepth();

        pt.lsts.imc.VehicleFormation vfMessage = new pt.lsts.imc.VehicleFormation();
        vfMessage.setLat(Math.toRadians(lld[0]));
        vfMessage.setLon(Math.toRadians(lld[1]));
        vfMessage.setZ(getManeuverLocation().getZ());
        vfMessage.setZUnits(ZUnits.valueOf(
                getManeuverLocation().getZUnits().toString()));
        vfMessage.setStartTime(startTime/1000.0);
        vfMessage.setParticipants(getParticipantsIMC());
        speed.setSpeedToMessage(vfMessage);

        // conversion into absolute times
        double[]  absoluteTimes = new double[points.size()];
        double lastTime = 0;
        for (int i = 0; i < points.size(); i++) {
            lastTime += points.get(i)[T];
            absoluteTimes[i] = lastTime;            
        }

        Vector<TrajectoryPoint> trajPoints = new Vector<>();

        for (int i = 0; i < points.size(); i++) {
            TrajectoryPoint point = new TrajectoryPoint();
            point.setX(points.get(i)[X]);
            point.setY(points.get(i)[Y]);
            point.setZ(points.get(i)[Z]);
            point.setT(points.get(i)[T]);
            trajPoints.add(point);
        }

        vfMessage.setCustom(getCustomSettings());
        vfMessage.setPoints(trajPoints);
        return vfMessage;
    }

    /**
     * Parse an IMC message into this object fields
     * @param msg VehicleFormation IMC message to be parsed
     */
    public void parseIMCMessage(IMCMessage msg) {

        pt.lsts.imc.VehicleFormation formation = null;
        try {
            formation = pt.lsts.imc.VehicleFormation.clone(msg);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        super.parseIMCMessage(msg);
        setStartTime((long)(formation.getStartTime() * 1000.0));
        Vector<VehicleFormationParticipant> msgParticipants = formation.getParticipants();

        participants.clear();
        participantOffsets.clear();

        for (VehicleFormationParticipant participant : msgParticipants) {
            try {
                VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(participant.getValue("vid")));
                Double[] offsets = new Double[3];
                offsets[0] = participant.getDouble("off_x");
                offsets[1] = participant.getDouble("off_y");
                offsets[2] = participant.getDouble("off_z");          
                NeptusLog.pub().info("<###> "+vt);
                if (vt != null) {
                    participants.add(vt);
                    participantOffsets.add(offsets);
                }
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        
        
    }

    // XML METHODS //

    @Override
    public Document getManeuverAsDocument(String rootElementName) {    
        Document doc = super.getManeuverAsDocument(rootElementName);
        Element root = doc.getRootElement();
        root.addElement("startTime").setText(""+startTime);        
        Element parts = root.addElement("participants");
        for (int i = 0; i < participants.size(); i++) {
            String veh = participants.get(i).toString();
            Element part = parts.addElement("participant");
            part.setText(veh);
            Double[] offs = participantOffsets.get(i);
            part.addAttribute("xOffset", offs[0].toString());
            part.addAttribute("yOffset", offs[1].toString());
            part.addAttribute("zOffset", offs[2].toString());
        }        
        return doc;
    }


    @Override
    public void loadManeuverFromXML(String xml) {
        super.loadManeuverFromXML(xml);
        try {
            Document doc = DocumentHelper.parseText(xml);

            Node node = doc.selectSingleNode("//startTime");
            startTime = Long.parseLong(node.getText());

            List<?> list = doc.selectNodes("//participants/participant");

            for (Object o : list) {
                Element el = (Element) o;
                Double[] xyz = new Double[3];
                xyz[X] = Double.parseDouble(el.selectSingleNode("@xOffset").getText());
                xyz[Y] = Double.parseDouble(el.selectSingleNode("@yOffset").getText());
                xyz[Z] = Double.parseDouble(el.selectSingleNode("@zOffset").getText());
                participantOffsets.add(xyz);                
                participants.add(VehiclesHolder.getVehicleById(el.getText()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // EDITING INTERACTION //

    protected JFormattedTextField getOffsetTextField() {
        JFormattedTextField tfield = new JFormattedTextField(new DecimalFormat("#0.00"));
        tfield.addFocusListener(new SelectAllFocusListener());
        tfield.setText("0.0");
        return tfield;
    }


    @Override
    public void mouseClicked(MouseEvent event, final StateRenderer2D source) {
        final StateRenderer2D r2d = source;
        final Vector<VehicleType> editedVehicles = new Vector<VehicleType>();


        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            popup.add("Edit participants").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(r2d), "Edit participants");

                    final JPanel main = new JPanel(new GridLayout(0, 5, 2, 2));
                    JPanel panel = new JPanel(new BorderLayout());

                    final Vector<JFormattedTextField> xOffsetsFields = new Vector<JFormattedTextField>();
                    final Vector<JFormattedTextField> yOffsetsFields = new Vector<JFormattedTextField>();
                    final Vector<JFormattedTextField> zOffsetsFields = new Vector<JFormattedTextField>();
                    final Vector<JLabel> vehicleIds = new Vector<JLabel>();
                    final Vector<JButton> removeBtns = new Vector<JButton>();

                    main.add(new JLabel());
                    main.add(new JLabel("Along-track offset", JLabel.CENTER));
                    main.add(new JLabel("Cross-track offset", JLabel.CENTER));
                    main.add(new JLabel("Depth offset", JLabel.CENTER));
                    main.add(new JLabel());

                    for (int i = 0; i < participants.size(); i++) {
                        final String vid = participants.get(i).getId();
                        editedVehicles.add(participants.get(i));
                        vehicleIds.add(new JLabel(vid));                              
                        xOffsetsFields.add(getOffsetTextField());
                        xOffsetsFields.lastElement().setText(""+participantOffsets.get(i)[0]);
                        yOffsetsFields.add(getOffsetTextField());
                        yOffsetsFields.lastElement().setText(""+participantOffsets.get(i)[1]);
                        zOffsetsFields.add(getOffsetTextField());
                        zOffsetsFields.lastElement().setText(""+participantOffsets.get(i)[2]);

                        JButton rmBtn = new JButton("remove");
                        rmBtn.addActionListener(new ActionListener() {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                int id = removeBtns.indexOf(e.getSource());
                                main.remove(vehicleIds.get(id));
                                main.remove(xOffsetsFields.get(id));
                                main.remove(yOffsetsFields.get(id));
                                main.remove(zOffsetsFields.get(id));
                                main.remove((JButton)e.getSource());
                                editedVehicles.remove(id);
                                removeBtns.remove(id);
                                xOffsetsFields.remove(id);
                                yOffsetsFields.remove(id);
                                zOffsetsFields.remove(id);
                                vehicleIds.remove(id);
                                main.invalidate();
                                main.revalidate();
                            }
                        });
                        removeBtns.add(rmBtn);     

                        main.add(vehicleIds.lastElement());
                        main.add(xOffsetsFields.lastElement());
                        main.add(yOffsetsFields.lastElement());
                        main.add(zOffsetsFields.lastElement());                        
                        main.add(rmBtn);
                    }

                    JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
                    JButton btn1 = new JButton("Add Participant");
                    btn1.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {                            

                            VehicleType vt = VehicleChooser.showVehicleDialog(editedVehicles, null, null);
                            if (vt != null) {
                                editedVehicles.add(vt);
                                vehicleIds.add(new JLabel(vt.getId()));                              
                                xOffsetsFields.add(getOffsetTextField());
                                yOffsetsFields.add(getOffsetTextField());
                                zOffsetsFields.add(getOffsetTextField());

                                JButton rmBtn = new JButton("remove");
                                rmBtn.addActionListener(new ActionListener() {

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        int id = removeBtns.indexOf(e.getSource());
                                        main.remove(vehicleIds.get(id));
                                        main.remove(xOffsetsFields.get(id));
                                        main.remove(yOffsetsFields.get(id));
                                        main.remove(zOffsetsFields.get(id));
                                        main.remove((JButton)e.getSource());
                                        editedVehicles.remove(id);
                                        removeBtns.remove(id);
                                        xOffsetsFields.remove(id);
                                        yOffsetsFields.remove(id);
                                        zOffsetsFields.remove(id);
                                        vehicleIds.remove(id);
                                        main.invalidate();
                                        main.revalidate();
                                    }
                                });
                                removeBtns.add(rmBtn);     

                                main.add(vehicleIds.lastElement());
                                main.add(xOffsetsFields.lastElement());
                                main.add(yOffsetsFields.lastElement());
                                main.add(zOffsetsFields.lastElement());                        
                                main.add(rmBtn);

                                main.invalidate();
                                main.revalidate();
                            }
                        }
                    });

                    controls.add(btn1);



                    JButton btn2 = new JButton("Cancel");
                    btn2.addActionListener(new ActionListener() {                        
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            SwingUtilities.getWindowAncestor(((JButton)arg0.getSource())).setVisible(false);
                        }
                    });

                    controls.add(btn2);

                    JButton btn3 = new JButton("OK");
                    btn3.addActionListener(new ActionListener() {                        
                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            participants.clear();
                            participantOffsets.clear();
                            for (int i = 0; i < editedVehicles.size(); i++) {
                                double x = 0, y = 0, z = 0;
                                participants.add(editedVehicles.get(i));
                                try {
                                    x = Double.parseDouble(xOffsetsFields.get(i).getText().replace(',', '.'));
                                    y = Double.parseDouble(yOffsetsFields.get(i).getText().replace(',', '.'));
                                    z = Double.parseDouble(zOffsetsFields.get(i).getText().replace(',', '.'));
                                }
                                catch (Exception e) { e.printStackTrace(); }
                                participantOffsets.add(new Double[] {x,y,z});
                            }
                            SwingUtilities.getWindowAncestor(((JButton)arg0.getSource())).setVisible(false);
                        }
                    });

                    controls.add(btn3);

                    panel.add(main, BorderLayout.CENTER);
                    panel.add(controls, BorderLayout.SOUTH);

                    dialog.getContentPane().add(panel);                    
                    //dialog.setModal(true);
                    dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                    dialog.setSize(600, 180);
                    GuiUtils.centerParent(dialog, SwingUtilities.getWindowAncestor(r2d));
                    dialog.setVisible(true);
                }
            });

            popup.addSeparator();

            popup.add("Edit points as text").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    editPointsDialog(SwingUtilities.getWindowAncestor(source));//(Component)e.getSource()));
                }
            });

            popup.add("Clear trajectory").addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    points.clear();
                    r2d.repaint();
                }
            });

            popup.add("Stop editing "+getId()).addActionListener(new ActionListener() {                
                public void actionPerformed(ActionEvent e) {
                    r2d.setActiveInteraction(null);
                }
            });



            popup.show(source, event.getX(), event.getY());
        }
        else {
            if (event.getClickCount() == 1) {

                Point2D clicked = event.getPoint();
                LocationType curLoc = source.getRealWorldLocation(clicked);
                double distance;

                double[] offsets = source.getRealWorldLocation(clicked).getOffsetFrom(startLoc);

                double xyzt[] = new double[4];
                for (int i = 0; i < 3; i++)
                    xyzt[i] = offsets[i];

                if (hasTime) {
                    // calculate distance and time required from previous point
                    if (previousLoc != null)
                        distance = curLoc.getDistanceInMeters(previousLoc);
                    else 
                        distance = curLoc.getDistanceInMeters(startLoc);
                    xyzt[3] = distance / speed.getMPS();
                }
                else
                    xyzt[3] = -1;

                boolean skip = false;
                if (!event.isAltDown() && !event.isAltGraphDown()) {
                    if (points.size() == 0)
                        points.add(new double[] { 0, 0, 0, hasTime ? 0 : -1 });
                }
                else {
                    if (points.size() > 0) {
                        points.remove(points.size() - 1);
                        skip = true;
                    }
                }

                if (!skip) {
                    points.add(xyzt);
                    previousLoc = curLoc;
                }
                source.repaint();

            }
        }    
        adapter.mouseClicked(event, source);
    }

    public static void main(String[] args) {
        /* ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
        VehicleFormation formation = new VehicleFormation();
        formation.points.add(new double[] {0,1,2,3});
        formation.points.add(new double[] {4,5,6,7});
        formation.startTime = System.currentTimeMillis();
        formation.getParticipants().add(VehiclesHolder.getVehicleById("lauv-seacon-lsts"));
        formation.getParticipants().add(VehiclesHolder.getVehicleById("lauv-seacon-1"));
        formation.getParticipants().add(VehiclesHolder.getVehicleById("swordfish"));
        formation.getParticipantOffsets().add(new Double[] {0d,1d,2d});
        formation.getParticipantOffsets().add(new Double[] {3d,4d,5d});
        formation.getParticipantOffsets().add(new Double[] {6d,7d,8d});
        NeptusLog.pub().info("<###> "+formation.getManeuverAsDocument("VehicleFormation").asXML());
        MissionType mt = new MissionType();
        PlanType plan = new PlanType(mt);
        plan.getGraph().addManeuver(formation);        
        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mt));
        PlanElement pe = new PlanElement(MapGroup.getMapGroupInstance(mt), new MapType());
        pe.setPlan(plan);
        r2d.addPostRenderPainter(pe, "plan");
        r2d.setActiveInteraction(formation);
        GuiUtils.testFrame(r2d);
         */
        VehicleFormation formation = new VehicleFormation();
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(formation.getManeuverAsDocument("VehicleFormation")));
    }
}
