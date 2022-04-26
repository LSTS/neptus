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
 * 29/Jul/2012
 */
package pt.lsts.neptus.gui.system;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolIconEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 *
 */
public class SystemPainterHelper {

    public static enum CircleTypeBySystemType { AIR, SUBSURFACE, SURFACE, SURFACE_UNIT, DEFAULT };

    public static final int AGE_TRANSPARENCY = 128;
    
    public static final Color EXTERNAL_SYSTEM_COLOR = new Color(255, 0, 255); // PLUM_RED
    
    protected static GeneralPath shipShape = new GeneralPath();
    static {
        shipShape.moveTo(0, -5);
        shipShape.lineTo(-5, -3.5);
        shipShape.lineTo(-5, 5);
        shipShape.lineTo(5, 5);
        shipShape.lineTo(5, -3.5);
        shipShape.lineTo(0, -5);
        shipShape.closePath();
    }

    private SystemPainterHelper() {
    }
    
    /**
     * @param sys
     * @return
     */
    public static final boolean isLocationKnown(ImcSystem sys) {
        return isLocationKnown(sys.getLocation(), sys.getLocationTimeMillis());
    }

    /**
     * @param loc
     * @param timeMillis
     * @return
     */
    public static final boolean isLocationKnown(LocationType loc, long timeMillis) {
        if (getLocationAge(loc, timeMillis) < 10000
                && !loc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * @param loc
     * @param timeMillis
     * @return
     */
    public static final long getLocationAge(LocationType loc, long timeMillis) {
        return System.currentTimeMillis() - timeMillis;
    }


    /**
     * @param g
     * @param sys
     * @param isLocationKnown
     * @param isMainVehicle
     * @param milStd2525FilledOrNot
     */
    public static final void drawMilStd2525LikeSymbolForSystem(Graphics2D g, ImcSystem sys, boolean isLocationKnown,
            boolean isMainVehicle, boolean milStd2525FilledOrNot) {
        MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum type = SymbolTypeEnum.AIR;
        if (sys.getType() == SystemTypeEnum.VEHICLE) {
            if (sys.getTypeVehicle() == VehicleTypeEnum.UAV)
                type = SymbolTypeEnum.AIR;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.UUV)
                type = SymbolTypeEnum.SUBSURFACE;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.UGV)
                type = SymbolTypeEnum.SURFACE;
            else if (sys.getTypeVehicle() == VehicleTypeEnum.USV)
                type = SymbolTypeEnum.SURFACE;
            else
                type = SymbolTypeEnum.SURFACE_UNIT;
        }
        else if (sys.getType() == SystemTypeEnum.CCU)
            type = SymbolTypeEnum.SURFACE_UNIT;
        else
            type = SymbolTypeEnum.SURFACE;

        MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum shapeType = SymbolShapeEnum.FRIEND;
        if (!sys.isWithAuthority()) // (!sd..isWithAuthority())
            shapeType = SymbolShapeEnum.NEUTRAL;
        if (sys.getType() == SystemTypeEnum.UNKNOWN)
            shapeType = SymbolShapeEnum.UNKNOWN;

        MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum operationalCondition = SymbolOperationalConditionEnum.NONE;
        if (sys.isOnErrorState())
            operationalCondition = SymbolOperationalConditionEnum.ERROR;
        
        MilStd2525LikeSymbolsDefinitions.SymbolIconEnum drawIcon = MilStd2525LikeSymbolsDefinitions.SymbolIconEnum.UAS;
        if (sys.getType() == SystemTypeEnum.CCU)
            drawIcon = SymbolIconEnum.CCU;
        else if (sys.getType() == SystemTypeEnum.UNKNOWN)
            drawIcon = SymbolIconEnum.UNKNOWN;
        else if (sys.getType() == SystemTypeEnum.MOBILESENSOR || sys.getType() == SystemTypeEnum.STATICSENSOR)
            drawIcon = SymbolIconEnum.SENSOR;
        
        drawMilStd2525LikeSymbolForSystem(g, type, shapeType, operationalCondition, drawIcon, isLocationKnown,
                isMainVehicle, milStd2525FilledOrNot);
    }

