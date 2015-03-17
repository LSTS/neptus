/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Nov 1, 2010
 */
package pt.lsts.neptus.plugins.oplimits;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import pt.lsts.imc.GetOperationalLimits;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.OperationalLimits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.OperationLimits;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Operation Limits", category = CATEGORY.PLANNING, icon = "pt/lsts/neptus/plugins/oplimits/lock.png", documentation = "oplimits/oplimits.html")
public class OperationLimitsSubPanel extends ConsolePanel implements ConfigurationListener,
        MainVehicleChangeListener, NeptusMessageListener, Renderer2DPainter, StateRendererInteraction {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Operation Limits File", description = "Where to store and load operational limits")
    public File operationLimitsFile = new File(".", "conf/oplimits.xml");

    @NeptusProperty(name = "Separate Operational Areas Per Vehicle", description = "If selected, each vehicle will have its own operational limits file")
    public boolean separateOpAreas = true;

    @NeptusProperty
    public boolean showOnMap = true;

    protected byte[] lastMD5 = null;
    protected OperationLimits limits = null;
    protected ToolbarSwitch sw;
    protected InteractionAdapter adapter = new InteractionAdapter(null);
    protected AbstractAction updateAction, editLimits, sendAction, showOpArea, clearRect;
    protected boolean editing = false;
    // rectangle editing variables
    protected PathElement rectangle = null;
    protected ParallelepipedElement pp = null, selection = null;
    protected JDialog parentDialog = null;
    protected LocationType[] points = new LocationType[4];
    protected int clickCount = 0;
    protected Point2D lastDragPoint = null;
    protected boolean dragging = false;

    protected void createActions() {
        editLimits = new AbstractAction(I18n.text("Operation Limits"),
                ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/edit.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                OperationLimitsPanel panel = new OperationLimitsPanel(getConsole().getMission(), true);

                OperationLimits before = OperationLimits.loadXml(limits.asXml());

                panel.setLimits(limits);
                final JDialog dialog = new JDialog(getConsole(), I18n.text("Set Operation Limits"));
                dialog.getContentPane().setLayout(new BorderLayout());
                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                dialog.add(panel);
                JButton okButton = new JButton(new AbstractAction(I18n.text("Ok")) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });
                GuiUtils.reactEnterKeyPress(okButton);
                dialog.getContentPane().add(okButton, BorderLayout.SOUTH);
                dialog.pack();
                GuiUtils.centerParent(dialog, getConsole());
                dialog.setVisible(true);
                int resp = JOptionPane.showConfirmDialog(getConsole(), I18n.text("Do you want to save changes?"),
                        I18n.text("Operation Limits"), JOptionPane.YES_NO_OPTION);
                if (resp == JOptionPane.YES_OPTION) {
                    limits = panel.getLimits();
                    storeXml(limits.asXml());
                    updateAction.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/update_request.png"));
                    updateAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                            I18n.text("Local operational limits have changes"));

                    if (editing)
                        setActive(true, null);

                }
                else {
                    limits.setOpAreaLat(before.getOpAreaLat());
                    limits.setOpAreaLon(before.getOpAreaLon());
                    limits.setOpAreaLength(before.getOpAreaLength());
                    limits.setOpAreaWidth(before.getOpAreaWidth());
                    limits.setOpRotationRads(before.getOpRotationRads());
                }
            }
        };
        sendAction = new AbstractAction(I18n.text("Send to Vehicle"),
                ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/up.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                OperationalLimits msg = getLimitsMessage();

                synchronized (OperationLimitsSubPanel.this) {
                    lastMD5 = msg.payloadMD5();
                    send(msg);
                    send(new GetOperationalLimits());
                }
            }
        };

        updateAction = new AbstractAction(I18n.text("Download limits from vehicle"),
                ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/update_ok.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                updateAction.putValue(AbstractAction.SMALL_ICON,
                        ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/update_request.png"));
                updateAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                        I18n.text("Request sent but limits not yet received"));
                send(IMCDefinition.getInstance().create("GetOperationalLimits"));
            }
        };

        showOpArea = new AbstractAction(I18n.text("Show Operational Area on map"),
                ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/visibility.png")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                showOnMap = ((AbstractButton) e.getSource()).isSelected();
                showOnMap(showOnMap);
            }
        };

        clearRect = new AbstractAction(I18n.text("Clear operational area")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                pp = null;
                rectangle = null;
                setLimitsFromSelection(pp);
                clickCount = 0;
            }
        };
    }

    protected OperationalLimits getLimitsMessage() {

        long bmask = 0;
        OperationalLimits oplimits = new OperationalLimits();

        if (limits.getMaxDepth() != null) {
            bmask = bmask | OperationalLimits.OPL_MAX_DEPTH;
            oplimits.setMaxDepth(limits.getMaxDepth());
        }
        if (limits.getMaxAltitude() != null) {
            bmask = bmask | OperationalLimits.OPL_MAX_ALT;
            oplimits.setMaxAltitude(limits.getMaxAltitude());
        }
        if (limits.getMinAltitude() != null) {
            bmask = bmask | OperationalLimits.OPL_MIN_ALT;
            oplimits.setMinAltitude(limits.getMinAltitude());
        }
        if (limits.getMaxSpeed() != null) {
            bmask = bmask | OperationalLimits.OPL_MAX_SPEED;
            oplimits.setMaxSpeed(limits.getMaxSpeed());
        }
        if (limits.getMinSpeed() != null) {
            bmask = bmask | OperationalLimits.OPL_MIN_SPEED;
            oplimits.setMinSpeed(limits.getMinSpeed());
        }
        if (limits.getMaxVertRate() != null) {
            bmask = bmask | OperationalLimits.OPL_MAX_VRATE;
            oplimits.setMaxVrate(limits.getMaxVertRate());
        }
        if (limits.getOpAreaLat() != null) {
            bmask = bmask | OperationalLimits.OPL_AREA;
            oplimits.setLat(Math.toRadians(limits.getOpAreaLat()));
            oplimits.setLon(Math.toRadians(limits.getOpAreaLon()));
            oplimits.setLength(limits.getOpAreaLength());
            oplimits.setWidth(limits.getOpAreaWidth());
            oplimits.setOrientation(limits.getOpRotationRads());
        }
        oplimits.setMask((short) bmask);

        return oplimits;
    }

    public OperationLimitsSubPanel(ConsoleLayout console) {
        super(console);
        removeAll();
        createActions();
        setSize(new Dimension(95, 40));
        setLayout(new BorderLayout());
        JLabel lbl = new JLabel(I18n.text("Operational Limits"));
        lbl.setFont(new Font("Arial", Font.BOLD, 6));
        add(lbl, BorderLayout.NORTH);

        JPanel tmp = new JPanel();
        tmp.setLayout(new BoxLayout(tmp, BoxLayout.LINE_AXIS));
        tmp.add(new ToolbarButton(editLimits));
        tmp.add(new ToolbarButton(sendAction));
        sw = (ToolbarSwitch) tmp.add(new ToolbarSwitch(showOpArea));
        sw.setSelected(showOnMap);
        tmp.add(new ToolbarButton(updateAction));

        add(tmp, BorderLayout.CENTER);
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "OperationalLimits" };
    }

    @Override
    public void messageArrived(IMCMessage message) {

        final IMCMessage msg = message;
        if (message.getMgid() == OperationalLimits.ID_STATIC) {

            new Thread() {
                @Override
                public void run() {
                    synchronized (OperationLimitsSubPanel.this) {
                        if (lastMD5 != null) {
                            if (!Arrays.equals(msg.payloadMD5(), lastMD5)) {
				int option = JOptionPane.showConfirmDialog(getConsole(),
									   I18n.text("Replace current operational limits with received ones?"),
									   I18n.text("Operation Limits"), JOptionPane.YES_NO_OPTION);
                                if (option != JOptionPane.YES_OPTION)
                                    return;
                            }
                            else {
                                post(Notification.success(I18n.text("Operation Limits"), I18n.text("Syncronized")).src(
                                        getConsole().getMainSystem()));
                            }
                        }
                    }
                    try {
                        OperationalLimits received = OperationalLimits.clone(msg);
                        lastMD5 = msg.payloadMD5();
                        if ((received.getMask() & OperationalLimits.OPL_MAX_DEPTH) != 0)
                            limits.setMaxDepth(received.getMaxDepth());
                        else
                            limits.setMaxDepth(null);

                        if ((received.getMask() & OperationalLimits.OPL_MAX_ALT) != 0)
                            limits.setMaxAltitude(received.getMaxAltitude());
                        else
                            limits.setMaxAltitude(null);

                        if ((received.getMask() & OperationalLimits.OPL_MIN_ALT) != 0)
                            limits.setMinAltitude(received.getMinAltitude());
                        else
                            limits.setMinAltitude(null);

                        if ((received.getMask() & OperationalLimits.OPL_MAX_SPEED) != 0)
                            limits.setMaxSpeed(received.getMaxSpeed());
                        else
                            limits.setMaxSpeed(null);

                        if ((received.getMask() & OperationalLimits.OPL_MIN_SPEED) != 0)
                            limits.setMinSpeed(received.getMinSpeed());
                        else
                            limits.setMinSpeed(null);

                        if ((received.getMask() & OperationalLimits.OPL_MAX_VRATE) != 0)
                            limits.setMaxVertRate(received.getMaxVrate());
                        else
                            limits.setMaxVertRate(null);

                        if ((received.getMask() & OperationalLimits.OPL_AREA) != 0) {

                            limits.setOpAreaLat(Math.toDegrees(received.getLat()));
                            limits.setOpAreaLon(Math.toDegrees(received.getLon()));
                            limits.setOpAreaLength(received.getLength());
                            limits.setOpAreaWidth(received.getWidth());
                            limits.setOpRotationRads(received.getOrientation());
                        }
                        else {
                            limits.setOpAreaLat(null);
                            limits.setOpAreaLon(null);
                            limits.setOpAreaLength(null);
                            limits.setOpAreaWidth(null);
                            limits.setOpRotationRads(null);
                        }

                        pp = getSelectionFromLimits(limits);
                        updateAction.putValue(AbstractAction.SMALL_ICON,
                                ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/update_ok.png"));
                        updateAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                                I18n.text("Download limits from vehicle"));

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }.start();
        }
    }

    protected void showOnMap(boolean show) {
        if (show) {
            Vector<ILayerPainter> renders = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
            for (ILayerPainter str2d : renders)
                str2d.addPostRenderPainter(this, I18n.text("Operational Limits"));
        }
        else {
            Vector<ILayerPainter> renders = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
            for (ILayerPainter str2d : renders)
                str2d.removePostRenderPainter(this);
        }
    }

    protected boolean storeXml(String xml) {
        if (!separateOpAreas && operationLimitsFile.canRead()) {
            NeptusLog.pub().info("<###>saving to " + operationLimitsFile.getAbsolutePath());
            return FileUtil.saveToFile(operationLimitsFile.getAbsolutePath(), xml);
        }
        else if (getConsole().getMainSystem() != null) {
            File f = new File("conf/oplimits/" + getConsole().getMainSystem() + ".xml");
            f.getParentFile().mkdirs();
            NeptusLog.pub().info("<###>saving to " + f.getAbsolutePath());
            return FileUtil.saveToFile(f.getAbsolutePath(), xml);
        }
        else {
            operationLimitsFile = new File("conf/oplimits/limits.xml");
            operationLimitsFile.getParentFile().mkdirs();
            NeptusLog.pub().info("<###>saving to " + operationLimitsFile.getAbsolutePath());
            return FileUtil.saveToFile(operationLimitsFile.getAbsolutePath(), xml);
        }
    }

    protected String getOpLimitsXml() {
        if (!separateOpAreas && operationLimitsFile.canRead())
            return FileUtil.getFileAsString(operationLimitsFile.getAbsolutePath());
        else if (getConsole().getMainSystem() != null) {
            File f = new File("conf/oplimits/" + getConsole().getMainSystem() + ".xml");
            if (f.canRead())
                return FileUtil.getFileAsString(f);
        }
        return new OperationLimits().asXml();
    }

    @Override
    public void initSubPanel() {
        limits = OperationLimits.loadXml(getOpLimitsXml());
        showOnMap(showOnMap);

        Vector<CustomInteractionSupport> panels = getConsole().getSubPanelsOfInterface(CustomInteractionSupport.class);
        for (CustomInteractionSupport cis : panels)
            cis.addInteraction(this);

    }

    @Override
    public void propertiesChanged() {
        boolean previously = sw.isSelected();
        sw.setSelected(showOnMap);
        if (previously != sw.isSelected())
            showOnMap(showOnMap);
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        showOnMap(false);

        pp = getSelectionFromLimits(limits);
        clickCount = 0;
        // add oplimits rendererd (with new limits) if necessary
        showOnMap(showOnMap);
        updateAction.actionPerformed(null);
    }

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }

    @Override
    public Cursor getMouseCursor() {
        return adapter.getMouseCursor();
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        adapter.keyPressed(event, source);
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        adapter.keyReleased(event, source);
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        adapter.mouseClicked(event, source);
        boolean handled = false;
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            popup.add(editLimits);
            popup.add(clearRect);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(showOpArea);
            popup.add(item);
            item.setSelected(showOnMap);
            popup.addSeparator();
            popup.add(sendAction);
            popup.add(updateAction);
            handled = true;
            popup.show(source, event.getX(), event.getY());
        }
        else {            
            if (rectangle == null) {
                points[0] = source.getRealWorldLocation(event.getPoint());
                rectangle = new PathElement(source.getMapGroup(), null, points[0]);
                rectangle.setShape(true);
                rectangle.setFinished(true);
                rectangle.setStroke(new BasicStroke(2.0f));
                rectangle.addPoint(0, 0, 0, false);
                clickCount = 1;
                handled = true;
            }
            else if (clickCount == 1) {
                clickCount++;
                points[1] = source.getRealWorldLocation(event.getPoint());
                double[] offsets = points[1].getOffsetFrom(rectangle.getCenterLocation());
                rectangle.addPoint(offsets[1], offsets[0], 0, false);
                handled = true;
            }
            else if (clickCount == 2) {
                clickCount++;
                LocationType loc = source.getRealWorldLocation(event.getPoint());
                double[] offsets = loc.getOffsetFrom(rectangle.getCenterLocation());
                double[] offsets2 = points[1].getOffsetFrom(points[0]);

                double px = offsets[1];
                double py = offsets[0];

                double lx1 = 0;
                double ly1 = 0;
                double lx2 = offsets2[1];
                double ly2 = offsets2[0];

                double angle = points[0].getXYAngle(points[1]) + Math.PI / 2;
                double dist = MathMiscUtils.pointLineDistance(px, py, lx1, ly1, lx2, ly2);

                points[2] = new LocationType(points[0]);
                points[3] = new LocationType(points[1]);
                points[2].translatePosition(-Math.cos(angle) * dist, -Math.sin(angle) * dist, 0);
                points[3].translatePosition(-Math.cos(angle) * dist, -Math.sin(angle) * dist, 0);

                double inc = Math.PI / 2;

                if ((int) angle != (int) points[2].getXYAngle(loc))
                    inc = 3 * Math.PI / 2;

                rectangle.addPoint(lx2 + Math.sin(angle + inc) * dist, ly2 + Math.cos(angle + inc) * dist, 0, false);
                rectangle.addPoint(Math.sin(angle + inc) * dist, Math.cos(angle + inc) * dist, 0, false);

                pp = new ParallelepipedElement(null, null);
                pp.setCenterLocation(RectangleEditor.centroid(points));
                pp.setWidth(points[0].getDistanceInMeters(points[1]));
                pp.setLength(points[0].getDistanceInMeters(points[2]));
                pp.setHeight(0);
                pp.setYaw(Math.toDegrees(points[0].getXYAngle(points[2])));
                pp.setMyColor(Color.red);

                // special case...
                double d = RectangleEditor.centroid(points).getDistanceInMeters(loc)
                        - RectangleEditor.centroid(points[0], points[1]).getDistanceInMeters(loc);
                if (d > 0)
                    pp.setCenterLocation(new LocationType(pp.getCenterLocation().translatePosition(
                            points[0].getOffsetFrom(points[2]))));
                setLimitsFromSelection(pp);
                handled = true;

            }            
            repaint();
        }
        if (!handled)
            adapter.mouseClicked(event, source);        
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        adapter.keyTyped(event, source);
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (dragging) {
            double my = event.getPoint().getY() - lastDragPoint.getY();

            if (event.isShiftDown())
                pp.rotateRight(my);
            else {
                LocationType prev = source.getRealWorldLocation(lastDragPoint);
                LocationType now = source.getRealWorldLocation(event.getPoint());
                double[] offsets = now.getOffsetFrom(prev);
                pp.getCenterLocation().translatePosition(offsets[0], offsets[1], 0);

            }
            source.repaint();
        }
        else {
            //if (!event.isShiftDown())
            //    adapter.mouseDragged(event, source);
            adapter.mouseDragged(event, source);
        }
        lastDragPoint = event.getPoint();
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        boolean handled = false;
        if (editing) {
            lastDragPoint = event.getPoint();
            if (pp != null && pp.containsPoint(source.getRealWorldLocation(lastDragPoint), source)) {
                dragging = true;
                handled = true;
            }
        }
        if (!handled)
            adapter.mousePressed(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        adapter.mouseMoved(event, source);
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        adapter.mouseReleased(event, source);
        if (dragging) {
            dragging = false;
            setLimitsFromSelection(pp);            
        }
        lastDragPoint = null;

    }
    
    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
        adapter.mouseExited(event, source);
    }
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
        adapter.wheelMoved(event, source);
    }

    protected JLabel label = new JLabel("<html></html>");
    {
        label.setOpaque(true);
        label.setBorder(new EmptyBorder(3, 3, 3, 3));
        label.setBackground(new Color(255, 255, 255, 128));
    }

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        Graphics2D g = (Graphics2D) g2.create();
        if (limits != null && !editing) {
            limits.paint(g, renderer);
        }
        else {
            if (limits != null) {
                NumberFormat nf = GuiUtils.getNeptusDecimalFormat(1);
                StringBuilder sb = new StringBuilder("<html><h3>" + I18n.text("Operational Limits")
                        + "</h3><font color='red'>");

                if (limits.getMaxDepth() != null && !limits.getMaxDepth().isNaN())
                    sb.append(I18n.text("Max Depth") + ": <b>" + nf.format(limits.getMaxDepth()) + " m</b><br>");
                if (limits.getMaxAltitude() != null && !limits.getMaxAltitude().isNaN())
                    sb.append(I18n.text("Max Altitude") + ": <b>" + nf.format(limits.getMaxAltitude()) + " m</b><br>");
                if (limits.getMinAltitude() != null && !limits.getMinAltitude().isNaN())
                    sb.append(I18n.text("Min Altitude") + ": <b>" + nf.format(limits.getMinAltitude()) + " m</b><br>");
                if (limits.getMinSpeed() != null && !limits.getMinSpeed().isNaN())
                    sb.append(I18n.text("Min Speed") + ": <b>" + nf.format(limits.getMinSpeed()) + " m/s</b><br>");
                if (limits.getMaxSpeed() != null && !limits.getMaxSpeed().isNaN())
                    sb.append(I18n.text("Max Speed") + ": <b>" + nf.format(limits.getMaxSpeed()) + " m/s</b><br>");
                sb.append("</font></html>");

                label.setText(sb.toString());
                Dimension dim = label.getPreferredSize();
                label.setBounds(10, 10, (int) dim.getWidth(), (int) dim.getHeight());
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.translate(10, 10);
                label.paint(g2d);
            }
            if (clickCount == 1) {
                g.setColor(Color.red.darker());
                Point2D pt = renderer.getScreenPosition(points[0]);
                g.draw(new Line2D.Double(pt.getX() - 5, pt.getY() - 5, pt.getX() + 5, pt.getY() + 5));
                g.draw(new Line2D.Double(pt.getX() - 5, pt.getY() + 5, pt.getX() + 5, pt.getY() - 5));
                g.dispose();
                return;
            }

            if (pp != null) {
                pp.paint((Graphics2D) g.create(), renderer, -renderer.getRotation());
            }
            else if (rectangle != null) {
                rectangle.setMyColor(Color.red);
                rectangle.setFill(true);
                rectangle.paint((Graphics2D) g.create(), renderer, -renderer.getRotation());
            }
            g.dispose();
        }
    }

    public OperationLimits setLimitsFromSelection(ParallelepipedElement selection) {

        if (selection == null) {
            limits.setOpAreaLat(null);
            limits.setOpAreaLon(null);
            limits.setOpRotationRads(null);
            limits.setOpAreaWidth(null);
            limits.setOpAreaLength(null);
        }
        else {
            double lld[] = selection.getCenterLocation().getAbsoluteLatLonDepth();

            limits.setOpAreaLat(lld[0]);
            limits.setOpAreaLon(lld[1]);
            limits.setOpAreaLength(selection.getLength());
            limits.setOpAreaWidth(selection.getWidth());
            limits.setOpRotationRads(selection.getYawRad());
        }
        byte[] newMD5 = getLimitsMessage().payloadMD5();
        if (lastMD5 == null || !ByteUtil.equal(newMD5, lastMD5)) {
            lastMD5 = getLimitsMessage().payloadMD5();
            updateAction.putValue(AbstractAction.SMALL_ICON,
                    ImageUtils.getIcon("pt/lsts/neptus/plugins/oplimits/update_request.png"));
            updateAction.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Local operational limits have changes"));
        }

        return limits;
    }

    public ParallelepipedElement getSelectionFromLimits(OperationLimits limits) {
        if (limits.getOpAreaLat() == null)
            pp = null;
        else {
            pp = new ParallelepipedElement(null, null);
            pp.setWidth(limits.getOpAreaWidth());
            pp.setLength(limits.getOpAreaLength());
            pp.setYawDeg(Math.toDegrees(limits.getOpRotationRads()));
            LocationType lt = new LocationType();
            lt.setLatitudeDegs(limits.getOpAreaLat());
            lt.setLongitudeDegs(limits.getOpAreaLon());
            pp.setCenterLocation(lt);
            pp.setMyColor(Color.red);
        }

        if (pp == null)
            clickCount = 0;
        return pp;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        showOnMap(mode == false ? sw.getState() : mode);

        sw.setEnabled(!mode);
        adapter.setActive(mode, source);
        editing = mode;
        if (editing && limits != null) {
            getSelectionFromLimits(limits);
        }
        if (!editing && pp != null) {
            setLimitsFromSelection(pp);
        }
        repaint();
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        adapter.paintInteraction(g, source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
