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
 * Author: pdias
 * Mar 24, 2018
 */
package pt.lsts.neptus.util.coord;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class is mainly for savin a state of a {@link StateRenderer2D} in order to
 * be able to calculate positions. It is useful for in thread use.
 * 
 * @author pdias
 *
 */
public class MapTileRendererCalculator {
    
    private final LocationType center;
    private final Dimension size;
    private final Point2D worldPixelXY;
    private final float zoom;
    private final int levelOfDetail;
    private final double rotationRads;
    
    public MapTileRendererCalculator(StateRenderer2D renderer) {
        this.center = new LocationType(renderer.getCenter());
        this.size = renderer.getSize();
        this.worldPixelXY = (Point2D) renderer.worldPixelXY.clone();
        this.zoom = renderer.getZoom();
        this.levelOfDetail = renderer.getLevelOfDetail();
        this.rotationRads = renderer.getRotation();
    }
    
    /**
     * @return the center
     */
    public LocationType getCenter() {
        return center.getNewAbsoluteLatLonDepth();
    }
    
    /**
     * @return Renderer rotation in rads.
     */
    public double getRotation() {
        return rotationRads;
    }
    
    /**
     * @return the size
     */
    public Dimension getSize() {
        return new Dimension(size);
    }
    
    /**
     * @return
     */
    public double getWidth() {
        return size.getWidth();
    }
    
    /**
     * @return
     */
    public double getHeight() {
        return size.getHeight();
    }
       
    /**
     * @return the zoom
     */
    public float getZoom() {
        return zoom;
    }
    
    /**
     * @return the levelOfDetail
     */
    public int getLevelOfDetail() {
        return levelOfDetail;
    }
    
    /**
     * @param lt
     * @return
     */
    public Point2D getScreenPosition(LocationType lt) {
        return getScreenPositionHelper(lt, worldPixelXY, size, levelOfDetail, rotationRads);
    }

    /**
     * @param lt
     * @param worldPixelXY
     * @param size
     * @param levelOfDetail
     * @param rotationRads
     * @return
     */
    public static Point2D getScreenPositionHelper(LocationType lt, Point2D worldPixelXY, Dimension size,
            int levelOfDetail, double rotationRads) {
        // new code using Mercator projection
        double x = size.getWidth() / 2.0;
        double y = size.getHeight() / 2.0;

        // double[] dxy = center.getDistanceInPixelTo(lt, levelOfDetail);
        Point2D ltPix = lt.getPointInPixel(levelOfDetail);
        double[] dxy = { -worldPixelXY.getX() + ltPix.getX(), -worldPixelXY.getY() + ltPix.getY() };
        x += dxy[0];
        y += dxy[1];

        Point2D pt = new Point2D.Double(x, y);

        double xc = size.getWidth() / 2.0;
        double yc = size.getHeight() / 2.0;

        if (rotationRads != 0) {
            double dist = pt.distance(xc, yc);
            double angle = Math.atan2(y - yc, x - xc);
            angle -= rotationRads;

            double newX = xc + dist * Math.cos(angle);
            double newY = yc + dist * Math.sin(angle);

            x = newX;
            y = newY;
        }
        Point2D result = new Point2D.Double(x, y);
        return result;
    }

    /**
     * @param screenCoordinates
     * @return
     */
    public LocationType getRealWorldLocation(Point2D screenCoordinates) {
        return getRealWorldLocationHelper(screenCoordinates, screenCoordinates, size, levelOfDetail, rotationRads);
    }

    /**
     * @param screenCoordinates
     * @param worldPixelXY
     * @param size
     * @param levelOfDetail
     * @param rotationRads
     * @return
     */
    public static LocationType getRealWorldLocationHelper(Point2D screenCoordinates, Point2D worldPixelXY, Dimension size,
            int levelOfDetail, double rotationRads) {
        // distance to the center
        double tx = screenCoordinates.getX() - size.getWidth() / 2;
        double ty = screenCoordinates.getY() - size.getHeight() / 2;

        if (rotationRads != 0) {
            double angle = Math.atan2(ty, tx);
            double dist = Math.sqrt((tx * tx + ty * ty));

            angle += rotationRads;
            tx = dist * Math.cos(angle);
            ty = dist * Math.sin(angle);
        }
        Point2D centerXY = worldPixelXY;

        double[] latLong = MapTileUtil.xyToDegrees(centerXY.getX() + tx, centerXY.getY() + ty, levelOfDetail);

        LocationType loc = new LocationType();
        loc.setLatitudeDegs(latLong[0]);
        loc.setLongitudeDegs(latLong[1]);

        return loc;
    }
}
