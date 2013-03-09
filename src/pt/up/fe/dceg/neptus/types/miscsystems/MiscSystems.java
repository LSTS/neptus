/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: pdias
 * 2005/10/09
 */
package pt.up.fe.dceg.neptus.types.miscsystems;

import javax.media.j3d.TransformGroup;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.miscsystems.config.OptionsConfiguration;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.mnstarfire.loaders3d.Inspector3DS;

/**
 * @author Paulo Dias
 *
 */
public class MiscSystems implements XmlOutputMethods, XmlInputMethods {
	
    public static enum MiscSystemTypeEnum {AcousticTransponder, Payload}
    
    private static final String DEFAULT_CONFIGURE_HANDLER = OptionsConfiguration.class.getPackage()
            .getName() + "." + OptionsConfiguration.class.getSimpleName();

	protected static String DEFAULT_ROOT_ELEMENT = "miscSystem";

	protected String rootElementName = DEFAULT_ROOT_ELEMENT;
	
    protected String id = "";
    protected String name = "";
    protected String model = "";
    
    protected float xSize = 0;
    protected float ySize = 0;
    protected float zSize = 0;
    protected String topImageHref = "";
    protected String sideImageHref = "";
    protected String model3dHref ="";

    protected Inspector3DS obj3Dloader;

    protected String documentationStr = "";
    protected String implementationClassStr = DEFAULT_CONFIGURE_HANDLER; 
    protected Element configurationXMLElement = DocumentHelper.createElement("configuration");
    protected OptionsConfiguration optionsConfiguration = null;
    
    private Document doc = null;
    private String originalFilePath = "";
    protected boolean isLoadOk = true;
    
    protected MiscSystemTypeEnum type = MiscSystemTypeEnum.AcousticTransponder;
    
    /**
	 * 
	 */
	public MiscSystems() {
        super();
	}
    
