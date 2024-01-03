/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.types.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.Material;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer3d.Obj3D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * 
 * @author ZP
 * 
 */
public class ScatterPointsElement extends AbstractElement {

    private LocationType lt = new LocationType();
    private List<Point3d> points = Collections.synchronizedList(new Vector<Point3d>());
    public static final int INFINITE_NUMBER_OF_POINTS = Integer.MAX_VALUE;
    private int numberOfPoints = INFINITE_NUMBER_OF_POINTS;
    private ColorMap cmap = ColorMapFactory.createGrayScaleColorMap();
    
    // TODO para saber os pontos que entrem e os que saem no 3d ...
    protected Point3d lastAdded = null;
    protected Point3d lastRemoved = null;
    private int gradientcolor = 0;

    public ScatterPointsElement() {
        super();
    }

    public ScatterPointsElement(Color baseColor) {
        super();
        if (baseColor == Color.BLACK)
            baseColor = Color.LIGHT_GRAY;
        cmap = new InterpolationColorMap(new double[] { 0.0, 1.0 }, new Color[] { Color.BLACK, baseColor });
    }

    public ScatterPointsElement(MapGroup mg, MapType map) {
        super(mg, map);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    public ScatterPointsElement(MapGroup mg, MapType map, Color baseColor) {
        super(mg, map);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));

