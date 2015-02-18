/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel Ribeiro
 * Feb 11, 2015
 */

package pt.lsts.neptus.mra.markermanagement;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.text.DateFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.util.ImageUtils;


/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class MarkerEdit extends JFrame {

    private static final long serialVersionUID = 1613149353413851878L;

    private JMenuBar menuBar;

    private MarkerManagement parent;
    private AbstractAction save, del, exit, freeDraw, rectDraw;
    private String[] classificationList = new String[] {"1 - <Unknown>", "2 - <Ship>", "3 - <Etc1>", "4 - <Etc2>", "5 - <Etc3>"};
    private JPopupMenu drawPopupMenu;
    private LogMarkerItem selectedMarker;

    //
    private JTextField nameLabelValue;
    private JLabel timeStampValue;
    private JLabel locationValue;
    private JLabel depthValue;
    private JComboBox<String> classifValue;
    private JTextArea annotationValue;

    public MarkerEdit(MarkerManagement parent) {
        setIconImage(Toolkit.getDefaultToolkit().getImage(MarkerEdit.class.getResource("/images/menus/edit.png")));
        this.parent = parent;

        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 470, 340);

        setupFileMenu();

        initialize();

    }

    @SuppressWarnings("unchecked")
    private void initialize() {
        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new MigLayout("", "[][][][][grow][][][][grow]", "[][][][][][][grow][][grow]"));

        JLabel markerImage = new JLabel();
        markerImage.setHorizontalAlignment(SwingConstants.CENTER);
        markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));

        setupDrawPopup();

        markerImage.setComponentPopupMenu(drawPopupMenu);

        panel.add(markerImage, "cell 0 0 7 7,alignx center");

        JLabel nameLabel = new JLabel("Label:");
        panel.add(nameLabel, "cell 7 0,alignx left");

        nameLabelValue = new JTextField();
        nameLabelValue.setBackground(Color.WHITE);
        nameLabelValue.setText("MARKER_LABEL");
        panel.add(nameLabelValue, "cell 8 0,alignx left");
        nameLabelValue.setColumns(13);

        JLabel timeStampLabel = new JLabel("Timestamp:");
        panel.add(timeStampLabel, "cell 7 1,alignx left");

        timeStampValue = new JLabel("TS");
        panel.add(timeStampValue, "cell 8 1,alignx left");

        JLabel locationLabel = new JLabel("Location:");
        panel.add(locationLabel, "cell 7 2,alignx left");

        locationValue = new JLabel("LOCATION");
        panel.add(locationValue, "cell 8 2,alignx left");

        JLabel depthLabel = new JLabel("Depth:");
        panel.add(depthLabel, "cell 7 3,alignx left");

        depthValue = new JLabel("DEPTH");
        panel.add(depthValue, "cell 8 3,alignx left");

        JLabel classifLabel = new JLabel("Classification:");
        panel.add(classifLabel, "cell 7 4,alignx trailing");

        classifValue = new JComboBox<>();
        classifValue.setBackground(Color.WHITE);
        classifValue.setModel(new DefaultComboBoxModel(Classification.values()));
        panel.add(classifValue, "cell 8 4,alignx left");

        JLabel annotationLabel = new JLabel("Annotation:");
        panel.add(annotationLabel, "cell 7 5");

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, "cell 7 6 2 1,grow");

        annotationValue = new JTextArea();
        annotationValue.setText("<Your annotations here>");
        annotationValue.setLineWrap(true); //Auto down line if the line is too long
        annotationValue.setWrapStyleWord(true); //Auto set up the style of words
        annotationValue.setRows(8);
        scrollPane.setViewportView(annotationValue);
    }
    

    public void loadMarker(LogMarkerItem log) {
        selectedMarker = log;

        nameLabelValue.setText(selectedMarker.getLabel());
        timeStampValue.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(selectedMarker.getTimestamp()));
        locationValue.setText(selectedMarker.getLocation().toString());
        depthValue.setText(Integer.toString(selectedMarker.getDepth()));
        classifValue.setSelectedItem(selectedMarker.getClassification());
        
        if (!selectedMarker.getAnnotation().equals(""))
            annotationValue.setText(selectedMarker.getAnnotation());
        
        System.out.println(selectedMarker.toString());
    }

    private void setupDrawPopup() {
        drawPopupMenu = new JPopupMenu();
        freeDraw = new AbstractAction(I18n.text("Free draw"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub

            }
        };
        freeDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Draw with mouse.") + ".");

        rectDraw = new AbstractAction(I18n.text("Rectangle draw"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                // TODO Auto-generated method stub

            }
        };
        freeDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Draw rectangle.") + ".");

        drawPopupMenu.add(rectDraw);
        drawPopupMenu.add(freeDraw);
    }

    private void setupFileMenu() {
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu(I18n.text("File"));
        menuBar.add(mnFile);

        save = new AbstractAction(I18n.text("Save"), ImageUtils.getIcon("images/menus/save.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO : save current marker
            }
        };
        save.putValue(Action.SHORT_DESCRIPTION, I18n.text("Save Marker") + ".");

        del = new AbstractAction(I18n.text("Delete"), ImageUtils.getIcon("images/menus/editdelete.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                int res = showDelDialog();
                if (res==0)  { 
                    //TODO : delete
                }
            }
        };
        del.putValue(Action.SHORT_DESCRIPTION, I18n.text("Delete Marker") + ".");


        exit = new AbstractAction(I18n.text("Exit"), ImageUtils.getIcon("images/menus/exit.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        exit.putValue(Action.SHORT_DESCRIPTION, I18n.text("Exit Marker Editor") + ".");

        mnFile.add(save);
        mnFile.add(del);
        mnFile.add(exit);

    }

    private int showDelDialog() {
        Object[] options = {"Yes, please", "No, thanks"};
        int n = JOptionPane.showOptionDialog(this,
                "Are you sure you want to delete this marker?",
                "Confirm delete",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return n;
    }

}