    public MiscSystems(String xml) {
        super();
        isLoadOk = load (xml);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
    
    /**
	 * @return the rootElementName
	 */
	public String getRootElementName() {
		return rootElementName;
	}
	
	/**
	 * @param rootElementName the rootElementName to set
	 */
	public void setRootElementName(String rootElementName) {
		this.rootElementName = rootElementName;
	}
    
    /**
	 * @return the originalFilePath
	 */
	public String getOriginalFilePath() {
		return originalFilePath;
	}
	
	/**
	 * @param originalFilePath the originalFilePath to set
	 */
	public void setOriginalFilePath(String originalFilePath) {
		this.originalFilePath = originalFilePath;
	}
    
	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the model.
	 */
	public String getModel() {
		return model;
	}

	/**
	 * @param model
	 *            The model to set.
	 */
	public void setModel(String model) {
		this.model = model;
	}

	/**
	 * @return Returns the xSize.
	 */
	public float getXSize() {
		return xSize;
	}

	/**
	 * @param size
	 *            The xSize to set.
	 */
	public void setXSize(float size) {
		xSize = size;
	}

	/**
	 * @return Returns the ySize.
	 */
	public float getYSize() {
		return ySize;
	}

	/**
	 * @param size
	 *            The ySize to set.
	 */
	public void setYSize(float size) {
		ySize = size;
	}

	/**
	 * @return Returns the zSize.
	 */
	public float getZSize() {
		return zSize;
	}

	/**
	 * @param size
	 *            The zSize to set.
	 */
	public void setZSize(float size) {
		zSize = size;
	}

	/**
	 * @return Returns the topImageHref.
	 */
	public String getTopImageHref() {
		return topImageHref;
	}

	/**
	 * @param topImageHref
	 *            The topImageHref to set.
	 */
	public void setTopImageHref(String topImageHref) {
		this.topImageHref = topImageHref;
	}

	/**
	 * @return Returns the sideImageHref.
	 */
	public String getSideImageHref() {
		return sideImageHref;
	}

	/**
	 * @param sideImageHref
	 *            The sideImageHref to set.
	 */
	public void setSideImageHref(String sideImageHref) {
		this.sideImageHref = sideImageHref;
	}

	/**
	 * @return Returns the model3dHref.
	 */
	public String getModel3DHref() {
		return model3dHref;
	}

	/**
	 * @param model3dHref
	 *            The model3dHref to set.
	 */
	public void setModel3DHref(String model3dHref) {
		this.model3dHref = model3dHref;
	}

    /**
     * @return
     */
    public TransformGroup getModel3D () {
        TransformGroup themodel;
        obj3Dloader = new Inspector3DS(ConfigFetch.resolvePath(this.getModel3DHref())); // constructor
        obj3Dloader.parseIt(); // process the file
        themodel  = obj3Dloader.getModel(); // get the resulting 3D
        return themodel; 
    } 

    
    /**
	 * @return the documentationStr
	 */
	public String getDocumentationStr() {
		return documentationStr;
	}
	
	/**
	 * @param documentationStr the documentationStr to set
	 */
	public void setDocumentationStr(String documentationStr) {
		this.documentationStr = documentationStr;
	}
    
    
	/**
	 * @return the implementationClassStr
	 */
	public String getImplementationClassStr() {
		return implementationClassStr;
	}
	
	/**
	 * @param implementationClassStr the implementationClassStr to set
	 */
	public void setImplementationClassStr(String implementationClassStr) {
		this.implementationClassStr = implementationClassStr;
		ClassLoader cl = this.getClass().getClassLoader();
        try {
            if (implementationClassStr.indexOf('.') == -1) {
                implementationClassStr = OptionsConfiguration.class.getPackage().getName() + "."
                        + implementationClassStr;
            }
            Class<?> clazz = cl.loadClass(implementationClassStr);
            optionsConfiguration = (OptionsConfiguration) clazz.newInstance();
        }
        catch (Exception e) {
			optionsConfiguration = new OptionsConfiguration();
		}
		optionsConfiguration.load(configurationXMLElement);
	}
	
	/**
	 * @return the optionsConfiguration
	 */
	public OptionsConfiguration getOptionsConfiguration() {
		return optionsConfiguration;
	}
	
	/**
	 * @return the configurationXMLElement
	 */
	public Element getConfigurationXMLElement() {
		return configurationXMLElement;
	}
	
	/**
	 * @param configurationXMLElement the configurationXMLElement to set
	 */
	public void setConfigurationXMLElement(Element configurationXMLElement) {
		if (!"configuration".equals(configurationXMLElement.getName())) 
			configurationXMLElement.setName("configuration");
		this.configurationXMLElement = configurationXMLElement;
	}
	
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(org.dom4j.Element)
     */
	public boolean load(Element elem) {
		doc = Dom4JUtil.elementToDocument(elem);
		if (doc == null) {
			isLoadOk = false;
			return false;
		}
		return load();
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#load(java.lang.String)
     */
	public boolean load(String xml) {
		try {
			doc = DocumentHelper.parseText(xml);
		} catch (DocumentException e) {
			e.printStackTrace();
			isLoadOk = false;
			return false;
		}
		return load();
	}
    
    /**
     * @return
     */
    private boolean load() {
        
        try {
        	this.setRootElementName(doc.getRootElement().getName());
        	
        	this.type = MiscSystemTypeEnum.AcousticTransponder;
        	if ("payload".equalsIgnoreCase(doc.selectSingleNode("/node()").getName()))
        	    this.type = MiscSystemTypeEnum.Payload;
        	
            this.setId(doc.selectSingleNode("/node()/properties/id").getText());
            this.setName(doc.selectSingleNode("/node()/properties/name").getText());
            Node node = doc.selectSingleNode("/node()/properties/model");
            if (node != null)
            	this.setModel(node.getText());
            else
            	this.model = "";
            
            node = doc.selectSingleNode("/node()/properties/appearence");
            if (node != null) {
                this.setXSize(new Float(
                        node.selectSingleNode("x-size").getText()).floatValue());
                this.setYSize(new Float(
                        node.selectSingleNode("y-size").getText()).floatValue());
                this.setZSize(new Float(
                        node.selectSingleNode("z-size").getText()).floatValue());
                
                Node node1 = node.selectSingleNode("top-image-2D");
                if (node1 != null) {
                    this.setTopImageHref(node1.getText());
	                this.setTopImageHref(ConfigFetch.resolvePathWithParent(originalFilePath,
	                		getTopImageHref()));
                }
                else {
                	this.setTopImageHref("");
                }

                node1 = node.selectSingleNode("side-image-2D");
                if (node1 != null) {
                    this.setSideImageHref(node1.getText());
                    this.setSideImageHref(ConfigFetch.resolvePathWithParent(originalFilePath,
                            getSideImageHref()));
                }
                else {
                	this.setSideImageHref("");
                }

                node1 = node.selectSingleNode("model-3D");
                if (node1 != null) {
	                this.setModel3DHref(node1.getText());
	                this.setModel3DHref(ConfigFetch.resolvePathWithParent(originalFilePath,
	                        getModel3DHref()));
                }
                else {
                	this.setModel3DHref("");
                }
            }
            
            node = doc.selectSingleNode("/node()/configuration");
            if (node == null) {
            	configurationXMLElement = DocumentHelper.createElement("configuration");
            }
            else
                configurationXMLElement = (Element) node;

            node = doc.selectSingleNode("/node()/annotation/documentation");
            if (node != null) {
            	setDocumentationStr(node.getText());
            } else {
            	setDocumentationStr("");
            }
            

            node = doc.selectSingleNode("/node()/annotation/implementation-class");
            if (node != null) {
            	setImplementationClassStr(node.getText());
            } else {
            	setImplementationClassStr(DEFAULT_CONFIGURE_HANDLER);
            }

            
//            this.setTranponderDelay(new Float(
//                    doc.selectSingleNode("/transponder/configuration/transponder-delay").getText()).floatValue());
//            this.setResponderLockout(new Float(
//                    doc.selectSingleNode("/transponder/configuration/responder-lockout").getText()).floatValue());
//            this.setInterrogationChannel(new Integer(
//                    doc.selectSingleNode("/transponder/configuration/interrogation-channel").getText()).intValue());
//            this.setReplyChannel(new Integer(
//                    doc.selectSingleNode("/transponder/configuration/reply-channel").getText()).intValue());

        } catch (Exception e)
        {
            NeptusLog.pub().error(this, e);
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlInputMethods#isLoadOk()
     */
	public boolean isLoadOk() {
		return isLoadOk;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
	 */
	public String asXML() {
		return asXML(rootElementName);
	}

    
    /* (non-Javadoc)
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
		//String rootElementName = DEFAULT_ROOT_ELEMENT;
		return asElement(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
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
		//String rootElementName = DEFAULT_ROOT_ELEMENT;
		return asDocument(rootElementName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
	 */
	public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement( rootElementName );
        
        //root.addComment(ConfigFetch.getSaveAsCommentForXML());
        
        Element properties = root.addElement( "properties" );
        properties.addElement( "id" ).addText(getId());
        properties.addElement( "name" ).addText(getName());
        
        if (model != null && !model.equalsIgnoreCase(""))
        	properties.addElement( "model" ).addText(getModel());
        
        if (getXSize() + getYSize() + getZSize() > 0) {
	        Element appearence = properties.addElement( "appearence" );
	        appearence.addElement( "x-size" ).addText(
	                Float.toString(getXSize()));
	        appearence.addElement( "y-size" ).addText(
	                Float.toString(getYSize()));
	        appearence.addElement( "z-size" ).addText(
	                Float.toString(getZSize()));

	        if (getTopImageHref() != null || !"".equalsIgnoreCase(getTopImageHref())) {
		        if (originalFilePath == "")
		            appearence.addElement("top-image-2D").addText(getTopImageHref());
		        else
		            appearence.addElement("top-image-2D").addText(
		                    FileUtil.relativizeFilePathAsURI(originalFilePath,
		                            getTopImageHref()));
	        }
	        if (getSideImageHref() != null || !"".equalsIgnoreCase(getSideImageHref())) {
		        if (originalFilePath == "")
		            appearence.addElement( "side-image-2D" ).addText(getSideImageHref());
		        else
		            appearence.addElement( "side-image-2D" ).addText(
		                    FileUtil.relativizeFilePathAsURI(originalFilePath,
		                            getSideImageHref()));
	        }
	        if (getModel3DHref() != null || !"".equalsIgnoreCase(getModel3DHref())) {
		        if (originalFilePath == "")
		            appearence.addElement( "model-3D" ).addText(getModel3DHref());
		        else
		            appearence.addElement( "model-3D" ).addText(
		                    FileUtil.relativizeFilePathAsURI(originalFilePath,
		                            getModel3DHref()));
	        }
        }
        
        Element annotation = root.addElement( "annotation" );
        if (getDocumentationStr() != null && "".equalsIgnoreCase(getDocumentationStr())) {
        	annotation.addElement("documentation").addText(getDocumentationStr());
        }
		if (getImplementationClassStr() != null
				&& "".equalsIgnoreCase(getImplementationClassStr())
				&& DEFAULT_CONFIGURE_HANDLER
						.equalsIgnoreCase(getImplementationClassStr())) {
			annotation.addElement("implemenattion-class").addText(getImplementationClassStr());
        }
        if (!annotation.hasContent()) {
        	root.remove(annotation);
        }
        
//        Element configuration = root.addElement( "configuration" );
//        configuration.addElement("transponder-delay").addText(
//                new Float(getTranponderDelay()).toString()).addAttribute(
//                "unit", "ms");
//        configuration.addElement("responder-lockout").addText(
//                new Float(getResponderLockout()).toString()).addAttribute(
//                "unit", "s");
//        configuration.addElement("interrogation-channel").addText(
//                new Integer(getInterrogationChannel()).toString());
//        configuration.addElement("reply-channel").addText(
//                new Integer(getReplyChannel()).toString());

        return document;
    }

	
	public static void main(String[] args) {
		System.out.println(MiscSystems.class.getPackage().getName());
	}
}
