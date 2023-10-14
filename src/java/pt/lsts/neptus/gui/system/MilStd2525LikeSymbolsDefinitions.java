/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 3 de Fev de 2012
 */
package pt.lsts.neptus.gui.system;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * MIL-STD-2525C (17 November 2008) superseding MIL-STD-2525B w/change 27 March 2007.
 * Department of Defense Interface Standard - Common Warfighting Symbology.
 * 
 * See also AEGIS B/L 7 and Naval Tactical Data System (NTDS) with not ratified draft STANAG-4420.
 * 
 * @author pdias
 *
 */
public class MilStd2525LikeSymbolsDefinitions {

    public static boolean debugOn = false;

    // Blue
    private static final Color CYAN = new Color(0, 255, 255);
    private static final Color CRYSTAL_BLUE = new Color(128, 224, 255);
    //Yellow
    private static final Color YELLOW = new Color(255, 255, 0); 
    private static final Color LIGHT_YELLOW = new Color(255, 255, 128);
    // Green
    private static final Color NEON_GREEN = new Color(0, 255, 0); 
    private static final Color BAMBOO_GREEN = new Color(170, 255, 170);
    // Red
    private static final Color RED = new Color(255, 0, 0); 
    private static final Color SALMON = new Color(255, 128, 128);
    // Purple (Weather / Assumed Friendly or Commercial - proposed)
    private static final Color PLUM_RED = new Color(255, 0, 255); //version C, was in B new Color(128, 0, 128);
    private static final Color LIGHT_ORCHID = new Color(255, 161, 255); //version C, was in B new Color(226, 159, 255);
    // Brown (Weather)
    private static final Color SAFARI = new Color(128, 98, 16); // Don't exist in version C anymore
    private static final Color KHAKI  = new Color(210, 176, 106); // Don't exist in version C anymore 
    // Black (Boundaries, lines, areas, text, icons, and frames)
    private static final Color BLACK = new Color(0, 0, 0); 
    // White
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Color OFF_WHITE_6PERC_GREY = new Color(239, 239, 239);

    // Friend, Assumed Friend 
    public static final Color BLUE_ICON = CYAN;
    public static final Color BLUE_FILL = CRYSTAL_BLUE;
    public static final Color BLUE_FILL_MEDIUM = new Color(0, 168, 220); // new in version C
    public static final Color BLUE_FILL_DARK = new Color(0, 107, 140); // new in version C

    // Unknown, Pending
    public static final Color YELLOW_ICON = YELLOW;
    public static final Color YELLOW_FILL = LIGHT_YELLOW;
    public static final Color YELLOW_FILL_MEDIUM = new Color(255, 255, 0); // new in version C
    public static final Color YELLOW_FILL_DARK = new Color(225, 220, 0); // new in version C
    // Neutral
    public static final Color GREEN_ICON = NEON_GREEN;
    public static final Color GREEN_FILL = BAMBOO_GREEN;
    public static final Color GREEN_FILL_MEDIUM = new Color(0, 226, 0); // new in version C
    public static final Color GREEN_FILL_DARK = new Color(0, 160, 0); // new in version C
    // Hostile, Suspect, Joker, Faker 
    public static final Color RED_ICON = RED;
    public static final Color RED_FILL = SALMON;
    public static final Color RED_FILL_MEDIUM = new Color(255, 48, 49); // new in version C
    public static final Color RED_FILL_DARK = new Color(200, 0, 0); // new in version C
    // Weather 1 - Based on the samples provided, the Weather Symbol Sets use Blue (0, 0, 255) and Purple (194, 0, 255) along 
    //             with Yellow, Neon Green, Red, Safari Brown and Black.  There are no instances of Plum Red, Light Orchid or 
    //             Khaki being used.  This inconsistency is still unresolved in Mil-Std-2525B
    public static final Color PURPLE_ICON = PLUM_RED;
    public static final Color PURPLE_FILL = LIGHT_ORCHID;
    public static final Color PURPLE_FILL_MEDIUM = new Color(128, 0, 128); // new in version C
    public static final Color PURPLE_FILL_DARK = new Color(80, 0, 80); // new in version C
    // Weather 2
    public static final Color BROWN_ICON = SAFARI;
    public static final Color BROWN_FILL = KHAKI;
    public static final Color BROWN_FILL_MEDIUM = new Color(164, 139, 86); // don't exist in version C
    public static final Color BROWN_FILL_DARK = new Color(118, 91, 32); // don't exist in version C
    // Boundaries, lines, areas, text, icons, and frames
    public static final Color BLACK_ICON = BLACK;
    public static final Color BLACK_FILL = BLACK;
    public static final Color BLACK_FILL_MEDIUM = BLACK_FILL; // don't exist in version C
    public static final Color BLACK_FILL_DARK = BLACK_FILL; // don't exist in version C
    // *
    public static final Color WHITE_ICON = WHITE;
    public static final Color WHITE_FILL = OFF_WHITE_6PERC_GREY;
    public static final Color WHITE_FILL_MEDIUM = WHITE_FILL; // don't exist in version C
    public static final Color WHITE_FILL_DARK = WHITE_FILL; // don't exist in version C

