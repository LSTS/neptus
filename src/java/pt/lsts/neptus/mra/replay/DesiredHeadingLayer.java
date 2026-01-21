/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jcordeiro
 * January 06, 2026
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.NeptusLog;

@PluginDescription(icon = "images/menus/compass.png", name = "Desired Heading")
public class DesiredHeadingLayer implements LogReplayLayer {
    private static final Color ARROW_COLOR = new Color(255, 255, 0, 200);
    private static final int ARROW_LENGTH_PX = 40;
    private static final int ARROW_OFFSET_PX = 20;
    private static final int ARROW_GAP_PX    = 4;
    private static final int ARROW_HEAD_LEN  = 10;
    private static final int ARROW_HEAD_WIDTH    = 6;

    private LocationType currentPosition;
    private double currentHeading = 0;
    private boolean hasPosition = false;
    private LsfIndex index;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!hasPosition) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setColor(ARROW_COLOR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2.5f));

            Point2D vehiclePoint = renderer.getScreenPosition(currentPosition);

            double headingRad = Math.toRadians(currentHeading);

            double ux = Math.sin(headingRad);
            double uy = -Math.cos(headingRad);

            Point2D startPoint = new Point2D.Double(
                    vehiclePoint.getX() + ux * ARROW_OFFSET_PX,
                    vehiclePoint.getY() + uy * ARROW_OFFSET_PX
            );

            Point2D endPoint = new Point2D.Double(
                    startPoint.getX() + ux * ARROW_LENGTH_PX,
                    startPoint.getY() + uy * ARROW_LENGTH_PX
            );

            g2.drawLine(
                    (int) startPoint.getX(),
                    (int) startPoint.getY(),
                    (int) endPoint.getX(),
                    (int) endPoint.getY()
            );

            Point2D arrowTip = new Point2D.Double(
                    endPoint.getX() + ux * ARROW_GAP_PX,
                    endPoint.getY() + uy * ARROW_GAP_PX
            );
            double angle = Math.atan2(uy, ux) - Math.PI/2;
            drawArrowHead(g2, arrowTip, angle);
        } finally {
            g2.dispose();
        }
    }

    private void drawArrowHead(Graphics2D g2, Point2D point, double angle) {
        AffineTransform old = g2.getTransform();

        g2.translate(point.getX(), point.getY());
        g2.rotate(angle);

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(0, 0);
        arrowHead.addPoint(-(int)(ARROW_HEAD_WIDTH), (int) -ARROW_HEAD_LEN);
        arrowHead.addPoint((int)(ARROW_HEAD_WIDTH), (int) -ARROW_HEAD_LEN);

        g2.fill(arrowHead);
        g2.setTransform(old);
    }

    @Override
    public void onMessage(IMCMessage message) {
        if ("DesiredHeading".equals(message.getAbbrev())) {
            currentHeading = Math.toDegrees(message.getDouble("value"));
        }
        else if ("EstimatedState".equals(message.getAbbrev())) {
            try {
                EstimatedState state = (EstimatedState) message;
                currentPosition = new LocationType(
                        Math.toDegrees(state.getLat()),
                        Math.toDegrees(state.getLon())
                );
                currentPosition.setDepth(state.getDepth());
                currentPosition.translatePosition(state.getX(), state.getY(), state.getZ());
                hasPosition = true;
            } catch (Exception e) {
                NeptusLog.pub().error("Error processing EstimatedState", e);
            }
        }
    }

    @Override
    public void parse(IMraLogGroup source) {
        this.index = source.getLsfIndex();
    }

    @Override
    public String[] getObservedMessages() {
        return new String[]{"DesiredHeading", "EstimatedState"};
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return true;
    }

    @Override
    public String getName() {
        return "Desired Heading";
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {
        currentPosition = null;
        hasPosition = false;
    }
}