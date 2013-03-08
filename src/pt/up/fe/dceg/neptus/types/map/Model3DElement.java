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
 * 20??/??/??
 * $Id:: Model3DElement.java 9845 2013-02-01 19:53:46Z pdias              $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;

import javax.swing.JFileChooser;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.ParametersSheetPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.beans.editor.FilePropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
/**
 * @author Ze Carlos
 * @author Paulo Dias
 * @author RJPG
 */
public class Model3DElement extends AbstractElement implements ScalableElement, RotatableElement, PropertiesProvider {

    protected static final String DEFAULT_ROOT_ELEMENT = "model3d";

    private static final Image DEFAULT_MODEL3D_MARKER = ImageUtils
            .getImage("images/buttons/model3d.png");

	String image2DFilename = null;
	double image2DScale = 1.0;
	boolean image2DFixedSize = false;
	
    boolean has2DImage = false;
    
	String model3DFilename = null;
	double model3DScale = 1.0;
	
    protected String originalFilePath = "";

    private Image image2D = null;
	
    /**
     * 
     */
    public Model3DElement() {
        super();
    }

    /**
     * @param xml
     */
    public Model3DElement(String xml) {
        // super(xml);
        load(xml);
    }

    public Model3DElement(String xml, String originalFilePath) {
        // super(xml);
        this.originalFilePath = originalFilePath;
        load(xml);
    }
    