    // My definitions
    public static final Color ORANGE_ICON = new Color(255, 128, 0);
    public static final Color ORANGE_FILL = new Color(230, 121, 56);
    public static final Color ORANGE_FILL_MEDIUM = new Color(246, 96, 5); // don't exist in version C
    public static final Color ORANGE_FILL_DARK = new Color(232, 103, 25); // don't exist in version C

    private static final int SYMBOL_SIZE = 32;

    private static final GeneralPath ICON_SHAPE_OCTAGON = new GeneralPath();
    static {
        double halfLength = SYMBOL_SIZE / 2.;
        double halfLengthCosPiPer4 = halfLength * Math.cos(Math.PI / 4.);

        ICON_SHAPE_OCTAGON.moveTo(0, -halfLength );
        ICON_SHAPE_OCTAGON.lineTo(halfLengthCosPiPer4, -halfLengthCosPiPer4);
        ICON_SHAPE_OCTAGON.lineTo(halfLength, 0);
        ICON_SHAPE_OCTAGON.lineTo(halfLengthCosPiPer4, halfLengthCosPiPer4);
        ICON_SHAPE_OCTAGON.lineTo(0, halfLength);
        ICON_SHAPE_OCTAGON.lineTo(-halfLengthCosPiPer4, halfLengthCosPiPer4);
        ICON_SHAPE_OCTAGON.lineTo(-halfLength, 0);
        ICON_SHAPE_OCTAGON.lineTo(-halfLengthCosPiPer4, -halfLengthCosPiPer4);
        ICON_SHAPE_OCTAGON.closePath();
    }

    public enum SymbolTypeEnum {
        AIR, SURFACE_UNIT, SURFACE, SUBSURFACE;
    };

    public enum SymbolShapeEnum {
        FRIEND, NEUTRAL, HOSTILE, UNKNOWN, UNFRAMED;
    };

    public enum SymbolColorEnum {
        BLUE, RED, GREEN, YELLOW, PURPLE, BROWN, ORANGE;

        public Color getFillColor() {
            switch (this) {
                case BLUE:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return BLUE_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return BLUE_FILL_MEDIUM;
                    return BLUE_FILL;
                case RED:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return RED_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return RED_FILL_MEDIUM;
                    return RED_FILL;
                case GREEN:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return GREEN_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return GREEN_FILL_MEDIUM;
                    return GREEN_FILL;
                case YELLOW:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return YELLOW_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return YELLOW_FILL_MEDIUM;
                    return YELLOW_FILL;
                case PURPLE:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return PURPLE_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return PURPLE_FILL_MEDIUM;
                    return PURPLE_FILL;
                case BROWN:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return BROWN_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return BROWN_FILL_MEDIUM;
                    return BROWN_FILL;
                case ORANGE:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return ORANGE_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return ORANGE_FILL_MEDIUM;
                    return ORANGE_FILL;
                default:
                    if (colorIntencity == SymbolColorIntencityEnum.DARK)
                        return BLUE_FILL_DARK;
                    else if (colorIntencity == SymbolColorIntencityEnum.MEDIUM)
                        return BLUE_FILL_MEDIUM;
                    return BLUE_FILL;
            }
        }

