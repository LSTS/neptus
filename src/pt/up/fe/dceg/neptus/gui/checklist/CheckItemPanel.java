/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * Author: Paulo Dias
 * Created in 26/Jun/2005
 */
package pt.up.fe.dceg.neptus.gui.checklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoSubItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoUserActionItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoUserLogItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckItem;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class CheckItemPanel 
extends JPanel
implements PropertyChangeListener
{
    private static final long serialVersionUID = 1L;

    public static Icon OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/selectedIcon.png", 18, 18));
    public static Icon NOT_OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/boxIcon.png", 18, 18));
    private static final Icon EDIT_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/edit.png", 16, 16)); 
	//private static final Icon SUB_ITEMS_IMAGE_ICON = new ImageIcon(CheckItemPanel.class.getResource("/images/menus/wizard.png"));

	//private static final String CHANGED_PROPERTY = ChecklistPanel.CHANGED_PROPERTY;
    
	public static final Color CHECK_COLOR = new Color(190, 220, 240); // blue

	private JPanel infoPanel = null;
	private JCheckBox checkBox = null;
	private JLabel nameCheckItem = null;
	private JPanel notesPanel = null;
	
	private JScrollPane noteScrollPane = null;
	private JTextArea noteTextArea = null;
	private JToggleButton noteToggleButton = null;
	
	private JPanel buttonsPanel = null;
	private JLabel statesLabel = null;
	private JPanel actionsPanel = null;
	private AutoItemsList actionsListPanelHolder = null;
	
    private CheckItem checkItem = null;
    private JLabel dateChangedLabel = null;
    private JPanel datePanel = null;
    private JLabel dateLabLabel = null;
    
    private Date trialTime = new Date();
    
    private Color baseBackgroundColor = null;
    
    /**
     * MMddHHmm[[[CC]yy][.ss]]
     * E.g.: 2002-12-17T09:30:47.0Z
     */
    public static final SimpleDateFormat dateXMLFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.0Z'");

    
	/**
	 * This is the default constructor
	 */
	public CheckItemPanel() {
		super();
		initialize();
	}

    public CheckItemPanel(CheckItem ci) {
        super();
        this.checkItem = ci;
        initialize();
        getCheckBox().setSelected(checkItem.isChecked());
        nameCheckItem.setText(checkItem.getName());
        getNoteTextArea().setText(checkItem.getNote());
        dateChangedLabel.setText(checkItem.getDateChecked());
        
        for(CheckAutoSubItem casi : checkItem.getAutoSubItems()) {
        	if(casi.getSubItemType().equals(VariableIntervalItem.TYPE_ID))
        		this.getActionsListPanelHolder().add(new VariableIntervalItem(this.getActionsListPanelHolder(),(CheckAutoVarIntervalItem) casi));
        	if(casi.getSubItemType().equals(UserActionItem.TYPE_ID))
    			this.getActionsListPanelHolder().add(new UserActionItem(this.getActionsListPanelHolder(),(CheckAutoUserActionItem) casi));
        	if(casi.getSubItemType().equals(UserCommentItem.TYPE_ID))
    			this.getActionsListPanelHolder().add(new UserCommentItem(this.getActionsListPanelHolder(),(CheckAutoUserLogItem) casi));
        }
        fixStateLabel();
    }

    
    
    /**
     * @return Returns the isChecked.
     */
    public boolean isChecked() {
        //FIXME pdias test for subitems values
        return getCheckBox().isSelected();
    }

    /**
     * @param isChecked The isChecked to set.
     */
    public void setChecked(boolean isChecked) {
        //FIXME pdias test for subitems values
    	getCheckBox().setSelected(isChecked);
        getCheckBox().revalidate();
        getCheckBox().repaint();
    }

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private  void initialize() {
		baseBackgroundColor = getBackground();
		this.setInheritsPopupMenu(true);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		//this.setSize(360, 200);
		this.setBorder(BorderFactory.createLineBorder(Color.gray,1));
		this.add(getInfoPanel(), null);
		this.add(getNotesPanel(), null);
		this.add(getActionsPanel(), null);
        
        this.addPropertyChangeListener(ChecklistPanel.DIRTY_PROPERTY, this);
        /*
        this.addMouseListener(new java.awt.event.MouseAdapter() { 
            public void mouseClicked(java.awt.event.MouseEvent e) {
                System.out.println("mouseClickedItemCI()" + e.isControlDown());
                System.out.println("Source" + ((Component)e.getSource()).getParent().getParent().getParent().getParent().getClass());
                MyJTaskPaneGroup mjtpg = getMyJTaskPaneGroupFromItem((Component) e
                            .getSource()); 
                }});
        */
        fixStateLabel();
	}
    
    /**
     * @param itemPanel
     * @return
     */
    public static MyJTaskPaneGroup getMyJTaskPaneGroupFromItem(Component itemPanel) {
        try {
            MyJTaskPaneGroup mjtpg = (MyJTaskPaneGroup) itemPanel.getParent()
					.getParent().getParent().getParent();
            return mjtpg;
        }
        catch (RuntimeException e) {            
            //e.printStackTrace();
            return null;
        }
    }
    
	/**
	 * This method initializes jPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getInfoPanel() {
		if (infoPanel == null) {
			nameCheckItem = new JLabel();
			infoPanel = new JPanel();
			infoPanel.setLayout(new BorderLayout());
            infoPanel.setOpaque(false);
            infoPanel.setInheritsPopupMenu(true);
			nameCheckItem.setText("JLabel");
			nameCheckItem.setPreferredSize(new Dimension(300,16));
			infoPanel.add(getCheckBox(), BorderLayout.WEST);
			infoPanel.add(nameCheckItem, BorderLayout.CENTER);
			infoPanel.add(getButtonsPanel(), BorderLayout.EAST);
			infoPanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
				    if (e.getButton() != MouseEvent.BUTTON3) {
				        getCheckBox().doClick();
				    }
				    else {
				        e.setSource(CheckItemPanel.this);
                        for (MouseListener ml : CheckItemPanel.this.getMouseListeners()) {
                            ml.mouseClicked(e);
                        }
				    }
				}
			});
		}
		return infoPanel;
	}
    
	private JPanel getButtonsPanel() {
		if(buttonsPanel == null) {
			buttonsPanel=new JPanel();
			buttonsPanel.setLayout(new BorderLayout());
			buttonsPanel.setOpaque(false);
			buttonsPanel.setBorder(null);
			buttonsPanel.add(getStatesLabel(), BorderLayout.WEST);
			buttonsPanel.add(getNoteToggleButton(), BorderLayout.EAST);
			//buttonsPanel.add(getActionsToggleButton(), BorderLayout.EAST);
		}
		return buttonsPanel;
	}
    
	public JLabel getStatesLabel() {
		if (statesLabel == null) {
			statesLabel = new JLabel();
			statesLabel.setForeground(CHECK_COLOR.darker().darker());
		}
		return statesLabel;
	}
    
	private void fixStateLabel() {
    	String lb = " ";
    	if (!"".equalsIgnoreCase(getNoteTextArea().getText()))
    		lb += "N ";
    	if (getActionsListPanelHolder().numberOfSubItems() > 0)
    		lb += "A ";
    	getStatesLabel().setText(lb);
    }
    

	
	/* (non-Javadoc)
     * @see java.awt.Component#setName(java.lang.String)
     */
	@Override
    public void setName(String name) {
        nameCheckItem.setText(name);
        super.setName(name);
    }

    /* (non-Javadoc)
     * @see java.awt.Component#getName()
     */
    @Override
    public String getName()
    {
        if (nameCheckItem == null)
             return "";
        return nameCheckItem.getText();
    }

    /**
	 * This method initializes jCheckBox	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */    
	private JCheckBox getCheckBox() {
		if (checkBox == null) {
			checkBox = new JCheckBox();
			checkBox.setIcon(NOT_OK_IMAGE_ICON);
            checkBox.setOpaque(false);
            checkBox.setText(" ");
            checkBox.setInheritsPopupMenu(true);
			checkBox.addItemListener(new ItemListener() { 
				public void itemStateChanged(ItemEvent e) {    
					//System.out.println("itemStateChanged()");
                    MyJTaskPaneGroup mtpg = getMyJTaskPaneGroupFromItem(CheckItemPanel.this);
                    if (getCheckBox().isSelected()) {
                        setBackground(CHECK_COLOR);
                        //setBackground(new Color(181, 198, 216));
                        checkBox.setIcon(OK_IMAGE_ICON);
                        getDatePanel().setVisible(true);
                        trialTime = new Date();
                        String dateTime = dateXMLFormater.format(trialTime);
                        dateChangedLabel.setText(dateTime);
                        if (mtpg != null) {
							mtpg.firePropertyChange(
									MyJTaskPaneGroup.CHILD_ITEM_CHECKED_PROPERTY,
									false, true);
						}
                    }
                    else {
                        setBackground(baseBackgroundColor);
                        checkBox.setIcon(NOT_OK_IMAGE_ICON);
                        getDatePanel().setVisible(false);
                        if (mtpg != null) {
                            mtpg.firePropertyChange(
                                    MyJTaskPaneGroup.CHILD_ITEM_CHECKED_PROPERTY,
                                    true, false);
                        }
                    }
                    //CheckItemPanel.this.fireChangeEvent((Component) e.getSource());
                    if (mtpg != null) {
                    	mtpg.firePropertyChange(
                    			ChecklistPanel.DIRTY_PROPERTY,
                    			false, true);
                    }
				}
			});
		}
		return checkBox;
	}
	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */    
	private JPanel getNotesPanel() {
		if (notesPanel == null) {
			notesPanel = new JPanel();
			notesPanel.setLayout(new BorderLayout());
            notesPanel.setOpaque(false);
			notesPanel.setVisible(false);
			notesPanel.add(getNoteScrollPane(), BorderLayout.CENTER);
			notesPanel.add(getDatePanel(), BorderLayout.NORTH);
		}
		return notesPanel;
	}
	
	
	private JPanel getActionsPanel() {
		if (actionsPanel == null) {
			actionsPanel = new JPanel();
			actionsPanel.setLayout(new BorderLayout());
			actionsPanel.setBorder(new TitledBorder("Actions"));
            actionsPanel.setOpaque(false);
			actionsPanel.setVisible(false);
			//actionsPanel.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
			//actionsPanel.add(new JLabel("wefwerfgew"));
			//actionsPanel.add(getUserActionPanel());
			actionsPanel.add(getActionsListPanelHolder(), BorderLayout.CENTER);
		}
		return actionsPanel;
	}
	
//	private JPanel getUserActionPanel()
//	{
//		if(userActionsPanel==null)
//		{
//			userActionsPanel=new JPanel();
//			userActionsPanel.setLayout(new BorderLayout());
//            userActionsPanel.setOpaque(false);
//			
//			JPanel panelAux2=new JPanel();
//			userActionCheckBox=new JCheckBox("Wait for user action");
//			userConfirmationCheckBox=new JCheckBox("User confirmation");
//			
//			panelAux2.add(userActionCheckBox);
//			panelAux2.add(userConfirmationCheckBox);
//			userActionsPanel.add(panelAux2,java.awt.BorderLayout.NORTH);
//			userMSGActionText = new JTextField();
//			
//			JPanel panelAux=new JPanel();
//			userMSGActionText.setColumns(20);
//			userMSGActionText.setEnabled(false);
//			panelAux.add(new JLabel("Message:"));
//			
//			userActionCheckBox.addItemListener(new java.awt.event.ItemListener() { 
//				public void itemStateChanged(java.awt.event.ItemEvent e) {    
//                    if (userActionCheckBox.isSelected())
//                    {
//            			userMSGActionText.setEnabled(true);            
//                    }
//                    else
//                    {
//                    	userMSGActionText.setEnabled(false);
//                    }
//                }
//			});
//			//userMSGActionText.setSize(100, 15);
//			//userMSGActionText.setMinimumSize(new Dimension(100, 15));
//			//userMSGActionText.setMaximumSize(new Dimension(100, 15));
//			panelAux.add(userMSGActionText);
//			userActionsPanel.add(panelAux,java.awt.BorderLayout.CENTER);
//		}
//		return userActionsPanel;
//	}
	
	private AutoItemsList getActionsListPanelHolder() {
		if (actionsListPanelHolder == null) {
			actionsListPanelHolder = new AutoItemsList(this);
			// arCheckPanel.add(new
			// JLabel("teste no centro"),java.awt.BorderLayout.CENTER);
		}
		return actionsListPanelHolder;
	}
	
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane
	 */    
	private JScrollPane getNoteScrollPane() {
		if (noteScrollPane == null) {
			noteScrollPane = new JScrollPane();
            noteScrollPane.setOpaque(false);
			noteScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			noteScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			noteScrollPane.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
			noteScrollPane.setPreferredSize(new Dimension(300,50));
			noteScrollPane.setViewportView(getNoteTextArea());
		}
		return noteScrollPane;
	}
	/**
	 * This method initializes jTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */    
	private JTextArea getNoteTextArea() {
		if (noteTextArea == null) {
			noteTextArea = new JTextArea();
			noteTextArea.setLineWrap(true);
			noteTextArea.addKeyListener(new KeyAdapter() {
					@Override
			        public void keyTyped(KeyEvent e) {
			            //System.out.println("keyTyped()");
                        CheckItemPanel.this.fireChangeEvent((Component) e.getSource());
			        }
			        
			        @Override
			        public void keyReleased(KeyEvent e) {
				        fixStateLabel();
			        }
			    });
		}
		return noteTextArea;
	}
	/**
	 * This method initializes jToggleButton	
	 * 	
	 * @return javax.swing.JToggleButton	
	 */    
	private JToggleButton getNoteToggleButton() {
		if (noteToggleButton == null) {
			noteToggleButton = new JToggleButton();
			//jToggleButton.setText("set note");
            noteToggleButton.setMargin(new Insets(0,0,0,0));
			noteToggleButton.setIcon(EDIT_IMAGE_ICON);
			noteToggleButton.setToolTipText("See Note & Actions");
			noteToggleButton.setFont(new Font("Dialog", Font.BOLD, 10));
			noteToggleButton.addItemListener(new ItemListener() { 
				public void itemStateChanged(ItemEvent e) {    
					//System.out.println("itemStateChanged()");
					if (getNoteToggleButton().isSelected()) {
					    getNotesPanel().setVisible(true);
					    getActionsPanel().setVisible(true);
					}
					else {
					    getNotesPanel().setVisible(false);
					    getActionsPanel().setVisible(false);
					}
					//System.out.println("Text press");
				}
			});
		}
		return noteToggleButton;
	}

	

	
    /**
     * This method initializes dateChangedLabel	
     * 	
     * @return javax.swing.JLabel	
     */    
    private JLabel getDateChangedLabel() {
    	if (dateChangedLabel == null) {
    		dateChangedLabel = new JLabel();
    		dateChangedLabel.setText("DateTime");
    		dateChangedLabel.addPropertyChangeListener("text",
    		        new PropertyChangeListener() {
    		                public void propertyChange(PropertyChangeEvent e) {
    		                    //System.out.println("propertyChange(text)");
                                // TODO Auto-generated property Event stub "text" 
    		                }
    		            });
    	}
    	return dateChangedLabel;
    }

    /**
     * This method initializes datePanel	
     * 	
     * @return javax.swing.JPanel	
     */    
    private JPanel getDatePanel() {
    	if (datePanel == null) {
    		dateLabLabel = new JLabel();
    		dateLabLabel.setText("Date checked:");
    		datePanel = new JPanel();
            datePanel.setOpaque(false);
    		datePanel.setVisible(false);
    		datePanel.add(dateLabLabel, null);
    		datePanel.add(getDateChangedLabel(), null);
    	}
    	return datePanel;
    }
    
    
    
    public Container getParentGroup() {
        return this.getParent().getParent().getParent().getParent();
    }

    
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CheckItemPanel) {
            CheckItemPanel comp = (CheckItemPanel) obj;
            if (this.nameCheckItem.getText().equals(comp.nameCheckItem.getText()))
                return true;
            else
                return false;
        }
        else
            return false;
    }

    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(ChecklistPanel.DIRTY_PROPERTY)) {
            NeptusLog.pub().info(this
                    + ": user change"); // + arg0.getSource());
            boolean newValue = ((Boolean) e.getNewValue())
                    .booleanValue();
            ((MyJTaskPaneGroup) this.getParentGroup()).firePropertyChange(
                    ChecklistPanel.DIRTY_PROPERTY, !newValue, newValue);
        }
    }
    
	void fireChangeEvent(Component source) {
        //NeptusLog.pub().warn("[" +
        //        source + "]fireChangeEvent Panel: " + true);
        Container parent = this.getParent();
        if (parent != null) {
            //System.err.println("dd " + parent.getParent().getParent().getParent().getClass());
            parent = parent.getParent().getParent().getParent();
            //System.err.println("...");
            ((MyJTaskPaneGroup)parent).firePropertyChange(ChecklistPanel.DIRTY_PROPERTY, false, true);
            //System.err.println("......");
        }
        fixStateLabel();
    }

    public CheckItem getCheckItem() {
        CheckItem ci = new CheckItem();
        ci.setChecked(checkBox.isSelected());
        ci.setName(nameCheckItem.getText());
        if (ci.isChecked())
            ci.setDateChecked(dateChangedLabel.getText());
        ci.setNote(noteTextArea.getText());
        
        Component[] list = getActionsListPanelHolder().getComponents();
        //System.out.println("num de elem: "+list.length);
        for(Component c : list) {
        	//System.out.println("antes de tentar");
        	try {			
        		//System.out.println(c);
        		ci.addAutoSubItem( ((CheckSubItem) c).getCheckAutoSubItem());
        		//System.out.println("adicionou");
        	}
        	catch (Exception e2) {
        		//e2.printStackTrace();
        	}
        }
        return ci;
    }
}
