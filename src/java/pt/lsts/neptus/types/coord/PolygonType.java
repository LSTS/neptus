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
 * 22/08/2016
 */
package pt.lsts.neptus.types.coord;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@XmlType
public class PolygonType implements Renderer2DPainter {

    @XmlElement
    protected ArrayList<Vertex> vertices = new ArrayList<>();
    private String id;

    protected PathElement elem = null;
    protected boolean filled = true;

    private Color color = Color.RED.darker();

    protected GeneralPath coverage = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Add a polygon vertex
     * 
     * @param latDegs the latitude of the vertex to add
     * @param lonDegs the longitude of the vertex to add
     */
    public void addVertex(double latDegs, double lonDegs) {
        double minDist = Double.MAX_VALUE;
        int pos = 0;
        synchronized (vertices) {
            if (vertices.size() < 2)
                vertices.add(new Vertex(latDegs, lonDegs));
            else {
                LocationType loc = new LocationType(latDegs, lonDegs);

                for (int i = 1; i < vertices.size(); i++) {
                    double dist = loc.getDistanceInMeters(vertices.get(i - 1).getLocation())
                            + loc.getDistanceInMeters(vertices.get(i).getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        pos = i;
                    }
                }

                vertices.add(pos, new Vertex(latDegs, lonDegs));
            }
        }
        recomputePath();
    }

    /**
     * Add new vertex given its LocationType
     */
    public void addVertex(LocationType loc) {
        synchronized (vertices) {
            vertices.add(new Vertex(loc));
        }
        recomputePath();
    }

    public void addVertex(int index, LocationType loc) {
        synchronized (vertices) {
            vertices.add(index, new Vertex(loc));
        }
        recomputePath();
    }

    /**
     * Remove all polygon vertices
     */
    public void clearVertices() {
        synchronized (vertices) {
            vertices.clear();
        }
        recomputePath();
    }

    /**
     * Retrieve an <strong>unmodifiable</strong> list of vertices
     * 
     * @return list of the vertices in this polygon
     */
    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public void removeVertex(Vertex v) {
        if (v == null)
            return;
        synchronized (vertices) {
            vertices.remove(v);
        }
        recomputePath();
    }

    public int getVerticesSize() {
        return vertices.size();
    }

    public void setColor(Color c) {
        color = c;
        if (elem != null)
            elem.setMyColor(c);
    }

    /**
     * @param filled the filled to set
     */
    public final void setFilled(boolean filled) {
        this.filled = filled;
        if (elem != null)
            elem.setFilled(filled);
    }

    /**
     * @see Renderer2DPainter
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (elem == null)
            recomputePath();

        if (elem == null)
            return;
        else {
            elem.setMyColor(color);
            elem.paint(g, renderer, renderer.getRotation());
        }
        Point2D pt = renderer.getScreenPosition(elem.getCenterLocation());

        g.translate(pt.getX(), pt.getY());
        g.rotate(-renderer.getRotation());
        g.scale(renderer.getZoom(), renderer.getZoom());
        g.setColor(Color.red);

        g.setColor(Color.blue);
        if (coverage != null)
            g.draw(coverage);
    }

    public void recomputePath() {
        synchronized (vertices) {
            if (vertices.isEmpty()) {
                elem = null;
                return;
            }

            elem = new PathElement();
            elem.setFilled(filled);
            elem.setMyColor(Color.yellow);

            elem.setCenterLocation(new LocationType(vertices.get(0).getLocation()));
            for (Vertex v : vertices)
                elem.addPoint(new LocationType(v.getLocation()));
            elem.setFinished(true);
        }
    }

    public boolean containsPoint(LocationType loc) {
        return elem.containsPoint(loc, null);
    }

    @Override
    public String toString() {
        return "Polygon (" + vertices.size() + " vertices)";
    }

    public PolygonType clone() {
        StringWriter writer = new StringWriter();
        JAXB.marshal(this, writer);
        return JAXB.unmarshal(new StringReader(writer.toString()), getClass());
    }

    /**
     * Given a list of locations, calculates and returns its centroid's location
     */
    public LocationType getCentroid() {
        List<LocationType> locations = new ArrayList<>();
        vertices.forEach(v -> locations.add(v.lt.getNewAbsoluteLatLonDepth()));
        return CoordinateUtil.computeLocationsCentroid(locations);
    }