        public Color getIconColor() {
            switch (this) {
                case BLUE:
                    return BLUE_ICON;
                case RED:
                    return RED_ICON;
                case GREEN:
                    return GREEN_ICON;
                case YELLOW:
                    return YELLOW_ICON;
                case PURPLE:
                    return PURPLE_ICON;
                case BROWN:
                    return BROWN_ICON;
                case ORANGE:
                    return ORANGE_ICON;
                default:
                    return BLUE_ICON;
            }
        }
    };

    public enum SymbolColorIntencityEnum {
        LIGHT, MEDIUM, DARK;
    };

    public static SymbolColorIntencityEnum colorIntencity = SymbolColorIntencityEnum.LIGHT; 

    public enum SymbolOperationalConditionEnum {
        NONE, NORMAL, WARNING, FAULT, ERROR, FAILURE;
    };

    public enum SymbolIconEnum {
        NONE, UAS, CCU, SENSOR, UNKNOWN;
    };

    /**
     * @param g
     * @param type
     * @param shapeType
     * @param operationalCondition
     * @param iconSize
     * @param frameOn
     * @param fillOn
     * @param continuousOrDashedFrameStroke
     * @param useTransparency
     * @param drawIcon
     * @param drawMainIndicator
     */
    public static void paintMilStd2525(Graphics2D g, SymbolTypeEnum type, SymbolShapeEnum shapeType,
            SymbolOperationalConditionEnum operationalCondition, int iconSize, boolean frameOn, boolean fillOn,
            boolean continuousOrDashedFrameStroke, int useTransparency, SymbolIconEnum drawIcon, boolean drawMainIndicator) {

        SymbolColorEnum colorType = SymbolColorEnum.BLUE;
        switch (shapeType) {
            case FRIEND:
                colorType = SymbolColorEnum.BLUE;
                break;
            case NEUTRAL:
                colorType = SymbolColorEnum.GREEN;
                break;
            case HOSTILE:
                colorType = SymbolColorEnum.RED;
                break;
            case UNKNOWN:
                colorType = SymbolColorEnum.YELLOW;
                break;
            case UNFRAMED:
                break;
        }

        paintMilStd2525(g, type, shapeType, colorType, operationalCondition, iconSize, frameOn, fillOn,
                continuousOrDashedFrameStroke, useTransparency, drawIcon, drawMainIndicator);
    }

