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
 * 18/Jun/2005
 */
package pt.lsts.neptus.types.map;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.io.File;

import javax.swing.JPanel;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ImageObjectParameters;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.UTMCoordinates;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Refactored in 06/11/2006.
 * 
 * @author Paulo Dias
 * @author Ze Carlos
 * @author RJPG
 */
public class ImageElement extends AbstractElement implements ScalableElement, RotatableElement {
    protected static final String DEFAULT_ROOT_ELEMENT = "image";

    public static final int DEFAULT_RESOLUTION = 100;

    private String imageFileName = null;
    private double imageScale = 1.0; // meters per pixel
    private double imageScaleV = Double.NaN; // meters per pixel

    private String bathymetricImageFileName = null;

    private boolean isBathymetric = false;
    private double maxHeight = 0.0, maxDepth = 20.0; // max values
    private double resolution = DEFAULT_RESOLUTION;

    protected String originalFilePath = "";

    boolean selected = false;
    private Image image = null;
    private Image heightImage = null; // depth image

    private ImageObjectParameters params = new ImageObjectParameters();
    /**
     * 
     */
    public ImageElement() {
        super();
    }

    public ImageElement(File imgFile, File worldFile, LocationType utmReference) throws Exception {
        originalFilePath = imgFile.getAbsolutePath();
        setId(imgFile.getName());
        String[] lines = FileUtil.getFileAsString(worldFile).split("\n");

        image = ImageUtils.getImage(imgFile.getAbsolutePath());

        if (lines.length != 6)
            throw new Exception("World file not understood (6 text lines expected but "+lines.length+" lines found)");

        double scaleX = Double.parseDouble(lines[0]);
        double scaleY = Double.parseDouble(lines[3]);
        double coordX = Double.parseDouble(lines[4]);
        double coordY = Double.parseDouble(lines[5]);

        if (scaleX != -scaleY)
            throw new Exception("World file not understood (x and y scales differ)");

        double rotX = Double.parseDouble(lines[1]);
        double rotY = Double.parseDouble(lines[2]);

        if (rotX != 0 || rotY != 0)
            throw new Exception("World files with rotation are not supported");
        
        utmReference.convertToAbsoluteLatLonDepth();
        UTMCoordinates refCoords = new UTMCoordinates(utmReference.getLatitudeDegs(), utmReference.getLongitudeDegs());
        UTMCoordinates coords2 = new UTMCoordinates(coordX, coordY, refCoords.getZoneNumber(), refCoords.getZoneLetter());
        coordX = coords2.getLatitudeDegrees();
        coordY = coords2.getLongitudeDegrees();
        
        double scale = Math.abs(scaleX);

        setImageFileName(imgFile.getAbsolutePath());
        setImageScale(scale);
        
        double width = image.getWidth(null) * scaleX;
        double height = image.getHeight(null) * scaleY;
        
        LocationType center = new LocationType(coordX, coordY);
        center.translatePosition(height / 2, width / 2, 0);
        
        setCenterLocation(center);
    }

    /**
     * @param xml
     */
    public ImageElement(String xml) {
        // super(xml);
        load(xml);
    }

    public ImageElement(String xml, String originalFilePath) {
        // super(xml);
        this.originalFilePath = originalFilePath;
        load(xml);
    }