    /**
     * @param g
     * @param type
     * @param shapeType
     * @param operationalCondition
     * @param drawIcon
     * @param isLocationKnown
     * @param isMainVehicle
     * @param milStd2525FilledOrNot
     */
    public static final void drawMilStd2525LikeSymbolForSystem(Graphics2D g, MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum type, 
            MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum shapeType,
            MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum operationalCondition,
            MilStd2525LikeSymbolsDefinitions.SymbolIconEnum drawIcon,
            boolean isLocationKnown, boolean isMainVehicle, boolean milStd2525FilledOrNot) {
        Graphics2D g2 = (Graphics2D) g.create();

        boolean drawMainIndicator = isMainVehicle; //sd.isMainVehicle();

        MilStd2525LikeSymbolsDefinitions.paintMilStd2525(g2, type, shapeType, operationalCondition, 30, true,
                milStd2525FilledOrNot, true, (isLocationKnown ? 255 : 128), drawIcon, drawMainIndicator);

        g2.dispose();
    }

    /**
     * @param renderer
     * @param g
     * @param sys
     * @param color
     * @param iconDiameter 
     * @param isLocationKnownUpToDate 
     */
    public static final void drawSystemIcon(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, Color color,
            double iconDiameter, boolean isLocationKnownUpToDate) {
        drawSystemIcon(renderer, g, sys.getYawDegrees(), color, iconDiameter, isLocationKnownUpToDate);
    }
     
