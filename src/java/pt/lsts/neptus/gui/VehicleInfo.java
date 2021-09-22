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
 * 2005/03/10
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import org.dom4j.Document;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.renderer3d.Object3DCreationHelper;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.FTPArgs;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.comm.protocol.ProtocolArgs;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.misc.FileType;
import pt.lsts.neptus.types.mission.VehicleMission;
import pt.lsts.neptus.types.vehicle.TemplateFileVehicle;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.JTreeUtils;
import pt.lsts.neptus.util.editors.EditorLauncher;

/**
 * @author Paulo Dias
 * 
 */
public class VehicleInfo extends JPanel implements PropertiesProvider {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private VehicleType vehicle = null;

    private JPanel jContentPane = null;
    private JDialog vehicleInfoDialog = null;

    private JPanel genInfoPanel = null;
    private JPanel idPanel = null;
    private JLabel idLabel = null;
    private JLabel idValue = null;
    private JPanel namePanel = null;
    private JLabel nameLabel = null;
    private JLabel nameValue = null;
    private JPanel typePanel = null;
    private JLabel typeLabel = null;
    private JLabel typeValue = null;
    private ImagePanel imagePanel = null;
    private JPanel ginfoPanel = null;
    private JTree filesTree = null;
    // private DefaultTreeModel treeModel= null;
    private DefaultMutableTreeNode root = null;

    private JPanel fileInfoPanel = null;
    private JPanel filesPanel = null;
    private JScrollPane jScrollPane = null;
    private JLabel fxTypeLabel = null;
    private JLabel fxTypeValue = null;
    private JLabel fxHrefLabel = null;
    private JLabel fxDescLabel = null;
    private JTextArea fxDescTextArea = null;
    private JTextField fxHrefTextField = null;
    private JButton fxEditButton = null;
    private JLabel dimLabel = null;
    private JTextField dimTextField = null;
    private JScrollPane fxDescScrollPane = null;
    private CoordinateSystemPanel coordinateSystemPanel = null;
    private JPanel attitudePanel = null;

    private CoordinateSystem coordinateSystem = null;

    private boolean editableInfo = true;
    private boolean editableVehicleRef = false;

    private JButton okButton = null;
    private JPanel okPanel = null;
    private JButton cancelButton = null;
    private JPanel cardsPanel = null;
    private JPanel card1 = null;
    private JPanel card2 = null;
    private JScrollPane jScrollPane1 = null;
    private JTextArea fxDescTextArea2 = null;
    private JTextField fxParamXsltTextField = null;
    private JTextField fxOutputFileTextField = null;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;

    private JButton viewer3DButton = null;

    private JButton propertiesButton = null;

    private JButton openConsolesButton = null;

    private JPanel modelPanel = null;

    private JLabel modelLabel = null;

    private JLabel modelValue = null;

    /**
     * 
     */
    public VehicleInfo(VehicleType vehicle) {
        super();
        this.vehicle = vehicle;
        initialize();
    }

    /**
     * @param ve
     * @param cs
     */
    public VehicleInfo(VehicleType vehicle, CoordinateSystem cs) {
        super();
        this.vehicle = vehicle;
        this.coordinateSystem = cs;
        initialize();
    }