    public ImageElement(MapGroup mg, MapType map) {
        super(mg, map);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.map.MapElement#getType()
     */
    public String getType() {
        return "Image";
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.map.AbstractElement#load(org.dom4j.Element)
     */
    @Override
    public boolean load(Element elem) {
        if (!super.load(elem))
            return false;

        try {
            // doc = DocumentHelper.parseText(xml);
            Node nd = doc.selectSingleNode("//href");
            if (nd != null) {
                if ("".equals(originalFilePath))
                    this.imageFileName = nd.getText();
                else
                    this.imageFileName = ConfigFetch.resolvePathWithParent(originalFilePath, nd.getText());

                this.image = ImageUtils.getImage(this.imageFileName);
            }
            nd = doc.selectSingleNode("//scale");
            if (nd == null) {
                this.imageScale = 1.0;
                this.imageScaleV = Double.NaN;
            }
            else {
                this.imageScale = Double.parseDouble(nd.getText());
                this.imageScaleV = Double.NaN;
                Node vScaleNode = nd.selectSingleNode("@vertical");
                if (vScaleNode != null) {
                    try {
                        this.imageScaleV = Double.parseDouble(vScaleNode.getText());
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(this, e);
                    }
                }
            }

            // Tests if it is a batimetric image
            nd = doc.selectSingleNode("//max-height");
            if (nd == null)
                this.isBathymetric = false;
            else {
                this.isBathymetric = true;

                this.maxHeight = Double.parseDouble(nd.getText());
                nd = doc.selectSingleNode("//max-depth");
                if (nd == null)
                    this.isBathymetric = false;
                else
                    this.maxDepth = Double.parseDouble(nd.getText());
                nd = doc.selectSingleNode("//resolution");
                if (nd == null)
                    this.resolution = DEFAULT_RESOLUTION;
                else
                    this.resolution = Integer.parseInt(nd.getText());

                nd = doc.selectSingleNode("//bathymetryImage"); // oldName, new is href-altitude
                if (nd != null)
                    setBathymetricImageFileName(ConfigFetch.resolvePathWithParent(originalFilePath, nd.getText()));
                nd = doc.selectSingleNode("//href-altitude");
                if (nd != null)
                    setBathymetricImageFileName(ConfigFetch.resolvePathWithParent(originalFilePath, nd.getText()));
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
        isLoadOk = true;
        return true;
    }

    /**
     * @return Returns the imageFileName.
     */
    public String getImageFileName() {
        return imageFileName;
    }

    /**
     * @param imageFileName The imageFileName to set.
     */
    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
        this.image = ImageUtils.getImage(imageFileName);
        this.heightImage = ImageUtils.getImage(imageFileName);
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getHeightImage() {
        return heightImage;
    }

    public void setHeightImage(Image heightImage) {
        this.heightImage = heightImage;
    }

    /**
     * @return Returns the imageScale.
     */
    public double getImageScale() {
        return imageScale;
    }

    /**
     * Please note that setting this will set the scale horizontal
     * equal to the vertical.
     * 
     * @param imageScale The imageScale to set.
     */
    public void setImageScale(double imageScale) {
        this.imageScale = imageScale;
        setImageScaleV(Double.NaN);;
    }
    
    /**
     * This if different than {@link Double#NaN} will allow different 
     * vertical scale than horizontal.
     * 
     * @return the imageScaleV
     */
    public double getImageScaleV() {
        return imageScaleV;
    }
    
    /**
     * Sets, if different than {@link Double#NaN},  a different 
     * vertical scale than horizontal.
     * 
     * @param imageScaleV the imageScaleV to set
     */
    public void setImageScaleV(double imageScaleV) {
        this.imageScaleV = imageScaleV;
    }

    /**
     * @return Returns the isBatimetric.
     */
    public boolean isBathymetric() {
        return isBathymetric;
    }

    /**
     * @param isBatimetric The isBatimetric to set.
     */
    public void setBathymetric(boolean isBatimetric) {
        this.isBathymetric = isBatimetric;
    }

    /**
     * @return Returns the maxDepth.
     */
    public double getMaxDepth() {
        return maxDepth;
    }

    /**
     * @param maxDepth The maxDepth to set.
     */
    public void setMaxDepth(double maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * @return Returns the maxHeight.
     */
    public double getMaxHeight() {
        return maxHeight;
    }

    /**
     * @param maxHeight The maxHight to set.
     */
    public void setMaxHeight(double maxHeight) {
        this.maxHeight = maxHeight;
    }

    /**
     * @return Returns the resolution.
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * @param resolution The resolution to set.
     */
    public void setResolution(double resolution) {
        this.resolution = resolution;
    }

    /**
     * @return Returns the originalFilePath.
     */
    public String getOriginalFilePath() {
        return originalFilePath;
    }

    /**
     * @param originalFilePath The originalFilePath to set.
     */
    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
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
        // Element root = super.asElement(DEFAULT_ROOT_ELEMENT);
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        if ("".equals(originalFilePath)) {
            root.addElement("href").addText(getImageFileName());
            NeptusLog.pub().error(this + ": Original file path is empty!");
        }
        else
            root.addElement("href")
                    .addText(FileUtil.relativizeFilePathAsURI(getOriginalFilePath(), getImageFileName()));
        Element scaleElm = root.addElement("scale");
        scaleElm.addText(Double.toString(getImageScale()));
        if (!Double.isNaN(imageScaleV)) {
            scaleElm.addAttribute("vertical", "" + imageScaleV);
        }
            
        if (isBathymetric()) {
            root.addElement("max-height").addText(Double.toString(getMaxHeight()));
            root.addElement("max-depth").addText(Double.toString(getMaxDepth()));

            // Old Schema
            // if (getBathymetricImageFileName() != null)
            // root.addElement("bathymetryImage").addText(
            // FileUtil.relativizeFilePathAsURI(getOriginalFilePath(),
            // getBathymetricImageFileName()));
            // if (getResolution() != DEFAULT_RESOLUTION)
            // root.addElement("resolution").addText(
            // Integer.toString(getResolution()));

            // for some reason this was not in sync with the schema, I've trace back at least to 2007 and it was like
            // this and not like the previous
            if (getResolution() != DEFAULT_RESOLUTION)
                root.addElement("resolution").addText(Double.toString(getResolution()));
            if (getBathymetricImageFileName() != null)
                root.addElement("href-altitude").addText(
                        FileUtil.relativizeFilePathAsURI(getOriginalFilePath(), getBathymetricImageFileName()));
        }

        return document;
    }

    @Override
    /**
     * Verifies if this paralledpiped is intercepted by the given coordinates
     */
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {

        double diff[] = lt.getOffsetFrom(getCenterLocation());
        double width = image.getWidth(null) * imageScale;
        double length = image.getHeight(null) * imageScale;

        double maxDiffX = width / 2;
        double maxDiffY = length / 2;

        return Math.abs(diff[1]) <= maxDiffX && Math.abs(diff[0]) <= maxDiffY;
    }

    @Override
    public int getLayerPriority() {
        return -2;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        params.setCenter(getCenterLocation());
        params.setImageFileName(getImageFileName());
        params.setImageScale(getImageScale());
        params.setImageScaleV(getImageScaleV());
        params.setIsBathymetric(isBathymetric());
        params.setMaxHeight(getMaxHeight());
        params.setMaxDepth(getMaxDepth());
        params.setResolution(getResolution());
        params.setBathimFile(getBathymetricImageFileName());
        params.setTransparency(getTransparency());
        params.setRotationDegs(getYawDeg());
        return params;
    }

    public boolean paramsOK(JPanel paramsPanel) {
        return (params.getErrors() == null);
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {

        setCenterLocation(params.getCenter());
        setImageFileName(params.getImageFileName());
        setImageScale(params.getImageScale());
        setImageScaleV(params.getImageScaleV());
        setBathymetric(params.getIsBathymetric());
        setMaxHeight(params.getMaxHeight());
        setMaxDepth(params.getMaxDepth());
        setResolution(params.getResolution());
        setTransparency(params.getTransparency());
        setYawDeg(params.getRotationDegs());

        if (isBathymetric() && params.getBathimFile() != null)
            setBathymetricImageFileName(params.getBathimFile().getAbsolutePath());
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        if (getImage() == null) {
            NeptusLog.pub().debug(this + ": Tried to draw a null image: " + getImageFileName());
            return;
        }

        Point2D center = renderer.getScreenPosition(getCenterLocation());
        g.translate(center.getX(), center.getY());
        g.rotate(getYawRad() - renderer.getRotation());
        double scaleH = getImageScale() * renderer.getZoom();
        double scaleV = (Double.isNaN(getImageScaleV()) ? getImageScale() : getImageScaleV()) * renderer.getZoom();
        g.scale(scaleH, scaleV);
        if (transparency > 0 && transparency <= 100)
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (100 - transparency) / 1E2f));
        g.drawImage(getImage(), -getImage().getWidth(renderer) / 2, -getImage().getHeight(renderer) / 2, null);
    }

    public void grow(double ammount) {
        imageScale *= 1.01;
    }

    public void shrink(double ammount) {
        imageScale /= 1.01;
    }

    public void rotateLeft(double ammount) {
        setYawDeg(getYawDeg() - ammount);
    }

    public void rotateRight(double ammount) {
        setYawDeg(getYawDeg() + ammount);
    }

    public double[] getDimension() {
        return new double[] { getImage().getWidth(null) * imageScale, getImage().getHeight(null) * imageScale, 0 };
    }

    public void setDimension(double[] newDimension) {
        if (newDimension.length != 3) {
            NeptusLog.pub()
                    .error(new Exception("Tried to set the dimension with an invalid array (size="
                            + newDimension.length + ")"));
            return;
        }
        this.imageScale = newDimension[0] / (double) getImage().getWidth(null);
    }

    public String getBathymetricImageFileName() {
        return bathymetricImageFileName;
    }

    public void setBathymetricImageFileName(String bathymetricImageFileName) {
        this.bathymetricImageFileName = bathymetricImageFileName;
    }
    
    @Override
    public String getTypeAbbrev() {
        return "img";
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_IMAGE;
    }
}