    @XmlType
    public static class Vertex {
        private LocationType lt;

        public Vertex() {
            lt = new LocationType();
        }

        public Vertex(LocationType lt) {
            this.lt = new LocationType(lt);
        }

        public Vertex(double latDegs, double lonDegs) {
            lt = new LocationType(latDegs, lonDegs);
        }

        @Override
        public int hashCode() {
            return ("" + lt.getLatitudeDegs() + "," + lt.getLongitudeDegs()).hashCode();
        }

        public void setLocation(LocationType newLt) {
            lt = new LocationType(newLt);
        }

        public LocationType getLocation() {
            return lt;
        }

        public double getLatitudeDegs() {
            return lt.getNewAbsoluteLatLonDepth().getLatitudeDegs();
        }

        public double getLongitudeDegs() {
            return lt.getNewAbsoluteLatLonDepth().getLongitudeDegs();
        }
    }

    public void translate(double offsetNorth, double offsetEast) {
        synchronized (vertices) {
            vertices.forEach(
                    v -> v.getLocation().translatePosition(offsetNorth, offsetEast, 0).convertToAbsoluteLatLonDepth());
        }
        recomputePath();
    }

    /**
     * Rotate this polygon by yaw rads
     */
    public void rotate(double yawRads) {
        synchronized (vertices) {

            LocationType pivot = elem.getCenterLocation();
            LocationType center = elem.getCenterPoint();
            double centerOffsets[] = center.getOffsetFrom(pivot);

            Point2D[] pts = new Point2D.Double[vertices.size()];
            int i = 0;
            for (PolygonType.Vertex v : vertices) {
                double ofs[] = v.getLocation().getOffsetFrom(pivot);
                pts[i++] = new Point2D.Double(ofs[0], ofs[1]);
            }

            AffineTransform t = AffineTransform.getRotateInstance(yawRads, centerOffsets[0], centerOffsets[1]);
            t.transform(pts, 0, pts, 0, pts.length);

            for (i = 0; i < pts.length; i++) {
                LocationType loc = new LocationType(pivot);
                loc.translatePosition(pts[i].getX(), pts[i].getY(), 0);
                vertices.get(i).setLocation(loc);
            }
        }
        recomputePath();
    }

    public Pair<Double, Double> getDiameterAndAngle() {
        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);

        synchronized (vertices) {
            if (vertices.isEmpty())
                return new Pair<Double, Double>(0.0, 0.0);

            Vertex pivot = vertices.get(0);
            for (Vertex v : vertices) {
                double[] offsets = v.getLocation().getOffsetFrom(pivot.getLocation());
                path.lineTo(offsets[0], offsets[1]);
            }
        }
        path.closePath();

        double minAngle = 0, minDiameter = Double.MAX_VALUE;

        for (double theta = 0; theta < 180; theta += 0.1) {
            AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(theta));
            Shape rotated = t.createTransformedShape(path);