    /**
     * @param vehicle
     * @param cs
     * @param editInfo
     * @param editCS
     */
    public VehicleInfo(VehicleType vehicle, CoordinateSystem cs, boolean editInfo, boolean editCS) {
        super();
        this.vehicle = vehicle;
        this.coordinateSystem = cs;
        initialize();
        setInfoEditable(editInfo);
        setVehicleRefEditable(editCS);
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
        }
        return jContentPane;
    }

    /**
     * This method initializes jDialog
     * 
     * @return javax.swing.JDialog
     */
    private JDialog getVehicleInfoDialog() {
        if (vehicleInfoDialog == null) {
            vehicleInfoDialog = new JDialog();
            vehicleInfoDialog.setContentPane(getJContentPane());
            vehicleInfoDialog.setTitle("System Info");
            vehicleInfoDialog.setResizable(false);
            vehicleInfoDialog.setSize(410, 550);
            // vehicleInfoDialog.setMinimumSize(new java.awt.Dimension(350,34));
            // vehicleInfoDialog.setPreferredSize(new java.awt.Dimension(350,34));
            // vehicleInfoDialog.setPreferredSize(new Dimension(this.getWidth() + 5, this.getHeight() + 40));
            vehicleInfoDialog.setLayout(new BorderLayout());
            vehicleInfoDialog.getContentPane().add(this, BorderLayout.CENTER);
            vehicleInfoDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            // vehicleInfoDialog.setModal(true);
            vehicleInfoDialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            vehicleInfoDialog.setAlwaysOnTop(true);
            GuiUtils.centerOnScreen(vehicleInfoDialog);
            vehicleInfoDialog.setVisible(true);
            vehicleInfoDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    closeFrame();
                }
            });
            return vehicleInfoDialog;
        }
        return vehicleInfoDialog;
    }

    public void setInfoEditable(boolean value) {
        this.editableInfo = value;
    }

    public void setVehicleRefEditable(boolean value) {
        this.editableVehicleRef = value;
        if (!editableVehicleRef) {
            getCoordinateSystemPanel().setEditable(false);
            getOkButton().setEnabled(false);
            getCancelButton().setEnabled(false);
        }
        else {
            getCoordinateSystemPanel().setEditable(true);
            getOkButton().setEnabled(true);
            getCancelButton().setEnabled(true);
        }
    }

    public static VehicleType showVehicleInfoDialog(VehicleType ve) {
        VehicleInfo vecPanel = new VehicleInfo(ve);
        vecPanel.getVehicleInfoDialog();

        return ve;
    }

    public static CoordinateSystem showVehicleMissionEditDialog(VehicleType ve, CoordinateSystem cs) {
        VehicleInfo vecPanel = new VehicleInfo(ve, cs, false, false);
        vecPanel.getVehicleInfoDialog();

        return cs;
    }

    public static CoordinateSystem showVehiclePlannerEditDialog(VehicleType ve, CoordinateSystem cs) {
        VehicleInfo vecPanel = new VehicleInfo(ve, cs, false, true);
        vecPanel.getVehicleInfoDialog();

        return cs;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(null);
        this.setPreferredSize(new java.awt.Dimension(402, 479));
        this.setSize(402, 563);
        this.setLocation(5, 5);
        this.add(getGenInfoPanel(), null);
        this.add(getFilesPanel(), null);
        this.add(getAttitudePanel(), null);
        this.add(getOkPanel(), null);
        int width = getGenInfoPanel().getWidth();// +getFilesPanel().getWidth()+getCoordinateSystemPanel().getWidth();
        int height = getGenInfoPanel().getHeight() + getFilesPanel().getHeight()
                + getCoordinateSystemPanel().getHeight() + 20 + 200;
        this.setSize(width, height);
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getGenInfoPanel() {
        if (genInfoPanel == null) {
            dimLabel = new JLabel();
            genInfoPanel = new JPanel();
            genInfoPanel.setLayout(null);
            genInfoPanel.setBorder(BorderFactory.createTitledBorder(null, "Info", TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
            genInfoPanel.setBounds(6, 9, 390, 130);
            dimLabel.setBounds(191, 105, 73, 17);
            dimLabel.setText("L/W/H (m):");
            dimLabel.setToolTipText("Length/Width/Height");
            genInfoPanel.add(getImagePanel(), null);
            genInfoPanel.add(getGinfoPanel(), null);
            genInfoPanel.add(dimLabel, null);
            genInfoPanel.add(getDimTextField(), null);
            genInfoPanel.add(getViewer3DButton(), null);
            genInfoPanel.add(getPropertiesButton(), null);
        }
        return genInfoPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getIdPanel() {
        if (idPanel == null) {
            FlowLayout flowLayout3 = new FlowLayout();
            idValue = new JLabel();
            idLabel = new JLabel();
            idPanel = new JPanel();
            idPanel.setBounds(new Rectangle(0, 0, 200, 18));
            idPanel.setLayout(flowLayout3);
            idLabel.setText("Id:");
            idValue.setText(vehicle.getId());
            idValue.setForeground(java.awt.Color.darkGray);
            flowLayout3.setAlignment(java.awt.FlowLayout.LEFT);
            flowLayout3.setHgap(5);
            flowLayout3.setVgap(1);
            idPanel.add(idLabel, null);
            idPanel.add(idValue, null);
        }
        return idPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getNamePanel() {
        if (namePanel == null) {
            nameValue = new JLabel();
            nameLabel = new JLabel();
            FlowLayout flowLayout4 = new FlowLayout();
            namePanel = new JPanel();
            namePanel.setLocation(new Point(0, 22));
            namePanel.setSize(new Dimension(200, 18));
            namePanel.setLayout(flowLayout4);
            flowLayout4.setAlignment(java.awt.FlowLayout.LEFT);
            flowLayout4.setHgap(5);
            flowLayout4.setVgap(1);
            nameLabel.setText("Name:");
            nameValue.setText(vehicle.getName());
            nameValue.setForeground(java.awt.Color.darkGray);
            namePanel.add(nameLabel, null);
            namePanel.add(nameValue, null);
        }
        return namePanel;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getTypePanel() {
        if (typePanel == null) {
            typeValue = new JLabel();
            typeLabel = new JLabel();
            FlowLayout flowLayout6 = new FlowLayout();
            typePanel = new JPanel();
            typePanel.setBounds(new Rectangle(0, 65, 112, 19));
            typePanel.setLayout(flowLayout6);
            flowLayout6.setAlignment(java.awt.FlowLayout.LEFT);
            typeLabel.setText("Type:");
            typeValue.setText(vehicle.getType().toUpperCase());
            typeValue.setForeground(java.awt.Color.darkGray);
            flowLayout6.setHgap(5);
            flowLayout6.setVgap(1);
            typePanel.add(typeLabel, null);
            typePanel.add(typeValue, null);
        }
        return typePanel;
    }

    /**
     * This method initializes imagePanel
     * 
     * @return pt.lsts.neptus.gui.ImagePanel
     */
    private ImagePanel getImagePanel() {
        if (imagePanel == null) {
            imagePanel = new ImagePanel();
            if (vehicle.getPresentationImageHref().equalsIgnoreCase(""))
                imagePanel.setImage(vehicle.getSideImageHref());
            else
                imagePanel.setImage(vehicle.getPresentationImageHref());
            imagePanel.setSize(174, 92);
            // imagePanel.setImageWidth(174);
            // imagePanel.setImageHeight(103);
            imagePanel.setPreferredSize(new java.awt.Dimension(150, 60));
            imagePanel.adjustImageSizeToPanelSize();
            imagePanel.setLocation(7, 17);
        }
        return imagePanel;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getGinfoPanel() {
        if (ginfoPanel == null) {
            ginfoPanel = new JPanel();
            ginfoPanel.setLayout(null);
            ginfoPanel.setBounds(185, 16, 200, 86);
            ginfoPanel.add(getIdPanel(), null);
            ginfoPanel.add(getNamePanel(), null);
            ginfoPanel.add(getTypePanel(), null);
            ginfoPanel.add(getOpenConsolesButton(), null);
            ginfoPanel.add(getModelPanel(), null);
        }
        return ginfoPanel;
    }

    /**
     * This method initializes jTree
     * 
     * @return javax.swing.JTree
     */
    private JTree getFilesTree() {
        if (filesTree == null) {
            root = new DefaultMutableTreeNode("Files");
            // treeModel = new DefaultTreeModel(root);
            constructTree(root);
            filesTree = new JTree(root);
            filesTree.setEditable(false);
            filesTree.setRootVisible(false);
            filesTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
                public void valueChanged(javax.swing.event.TreeSelectionEvent e) {
                    // NeptusLog.pub().info("<###>valueChanged() " + e.getSource());
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) filesTree.getLastSelectedPathComponent();

                    if (node == null)
                        return;

                    Object nodeInfo = node.getUserObject();
                    if (nodeInfo instanceof TemplateFileVehicle) {
                        // Este tem q ser primeiro já q TemplateFileVehicle extende FileType
                        CardLayout cl = (CardLayout) (getCardsPanel().getLayout());
                        cl.show(getCardsPanel(), "card2");

                        TemplateFileVehicle ft = (TemplateFileVehicle) nodeInfo;
                        fxTypeValue.setText(ft.getType());
                        fxHrefTextField.setText(ft.getHref());
                        fxDescTextArea2.setText(ft.getDescription());
                        fxParamXsltTextField.setText(ft.getParametersToPass());
                        fxOutputFileTextField.setText(ft.getOutputFileName());
                        fxEditButton.setEnabled(true);
                        if (editableInfo)
                            fxEditButton.setText("Edit");
                        else
                            fxEditButton.setText("View");

                    }
                    else if (nodeInfo instanceof FileType) {
                        CardLayout cl = (CardLayout) (getCardsPanel().getLayout());
                        cl.show(getCardsPanel(), "card1");

                        FileType ft = (FileType) nodeInfo;
                        fxTypeValue.setText(ft.getType());
                        fxHrefTextField.setText(ft.getHref());
                        fxDescTextArea.setText(ft.getDescription());
                        fxEditButton.setEnabled(true);
                        if (editableInfo)
                            fxEditButton.setText("Edit");
                        else
                            fxEditButton.setText("View");
                    }
                    else {
                        CardLayout cl = (CardLayout) (getCardsPanel().getLayout());
                        cl.show(getCardsPanel(), "card1");

                        fxTypeValue.setText("");
                        fxHrefTextField.setText("");
                        fxDescTextArea.setText("");
                        fxEditButton.setEnabled(false);
                    }
                }
            });
            JTreeUtils.expandAll(filesTree);
        }
        return filesTree;
    }

    private void constructTree(DefaultMutableTreeNode root) {
        // MapObjectTreeNode tmp;
        DefaultMutableTreeNode parent = root;

        Iterator<?> it = vehicle.getTransformationXSLTTemplates().values().iterator();
        if (it.hasNext()) {
            parent = new DefaultMutableTreeNode("Transformation XSLT");
            root.add(parent);
        }
        while (it.hasNext()) {
            // NeptusLog.pub().info("<###>hello!.,,,,,,,,,");
            TemplateFileVehicle tfx = (TemplateFileVehicle) it.next();
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(tfx);
            parent.add(child);
            // filesTree.scrollPathToVisible(new TreePath(child.getPath()));
        }

        FileType aft = vehicle.getManeuverAdditionalFile();
        if (aft != null) {
            parent = new DefaultMutableTreeNode("Adicional Maneuver File");
            root.add(parent);

            DefaultMutableTreeNode child = new DefaultMutableTreeNode(aft);
            parent.add(child);
            // filesTree.scrollPathToVisible(new TreePath(child.getPath()));
        }

        it = vehicle.getMiscConfigurationFiles().values().iterator();
        if (it.hasNext()) {
            parent = new DefaultMutableTreeNode("Misc files");
            root.add(parent);
        }
        while (it.hasNext()) {
            // NeptusLog.pub().info("<###>hello!.,,,,,,,,,");
            FileType tfx = (FileType) it.next();
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(tfx);
            parent.add(child);
            // filesTree.scrollPathToVisible(new TreePath(child.getPath()));
        }

        // filesTree.addTreeSelectionListener(this);
        // filesTree.addKeyListener(this);
        // return filesTree;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getFileInfoPanel() {
        if (fileInfoPanel == null) {
            fxTypeLabel = new JLabel();
            fxTypeValue = new JLabel();
            fxHrefLabel = new JLabel();
            fxDescLabel = new JLabel();
            fileInfoPanel = new JPanel();
            fileInfoPanel.setLayout(null);
            fxTypeLabel.setBounds(11, 18, 35, 18);
            fxTypeLabel.setText("Type:");
            fxTypeValue.setBounds(54, 18, 135, 18);
            fxTypeValue.setText("");
            fileInfoPanel.setSize(272, 209);
            fileInfoPanel.setLocation(114, 19);
            fileInfoPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "File info",
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
            fxHrefLabel.setBounds(11, 44, 35, 18);
            fxHrefLabel.setText("Href:");
            fxDescLabel.setBounds(11, 70, 78, 18);
            fxDescLabel.setText("Description:");
            fileInfoPanel.add(fxTypeLabel, null);
            fileInfoPanel.add(fxTypeValue, null);
            fileInfoPanel.add(fxHrefLabel, null);
            fileInfoPanel.add(getFxHrefTextField(), null);
            fileInfoPanel.add(fxDescLabel, null);
            fileInfoPanel.add(getFxEditButton(), null);
            fileInfoPanel.add(getCardsPanel(), null);
        }
        return fileInfoPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getFilesPanel() {
        if (filesPanel == null) {
            filesPanel = new JPanel();
            filesPanel.setLayout(null);
            filesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Files",
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
            filesPanel.setBounds(5, 146, 390, 232);
            filesPanel.add(getJScrollPane(), null);
            filesPanel.add(getFileInfoPanel(), null);
        }
        return filesPanel;
    }

    /**
     * This method initializes jScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setViewportView(getFilesTree());
            jScrollPane.setBounds(6, 19, 101, 208);
            jScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.gray, 1));
        }
        return jScrollPane;
    }

    /**
     * This method initializes jTextArea
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getFxDescTextArea() {
        if (fxDescTextArea == null) {
            fxDescTextArea = new JTextArea();
            fxDescTextArea.setEditable(false);
            fxDescTextArea.setForeground(java.awt.Color.gray);
            fxDescTextArea.setLineWrap(true);
            // fxDescTextArea.setPreferredSize(new java.awt.Dimension(252,107));
        }
        return fxDescTextArea;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getFxHrefTextField() {
        if (fxHrefTextField == null) {
            fxHrefTextField = new JTextField();
            fxHrefTextField.setBounds(54, 44, 209, 18);
            fxHrefTextField.setEditable(false);
            fxHrefTextField.setForeground(java.awt.Color.gray);
        }
        return fxHrefTextField;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getFxEditButton() {
        if (fxEditButton == null) {
            fxEditButton = new JButton() {
                private static final long serialVersionUID = 1L;

                public void setEnabled(boolean value) {
                    if (value == false)
                        super.setEnabled(false);
                    else {
                        // if (editableInfo)
                        super.setEnabled(value);
                        // else
                        // super.setEnabled(false);
                    }
                }
            };
            fxEditButton.setBounds(180, 74, 74, 16);
            fxEditButton.setText("Edit");
            fxEditButton.setEnabled(false);
            fxEditButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (!new File(fxHrefTextField.getText()).exists()) {
                        JOptionPane.showMessageDialog(fxEditButton, "The file doesn't exists!");
                        return;
                    }
                    if (new File(fxHrefTextField.getText()).isDirectory()) {
                        JOptionPane.showMessageDialog(fxEditButton, "Cannot edit a directory!");
                        return;
                    }
                    if (editableInfo) {
                        EditorLauncher ed = new EditorLauncher();
                        short edType;
                        if ("xml".equals(fxTypeValue.getText()) || "xslt".equals(fxTypeValue.getText())
                                || "xsl".equals(fxTypeValue.getText()))
                            edType = ed.XML_EDITOR_TYPE;
                        else
                            edType = ed.TEXT_EDITOR_TYPE;
                        boolean rsb = ed.editFile(fxHrefTextField.getText(), edType, false);
                        NeptusLog.pub().info("<###>>" + rsb);
                    }
                    else {
                        // TextViewer.showFileViewer(fxHrefTextField.getText());
                        TextViewer.showFileViewerDialog(SwingUtilities.getWindowAncestor(VehicleInfo.this),
                                fxHrefTextField.getText());
                    }
                }
            });
        }
        return fxEditButton;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getDimTextField() {
        if (dimTextField == null) {
            dimTextField = new JTextField();
            dimTextField.setBounds(267, 105, 117, 17);
            dimTextField.setEditable(false);
            dimTextField.setText(vehicle.getXSize() + " x " + vehicle.getYSize() + " x " + vehicle.getZSize());
            dimTextField.setToolTipText("Length/Width/Height");
        }
        return dimTextField;
    }

    /**
     * This method initializes jScrollPane1
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getFxDescScrollPane() {
        if (fxDescScrollPane == null) {
            fxDescScrollPane = new JScrollPane();
            // fxDescScrollPane.setBounds(11, 96, 252, 107);
            fxDescScrollPane.setBounds(0, 0, 249, 109);
            fxDescScrollPane.setViewportView(getFxDescTextArea());
            fxDescScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            fxDescScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            // fxDescScrollPane.setPreferredSize(new java.awt.Dimension(252,107));
        }
        return fxDescScrollPane;
    }

    /**
     * This method initializes coordinateSystemPanel
     * 
     * @return pt.lsts.neptus.gui.CoordinateSystemPanel
     */
    private CoordinateSystemPanel getCoordinateSystemPanel() {
        if (coordinateSystemPanel == null) {
            if (coordinateSystem == null) {
                coordinateSystemPanel = new CoordinateSystemPanel(vehicle.getCoordinateSystem());
                coordinateSystemPanel.setChangeHomeVisible(false);
            }
            else {
                coordinateSystemPanel = new CoordinateSystemPanel(coordinateSystem);
                coordinateSystemPanel.setChangeHomeVisible(true);
            }

            coordinateSystemPanel.setPreferredSize(new java.awt.Dimension(100, 16));
            coordinateSystemPanel.setEditable(false);
        }
        return coordinateSystemPanel;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getAttitudePanel() {
        if (attitudePanel == null) {
            attitudePanel = new JPanel();
            attitudePanel.setLayout(new BorderLayout());
            attitudePanel.setBounds(5, 387, 390, 121);
            attitudePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Inertial Coordinate System",
                    javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                    javax.swing.border.TitledBorder.DEFAULT_POSITION, null, null));
            attitudePanel.add(getCoordinateSystemPanel(), java.awt.BorderLayout.CENTER);
        }
        return attitudePanel;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText("Ok");
            okButton.setPreferredSize(new java.awt.Dimension(90, 28));
            okButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    CoordinateSystem cs = getCoordinateSystemPanel().getCoordinateSystem();
                    coordinateSystem.setCoordinateSystem(cs);
                    closeFrame();
                }
            });
        }
        return okButton;
    }

    /**
	 * 
	 */
    private void closeFrame() {
        Object hier = getOkPanel();
        while (!(hier instanceof JFrame) && !(hier instanceof JInternalFrame)) {
            hier = ((JComponent) hier).getParent();
        }
        if (hier instanceof JFrame)
            ((JFrame) hier).dispose();
        else if (hier instanceof JInternalFrame)
            ((JInternalFrame) hier).dispose();
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getOkPanel() {
        if (okPanel == null) {
            FlowLayout flowLayout1 = new FlowLayout();
            okPanel = new JPanel();
            okPanel.setLayout(flowLayout1);
            okPanel.setBounds(8, 518, 387, 42);
            flowLayout1.setAlignment(java.awt.FlowLayout.RIGHT);
            okPanel.add(getOkButton(), null);
            okPanel.add(getCancelButton(), null);
        }
        return okPanel;
    }

    /**
     * This method initializes jButton1
     * 
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setPreferredSize(new java.awt.Dimension(90, 28));
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    closeFrame();
                }
            });
        }
        return cancelButton;
    }

    /**
     * This method initializes jPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getCardsPanel() {
        if (cardsPanel == null) {
            cardsPanel = new JPanel();
            cardsPanel.setLayout(new CardLayout());
            cardsPanel.setBounds(10, 92, 252, 111);
            cardsPanel.add(getCard1(), getCard1().getName());
            cardsPanel.add(getCard2(), getCard2().getName());
        }
        return cardsPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getCard1() {
        if (card1 == null) {
            card1 = new JPanel();
            card1.setLayout(null);
            card1.setName("card1");
            card1.add(getFxDescScrollPane(), null);
        }
        return card1;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getCard2() {
        if (card2 == null) {
            jLabel1 = new JLabel();
            jLabel = new JLabel();
            card2 = new JPanel();
            card2.setLayout(null);
            card2.setName("card2");
            jLabel.setBounds(3, 68, 48, 16);
            jLabel.setText("Params:");
            jLabel1.setBounds(3, 91, 48, 16);
            jLabel1.setText("Output:");
            card2.add(getJScrollPane1(), null);
            card2.add(getFxParamXsltTextField(), null);
            card2.add(getFxOutputFileTextField(), null);
            card2.add(jLabel, null);
            card2.add(jLabel1, null);
        }
        return card2;
    }

    /**
     * This method initializes jScrollPane1
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getJScrollPane1() {
        if (jScrollPane1 == null) {
            jScrollPane1 = new JScrollPane();
            jScrollPane1.setBounds(0, 0, 252, 64);
            jScrollPane1.setViewportView(getFxDescTextArea2());
        }
        return jScrollPane1;
    }

    /**
     * This method initializes jTextArea
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getFxDescTextArea2() {
        if (fxDescTextArea2 == null) {
            fxDescTextArea2 = new JTextArea();
            fxDescTextArea2.setEditable(false);
            fxDescTextArea2.setForeground(java.awt.Color.gray);
            fxDescTextArea2.setLineWrap(false);
        }
        return fxDescTextArea2;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getFxParamXsltTextField() {
        if (fxParamXsltTextField == null) {
            fxParamXsltTextField = new JTextField();
            // paramXsltTextField.setLocation(59, 78);
            // paramXsltTextField.setSize(30, 20);
            fxParamXsltTextField.setBounds(57, 68, 192, 18);
            fxParamXsltTextField.setEditable(false);
            fxParamXsltTextField.setForeground(java.awt.Color.gray);
        }
        return fxParamXsltTextField;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getFxOutputFileTextField() {
        if (fxOutputFileTextField == null) {
            fxOutputFileTextField = new JTextField();
            // jTextField.setBounds(61, 105, 10, 20);
            fxOutputFileTextField.setBounds(57, 91, 191, 18);
            fxOutputFileTextField.setEditable(false);
            fxOutputFileTextField.setForeground(java.awt.Color.gray);
        }
        return fxOutputFileTextField;
    }

    public VehicleMission getVehicleMission() {
        VehicleMission vm = new VehicleMission();
        vm.setCoordinateSystem(getCoordinateSystemPanel().getCoordinateSystem());
        vm.setId(vehicle.getId());
        vm.setName(vehicle.getName());
        vm.setVehicle(vehicle);

        return vm;
    }

    /**
     * This method initializes viewer3DButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getViewer3DButton() {
        if (viewer3DButton == null) {
            viewer3DButton = new JButton();
            viewer3DButton.setText("3D model");
            viewer3DButton.setBounds(new java.awt.Rectangle(8, 111, 85, 15));
            viewer3DButton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 10));
            viewer3DButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    viewer3DButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                Object3DCreationHelper.open3DViewerDialogForModelPath(VehicleInfo.this, vehicle.getName(), vehicle.getModel3DHref());
                            }
                            catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            catch (Error e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            viewer3DButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return viewer3DButton;
    }

    // Properties to edit
    public DefaultProperty[] getProperties() {
        LinkedList<DefaultProperty> propertiesList = new LinkedList<DefaultProperty>();
        DefaultProperty comP = PropertiesEditor.getPropertyInstance("icon color", "Properties", Color.class,
                vehicle.getIconColor(), true);
        propertiesList.add(comP);

        // Base
        String categoryBase = "Base";
        String protocolsStringBase = "";
        for (String protocol : vehicle.getProtocols())
            protocolsStringBase += protocol + " ";
        comP = PropertiesEditor.getPropertyInstance("protocols suported", categoryBase, String.class,
                protocolsStringBase, false);
        propertiesList.add(comP);

        for (ProtocolArgs pArgs : vehicle.getProtocolsArgs().values()) {
            if (pArgs instanceof IMCArgs) {
                IMCArgs nArgs = (IMCArgs) pArgs;
                comP = PropertiesEditor.getPropertyInstance("imc.port", categoryBase, Integer.class,
                        Integer.valueOf(nArgs.getPort()), true);
                propertiesList.add(comP);
                comP = PropertiesEditor.getPropertyInstance("imc.portTCP", categoryBase, Integer.class, Integer.valueOf(
                        nArgs.getPortTCP()), true);
                propertiesList.add(comP);
                comP = PropertiesEditor.getPropertyInstance("imc.udp-on", categoryBase, Boolean.class, Boolean.valueOf(
                        nArgs.isUdpOn()), true);
                propertiesList.add(comP);
                comP = PropertiesEditor.getPropertyInstance("imc.tcp-on", categoryBase, Boolean.class, Boolean.valueOf(
                        nArgs.isTcpOn()), true);
                propertiesList.add(comP);
                comP = PropertiesEditor.getPropertyInstance("imc.imc-id", categoryBase, ImcId16.class,
                        (nArgs.getImcId() == null) ? ImcId16.NULL_ID : nArgs.getImcId(), true);
                propertiesList.add(comP);
            }
            else if (pArgs instanceof FTPArgs) {
                FTPArgs fArgs = (FTPArgs) pArgs;
                comP = PropertiesEditor.getPropertyInstance(
                        "ftp",
                        categoryBase,
                        String.class,
                        new String("transfer mode = " + fArgs.getTransferMode() + ", connection mode = "
                                + fArgs.getConnectionMode()), false);
                propertiesList.add(comP);
            }
            else if (pArgs instanceof PropertiesProvider) {
                for (DefaultProperty prop : ((PropertiesProvider)pArgs).getProperties()) {
                    prop.setCategory(((PropertiesProvider) pArgs).getPropertiesDialogTitle());
                    propertiesList.add(prop);
                }                
            }
        }

        for (CommMean cm : vehicle.getCommunicationMeans().values()) {
            String category = cm.getName();
            comP = PropertiesEditor.getPropertyInstance("host name", category, String.class,
                    new String(cm.getHostAddress()), true);
            propertiesList.add(comP);

            String protocolsString = "";
            for (String protocol : cm.getProtocols())
                protocolsString += protocol + " ";
            comP = PropertiesEditor.getPropertyInstance("protocols suported", category, String.class, protocolsString,
                    false);
            propertiesList.add(comP);

            comP = PropertiesEditor.getPropertyInstance("latency", category, String.class, new String(cm.getLatency()
                    + " " + cm.getLatencyUnit()), false);
            propertiesList.add(comP);

            for (ProtocolArgs pArgs : cm.getProtocolsArgs().values()) {
                if (pArgs instanceof IMCArgs) {
                    IMCArgs nArgs = (IMCArgs) pArgs;
                    comP = PropertiesEditor.getPropertyInstance("imc.port", category, Integer.class,
                            Integer.valueOf(nArgs.getPort()), true);
                    propertiesList.add(comP);
                    comP = PropertiesEditor.getPropertyInstance("imc.portTCP", category, Integer.class, Integer.valueOf(
                            nArgs.getPortTCP()), true);
                    propertiesList.add(comP);
                    comP = PropertiesEditor.getPropertyInstance("imc.udp-on", category, Boolean.class, Boolean.valueOf(
                            nArgs.isUdpOn()), true);
                    propertiesList.add(comP);
                    comP = PropertiesEditor.getPropertyInstance("imc.tcp-on", category, Boolean.class, Boolean.valueOf(
                            nArgs.isTcpOn()), true);
                    propertiesList.add(comP);
                    comP = PropertiesEditor.getPropertyInstance("imc.imc-id", category, ImcId16.class,
                            (nArgs.getImcId() == null) ? ImcId16.NULL_ID : nArgs.getImcId(), true);
                    propertiesList.add(comP);
                }
                else if (pArgs instanceof FTPArgs) {
                    FTPArgs fArgs = (FTPArgs) pArgs;
                    comP = PropertiesEditor.getPropertyInstance(
                            "ftp",
                            category,
                            String.class,
                            new String("transfer mode = " + fArgs.getTransferMode() + ", connection mode = "
                                    + fArgs.getConnectionMode()), false);
                    propertiesList.add(comP);
                }
            }
        }

        for (String id : vehicle.getTransformationXSLTTemplates().keySet()) {
            TemplateFileVehicle tfile = vehicle.getTransformationXSLTTemplates().get(id);
            String category = id + ":" + tfile.getName() + " parameters";
            for (String param : tfile.getParametersToPassList().keySet()) {
                String value = tfile.getParametersToPassList().get(param);
                comP = PropertiesEditor.getPropertyInstance(param, category, String.class, value, true);
                propertiesList.add(comP);
            }
        }
        /*
         * DefaultProperty connColor = PropertiesEditor.getPropertyInstance("Connections color", Color.class,
         * SensorObject.getConnectionsColor(), true); DefaultProperty blinkProperty =
         * PropertiesEditor.getPropertyInstance("Blink when a message is received", Boolean.class, new
         * Boolean(blinkOnMessage), true); DefaultProperty showToolbar =
         * PropertiesEditor.getPropertyInstance("Show Toolbar", Boolean.class, toolbarPanel.isVisible(), true);
         * DefaultProperty showGrid = PropertiesEditor.getPropertyInstance("Show Grid", Boolean.class, gridShown, true);
         * DefaultProperty gridCol = PropertiesEditor.getPropertyInstance("Grid color", Color.class, gridColor, true);
         * DefaultProperty backColor = PropertiesEditor.getPropertyInstance("Background color",Color.class, bgColor,
         * true);
         */
        // return new Property[] {showGrid, gridCol, backColor, showToolbar, connColor, blinkProperty};
        DefaultProperty[] prop = new DefaultProperty[propertiesList.size()];
        return propertiesList.toArray(prop);
    }

    public void setProperties(Property[] properties) {
        LinkedHashMap<String, String> transFilesList = new LinkedHashMap<String, String>();
        for (String id : vehicle.getTransformationXSLTTemplates().keySet()) {
            TemplateFileVehicle tfile = vehicle.getTransformationXSLTTemplates().get(id);
            String category = id + ":" + tfile.getName() + " parameters";
            transFilesList.put(category, id);
        }

        for (Property prop : properties) {
            String cat = prop.getCategory();
            if ("Properties".equalsIgnoreCase(cat)) {
                if (prop.getName().equals("icon color")) {
                    vehicle.setIconColor((Color) prop.getValue());
                }
            }
            if ("Base".equalsIgnoreCase(cat)) {
                for (String protocol : vehicle.getProtocolsArgs().keySet()) {
                    if (prop.getName().startsWith(protocol)) {
                        if (protocol.equalsIgnoreCase(CommMean.IMC)) {
                            ProtocolArgs protoArgs = vehicle.getProtocolsArgs().get(protocol);
                            if (protoArgs != null) {
                                if (prop.getName().equals("imc.port"))
                                    ((IMCArgs) protoArgs).setPort((Integer) prop.getValue());
                                else if (prop.getName().equals("imc.portTCP"))
                                    ((IMCArgs) protoArgs).setPortTCP((Integer) prop.getValue());
                                else if (prop.getName().equals("imc.udp-on"))
                                    ((IMCArgs) protoArgs).setUdpOn((Boolean) prop.getValue());
                                else if (prop.getName().equals("imc.tcp-on"))
                                    ((IMCArgs) protoArgs).setTcpOn((Boolean) prop.getValue());
                                else if (prop.getName().equals("imc.imc-id"))
                                    ((IMCArgs) protoArgs).setImcId((ImcId16) prop.getValue());
                            }
                        }
                    }
                }

            }

            CommMean cm = vehicle.getCommunicationMeans().get(cat);
            if (cm != null) {
                if (prop.getName().equals("host name")) {
                    cm.setHostAddress((String) prop.getValue());
                }
                else {
                    // for (String protocol : cm.getProtocols())
                    for (String protocol : cm.getProtocolsArgs().keySet()) {
                        if (prop.getName().startsWith(protocol)) {
                            if (protocol.equalsIgnoreCase(CommMean.IMC)) {
                                ProtocolArgs protoArgs = cm.getProtocolsArgs().get(protocol);
                                if (protoArgs != null) {
                                    if (prop.getName().equals("imc.port"))
                                        ((IMCArgs) protoArgs).setPort((Integer) prop.getValue());
                                    else if (prop.getName().equals("imc.portTCP"))
                                        ((IMCArgs) protoArgs).setPortTCP((Integer) prop.getValue());
                                    else if (prop.getName().equals("imc.udp-on"))
                                        ((IMCArgs) protoArgs).setUdpOn((Boolean) prop.getValue());
                                    else if (prop.getName().equals("imc.tcp-on"))
                                        ((IMCArgs) protoArgs).setTcpOn((Boolean) prop.getValue());
                                    else if (prop.getName().equals("imc.imc-id"))
                                        ((IMCArgs) protoArgs).setImcId((ImcId16) prop.getValue());
                                }
                            }
                        }
                    }
                }
            }
            // Look for XSLTs parameters
            String idTFile = transFilesList.get(cat);
            if (idTFile != null) {
                TemplateFileVehicle tFile = vehicle.getTransformationXSLTTemplates().get(idTFile);
                tFile.getParametersToPassList().put(prop.getName(), (String) prop.getValue());
            }
        }
    }

    public String getPropertiesDialogTitle() {
        return "Vehicle properties";
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

    /**
     * This method initializes propertiesButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getPropertiesButton() {
        if (propertiesButton == null) {
            propertiesButton = new JButton();
            propertiesButton.setBounds(new java.awt.Rectangle(95, 111, 85, 15));
            propertiesButton.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 10));
            propertiesButton.setText("Properties");
            propertiesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(VehicleInfo.this),
                    // "<html>The properties are not <b>stored</b>. The modification is only " +
                    // "valid until you restart the aplication!");
                    if (!PropertiesEditor.editProperties(VehicleInfo.this,
                            SwingUtilities.getWindowAncestor(VehicleInfo.this), true)) {
                        String filePath = vehicle.getOriginalFilePath();
                        Document doc = vehicle.asDocument();
//                        if (VehicleType.validate(doc)) {
//                            String dataToSave = FileUtil.getAsPrettyPrintFormatedXMLString(doc);
//                            FileUtil.saveToFile(filePath, dataToSave);
//                        }
//                        else {
//                            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(VehicleInfo.this),
//                                    "<html>The properties were not" + " <b>stored</b>. The data is not valid!");
//                        }
                        // Let us relax the validation
                        String dataToSave = FileUtil.getAsPrettyPrintFormatedXMLString(doc);
                        FileUtil.saveToFile(filePath, dataToSave);
                    }
                    try {
                        ImcMsgManager.getManager().stop();
                        Thread.sleep(100);
                        ImcMsgManager.getManager().start();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
        return propertiesButton;
    }

    /**
     * This method initializes openConsolesButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOpenConsolesButton() {
        if (openConsolesButton == null) {
            openConsolesButton = new JButton();
            openConsolesButton.setFont(new Font("Dialog", Font.BOLD, 10));
            openConsolesButton.setBounds(new Rectangle(116, 63, 80, 15));
            openConsolesButton.setText("Consoles");
            openConsolesButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    VehiclesHolder.showConsole(vehicle, VehicleInfo.this);
                }
            });
        }
        return openConsolesButton;
    }

    /**
     * This method initializes modelPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getModelPanel() {
        if (modelPanel == null) {
            modelValue = new JLabel();
            modelValue.setForeground(Color.darkGray);
            modelValue.setText(vehicle.getModel());
            modelLabel = new JLabel();
            modelLabel.setText("Model:");
            FlowLayout flowLayout41 = new FlowLayout();
            flowLayout41.setHgap(5);
            flowLayout41.setAlignment(FlowLayout.LEFT);
            flowLayout41.setVgap(1);
            modelPanel = new JPanel();
            modelPanel.setLocation(new Point(0, 44));
            modelPanel.setSize(new Dimension(200, 18));
            modelPanel.setLayout(flowLayout41);
            modelPanel.add(modelLabel, null);
            modelPanel.add(modelValue, null);
        }
        return modelPanel;
    }

    // PropertiesEditor.editProperties((SubPanel)c);

    /* *
     * 
     * @param args / public static void main(String[] args) { VehicleType ve = new
     * VehicleType("./vehicles-defs/isurus-vehicle-lsts.xml"); //ve.setId("isurus"); //ve.setName("Isurus");
     * VehicleInfo.showVehicleInfoDialog(ve); }
     */

} // @jve:decl-index=0:visual-constraint="11,10"
