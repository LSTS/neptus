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
 * Mar 15, 2005
 * $Id:: GeometryElement.java 9845 2013-02-01 19:53:46Z pdias             $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;
import java.awt.Image;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.objparams.ParallelepipedParameters;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.texture.TextureType;
import pt.up.fe.dceg.neptus.types.texture.TexturesHolder;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Ze Carlos
 * @author Paulo Dias
 */
public abstract class GeometryElement extends AbstractElement implements RotatableElement, ScalableElement {
    protected static final String DEFAULT_ROOT_ELEMENT = "geometry";

    public static final String PARALLELEPIPED = "Parallelepiped";
    public static final String ELLIPSOID = "Ellipsoid";
    public static final String CYLINDER = "Cylinder";
    private String geometryType = "Unknown";

    protected double width = 1; // dimensionX
    protected double length = 1; // dimensionY
    protected double height = 1; // dimensionZ
    // public double width = 1, length = 1, height = 1;

    protected Color color = null;
    protected TextureType textureType = null;

    // Material
    public Image texture = null;
    public float shininess = 0.3f;

    ParallelepipedParameters paramsPanel = new ParallelepipedParameters();

    public GeometryElement() {
        super();
    }

    public GeometryElement(String xml) {
        load(xml);
    }

    public GeometryElement(MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    @Override
    public boolean load(Element elem) {
        if (!super.load(elem))
            return false;

        try {

            this.setType(doc.selectSingleNode("//type").getText());
            this.setDimensionX(Double.parseDouble(doc.selectSingleNode("//x-dim").getText()));
            this.setDimensionY(Double.parseDouble(doc.selectSingleNode("//y-dim").getText()));
            this.setDimensionZ(Double.parseDouble(doc.selectSingleNode("//z-dim").getText()));

            Node nd = doc.selectSingleNode("//color");
            if (nd != null) {
                String rS = nd.selectSingleNode("r").getText();
                String gS = nd.selectSingleNode("g").getText();
                String bS = nd.selectSingleNode("b").getText();
                int rr = Integer.parseInt(rS);
                int gg = Integer.parseInt(gS);
                int bb = Integer.parseInt(bS);
                this.setMyColor(new Color(rr, gg, bb));
            }

            nd = doc.selectSingleNode("//texture");
            if (nd != null) {
                String textureName = nd.getText().toLowerCase();
                // textureType = TexturesHolder.getTextureByName(textureName);
                setTextureType(TexturesHolder.getTextureByName(textureName));
                // System.err.println("textua geometry"+texture+", "+textureName);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this + ":XML not recognized!!!");
            isLoadOk = false;
            return false;
        }
        isLoadOk = true;
        return true;
    }

    public String getType() {
        return geometryType;
    }

    public void setType(String geometryType) {
        this.geometryType = geometryType;
    }

    public double getDimensionX() {
        return width;
    }

    public void setDimensionX(double dimensionX) {
        this.width = dimensionX;
    }

    public double getDimensionY() {
        return length;
    }

    public void setDimensionY(double dimensionY) {
        this.length = dimensionY;
    }

    public double getDimensionZ() {
        return height;
    }

    public void setDimensionZ(double dimensionZ) {
        this.height = dimensionZ;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Color getMyColor() {
        return color;
    }

    public void setMyColor(Color myColor) {
        this.color = myColor;
        paramsPanel.setColor(myColor);
    }

    /**
     * @return Returns the texture.
     */
    public TextureType getTextureType() {
        return textureType;
    }

    /**
     * @param texture The texture to set.
     */
    public void setTextureType(TextureType texture) {
        this.textureType = texture;
        if (this.textureType != null) {
            this.texture = this.textureType.getTextureImage();
            this.shininess = this.textureType.getShininess();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
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
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        root.addElement("type").addText(getType());
        Element dim = root.addElement("dimensions");
        dim.addElement("x-dim").addText(Double.toString(getDimensionX()));
        dim.addElement("y-dim").addText(Double.toString(getDimensionY()));
        dim.addElement("z-dim").addText(Double.toString(getDimensionZ()));

        if (getMyColor() != null) {
            Element colorE = root.addElement("color");
            colorE.addElement("r").setText(Integer.toString(getMyColor().getRed()));
            colorE.addElement("g").setText(Integer.toString(getMyColor().getGreen()));
            colorE.addElement("b").setText(Integer.toString(getMyColor().getBlue()));
        }

        if (getTextureType() != null) {
            Element textureE = root.addElement("texture");
            textureE.setText(getTextureType().getName());
        }

        return document;
    }

    @Override
    public int getLayerPriority() {
        return 0;
    }

    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        double dist = lt.getDistanceInMeters(getCenterLocation());
        double angle = getCenterLocation().getXYAngle(lt);

        double diff[];

        if (getYawDeg() != 0) {
            diff = new double[3];
            angle -= getYawRad();
            diff[0] = dist * Math.cos(angle);
            diff[1] = dist * Math.sin(angle);
        }
        else {
            diff = lt.getOffsetFrom(getCenterLocation());
        }
        double maxDiffX = width / 2;
        double maxDiffY = length / 2;

        return Math.abs(diff[1]) <= maxDiffX && Math.abs(diff[0]) <= maxDiffY;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        paramsPanel.setDimensions(width, length, height);
        paramsPanel.setRotation(getRollDeg(), getPitchDeg(), getYawDeg());
        if (getCenterLocation() == null) {
            setCenterLocation(new LocationType());
            getCenterLocation().setLocation(getMapGroup().getCoordinateSystem());
        }

        paramsPanel.setLocationType(getCenterLocation());
        paramsPanel.setColor(color);
        paramsPanel.getTexturesCombo().setSelectedTexture(getTextureType());
        paramsPanel.setEditable(editable);

        return paramsPanel;
    }

    @Override
    public void initialize(ParametersPanel pPanel) {
        if (!(paramsPanel instanceof ParallelepipedParameters)) {
            System.err.println("Not my parameters!... Default parameters will be set.");
        }
        else {
            paramsPanel = (ParallelepipedParameters) pPanel;

            setCenterLocation(paramsPanel.getLocationType());

            double dim[] = paramsPanel.getDimension();
            this.width = dim[0];
            this.length = dim[1];
            this.height = dim[2];

            double rot[] = paramsPanel.getRotation();
            setRollDeg(rot[0]);
            setPitchDeg(rot[1]);
            setYawDeg(rot[2]);

            this.color = paramsPanel.getChosenColor();
            setTextureType(paramsPanel.getSelectedTexture());
        }
    }

    public void rotateLeft(double ammount) {
        setYawDeg(getYawDeg() - ammount);

    }

    public void rotateRight(double ammount) {
        setYawDeg(getYawDeg() + ammount);
    }

    public void grow(double ammount) {
        // Do Parallelepiped
        double maxDim = Math.max(width, height);
        maxDim = Math.max(maxDim, length);

        this.width += ammount * (width / maxDim);
        this.height += ammount * (height / maxDim);
        this.length += ammount * (length / maxDim);

        // Da Elipse e Cilindro
        // width += ammount;
        // length += ammount * (length/width);
        // height += ammount * (height/width);

    }

    public void shrink(double ammount) {
        // Da Elipse e Cilindro
        final double minimum = 0.1;

        if ((width - ammount) < minimum)
            return;

        if ((length - ammount * (length / width)) < minimum)
            return;

        if ((height - ammount * (height / width)) < minimum)
            return;

        // width -= ammount;
        // length -= ammount * (length/width);
        // height -= ammount * (height/width);

        // Do Parallelepiped
        double maxDim = Math.max(width, height);
        maxDim = Math.max(maxDim, length);

        this.width -= ammount * (width / maxDim);
        this.height -= ammount * (height / maxDim);
        this.length -= ammount * (length / maxDim);

    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double[] getDimension() {
        return new double[] { getWidth(), getLength(), getHeight() };
    }

    public void setDimension(double[] newDimension) {
        if (newDimension.length != 3) {
            NeptusLog.pub()
                    .error(new Exception("Tried to set the dimension with an invalid array (size="
                            + newDimension.length + ")"));
            return;
        }
        setWidth(newDimension[0]);
        setLength(newDimension[1]);
        setHeight(newDimension[2]);
    }

    @Override
    public double getTopHeight() {
        return getCenterLocation().getHeight() + getHeight() / 2;
    }
}