            if (rotated.getBounds2D().getHeight() < minDiameter) {
                minDiameter = rotated.getBounds2D().getHeight();
                minAngle = theta;
            }
        }

        return new Pair<Double, Double>(minDiameter, Math.toRadians(minAngle));
    }

    public double getDiameter() {
        return getDiameterAndAngle().first();
    }

    public double getArea() {
        double sum = 0;

        synchronized (vertices) {
            if (vertices.size() < 2)
                return 0;

            LocationType pivot = vertices.get(0).getLocation();

            for (int i = 1; i < vertices.size(); i++) {
                double[] cur = vertices.get(i).getLocation().getOffsetFrom(pivot);
                double[] prev = vertices.get(i - 1).getLocation().getOffsetFrom(pivot);
                sum += (prev[0] * cur[1]) - (prev[1] * cur[0]);
            }

            double[] cur = vertices.get(vertices.size() - 1).getLocation().getOffsetFrom(pivot);
            double[] prev = vertices.get(0).getLocation().getOffsetFrom(pivot);
            sum += (prev[0] * cur[1]) - (prev[1] * cur[0]);
        }

        return Math.abs(sum / 2);
    }

    public ArrayList<PolygonType> subAreas(int numAreas, double angle) {
        ArrayList<PolygonType> polygons = new ArrayList<>();
        double subAreaSize = getArea() / numAreas;

        GeneralPath shape = new GeneralPath();
        shape.moveTo(0, 0);
        synchronized (vertices) {
            if (vertices.isEmpty())
                return new ArrayList<>();
            Vertex pivot = vertices.get(0);
            for (Vertex v : vertices) {
                double[] offsets = v.getLocation().getOffsetFrom(pivot.getLocation());
                shape.lineTo(offsets[0], offsets[1]);
            }
        }

        shape.closePath();
        AffineTransform t = AffineTransform.getRotateInstance(angle);

        Shape rotated = t.createTransformedShape(shape);

        Rectangle2D bounds = rotated.getBounds2D();
        double startY = bounds.getY(), endY = bounds.getY() + 1;

        while (endY < bounds.getMaxY()) {

            Rectangle2D bounds2 = new Rectangle2D.Double(bounds.getMinX() - 10, startY, bounds.getWidth() + 20,
                    endY - startY);
            Area a = new Area(rotated);
            a.intersect(new Area(bounds2));

            double size = Areas.approxArea(a.getPathIterator(null));

            if (size >= subAreaSize || endY + 1 >= bounds.getMaxY()) {
                try {
                    PolygonType polygon = new PolygonType();
                    PathIterator pi = a.getPathIterator(t.createInverse());
                    double[] point = new double[6];
                    while (!pi.isDone()) {
                        pi.currentSegment(point);
                        LocationType loc = new LocationType(vertices.get(0).getLocation());
                        loc.translatePosition(point[0], point[1], 0);
                        polygon.addVertex(loc);
                        pi.next();
                    }
                    polygon.recomputePath();
                    polygons.add(polygon);
                }
                catch (NoninvertibleTransformException e) {
                    e.printStackTrace();
                }
                startY = endY;
            }

            endY += 1;
        }
        return polygons;
    }

    public ArrayList<PolygonType> split(double areaSize) {
        int numAreas = (int) Math.ceil(getArea() / areaSize);
        return subAreas(numAreas, getDiameterAndAngle().second());
    }

    public ArrayList<LocationType> getCoveragePath(double angle, double swathWidth, int corner) {
        return getCoveragePath(angle, swathWidth, 0, corner);
    }
    
    public ArrayList<LocationType> getCoveragePath(double angle, double swathWidth, double swathIncrement, int corner) {
        List<Point2D> points = new ArrayList<>();
        coverage = new GeneralPath();
        synchronized (vertices) {
            if (vertices.isEmpty())
                return new ArrayList<>();
            LocationType pivot = vertices.get(0).getLocation();

            points = vertices.stream().map(v -> v.getLocation().getOffsetFrom(pivot))
                    .map(offsets -> new Point2D.Double(offsets[0], offsets[1])).collect(Collectors.toList());
        }

        Point2D[] original = points.toArray(new Point2D[0]);
        Point2D[] dest = new Point2D[points.size()];

        AffineTransform t = AffineTransform.getRotateInstance(angle);
        AffineTransform inverse = AffineTransform.getRotateInstance(Math.toRadians(270) - angle);
        t.transform(original, 0, dest, 0, original.length);

        GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);

        for (int i = 0; i < dest.length; i++)
            path.lineTo(dest[i].getX(), dest[i].getY());

        path.lineTo(0, 0);
        path.closePath();
        Rectangle2D rect = path.getBounds2D();
        double margin = (rect.getHeight() % swathWidth) / 2;

        ArrayList<LocationType> ret = new ArrayList<>();

        int count = corner % 2;
        double increment = 0;
        
        for (double y = margin; y < rect.getHeight(); y += swathWidth + increment, count++, increment += swathIncrement) {

            double pos = y;

            if (corner > 1)
                pos = rect.getHeight() - y;

            Point2D pt1 = new Point2D.Double(rect.getMinX(), rect.getMinY() + pos);
            Point2D pt2 = new Point2D.Double(rect.getMaxX(), rect.getMinY() + pos);
            Line2D.Double lineBefore = new Line2D.Double(pt1, pt2);

            Area a = new Area(path);
            a.intersect(new Area(lineBefore.getBounds()));
            Rectangle2D intersection = a.getBounds2D();

            pt1.setLocation(intersection.getMinX(), pt1.getY());
            pt2.setLocation(intersection.getMaxX(), pt1.getY());
            inverse.transform(pt1, pt1);
            inverse.transform(pt2, pt2);

            LocationType pivot = new LocationType(vertices.get(0).getLocation());

            if (count % 2 == 0) {
                ret.add(new LocationType(pivot).translatePosition(-pt1.getY(), pt1.getX(), 0));
                ret.add(new LocationType(pivot).translatePosition(-pt2.getY(), pt2.getX(), 0));
            }
            else {
                ret.add(new LocationType(pivot).translatePosition(-pt2.getY(), pt2.getX(), 0));
                ret.add(new LocationType(pivot).translatePosition(-pt1.getY(), pt1.getX(), 0));
            }
        }

        return ret;
    }

    public ArrayList<LocationType> getCoveragePath(double swathWidth, int corner) {
        return getCoveragePath(swathWidth, 0, corner);
    }
    
    public ArrayList<LocationType> getCoveragePathInc(double swathWidth, double swathIncrement, int corner) {
        Pair<Double, Double> diamAng = getDiameterAndAngle();
        return getCoveragePath(diamAng.second(), swathWidth, swathIncrement, corner);
    }

    public double getPathLength(double swathWidth, int corner) {
        ArrayList<LocationType> path = getCoveragePath(swathWidth, corner);
        double length = 0;

        for (int i = 1; i < path.size(); i++) {
            length += path.get(i - 1).getHorizontalDistanceInMeters(path.get(i));
        }

        return length;
    }

    public double getPathLength(double angle, double swathWidth, int corner) {
        return getPathLength(angle, swathWidth, 0, corner);
    }
    
    public double getPathLength(double angle, double swathWidth, double swathIncrement, int corner) {
        ArrayList<LocationType> path = getCoveragePath(angle, swathWidth, swathIncrement, corner);
        double length = 0;

        for (int i = 1; i < path.size(); i++) {
            length += path.get(i - 1).getHorizontalDistanceInMeters(path.get(i));
        }

        return length;
    }

    public static Collection<PolygonType> loadGeoJsonPolygons(File f) throws Exception {
        ArrayList<PolygonType> result = new ArrayList<PolygonType>();
        FeatureCollection features = new ObjectMapper().readValue(Files.toByteArray(f), FeatureCollection.class);
        for (Feature feature : features.getFeatures()) {
            GeoJsonObject obj = feature.getGeometry();

            if (obj instanceof Polygon) {
                Polygon polygon = (Polygon) obj;
                PolygonType ptype = new PolygonType();
                for (List<LngLatAlt> pts : polygon.getCoordinates())
                    for (LngLatAlt pt : pts)
                        ptype.addVertex(new LocationType(pt.getLatitude(), pt.getLongitude()));
            }
        }
        return result;
    }

    public static void main(String[] args) {

        PolygonType pt = new PolygonType();
        pt.setColor(Color.yellow);
        LocationType loc = new LocationType(41, -8);

        LocationType loc1 = new LocationType(loc).translatePosition(-123, 34, 0);
        LocationType loc2 = new LocationType(loc).translatePosition(-290, -140, 0);
        LocationType loc3 = new LocationType(loc).translatePosition(-108, 380, 0);
        LocationType loc4 = new LocationType(loc).translatePosition(130, 70, 0);

        pt.addVertex(loc);
        pt.addVertex(loc1);
        pt.addVertex(loc2);
        pt.addVertex(loc3);
        pt.addVertex(loc4);
        pt.recomputePath();
        pt.getCoveragePath(20, 0);

        StateRenderer2D r2d = new StateRenderer2D(loc);
        r2d.addPostRenderPainter(pt, "Polygon");
        int i = 1;
        for (PolygonType p : pt.subAreas(2, Math.PI / 3)) {
            r2d.addPostRenderPainter(p, "Polygon" + (i++));
        }

        GuiUtils.testFrame(r2d);
    }
}
