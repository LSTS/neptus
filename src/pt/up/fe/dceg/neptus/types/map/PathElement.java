/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * Mar 29, 2005
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.vecmath.Point3d;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.objparams.DrawingParameters;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author Ze Carlos
 */
public class PathElement extends AbstractElement {
    protected static final String DEFAULT_ROOT_ELEMENT = "path";

    protected Vector<Point3d> offsets3D = new Vector<Point3d>();

    /**
     * This shape is in meters [east, -north] point tuples.
     */
    private final GeneralPath myPath;
    private GeneralPath scaledPath = null;
    private final DrawingParameters params = new DrawingParameters();
    private boolean finished = false;
    protected Color myColor = params.getColor();
    private final LocationType tmp = new LocationType();
    private Point2D nextPoint = null;
    private double[] bounds3d = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
            -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE };
    public static final int SOUTH_COORD = 0, NORTH_COORD = 1, DOWN_COORD = 2, UP_COORD = 3, WEST_COORD = 4,
            EAST_COORD = 5;
    private Stroke stroke = new BasicStroke(1);
    private boolean isShape = false;
    private boolean fill = true;
    boolean firstPoint = true;

    /**
     * 
     */
    public PathElement() {
        super();
        this.myPath = new GeneralPath();
        // myPath.moveTo((float)0, (float)0);
    }

    /**
     * @param xml
     */
    public PathElement(String xml) {
        this.myPath = new GeneralPath();
        // myPath.moveTo((float)0, (float)0);
        load(xml);
    }

    public PathElement(MapGroup mg, MapType parentMap, double realX, double realY) {
        super(mg, parentMap);
        LocationType lt = new LocationType();
        lt.setOffsetEast(realX);
        lt.setOffsetNorth(realY);

        setCenterLocation(lt);
        this.myPath = new GeneralPath();
        // myPath.moveTo((float)0, (float)0);
    }

    public PathElement(MapGroup mg, MapType parentMap, LocationType firstPoint) {
        super(mg, parentMap);
        setCenterLocation(firstPoint);
        this.myPath = new GeneralPath();
        this.myPath.moveTo(0, 0);
    }

    @Override
    public boolean load(Element elem) {
        offsets3D.clear();
        super.load(elem);
        Node nd;
        addPoint(0, 0, 0, false);
        boolean lastPointIsZero = true;

        try {
            List<?> lst = doc.selectNodes("//translation");
            Iterator<?> it = lst.iterator();
            while (it.hasNext()) {
                nd = (Node) it.next();
                String vlx = nd.selectSingleNode("n").getText();
                double x = Double.parseDouble(vlx);
                String vly = nd.selectSingleNode("e").getText();
                double y = Double.parseDouble(vly);
                String vlz = nd.selectSingleNode("d").getText();
                double z = Double.parseDouble(vlz);
                // Point3d pt = new Point3d(x,y,z);
                // offsets3D.add(pt);

                if (lastPointIsZero) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    else
                        lastPointIsZero = false;
                }
                addPoint(y, x, z, false);
            }

            nd = doc.selectSingleNode("//color");
            if (nd != null) {
                String rS = nd.selectSingleNode("r").getText();
                String gS = nd.selectSingleNode("g").getText();
                String bS = nd.selectSingleNode("b").getText();
                int rr = Integer.parseInt(rS);
                int gg = Integer.parseInt(gS);
                int bb = Integer.parseInt(bS);
                this.setMyColor(new Color(rr, gg, bb));
            }
            else {
                System.out.println("Didn't found color!!");
            }

            nd = doc.selectSingleNode("//filled");
            if (nd != null) {
                if (nd.getText().equalsIgnoreCase("true"))
                    isShape = true;
                else
                    isShape = false;
            }
            else {
                System.out.println("Unable to find filled tag (defaulting to false)!!");
            }

            setFinished(true);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error(this + ":XML not recognized!!!");
            isLoadOk = false;
            return false;
        }
        isLoadOk = true;
        return true;
    }

    @Override
    public String getType() {
        return "Path";
    }

    public Vector<Point3d> getPoints() {
        return offsets3D;
    }

    public void setPoints(Vector<Point3d> newPoints) {
        this.offsets3D.clear();
        for (Point3d pt : newPoints) {
            LocationType lt = new LocationType();
            lt.setLocation(getCenterLocation());
            this.addPoint(pt.y, pt.x, pt.z, false);
        }
    }

    public Color getMyColor() {
        return myColor;
    }

    public void setMyColor(Color myColor) {
        this.myColor = myColor;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    @Override
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
     */
    @Override
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /**
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    @Override
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /**
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    @Override
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        Element typeE = root.addElement("filled");
        typeE.setText(isShape ? "true" : "false");

        if (getMyColor() != null) {
            Element colorE = root.addElement("color");
            colorE.addElement("r").setText(Integer.toString(getMyColor().getRed()));
            colorE.addElement("g").setText(Integer.toString(getMyColor().getGreen()));
            colorE.addElement("b").setText(Integer.toString(getMyColor().getBlue()));
        }
        else {
            System.out.println("Getmycolor = null (" + getId() + ")");
        }

        if (offsets3D.size() == 0)
            offsets3D.add(new Point3d(0, 0, 0));
        Iterator<Point3d> it = offsets3D.iterator();
        while (it.hasNext()) {
            Point3d pt = it.next();
            Element transl = root.addElement("translation");
            transl.addElement("n").addText(Double.toString(pt.x));
            transl.addElement("e").addText(Double.toString(pt.y));
            transl.addElement("d").addText(Double.toString(pt.z));
        }
        return document;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        if (finished) {
            if (nextPoint != null) {
                tmp.setOffsetEast(nextPoint.getX());
                tmp.setOffsetNorth(nextPoint.getY());
                // double offs[] = tmp.getOffsetFrom(getCenterLocation());
                // myPath.lineTo((float)offs[1], (float)offs[0]);
                nextPoint = null;
            }
        }
    }

    public Point2D getNextPoint() {
        return nextPoint;
    }

    public void setNextPoint(Point2D nextPoint) {
        this.nextPoint = nextPoint;
    }

    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        double[] offsets = point.getOffsetFrom(getCenterLocation());
        Point2D pt = new Point2D.Double(offsets[1], -offsets[0]);
        return myPath.contains(pt);
    }

    @Override
    public int getLayerPriority() {
        return 5;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        params.getShapeCheck().setSelected(isShape);
        params.getColorField().setBackground(getMyColor());
        params.setEditable(editable);
        return params;
    }

    public boolean paramsOK(JPanel paramsPanel) {
        return true;
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {
        myColor = params.getColor();
        isShape = params.getShapeCheck().isSelected();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {

        double zoom = renderer.getZoom();

        Color c = myColor;

        if (!isFinished() || isSelected())
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
        else
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 175));

        Point2D tt = renderer.getScreenPosition(getCenterLocation());

        g.drawLine((int) tt.getX(), (int) tt.getY(), (int) tt.getX(), (int) tt.getY());

        if (nextPoint != null) {
            scaledPath = (GeneralPath) myPath.clone();
            tmp.setOffsetEast(nextPoint.getX());
            tmp.setOffsetNorth(nextPoint.getY());
        }
        else {
            scaledPath = (GeneralPath) myPath.clone();
            if (isShape)
                scaledPath.lineTo(0, 0);
        }

        AffineTransform transform = new AffineTransform();
        transform.translate(tt.getX(), tt.getY());
        transform.scale(zoom, zoom);
        transform.rotate(-renderer.getRotation());
        scaledPath.transform(transform);
        g.setStroke(stroke);
        g.draw(scaledPath);
        if (isShape && isFill()) {
            g.setColor(new Color(myColor.getRed(), myColor.getGreen(), myColor.getBlue(), 100));
            g.fill(scaledPath);
        }
    }

    public void addPoint(double eastOffset, double northOffset, double depthOffset, boolean absolute) {

        if (eastOffset < bounds3d[WEST_COORD])
            bounds3d[WEST_COORD] = eastOffset;
        if (eastOffset > bounds3d[EAST_COORD])
            bounds3d[EAST_COORD] = eastOffset;

        if (northOffset < bounds3d[SOUTH_COORD])
            bounds3d[SOUTH_COORD] = northOffset;
        if (northOffset > bounds3d[NORTH_COORD])
            bounds3d[NORTH_COORD] = northOffset;

        if (depthOffset < bounds3d[DOWN_COORD])
            bounds3d[DOWN_COORD] = depthOffset;
        if (depthOffset > bounds3d[UP_COORD])
            bounds3d[UP_COORD] = depthOffset;

        if (!absolute) {
            if (!firstPoint)
                myPath.lineTo((float) eastOffset, -(float) northOffset);
            else {
                firstPoint = false;
                myPath.moveTo((float) eastOffset, -(float) northOffset);
            }
            offsets3D.add(new Point3d(northOffset, eastOffset, depthOffset));
        }
        else {
            System.out.println("add point absolute!: " + ReflectionUtil.getCallerStamp());
            tmp.setOffsetEast(eastOffset);
            tmp.setOffsetNorth(northOffset);
            double offsets[] = tmp.getOffsetFrom(getCenterLocation());
            if (!firstPoint)
                myPath.lineTo((float) offsets[1], (float) offsets[0]);
            else {
                firstPoint = false;
                myPath.moveTo((float) offsets[1], (float) offsets[0]);
            }

            offsets3D.add(new Point3d(offsets[0], offsets[1], offsets[2]));
        }
    }

    public Point3d[] getPath() {
        Vector<Point3d> points = offsets3D;

        if (points == null || points.size() <= 1)
            return new Point3d[] {};

        Point3d[] pts = new Point3d[(points.size() * 2) - 2];
        int i = 1;
        int x = 1;
        pts[0] = points.firstElement();
        while (i < points.size() - 1) {
            pts[x] = points.get(i);
            x++;
            pts[x] = points.get(i);
            i++;
            x++;
        }
        pts[pts.length - 1] = points.get(points.size() - 1);

        return pts;
    }

    public double[] getBounds3d() {
        return bounds3d;
    }

    public void setBounds3d(double[] bounds3d) {
        this.bounds3d = bounds3d;
    }

    public LocationType getCenterPoint() {
        // System.out.println("south="+bounds3d[SOUTH_COORD]);
        // System.out.println("north="+bounds3d[NORTH_COORD]);
        // System.out.println("east="+bounds3d[EAST_COORD]);
        // System.out.println("west="+bounds3d[WEST_COORD]);
        //
        LocationType lt = new LocationType(getCenterLocation());
        lt.translatePosition((bounds3d[NORTH_COORD] + bounds3d[SOUTH_COORD]) / 2,
                (bounds3d[EAST_COORD] + bounds3d[WEST_COORD]) / 2, (bounds3d[UP_COORD] + bounds3d[DOWN_COORD]) / 2);
        return lt;
    }

    @Override
    public Vector<LocationType> getShapePoints() {
        if (!isShape)
            return super.getShapePoints();

        Vector<LocationType> ret = new Vector<LocationType>();

        LocationType lt = new LocationType(getCenterLocation());

        ret.add(lt); // center is first point
        Vector<Point3d> myPts = getPoints();

        for (Point3d pt : myPts) {
            LocationType tmp = new LocationType(lt);
            tmp.translatePosition(pt.x, pt.y, pt.z);
            ret.add(tmp);
            // System.out.println(tmp.getOffsetNorth()+", "+tmp.getOffsetEast());
        }

        return ret;
    }

    public boolean isShape() {
        return isShape;
    }

    public void setShape(boolean isShape) {
        this.isShape = isShape;
    }

    /**
     * @return the fill
     */
    public boolean isFill() {
        return fill;
    }

    /**
     * @param fill the fill to set
     */
    public void setFill(boolean fill) {
        this.fill = fill;
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_PATH;
    }
}
