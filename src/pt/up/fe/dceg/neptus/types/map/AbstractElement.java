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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.XmlInputMethods;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.NameNormalizer;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * This is the superclass of all objects that can be placed on a map
 * Refactored in 17/11/2006.
 * @author Paulo Dias
 * @author Zé Carlos
 */
public abstract class AbstractElement 
implements ActionListener, Comparable<AbstractElement>, XmlOutputMethods, XmlInputMethods 
{
	public enum ELEMENT_TYPE { MODEL_3D, TYPE_HOMEREFERENCE, TYPE_MARK, TYPE_PARALLELEPIPED, TYPE_ELLIPSOID, TYPE_TRANSPONDER, TYPE_PATH, TYPE_CYLINDER, TYPE_IMAGE, TYPE_OTHER };

	private static Random rnd = new Random(System.currentTimeMillis());
    protected static final LocationType guine = LocationType.ABSOLUTE_ZERO;
    protected boolean isLoadOk = true;
    protected static final String DEFAULT_ROOT_ELEMENT = "mark";
    
    private double phi   = 0;
    private double theta = 0;
    private double psi   = 0;
    
    protected int transparency = 0;

    protected LocationType centerLocation = new LocationType(); 
   
    protected Document doc = null;
    
    protected String name = "obj_"+System.currentTimeMillis()+rnd.nextInt(100);
    protected String id = name;

    // ===== Old MapObject
    protected boolean selected = false;
    private MapGroup mapGroup = null;
    private MapType parentMap = null;
    private MissionType missionType = null;
    // ===== END Old MapObject

    // ===== Param panels
    protected ParametersPanel paramsPanel;
    public boolean userCancel = false, copyChars = true;
    public String[] takenNames = new String[0];
    protected JDialog dialog;
    protected JTextField objID, objName, transp;
    // ===== END Param panels

    // ===== Abstract functions
    
    public abstract void paint(Graphics2D g, StateRenderer2D renderer, double rotation);
    public abstract ParametersPanel getParametersPanel(boolean editable, MapType map);
    public abstract void initialize(ParametersPanel paramsPanel);
    public abstract boolean containsPoint(LocationType point, StateRenderer2D renderer);
    public abstract ELEMENT_TYPE getElementType();
    
    /**
     * Returns the showing priority for this object (0 normal, <0 background, >0 on top)
     * @return The layer priority for this object
     */
    public abstract int getLayerPriority();

	/**
     * Creates a map element
     */
    public AbstractElement() {
	    
    }

    /**
     * @param xml
     */
    public AbstractElement(String xml) {
        load(xml);
    }

    public AbstractElement(MapGroup mg, MapType parentMap) {
        this.mapGroup = mg;
        this.parentMap = parentMap;     
    }

    public boolean isLoadOk() {
        return isLoadOk;
    }

    /**
     * @param xml
     * @return
     */
    public boolean load(String xml) {
        try {
            doc = DocumentHelper.parseText(xml);
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return load(doc.getRootElement());
    }

    /**
     * @param elem
     * @return
     */
    public boolean load(Element elem) {
        doc = Dom4JUtil.elementToDocument(elem);
        if (doc == null) {
            isLoadOk = false;
            return false;
        }
        isLoadOk = !getCenterLocation().load(doc);
        if (isLoadOk())
            return false;

        setId(getCenterLocation().getId());
        setName(getCenterLocation().getName());

        Node nd;
        try {
            nd = doc.selectSingleNode("//attitude");
            if (nd != null) {
                Node ndTemp;
                ndTemp = nd.selectSingleNode("phi");
                if (ndTemp != null) {
                    String text = ndTemp.getText();
                    double val;
                    try {
                        val = Double.parseDouble(text);
                    }
                    catch (NumberFormatException e) {
                        NeptusLog.pub().error(e.getMessage());
                        val = 0;
                    }
                    setPhi(val);
                }
                ndTemp = nd.selectSingleNode("theta");
                if (ndTemp != null) {
                    String text = ndTemp.getText();
                    double val;
                    try {
                        val = Double.parseDouble(text);
                    }
                    catch (NumberFormatException e) {
                        NeptusLog.pub().error(e.getMessage());
                        val = 0;
                    }
                    setTheta(val);
                }
                ndTemp = nd.selectSingleNode("psi");
                if (ndTemp != null) {
                    String text = ndTemp.getText();
                    double val; 
                    try {
                        val = Double.parseDouble(text);
                    }
                    catch (NumberFormatException e) {
                        NeptusLog.pub().error(e.getMessage());
                        val = 0;
                    }
                    setPsi(val);
                }
            }
            nd = doc.selectSingleNode("//transparency");
            if (nd != null) {
                String text = nd.getText();
                double val; 
                try {
                    val = Double.parseDouble(text);
                }
                catch (NumberFormatException e) {
                    NeptusLog.pub().error(e.getMessage());
                    val = 0;
                }
                if (val < 0)
                    val = 0;
                if (val > 100)
                    val = 100;
                
                setTransparency((int)val);
            }            
        }
        catch (Exception e) {
            NeptusLog.pub().error(this + ":XML not recognized!!!");
            isLoadOk = false;
            return false;
        }
        isLoadOk =true;
        return true;
    }
    // ===== XMLOutput Interface

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
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

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement()
     */
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asElement(java.lang.String)
     */
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument()
     */
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = DocumentHelper.createDocument();
        //Element root = document.addElement( rootElementName );
        
        //root.addElement( "id" ).addText(getId());
        //root.addElement( "name" ).addText(getName());
        
        //Se não estiver assim entra em ciclo infinito
     /*   LocationType lt = new LocationType();
        lt.setLocation(getCentralLocation());
        lt.setId(this.getId());
        lt.setName(this.getName());*/
        
        getCenterLocation().setId(getId());
        getCenterLocation().setName(getName());
        
        Element root = getCenterLocation().asElement(rootElementName);
        
        //TODO Testar (acho q está testado)
        if ( (phi != 0) || (theta != 0) || (psi != 0) ) {
            Element att = root.addElement("attitude");
            if (phi != 0)
                att.addElement("phi").addText(Double.toString(phi));
            if (theta != 0)
                att.addElement("theta").addText(Double.toString(theta));
            if (psi != 0)
                att.addElement("psi").addText(Double.toString(psi));
            
            //root.add(att);
        }
        if (transparency != 0)
            root.addElement("transparency").addText(Double.toString(transparency));
        
        document.add(root);

        return document;
    }
    // ===== END XMLOutput Interface

    /**
     * @return
     */
    public abstract String getType();

    /**
     * @return
     */
    public int getTransparency() {
        return transparency;
    }

    /**
     * @param transparency
     */
    public void setTransparency(int transparency) {
        if (transparency < 0)
            this.transparency = 0;
        else if (transparency > 100)
            this.transparency = 100;
        else
            this.transparency = transparency;
    }

    // ===== Attitude Setters and Getters
    /**
     * @return Returns the phi.
     */
    public double getPhi() {
        return phi;
    }

    /**
     * @param phi The phi to set.
     */
    public void setPhi(double phi) {
        this.phi = phi;
    }

    /**
     * @return Returns the phi.
     */
    public double getRoll() {
        return phi;
    }

    /**
     * @param phi The phi to set.
     */
    public void setRoll(double phi) {
        this.phi = phi;
    }

    /**
     * @return Returns the theta.
     */
    public double getTheta() {
        return theta;
    }

    /**
     * @param theta The theta to set.
     */
    public void setTheta(double theta) {
        this.theta = theta;
    }

    /**
     * @return Returns the theta.
     */
    public double getPitch() {
        return theta;
    }

    /**
     * @param theta The theta to set.
     */
    public void setPitch(double theta) {
        this.theta = theta;
    }

    /**
     * @return Returns the psi.
     */
    public double getPsi() {
        return psi;
    }

    /**
     * @param psi The psi to set.
     */
    public void setPsi(double psi) {
        this.psi = psi;
    }

    /**
     * @return Returns the psi.
     */
    public double getYaw() {
        return psi;
    }

    /**
     * @param psi The psi to set.
     */
    public void setYaw(double psi) {
        this.psi = psi;
    }
    
    public void setRollDeg(double roll) {
        this.phi = roll;
    }
    
    public void setPitchDeg(double pitch) {
        this.theta = pitch;
    }

    public void setYawDeg(double yaw) {
        this.psi = yaw;
    }
    

    /**
     * @return the current roll rotation in RADIANS!
     */
    
    public double getRollRad() {
        return Math.toRadians(phi);
    }
    
    /**
     * @return the current pitch rotation in RADIANS!
     */
    public double getPitchRad() {
        return Math.toRadians(theta);
    }
    
    /**
     * @return the current yaw rotation in RADIANS!
     */
    public double getYawRad() {
        return Math.toRadians(psi);
    }

    
    /**
     * @return
     */
    public double getRollDeg() {
        return phi % 360;
    }
    
    /**
     * @return
     */
    public double getPitchDeg() {
        return theta % 360;
    }
    
    /**
     * @return
     */
    public double getYawDeg() {
        return psi % 360;
    }
    // ===== END Attitude Setters and Getters

    public double getTopHeight() {
        return getCenterLocation().getHeight();
    }

    // ===== Parentwood
    /**
     * @return
     */
    public MapGroup getMapGroup() {
        if (mapGroup == null) {
          //  NeptusLog.pub().warn(this+" MapGroup for this "+getClass().getSimpleName()+" is null!!");
            mapGroup = MapGroup.getNewInstance(null);
        }
        return mapGroup;
    }
    
    /**
     * @param mapGroup
     */
    public void setMapGroup(MapGroup mapGroup) {
        this.mapGroup = mapGroup;
    }
    
    /**
     * @param parentMap
     */
    public void setParentMap(MapType parentMap) {
        this.parentMap = parentMap;
    }
    
    /**
     * @return
     */
    public MapType getParentMap() {
        if (parentMap == null) {
            //NeptusLog.pub().warn("Object "+this.getId()+" of type "+this.getType()+" has no parent map!!");
            parentMap = new MapType();
        }
            return parentMap;
    }

    /**
     * @return
     */
    public MissionType getMissionType() {
        if (missionType == null) {
            NeptusLog.pub().debug("[MapObject] getMissionType() created a new mission instance!");
            missionType = new MissionType();
        }
        return missionType;
    }
    /**
     * @param missionType
     */
    public void setMissionType(MissionType missionType) {
        this.missionType = missionType;
    }
    // ===== END Parentwood
    
    
    // ===== Comparable Interface

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(AbstractElement anotherMapObject) {
//        if (anotherMapObject instanceof AbstractElement) {
        	AbstractElement tmp = (AbstractElement) anotherMapObject;
        	int diff = getLayerPriority() - tmp.getLayerPriority();
        	
        	if (diff != 0)
        		return diff;
        	
        	if (tmp.getTopHeight() > getTopHeight()) {
        		return -1;        		
        	}
        	
        	if (tmp.getTopHeight() < getTopHeight()) {
        		return 1;        		
        	}
        	
        	if (this instanceof ImageElement && anotherMapObject instanceof ImageElement) {
        		if (((ImageElement)this).getImageScale() > ((ImageElement)anotherMapObject).getImageScale())
        			return -1;	
        		return 1;
        	}

            
//        }
        return 0;
    }
    
    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Comparator getIDComparator() {
        Comparator cmp = new Comparator() {
            public int compare(Object o1, Object o2) {
                if (! (o1 instanceof AbstractElement) || ! (o2 instanceof AbstractElement))
                    return 0;
                return ((AbstractElement)o1).getId().compareTo(((AbstractElement)o2).getId()); 
            };
        };
        return cmp;
    }   
    // ===== END Comparable Interface



    public LocationType getCenterLocation() {
		return centerLocation;
	}

	public void setCenterLocation(LocationType centralLocation) {
			this.centerLocation = centralLocation;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    /**
     * Returns the result of getName() - the default implementation returns the
     * field <b>name</b>
     */
    public String toString() {
        return getName();
    }

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}	
    
    
    // ===== Param stuff
    /**
     * The actions are triggered by the dialog created 
     * for the object parameters
     */
    public void actionPerformed(ActionEvent action) {
        
        if ("add".equals(action.getActionCommand())) {
            
            if (!NameNormalizer.isNeptusValidIdentifier(objID.getText())) {
                JOptionPane.showMessageDialog(paramsPanel, I18n.text("The object identifier is not valid"));
                return;
            }
            
            try {
            	double val = Double.parseDouble(transp.getText());
            	if (val < 0)
            		val = 0;
            	if (val > 100)
            		val = 100;
            	setTransparency((int)val);
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().debug(e.getMessage());
            	JOptionPane.showMessageDialog(paramsPanel, I18n.text("The object transparency is not valid"));
            	return;
			}
            
            if (takenNames != null) {
                for (int i = 0; i < takenNames.length; i++) {
                    if (((String)takenNames[i]).equals(objID.getText())) {
                        JOptionPane.showMessageDialog(paramsPanel, I18n.text("The entered identifier is already in use"));
                        return;
                    }
                }
            }
        
            if (objName.getText().length() == 0) {
                JOptionPane.showMessageDialog(paramsPanel, I18n.text("The object has to have a name"));
                return;
            }
                        
            if (paramsPanel.getErrors() != null) {
                JOptionPane.showMessageDialog(paramsPanel, paramsPanel.getErrors());
                return;
            }
            
            setName(objName.getText());
            setId(objID.getText());
            
            initialize(paramsPanel);
            
            dialog.setVisible(false);
            dialog.dispose();
            return;
            
        }
        
        if ("cancel".equals(action.getActionCommand())) {
            dialog.setVisible(false);
            dialog.dispose();
            this.userCancel = true;
            return;
        }
    }
    
    /**
     * Creates a shows a parameters dialog for the current object
     * The user can chage the parameters of the current object here.
     * @param takenNames
     */
    public void showParametersDialog(Component parentComp, String[] takenNames, MapType map, boolean editable) {
        
        this.takenNames = takenNames;
        this.parentMap = map;
        
        paramsPanel = getParametersPanel(editable,map);
        
        if (parentComp == null || SwingUtilities.getWindowAncestor(parentComp) == null) {
            dialog = new JDialog((Frame)ConfigFetch.getSuperParentFrame());
        }
        else {
        	dialog = new JDialog(SwingUtilities.getWindowAncestor(parentComp));
        }
        JPanel idPanel = new JPanel();
        FlowLayout flow = new FlowLayout();
        flow.setAlignment(FlowLayout.LEFT);
        idPanel.setLayout(flow);
 
        objName = new JTextField(8);
        objName.setEditable(editable);
        objName.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                copyChars = false;
            }
        });
        
        objID = new JTextField(8);
        objID.setEditable(editable);
        
        if (getParentMap() != null) {
            int i = 1;
            while(getParentMap().getObject(getClass().getSimpleName()+i) != null)
                i++;
            objID.setText(getType()+i);
            objName.setText(getType()+i);
        }
        
        transp = new JTextField(3);
        transp.setEditable(editable);
        transp.setText(""+getTransparency());
        
        idPanel.add(new JLabel(I18n.text("Object ID:")));
        idPanel.add(objID);
        
        idPanel.add(new JLabel(I18n.text("Object name:")));
        idPanel.add(objName);
        
        idPanel.add(new JLabel(I18n.text("Transparency:")));
        idPanel.add(transp);
        
        if (takenNames == null) {
            objName.setEnabled(false);
            objName.setText(this.getName());
            objID.setEnabled(false);
            objID.setText(this.getId());
            copyChars = false;
        }
        
        objID.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if (copyChars) {
                    if (Character.isLetterOrDigit(e.getKeyChar()))
                        objName.setText(objID.getText()+e.getKeyChar());
                    else
                        objName.setText(objID.getText());
                }
            }
        });
        
        JPanel buttonsPanel = new JPanel();
        FlowLayout layout = new FlowLayout();
        layout.setAlignment(FlowLayout.RIGHT);        
        buttonsPanel.setLayout(layout);

        JButton add = new JButton(I18n.text("OK"));
        add.setActionCommand("add");
        add.addActionListener(this);
        add.setPreferredSize(new Dimension(100,25));
        buttonsPanel.add(add);
       
        JButton cancel = new JButton(I18n.text("Cancel"));
        cancel.setActionCommand("cancel");
        cancel.addActionListener(this);
        cancel.setPreferredSize(new Dimension(100,25));
        buttonsPanel.add(cancel);
        
        GuiUtils.reactEnterKeyPress(add);
		GuiUtils.reactEscapeKeyPress(cancel);
		
        dialog.setLayout(new BorderLayout());
        dialog.setTitle(I18n.textf("Parameters for %mapElementType", getType()));
        //dialog.setModal(true);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);

        dialog.getContentPane().add(idPanel, BorderLayout.NORTH);
        dialog.getContentPane().add(paramsPanel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        //dialog.setSize(paramsPanel.getPreferredSize());
        Dimension pSize = paramsPanel.getPreferredSize();
        dialog.setSize(Math.max(pSize.width, 480), pSize.height + 12*2);
        GuiUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                userCancel = true;
                dialog.setVisible(false);
                dialog.dispose();
            }
        });
    }
    
    /**
     * @return
     */
    public boolean isUserCancel() {
        return userCancel;
    }

    /**
     * Sets the currently taken object names (ids)
     * @param takenNames An array of String with the all the existing object names (ids)
     */
    public void setTakenNames(String[] takenNames) {
        this.takenNames = takenNames;
    }

    // ===== END Param stuff
    
    /**
     * Given a java.util.Color, returns its complementar color (negative)
     * @param original A java.util.Color
     * @return A color calculated by inverting all the color components (RGB)
     */
    public Color invertColor(Color original) {
        Color inverted = new Color(255 - original.getRed(), 255 - original.getGreen(), 255 - original.getBlue());
        return inverted;
    }
    
    public Vector<LocationType> getShapePoints() {
    	return null;
    }

    /**
     * Returns the offset from absolute 0 as double[3]:
     * <p>
     * <blockquote>
     * <li> 0 - The offset (in the North direction) from absolut (0,0,0)
     * <li> 1 - The offset (in the East direction) from absolut (0,0,0)
     * <li> 2 - The offset (in the Down direction) from absolut (0,0,0)
     * </blockquote>
     * <p>
     * @return A 3 component absolute offset
     */
    public double[] getNEDPosition() {
        NeptusLog.pub().debug(getCenterLocation().getDebugString());
        return getCenterLocation().getOffsetFrom(guine);
    }
    
    public AbstractElement getClone() throws Exception {
        String xml = asXML();
        AbstractElement clone = getClass().newInstance();
        clone.load(xml);        
        return clone;
    }
}