    public Model3DElement(MapGroup mg, MapType map) {
        super(mg, map);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }

    
	@Override
	public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
		if (image2DFilename == null) {
			 double distance = getCenterLocation().getDistanceInMeters(point);
		        if ((distance * renderer.getZoom()) < 10)
		            return true;
		        else
		        	return false;
		}
		else {
	        double diff[] = centerLocation.getOffsetFrom(point);
	        double width = image2D.getWidth(null) * image2DScale;
	        double length = image2D.getHeight(null) * image2DScale;
	        
	        double maxDiffX = width / 2;
	        double maxDiffY = length / 2;
	        
	        return Math.abs(diff[1]) <= maxDiffX && Math.abs(diff[0]) <= maxDiffY;			
		}		
	}

	@Override
	public int getLayerPriority() {		
		return 0;
	}

	@Override
	public ParametersPanel getParametersPanel(boolean editable, MapType map) {		
		return new ParametersSheetPanel(getProperties());
	}

	@Override
	public String getType() {
		return "Model3D";
	}

    /**
     * @return Returns the has2DImage.
     */
    public boolean isHas2DImage() {
        return has2DImage;
    }

    /**
     * @param has2DImage The has2DImage to set.
     */
    public void setHas2DImage(boolean has2DImage) {
        this.has2DImage = has2DImage;
    }

    /**
     * @return Returns the image2D.
     */
    public Image getImage2D() {
        return image2D;
    }

    /**
     * @param image2D The image2D to set.
     */
    public void setImage2D(Image image2D) {
        this.image2D = image2D;
    }

    /**
     * @return Returns the image2DFilename.
     */
    public String getImage2DFilename() {
        return image2DFilename;
    }

    /**
     * @param image2DFilename The image2DFilename to set.
     */
    public void setImage2DFilename(String image2DFilename) {
        this.image2DFilename = image2DFilename;
    }

    /**
     * @return Returns the image2DFixedSize.
     */
    public boolean isImage2DFixedSize() {
        return image2DFixedSize;
    }

    /**
     * @param image2DFixedSize The image2DFixedSize to set.
     */
    public void setImage2DFixedSize(boolean image2DFixedSize) {
        this.image2DFixedSize = image2DFixedSize;
    }

    /**
     * @return Returns the image2DScale.
     */
    public double getImage2DScale() {
        return image2DScale;
    }

    /**
     * @param image2DScale The image2DScale to set.
     */
    public void setImage2DScale(double image2DScale) {
        this.image2DScale = image2DScale;
    }

    /**
     * @return Returns the model3DFilename.
     */
    public String getModel3DFilename() {
        return model3DFilename;
    }

    /**
     * @param model3DFilename The model3DFilename to set.
     */
    public void setModel3DFilename(String model3DFilename) {
        this.model3DFilename = model3DFilename;
    }

    /**
     * @return Returns the model3DScale.
     */
    public double getModel3DScale() {
        return model3DScale;
    }

    /**
     * @param model3DScale The model3DScale to set.
     */
    public void setModel3DScale(double model3DScale) {
        this.model3DScale = model3DScale;
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

    @Override
	public void initialize(ParametersPanel paramsPanel) {		
    	setProperties(((ParametersSheetPanel) paramsPanel).getProperties());
	}

	@Override
	public void paint(Graphics2D g2, StateRenderer2D renderer, double rotation) {
		Graphics2D g = (Graphics2D) g2.create();
//		g.scale(1, -1);
		
	    //FIXME
		if (image2DFilename == null) {
			image2D = null;
		}
		if (image2D == null && image2DFilename != null) {
			image2D = ImageUtils.getImage(image2DFilename);
		}
		
		if (image2D != null) {
			if (image2DFixedSize || image2DScale == 0) {
//				LocationType mapCenter = renderer.getCenter();
//		        double zoom = renderer.getZoom();
		            
//		        double tx = (getCenterLocation().getOffsetFrom(mapCenter)[1] * zoom);
//		        double ty = (getCenterLocation().getOffsetFrom(mapCenter)[0] * zoom);
		        Point2D tt = renderer.getScreenPosition(getCenterLocation());
		        double tx = tt.getX();// - renderer.getWidth() / 2;
		        double ty = tt.getY();// - renderer.getHeight() / 2;

		        g.translate(tx, ty);
		        
		        g.rotate(-rotation);
		        
		        g.drawImage(image2D, -image2D.getWidth(null)/2, -image2D.getHeight(null)/2, null);		        
			}
			else {
//				LocationType mapCenter = renderer.getCenter();
				double zoom = renderer.getZoom();

				AffineTransform oldTransform  = g.getTransform();
//				double offsets[] = (getCenterLocation().getOffsetFrom(mapCenter));
//				g.translate(offsets[1]*zoom, offsets[0]*zoom);
		        Point2D tt = renderer.getScreenPosition(getCenterLocation());
		        double tx = tt.getX();// - renderer.getWidth() / 2;
		        double ty = tt.getY();// - renderer.getHeight() / 2;
		        g.translate(tx, ty);

				g.rotate(-getYawRad());
				g.translate((-image2DScale*image2D.getWidth(null)/2) * zoom, (image2DScale*image2D.getHeight(null)/2) * zoom);
				g.scale(image2DScale*zoom,-image2DScale*zoom);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);        
				g.drawImage(image2D, 0, 0, null);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g.setTransform(oldTransform);
			}
		}
		else {
//			LocationType mapCenter = renderer.getCenter();
//	        double zoom = renderer.getZoom();
	            
//	        double tx = (getCenterLocation().getOffsetFrom(mapCenter)[1] * zoom);
//	        double ty = (getCenterLocation().getOffsetFrom(mapCenter)[0] * zoom);
            Point2D tt = renderer.getScreenPosition(getCenterLocation());
            double tx = tt.getX() - renderer.getWidth() / 2;
            double ty = -tt.getY() + renderer.getHeight() / 2;
	        
	        g.translate(tx, ty);
	        g.scale(1,-1);
	        g.rotate(-rotation);
	        
	        g.setColor(new Color(255,255,255,100));
            g.fillOval(-5, -5, 10, 10);
	        
	        if (!isSelected())
	            g.setColor(new Color(255,0,0,100));
	        else
	            g.setColor(Color.WHITE);
	        
	        g.drawOval(-5,-5,10,10);	        
            
	        g.setColor(Color.RED);
	        g.drawString("3D", -6, 6);
	        
	        g.setColor(Color.BLACK);
	        g.drawString(getName(), 6, 6);
	        
	        g.rotate(rotation);
	        g.scale(1,-1);
	        g.translate(-tx, -ty);
		}
		
		g.dispose();
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.map.AbstractElement#load(org.dom4j.Element)
     */
    @Override
    public boolean load(Element elem) {
        if (!super.load(elem))
            return false;

        try {
            // doc = DocumentHelper.parseText(xml);
            Node nd = doc.selectSingleNode("//href");
            if (nd != null) {
                if (originalFilePath == "")
                    this.model3DFilename = nd.getText();
                else
                    this.model3DFilename = ConfigFetch.resolvePathWithParent(originalFilePath, nd.getText());
            }
            nd = doc.selectSingleNode("//scale");
            if (nd == null)
                this.model3DScale = 1.0;
            else
                this.model3DScale = Double.parseDouble(nd.getText());

            // Tests if a 2D image is given
            nd = doc.selectSingleNode("//href-2d");
            if (nd != null && !(nd.equals(""))) {
                if (originalFilePath == "")
                    this.image2DFilename = nd.getText();
                else
                    this.image2DFilename = ConfigFetch.resolvePathWithParent(originalFilePath, nd.getText());

                this.image2D = ImageUtils.getImage(this.image2DFilename);
                setHas2DImage(true);
            }
            else {
                image2DFilename = null;
                image2D = DEFAULT_MODEL3D_MARKER;
                image2DFixedSize = true;
                setHas2DImage(false);
            }

            nd = doc.selectSingleNode("//scale-2d");
            if (nd == null)
                this.image2DScale = 1.0;
            else
                this.image2DScale = Double.parseDouble(nd.getText());

            nd = doc.selectSingleNode("//fixed-size");
            if (nd == null)
                this.image2DFixedSize = false;
            else
                this.image2DFixedSize = Boolean.parseBoolean(nd.getText());
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            isLoadOk = false;
            return false;
        }
        isLoadOk = true;
        return true;
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
        // Element root = super.asElement(DEFAULT_ROOT_ELEMENT);
        Element root = (Element) super.asDocument(DEFAULT_ROOT_ELEMENT).getRootElement().detach();
        document.add(root);

        if (originalFilePath == "") {
            root.addElement("href").addText(getModel3DFilename());
            NeptusLog.pub().error(this + ": Original file path is empty!");
        }
        else
            root.addElement("href").addText(
                    FileUtil.relativizeFilePathAsURI(getOriginalFilePath(), getModel3DFilename()));
        root.addElement("scale").addText(Double.toString(getModel3DScale()));

        if (isHas2DImage()) {
            if (originalFilePath == "") {
                if (getImage2DFilename() != null)
                    root.addElement("href-2d").addText(getImage2DFilename());
                else
                    root.addElement("href-2d").addText("");
                NeptusLog.pub().error(this + ": Original file path is empty!");
            }
            else
                root.addElement("href-2d").addText(
                        FileUtil.relativizeFilePathAsURI(getOriginalFilePath(), getImage2DFilename()));

            root.addElement("scale-2d").addText(Double.toString(getImage2DScale()));
            root.addElement("fixed-size").addText(Boolean.toString((isImage2DFixedSize())));
        }

        return document;
    }

    public DefaultProperty[] getProperties() {    	
    	
    	DefaultProperty center = PropertiesEditor.getPropertyInstance("Position", "Map", LocationType.class, getCenterLocation(), true);
    	
    	DefaultProperty model3d;
    	if (getModel3DFilename() != null)
    		model3d = PropertiesEditor.getPropertyInstance("3D Model file", "3D", File.class, new File(getModel3DFilename()), true);
    	else
    		model3d = PropertiesEditor.getPropertyInstance("3D Model file", "3D", File.class, null, true);
    	
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(model3d, new FilePropertyEditor() {
    		@Override
    		protected void customizeFileChooser(JFileChooser jfc) {
    		    File fx;
    	        if (getModel3DFilename() != null && new File(getModel3DFilename()).exists()) {
    	            fx = new File(getModel3DFilename());
    	        } 
    	        else if (getImage2DFilename() != null && new File(getImage2DFilename()).exists()) {
                    fx = new File(getImage2DFilename());
                } 
    	        else {
    	            fx = new File(ConfigFetch.getConfigFile());
    	            if (!fx.exists()) {
    	                fx = new File(ConfigFetch.resolvePath("."));
    	                if (!fx.exists()) {
    	                    fx = new File(".");
    	                }
    	            }
    	        }
    	        jfc.setCurrentDirectory(fx);
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("3DS Models", new String[]{"3ds"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("X3D Models", new String[]{"x3d"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("WRL Models", new String[]{"wrl"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("All Models", new String[]{"3ds", "x3d", "wrl"}));
    			jfc.setAcceptAllFileFilterUsed(false);
    			jfc.setFileView(new NeptusFileView());
    		}
    	});
    		
    	DefaultProperty scale = PropertiesEditor.getPropertyInstance("3D Model Scale", "3D", Double.class, model3DScale, true);    	
    	
    	DefaultProperty image2d;
    	if (getImage2DFilename() != null)	
        	image2d = PropertiesEditor.getPropertyInstance("2D Image file", "2D", File.class, new File(getImage2DFilename()), true);
        else
        	image2d = PropertiesEditor.getPropertyInstance("2D Image file", "2D", File.class, null, true);
    	
    	PropertiesEditor.getPropertyEditorRegistry().registerEditor(image2d, new FilePropertyEditor() {
    		@Override
    		protected void customizeFileChooser(JFileChooser jfc) {
    			//JFileChooser chooser = new JFileChooser(new File(ConfigFetch.getConfigFile()));
                File fx;
                if (getImage2DFilename() != null && new File(getImage2DFilename()).exists()) {
                    fx = new File(getImage2DFilename());
                }
                else if (getModel3DFilename() != null && new File(getModel3DFilename()).exists()) {
                    fx = new File(getModel3DFilename());
                } 
                else {
                    fx = new File(ConfigFetch.getConfigFile());
                    if (!fx.exists()) {
                        fx = new File(ConfigFetch.resolvePath("."));
                        if (!fx.exists()) {
                            fx = new File(".");
                        }
                    }
                }
                jfc.setCurrentDirectory(fx);
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("PNG Images", new String[]{"png"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("JPG Images", new String[]{"jpg"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("GIF Images", new String[]{"gif"}));
    			jfc.addChoosableFileFilter(GuiUtils.getCustomFileFilter("All Images", new String[]{"png", "jpg", "gif"}));
    			jfc.setAcceptAllFileFilterUsed(false);   
    			jfc.setFileView(new NeptusFileView());
    		}
    	});
    	
    	DefaultProperty scale2D = PropertiesEditor.getPropertyInstance("2D Image scale", "2D", Double.class, image2DScale, true);    	
    	
    	DefaultProperty roll = PropertiesEditor.getPropertyInstance("Roll", "Attitude", Double.class, getRollDeg(), true);
    	DefaultProperty pitch = PropertiesEditor.getPropertyInstance("Pitch", "Attitude", Double.class, getPitchDeg(), true);
    	DefaultProperty yaw = PropertiesEditor.getPropertyInstance("Yaw", "Attitude", Double.class, getYawDeg(), true);

    	return new DefaultProperty[] {
    			center, model3d, scale, image2d, scale2D, roll, pitch, yaw
    	};
    }
    
    public String getPropertiesDialogTitle() {    	
    	return "3D Model Properties";
    }
    
    public String[] getPropertiesErrors(Property[] properties) {    	
    	return null;
    }
    
    public void setProperties(Property[] properties) {
    	
    	for (Property p : properties) {
    		
    		if (p.getName().equals("Position")) {
    			setCenterLocation((LocationType)p.getValue());
    		}
    		if (p.getName().equals("3D Model file")) {
    			if (p.getValue() != null)
    				setModel3DFilename(((File)p.getValue()).getAbsolutePath());
    			else
    				setModel3DFilename(new File("models/transp.3sd").getAbsolutePath());
    		}
    		if (p.getName().equals("2D Image file")) {
    			if (p.getValue() != null) {
    				setImage2DFilename(((File)p.getValue()).getAbsolutePath());
    				setHas2DImage(true);
    			}
    			else {
    				setImage2DFilename(null);
					setHas2DImage(true);
    			}
    		}
    		
    		if (p.getName().equals("2D Image scale")) {
    			this.image2DScale = (Double)p.getValue();
    		}
    		
    		if (p.getName().equals("3D Model Scale")) {
    			this.model3DScale = (Double)p.getValue();
    		}    		
    		if (p.getName().equals("Roll")) {
    			setRollDeg((Double)p.getValue());
    		}
    		if (p.getName().equals("Pitch")) {
    			setPitchDeg((Double)p.getValue());
    		}
    		if (p.getName().equals("Yaw")) {
    			setYawDeg((Double)p.getValue());
    		}    		
    	}
    }
    
    @Override
    public double getYaw() {
    	return super.getYaw();
    }
    
    public void rotateLeft(double ammount) {
    	setYawDeg(getYawDeg()-ammount);
    }
    
    public void rotateRight(double ammount) {
    	setYawDeg(getYawDeg()+ammount);
    }
    
    public double[] getDimension() {
    	return new double[] {model3DScale, model3DScale, model3DScale};
    }
    
    public void grow(double ammount) {
    	model3DScale = model3DScale * 1.1;
    	image2DScale = image2DScale * 1.1;
    }
    
    public void setDimension(double[] newDimension) {
    	model3DScale = newDimension[0];
    	image2DScale = newDimension[0];
    }
    
    public void shrink(double ammount) {
    	model3DScale = model3DScale * (1.0/1.1);
    	image2DScale = image2DScale * (1.0/1.1);
    	
    	model3DScale = Math.max(0.001, model3DScale);
    	image2DScale = Math.max(0.001, image2DScale);
    }
    
    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.MODEL_3D;
    }
}
