/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 15, 2005
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.util.Vector;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParallelepipedParameters;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.texture.TextureType;
import pt.lsts.neptus.types.texture.TexturesHolder;

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

    protected Color color = Color.red;
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

            try {
                Node n = doc.selectSingleNode("//filled");
                setFilled(n.getText().equals("true"));
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
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
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        root.addElement("type").addText(getType());
        root.addElement("filled").addText(""+isFilled());
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
        paramsPanel.setFilled(isFilled());
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
            setFilled(paramsPanel.isFilled());
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

    @Override
    public Vector<LocationType> getShapePoints() {
        LocationType center = new LocationType(getCenterLocation());
        Vector<LocationType> locs = new Vector<>();

        double width = getWidth();
        double length = getLength();
        double yaw = getYawRad();

        Ellipse2D.Double tmp = new Ellipse2D.Double(-width / 2, -length / 2, width, length);

        AffineTransform rot = new AffineTransform();
        rot.rotate(yaw);

        PathIterator it = tmp.getPathIterator(rot, 2f);

        while(!it.isDone()) {

            double[] offsets = new double[6];

            int op = it.currentSegment(offsets);
            if (op == PathIterator.SEG_MOVETO || op == PathIterator.SEG_LINETO) {
                LocationType loc = new LocationType(center);
                loc.translatePosition(offsets[0], offsets[1], 0);
                locs.add(loc);
            }
            it.next();
        }

        return locs;
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