        setColor(baseColor, Color.gray.darker().darker());
    }

    
    public void setColor(Color tail, Color head) {
        cmap = new InterpolationColorMap(new double[] { 0.0, 1.0 }, new Color[] {
                new Color(tail.getRed(), tail.getGreen(), tail.getBlue()),
                new Color(head.getRed(), head.getGreen(), head.getBlue()) });
    }

    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return false;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        return null;
    }

    @Override
    public String getType() {
        return "Scatter points";
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Graphics2D g2 = (Graphics2D) g.create();
        // double zoom = renderer.getZoom();

        Point2D ofs = renderer.getScreenPosition(getCenterLocation());

        g2.translate(ofs.getX(), ofs.getY());
        // g2.rotate(rotation);

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // lock.lock();
        int curPoint = 0;

        synchronized (points) {
            for (Point3d pt : points) {
                LocationType locT = new LocationType(getCenterLocation());
                locT.translatePosition(pt.x, pt.y, pt.z);
                locT.convertToAbsoluteLatLonDepth();

                Point2D ofsT = renderer.getScreenPosition(locT);
                if (!renderer.contains((int)ofsT.getX(), (int)ofsT.getY())) {
                    continue;
                }
                double transX = ofsT.getX() - ofs.getX();
                double transY = ofsT.getY() - ofs.getY();

                g2.setColor(cmap.getColor(1.0 - ((double) curPoint++ / (double) points.size())));
                g2.draw(new Line2D.Double(transX, transY, transX, transY));
            }
        }
        // lock.unlock();
        g2.dispose();
    }

    @Override
    public int getLayerPriority() {
        return 0;
    }

    @Override
    public LocationType getCenterLocation() {
        return lt;
    }

    @Override
    public void setCenterLocation(LocationType l) {
        lt.setLocation(l);
    }

    public void clearPoints() {

        // lock.lock();
        synchronized (points) {
            points.clear();
        }

        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(getParentMap());
        mce.setMapGroup(getMapGroup());
        mce.setChangedObject(this);
        getParentMap().warnChangeListeners(mce);
        // lock.unlock();
    }

    public void addPoint(LocationType loc) {
        if (loc.getDistanceInMeters(getCenterLocation()) > 3000) {
            setCenterLocation(loc);
            addPoint(0, 0, 0);
            clearPoints();
        }
        else {
            double[] offsets = loc.getOffsetFrom(getCenterLocation());
            addPoint(offsets[0], offsets[1], offsets[2]);
        }
    }

    private void addPoint(double offsetNorth, double offsetEast, double offsetDown) {
        // lock.lock();
        synchronized (points) {
            if (numberOfPoints != INFINITE_NUMBER_OF_POINTS && points.size() >= numberOfPoints) {
                points.remove(0);
            }
            points.add(new Point3d(offsetNorth, offsetEast, offsetDown));
        }
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(getParentMap());
        mce.setMapGroup(getMapGroup());
        mce.setChangedObject(this);
        setLastAdded(new Point3d(offsetNorth, offsetEast, offsetDown));
        getParentMap().warnChangeListeners(mce);
        // getMapGroup().warnListeners(mce);
        // NeptusLog.pub().info("<###>I send mce to "+getMapGroup());
        // lock.unlock();
        setLastAdded(null);
    }

    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    public void setNumberOfPoints(int numberOfPoints) {
        // lock.lock();
        if (numberOfPoints < 0)
            this.numberOfPoints = INFINITE_NUMBER_OF_POINTS;
        else
            this.numberOfPoints = numberOfPoints;

        synchronized (points) {
            while (points.size() > numberOfPoints)
                points.remove(0);
        }

        // lock.unlock();
    }

    // FIXME REMOVE THIS
    public TransformGroup getModel3D() {
        Shape3D shape3D;
        TransformGroup model = new TransformGroup();
        Appearance appearance = new Appearance();

        Material mat = new Material();
        gradientcolor++;
        NeptusLog.pub().info("<###> " + gradientcolor);
        // 0xfffffffB
        // Color color=Color.WHITE;
        // Color color=new Color(gradientcolor, gradientcolor, gradientcolor);
        // Color color=new Color((gradientcolor&0x00ff0000)>>16, (gradientcolor&0x0000ff00)>>8,gradientcolor&0x000000ff
        // );
        Color color = new Color(gradientcolor);
        Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
        c.set(color);
        mat.setEmissiveColor(c);
        mat.setAmbientColor(c);
        mat.setDiffuseColor(c);
        // mat.setSpecularColor(c);
        // mat.setShininess(20.0f);
        appearance.setMaterial(mat);
        // appearance.set

        Point3d[] myCoords = null;
        synchronized (points) {
            myCoords = new Point3d[points.size() * 4];
            int i = 0;
            for (Point3d j : points) {
                myCoords[i++] = new Point3d(j.x - 0.5, j.y, j.z);
                myCoords[i++] = new Point3d(j.x + 0.5, j.y, j.z);
                myCoords[i++] = new Point3d(j.x, j.y - 0.5, j.z);
                myCoords[i++] = new Point3d(j.x, j.y + 0.5, j.z);
            }
        }

        LineArray myLines = new LineArray(myCoords.length, LineArray.COORDINATES);
        myLines.setCoordinates(0, myCoords);

        RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                true, // boolean depthBufferWriteEnable,
                0.5f, // float alphaTestValue,
                RenderingAttributes.ALWAYS, // int alphaTestFunction,
                true, // boolean visible,
                true, // boolean ignoreVertexColors,
                false, // boolean rasterOpEnable,
                RenderingAttributes.ROP_COPY // int rasterOp
        );

        ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
        appearance.setRenderingAttributes(renderingAttributes);
        appearance.setColoringAttributes(coloringAttributes);
        shape3D = new Shape3D(myLines, appearance);

        // Transform3D t=new Transform3D();
        // Point3d p=new Point3d();
        // double[] offsets = getCenterLocation().getOffsetFrom(new LocationType());
        // t.set(offsets[1],offsets[0],offsets[2] ));
        // moset
        model.addChild(shape3D);
        System.err.println("O objecto está foi criado e está pronto para adicionar");
        /*
         * try { Thread.sleep(10000); } catch (InterruptedException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */
        return model;

    }

    public Point3d getLastAdded() {
        return lastAdded;

    }

    public Obj3D getFullObj3D(LocationType location) {
        Obj3D obj = new Obj3D();

        obj.setPos(this.getCenterLocation().getOffsetFrom(location));
        obj.setRoll(this.getRollRad());
        obj.setPitch(this.getPitchRad());
        obj.setYaw(this.getYawRad());

        synchronized (points) {
            for (Point3d j : points) {
                setLastAdded(j);
                obj.addObj3D(getLastAddedModel3D()[0]);
            }
        }
        setLastAdded(null);

        /*
         * System.err.println("O objecto está foi criado e está pronto para adicionar"); try { Thread.sleep(10000); }
         * catch (InterruptedException e) { // TODO Auto-generated catch block e.printStackTrace(); }
         */

        return obj;
    }

    public Obj3D[] getLastAddedModel3D() {
        Obj3D mark = new Obj3D();
        Obj3D mark5 = new Obj3D();
        {
            Shape3D shape3D;
            TransformGroup model = new TransformGroup();
            Appearance appearance = new Appearance();

            Material mat = new Material();
            Color3f c = new Color3f(1.0f, 0.0f, 0.0f);

            gradientcolor++;
            // 0xfffffffB
            // Color color=new Color(gradientcolor, gradientcolor, gradientcolor);
            // Color color=new Color(gradientcolor|0x000000ff,
            // (gradientcolor>>8)|0x000000ff,(gradientcolor>>16)|0x000000ff );
            Color color = invertColor(new Color(gradientcolor));
            c.set(color);
            mat.setEmissiveColor(c);
            mat.setAmbientColor(c);
            mat.setDiffuseColor(c);
            appearance.setMaterial(mat);

            // cruz
            Point3d myCoords[] = new Point3d[4];
            myCoords[0] = new Point3d(-0.5, 0, 0);
            myCoords[1] = new Point3d(+0.5, 0, 0);
            myCoords[2] = new Point3d(0, -0.5, 0);
            myCoords[3] = new Point3d(0, 0.5, 0);

            LineArray myLines = new LineArray(myCoords.length, LineArray.COORDINATES);
            myLines.setCoordinates(0, myCoords);

            RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                    true, // boolean depthBufferWriteEnable,
                    0.5f, // float alphaTestValue,
                    RenderingAttributes.ALWAYS, // int alphaTestFunction,
                    true, // boolean visible,
                    true, // boolean ignoreVertexColors,
                    false, // boolean rasterOpEnable,
                    RenderingAttributes.ROP_COPY // int rasterOp
            );

            ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
            appearance.setRenderingAttributes(renderingAttributes);
            appearance.setColoringAttributes(coloringAttributes);
            shape3D = new Shape3D(myLines, appearance);
            model.addChild(shape3D);

            mark.setModel3D(model);
            mark.setPos(getLastAdded());

        }

        Point3d ptn5 = new Point3d();
        if (points.size() >= 20)
            ptn5 = points.get(points.size() - 20);
        {
            Shape3D shape3D;
            TransformGroup model = new TransformGroup();
            Appearance appearance = new Appearance();

            Material mat = new Material();
            Color3f c = new Color3f(1.0f, 0.0f, 0.0f);
            c.set(Color.BLACK);
            mat.setEmissiveColor(c);
            mat.setAmbientColor(c);
            mat.setDiffuseColor(c);
            appearance.setMaterial(mat);

            // cruz
            Point3d myCoords[] = new Point3d[4];
            myCoords[0] = new Point3d(-0.5, 0, 0);
            myCoords[1] = new Point3d(+0.5, 0, 0);
            myCoords[2] = new Point3d(0, -0.5, 0);
            myCoords[3] = new Point3d(0, 0.5, 0);

            LineArray myLines = new LineArray(myCoords.length, LineArray.COORDINATES);
            myLines.setCoordinates(0, myCoords);

            RenderingAttributes renderingAttributes = new RenderingAttributes(true, // boolean depthBufferEnable,
                    true, // boolean depthBufferWriteEnable,
                    0.5f, // float alphaTestValue,
                    RenderingAttributes.ALWAYS, // int alphaTestFunction,
                    true, // boolean visible,
                    true, // boolean ignoreVertexColors,
                    false, // boolean rasterOpEnable,
                    RenderingAttributes.ROP_COPY // int rasterOp
            );

            ColoringAttributes coloringAttributes = new ColoringAttributes(c, ColoringAttributes.SHADE_GOURAUD);
            appearance.setRenderingAttributes(renderingAttributes);
            appearance.setColoringAttributes(coloringAttributes);
            shape3D = new Shape3D(myLines, appearance);
            model.addChild(shape3D);

            mark5.setModel3D(model);
            mark5.setPos(ptn5);

        }

        // return (Obj3D[]) pts.toArray();

        return new Obj3D[] { mark, mark5 };
    }

    public Point3d getLastAdded(int ptn) {

        if (points.size() >= ptn)
            return points.get(points.size() - ptn);
        else
            return null;

    }

    public void setLastAdded(Point3d lastAdded) {
        this.lastAdded = lastAdded;

    }

    public Point3d getLastRemoved() {
        return lastRemoved;
    }

    public void setLastremoved(Point3d lastRemoved) {
        this.lastRemoved = lastRemoved;
    }

    public List<Point3d> getPoints() {
        return points;
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_OTHER;
    }
}
