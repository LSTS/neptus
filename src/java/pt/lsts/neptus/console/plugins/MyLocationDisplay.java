/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/05/02
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", name = "MyLocationDisplay", version = "1.3.1", icon = "images/myloc.png", description = "My location display.", documentation = "my-location/my-location.html")
@LayerPriority(priority = 182)
public class MyLocationDisplay extends ConsolePanel implements Renderer2DPainter,
        IEditorMenuExtension, ConfigurationListener, SubPanelChangeListener, MissionChangeListener {

    private final Icon ICON = ImageUtils.getScaledIcon("images/myloc.png", 24, 24);

    private static int secondsToDisplayRanges = 30;

    private final Color orangeNINFO = new Color(230, 121, 56);

    @NeptusProperty(name = "Location", userLevel = LEVEL.ADVANCED)
    public LocationType location = MyState.getLocation();

    @NeptusProperty(name = "My Heading", userLevel = LEVEL.REGULAR)
    public double headingDegrees = 0;

    @NeptusProperty(name = "Use configured My Heading", userLevel = LEVEL.REGULAR)
    public boolean useConfiguredMyHeading = false;

    @NeptusProperty(name = "Follow Position Of", editable = false, 
            category = "Follow System", userLevel = LEVEL.ADVANCED,
            description ="Uses position and heading of other system as mine.")
    private String followPositionOf = "";

    @NeptusProperty(name = "Follow Heading Of", editable = false, 
            category = "Follow System", userLevel = LEVEL.ADVANCED,
            description ="Uses heading of other system as mine.")
    private String followHeadingOf = "";

    @NeptusProperty(name = "Follow Heading Of with Angle Offset", editable = false, 
            category = "Follow System", userLevel = LEVEL.ADVANCED,
            description ="Adds an angle offset to the heading of other system.")
    private short followHeadingOfAngleOffset = 0;

    @NeptusProperty(name = "Use System to Derive Heading", editable = false, 
            category = "Derive Heading", userLevel = LEVEL.ADVANCED,
            description = "Uses the angle between me and another system to derive mine heading.")
    private String useSystemToDeriveHeading = "";

    @NeptusProperty(name = "Angle Offset from Front to Derived Heading", editable = false, 
            category = "Derive Heading", userLevel = LEVEL.ADVANCED,
            description = "This is the angle offset between what you consider front and the line from you to derive heading system. (Clockwise positive angle.)")
    private short angleOffsetFromFrontToDerivedHeading = 0;

    @NeptusProperty(name = "Angle Offset from Front to Where the Operator is Looking", editable = false, 
            category = "Derive Heading", userLevel = LEVEL.ADVANCED,
            description = "This is the angle offset between what you consider front and where the operator is looking. (Clockwise positive angle.)")
    private short angleOffsetFromFrontToWhereTheOperatorIsLooking = 0;
    
    @NeptusProperty(name = "Offset from center in width in meters (from following system, positive right)", editable = true, 
            category = "Dimension", userLevel = LEVEL.ADVANCED,
            description = "")
    private double widthOffsetFromCenter = 0;
    @NeptusProperty(name = "Offset from center in length in meters (from following system, positive front)", editable = true, 
            category = "Dimension", userLevel = LEVEL.ADVANCED,
            description = "")
    private double lengthOffsetFromCenter = 0;

    @NeptusProperty(name = "Length", category = "Dimension", userLevel = LEVEL.REGULAR)
    public double length = 0;

    @NeptusProperty(name = "Width", category = "Dimension", userLevel = LEVEL.REGULAR)
    public double width = 0;
    
    @NeptusProperty(name = "Draught", category = "Dimension", userLevel = LEVEL.REGULAR)
    public double draught = 0;
    
    @NeptusProperty(name = "Show Data Source", editable = true,
            userLevel = LEVEL.ADVANCED,
            description ="Show position and heading sources on the screen.")
    private boolean enableShowDataSource = false;

    private long lastCalcPosTimeMillis = -1;

    private static GeneralPath arrowShape;
    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();
    private Vector<IMapPopup> renderersPopups = new Vector<IMapPopup>();

    protected GeneralPath myShape = new GeneralPath();

    private boolean recalcRqst;
    
    private ScheduledExecutorService executer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        String nameBase = new StringBuilder().append(MyLocationDisplay.class.getSimpleName())
                .append("::").append(Integer.toHexString(MyLocationDisplay.this.hashCode()))
                        .append("::").toString();
        ThreadGroup group = new ThreadGroup(nameBase);
        AtomicLong c = new AtomicLong(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread td = new Thread(group, r, nameBase + c.getAndIncrement());
            td.setDaemon(true);
            return td;
        }
    });
    
    {
        myShape.moveTo(0, 5);
        myShape.lineTo(-5, 3.5);
        myShape.lineTo(-5, -5);
        myShape.lineTo(5, -5);
        myShape.lineTo(5, 3.5);
        myShape.lineTo(0, 5);
        myShape.closePath();
    }

    public MyLocationDisplay(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        setVisibility(false);
        location = MyState.getLocation();
        headingDegrees = MyState.getAxisAnglesDegrees()[2];
        length = MyState.getLength();
        width = MyState.getWidth();
        lengthOffsetFromCenter = MyState.getLengthOffsetFromCenter();
        widthOffsetFromCenter = MyState.getWidthOffsetFromCenter();
        draught = MyState.getDraught();
    }

    @Override
    public void initSubPanel() {
        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : renderersPopups) {
            str2d.addMenuExtension(this);
        }

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, I18n.text("My location"));
        }
        
        executer.scheduleAtFixedRate(this::update, 5000, 500, TimeUnit.MILLISECONDS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        executer.shutdown();
    }

    public boolean update() {
        location = MyState.getLocation();
        LocationType newLocation = location;
        if (!useConfiguredMyHeading)
            headingDegrees = MyState.getHeadingInDegrees();
        double newHeadingDegrees = headingDegrees;
        lastCalcPosTimeMillis = MyState.getLastLocationUpdateTimeMillis();
        length = MyState.getLength();
        width = MyState.getWidth();
        lengthOffsetFromCenter = MyState.getLengthOffsetFromCenter();
        widthOffsetFromCenter = MyState.getWidthOffsetFromCenter();
        draught = MyState.getDraught();
        
        boolean updateLocation = false;
        boolean updateHeading = false;
        boolean updateOffsetsInPos = false;
        boolean updateSize = false;
        boolean updateSizeOffsets = false;
        boolean updateDraught = false;

        // update pos if following system
        if (followPositionOf != null && followPositionOf.length() != 0) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(followPositionOf);
            LocationType loc = null;
            long locTime = -1;
            if (!useConfiguredMyHeading)
                headingDegrees = 0;
            long headingDegreesTime = -1;
            if (sys != null) {
                loc = sys.getLocation();
                if (loc != null) {
                    loc = loc.getNewAbsoluteLatLonDepth();
                    updateOffsetsInPos = true;
                }
                locTime = sys.getLocationTimeMillis();
                if (!useConfiguredMyHeading) {
                    headingDegrees = sys.getYawDegrees();
                    headingDegreesTime = sys.getAttitudeTimeMillis();
                }
            }
            else {
                ExternalSystem ext = ExternalSystemsHolder.lookupSystem(followPositionOf);
                if (ext != null) {
                    loc = ext.getLocation();
                    loc = loc.getNewAbsoluteLatLonDepth();
                    updateOffsetsInPos = true;
                    locTime = ext.getLocationTimeMillis();
                    if (!useConfiguredMyHeading) {
                        headingDegrees = ext.getYawDegrees();
                        headingDegreesTime = ext.getAttitudeTimeMillis();
                    }
                    
                    Object obj = ext.retrieveData(SystemUtils.WIDTH_KEY);
                    if (obj != null) {
                        double width = ((Number) obj).doubleValue();
                        obj = ext.retrieveData(SystemUtils.LENGHT_KEY);
                        if (obj != null) {
                            double length = ((Number) obj).doubleValue();

                            double widthOffsetFromCenter = 0;
                            obj = ext.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY);
                            if (obj != null)
                                widthOffsetFromCenter = ((Number) obj).doubleValue();
                            double lengthOffsetFromCenter = 0;
                            obj = ext.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY);
                            if (obj != null)
                                lengthOffsetFromCenter = ((Number) obj).doubleValue();

                            if (length != 0) {
                                this.length = length;
                                updateSize = true;
                            }
                            if (width != 0) {
                                this.width = width;
                                updateSize = true;
                            }
                            if (lengthOffsetFromCenter != 0) {
                                this.lengthOffsetFromCenter = lengthOffsetFromCenter;
                                updateSizeOffsets = true;
                            }
                            if (widthOffsetFromCenter != 0) {
                                this.widthOffsetFromCenter = widthOffsetFromCenter;
                                updateSizeOffsets = true;
                            }
                        }
                        
                        obj = ext.retrieveData(SystemUtils.DRAUGHT_KEY);
                        if (obj != null) {
                            double draught = ((Number) obj).doubleValue();
                            if (draught != 0) {
                                this.draught = draught;
                                updateDraught = true;
                            }
                        }
                    }
                }
            }
            if (recalcRqst || loc != null && locTime - lastCalcPosTimeMillis > 0) {
                updateLocation = true;
                newLocation = loc;
            }
            // If the time of update of heading is old or we are using other way to calculate heading, don't updated
            if (recalcRqst || headingDegreesTime - lastCalcPosTimeMillis > 0
                    && !(isFollowingHeadingOfFilled() || isSystemToDeriveHeadingFilled())) {
                updateHeading = true;
                newHeadingDegrees = headingDegrees;
            }
            recalcRqst = false;
        }
        
        // update just heading if following system
        if (!useConfiguredMyHeading && isFollowingHeadingOfFilled()) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(followHeadingOf);
            double headingDegrees = 0;
            long headingDegreesTime = -1;
            if (sys != null) {
                headingDegrees = sys.getYawDegrees();
                headingDegreesTime = sys.getAttitudeTimeMillis();
            }
            else {
                ExternalSystem ext = followHeadingOf == null || followHeadingOf.isEmpty() ? null : ExternalSystemsHolder.lookupSystem(followHeadingOf);
                if (ext != null) {
                    headingDegrees = ext.getYawDegrees();
                    headingDegreesTime = ext.getAttitudeTimeMillis();
                }
            }
            if (headingDegreesTime - lastCalcPosTimeMillis > 0) {
                updateHeading = true;
                newHeadingDegrees = headingDegrees + followHeadingOfAngleOffset;
            }
        }
        else if (!useConfiguredMyHeading && isSystemToDeriveHeadingFilled()) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(useSystemToDeriveHeading);
            LocationType loc = null;
            // long locTime = -1;
            if (sys != null) {
                loc = sys.getLocation();
                // locTime = sys.getLocationTimeMillis();
            }
            else {
                ExternalSystem ext = ExternalSystemsHolder.lookupSystem(useSystemToDeriveHeading);
                if (ext != null) {
                    loc = ext.getLocation();
                    // locTime = ext.getLocationTimeMillis();
                }
            }
            if (loc != null) {
                double[] bearingRange = CoordinateUtil.getNEBearingDegreesAndRange(newLocation, loc);
                bearingRange[0] += -angleOffsetFromFrontToDerivedHeading + angleOffsetFromFrontToWhereTheOperatorIsLooking;
                // if (Math.abs(locTime - lastCalcPosTimeMillis) < DateTimeUtil.MINUTE * 5) {
                updateHeading = true;
                newHeadingDegrees = bearingRange[0];
                // }
            }
        }

        if (useConfiguredMyHeading) {
            updateHeading = true;
            newHeadingDegrees = headingDegrees;
        }
        
        if (updateHeading)
            headingDegrees = newHeadingDegrees;

        if (updateLocation && updateOffsetsInPos) {
//            double oLength = -lengthOffsetFromCenter;
//            double oWidth = -widthOffsetFromCenter;
//            double oN = oLength * Math.cos(Math.toRadians(headingDegrees)) + oWidth * Math.cos(Math.toRadians(headingDegrees + 90));
//            double oE = oLength * Math.sin(Math.toRadians(headingDegrees)) + oWidth * Math.sin(Math.toRadians(headingDegrees + 90));
//            newLocation.translatePosition(oN, oE, 0).convertToAbsoluteLatLonDepth();
        }
        
        if (updateLocation && updateHeading)
            MyState.setLocationAndAxis(newLocation, AngleUtils.nomalizeAngleDegrees360(newHeadingDegrees));
        else if (updateLocation)
            MyState.setLocation(newLocation);
        else if (updateHeading)
            MyState.setHeadingInDegrees(AngleUtils.nomalizeAngleDegrees360(newHeadingDegrees));
        
        if (updateSize) {
            MyState.setLength(length);
            length = MyState.getLength();
            MyState.setWidth(width);
            width = MyState.getWidth();
        }

        if (updateSizeOffsets) {
            MyState.setLengthOffsetFromCenter(lengthOffsetFromCenter);
            lengthOffsetFromCenter = MyState.getLengthOffsetFromCenter();
            MyState.setWidthOffsetFromCenter(widthOffsetFromCenter);
            widthOffsetFromCenter = MyState.getWidthOffsetFromCenter();
        }
        
        if (updateDraught) {
            MyState.setDraught(draught);
            draught = MyState.getDraught();
        }

        return true;
    }

    private boolean isSystemToDeriveHeadingFilled() {
        return useSystemToDeriveHeading != null && !useSystemToDeriveHeading.isEmpty();
    }

    private boolean isFollowingHeadingOfFilled() {
        return followHeadingOf != null && !followHeadingOf.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        // if (!showInRender)
        // return;
        double alfaPercentage = 1;
        int dt = 3;
        long deltaTimeMillis = System.currentTimeMillis() - lastCalcPosTimeMillis;
        if (deltaTimeMillis > secondsToDisplayRanges * 1000.0) {
            // alfaPercentage = 0.5;
            dt = 0;
        }
        else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 2.0) {
            // alfaPercentage = 0.5;
            dt = 1;
        }
        else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 4.0) {
            // alfaPercentage = 0.7;
            dt = 2;
        }
        double rotationAngle = renderer.getRotation();
        Point2D centerPos = renderer.getScreenPosition(new LocationType(location));

        Color color = orangeNINFO;
        // Paint system loc
        Graphics2D g = (Graphics2D) g2.create();
        g.setStroke(new BasicStroke(2));
        
        { // Paint the vessel icon
            double diameter = Math.max(length, width);
            if (diameter > 0) {
                Graphics2D gt = (Graphics2D) g.create();

                double scaleX = (renderer.getZoom() / 10) * width;
                double scaleY = (renderer.getZoom() / 10) * length;

                diameter = diameter * renderer.getZoom();
                Color colorCircle = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage));
                gt.setColor(colorCircle);
                // gt.draw(new Ellipse2D.Double(centerPos.getX() - diameter / 2, centerPos.getY() - diameter / 2, diameter, diameter));

                gt.translate(centerPos.getX(), centerPos.getY());
                gt.rotate(Math.PI + Math.toRadians(headingDegrees) - renderer.getRotation());
                if (!useConfiguredMyHeading && isSystemToDeriveHeadingFilled()) {
                    gt.rotate(Math.toRadians(-angleOffsetFromFrontToWhereTheOperatorIsLooking));
                }
                else if (!useConfiguredMyHeading && isFollowingHeadingOfFilled()) {
                    gt.rotate(Math.toRadians(-followHeadingOfAngleOffset));
                }

                gt.draw(new Ellipse2D.Double(-diameter / 2 + widthOffsetFromCenter * renderer.getZoom() / 2.,
                        -diameter / 2 - lengthOffsetFromCenter * renderer.getZoom() / 2., diameter, diameter));
                gt.translate(widthOffsetFromCenter * renderer.getZoom() / 2., -lengthOffsetFromCenter * renderer.getZoom() /2.);
                
                gt.scale(scaleX, scaleY);
                gt.fill(myShape);
                
                gt.dispose();
            }
        }

        if (dt > 0) {
            g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
            // g.draw(new Ellipse2D.Double(centerPos.getX()-10,centerPos.getY()-10,20,20));
            g.draw(new Arc2D.Double(centerPos.getX() - 12, centerPos.getY() - 12, 24, 24, -30, 60, Arc2D.OPEN));
            g.draw(new Arc2D.Double(centerPos.getX() - 12, centerPos.getY() - 12, 24, 24, -30 + 180, 60, Arc2D.OPEN));
            // g.setColor(new Color(255, 200, 0, (int) (255 * alfaPercentage)));
            color = orangeNINFO;// new Color(255, 255, 0).brighter();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * alfaPercentage)));
            // g.draw(new Ellipse2D.Double(centerPos.getX()-12,centerPos.getY()-12,24,24));
            g.draw(new Arc2D.Double(centerPos.getX() - 14, centerPos.getY() - 14, 28, 28, -30, 60, Arc2D.OPEN));
            g.draw(new Arc2D.Double(centerPos.getX() - 14, centerPos.getY() - 14, 28, 28, -30 + 180, 60, Arc2D.OPEN));
        }
        if (dt > 1) {
            g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
            // g.draw(new Ellipse2D.Double(centerPos.getX()-14,centerPos.getY()-14,28,28));
            g.draw(new Arc2D.Double(centerPos.getX() - 16, centerPos.getY() - 16, 32, 32, -30, 60, Arc2D.OPEN));
            g.draw(new Arc2D.Double(centerPos.getX() - 16, centerPos.getY() - 16, 32, 32, -30 + 180, 60, Arc2D.OPEN));
            color = orangeNINFO;// new Color(255, 255, 0).brighter();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * alfaPercentage)));
            g.draw(new Arc2D.Double(centerPos.getX() - 18, centerPos.getY() - 18, 36, 36, -30, 60, Arc2D.OPEN));
            g.draw(new Arc2D.Double(centerPos.getX() - 18, centerPos.getY() - 18, 36, 36, -30 + 180, 60, Arc2D.OPEN));
        }

        g.translate(centerPos.getX(), centerPos.getY());
        color = orangeNINFO;// new Color(255, 255, 0).darker();
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * alfaPercentage));
        g.setColor(color);
        g.fill(new Ellipse2D.Double(-9, -9, 18, 18));
        // g.setColor(new Color(255, 255, 0, (int) (150 * alfaPercentage)).brighter());
        color = orangeNINFO.brighter();// new Color(255, 255, 0).brighter();
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (255 * alfaPercentage));
        g.setColor(color);
        g.setStroke(new BasicStroke(2));
        g.draw(new Ellipse2D.Double(-9, -9, 18, 18));
        g.setColor(new Color(0, 0, 0, (int) (140 * alfaPercentage)));
        g.fill(new Ellipse2D.Double(-2, -2, 4, 4));
        g.setColor(Color.BLACK);

        if (true) {
            double newYaw = Math.toRadians(headingDegrees);
            Shape shape = getArrow();
            g.rotate(-rotationAngle);
            g.rotate(newYaw + Math.PI);
            color = Color.BLACK; // orangeNINFO.brighter();//new Color(255, 255, 0).brighter();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
            g.setStroke(new BasicStroke(2));
            g.fill(shape);
            color = Color.BLACK.darker(); // orangeNINFO.brighter();//new Color(255, 255, 0).brighter();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
            g.draw(shape);
            g.setColor(Color.BLACK);
            g.rotate(-(newYaw + Math.PI));
            g.rotate(rotationAngle);
        }

        String meTextToDraw = I18n.text("Me");
        if (enableShowDataSource) {
            meTextToDraw += (followPositionOf != null && followPositionOf.length() != 0 ? " "
                    + I18n.text("Pos. external") : "")
                    + (isSystemToDeriveHeadingFilled() || isFollowingHeadingOfFilled() ? " "
                            + I18n.textc("Heading external", "indication that the heading comes from external source")
                            : "");
        }
        
        GuiUtils.drawText(meTextToDraw, 18, 14, Color.WHITE, Color.BLACK, g);

        g.dispose();
    }

    /**
     * @return
     */
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
     * pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType
     * , pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {

        final LocationType l = new LocationType(loc);
        Vector<JMenuItem> menus = new Vector<JMenuItem>();

        JMenu myLocMenu = new JMenu(I18n.text("My location"));
        myLocMenu.setIcon(ICON);
        menus.add(myLocMenu);

        AbstractAction addToMap = new AbstractAction(I18n.text("Add My location to map as marker")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        long tstamp = System.currentTimeMillis();
                        LocationType locContact = new LocationType(MyState.getLocation());
                        if (getConsole() == null)
                            return;
                        if (getConsole().getMission() == null)
                            return;
                        MissionType mission = getConsole().getMission();
                        LinkedHashMap<String, MapMission> mapList = mission.getMapsList();
                        if (mapList == null)
                            return;
                        if (mapList.size() == 0)
                            return;
                        MapMission mapMission = mapList.values().iterator().next();
                        MapType mapType = mapMission.getMap();
                        MarkElement contact = new MarkElement();
                        contact.setCenterLocation(locContact);
                        String id = I18n.textc("MyLoc", "String prefix for a marker") + "_" + DateTimeUtil.dateTimeFileNameFormatterMillis.format(new Date(tstamp));
                        contact.setId(id);
                        contact.setParentMap(mapType);
                        contact.setMapGroup(mapType.getMapGroup());
                        mapType.addObject(contact);
                        mission.save(false);
                        mapType.getMapGroup().warnListeners(new MapChangeEvent(MapChangeEvent.OBJECT_ADDED));
                    };
                }.start();
            }
        };
        myLocMenu.add(new JMenuItem(addToMap));

        JMenuItem mid = new JMenuItem("" + location.toString());
        mid.setEnabled(false);
        myLocMenu.add(mid);

        JMenuItem mid2 = new JMenuItem(I18n.text("Heading") + ": " + ((int) headingDegrees) + "\u00B0");
        mid2.setEnabled(false);
        myLocMenu.add(mid2);

        AbstractAction copy = new AbstractAction(I18n.text("Copy")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ClipboardOwner owner = new ClipboardOwner() {
                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    };
                };
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(MyState.getLocation().getClipboardText()), owner);
            }
        };
        myLocMenu.add(new JMenuItem(copy));

        AbstractAction paste = new AbstractAction(I18n.text("Paste")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unused")
                ClipboardOwner owner = new ClipboardOwner() {
                    @Override
                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    };
                };

                Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

                boolean hasTransferableText = (contents != null)
                        && contents.isDataFlavorSupported(DataFlavor.stringFlavor);

                if (hasTransferableText) {
                    try {
                        String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                        LocationType lt = new LocationType();
                        lt.fromClipboardText(text);
                        MyState.setLocation(lt);
                    }
                    catch (Exception e1) {
                        NeptusLog.pub().error(e1);
                    }
                }
            }
        };
        myLocMenu.add(new JMenuItem(paste));

        myLocMenu.add(new JSeparator());

        AbstractAction add = new AbstractAction(I18n.text("Set this location as Mine")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                MyState.setLocation(new LocationType(l));
            }
        };
        myLocMenu.add(new JMenuItem(add));

        String txtUsingSysLoc = followPositionOf != null && followPositionOf.length() != 0 ? " [" +
        		I18n.text("using") + " " + followPositionOf + "]" : "";
        txtUsingSysLoc = (txtUsingSysLoc.length() == 0 ? I18n.text("Set to use a system position and heading as mine")
                : I18n.text("Change the system to use position and heading from") + txtUsingSysLoc);
        AbstractAction useThisLoc = new AbstractAction(txtUsingSysLoc) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<String> options = new Vector<String>();
                String noneStr = I18n.text("NONE");
                options.add(noneStr);
                options.add(getConsole().getMainSystem());
                String initialValue = followPositionOf == null || followPositionOf.length() == 0 ? noneStr
                        : followPositionOf;

                // fill the options
                Vector<String> sysList = new Vector<String>();
                for (ImcSystem sys : ImcSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(sys.getName()))
                        sysList.add(sys.getName());
                }
                
                for (ExternalSystem sys : ExternalSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(sys.getName()))
                        sysList.add(sys.getName());
                }
                
                Collections.sort(sysList);
                options.addAll(sysList);
                Vector<String> extList = new Vector<String>();
                for (ExternalSystem ext : ExternalSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(ext.getName()))
                        sysList.add(ext.getName());
                }
                Collections.sort(extList);
                options.addAll(extList);
                if (!options.contains(initialValue))
                    options.add(2, initialValue);

                String[] aopt = options.toArray(new String[options.size()]);
                String ret = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Set to use a system position and heading as mine"),
                        I18n.text("Choose a system"), JOptionPane.QUESTION_MESSAGE, ICON, aopt, initialValue);
                if (ret == null)
                    return;

                if (noneStr.equalsIgnoreCase(ret))
                    followPositionOf = "";
                else {
                    followPositionOf = ret;
                }
            }
        };
        myLocMenu.add(new JMenuItem(useThisLoc));

        String txtUseThisHeading = isFollowingHeadingOfFilled() ? " [" +
                I18n.text("using") + " " + followHeadingOf + "]" : "";
        txtUseThisHeading = (txtUseThisHeading.length() == 0 ? I18n.text("Set to use a system heading as mine")
                : I18n.text("Change the system to use heading from") + txtUseThisHeading);
        AbstractAction useThisHeading = new AbstractAction(txtUseThisHeading) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<String> options = new Vector<String>();
                String noneStr = I18n.text("NONE");
                options.add(noneStr);
                options.add(getConsole().getMainSystem());
                String initialValue = followHeadingOf == null || followHeadingOf.length() == 0 ? noneStr
                        : followHeadingOf;

                // fill the options
                Vector<String> sysList = new Vector<String>();
                for (ImcSystem sys : ImcSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(sys.getName()))
                        sysList.add(sys.getName());
                }
                for (ExternalSystem sys : ExternalSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(sys.getName()))
                        sysList.add(sys.getName());
                }
                
                Collections.sort(sysList);
                options.addAll(sysList);
                Vector<String> extList = new Vector<String>();
                for (ExternalSystem ext : ExternalSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(ext.getName()))
                        sysList.add(ext.getName());
                }
                Collections.sort(extList);
                options.addAll(extList);
                if (!options.contains(initialValue))
                    options.add(2, initialValue);

                String[] aopt = options.toArray(new String[options.size()]);
                String ret = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Set to use a system heading as mine"),
                        I18n.text("Choose a system"), JOptionPane.QUESTION_MESSAGE, ICON, aopt, initialValue);
                if (ret == null)
                    return;

                if (noneStr.equalsIgnoreCase(ret))
                    followHeadingOf = "";
                else {
                    followHeadingOf = ret;
                    
                    boolean validValue = false;
                    while (!validValue) {
                        String res = JOptionPane.showInputDialog(getConsole(),
                                I18n.text("Introduce the angle offset from system to use heading from (clockwise positive angle)"),
                                Double.valueOf(AngleUtils.nomalizeAngleDegrees180(followHeadingOfAngleOffset))
                                        .shortValue());
                        if (res == null)
                            return;
                        try {
                            followHeadingOfAngleOffset = Short.parseShort(res);
                            validValue = true;
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().debug(ex.getMessage());
                            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(),
                                    I18n.text("Introduce the angle offset from system to use heading from (clockwise positive angle)"),
                                    I18n.text("Value must be a numeric value from [-180, 180]"));
                        }
                    }

                }
            }
        };
        myLocMenu.add(new JMenuItem(useThisHeading));

        String txtUsingSysDeriveHeading = isSystemToDeriveHeadingFilled() ? " [" + I18n.text("using") + " " + useSystemToDeriveHeading + "]" : "";
        txtUsingSysDeriveHeading = (txtUsingSysDeriveHeading.length() == 0 ? I18n.text("Set to use a system to derive heading")
                : I18n.text("Change the system to derive heading from") + txtUsingSysDeriveHeading);
        AbstractAction useThisForHeading = new AbstractAction(txtUsingSysDeriveHeading) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<String> options = new Vector<String>();
                String noneStr = I18n.text("NONE");
                options.add(noneStr);
                options.add(getConsole().getMainSystem());
                String initialValue = useSystemToDeriveHeading == null || useSystemToDeriveHeading.length() == 0 ? noneStr
                        : useSystemToDeriveHeading;

                // fill the options
                Vector<String> sysList = new Vector<String>();
                for (ImcSystem sys : ImcSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(sys.getName()))
                        sysList.add(sys.getName());
                }
                Collections.sort(sysList);
                options.addAll(sysList);
                Vector<String> extList = new Vector<String>();
                for (ExternalSystem ext : ExternalSystemsHolder.lookupAllSystems()) {
                    if (!options.contains(ext.getName()))
                        sysList.add(ext.getName());
                }
                Collections.sort(extList);
                options.addAll(extList);
                if (!options.contains(initialValue))
                    options.add(2, initialValue);

                if (followPositionOf != null && followPositionOf.length() > 0) {
                    if (options.contains(followPositionOf))
                        options.remove(followPositionOf);
                }

                String[] aopt = options.toArray(new String[options.size()]);
                String ret = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Set to use a system to derive heading"),
                        I18n.text("Choose a system"), JOptionPane.QUESTION_MESSAGE, ICON, aopt, initialValue);
                if (ret == null)
                    return;

                if (noneStr.equalsIgnoreCase(ret))
                    useSystemToDeriveHeading = "";
                else {
                    useSystemToDeriveHeading = ret;

                    boolean validValue = false;
                    while (!validValue) {
                        String res = JOptionPane.showInputDialog(getConsole(),
                                I18n.text("Introduce the angle offset from front to derived heading (clockwise positive angle)"),
                                Double.valueOf(AngleUtils.nomalizeAngleDegrees180(angleOffsetFromFrontToDerivedHeading))
                                        .shortValue());
                        if (res == null)
                            return;
                        try {
                            angleOffsetFromFrontToDerivedHeading = Short.parseShort(res);
                            validValue = true;
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().debug(ex.getMessage());
                            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(),
                                    I18n.text("Introduce the angle offset from front to derived heading (clockwise positive angle)"),
                                    I18n.text("Value must be a numeric value from [-180, 180]"));
                        }
                    }

                    validValue = false;
                    while (!validValue) {
                        String res = JOptionPane.showInputDialog(getConsole(),
                                I18n.text("Introduce the angle offset from front to where the operator is looking (clockwise positive angle)"),
                                Double.valueOf(AngleUtils.nomalizeAngleDegrees180(angleOffsetFromFrontToWhereTheOperatorIsLooking))
                                        .shortValue());
                        if (res == null)
                            return;
                        try {
                            angleOffsetFromFrontToWhereTheOperatorIsLooking = Short.parseShort(res);
                            validValue = true;
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().debug(ex.getMessage());
                            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(),
                                    I18n.text("Introduce the angle offset from front to where the operator is looking (clockwise positive angle)"),
                                    I18n.text("Value must be a numeric value from [-180, 180]"));
                        }
                    }
                }
            }
        };
        myLocMenu.add(new JMenuItem(useThisForHeading));

        myLocMenu.add(new JSeparator());

        AbstractAction settings = new AbstractAction(I18n.text("Settings")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(MyLocationDisplay.this, getConsole(), true);
            }
        };
        myLocMenu.add(new JMenuItem(settings));

        return menus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        MyState.setHeadingInDegrees(headingDegrees);
        MyState.setLength(length);
        length = MyState.getLength();
        MyState.setWidth(width);
        width = MyState.getWidth();
        MyState.setLengthOffsetFromCenter(lengthOffsetFromCenter);
        lengthOffsetFromCenter = MyState.getLengthOffsetFromCenter();
        MyState.setWidthOffsetFromCenter(widthOffsetFromCenter);
        widthOffsetFromCenter = MyState.getWidthOffsetFromCenter();
        
        recalcRqst = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.consolebase.
     * SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {

        if (panelChange == null)
            return;

        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), IMapPopup.class)) {

            IMapPopup sub = (IMapPopup) panelChange.getPanel();

            if (panelChange.added()) {
                renderersPopups.add(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.addMenuExtension(this);
                }
            }

            if (panelChange.removed()) {
                renderersPopups.remove(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.removeMenuExtension(this);
                }
            }
        }

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {

            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "My location");
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
     * pt.lsts.neptus.console.plugins.MissionChangeListener#missionReplaced(pt.lsts.neptus.types.mission
     * .MissionType)
     */
    @Override
    public void missionReplaced(MissionType mission) {
//        MyState.setLocation(mission.getHomeRef());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.console.plugins.MissionChangeListener#missionUpdated(pt.lsts.neptus.types.mission.
     * MissionType)
     */
    @Override
    public void missionUpdated(MissionType mission) {
    }
}