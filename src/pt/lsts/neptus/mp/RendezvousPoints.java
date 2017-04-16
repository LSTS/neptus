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
 * 15/04/2017
 */
package pt.lsts.neptus.mp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.OffScreenLayerImageControl;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;

/**
 * @author pdias
 *
 */
@XmlRootElement(name = "RendezvousPoints")
@XmlAccessorType(XmlAccessType.NONE)
@LayerPriority(priority = 35)
public class RendezvousPoints implements Renderer2DPainter {

    private static final Color COLOR_YELLOW = ColorUtils.setTransparencyToColor(Color.YELLOW, 130);
    private static final Color COLOR_BLACK = ColorUtils.setTransparencyToColor(Color.BLACK, 130);
    private static final Color COLOR_YELLOW_EDIT = Color.YELLOW;
    private static final Color COLOR_RED_EDIT = ColorUtils.setTransparencyToColor(Color.RED, 240);

    private Ellipse2D.Double ellipseLarge = new Ellipse2D.Double(-30 / 2., -30 / 2., 30, 30);
    private Ellipse2D.Double ellipseMedium = new Ellipse2D.Double(-22 / 2., -22 / 2., 22, 22);
    private Ellipse2D.Double ellipseSmall = new Ellipse2D.Double(-12 / 2., -12 / 2., 12, 12);
    private Ellipse2D.Double ellipseLittle = new Ellipse2D.Double(-6 / 2., -6 / 2., 6, 6);

    // @XmlElementWrapper(name = "points")
    @XmlElement(name = "point")
    private List<Point> points = Collections.synchronizedList(new ArrayList<>());
    
    private OffScreenLayerImageControl offScreen = new OffScreenLayerImageControl();
    private boolean isEditing = false;

    public RendezvousPoints() {
    }

    /**
     * @return the isEditing
     */
    public boolean isEditing() {
        return isEditing;
    }
    
    /**
     * @param isEditing the isEditing to set
     */
    public void setEditing(boolean isEditing) {
        if (this.isEditing != isEditing)
            offScreen.triggerImageRebuild();

        this.isEditing = isEditing;
    }
    
    public int numberOfPoints() {
        synchronized (points) {
            return points.size();
        }
    }
    
    public void addPoint(double latDeg, double lonDeg) {
        synchronized (points) {
            points.add(new Point(latDeg, lonDeg));
        }
        offScreen.triggerImageRebuild();
    }
    
    public void addPoint(LocationType loc) {
        synchronized (points) {
            points.add(Point.from(loc));
        }
        offScreen.triggerImageRebuild();
    }

    public void removePoint(LocationType loc) {
        synchronized (points) {
            points.remove(Point.from(loc));
        }
        offScreen.triggerImageRebuild();
    }
    
    public void triggerChange() {
        offScreen.triggerImageRebuild();
    }

    public void removePoint(Point pt) {
        synchronized (points) {
            points.remove(pt);
        }
        offScreen.triggerImageRebuild();
    }

    public List<LocationType> getLocations() {
        synchronized (points) {
            ArrayList<LocationType> ret = new ArrayList<>();
            points.forEach(pt -> ret.add(pt.asLocation()));
            return Collections.unmodifiableList(ret);
        }
    }

    public List<Point> getPoints() {
        synchronized (points) {
            return Collections.unmodifiableList(points);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        boolean recreateImage = offScreen.paintPhaseStartTestRecreateImageAndRecreate(g, renderer);
        if (recreateImage) {
            Graphics2D g1 = offScreen.getImageGraphics();

            synchronized (points) {
                LocationType lt = new LocationType();
                for (Point point : points) {
                    Graphics2D g2 = (Graphics2D) g1.create();
                    lt.setLatitudeDegs(point.getLatDeg());
                    lt.setLongitudeDegs(point.getLonDeg());
                    Point2D pt = renderer.getScreenPosition(lt);
                    g2.translate(pt.getX(), pt.getY());

                    g2.setColor(isEditing() ? COLOR_YELLOW_EDIT : COLOR_YELLOW);
                    g2.fill(ellipseLarge);
                    g2.setColor(isEditing() ? COLOR_RED_EDIT : COLOR_BLACK);
                    g2.fill(ellipseMedium);
                    g2.setColor(isEditing() ? COLOR_YELLOW_EDIT : COLOR_YELLOW);
                    g2.fill(ellipseSmall);
                    g2.setColor(isEditing() ? COLOR_RED_EDIT : COLOR_BLACK);
                    g2.fill(ellipseLittle);
                    g2.dispose();
                }
            }
        }            
        offScreen.paintPhaseEndFinishImageRecreateAndPaintImageCacheToRenderer(g, renderer);
    }

    @XmlType(name = "point")
    public static class Point {
        private double latDeg = 0;
        private double lonDeg = 0;

        public Point() {
        }
        
        public LocationType asLocation() {
            return new LocationType(latDeg, lonDeg);
        }

        public Point(double latDeg, double lonDeg) {
            this.latDeg = latDeg;
            this.lonDeg = lonDeg;
        }

        public static Point from(LocationType loc) {
            LocationType locA = loc.getNewAbsoluteLatLonDepth();
            return new Point(locA.getLatitudeDegs(), locA.getLongitudeDegs());
        }

        public double getLatDeg() {
            return latDeg;
        }

        public void setLatDeg(double latDeg) {
            this.latDeg = latDeg;
        }

        public double getLonDeg() {
            return lonDeg;
        }

        public void setLonDeg(double lonDeg) {
            this.lonDeg = lonDeg;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                if (this.getLatDeg() == ((Point) obj).getLatDeg()
                        && this.getLonDeg() == ((Point) obj).getLonDeg())
                    return true;
                else
                    return false;
            }
            else {
                return super.equals(obj);
            }
        }
    }
    
    public String asXml() {
        StringWriter writer = new StringWriter();
        JAXB.marshal(this, writer);
        return writer.toString();
    }

    public static RendezvousPoints loadXml(String xml) {
        return JAXB.unmarshal(new StringReader(xml), RendezvousPoints.class);
    }
   
    public static void main(String[] args) {
        RendezvousPoints rps = new RendezvousPoints();
        
        rps.addPoint(41, -8);
        rps.addPoint(LocationType.FEUP);
        
        String xml = rps.asXml();
        NeptusLog.pub().info(xml);
        RendezvousPoints rps2 = RendezvousPoints.loadXml(xml);
        NeptusLog.pub().info(rps2.asXml());
    }
}
