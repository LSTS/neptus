/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 19 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.mapdecomposition;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class GridArea {
    private PolygonType polygon;

    private int cellWidth;
    private int cellHeight;

    private double gridWidth;
    private double gridHeight;
    private double yawRads;

    private LocationType center;

    private int nrows;
    private int ncols;
    private GridCell[][] grid;

    // rectangle bounds
    private LocationType topLeft = null;
    private LocationType topRight = null;
    private LocationType bottomLeft = null;
    private LocationType bottomRight = null;

    public GridArea(PolygonType mapObject, int cellWidth) {
        polygon = mapObject;
        this.cellWidth = cellWidth;
        this.cellHeight = this.cellWidth;

        computeAreaDimensions();
    }

    public double getWidth() {
        return gridWidth;
    }

    public double getHeight() {
        return gridHeight;
    }

    public double getYawRads() {
        return yawRads;
    }

    public void setYawRads(double yawRads) {
        this.yawRads = yawRads;
    }

    public void recomputeDimensions(PolygonType polygon) {
        this.polygon = polygon;
        computeAreaDimensions();
    }

    /**
     * Computes the bounding rectangle' paramaters
     * of the polygon that represents this area.
     *
     * TODO work with non-convex polygons
     * */
    private void computeAreaDimensions() {
        List<PolygonType.Vertex> vertices = polygon.getVertices();

        PolygonType.Vertex minLatV = vertices.get(0);
        PolygonType.Vertex maxLatV = vertices.get(0);
        PolygonType.Vertex minLonV = vertices.get(0);
        PolygonType.Vertex maxLonV = vertices.get(0);

        for(int i = 1; i < vertices.size(); i++) {
            PolygonType.Vertex currentV = vertices.get(i);
            if(currentV.getLatitudeDegs() < minLatV.getLatitudeDegs())
                minLatV = currentV;
            else if(currentV.getLatitudeDegs() > maxLatV.getLatitudeDegs())
                maxLatV = currentV;

            if(currentV.getLongitudeDegs() < minLonV.getLongitudeDegs())
                minLonV = currentV;
            else if(currentV.getLongitudeDegs() > maxLonV.getLongitudeDegs())
                maxLonV = currentV;
        }

        // area vertices
        topLeft = new LocationType(maxLatV.getLatitudeDegs(), minLonV.getLongitudeDegs());
        topRight = new LocationType(maxLatV.getLatitudeDegs(), maxLonV.getLongitudeDegs());

        bottomLeft = new LocationType(minLatV.getLatitudeDegs(), minLonV.getLongitudeDegs());
        bottomRight = new LocationType(minLatV.getLatitudeDegs(), maxLonV.getLongitudeDegs());

        // area dimensions
        gridWidth = topRight.getDistanceInMeters(topLeft);
        gridHeight = topRight.getDistanceInMeters(bottomRight);

        center = CoordinateUtil.computeLocationsCentroid(Arrays.asList(
                new LocationType[]{topLeft, topRight, bottomLeft, bottomRight}));
    }

    public LocationType getCenterLocation() {
        return center;
    }

    public void displayArea(Graphics2D g, StateRenderer2D source, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        Point2D p = source.getScreenPosition(this.center);
        double scale = source.getZoom();
        int w = (int) (this.gridWidth * scale);
        int h = (int) (this.gridHeight * scale);

        AffineTransform transform = new AffineTransform();
        transform.translate(p.getX(), p.getY());
        transform.rotate(-source.getRotation());

        g2.transform(transform);
        g2.setColor(color);
        g2.drawRect(-w/2, -h/2, w, h);

        int radius = 10;
        g2.setColor(Color.green);
        g2.fillOval(-w/2, -h/2, radius, radius);
    }
}
