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
 * Author: zp
 * Feb 1, 2014
 */
package pt.lsts.neptus.plugins.beacons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.MissionType;

/**
 * @author zp
 *
 */
@PluginDescription(name="Transponder Visibility", icon="pt/lsts/neptus/plugins/beacons/transponder.png")
public class BeaconsVisibilityLayer extends ConsoleLayer implements MissionChangeListener {

    @NeptusProperty(name="Maximum communication radius")
    private double commRadius = 800;
    
    private List<Area> beaconVisibility = Collections.synchronizedList(new ArrayList<Area>());
    private List<LocationType> beaconPositions = Collections.synchronizedList(new ArrayList<LocationType>());
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void missionUpdated(MissionType mission) {
        parseMap();
    }

    @Override
    public void missionReplaced(MissionType mission) {
        parseMap();
    }

    @Override
    public void initLayer() {
        parseMap();
    }

    private void parseMap() {
        beaconPositions.clear();
        beaconVisibility.clear();
        MapGroup mg = MapGroup.getMapGroupInstance(getConsole().getMission());
        Vector<TransponderElement> elems = mg.getAllObjectsOfType(TransponderElement.class);

        if (elems.isEmpty()) {
            return;
        }

        Vector<AbstractElement> obstacles = mg.getObstacles();
        for (TransponderElement e : elems) {
            LocationType center = new LocationType(e.getCenterLocation());
            beaconPositions.add(center);
            beaconVisibility.add(computeVisibility(center, commRadius, obstacles));
        }
    }

    private Area computeVisibility(LocationType center, double radius, Vector<AbstractElement> obstacles) {        
        Area visibleArea = new Area(new Ellipse2D.Double(-radius, -radius, radius*2, radius*2));

        for (AbstractElement elem : obstacles) {
            Vector<LocationType> corners = elem.getShapePoints();
            if (corners == null || corners.isEmpty())
                continue;

            double[] lastOffsets = corners.lastElement().getOffsetFrom(center);
            Point2D prevPoint = new Point2D.Double(lastOffsets[1], -lastOffsets[0]);
            Area a = new Area();
            for (LocationType l : corners) {
                double[] offsets = l.getOffsetFrom(center);
                Point2D p = new Point2D.Double(offsets[1], -offsets[0]);
                GeneralPath gp = new GeneralPath();
                gp.moveTo(p.getX(), p.getY());
                double ang = Math.atan2(p.getY(), p.getX());
                double px = p.getX() + commRadius * 2 *  Math.cos(ang);
                double py = p.getY() + commRadius * 2 * Math.sin(ang);
                gp.lineTo(px, py);
                ang = Math.atan2(prevPoint.getY(), prevPoint.getX());
                px = prevPoint.getX() + commRadius * 2 * Math.cos(ang);
                py = prevPoint.getY() + commRadius * 2 * Math.sin(ang);                     
                gp.lineTo(px, py);
                gp.lineTo(prevPoint.getX(), prevPoint.getY());
                gp.closePath();

                a.add(new Area(gp));
                prevPoint = p;
            }
            visibleArea.subtract(a);
        }
        return visibleArea;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        for (int i = 0; i < beaconPositions.size(); i++) {
            Graphics2D g2 = (Graphics2D)g.create();
            Point2D center = renderer.getScreenPosition(beaconPositions.get(i));

            g2.translate(center.getX(), center.getY());
            g2.scale(renderer.getZoom(), renderer.getZoom());

            //g2.fill(beaconVisibility.get(i));
            g2.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), (float)commRadius, new float[]{0.3f, 0.5f}, 
                    new Color[] {new Color(0,255,0,40), new Color(0,255,0,0)}));
            g2.fill(beaconVisibility.get(i));
        }
    }

    @Override
    public void cleanLayer() {

    }
}