    /**
     * Assumes 0,0 to be the center of the graphics.
     * @param g
     * @param type
     * @param shapeType
     * @param colorType
     * @param operationalCondition
     * @param iconSize
     * @param frameOn
     * @param fillOn
     * @param continuousOrDashedFrameStroke
     * @param useTransparency
     */
    public static void paintMilStd2525(Graphics2D g, SymbolTypeEnum type, SymbolShapeEnum shapeType, SymbolColorEnum colorType,
            SymbolOperationalConditionEnum operationalCondition, int iconSize, boolean frameOn, boolean fillOn,
            boolean continuousOrDashedFrameStroke, int useTransparency, SymbolIconEnum drawIcon, boolean drawMainIndicator) {

        Graphics2D g2 = (Graphics2D) g.create();

        if (useTransparency != 255)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, useTransparency / 255f));

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double zs = iconSize * 1. / SYMBOL_SIZE;
        if (zs != 1)
            g2.scale(zs, zs);

        // Assumes 0,0 to be the center. put the 0,0 to top left
        g2.translate(-SYMBOL_SIZE / 2., -SYMBOL_SIZE / 2.);

        Color colorToFill = colorType.getFillColor();
        Color colorToIcon = colorType.getIconColor();
        Color colorBlack = BLACK_FILL;
        Color colorWhite = WHITE_FILL;

        Shape shape;
        if (shapeType == SymbolShapeEnum.NEUTRAL) {
            GeneralPath gp;
            switch (type) {
                case AIR:
                case SUBSURFACE:
                    gp = new GeneralPath();
                    gp.moveTo(-SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE);
                    gp.lineTo(-SYMBOL_SIZE * 0.1 / 2., -SYMBOL_SIZE * 0.2);
                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), -SYMBOL_SIZE * 0.2);
                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), SYMBOL_SIZE);
                    shape = gp;
                    if (type == SymbolTypeEnum.SUBSURFACE)
                        shape = gp.createTransformedShape(new AffineTransform(-1, 0, 0, -1, SYMBOL_SIZE, SYMBOL_SIZE));
                    break;
                    //                case SUBSURFACE:
                    //                    gp = new GeneralPath();
                    //                    gp.moveTo(-SYMBOL_SIZE * 0.1 / 2., 0);
                    //                    gp.lineTo(-SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE * 1.2);
                    //                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), SYMBOL_SIZE * 1.2);
                    //                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), 0);
                    //                    shape = gp;
                    //                    break;
                case SURFACE:
                case SURFACE_UNIT:
                    shape = new RoundRectangle2D.Double(-SYMBOL_SIZE * 0.1 / 2., -SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE * 1.1, SYMBOL_SIZE * 1.1, 0, 0);
                    break;
                default:
                    shape = new Ellipse2D.Double(-SYMBOL_SIZE / 10., -SYMBOL_SIZE / 10., SYMBOL_SIZE / 5, SYMBOL_SIZE / 5);
            }
        }
        else if (shapeType == SymbolShapeEnum.HOSTILE) {
            GeneralPath gp;
            switch (type) {
                case AIR:
                case SUBSURFACE:
                    gp = new GeneralPath();
                    gp.moveTo(-SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE);
                    gp.lineTo(-SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE * 0.2);
                    gp.lineTo(SYMBOL_SIZE / 2., -SYMBOL_SIZE * 0.3);
                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 / 2.), SYMBOL_SIZE * 0.2);
                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 / 2.), SYMBOL_SIZE);
                    shape = gp;
                    if (type == SymbolTypeEnum.SUBSURFACE)
                        shape = gp.createTransformedShape(new AffineTransform(-1, 0, 0, -1, SYMBOL_SIZE, SYMBOL_SIZE));
                    break;
                    //                case SUBSURFACE:
                    //                    gp = new GeneralPath();
                    //                    gp.moveTo(-SYMBOL_SIZE * 0.1 / 2., 0);
                    //                    gp.lineTo(-SYMBOL_SIZE * 0.1 / 2., SYMBOL_SIZE * 0.8);
                    //                    gp.lineTo(SYMBOL_SIZE / 2., SYMBOL_SIZE * 1.3);
                    //                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), SYMBOL_SIZE * 0.8);
                    //                    gp.lineTo(SYMBOL_SIZE * (1 + 0.1 /2.), 0);
                    //                    shape = gp;
                    //                    break;
                case SURFACE:
                case SURFACE_UNIT:
                    gp = new GeneralPath();
                    gp.moveTo(SYMBOL_SIZE / 2., - SYMBOL_SIZE * 0.44 / 2.);
                    gp.lineTo(SYMBOL_SIZE * (1 + 0.44 / 2.), SYMBOL_SIZE / 2.);
                    gp.lineTo(SYMBOL_SIZE / 2., SYMBOL_SIZE * (1 + 0.44 / 2.));
                    gp.lineTo(-SYMBOL_SIZE * 0.44 / 2., SYMBOL_SIZE / 2.);
                    gp.closePath();
                    shape = gp;
                    break;
                default:
                    shape = new Ellipse2D.Double(-SYMBOL_SIZE / 10., -SYMBOL_SIZE / 10., SYMBOL_SIZE / 5, SYMBOL_SIZE / 5);
            }
        }
        else if (shapeType == SymbolShapeEnum.UNKNOWN) {
            GeneralPath gp;
            double halfLength = SYMBOL_SIZE / 2.;
            double halfLengthCosPiPer4 = halfLength * Math.cos(Math.PI / 4.);
            switch (type) {
                case AIR:
                case SUBSURFACE:
                    gp = new GeneralPath();
                    gp.moveTo(SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE);
                    gp.curveTo(-(SYMBOL_SIZE * 0.5 / 2.), SYMBOL_SIZE, 
                            -(SYMBOL_SIZE * 0.5 / 2.), SYMBOL_SIZE / 2. - halfLengthCosPiPer4, 
                            SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE / 2. - halfLengthCosPiPer4);
                    gp.curveTo(SYMBOL_SIZE / 2. - halfLengthCosPiPer4, -SYMBOL_SIZE * 0.3,  
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, -SYMBOL_SIZE * 0.3, 
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, SYMBOL_SIZE / 2. - halfLengthCosPiPer4);
                    gp.curveTo(SYMBOL_SIZE + (SYMBOL_SIZE * 0.5 / 2.), SYMBOL_SIZE / 2. - halfLengthCosPiPer4,
                            SYMBOL_SIZE + (SYMBOL_SIZE * 0.5 / 2.), SYMBOL_SIZE,
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, SYMBOL_SIZE);
                    shape = gp;
                    if (type == SymbolTypeEnum.SUBSURFACE)
                        shape = gp.createTransformedShape(new AffineTransform(-1, 0, 0, -1, SYMBOL_SIZE, SYMBOL_SIZE));
                    break;
                case SURFACE:
                case SURFACE_UNIT:
                    gp = new GeneralPath();
                    gp.moveTo(SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE / 2. - halfLengthCosPiPer4);
                    gp.curveTo(SYMBOL_SIZE / 2. - halfLengthCosPiPer4, -SYMBOL_SIZE * 0.44 / 2,  
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, -SYMBOL_SIZE * 0.44 / 2, 
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, SYMBOL_SIZE / 2. - halfLengthCosPiPer4);
                    gp.curveTo(SYMBOL_SIZE + SYMBOL_SIZE * 0.44 / 2, SYMBOL_SIZE / 2. - halfLengthCosPiPer4,
                            SYMBOL_SIZE + SYMBOL_SIZE * 0.44 / 2, SYMBOL_SIZE / 2. + halfLengthCosPiPer4, 
                            SYMBOL_SIZE / 2. + halfLengthCosPiPer4, SYMBOL_SIZE / 2. + halfLengthCosPiPer4);
                    gp.curveTo(SYMBOL_SIZE / 2. + halfLengthCosPiPer4, SYMBOL_SIZE + SYMBOL_SIZE * 0.44 / 2,
                            SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE + SYMBOL_SIZE * 0.44 / 2, 
                            SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE / 2. + halfLengthCosPiPer4);
                    gp.curveTo(-SYMBOL_SIZE * 0.44 / 2, SYMBOL_SIZE / 2. + halfLengthCosPiPer4,
                            -SYMBOL_SIZE * 0.44 / 2, SYMBOL_SIZE / 2. - halfLengthCosPiPer4,
                            SYMBOL_SIZE / 2. - halfLengthCosPiPer4, SYMBOL_SIZE / 2. - halfLengthCosPiPer4);
                    shape = gp;
                    break;
                default:
                    shape = new Ellipse2D.Double(-SYMBOL_SIZE / 10., -SYMBOL_SIZE / 10., SYMBOL_SIZE / 5, SYMBOL_SIZE / 5);
            }
        }
        else { // SymbolShapeEnum.FRIEND
            switch (type) {
                case AIR:
                    shape = new Arc2D.Double(-SYMBOL_SIZE * 0.1 / 2., -SYMBOL_SIZE * 0.2 * 2. / 2., SYMBOL_SIZE * 1.1,
                            SYMBOL_SIZE * 1.2 * 2, 0, 180, 0);
                    break;
                case SUBSURFACE:
                    shape = new Arc2D.Double(-SYMBOL_SIZE * 0.1 / 2., -SYMBOL_SIZE * 1.2 * 2. / 2., SYMBOL_SIZE * 1.1,
                            SYMBOL_SIZE * 1.2 * 2, 0, -180, 0);
                    break;
                case SURFACE:
                    shape = new Ellipse2D.Double(-SYMBOL_SIZE * 0.2 / 2., -SYMBOL_SIZE * 0.2 / 2., SYMBOL_SIZE * 1.2,
                            SYMBOL_SIZE * 1.2);
                    break;
                case SURFACE_UNIT:
                    shape = new RoundRectangle2D.Double(-SYMBOL_SIZE * 0.5 / 2., 0, SYMBOL_SIZE * 1.5, SYMBOL_SIZE, 0, 0);
                    break;
                default:
                    shape = new Ellipse2D.Double(-SYMBOL_SIZE / 10., -SYMBOL_SIZE / 10., SYMBOL_SIZE / 5, SYMBOL_SIZE / 5);
            }
        }

        if (fillOn) {
            g2.setColor(colorToFill);
            g2.fill(shape);

            // For Neutral Air or Subsurface apply a proposed modification of a notch
            if (shapeType == SymbolShapeEnum.NEUTRAL
                    && (type == SymbolTypeEnum.AIR || type == SymbolTypeEnum.SUBSURFACE)) {
                boolean isAirOrSub = type == SymbolTypeEnum.SUBSURFACE ? false : true;
                //                Shape shapeNotched = new RoundRectangle2D.Double(SYMBOL_SIZE / 2. - SYMBOL_SIZE * 0.4 / 2.,
                //                        isAirOrSub ? SYMBOL_SIZE - SYMBOL_SIZE * 0.2 : 0, SYMBOL_SIZE * 0.4, SYMBOL_SIZE * 0.2, 0, 0);
                Graphics2D gI = (Graphics2D) g2.create();
                if (fillOn)
                    gI.setColor(colorBlack);
                else
                    gI.setColor(colorToIcon);
                Shape shapeNotched = new RoundRectangle2D.Double(-SYMBOL_SIZE * 0.1,
                        isAirOrSub ? SYMBOL_SIZE - SYMBOL_SIZE * 0.2 : 0, SYMBOL_SIZE * 0.2, SYMBOL_SIZE * 0.2, 0, 0);

                gI.fill(shapeNotched);
                shapeNotched = new RoundRectangle2D.Double(SYMBOL_SIZE - SYMBOL_SIZE * 0.1,
                        isAirOrSub ? SYMBOL_SIZE - SYMBOL_SIZE * 0.2 : 0, SYMBOL_SIZE * 0.2, SYMBOL_SIZE * 0.2, 0, 0);
                gI.fill(shapeNotched);
                gI.dispose();
            }
        }
        if (frameOn) {
            Graphics2D gI = (Graphics2D) g2.create();

            Stroke oldStroke = gI.getStroke();
            if (!continuousOrDashedFrameStroke) {
                gI.setColor(colorWhite);
                gI.setStroke(new BasicStroke(SYMBOL_SIZE / 10));
                gI.draw(shape);
            }
            if (fillOn)
                gI.setColor(colorBlack);
            else
                gI.setColor(colorToIcon);
            if (!continuousOrDashedFrameStroke)
                gI.setStroke(new BasicStroke(SYMBOL_SIZE / 10, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] {
                        SYMBOL_SIZE / 10, SYMBOL_SIZE / 10 }, 0));
            else
                gI.setStroke(new BasicStroke(SYMBOL_SIZE / 10));
            gI.draw(shape);
            gI.setStroke(oldStroke);

            gI.dispose();
        }

        if (drawIcon != SymbolIconEnum.NONE) {
            Graphics2D gI = (Graphics2D) g2.create();
            // Go to center
            gI.translate(SYMBOL_SIZE / 2., SYMBOL_SIZE / 2.);

            if (fillOn)
                gI.setColor(colorBlack);
            else
                gI.setColor(colorToIcon);

            if (drawIcon == SymbolIconEnum.UAS) {
                // UAS icon
                GeneralPath sp = new GeneralPath();
                sp.moveTo(0, SYMBOL_SIZE / 7.);
                sp.lineTo(-SYMBOL_SIZE / 3., -SYMBOL_SIZE / 8.);
                sp.lineTo(0, SYMBOL_SIZE / 8.);
                sp.lineTo(SYMBOL_SIZE / 3., -SYMBOL_SIZE / 8.);
                sp.closePath();
                gI.setStroke(new BasicStroke(SYMBOL_SIZE / 10f));
                gI.draw(sp);
            }
            else if (drawIcon == SymbolIconEnum.CCU) {
                // CCU icon
                gI.setFont(new Font("Arial", Font.BOLD, 10));
                String tt = "CCU";
                Rectangle2D sB1 = gI.getFontMetrics().getStringBounds(tt, gI);
                double sw0 = SYMBOL_SIZE * 0.6 / sB1.getWidth();
                double sh0 = SYMBOL_SIZE * 0.4 / sB1.getHeight();
                gI.scale(sw0, sh0);
                gI.translate(-sB1.getWidth() / 2., sB1.getHeight() / 4.);
                gI.drawString(tt, 0, 0);
            }
            else if (drawIcon == SymbolIconEnum.SENSOR) {
                // Sensor icon
                GeneralPath sps = new GeneralPath();
                double dSize = SYMBOL_SIZE / 2. * 0.52;
                double infl = dSize * 0.4;
                double cOff = dSize * 0.6;
                sps.moveTo(-dSize, -dSize);
                sps.curveTo(-dSize + cOff, -dSize + infl,
                        dSize - dSize * 0.6, -dSize + infl,
                        dSize, -dSize);
                sps.curveTo(dSize - infl, -dSize + cOff,
                        dSize - infl, dSize - cOff,
                        dSize, dSize);
                sps.curveTo(dSize - cOff, dSize - infl,
                        -dSize + cOff, dSize - infl,
                        -dSize, dSize);
                sps.curveTo(-dSize + infl, dSize - cOff,
                        -dSize + infl, -dSize + cOff,
                        -dSize, -dSize);
                gI.rotate(Math.PI / 4);
                gI.fill(sps);
                gI.rotate(-Math.PI / 4);
            }
            else if (drawIcon == SymbolIconEnum.UNKNOWN) {
                gI.setFont(new Font("Arial", Font.BOLD, 10));
                String tt = "?";
                Rectangle2D sB1 = gI.getFontMetrics().getStringBounds(tt, gI);
                double sw0 = SYMBOL_SIZE * 0.5 / sB1.getWidth();
                double sh0 = SYMBOL_SIZE * 0.5 / sB1.getHeight();
                gI.scale(sw0, sh0);
                gI.translate(-sB1.getWidth() / 2., sB1.getHeight() / 4.);
                gI.drawString(tt, 0, 0);
            }

            gI.dispose();
        }

        if (drawMainIndicator) {
            Graphics2D gI = (Graphics2D) g2.create();
            gI.translate(SYMBOL_SIZE / 2., SYMBOL_SIZE / 2.); // Go to center
            gI.setFont(new Font("Arial", Font.BOLD, 10));
            String tt = "M";
            Rectangle2D sB1 = gI.getFontMetrics().getStringBounds(tt, gI);
            double sw0 = 10.0 / sB1.getWidth();
            double sh0 = 10.0 / sB1.getHeight();
            //            gI.translate(0, SYMBOL_SIZE / 2. - SYMBOL_SIZE / 10.);
            gI.scale(sw0, sh0);
            //            gI.translate(-sB1.getWidth() / 2., 0);
            gI.translate(-sB1.getWidth() / 2., (SYMBOL_SIZE / 2. - SYMBOL_SIZE / 10.) / sh0);
            if (fillOn)
                gI.setColor(colorBlack);
            else
                gI.setColor(colorToIcon);
            gI.drawString(tt, 0, 0);
            gI.dispose();
        }

        if (operationalCondition != SymbolOperationalConditionEnum.NONE) {
            Rectangle2D bounds = shape.getBounds2D();
            Shape opShape = new RoundRectangle2D.Double(bounds.getMinX(), bounds.getMaxY() + SYMBOL_SIZE / 10,
                    bounds.getWidth(), SYMBOL_SIZE / 4, 0, 0);
            Color opColor = Color.LIGHT_GRAY;
            switch (operationalCondition) {
                case NORMAL:
                    opColor = Color.GREEN;
                    break;
                case WARNING:
                    opColor = Color.BLUE;
                    break;
                case FAULT:
                    opColor = Color.YELLOW;
                    break;
                case ERROR:
                    opColor = new Color(255, 128, 0); // orange
                    break;
                case FAILURE:
                    opColor = Color.RED;
                    break;
                default:
                    break;
            }
            g2.setColor(opColor);
            g2.fill(opShape);
            g2.setColor(colorBlack);
            g2.draw(opShape);
        }
        g2.dispose();

        // Debug Icon shape
        if (debugOn) {
            g2 = (Graphics2D) g.create();
            if (zs != 1)
                g2.scale(zs, zs);
            g2.setColor(ColorUtils.setTransparencyToColor(Color.LIGHT_GRAY, 128));
            g2.fill(ICON_SHAPE_OCTAGON);
            g2.setColor(ColorUtils.setTransparencyToColor(Color.GRAY, 128));
            g2.draw(ICON_SHAPE_OCTAGON);

            g2.dispose();
        }
    }


    @SuppressWarnings("serial")
    public static void main(String[] args) {
        //        debugOn = true;

        JPanel panel = new JPanel(true) {
            int iconSize = 32; // 16 24 32
            int spacingSize = (int) (iconSize * 1.6);

            SymbolShapeEnum shapeType = SymbolShapeEnum.FRIEND;
            SymbolColorEnum colorType = SymbolColorEnum.BLUE;
            SymbolOperationalConditionEnum operationalCondition = SymbolOperationalConditionEnum.NONE;
            SymbolColorEnum[] altColor =              { null,                null,                null,               null,               null,               null,               null,               SymbolColorEnum.PURPLE, SymbolColorEnum.BROWN,  SymbolColorEnum.ORANGE };
            boolean[] frameOn =                       { true,                true,                true,               true,               true,               true,               true,               true,                   true,                   true };
            boolean[] fillOn =                        { true,                true,                true,               true,               false,              false,              true,               true,                   true,                   true };
            boolean[] continuousOrDashedFrameStroke = { true,                false,               true,               false,              true,               false,              true,               true,                   true,                   true };
            int[] useTransparency =                   { 255,                 255,                 255,                255,                255,                255,                128,                255,                    255,                    255  };
            SymbolIconEnum[] drawIcon =               { SymbolIconEnum.NONE, SymbolIconEnum.NONE, SymbolIconEnum.UAS, SymbolIconEnum.UAS, SymbolIconEnum.UAS, SymbolIconEnum.UAS, SymbolIconEnum.UAS, SymbolIconEnum.CCU,     SymbolIconEnum.SENSOR,  SymbolIconEnum.UNKNOWN };
            boolean[] drawMainIndicator =             { false,               false,               true,               true,               true,               true,               false,              false,                  false,                  false};

            @Override
            public void paint(Graphics g) {
                super.paint(g);

                Graphics2D g2 = (Graphics2D) g;

                g2.translate(spacingSize / 2, spacingSize / 2);

                for (int i = 0; i < frameOn.length; i++) {
                    shapeType = SymbolShapeEnum.FRIEND;
                    colorType = altColor[i] != null ? altColor[i] : SymbolColorEnum.BLUE;
                    paintMilStd2525(g2, SymbolTypeEnum.AIR, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE_UNIT, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SUBSURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);

                    shapeType = SymbolShapeEnum.NEUTRAL;
                    colorType = altColor[i] != null ? altColor[i] : SymbolColorEnum.GREEN;
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.AIR, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE_UNIT, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SUBSURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);

                    shapeType = SymbolShapeEnum.HOSTILE;
                    colorType = altColor[i] != null ? altColor[i] : SymbolColorEnum.RED;
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.AIR, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE_UNIT, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SUBSURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);

                    shapeType = SymbolShapeEnum.UNKNOWN;
                    colorType = altColor[i] != null ? altColor[i] : SymbolColorEnum.YELLOW;
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.AIR, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE_UNIT, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);
                    g2.translate(spacingSize, 0);
                    paintMilStd2525(g2, SymbolTypeEnum.SUBSURFACE, shapeType, colorType, operationalCondition, iconSize, frameOn[i],
                            fillOn[i], continuousOrDashedFrameStroke[i], useTransparency[i], drawIcon[i], drawMainIndicator[i]);

                    g2.translate(-spacingSize * 15, spacingSize + iconSize / 2.);
                }
            }
            //            @Override
            //            public Dimension getSize() {
            //                return new Dimension(spacingSize * 16 + 20, spacingSize * frameOn.length);
            //            }
        };
        panel.setBackground(Color.WHITE);
        panel.setBackground(new Color(2, 113, 171)); // Render blue

        JScrollPane jsp = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        GuiUtils.testFrame(jsp, "MIL-STD-2525 Like Symbology", 860, 750);

//        try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        colorIntencity = SymbolColorIntencityEnum.MEDIUM;
//        panel.repaint();
//        try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        colorIntencity = SymbolColorIntencityEnum.DARK;
//        panel.repaint();
//        try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        colorIntencity = SymbolColorIntencityEnum.LIGHT;
//        panel.repaint();
//        try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        debugOn = true;
//        panel.repaint();
//        try { Thread.sleep(5000); } catch (InterruptedException e) { }
//        debugOn = false;
//        panel.repaint();
    }
}