    /**
     * @param renderer
     * @param g
     * @param headingAngleDegrees
     * @param color
     * @param iconDiameter
     * @param isLocationKnownUpToDate
     */
    public static final void drawSystemIcon(StateRenderer2D renderer, Graphics2D g, double headingAngleDegrees, Color color,
            double iconDiameter, boolean isLocationKnownUpToDate) {
        Graphics2D g2 = (Graphics2D) g.create();
        // long atMillis = sys.getAttitudeTimeMillis();
        double yawRad = Math.toRadians(headingAngleDegrees);
        GeneralPath gp = SystemIconsUtil.getUAV();
        double scale = iconDiameter / gp.getBounds2D().getWidth();
        if (scale != 1.0)
            g2.scale(scale, scale);

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));
        
        g2.setColor(color);
        g2.rotate(yawRad - renderer.getRotation());
        g2.fill(gp);
        g2.setColor(Color.BLACK);
        g2.draw(gp);

        g2.dispose();
    }

    /**
     * @param g
     * @param sysName
     * @param color
     * @param safetyOffset
     * @param isLocationKnownUpToDate
     */
    public static final void drawSystemNameLabel(Graphics2D g, String sysName, Color color, double safetyOffset, boolean isLocationKnownUpToDate) {
        Graphics2D g2 = (Graphics2D) g.create();

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        g2.setColor(Color.BLACK);
        g2.drawString(sysName, (int) (12 * safetyOffset / 20) + 1, 1);
        g2.setColor(color);
        g2.drawString(sysName, (int) (12 * safetyOffset / 20), 0);
        g2.dispose();
    }

    public static final void drawSystemLocationAge(Graphics2D g, long locAgeMillis, Color color, double safetyOffset, boolean isLocationKnownUpToDate) {
        if (isLocationKnownUpToDate)
            return;
        
        Graphics2D g2 = (Graphics2D) g.create();

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        StringBuilder timeSB = new StringBuilder("\u231B ");
        timeSB.append(DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - locAgeMillis, true));
        String timeStr = timeSB.toString();
        Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(timeStr, g2);
        g2.setColor(Color.BLACK);
        g2.drawString(timeStr, (int) (12 * safetyOffset / 20) + 1, (int) (1 + stringBounds.getHeight() + 2));
        g2.setColor(color);
        g2.drawString(timeStr, (int) (12 * safetyOffset / 20), (int) (stringBounds.getHeight() + 2));
        g2.dispose();
    }

    /**
     * @param g
     * @param color
     * @param diameter
     * @param isLocationKnownUpToDate
     */
    public static final void drawCircleForSystem(Graphics2D g, Color color, double diameter, boolean isLocationKnownUpToDate) {
        drawCircleForSystem(g, color, diameter, CircleTypeBySystemType.DEFAULT, isLocationKnownUpToDate);
    }

    /**
     * @param g
     * @param color
     * @param diameter
     * @param circleType
     * @param isLocationKnownUpToDate
     */
    public static final void drawCircleForSystem(Graphics2D g, Color color, double diameter, CircleTypeBySystemType circleType, boolean isLocationKnownUpToDate) {
        if (circleType == null)
            return;
        
        Graphics2D g2 = (Graphics2D) g.create();

        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

//        g2.setColor(Color.BLACK);
//        g2.drawOval((int) (-diameter / 2) - 1, (int) (-diameter / 2) - 1, (int) diameter + 2, (int) diameter + 2);
//        g2.setColor(color);
//        g2.drawOval((int) (-diameter / 2), (int) (-diameter / 2), (int) diameter, (int) diameter);
//        g2.dispose();
        
        Shape shape;
        Shape shape1;
        switch (circleType) {
            case AIR: //AIR
                shape = new Arc2D.Double(-diameter * 0.1 / 2., -diameter * 0.2 * 2. / 2., diameter * 1.1,
                        diameter * 1.2 * 2, 0, 180, 0);
                shape1 = new Arc2D.Double(-diameter * 0.1 / 2. - 1, -diameter * 0.2 * 2. / 2. - 1, diameter * 1.1 + 2,
                        diameter * 1.2 * 2 + 2, 0, 180, 0);
                break;
            case SUBSURFACE: //SUBSURFACE
                shape = new Arc2D.Double(-diameter * 0.1 / 2., -diameter * 1.2 * 2. / 2., diameter * 1.1,
                        diameter * 1.2 * 2, 0, -180, 0);
                shape1 = new Arc2D.Double(-diameter * 0.1 / 2. - 1, -diameter * 1.2 * 2. / 2. - 1, diameter * 1.1 + 2,
                        diameter * 1.2 * 2 + 2, 0, -180, 0);
                break;
            case SURFACE: //SURFACE
            default:
                shape = new Ellipse2D.Double(-diameter * 0.2 / 2., -diameter * 0.2 / 2., diameter * 1.2,
                        diameter * 1.2);
                shape1 = new Ellipse2D.Double(-diameter * 0.2 / 2. - 1, -diameter * 0.2 / 2. - 1, diameter * 1.2 + 2,
                        diameter * 1.2 + 2);
                break;
            case SURFACE_UNIT: //SURFACE_UNIT
                shape = new RoundRectangle2D.Double(-diameter * 0.5 / 2., 0, diameter * 1.5, diameter, 0, 0);
                shape1 = new RoundRectangle2D.Double(-diameter * 0.5 / 2. - 1, 0 - 1, diameter * 1.5 + 2, diameter + 2, 0, 0);
                break;
        }
        g2.translate(-diameter / 2., -diameter / 2.);
        g2.setColor(Color.BLACK);
        g2.draw(shape1);
        g2.setColor(color);
        g2.draw(shape);

        g2.dispose();
    }

    /**
     * @param renderer
     * @param g2
     * @param sys
     * @param isLocationKnownUpToDate 
     */
    public static final void drawErrorStateForSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, double diameter, boolean isLocationKnownUpToDate) {
        if (sys.isOnErrorState()) {
            Graphics2D g2 = (Graphics2D) g.create();

            int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
            if (useTransparency != 255)
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

            Shape opShape = new RoundRectangle2D.Double(-diameter / 2.0, diameter / 2.0 + diameter / 10, diameter, diameter / 3, 0, 0);
            Color opColor = new Color(255, 128, 0); // orange
            g2.setColor(opColor);
            g2.fill(opShape);
            g2.setColor(Color.BLACK);
            g2.draw(opShape);
            
            g2.dispose();
        }
    }

    public static final void drawCourseSpeedVectorForSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, 
            double iconWidth, boolean isLocationKnownUpToDate, double minimumSpeedToBeStopped) {
        drawCourseSpeedVectorForSystem(renderer, g, sys, Color.WHITE, iconWidth, isLocationKnownUpToDate, minimumSpeedToBeStopped);
    }

    /**
     * @param renderer
     * @param g2
     * @param sys
     * @param iconWidth 
     * @param color 
     * @param isLocationKnownUpToDate 
     */
    public static final void drawCourseSpeedVectorForSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, Color color, 
            double iconWidth, boolean isLocationKnownUpToDate, double minimumSpeedToBeStopped) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        Color colorToPaint = color == null ? g2.getColor() : color;
        @SuppressWarnings("unused")
        int vectorOffset = (int) (iconWidth / 2d);
        
        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        Object obj = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
        if (obj != null) {
            double courseDegrees = (Integer) obj;
            obj = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
            if (obj != null) {
                double gSpeed = (Double) obj;
                drawCourseSpeedVectorForSystem(renderer, g2, courseDegrees, gSpeed, colorToPaint, iconWidth,
                        isLocationKnownUpToDate, minimumSpeedToBeStopped);
            }
        }
        
        g2.dispose();
        return;
    }

    /**
     * @param renderer
     * @param g
     * @param courseDegrees
     * @param speed
     * @param color
     * @param iconWidth
     * @param isLocationKnownUpToDate
     * @param minimumSpeedToBeStopped
     */
    public static final void drawCourseSpeedVectorForSystem(StateRenderer2D renderer, Graphics2D g, 
            double courseDegrees, double speed, Color color, 
            double iconWidth, boolean isLocationKnownUpToDate, double minimumSpeedToBeStopped) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        Color colorToPaint = color == null ? g2.getColor() : color;
        int vectorOffset = (int) (iconWidth / 2d);
        
        int useTransparency = (isLocationKnownUpToDate ? 255 : AGE_TRANSPARENCY);
        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        if (Double.isFinite(courseDegrees) && Double.isFinite(speed) && speed > minimumSpeedToBeStopped) {
            g2.rotate(Math.toRadians(courseDegrees) - renderer.getRotation());
            Stroke cs = g2.getStroke();
            double zs = speed * renderer.getZoom();
            if (zs < 50) {
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0,
                        new float[] { 5, 5 }, 0));
                g2.setColor(Color.BLACK);
                g2.drawLine(2, -vectorOffset, 0, -50);
                g2.drawLine(-2, -vectorOffset, 0, -50);
                g2.setColor(colorToPaint);
                g2.drawLine(0, -vectorOffset, 0, -50);
            }
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.BLACK);
            g2.drawLine(2, -vectorOffset, 0, -(int) zs);
            g2.drawLine(-2, -vectorOffset, 0, -(int) zs);
            g2.setColor(colorToPaint);
            g2.drawLine(0, -vectorOffset, 0, -(int) zs);
            g2.setStroke(cs);
        }
        
        g2.dispose();
        return;
    }
    
    
    public static final void drawVesselDimentionsIconForSystem(StateRenderer2D renderer, Graphics2D g, double width,
            double length, double widthOffsetFromCenter, double lenghtOffsetFromCenter, double headingDegrees, 
            Color color, boolean isLocationKnownUpToDate) {

        double alfaPercentage = isLocationKnownUpToDate ? 1 : AGE_TRANSPARENCY / 255.;
        
        double diameter = Math.max(length, width);
        if (diameter > 0) {
            Graphics2D gt = (Graphics2D) g.create();

            double scaleX = (renderer.getZoom() / 10) * width;
            double scaleY = (renderer.getZoom() / 10) * length;

            diameter = diameter * renderer.getZoom();
            Color colorCircle = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage));
            gt.setColor(colorCircle);

            gt.rotate(Math.toRadians(headingDegrees) - renderer.getRotation());

            gt.draw(new Ellipse2D.Double(-diameter / 2 - widthOffsetFromCenter * renderer.getZoom() / 2.,
                    -diameter / 2 + lenghtOffsetFromCenter * renderer.getZoom() / 2., diameter, diameter));

            gt.translate(-widthOffsetFromCenter * renderer.getZoom() / 2., lenghtOffsetFromCenter * renderer.getZoom() /2.);
            gt.scale(scaleX, scaleY);
            gt.fill(shipShape);
            
            gt.dispose();
        }
    }
}
