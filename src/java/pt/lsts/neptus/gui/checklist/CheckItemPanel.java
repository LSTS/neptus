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
 * Author: Paulo Dias
 * Created in 26/Jun/2005
 */
package pt.lsts.neptus.gui.checklist;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserActionItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;
import pt.lsts.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.lsts.neptus.types.checklist.CheckItem;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class CheckItemPanel extends JPanel implements PropertyChangeListener {

    private static final long serialVersionUID = 1L;

    public static Icon OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/selectedIcon.png", 18, 18));
    public static Icon NOT_OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/boxIcon.png", 18, 18));
    private static final Icon EDIT_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/edit.png", 16, 16)); 

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
    public String getName() {
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
                @Override
                public void itemStateChanged(ItemEvent e) {    
                    MyJTaskPaneGroup mtpg = getMyJTaskPaneGroupFromItem(CheckItemPanel.this);
                    if (getCheckBox().isSelected()) {
                        setBackground(CHECK_COLOR);
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
            actionsPanel.setBorder(new TitledBorder(I18n.text("Actions")));
            actionsPanel.setOpaque(false);
            actionsPanel.setVisible(false);
            actionsPanel.add(getActionsListPanelHolder(), BorderLayout.CENTER);
        }
        return actionsPanel;
    }

    private AutoItemsList getActionsListPanelHolder() {
        if (actionsListPanelHolder == null) {
            actionsListPanelHolder = new AutoItemsList(this);
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
                    //NeptusLog.pub().info("<###>keyTyped()");
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
            noteToggleButton.setMargin(new Insets(0,0,0,0));
            noteToggleButton.setIcon(EDIT_IMAGE_ICON);
            noteToggleButton.setToolTipText(I18n.text("See Note & Actions"));
            noteToggleButton.setFont(new Font("Dialog", Font.BOLD, 10));
            noteToggleButton.addItemListener(new ItemListener() { 
                @Override
                public void itemStateChanged(ItemEvent e) {    
                    if (getNoteToggleButton().isSelected()) {
                        getNotesPanel().setVisible(true);
                        getActionsPanel().setVisible(true);
                    }
                    else {
                        getNotesPanel().setVisible(false);
                        getActionsPanel().setVisible(false);
                    }
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
            dateChangedLabel.setText(I18n.text("DateTime"));
            dateChangedLabel.addPropertyChangeListener("text",
                    new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    //NeptusLog.pub().info("<###>propertyChange(text)");
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
            dateLabLabel.setText(I18n.text("Date checked:"));
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

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(ChecklistPanel.DIRTY_PROPERTY)) {
            NeptusLog.pub().info(this + ": user change");
            boolean newValue = ((Boolean) e.getNewValue())
                    .booleanValue();
            ((MyJTaskPaneGroup) this.getParentGroup()).firePropertyChange(
                    ChecklistPanel.DIRTY_PROPERTY, !newValue, newValue);
        }
    }

    void fireChangeEvent(Component source) {
        Container parent = this.getParent();
        if (parent != null) {
            parent = parent.getParent().getParent().getParent();
            ((MyJTaskPaneGroup)parent).firePropertyChange(ChecklistPanel.DIRTY_PROPERTY, false, true);
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
        for(Component c : list) {
            try {			
                ci.addAutoSubItem( ((CheckSubItem) c).getCheckAutoSubItem());
            }
            catch (Exception e2) {
                //e2.printStackTrace();
            }
        }
        return ci;
    }
}
