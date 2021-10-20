/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 26/Jun/2005
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.SystemImcMsgCommInfo;
import pt.lsts.neptus.gui.BlockingGlassPane;
import pt.lsts.neptus.gui.checklist.exec.CheckListExe;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.types.checklist.CheckItem;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.output.OutputMonitor;
import pt.lsts.neptus.util.xsl.TransformFOP;

/**
 * @author Paulo Dias
 */
public class ChecklistPanel extends JPanel implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;

    public static final String DIRTY_PROPERTY = "user change";
    public static final String SAVEAS_PROPERTY = "file changed";

    public static Icon NEPTUS_ICON = new ImageIcon(ImageUtils.getImage("images/neptus-icon1.png"));
    public static Icon OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage("images/checklists/selectedIcon.png",
            22, 22));
    public static Icon NOT_OK_IMAGE_ICON = new ImageIcon(ImageUtils.getScaledImage("images/checklists/boxIcon.png", 22,
            22));
    public static Icon ICON_PDF = new ImageIcon(ImageUtils.getImage("images/checklists/pdf.png"));
    public static Icon ICON_SAVEAS = new ImageIcon(ImageUtils.getImage("images/checklists/filesaveas.png"));
    public static Icon ICON_SAVE = new ImageIcon(ImageUtils.getImage("images/checklists/filesave.png"));
    public static Icon ICON_OPEN = new ImageIcon(ImageUtils.getImage("images/checklists/fileopen.png"));
    public static Icon ICON_NEW = new ImageIcon(ImageUtils.getImage("images/checklists/filenew.png"));
    public static Icon ICON_CANCEL = new ImageIcon(ImageUtils.getImage("images/checklists/cancel.png"));
    public static Icon ICON_RUN = new ImageIcon(ImageUtils.getImage("images/checklists/run.png"));

    public static String FLAT_TITLE;
    public static String NOT_FLAT_TITLE;

    private static final int MAX_NUMBER_OF_SHOWN_CHARS = 20;

    protected static Color BLUE_1 = new Color(181, 198, 216);
    protected static Color GREEN_1 = new Color(151, 206, 93);
    protected static Color RED_1 = new Color(246, 198, 181);
    protected static Color ORANGE_1 = new Color(230, 158, 35);

    private JFrame jFrame = null;
    private JInternalFrame jInternalFrame = null;

    private ChecklistType checklist = null;

    private String originalFilePath = "";

    private boolean isEditable = false;
    private boolean isFlat = true;

    private boolean userCancel = false;
    private boolean isChanged = false;

    private MyJTaskPaneGroup templateTaskPaneGroup = null;

    // private boolean ignoreExpandTaskGroup = false;
    private MyJTaskPaneGroup selectedGroup = null;

    // ------- UI Components -------
    private JPanel headerPanel = null;

    private JPanel hNamePanel = null;
    private JLabel nameLabel = null;
    private JLabel nameText = null;

    private JPanel hVersionPanel = null;
    private JLabel versionLabel = null;
    private JTextField versionText = null;

    private JPanel hDescriptionPanel = null;
    private JLabel descLabel = null;
    private JScrollPane descScrollPane = null;
    private JTextArea descTextArea = null;

    private JPanel toolButtonsPanel = null;
    private JButton generatePDFButton = null;
    private JButton saveAsButton = null;
    private JButton saveButton = null;
    private JButton cancelButton = null;
    private JButton runButton = null;
    private JButton newButton = null;
    private JButton openButton = null;

    private JScrollPane checkTaskPaneScrollPane = null;
    private JXTaskPaneContainer checkTaskPane = null;

    private JPanel editPanel = null;
    private JLabel selectedGroupLabel = null;
    private JButton appendGroupButton = null;
    private JButton appendItemButton = null;
    // END ------- UI Components -------

    // ------- Popup Menu for Checklist -------
    private JPopupMenu checklistPopupMenu = null;
    private JMenuItem addChecklistMenuItem = null;
    private JCheckBoxMenuItem makeFlatChecklistCheckBoxMenuItem = null;
    private JMenuItem unCheckAllChecklistGroupsMenuItem = null;

    // ------- Popup Menu for Checklist Group -------
    private JPopupMenu checkGroupPopupMenu = null;
    private JMenuItem editNameCheckGroupMenuItem = null;
    private JMenuItem removeCheckGroupMenuItem = null;
    private JMenuItem addCheckItemMenuItem = null;
    private JMenuItem moveUpCheckGroupMenuItem = null;
    private JMenuItem moveDownCheckGroupMenuItem = null;
    private JMenuItem insertCheckGroupMenuItem = null;
    private JMenuItem unCheckAllGroupItemsMenuItem = null;

    // ------- Popup Menu for Checklist Group Item -------
    private JPopupMenu checkItemPopupMenu = null;
    private JMenuItem editNameCheckItemMenuItem = null;
    private JMenuItem editDateCheckItemMenuItem = null;
    private JMenuItem removeCheckItemMenuItem = null;
    private JMenuItem moveUpCheckItemMenuItem = null;
    private JMenuItem moveDownCheckItemMenuItem = null;
    private JMenuItem insertCheckItemMenuItem = null;

    private Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();

    /**
     * This is the default constructor
     */
    public ChecklistPanel(ChecklistType cl) {
        super();
        
        FLAT_TITLE = I18n.text("Flat Checklist");
        NOT_FLAT_TITLE = I18n.text("Not Flat Checklist");

        checklist = cl;
        isFlat = checklist.isFlat();
        originalFilePath = checklist.getOriginalFilePath();

        if (originalFilePath.length() < 1)
            setChanged(true);

        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setHgap(5);
        borderLayout.setVgap(5);
        this.setLayout(borderLayout);
        this.setSize(391, 550);
        this.add(getHeaderPanel(), BorderLayout.NORTH);
        this.add(getCheckTaskPaneScrollPane(), BorderLayout.CENTER);
        this.add(getEditPanel(), BorderLayout.SOUTH);

        // if ( (originalFilePath == null) ||
        // originalFilePath.equalsIgnoreCase(""))
        // {
        getSaveButton().setEnabled(false);
        // setChanged(false);
        // }

        this.addPropertyChangeListener(DIRTY_PROPERTY, this);
        this.addPropertyChangeListener(SAVEAS_PROPERTY, this);

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.Component#getName()
     */
    @Override
    public String getName() {
        if (nameText == null)
            return "";
        return nameText.getText();
    }

    /**
     * @return Returns the isEditable.
     */
    public boolean isEditable() {
        return isEditable;
    }

    /**
     * @param isEditable The isEditable to set.
     */
    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    /**
     * @return Returns the isChanged.
     */
    public boolean isChanged() {
        return isChanged;
    }

    /**
     * @return Returns the isChanged.
     */
    public boolean isDirty() {
        return isChanged;
    }

    /**
     * @param isChanged The isChanged to set.
     */
    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

    /**
     * @param source
     */
    private void fireChangeEvent(Component source) {
        // NeptusLog.pub().warn("[" +
        // source + "]fireChangeEvent: "
        // + ChecklistPanel.this.isChanged() + "->" + true);
        ChecklistPanel.this.firePropertyChange(DIRTY_PROPERTY, ChecklistPanel.this.isChanged(), true);
    }

    /**
     * @return Returns the isFlat.
     */
    private boolean isFlat() {
        return isFlat;
    }

    /**
     * @param isFlat The isFlat to set.
     */
    private void setFlat(boolean isFlat) {
        this.isFlat = isFlat;
    }

    /**
     * @return
     */
    private boolean makeItFlat() {
        if (isFlat())
            return true;
        else {
            int countCG = checkTaskPane.getComponentCount();
            if (countCG == 0) {
                setFlat(true);
                return true;
            }
            else if (countCG == 1) {
                Component[] compJTaskPaneGroup = getCheckTaskPane().getComponents();
                MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) compJTaskPaneGroup[0];
                // mtpg.setTitle(FLAT_TITLE); //ChecklistType.FLAT_ID);
                mtpg.setGroupName(FLAT_TITLE); // ChecklistType.FLAT_ID);
                setFlat(true);
                setSelectedGroup(mtpg);
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    private boolean makeItNotFlat() {
        if (!isFlat())
            return true;
        else {
            setFlat(false);
            Component[] compJTaskPaneGroup = getCheckTaskPane().getComponents();
            MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) compJTaskPaneGroup[0];
            setSelectedGroup(mtpg);
            if (mtpg.getGroupName().equalsIgnoreCase(FLAT_TITLE)) {
                mtpg.setGroupName(NOT_FLAT_TITLE);
            }
            return true;
        }
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getHeaderPanel() {
        if (headerPanel == null) {
            headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.add(getToolButtonsPanel());
            headerPanel.add(getHNamePanel());
            headerPanel.add(getHVersionPanel());
            headerPanel.add(getHDescriptionPanel());
            /*
             * for (int i = 0; i < 15; i++) { jPanel.add(new CheckItemPanel()); }
             */
            /*
             * JTaskPane tp = new JTaskPane(); JTaskPaneGroup tpg = new JTaskPaneGroup(); tpg.setText("tas1");
             * CheckItemPanel ci = new CheckItemPanel(); tpg.add(ci); for (int i = 0; i < 20; i++) { CheckItemPanel ci1
             * = new CheckItemPanel(); tpg.add(ci1); } tp.add(tpg);
             * 
             * JTaskPaneGroup tpg1 = new JTaskPaneGroup(); tpg1.setText("tas1"); CheckItemPanel ci1 = new
             * CheckItemPanel(); tpg1.add(ci1); tp.add(tpg1); jPanel.add(tp);
             */

        }
        return headerPanel;
    }

    /**
     * This method initializes jScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getCheckTaskPaneScrollPane() {
        if (checkTaskPaneScrollPane == null) {
            checkTaskPaneScrollPane = new JScrollPane();
            checkTaskPaneScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            checkTaskPaneScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            checkTaskPaneScrollPane.setViewportView(getCheckTaskPane());
            checkTaskPaneScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }
        return checkTaskPaneScrollPane;
    }

    /**
     * 
     */
    private void checkForChecklistChangeForSave() {
        if (ChecklistPanel.this.isChanged()) {
            // Check for empty Check List
            if (getCheckTaskPane().getComponents().length == 0)
                return;
            if (isFlat) {
                if (getCheckTaskPane().getComponents().length == 1) {
                    MyJTaskPaneGroup jtpg = (MyJTaskPaneGroup) getCheckTaskPane().getComponents()[0];
                    if (jtpg.listCheckItemPanel.size() == 0)
                        return;
                }
            }

            int response = JOptionPane.showConfirmDialog(ChecklistPanel.this, 
                    "<html>"
                    + I18n.textf("The checklist %checklist was not saved yet.", "<strong>" + ChecklistPanel.this.getName() + "</strong>")
                    + "<br>"
                    + I18n.text("Do you want to save it now?")
                    + "</html>", I18n.text("Save checklist?"), JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                ChecklistPanel.this.save();
                userCancel = false;
            }
            else {
                userCancel = true;
            }
        }
        else
            userCancel = true;
    }

    /**
     * This method initializes jFrame
     * 
     * @return javax.swing.JFrame
     */
    private JFrame getJFrame(String title) {
        if (title == null || title.isEmpty())
            jFrame = new JFrame(I18n.textf("Checklist %name", nameText.getText()));
        else
            jFrame = new JFrame(title);
        jFrame.getContentPane().add(this);
        jFrame.setSize(getWidth() + 5, getHeight() + 80);
        jFrame.setAlwaysOnTop(true);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                checkForChecklistChangeForSave();
                hideFrame();
            }
        });
        GuiUtils.centerOnScreen(jFrame);
        jFrame.setIconImage(ImageUtils.getImage("images/box_checked.png"));
        jFrame.setVisible(true);

        return jFrame;
    }

    /**
     * @param internalFrame The jInternalFrame to set.
     */
    public void setJInternalFrame(JInternalFrame internalFrame) {
        jInternalFrame = internalFrame;
        jInternalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
            }
        });
    }

    public void addChangeListener(ChangeListener cl) {
        changeListeners.add(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        changeListeners.remove(cl);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
        if (prop.equals(DIRTY_PROPERTY)) {
            // NeptusLog.pub().info(ChecklistPanel.this
            // + ": user change"); // + arg0.getSource());
            boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
            ChecklistPanel.this.setChanged(newValue);
            if (newValue == false)
                getSaveButton().setEnabled(false);
            else if ((originalFilePath == null) || originalFilePath.equalsIgnoreCase(""))
                getSaveButton().setEnabled(false);
            else
                getSaveButton().setEnabled(true);
        }

        // System.err.println("Changes! "+e.getNewValue());

        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener cl : changeListeners) {
            cl.stateChanged(evt);
        }
    }

    /**
     * This method initializes checkTaskPane
     * 
     * @return com.l2fprod.common.swing.JTaskPane
     */
    private JXTaskPaneContainer getCheckTaskPane() {
        if (checkTaskPane == null) {
            checkTaskPane = new JXTaskPaneContainer();
            checkTaskPane.setBackground(java.awt.Color.white);
            checkTaskPane.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // NeptusLog.pub().info("<###>mouseClicked()");
                    if (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1) {
                        getChecklistPopupMenu().show((Component) e.getSource(), e.getX(), e.getY());
                    }
                }
            });
            checkTaskPane.addPropertyChangeListener(DIRTY_PROPERTY, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent arg0) {
                    // NeptusLog.pub().info(checkTaskPane
                    // + ": user change");
                    boolean newValue = ((Boolean) arg0.getNewValue()).booleanValue();
                    ChecklistPanel.this.firePropertyChange(DIRTY_PROPERTY, ChecklistPanel.this.isChanged(), newValue);
                }

            });

            /*
             * JTaskPaneGroup tpg = new JTaskPaneGroup(); tpg.setText("tas1"); tpg.setExpanded(false); CheckItemPanel ci
             * = new CheckItemPanel(); tpg.add(ci); for (int i = 0; i < 20; i++) { CheckItemPanel ci1 = new
             * CheckItemPanel(); tpg.add(ci1); } checkTaskPane.add(tpg);
             * 
             * JTaskPaneGroup tpg1 = new JTaskPaneGroup(); tpg1.setText("tas1"); CheckItemPanel ci1 = new
             * CheckItemPanel(); tpg1.add(ci1); checkTaskPane.add(tpg1);
             */

            if (isFlat) {
                MyJTaskPaneGroup tpg = createTemplateTaskPaneGroup();
                tpg.setGroupName(FLAT_TITLE);
                tpg.setCollapsed(false);
                LinkedHashMap<String, LinkedList<CheckItem>> cits = checklist.getGroupList();
                Iterator<LinkedList<CheckItem>> it = cits.values().iterator();
                while (it.hasNext()) {
                    LinkedList<CheckItem> lli = it.next();
                    Iterator<CheckItem> it1 = lli.iterator();
                    while (it1.hasNext()) {
                        CheckItem ci = it1.next();
                        CheckItemPanel cip = new CheckItemPanel(ci);
                        addCheckItemMouseAdapter(cip);
                        tpg.add(cip);
                    }
                }

                checkTaskPane.add(tpg);
                setSelectedGroup(tpg);
                getAppendItemButton().setEnabled(true);
            }
            else {
                // FIXME
                LinkedHashMap<String, LinkedList<CheckItem>> cits = checklist.getGroupList();
                Iterator<String> it = cits.keySet().iterator();
                while (it.hasNext()) {
                    String group = it.next();
                    MyJTaskPaneGroup tpg = createTemplateTaskPaneGroup();
                    tpg.setGroupName(group);
                    tpg.setCollapsed(true);

                    LinkedList<CheckItem> lli = cits.get(group);
                    Iterator<CheckItem> it1 = lli.iterator();
                    while (it1.hasNext()) {
                        CheckItem ci = it1.next();
                        CheckItemPanel cip = new CheckItemPanel(ci);
                        addCheckItemMouseAdapter(cip);
                        tpg.add(cip);
                    }
                    checkTaskPane.add(tpg);
                    setSelectedGroup(null);
                    getAppendItemButton().setEnabled(false);
                }
            }
        }
        return checkTaskPane;
    }

    /**
     * This method initializes descScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getDescScrollPane() {
        if (descScrollPane == null) {
            descScrollPane = new JScrollPane();
            descScrollPane.setPreferredSize(new Dimension(400, 100));
            // descScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(5,20,5,20));
            descScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            descScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            descScrollPane.setViewportView(getDescTextArea());
        }
        return descScrollPane;
    }

    /**
     * This method initializes descTextArea
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getDescTextArea() {
        if (descTextArea == null) {
            descTextArea = new JTextArea();
            descTextArea.setLineWrap(true);
            descTextArea.setText(checklist.getDescription());
            descTextArea.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // FIXME Ver melhor como fazer isto
                    // NeptusLog.pub().info("<###>keyTyped()");
                    fireChangeEvent(descTextArea);
                }
            });
        }
        return descTextArea;
    }

    /**
     * This method initializes h1Panel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getHNamePanel() {
        if (hNamePanel == null) {
            FlowLayout flowLayout1 = new FlowLayout();
            flowLayout1.setAlignment(FlowLayout.LEFT);
            hNamePanel = new JPanel();
            nameLabel = new JLabel();
            nameLabel.setText("<html><b>" + I18n.text("Name:"));
            nameText = new JLabel();
            nameText.setText(checklist.getName());
            hNamePanel.setLayout(flowLayout1);
            hNamePanel.add(nameLabel, null);
            hNamePanel.add(nameText, null);
        }
        return hNamePanel;
    }

    public void makeNameEditable() {
        nameText.setToolTipText(I18n.text("Right click to change (if edit mode)."));
        nameText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // NeptusLog.pub().info("<###>mouseClicked()");
                if (e.getButton() == MouseEvent.BUTTON3) {
                    String inputValue = JOptionPane.showInputDialog(ChecklistPanel.this,
                            I18n.textf("Please input a new name for \"%name\"", nameText.getText()), nameText.getText());
                    // NeptusLog.pub().info("<###>(new name)" + inputValue);
                    if (inputValue != null && !inputValue.equalsIgnoreCase("")) {
                        // TODO verificar se nome já existe pq isto é um ID
                        nameText.setText(inputValue);
                        fireChangeEvent(nameText);
                        if (jFrame != null) {
                            jFrame.setTitle(I18n.textf("Checklist %name", getName()));
                        }
                    }
                }
            }
        });
    }

    private JPanel getHVersionPanel() {
        if (hVersionPanel == null) {
            FlowLayout flowLayout1 = new FlowLayout();
            flowLayout1.setAlignment(FlowLayout.LEFT);
            hVersionPanel = new JPanel();
            versionLabel = new JLabel();
            versionLabel.setText("<html><b>" + I18n.text("Version:"));
            versionText = new JTextField(20);
            versionText.setText(checklist.getVersion());
            versionText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            versionText.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    // FIXME Ver melhor como fazer isto
                    // NeptusLog.pub().info("<###>keyTyped()");
                    fireChangeEvent(versionText);
                }
            });
            hVersionPanel.setLayout(flowLayout1);
            hVersionPanel.add(versionLabel, null);
            hVersionPanel.add(versionText, null);
        }
        return hVersionPanel;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getHDescriptionPanel() {
        if (hDescriptionPanel == null) {
            BorderLayout borderLayout1 = new BorderLayout();
            borderLayout1.setHgap(5);
            borderLayout1.setVgap(5);
            hDescriptionPanel = new JPanel();
            descLabel = new JLabel();
            descLabel.setText("<html><b>" + I18n.text("Description:"));
            hDescriptionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            hDescriptionPanel.setLayout(borderLayout1);
            hDescriptionPanel.add(descLabel, BorderLayout.NORTH);
            hDescriptionPanel.add(getDescScrollPane(), BorderLayout.CENTER);
        }
        return hDescriptionPanel;
    }

    /**
     * This method initializes toolButtonsPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getToolButtonsPanel() {
        if (toolButtonsPanel == null) {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.LEADING);
            toolButtonsPanel = new JPanel();
            toolButtonsPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
            toolButtonsPanel.setLayout(flowLayout);
            toolButtonsPanel.setSize(8, 8);
            toolButtonsPanel.add(new JLabel(NEPTUS_ICON));
            toolButtonsPanel.add(Box.createHorizontalStrut(10));
            toolButtonsPanel.add(getNewButton());
            toolButtonsPanel.add(getOpenButton());
            toolButtonsPanel.add(getSaveButton());
            toolButtonsPanel.add(getSaveAsButton());
            toolButtonsPanel.add(getGeneratePDFButton());
            toolButtonsPanel.add(getRunButton());
            toolButtonsPanel.add(getCancelButton());
        }
        return toolButtonsPanel;
    }

    private void iconifyToolButtons() {
        getOpenButton().setIcon(ICON_OPEN);
        getOpenButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getNewButton().setIcon(ICON_NEW);
        getNewButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getGeneratePDFButton().setIcon(ICON_PDF);
        getGeneratePDFButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getRunButton().setIcon(ICON_RUN);
        getRunButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getSaveAsButton().setIcon(ICON_SAVEAS);
        getSaveAsButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getSaveButton().setIcon(ICON_SAVE);
        getSaveButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
        getCancelButton().setIcon(ICON_CANCEL);
        getCancelButton().setPreferredSize(new Dimension(73 + 34, 26 + 14));
    }

    /**
     * This method initializes okButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getSaveButton() {
        if (saveButton == null) {
            saveButton = new JButton();
            saveButton.setText(I18n.text("Save"));
            saveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>Ok actionPerformed()");
                    userCancel = false;
                    // hideFrame();
                    // NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(getChecklistType().asXML()));
                    saveFile();
                }

            });
            saveButton.setPreferredSize(new Dimension(73, 26));
        }
        return saveButton;
    }

    public boolean save() {
        if ((originalFilePath == null) || originalFilePath.equalsIgnoreCase("")) {
            int response = JOptionPane.showConfirmDialog(this, 
                    "<html>"
                    + I18n.textf("The checklist %name was not saved yet.", "<strong>" + getName() + "</strong>")
                    + "<br>"
                    + I18n.text("Do you want to save it now?")
                    + "</html>", I18n.text("Save checklist?"),
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (response != JOptionPane.YES_OPTION)
                return false;
            return saveAsFile();
        }
        else
            return saveFile();
    }

    /**
     * @return
     */
    public boolean saveAsFile() {
        File fx;
        if ((originalFilePath == null) || originalFilePath.equalsIgnoreCase("")) {
            fx = ChecklistFileChooser.showSaveDialog(this, null);
        }
        else {
            fx = ChecklistFileChooser.showSaveDialog(this, new File(originalFilePath));
        }
        if ((fx != null) && !fx.getName().equalsIgnoreCase("")) {
            String ext = FileUtil.getFileExtension(fx);
            if (FileUtil.FILE_TYPE_CHECKLIST.equalsIgnoreCase(ext) || FileUtil.FILE_TYPE_XML.equalsIgnoreCase(ext)) {
                ext = "";
            }
            else
                ext = "." + FileUtil.FILE_TYPE_CHECKLIST;
            originalFilePath = fx.getAbsolutePath();
            originalFilePath = originalFilePath + ext;
            boolean ret = saveFile();
            if (ret) {
                getSaveButton().setEnabled(false);
                ChecklistPanel.this.firePropertyChange(SAVEAS_PROPERTY, false, true);
            }
            return ret;
        }
        return false;
    }

    /**
     * @return
     */
    public boolean saveFile() {
        if ((originalFilePath == null) || originalFilePath.equalsIgnoreCase("")) {
            JOptionPane.showMessageDialog(this, "<html>"
                    + I18n.text("Checklist <b>was not</b> saved!")
                    + "<br>"
                    + I18n.textf("File name was not valid [\"%file\"].", originalFilePath)
                    + "</html>");
            return false;
        }
        boolean ret = FileUtil.saveToFile(originalFilePath,
                FileUtil.getAsPrettyPrintFormatedXMLString(getChecklistType().asDocument())); // getChecklistType().asXML()
        if (ret) {
            checklist = getChecklistType();
            JOptionPane.showMessageDialog(this, "<html>" + I18n.text("Checklist saved!") 
                    + "<br>" + I18n.textf("To file \"%file\".", originalFilePath)
                    + "</html>");
            getSaveButton().setEnabled(false);
            setChanged(false);
        }
        return ret;
    }

    /**
     * @return the newButton
     */
    public JButton getNewButton() {
        if (newButton == null) {
            newButton = new JButton();
            newButton.setVisible(false);
            newButton.setText(I18n.text("New"));
            newButton.setPreferredSize(new Dimension(73, 26));
            newButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>Cancel actionPerformed()");
                    if (ChecklistPanel.this.isChanged()) {
                        ChecklistPanel.this.checkForChecklistChangeForSave();
                        // if (clp.userCancel)
                        // return;
                    }
                    BlockingGlassPane bgp = new BlockingGlassPane();
                    if (jFrame != null)
                        ChecklistPanel.this.jFrame.setGlassPane(bgp);
                    if (jInternalFrame != null)
                        ChecklistPanel.this.jInternalFrame.setGlassPane(bgp);
                    bgp.block(true);

                    ChecklistType clist = new ChecklistType();
                    clist.setName(I18n.text("New Checklist"));
                    ChecklistPanel.this.changeChecklist(clist);

                    bgp.block(false);
                }
            });
        }
        return newButton;
    }

    /**
     * @return the openButton
     */
    public JButton getOpenButton() {
        if (openButton == null) {
            openButton = new JButton();
            openButton.setText(I18n.text("Open"));
            openButton.setVisible(false);
            openButton.setPreferredSize(new Dimension(73, 26));
            openButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    // NeptusLog.pub().info("<###>Cancel actionPerformed()");
                    if (ChecklistPanel.this.isChanged()) {
                        ChecklistPanel.this.checkForChecklistChangeForSave();
                        // if (clp.userCancel)
                        // return;
                    }
                    File fx;
                    if ((ChecklistPanel.this.originalFilePath == null)
                            || ChecklistPanel.this.originalFilePath.equalsIgnoreCase("")) {
                        fx = ChecklistFileChooser.showOpenDialog(ChecklistPanel.this, null);
                    }
                    else {
                        fx = ChecklistFileChooser.showOpenDialog(ChecklistPanel.this, new File(
                                ChecklistPanel.this.originalFilePath));
                    }
                    if ((fx != null) && !fx.getName().equalsIgnoreCase("")) {
                        final File fxF = fx;
                        final BlockingGlassPane bgp = new BlockingGlassPane();
                        if (jFrame != null)
                            ChecklistPanel.this.jFrame.setGlassPane(bgp);
                        if (jInternalFrame != null)
                            ChecklistPanel.this.jInternalFrame.setGlassPane(bgp);
                        bgp.block(true);
                        SwingWorker<Void, ChecklistType> worker = new SwingWorker<Void, ChecklistType>() {
                            @Override
                            protected Void doInBackground() throws Exception {

                                ChecklistType clist = new ChecklistType(fxF.getAbsolutePath());
                                ArrayList<ChecklistType> al = new ArrayList<ChecklistType>();
                                al.add(clist);
                                process(al);
                                return null;
                            }

                            @Override
                            protected void process(List<ChecklistType> chunks) {
                                ChecklistPanel.this.changeChecklist(chunks.get(0));
                            };

                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                super.done();
                                bgp.block(false);
                            }
                        };
                        worker.execute();
                        // BlockingGlassPane bgp = new BlockingGlassPane();
                        // ChecklistPanel.this.jFrame.setGlassPane(bgp);
                        // bgp.block(true);
                        //
                        // ChecklistType clist = new ChecklistType(fx
                        // .getAbsolutePath());
                        // ChecklistPanel.this.changeChecklist(clist);
                        //
                        // bgp.block(false);
                    }
                }
            });
        }
        return openButton;
    }

    /**
     * This method initializes cancelButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setText(I18n.text("Cancel"));
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>Cancel actionPerformed()");
                    userCancel = true;
                    hideFrame();
                }
            });
            cancelButton.setPreferredSize(new Dimension(73, 26));
        }
        return cancelButton;
    }

    public JButton getRunButton() {
        if (runButton == null) {
            runButton = new JButton();
            runButton.setText(I18n.text("Run"));
            runButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ChecklistPanel.this.isChanged()) {
                        ChecklistPanel.this.checkForChecklistChangeForSave();
                    }

                    String system;
                    ImcMsgManager.getManager().start();
                    LinkedHashMap<ImcId16, SystemImcMsgCommInfo> list = ImcMsgManager.getManager().getCommInfo();

                    if (list.size() == 0) {
                        GuiUtils.errorMessage(ChecklistPanel.this, I18n.text("Comunications"), I18n.text("No System online"));
                        return;
                    }
                    SystemImcMsgCommInfo mv = (SystemImcMsgCommInfo) JOptionPane.showInputDialog(ChecklistPanel.this,
                            I18n.text("Choose one of the available systems"), I18n.text("Select System"), JOptionPane.QUESTION_MESSAGE,
                            new ImageIcon(), list.values().toArray(new SystemImcMsgCommInfo[0]), list.values()
                            .toArray(new SystemImcMsgCommInfo[0]));
                    system = mv.toString();

                    CheckListExe.showCheckListExeDialog(system, ChecklistPanel.this.getChecklistType().createCopy(),
                            SwingUtilities.getWindowAncestor(ChecklistPanel.this), new File(ChecklistPanel.this
                                    .getChecklistType().getOriginalFilePath()).getParent());

                    NeptusLog.pub().info("RunChecklist: "
                            + new File(ChecklistPanel.this.getChecklistType().getOriginalFilePath()).getParent());
                    NeptusLog.pub().info("RunChecklist: " + ChecklistPanel.this.getChecklistType().getOriginalFilePath());
                }
            });
            runButton.setPreferredSize(new Dimension(73, 26));
        }
        return runButton;
    }

    /**
     * This method initializes jTaskPaneGroup
     * 
     * @return com.l2fprod.common.swing.JTaskPaneGroup
     */
    private MyJTaskPaneGroup createTemplateTaskPaneGroup() {
        templateTaskPaneGroup = new MyJTaskPaneGroup();
        templateTaskPaneGroup.setIcon(OK_IMAGE_ICON);
        templateTaskPaneGroup.setScrollOnExpand(true);
        templateTaskPaneGroup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // NeptusLog.pub().info("<###>mouseClicked()" + e.isControlDown());
                MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                mtpg.requestFocusInWindow();
                if ((e.getButton() == MouseEvent.BUTTON3) & e.isControlDown()) {
                    // JPopupMenu jpm = getCheckGroupPopupMenu();
                    // jpm.setInvoker((Component) e.getSource());
                    // MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                    mtpg.setCollapsed(false);
                    editNameCheckGroupActionWorker(mtpg);
                }
                else if (e.getButton() == MouseEvent.BUTTON3) { // &
                    // e.getClickCount()
                    // == 1 )
                    // JPopupMenu jpm = getCheckGroupPopupMenu();
                    // jpm.setInvoker((Component) e.getSource());
                    // MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                    mtpg.setCollapsed(false);
                    getCheckGroupPopupMenu().show((Component) e.getSource(), e.getX(), e.getY());
                }
                else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
                    // MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                    // NeptusLog.pub().info("<###>mouseClicked(Button 1):" +
                    // e.getX()+":"+ e.getY());
                    // NeptusLog.pub().info("<###>wxh                   :" +
                    // mtpg.getWidth() + "|" + mtpg.getHeight());
                    if ((mtpg.getHeight() - e.getY()) < 20) {
                        // NeptusLog.pub().info("<###> "+true);
                        addNewCheckItemWorker(mtpg, false, null);
                    }
                    // mtpg.setBackground(Color.RED);
                }

                if (mtpg.isCollapsed()) {
                    if (mtpg == getSelectedGroup()) {
                        getAppendItemButton().setEnabled(false);
                        setSelectedGroup(null);
                    }
                }
                else {
                    getAppendItemButton().setEnabled(true);
                    setSelectedGroup(mtpg);
                }
            }
        });
        // templateTaskPaneGroup.addPropertyChangeListener(new
        // java.beans.PropertyChangeListener() {
        // public void propertyChange(java.beans.PropertyChangeEvent e) {
        // if ((e.getPropertyName().equals(JXTaskPane.EXPANDED_CHANGED_KEY)))
        // //"expanded"
        // {
        // //JTaskPaneGroup tpg = (JTaskPaneGroup) e.getSource();
        // if (ignoreExpandTaskGroup)
        // {
        // //NeptusLog.pub().info("<###> "+tpg.getText() + " propertyChange(expanded)");
        // return;
        // }
        // ignoreExpandTaskGroup = true;
        // //NeptusLog.pub().info("<###> "+tpg.getText() + " propertyChange(expanded)" +
        // e.getNewValue());
        // e.getNewValue();
        // ignoreExpandTaskGroup = false;
        // }
        // }
        // });
        templateTaskPaneGroup.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                // NeptusLog.pub().info("<###> "+e.getKeyCode() + " " + KeyEvent.VK_LEFT);
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                    if (!mtpg.isCollapsed())
                        mtpg.setCollapsed(true);
                }
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) e.getSource();
                    if (mtpg.isCollapsed())
                        mtpg.setCollapsed(false);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // NeptusLog.pub().info("<###>R");
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // NeptusLog.pub().info("<###>T");
            }

        });
        return templateTaskPaneGroup;
    }

    /**
     * @return the selectedGroup
     */
    private MyJTaskPaneGroup getSelectedGroup() {
        return selectedGroup;
    }

    /**
     * @param selectedGroup the selectedGroup to set
     */
    private void setSelectedGroup(MyJTaskPaneGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
        if (this.selectedGroup != null) {
            // String tl = this.selectedGroup.getTitle();
            String tl = this.selectedGroup.getGroupName();
            if (isFlat())
                getSelectedGroupLabel().setText("");
            else {
                if (tl.length() <= MAX_NUMBER_OF_SHOWN_CHARS)
                    getSelectedGroupLabel().setText("<html><b>" + tl);
                else
                    getSelectedGroupLabel().setText("<html><b>" + tl.substring(0, MAX_NUMBER_OF_SHOWN_CHARS) + "...");
            }
        }
        else
            getSelectedGroupLabel().setText("");
    }

    private void hideFrame() {
        if (jFrame != null) {
            jFrame.setVisible(false);
            jFrame.dispose();
        }
        if (jInternalFrame != null) {
            jInternalFrame.setVisible(false);
            jInternalFrame.doDefaultCloseAction();
            jInternalFrame.dispose();
        }
    }

    public ChecklistType getChecklistType() {
        if (userCancel)
            return checklist;

        // else
        ChecklistType newChecklist = new ChecklistType();
        newChecklist.setName(nameText.getText());
        newChecklist.setVersion(versionText.getText());
        newChecklist.setDescription(descTextArea.getText());
        newChecklist.setOriginalFilePath(originalFilePath);
        newChecklist.setFlat(isFlat);
        LinkedHashMap<String, LinkedList<CheckItem>> glists = newChecklist.getGroupList();
        if (isFlat) {
            LinkedList<CheckItem> lkl = new LinkedList<CheckItem>();
            if (getCheckTaskPane().getComponentCount() > 0) {
                MyJTaskPaneGroup jtpg = (MyJTaskPaneGroup) getCheckTaskPane().getComponents()[0];
                if (jtpg.getContentPane().getComponentCount() > 0) {
                    // System.err.println("Class" +
                    // jtpg.getContentPane().getComponents()[0].getClass());
                    /*
                     * CheckItemPanel[] compCheckItemPanel = (CheckItemPanel[]) jtpg.getComponents(); for (int j = 0; j
                     * < compCheckItemPanel.length; j++) { CheckItemPanel cip = compCheckItemPanel[j];
                     * lkl.add(cip.getCheckItem()); }
                     */
                    // MyJTaskPaneGroup jtpg = (MyJTaskPaneGroup)
                    // compJTaskPaneGroup[i];
                    // LinkedList lkl = new LinkedList();
                    if (!jtpg.listCheckItemPanel.isEmpty()) {
                        Iterator<Component> it = jtpg.listCheckItemPanel.iterator();
                        while (it.hasNext()) {
                            CheckItemPanel cip = (CheckItemPanel) it.next();
                            lkl.add(cip.getCheckItem());
                        }
                    }
                }
            }
            glists.put(ChecklistType.FLAT_ID, lkl);
        }
        else {
            if (getCheckTaskPane().getComponentCount() > 0) {
                // NeptusLog.pub().info("<###> "+getCheckTaskPane().getComponentCount());
                // Component[] ff = getCheckTaskPane().getComponents();
                Component[] compJTaskPaneGroup = getCheckTaskPane().getComponents();

                for (int i = 0; i < compJTaskPaneGroup.length; i++) {
                    MyJTaskPaneGroup jtpg = (MyJTaskPaneGroup) compJTaskPaneGroup[i];
                    // System.err.println("Class0" + jtpg.getComponentCount());
                    // System.err.println("Class" +
                    // jtpg.getContentPane().getComponentCount());
                    LinkedList<CheckItem> lkl = new LinkedList<CheckItem>();
                    if (!jtpg.listCheckItemPanel.isEmpty()) {
                        Iterator<Component> it = jtpg.listCheckItemPanel.iterator();
                        while (it.hasNext()) {
                            CheckItemPanel cip = (CheckItemPanel) it.next();
                            lkl.add(cip.getCheckItem());
                        }
                    }
                    // glists.put(jtpg.getTitle(), lkl); //deprecated in v0.2
                    // jtpg.getText();
                    glists.put(jtpg.getGroupName(), lkl);
                }
            }
        }
        return newChecklist;
    }

    /**
     * @param title
     * @param cl
     * @return
     */
    public static ChecklistPanel showChecklistPanel(String title, ChecklistType cl) {
        ChecklistPanel csp = new ChecklistPanel(cl);
        csp.getJFrame(title);
        // return csp.getChecklistType();
        return csp;
    }

    /**
     * @param cl
     */
    public static ChecklistPanel showChecklistPanel(ChecklistType cl) {
        return showChecklistPanel(null, cl);
    }

    /**
     * @param url
     */
    public static ChecklistPanel showChecklistPanel(String url) {
        ChecklistType cl = new ChecklistType(url);
        return showChecklistPanel(null, cl);
    }

    /**
     * This method initializes generatePDFButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getGeneratePDFButton() {
        if (generatePDFButton == null) {
            generatePDFButton = new JButton();
            generatePDFButton.setText(I18n.text("To PDF"));
            generatePDFButton.setPreferredSize(new java.awt.Dimension(73, 26));
            generatePDFButton.setEnabled(true);
            generatePDFButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>To PDF actionPerformed()");
                    boolean ret = false;
                    checkForChecklistChangeForSave();
                    if (isChanged()) {
                        // ret = save();
                        return;
                    }
                    else
                        ret = true;

                    if (!ret) {
                        JOptionPane.showMessageDialog(ChecklistPanel.this,
                                "<html>" + I18n.text("PDF <b>was not</b> created to file.") + "</html>");
                        return;
                    }

                    final String xml = originalFilePath;
                    File xmlFx = new File(xml);
                    xmlFx.getName();

                    String pdfFileName = createPDFFileName(xmlFx.getName());
                    final File pdfFx = new File(xmlFx.getParent(), pdfFileName);
                    final String pdf = pdfFx.getAbsolutePath();

                    Object[] optionsNew = { I18n.text("1 column"), I18n.text("2 column"), I18n.text("3 column") /* "Using XSL-FO" */};
                    final int choNew = JOptionPane.showOptionDialog(ChecklistPanel.this, I18n.text("Choose one to continue"),
                            I18n.text("Choose"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionsNew,
                            optionsNew[2]);

                    final int cho;
                    if (choNew == 3) {
                        Object[] options = { I18n.text("1 column"), I18n.text("2 column") };
                        cho = JOptionPane.showOptionDialog(ChecklistPanel.this, I18n.text("Choose one to continue"), I18n.text("Choose"),
                                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    }
                    else {
                        cho = 0;
                    }
                    final BlockingGlassPane bgp = new BlockingGlassPane();
                    if (jFrame != null)
                        ChecklistPanel.this.jFrame.setGlassPane(bgp);
                    if (jInternalFrame != null)
                        ChecklistPanel.this.jInternalFrame.setGlassPane(bgp);
                    bgp.block(true);
                    SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
                        @Override
                        protected Integer doInBackground() throws Exception {
                            if (choNew == 3) {
                                // FIXME As stylesheets estão hardcoded.
                                String xslt = ConfigFetch.resolvePathBasedOnConfigFile("conf/checklist-fo.xsl");
                                if (cho == 1)
                                    xslt = ConfigFetch.resolvePathBasedOnConfigFile("conf/checklist-fo-2col.xsl");
                                boolean ret = TransformFOP.convertXML2PDF(xml, xslt, pdf);
                                return ret ? 1 : 0;
                            }
                            else if (choNew == 0 || choNew == 1 || choNew == 2) {
                                boolean ret = GeneratorChecklistPDF.generateReport(checklist, pdfFx,
                                        (short) (choNew + 1));
                                return ret ? 1 : 0;
                            }
                            else {
                                return -1;
                            }
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            super.done();
                            try {
                                switch (get()) {
                                    case 1:
                                        JOptionPane.showMessageDialog(ChecklistPanel.this,
                                                I18n.textf("PDF created with success to file \"%file\".", pdf), I18n.text("PDF Creation"),
                                                JOptionPane.INFORMATION_MESSAGE);

                                        final String pdfF = pdf;
                                        new Thread() {
                                            @Override
                                            public void run() {
                                                openPDFInExternalViewer(pdfF);
                                            };
                                        }.start();
                                        break;
                                    case 0:
                                        JOptionPane.showMessageDialog(ChecklistPanel.this,
                                                "<html>" + I18n.text("PDF <b>was not</b> created to file.") + "</html>", I18n.text("PDF Creation"),
                                                JOptionPane.ERROR_MESSAGE);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            catch (Exception e) {
                                JOptionPane.showMessageDialog(ChecklistPanel.this,
                                        "<html>" + I18n.text("PDF <b>was not</b> created to file.") + "<br>" + I18n.textf("Error: %error", e.getMessage())
                                        + "</html>", I18n.text("PDF Creation"), JOptionPane.ERROR_MESSAGE);
                                e.printStackTrace();
                            }
                            bgp.block(false);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return generatePDFButton;
    }

    /**
     * @param pdf
     */
    protected void openPDFInExternalViewer(String pdf) {
        try {
            if (OsInfo.getName() == OsInfo.Name.WINDOWS) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", pdf});
            }
            else {
                String[] readers = { "xpdf", "kpdf", "FoxitReader", "evince", "acroread" };
                String reader = null;

                for (int count = 0; count < readers.length && reader == null; count++) {
                    if (Runtime.getRuntime().exec(new String[] { "which", readers[count] }).waitFor() == 0)
                        reader = readers[count];
                }
                if (reader == null)
                    throw new Exception("Could not find pdf reader");
                else
                    Runtime.getRuntime().exec(new String[] { reader, pdf });
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static String createPDFFileName(String name) {
        String ret = "", ext = "";
        int xmlInt = -1;
        // int xmlInt = name.lastIndexOf(".xml");
        ext = FileUtil.getFileExtension(name);
        if ("".equalsIgnoreCase(ext))
            xmlInt = -1;
        else
            xmlInt = name.lastIndexOf("." + ext);
        if (xmlInt == -1) {
            ret = name + ".pdf";
            return ret;
        }
        else {
            String sub = name.substring(0, xmlInt);
            ret = sub + ".pdf";
            return ret;
        }
    }

    /**
     * This method initializes saveAsButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getSaveAsButton() {
        if (saveAsButton == null) {
            saveAsButton = new JButton();
            saveAsButton.setText(I18n.text("Save as"));
            saveAsButton.setEnabled(true);
            saveAsButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // NeptusLog.pub().info("<###>Save as actionPerformed()");
                    saveAsFile();
                }
            });
            saveAsButton.setPreferredSize(new java.awt.Dimension(79, 26));
        }
        return saveAsButton;
    }

    /**
     * This method initializes checklistPopupMenu
     * 
     * @return javax.swing.JPopupMenu
     */
    private JPopupMenu getChecklistPopupMenu() {
        if (checklistPopupMenu == null) {
            checklistPopupMenu = new JPopupMenu();
            checklistPopupMenu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    // if (isVisible())
                    if (isFlat()) {
                        getMakeFlatChecklistCheckBoxMenuItem().setEnabled(false);
                        getMakeFlatChecklistCheckBoxMenuItem().setSelected(true);
                        getMakeFlatChecklistCheckBoxMenuItem().setEnabled(true);
                    }
                    else {
                        getMakeFlatChecklistCheckBoxMenuItem().setEnabled(false);
                        getMakeFlatChecklistCheckBoxMenuItem().setSelected(false);
                        getMakeFlatChecklistCheckBoxMenuItem().setEnabled(true);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            checklistPopupMenu.add(getAddChecklistMenuItem());
            checklistPopupMenu.add(getMakeFlatChecklistCheckBoxMenuItem());
            checklistPopupMenu.add(getUnCheckAllChecklistGroupsMenuItem());
        }
        return checklistPopupMenu;
    }

    /**
     * This method initializes jPopupMenu
     * 
     * @return javax.swing.JPopupMenu
     */
    private JPopupMenu getCheckGroupPopupMenu() {
        if (checkGroupPopupMenu == null) {
            checkGroupPopupMenu = new JPopupMenu();
            checkGroupPopupMenu.add(getEditNameCheckGroupMenuItem());
            checkGroupPopupMenu.add(getRemoveCheckGroupMenuItem());
            checkGroupPopupMenu.addSeparator();
            checkGroupPopupMenu.add(getAddCheckItemMenuItem());
            checkGroupPopupMenu.addSeparator();
            checkGroupPopupMenu.add(getMoveUpCheckGroupMenuItem());
            checkGroupPopupMenu.add(getMoveDownCheckGroupMenuItem());
            checkGroupPopupMenu.addSeparator();
            checkGroupPopupMenu.add(getInsertCheckGroupMenuItem());
            checkGroupPopupMenu.addSeparator();
            checkGroupPopupMenu.add(getUnCheckAllGroupItemsMenuItem());
            checkGroupPopupMenu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    // JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) e.getSource();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    JXTaskPaneContainer jtp = (JXTaskPaneContainer) mtpg.getParent();
                    // System.err.println("ZOrder: " +
                    // jtp.getComponentZOrder(mtpg));
                    if (jtp.getComponentCount() <= 1) {
                        getMoveUpCheckGroupMenuItem().setEnabled(false);
                        getMoveDownCheckGroupMenuItem().setEnabled(false);
                    }
                    else {
                        int zOrder = jtp.getComponentZOrder(mtpg);
                        int totalItems = jtp.getComponentCount();
                        if (zOrder == 0)
                            getMoveUpCheckGroupMenuItem().setEnabled(false);
                        else
                            getMoveUpCheckGroupMenuItem().setEnabled(true);
                        if (zOrder == (totalItems - 1))
                            getMoveDownCheckGroupMenuItem().setEnabled(false);
                        else
                            getMoveDownCheckGroupMenuItem().setEnabled(true);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
        return checkGroupPopupMenu;
    }

    /**
     * This method initializes addChecklistMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getAddChecklistMenuItem() {
        if (addChecklistMenuItem == null) {
            addChecklistMenuItem = new JMenuItem();
            addChecklistMenuItem.setText(I18n.text("Append New Group"));
            addChecklistMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    addNewChecklistGroupActionWorker(false, null);
                }
            });
        }
        return addChecklistMenuItem;
    }

    /**
     * @param insert Tels if the new ChecklistGroup is to be inserted.
     * @param insertTpg if insert==true tels the insert position.
     */
    private MyJTaskPaneGroup addNewChecklistGroupActionWorker(boolean insert, JXTaskPane insertTpg) {
        // dFIXME Verificar se a CL é flat ou não e se o user aceitar
        // setar a flag para false
        String inputValue = JOptionPane.showInputDialog(this, I18n.text("Please input a name for the new group"));
        if (inputValue != null && !inputValue.trim().equalsIgnoreCase("")) {
            inputValue = inputValue.trim();
            boolean notUnique = checkForCheckGroupExistence(inputValue);
            if (notUnique) {
                JOptionPane.showMessageDialog(this, I18n.text("Group already exists!"));
                return null;
            }
            MyJTaskPaneGroup tpg = createTemplateTaskPaneGroup(); // new
            // JTaskPaneGroup();
            // tpg.setTitle(inputValue); //deprecated tpg.setText(inputValue);
            tpg.setGroupName(inputValue);
            // tpg.setExpanded(false);
            tpg.setCollapsed(false);
            if (!insert)
                checkTaskPane.add(tpg);
            else {
                int zOrder = checkTaskPane.getComponentZOrder(insertTpg);
                checkTaskPane.add(tpg, zOrder);
            }
            fireChangeEvent(checkTaskPane);
            checkTaskPane.revalidate();
            checkTaskPane.repaint();
            makeItNotFlat();
            return tpg;
        }
        else {
            JOptionPane.showMessageDialog(this, I18n.text("Not a valid name!"));
            return null;
        }

    }

    /**
     * @param nameValue
     * @return
     */
    private boolean checkForCheckGroupExistence(String nameValue) {
        int totalGroups = checkTaskPane.getComponentCount();
        if (totalGroups == 0)
            return false;
        Component[] components = checkTaskPane.getComponents();
        MyJTaskPaneGroup testSubj = new MyJTaskPaneGroup();
        // testSubj.setTitle(nameValue);
        testSubj.setGroupName(nameValue);
        for (int i = 0; i < totalGroups; i++) {
            MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) components[i];
            if (testSubj.equals(mtpg))
                return true;
        }
        return false;
    }

    /**
     * This method initializes editNameCheckGroupMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditNameCheckGroupMenuItem() {
        if (editNameCheckGroupMenuItem == null) {
            editNameCheckGroupMenuItem = new JMenuItem();
            editNameCheckGroupMenuItem.setText(I18n.text("Edit Name (Ctrl + Right click)"));
            editNameCheckGroupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformedName()" +
                    // e.getSource().getClass());
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    // NeptusLog.pub().info("<###>()" +
                    // jmi.getParent().getClass());
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    // NeptusLog.pub().info("<###>(inv)" +
                    // jpm.getInvoker().getClass());
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    // NeptusLog.pub().info("<###>(name)" + mtpg.getTitle());
                    // //deprecated in v0.2 mtpg.setText(group);
                    editNameCheckGroupActionWorker(mtpg);
                }
            });
        }
        return editNameCheckGroupMenuItem;
    }

    /**
     * @return
     */
    private boolean editNameCheckGroupActionWorker(MyJTaskPaneGroup mtpg) {
        // String inputValue = JOptionPane.showInputDialog(mtpg,
        // "Please input a new name for \"" + mtpg.getTitle() + "\"",
        // mtpg.getTitle());
        String inputValue = JOptionPane.showInputDialog(this,
                I18n.textf("Please input a new name for \"%group\"", mtpg.getGroupName()), mtpg.getGroupName());

        // NeptusLog.pub().info("<###>(new name)" + inputValue);
        if (inputValue != null && !inputValue.trim().equalsIgnoreCase("")) {
            // dTODO verificar se nome já existe pq isto é um ID
            boolean notUnique = checkForCheckGroupExistence(inputValue);
            if (notUnique) {
                JOptionPane.showMessageDialog(this, I18n.text("Group already exists!"));
                return false;
            }

            // mtpg.setTitle(inputValue.trim()); // deprecated in v0.2
            // mtpg.setText(group);
            mtpg.setGroupName(inputValue.trim());
            fireChangeEvent(mtpg);
            checkTaskPane.revalidate();
            checkTaskPane.repaint();
            return true;
        }
        return false;
    }

    /**
     * This method initializes addCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getAddCheckItemMenuItem() {
        if (addCheckItemMenuItem == null) {
            addCheckItemMenuItem = new JMenuItem();
            addCheckItemMenuItem.setText(I18n.text("Append New Item (Double Click on the Bottom of the Group)"));
            addCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformedAdd()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    addNewCheckItemWorker(mtpg, false, null);
                }
            });
        }
        return addCheckItemMenuItem;
    }

    /**
     * @param mtpg The ChecklistGroup to add the ChecklistItem.
     * @param insert Tels if the new ChecklistItem is to be inserted or appended.
     * @param insertCip if insert==true tels the insert position.
     */
    private void addNewCheckItemWorker(MyJTaskPaneGroup mtpg, boolean insert, CheckItemPanel insertCip) {
        String inputValue = JOptionPane.showInputDialog(this, I18n.text("Please input the item name"));
        if (inputValue != null && !inputValue.equalsIgnoreCase("")) {
            // FIXME verificar se já existe já q é um ID
            inputValue = inputValue.trim();
            boolean notUnique = checkForCheckItemExistence(inputValue, mtpg);
            if (notUnique) {
                JOptionPane.showMessageDialog(this, I18n.text("Item already exists!"));
                return;
            }
            CheckItem cit = new CheckItem();
            cit.setName(inputValue);
            CheckItemPanel cip = new CheckItemPanel(cit);
            // cip.addMouseListener(new CheckItemMouseAdapter());
            addCheckItemMouseAdapter(cip);
            if (!insert)
                mtpg.add(cip);
            else {
                int zOrder = mtpg.getContentPane().getComponentZOrder(insertCip);
                mtpg.add(cip, zOrder);
            }
            fireChangeEvent(cip);
            mtpg.revalidate();
            mtpg.repaint();
        }
        else {
            JOptionPane.showMessageDialog(this, I18n.text("Item name not valid!"));
            return;
        }
    }

    /**
     * @param nameItemValue
     * @param mtpg
     * @return
     */
    private boolean checkForCheckItemExistence(String nameItemValue, MyJTaskPaneGroup mtpg) {
        int totalItems = mtpg.getContentPane().getComponentCount();
        if (totalItems == 0)
            return false;
        Component[] components = mtpg.getContentPane().getComponents();
        CheckItemPanel testSubj = new CheckItemPanel();
        testSubj.setName(nameItemValue);
        for (int i = 0; i < totalItems; i++) {
            CheckItemPanel cip = (CheckItemPanel) components[i];
            if (testSubj.equals(cip))
                return true;
        }
        return false;
    }

    /**
     * This method initializes removeCheckGroupMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getRemoveCheckGroupMenuItem() {
        if (removeCheckGroupMenuItem == null) {
            removeCheckGroupMenuItem = new JMenuItem();
            removeCheckGroupMenuItem.setText(I18n.text("Remove Group"));
            removeCheckGroupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformedRem()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    // System.err.println(mtpg.getComponentAt(jmi.getX(),
                    // jmi.getY()).getClass());
                    // int option =
                    // JOptionPane.showConfirmDialog(checkTaskPane,"<html>Are you sure you want to <b>delete</b><br>"
                    // +
                    // "<i>" + mtpg.getTitle() +
                    // "</i> group and all it's content?<html>");
                    int option = JOptionPane.showConfirmDialog(checkTaskPane, "<html>" 
                            + I18n.textf("Are you sure you want to <b>delete</b><br><i>%group</i> group and all it's content?", mtpg.getGroupName()) 
                            + "<html>");
                    if (option == JOptionPane.YES_OPTION) {
                        JXTaskPaneContainer jtp = (JXTaskPaneContainer) mtpg.getParent();
                        jtp.remove(mtpg);
                        fireChangeEvent(mtpg);
                        jtp.revalidate();
                        jtp.repaint();
                    }
                }
            });
            removeCheckGroupMenuItem.setEnabled(true);
        }
        return removeCheckGroupMenuItem;
    }

    /**
     * This method initializes makeFlatChecklistCheckBoxMenuItem
     * 
     * @return javax.swing.JCheckBoxMenuItem
     */
    private JCheckBoxMenuItem getMakeFlatChecklistCheckBoxMenuItem() {
        if (makeFlatChecklistCheckBoxMenuItem == null) {
            makeFlatChecklistCheckBoxMenuItem = new JCheckBoxMenuItem();
            makeFlatChecklistCheckBoxMenuItem.setText(FLAT_TITLE);
            makeFlatChecklistCheckBoxMenuItem.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    // NeptusLog.pub().info("<###>itemStateChanged()");
                    JCheckBoxMenuItem jmi = (JCheckBoxMenuItem) e.getSource();
                    // JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    // MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup)
                    // jpm.getInvoker();
                    if (jmi.isSelected()) {
                        if (makeItFlat()) {
                            if (jmi.isEnabled()) {
                                // System.err.println("111");
                                fireChangeEvent(checkTaskPane);
                            }
                        }
                        else {
                            JOptionPane.showMessageDialog(checkTaskPane, "<html>"
                                    + I18n.text("<b>Was not</b> possible to flatten this checklist!")
                                    + "</html>",
                                    I18n.text("Flatten Checklist"), JOptionPane.ERROR_MESSAGE);
                        }
                        // System.err.println("Flat" + makeItFlat());
                    }
                    else {
                        if (makeItNotFlat()) {
                            if (jmi.isEnabled()) {
                                // System.err.println("222");
                                fireChangeEvent(checkTaskPane);
                            }
                        }
                        else {
                            JOptionPane.showMessageDialog(checkTaskPane, "<html>"
                                    + I18n.text("<b>Was not</b> possible to deflatten this checklist!") + "</html>",
                                    I18n.text("Deflatten Checklist"), JOptionPane.ERROR_MESSAGE);
                        }
                        // System.err.println("NotFlat" +
                        // makeItNotFlat());
                    }
                }
            });
        }
        return makeFlatChecklistCheckBoxMenuItem;
    }

    /**
     * This method initializes checkItemPopupMenu
     * 
     * @return javax.swing.JPopupMenu
     */
    private JPopupMenu getCheckItemPopupMenu() {
        if (checkItemPopupMenu == null) {
            checkItemPopupMenu = new JPopupMenu();
            checkItemPopupMenu.add(getEditNameCheckItemMenuItem());
            checkItemPopupMenu.add(getEditDateCheckItemMenuItem());
            checkItemPopupMenu.add(getRemoveCheckItemMenuItem());
            checkItemPopupMenu.addSeparator();
            checkItemPopupMenu.add(getMoveUpCheckItemMenuItem());
            checkItemPopupMenu.add(getMoveDownCheckItemMenuItem());
            checkItemPopupMenu.addSeparator();
            checkItemPopupMenu.add(getInsertCheckItemMenuItem());
            checkItemPopupMenu.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                    // JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) e.getSource();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();

                    // System.err.println("ZOrder: " +
                    // jtp.getComponentZOrder(mtpg));
                    if (mtpg.getContentPane().getComponentCount() <= 1) {
                        getMoveUpCheckItemMenuItem().setEnabled(false);
                        getMoveDownCheckItemMenuItem().setEnabled(false);
                    }
                    else {
                        int zOrder = mtpg.getContentPane().getComponentZOrder(cip);
                        int totalItems = mtpg.getContentPane().getComponentCount();
                        if (zOrder == 0)
                            getMoveUpCheckItemMenuItem().setEnabled(false);
                        else
                            getMoveUpCheckItemMenuItem().setEnabled(true);
                        if (zOrder == (totalItems - 1))
                            getMoveDownCheckItemMenuItem().setEnabled(false);
                        else
                            getMoveDownCheckItemMenuItem().setEnabled(true);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                }
            });
        }
        return checkItemPopupMenu;
    }

    /**
     * This method initializes removeCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getRemoveCheckItemMenuItem() {
        if (removeCheckItemMenuItem == null) {
            removeCheckItemMenuItem = new JMenuItem();
            removeCheckItemMenuItem.setText(I18n.text("Remove Item"));
            removeCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();
                    mtpg.remove(cip);
                    fireChangeEvent(cip);
                    mtpg.revalidate();
                    mtpg.repaint();
                }
            });
        }
        return removeCheckItemMenuItem;
    }

    /**
     * This adds the MouseAdapter to the CheckItem.
     * 
     * @param cip
     */
    private void addCheckItemMouseAdapter(CheckItemPanel cip) {
        cip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // NeptusLog.pub().info("<###>mouseClickedItemCI()" +
                // e.isControlDown());
                // NeptusLog.pub().info("<###>Source"
                // + ((Component) e.getSource()).getParent().getParent()
                // .getParent().getParent().getClass());
                // MyJTaskPaneGroup mjtpg = CheckItemPanel
                // .getMyJTaskPaneGroupFromItem((Component) e.getSource());
                CheckItemPanel cip = (CheckItemPanel) e.getSource();
                cip.requestFocusInWindow();
                setSelectedGroup(CheckItemPanel.getMyJTaskPaneGroupFromItem(cip));
                if (e.getButton() == MouseEvent.BUTTON3) {
                    getCheckItemPopupMenu().show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * This method initializes moveUpCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getMoveUpCheckItemMenuItem() {
        if (moveUpCheckItemMenuItem == null) {
            moveUpCheckItemMenuItem = new JMenuItem();
            moveUpCheckItemMenuItem.setText(I18n.text("Move Item Up"));
            moveUpCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();
                    // JTaskPane jtp = (JTaskPane) mtpg.getParent();
                    int zOrder = mtpg.getContentPane().getComponentZOrder(cip);
                    // int totalItems =
                    // mtpg.getContentPane().getComponentCount();
                    mtpg.remove(cip);
                    mtpg.add(cip, zOrder - 1);
                    fireChangeEvent(cip);
                    mtpg.revalidate();
                    mtpg.repaint();
                }
            });
        }
        return moveUpCheckItemMenuItem;
    }

    /**
     * This method initializes moveDownCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getMoveDownCheckItemMenuItem() {
        if (moveDownCheckItemMenuItem == null) {
            moveDownCheckItemMenuItem = new JMenuItem();
            moveDownCheckItemMenuItem.setText(I18n.text("Move Item Down"));
            moveDownCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();
                    // JTaskPane jtp = (JTaskPane) mtpg.getParent();
                    int zOrder = mtpg.getContentPane().getComponentZOrder(cip);
                    // int totalItems =
                    // mtpg.getContentPane().getComponentCount();
                    mtpg.remove(cip);
                    mtpg.add(cip, zOrder + 1);
                    fireChangeEvent(cip);
                    mtpg.revalidate();
                    mtpg.repaint();
                }
            });
        }
        return moveDownCheckItemMenuItem;
    }

    /**
     * This method initializes moveUpCheckGroupMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getMoveUpCheckGroupMenuItem() {
        if (moveUpCheckGroupMenuItem == null) {
            moveUpCheckGroupMenuItem = new JMenuItem();
            moveUpCheckGroupMenuItem.setText(I18n.text("Move Group Up"));
            moveUpCheckGroupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    JXTaskPaneContainer jtp = (JXTaskPaneContainer) mtpg.getParent();
                    int zOrder = jtp.getComponentZOrder(mtpg);
                    // int totalItems = jtp.getComponentCount();
                    jtp.remove(mtpg);
                    jtp.add(mtpg, zOrder - 1);
                    fireChangeEvent(mtpg);
                    jtp.revalidate();
                    jtp.repaint();
                }
            });
        }
        return moveUpCheckGroupMenuItem;
    }

    /**
     * This method initializes moveDownCheckGroupMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getMoveDownCheckGroupMenuItem() {
        if (moveDownCheckGroupMenuItem == null) {
            moveDownCheckGroupMenuItem = new JMenuItem();
            moveDownCheckGroupMenuItem.setText(I18n.text("Move Group Down"));
            moveDownCheckGroupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    JXTaskPaneContainer jtp = (JXTaskPaneContainer) mtpg.getParent();
                    int zOrder = jtp.getComponentZOrder(mtpg);
                    // int totalItems = jtp.getComponentCount();
                    jtp.remove(mtpg);
                    jtp.add(mtpg, zOrder + 1);
                    fireChangeEvent(mtpg);
                    jtp.revalidate();
                    jtp.repaint();
                }
            });
        }
        return moveDownCheckGroupMenuItem;
    }

    /**
     * This method initializes insertCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getInsertCheckItemMenuItem() {
        if (insertCheckItemMenuItem == null) {
            insertCheckItemMenuItem = new JMenuItem();
            insertCheckItemMenuItem.setText(I18n.text("Insert New Item Before"));
            insertCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();
                    // JTaskPane jtp = (JTaskPane) mtpg.getParent();
                    addNewCheckItemWorker(mtpg, true, cip);
                }
            });
        }
        return insertCheckItemMenuItem;
    }

    /**
     * This method initializes insertCheckGroupMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getInsertCheckGroupMenuItem() {
        if (insertCheckGroupMenuItem == null) {
            insertCheckGroupMenuItem = new JMenuItem();
            insertCheckGroupMenuItem.setText(I18n.text("Insert New Group Before"));
            insertCheckGroupMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    addNewChecklistGroupActionWorker(true, mtpg);
                }
            });
        }
        return insertCheckGroupMenuItem;
    }

    /**
     * This method initializes editNameCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditNameCheckItemMenuItem() {
        if (editNameCheckItemMenuItem == null) {
            editNameCheckItemMenuItem = new JMenuItem();
            editNameCheckItemMenuItem.setText(I18n.text("Edit Item Name"));
            editNameCheckItemMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    CheckItemPanel cip = (CheckItemPanel) jpm.getInvoker();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) cip.getParentGroup();
                    editNameCheckItemActionWorker(cip, mtpg);
                }
            });
        }
        return editNameCheckItemMenuItem;
    }

    /**
     * @param cip
     * @param mtpg
     */
    private boolean editNameCheckItemActionWorker(CheckItemPanel cip, MyJTaskPaneGroup mtpg) {
        String inputValue = JOptionPane.showInputDialog(this, I18n.textf("Please input a new name for \"%name\"", cip.getName()),
                cip.getName());
        // NeptusLog.pub().info("<###>(new name)" + inputValue);
        if (inputValue != null && !inputValue.trim().equalsIgnoreCase("")) {
            // dTODO verificar se nome já existe pq isto é um ID
            boolean notUnique = checkForCheckItemExistence(inputValue, mtpg);
            if (notUnique) {
                JOptionPane.showMessageDialog(this, I18n.text("Item already exists!"));
                return false;
            }

            cip.setName(inputValue.trim());
            fireChangeEvent(cip);
            cip.revalidate();
            cip.repaint();
            return true;
        }
        return false;
    }

    /**
     * This method initializes editDateCheckItemMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditDateCheckItemMenuItem() {
        if (editDateCheckItemMenuItem == null) {
            editDateCheckItemMenuItem = new JMenuItem();
            editDateCheckItemMenuItem.setEnabled(false);
            editDateCheckItemMenuItem.setText(I18n.text("Edit Item Check Date"));
        }
        return editDateCheckItemMenuItem;
    }

    /**
     * @return the unCheckAllChecklistGroupsMenuItem
     */
    private JMenuItem getUnCheckAllChecklistGroupsMenuItem() {
        if (unCheckAllChecklistGroupsMenuItem == null) {
            unCheckAllChecklistGroupsMenuItem = new JMenuItem();
            unCheckAllChecklistGroupsMenuItem.setText(I18n.text("Uncheck All Checklist"));
            unCheckAllChecklistGroupsMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    // JMenuItem jmi = (JMenuItem) e.getSource();
                    // JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    // MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup)
                    // jpm.getInvoker();

                    Component[] compJTaskPaneGroup = getCheckTaskPane().getComponents();
                    for (Component comp : compJTaskPaneGroup) {
                        MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) comp;
                        // boolean runned = false;
                        for (Component cp : mtpg.listCheckItemPanel) {
                            CheckItemPanel cip = (CheckItemPanel) cp;
                            cip.setChecked(false);
                            // TODO rjpg uncheck the Actions checks
                            // runned = true;
                        }
                        // if (runned)
                        // mtpg.firePropertyChange(propertyName,
                        // oldValue, newValue)
                    }
                }
            });
        }
        return unCheckAllChecklistGroupsMenuItem;
    }

    /**
     * @return the unCheckAllGroupItemsMenuItem
     */
    private JMenuItem getUnCheckAllGroupItemsMenuItem() {
        if (unCheckAllGroupItemsMenuItem == null) {
            unCheckAllGroupItemsMenuItem = new JMenuItem();
            unCheckAllGroupItemsMenuItem.setText(I18n.text("Uncheck All Group"));
            unCheckAllGroupItemsMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    JMenuItem jmi = (JMenuItem) e.getSource();
                    JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                    MyJTaskPaneGroup mtpg = (MyJTaskPaneGroup) jpm.getInvoker();
                    for (Component cp : mtpg.listCheckItemPanel) {
                        CheckItemPanel cip = (CheckItemPanel) cp;
                        cip.setChecked(false);
                    }
                }
            });
        }
        return unCheckAllGroupItemsMenuItem;
    }

    /**
     * @return the editPanel
     */
    private JPanel getEditPanel() {
        if (editPanel == null) {
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(java.awt.FlowLayout.RIGHT);
            editPanel = new JPanel();
            editPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 5, 2, 5));
            editPanel.setLayout(flowLayout);
            editPanel.setSize(8, 8);
            editPanel.add(getSelectedGroupLabel(), null);
            editPanel.add(getAppendGroupButton(), null);
            editPanel.add(getAppendItemButton(), null);
        }
        return editPanel;
    }

    /**
     * @return the selectedGroupLabel
     */
    private JLabel getSelectedGroupLabel() {
        if (selectedGroupLabel == null) {
            selectedGroupLabel = new JLabel();
            selectedGroupLabel.setForeground(BLUE_1.darker().darker().darker());
        }
        return selectedGroupLabel;
    }

    /**
     * @return the addGroupButton
     */
    private JButton getAppendGroupButton() {
        if (appendGroupButton == null) {
            appendGroupButton = new JButton();
            appendGroupButton.setText(I18n.text("append group"));
            appendGroupButton.setToolTipText(I18n.text("Append group"));
            appendGroupButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>appendGroupButton actionPerformed()");
                    addNewChecklistGroupActionWorker(false, null);
                }
            });
            appendGroupButton.setPreferredSize(new Dimension(93 + 20, 21));
            appendGroupButton.setBackground(BLUE_1);
        }
        return appendGroupButton;
    }

    /**
     * @return the addItemButton
     */
    private JButton getAppendItemButton() {
        if (appendItemButton == null) {
            appendItemButton = new JButton();
            appendItemButton.setText(I18n.text("append item"));
            appendItemButton.setToolTipText(I18n.text("Append item"));
            appendItemButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>appendItemButton actionPerformed()");
                    if (getSelectedGroup() != null)
                        addNewCheckItemWorker(getSelectedGroup(), false, null);
                    else
                        getAppendItemButton().setEnabled(false);
                }

            });
            appendItemButton.setPreferredSize(new java.awt.Dimension(93 + 20, 21));
            appendItemButton.setBackground(BLUE_1);
            appendItemButton.setEnabled(false);
            appendItemButton.setVisible(true);
        }
        return appendItemButton;
    }

    private void changeChecklist(ChecklistType clist) {
        checklist = clist;
        isFlat = checklist.isFlat();
        originalFilePath = checklist.getOriginalFilePath();

        if (originalFilePath.length() < 1)
            setChanged(true);
        else
            setChanged(false);

        nameText.setText(checklist.getName());
        versionText.setText(checklist.getVersion());
        getDescTextArea().setText(checklist.getDescription());

        checkTaskPane = null;
        getCheckTaskPaneScrollPane().setViewportView(getCheckTaskPane());

        if (jFrame != null) {
            jFrame.setTitle("Checklist " + getName());
        }

        setChanged(false);
        getSaveButton().setEnabled(false);
    }

    /**
     * The main to run as a separated application
     * 
     * @param args
     */

    private static String ip = "127.0.0.1";
    private static int port = 6001;

    public static JDialog getIPDialog(Window w) {
        final JDialog jd = new JDialog(w);
        jd.setModalityType(ModalityType.DOCUMENT_MODAL);
        jd.setTitle(I18n.text("IP Port selection"));

        jd.setSize(200, 95);
        jd.setResizable(false);

        JLabel labelIP = new JLabel(I18n.text("IP"));
        labelIP.setHorizontalTextPosition(JLabel.RIGHT);

        JLabel labelPort = new JLabel(I18n.text("Port"));
        labelPort.setHorizontalTextPosition(JLabel.RIGHT);

        final JTextField textIP = new JTextField();
        textIP.setText(ip);
        final JFormattedTextField textPort = new JFormattedTextField(GuiUtils.getNeptusIntegerFormat() /*NumberFormat.getInstance()*/);
        textPort.setValue(port);

        JButton buttonOK = new JButton();
        buttonOK.setText(I18n.text("OK"));

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ip = textIP.getText();
                port = ((Number) textPort.getValue()).intValue();
                jd.dispose();
            }
        });

        JPanel panelaux = new JPanel();
        panelaux.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2));

        panel.add(labelPort);
        panel.add(textPort);

        panelaux.add(panel, BorderLayout.CENTER);
        panelaux.add(buttonOK, BorderLayout.SOUTH);

        jd.setContentPane(panelaux);
        return jd;
    }

    @SuppressWarnings("static-access")
    private static Options getCommandLineOptions() {
        // create the Options
        Options options = new Options();
        options.addOption("h", I18n.text("help"), false, I18n.text("this help"));

        // options.addOption(OptionBuilder.withLongOpt("verbose")
        // .withDescription("Verbosity level [off,fatal,warn,\"info\",debug]").withValueSeparator('=')
        // .hasOptionalArg().create("v"));

        options.addOption(Option.builder("f").longOpt("checklist").desc(I18n.text("checklist file")).argName("file")
                .valueSeparator('=').hasArg().build());

        options.addOption("g", "generate-pdf", false, I18n.text("generate pdf and don't open the interface"));

        options.addOption(Option.builder("o").longOpt("output-pdf").desc(I18n.text("output pdf file"))
                .argName("file").valueSeparator('=').hasArg().build());

        options.addOption(Option.builder("c").longOpt("pdf-columns")
                .desc(I18n.text("generate pdf with n column (1, 2, or 3) defaults to 1")).argName("n")
                .valueSeparator('=').hasArg().build());

        return options;
    }

    /**
     * @param options
     */
    private static void printUsage(Options options) {
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        // formatter.printHelp(NeptusLeaves.class.getCanonicalName(), options);
        formatter.printHelp("java -jar neptus-check.jar", "Neptus Checklist v" + ConfigFetch.getVersionSimpleString()
                + "\nCopyright (c) 2004-2021 Universidade do Porto - LSTS. All rights reserved.\n\n"
                + I18n.text("Options:") + "\n",
                options, I18n.textf("Report bugs to %email", "Paulo Dias <pdias@fe.up.pt>"), true);
    }

    public static void main(String... args) {
        GeneralPreferences.initialize();
        try {
            // create the command line parser
            CommandLineParser parser = new PosixParser();
            Options options = getCommandLineOptions();
            // parse the command line arguments
            CommandLine line = null;
            try {
                line = parser.parse(options, args);
            }
            catch (ParseException e) {
                // e.printStackTrace();
            }
            
            if (line == null || line.hasOption("help")) {
                printUsage(options);
                System.exit(0);
            }
            
            OutputMonitor.setDisable(true);
            NeptusLog.wasteRoot().setLevel(Level.OFF);
            NeptusLog.pubRoot().setLevel(Level.FATAL);
            ConfigFetch.initialize();
            // OutputMonitor.end();
            // BasicConfigurator.resetConfiguration();
            // NeptusLog.INSTANCE.logWaste.setLevel(Level.OFF);
            // NeptusLog.setLevel(Level.FATAL);
            if (OsInfo.getName() == OsInfo.Name.LINUX)
                GuiUtils.setLookAndFeel();
            else
                GuiUtils.setSystemLookAndFeel();
            // GuiUtils.setLookAndFeelNimbus();
            
            // ChecklistPanel clp = showChecklistPanel("checklists/check3.nchk");
            
            ChecklistPanel clp = null;
            if (args.length < 1) {
                ChecklistType ck = new ChecklistType();
                ck.setName(I18n.text("New Checklist"));
                clp = showChecklistPanel(I18n.text("New Checklist"), ck);
            }
            else {
                // String path = args[0];
                // File fx = new File(path);
                // if (!fx.getAbsoluteFile().exists()) {
                // ChecklistType ck = new ChecklistType();
                // ck.setName("New Checklist");
                // clp = showChecklistPanel("New Checklist", ck);
                // } else {
                // clp = showChecklistPanel(fx.getAbsolutePath());
                // }
                
                String path = null;
                String out = null;
                boolean genPDF = false;
                int columns = 1;
                if (line.hasOption("checklist")) {
                    path = line.getOptionValue("checklist");
                }
                if (line.hasOption("generate-pdf")) {
                    try {
                        genPDF = true;
                    }
                    catch (Exception e) {
                    }
                    if (!line.hasOption("checklist")) {
                        printUsage(options);
                        System.exit(0);
                    }
                    if (line.hasOption("pdf-columns")) {
                        try {
                            columns = Integer.parseInt(line.getOptionValue("pdf-columns"));
                            columns = Math.min(Math.max(columns, 1), 3);
                        }
                        catch (Exception e) {
                        }
                    }
                    if (line.hasOption("output-pdf")) {
                        try {
                            out = line.getOptionValue("output-pdf");
                        }
                        catch (Exception e) {
                            out = null;
                        }
                    }
                }
                
                File fx = new File(path);
                if (!genPDF) {
                    if (!fx.getAbsoluteFile().exists()) {
                        ChecklistType ck = new ChecklistType();
                        ck.setName(I18n.text("New Checklist"));
                        clp = showChecklistPanel(I18n.text("New Checklist"), ck);
                    }
                    else {
                        clp = showChecklistPanel(fx.getAbsolutePath());
                    }
                }
                else {
                    // Gen PDF
                    if (!fx.getAbsoluteFile().exists()) {
                        NeptusLog.pub().info("Checklist file not found!");
                        System.exit(1);
                    }
                    try {
                        ChecklistType checklist = new ChecklistType(fx.getAbsolutePath());
                        String pdfFileName = createPDFFileName(fx.getName());
                        if (out != null) {
                            pdfFileName = out;
                            new File(pdfFileName).getParentFile().mkdirs();
                        }
                        File pdfFx = new File(fx.getParent(), pdfFileName);
                        boolean ret = GeneratorChecklistPDF.generateReport(checklist, pdfFx, (short) columns);
                        System.exit(ret ? 0 : 1);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
            
            clp.setEditable(true);
            clp.jFrame.setIconImages(ConfigFetch.getIconImagesForFrames());
            
            clp.jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            clp.jFrame.setSize(790, 580);
            GuiUtils.centerOnScreen(clp.jFrame);
            clp.getCancelButton().setVisible(false);
            clp.getRunButton().setVisible(false);
            clp.getNewButton().setVisible(true);
            clp.getOpenButton().setVisible(true);
            clp.getOpenButton().requestFocusInWindow();
            
            clp.getEditPanel().add(new JLabel(" | \u00A9 From Neptus " + ConfigFetch.getVersionSimpleString()));
            clp.makeNameEditable();
            clp.iconifyToolButtons();
            clp.jFrame.setAlwaysOnTop(false);
            // clp.addPropertyChangeListener(new PropertyChangeListener() {
            // String cur = clp.getName();
            // @Override
            // public void propertyChange(PropertyChangeEvent evt) {
            // if (!cur.equals(clp.getName())) {
            // clp.jFrame.setTitle("Checklist " + clp.getName());
            // cur = clp.getName();
            // }
            // }
            // });
        }
        catch (Exception | Error e) {
            e.printStackTrace();
        }
    }
}
