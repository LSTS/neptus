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
 * 20??/??/??
 */
package pt.lsts.neptus.gui.checklist.exec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ChronometerPanel;
import pt.lsts.neptus.gui.checklist.CheckItemPanel;
import pt.lsts.neptus.gui.checklist.GeneratorChecklistPDF;
import pt.lsts.neptus.gui.checklist.UserActionItem;
import pt.lsts.neptus.gui.checklist.UserCommentItem;
import pt.lsts.neptus.gui.checklist.VariableIntervalItem;
import pt.lsts.neptus.gui.swing.JRoundButton;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserActionItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;
import pt.lsts.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.lsts.neptus.types.checklist.CheckItem;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

public class CheckListExe extends JDialog implements CheckSubItemProvider {

    private static final long serialVersionUID = 2800023006457068275L;

    private ChecklistType checklist = null;

    private JPanel top = null;
    private JButton showDescription = null;
    private JPanel textPanelDescription = null;
    private JDialog messagesFrame = null;
    private JTextPane msgTextArea = null;

    private JLabel currentGroupTitle = null;
    private JButton skipGroup = null;

    private JPanel itemTitlePanel = null;
    private JTextArea currentItemText = null;
    private JLabel currentItemTitle = null;
    private JScrollPane jScrollPane = null;
    private JRoundButton skipItem = null;
    private JRoundButton forceRetreatItem = null;
    private JRoundButton forceAdvanceItem = null;

    private ChronometerPanel chronometer = null;

    private boolean checkFlag = true;
    private JPanel endPanel = null;

    private JPanel listSubItems = null;
    private JScrollPane listSubScroll = null;

    private JPanel jPanel = null;

    private int currentItem = 0;
    private int currentGroup = 0;

    private String workingDir = null;

    private String system = null;
    public CheckListExe(String system, ChecklistType ct, Window w, String wd) {
        super(w);
        this.system = system;
        checklist = ct;
        workingDir = wd;
        initialize();

        repaintCheck();

        if (getCurrentItem() != null)
            if (getCurrentItem().isChecked() == false)
                loadCurrent();
            else
                advance();
    }

    private void initialize() {
        this.setTitle("Running :" + checklist.getName());
        this.setIconImage(ImageUtils.getImage("images/buttons/checklist.png"));

        this.createDescriptionFrame();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                CheckListExe.this.closeCurrentItem();
                CheckListExe.this.dispose();
            }
        });

        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(getTop(), BorderLayout.NORTH);
        contentPane.add(getJPanel(), BorderLayout.CENTER);

        chronometer = new ChronometerPanel();
        chronometer.hideButtons();
        chronometer.start();
        chronometer.setPreferredSize(new Dimension(100, 20));
        contentPane.add(chronometer, BorderLayout.SOUTH);

        this.setContentPane(contentPane);
    }

    private JPanel getTop() {
        if (top == null) {
            top = new JPanel();
            top.setLayout(new BorderLayout());
            showDescription = new JButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/info.png", 16, 16)));
            showDescription.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    GuiUtils.centerParent(messagesFrame, CheckListExe.this);
                    messagesFrame.setVisible(true);
                }
            });
            top.add(showDescription, BorderLayout.WEST);

            skipGroup = new JButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/forward.png", 16, 16)));
            skipGroup.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CheckListExe.this.skipGroup();
                }
            });

            skipGroup.setToolTipText("Skip Group");
            skipGroup.setText("Skip Group");
            top.add(skipGroup, BorderLayout.EAST);
            JPanel aux = new JPanel(new BorderLayout());
            aux.add(new JLabel("  Group : "), BorderLayout.WEST);
            aux.add(getCurrentGroupTitle(), BorderLayout.CENTER);
            top.add(aux, BorderLayout.CENTER);
            top.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
            top.setMaximumSize(new Dimension(2000, 50));

        }

        if (checklist.isFlat()) {
            currentGroupTitle.setText("Flat Checklist (no groups)");
            skipGroup.setEnabled(false);
        }
        else {

            currentGroupTitle.setText((String) checklist.getGroupList().keySet().toArray()[currentGroup]);
        }
        currentGroupTitle.setFont(new Font("Arial", Font.BOLD, 20));

        return top;
    }

    private JPanel getJPanel() {
        if (jPanel == null) {
            jPanel = new JPanel();
            jPanel.setLayout(new BorderLayout());
            jPanel.setOpaque(false);
            jPanel.add(getCurrentItemTitle(), BorderLayout.NORTH);
            jPanel.add(getItemTitlePanel(), BorderLayout.SOUTH);
            jPanel.add(getListSubScroll(), BorderLayout.CENTER);
            jPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        }
        return jPanel;
    }

    private JPanel getItemTitlePanel() {
        if (itemTitlePanel == null) {
            itemTitlePanel = new JPanel();
            itemTitlePanel.setLayout(new BorderLayout());
            itemTitlePanel.setOpaque(false);
            itemTitlePanel.setVisible(true);
            JPanel panelAux = new JPanel();
            panelAux.setLayout(new BorderLayout());
            panelAux.add(getSkipItem(), BorderLayout.NORTH);
            JPanel panelAux2 = new JPanel();
            panelAux2.setLayout(new BorderLayout());

            panelAux2.add(getForceRetreatItem(), BorderLayout.WEST);
            panelAux2.add(getForceAdvanceItem(), BorderLayout.EAST);

            panelAux.add(panelAux2, BorderLayout.SOUTH);
            itemTitlePanel.add(panelAux, BorderLayout.EAST);
            // itemTitlePanel.add(getCurrentItemTitle(),
            // java.awt.BorderLayout.NORTH);
            itemTitlePanel.add(getJScrollPane(), java.awt.BorderLayout.CENTER);
            itemTitlePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        return itemTitlePanel;
    }

    private JRoundButton getSkipItem() {// System.err.println("corri aqui");
        if (skipItem == null) {
            skipItem = new JRoundButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/forward.png", 16, 16)));
            skipItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CheckListExe.this.skipItem();
                }
            });
            skipItem.setToolTipText("Skip Item");
            // skipItem.setText("Advance Item");
        }

        return skipItem;
    }

    private JRoundButton getForceAdvanceItem() {// System.err.println("corri aqui");
        if (forceAdvanceItem == null) {
            forceAdvanceItem = new JRoundButton(new ImageIcon(ImageUtils.getScaledImage(
                    "images/buttons/forward_orange.png", 16, 16)));
            forceAdvanceItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CheckListExe.this.forceAdvanceItem();
                }
            });
            forceAdvanceItem.setToolTipText("Force to Advance Item");
            // skipItem.setText("Advance Item");
        }

        return forceAdvanceItem;
    }

    private JRoundButton getForceRetreatItem() {// System.err.println("corri aqui");
        if (forceRetreatItem == null) {
            forceRetreatItem = new JRoundButton(new ImageIcon(ImageUtils.getScaledImage(
                    "images/buttons/retreat_orange.png", 16, 16)));
            forceRetreatItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CheckListExe.this.forceRetreatItem();
                }
            });
            forceRetreatItem.setToolTipText("Force to retreat Item");
            // skipItem.setText("Advance Item");
        }

        return forceRetreatItem;
    }

    private JLabel getCurrentItemTitle() {
        if (currentItemTitle == null) {
            currentItemTitle = new JLabel();

        }
        String currGroup = (String) checklist.getGroupList().keySet().toArray()[currentGroup];
        CheckItem ci = (CheckItem) checklist.getGroupList().get(currGroup).toArray()[currentItem];

        currentItemTitle.setText(ci.getName());

        // Font f = currentItemTitle.getFont();
        currentItemTitle.setFont(new Font("Arial", Font.BOLD, 15));

        return currentItemTitle;
    }

    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setOpaque(false);
            jScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            jScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            jScrollPane.setPreferredSize(new java.awt.Dimension(300, 50));
            jScrollPane.setViewportView(getCurrentItemText());

        }
        return jScrollPane;
    }

    private JLabel getCurrentGroupTitle() {
        if (currentGroupTitle == null) {
            currentGroupTitle = new JLabel();
            // currentGroupTitle.setLineWrap(true);
            // currentItemText.setText(checklist.getDescription());
            // currentGroupTitle.setEditable(false);
        }
        return currentGroupTitle;
    }

    private JTextArea getCurrentItemText() {
        if (currentItemText == null) {
            currentItemText = new JTextArea();
            currentItemText.setLineWrap(true);
            currentItemText.setBorder(new LineBorder(Color.BLACK));

            currentItemText.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyTyped(java.awt.event.KeyEvent e) {
                    if (CheckListExe.this.getCurrentItem() != null)
                        CheckListExe.this.getCurrentItem().setNote(CheckListExe.this.currentItemText.getText());
                }
            });
            // currentItemText.setText(checklist.getDescription());
            // currentItemText.setEditable(false);
        }
        if (getCurrentItem() != null)
            currentItemText.setText(getCurrentItem().getNote());
        Component cmp = currentItemText;
        while (cmp != null) {
            cmp.doLayout();
            cmp.invalidate();
            cmp.validate();
            cmp = cmp.getParent();
        }

        return currentItemText;
    }

    public static void showCheckListExeDialog(String system, ChecklistType ct, Window w, String wd) {
        CheckListExe gctrans = new CheckListExe(system, ct, w, wd);
        gctrans.setModal(false);

        gctrans.setSize(new Dimension(700, 500));

        GuiUtils.centerParent(gctrans, w);
        gctrans.setVisible(true);
    }

    public void createDescriptionFrame() {
        if (messagesFrame == null) {
            messagesFrame = new JDialog(this);
            messagesFrame.setIconImage(ImageUtils.getImage("images/buttons/info.png"));
            messagesFrame.setName("Checklist Description");
            messagesFrame.setTitle("Checklist Description");
            messagesFrame.setSize(500, 300);
            messagesFrame.getContentPane().setLayout(new BorderLayout());

            messagesFrame.getContentPane().add(getTextPanelDescription(), BorderLayout.CENTER);
        }
    }

    public JPanel getTextPanelDescription() {
        if (textPanelDescription == null) {
            textPanelDescription = new JPanel();
            textPanelDescription.repaint();
            textPanelDescription.setLayout(new BorderLayout());

            JScrollPane jScrollPane = new JScrollPane();
            msgTextArea = new JTextPane();
            msgTextArea.setEditable(true);
            msgTextArea.setText(checklist.getDescription());

            msgTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyTyped(java.awt.event.KeyEvent e) {
                    if (CheckListExe.this.getCurrentItem() != null)
                        CheckListExe.this.checklist.setDescription(CheckListExe.this.msgTextArea.getText());
                }
            });
            jScrollPane.setViewportView(msgTextArea);
            textPanelDescription.add(jScrollPane, BorderLayout.CENTER);
            jScrollPane.setVisible(true);

        }
        return textPanelDescription;
    }

    private JScrollPane getListSubScroll() {

        if (listSubScroll == null) {
            listSubScroll = new JScrollPane();
            listSubScroll.setOpaque(false);
            listSubScroll.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            listSubScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            listSubScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 5, 3, 5));
            listSubScroll.setPreferredSize(new java.awt.Dimension(300, 50));
            listSubScroll.setBorder(new EmptyBorder(10, 10, 10, 10));
            // listSubScroll.setBorder(new LoweredBorder());
            listSubScroll.setViewportView(getListSubItems());
        }

        return listSubScroll;
    }

    public JPanel getListSubItems() {
        if (listSubItems == null) {
            listSubItems = new JPanel();
            listSubItems.setLayout(new BoxLayout(listSubItems, BoxLayout.Y_AXIS));
        }
        return listSubItems;
    }

    long initItemTime;

    private void loadCurrent() {
        if (currentItem == 0 && currentGroup == 0)
            forceRetreatItem.setEnabled(false);
        else
            forceRetreatItem.setEnabled(true);
        initItemTime = System.currentTimeMillis();

        if (checklist.isFlat()) {
            currentGroupTitle.setText("Flat Checklist (no groups)");
            skipGroup.setEnabled(false);
        }
        else {
            currentGroupTitle.setText((String) checklist.getGroupList().keySet().toArray()[currentGroup]);
        }

        String currGroup = (String) checklist.getGroupList().keySet().toArray()[currentGroup];
        CheckItem ci = (CheckItem) checklist.getGroupList().get(currGroup).toArray()[currentItem];
        currentItemTitle.setText(ci.getName());

        if (getCurrentItem().getNote() != null)
            currentItemText.setText(getCurrentItem().getNote());
        Component cmp = currentItemText;
        while (cmp != null) {
            cmp.doLayout();
            cmp.invalidate();
            cmp.validate();
            cmp = cmp.getParent();
        }

        if (getCurrentItem().getAutoSubItems().isEmpty()) {
            CheckAutoUserActionItem casi = new CheckAutoUserActionItem();
            casi.setAction(ci.getName() + getCurrentItem().getNote());
            addSubItem(casi);
        }
        else
            for (CheckAutoSubItem casi : getCurrentItem().getAutoSubItems()) {
                // casi.setChecked(false);
                addSubItem(casi);
            }
        jScrollPane.findComponentAt(0, 0);
        jScrollPane.findComponentAt(new Point(1, 1));
        repaintCheck();
    }

    private Vector<CheckSubItemExe> autoSubItems = new Vector<CheckSubItemExe>();

    private void addSubItem(CheckAutoSubItem casi) {
        if (casi.getSubItemType().equals(VariableIntervalItem.TYPE_ID)) {
            CheckVariableItem si = new CheckVariableItem(system, (CheckAutoVarIntervalItem) casi);
            si.addCheckSubItemProvider(this);
            this.listSubItems.add(si);
            autoSubItems.add(si);
        }
        if (casi.getSubItemType().equals(UserActionItem.TYPE_ID)) {
            CheckActionItem si = new CheckActionItem((CheckAutoUserActionItem) casi);
            si.addCheckSubItemProvider(this);
            this.listSubItems.add(si);
            autoSubItems.add(si);
        }
        if (casi.getSubItemType().equals(UserCommentItem.TYPE_ID)) {
            CheckLogItem si = new CheckLogItem((CheckAutoUserLogItem) casi);
            si.addCheckSubItemProvider(this);
            this.listSubItems.add(si);
            autoSubItems.add(si);
        }
    }

    private void closeCurrentItem() {

        for (CheckSubItemExe casi : autoSubItems) {
            casi.close();
        }

        autoSubItems.clear();
        this.listSubItems.removeAll();
        repaintCheck();
        // System.err.println("close item");
    }

    public void checkedAdvance() {
        boolean okFlag = true;

        for (CheckSubItemExe casi : autoSubItems) {
            if (!casi.isCheck())
                okFlag = false;
        }

        if (okFlag) {

            /*
             * final BlockingGlassPane bgp = new BlockingGlassPane(); this.setGlassPane(bgp); bgp.block(true);
             */

            getCurrentItem().setChecked(true);
            long time = System.currentTimeMillis();
            // long totalTime = time - initItemTime;
            // System.err.println(this + ": Total checklist load time: " +
            // totalTime + " ms.");

            getCurrentItem().setDateChecked(CheckItemPanel.dateXMLFormater.format(new Date(time)));

            /*
             * AsyncTask task =new AsyncTask(){
             * 
             * @Override public Object run() throws Exception { try { Thread.sleep(3000); } catch (InterruptedException
             * e) { // TODO Auto-generated catch block e.printStackTrace(); } return null; }
             * 
             * @Override public void finish() { advance(); bgp.block(false);
             * 
             * } };
             * 
             * AsyncWorker.post(task);
             */

            advance();

            /*
             * getCurrentItem().setRunDurationSeconds(totalTime / 1000.0);
             * 
             * SwingUtilities.invokeLater(new Thread() {
             * 
             * @Override public void run() { try { Thread.sleep(3000); } catch (InterruptedException e) { // TODO
             * Auto-generated catch block e.printStackTrace(); }
             * 
             * } });
             */

        }
        else {
            getCurrentItem().setChecked(false);
        }

    }

    public void Retreat() {
        closeCurrentItem();

        if (currentItem > 0) {
            currentItem--;
        }
        else {
            if (currentGroup > 0) {
                currentGroup--;
                currentItem = getCurrentItemLength() - 1;
            }
        }
        // for (CheckAutoSubItem casi : getCurrentItem().getAutoSubItems()) {
        // casi.setChecked(false);
        // }

        loadCurrent();
    }

    public void advance() {
        // System.err.println("avancei");

        closeCurrentItem();
        boolean flagMissCheck = false;
        boolean flagEnd = false;

        while (!flagMissCheck) {
            currentItem++;
            if (currentItem < getCurrentItemLength()) {
                if (!getCurrentItem().isChecked())
                    flagMissCheck = true;

            }
            else {
                currentItem = 0;
                currentGroup++;

                if (currentGroup >= getGroupLength()) {
                    flagMissCheck = true;
                    flagEnd = true;
                }
                else if (!getCurrentItem().isChecked())
                    flagMissCheck = true;
            }
        }

        if (!flagEnd)
            loadCurrent();
        else
            end();
    }

    private JPanel getEndPanel() {
        if (endPanel == null) {
            endPanel = new JPanel();
            JButton close = new JButton("Close");

            if (checkFlag == true)
                close = new JButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/checklist.png", 16, 16)));
            else
                close = new JButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/close.png", 16, 16)));

            close.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    chronometer.stop();

                    String saux = DateTimeUtil.dateTimeFileNameFormatter.format(new Date(System.currentTimeMillis()));
                    String aux2 = checklist.getName().replace('.', '\0');
                    aux2 = aux2.replace('<', '_');
                    aux2 = aux2.replace('>', '_');
                    aux2 = aux2.replace('\\', '_');
                    aux2 = aux2.replace('*', '_');
                    aux2 = aux2.replace(':', '_');
                    aux2 = aux2.replace('|', '_');
                    aux2 = aux2.replace('?', '_');
                    aux2 = aux2.replace('.', '_');
                    aux2 = aux2.replace('/', '_');
                    aux2 = aux2.replace('"', '_');

                    if (workingDir == null)
                        workingDir = ".";

                    aux2 = CheckListExe.this.workingDir + "/" + aux2 + " " + saux + "." + FileUtil.FILE_TYPE_CHECKLIST;
                    boolean ret = FileUtil.saveToFile(aux2,
                            FileUtil.getAsPrettyPrintFormatedXMLString(checklist.asDocument()));
                    if (ret) {

                        JOptionPane.showMessageDialog(CheckListExe.this, "<html>Checklist saved!!" + "<br>To file \""
                                + aux2 + "\".</html>");

                    }

                    CheckListExe.this.dispose();
                }
            });
            close.setToolTipText("Close Checklist Execution");
            close.setText("Close");

            final JButton produceReport = new JButton(new ImageIcon(ImageUtils.getScaledImage(
                    "images/buttons/report.png", 16, 16)));
            produceReport.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>To PDF actionPerformed()");

                    // FIXME Perguntar se quer guardar ou não (tem q testar se
                    // tem alterações)
                    boolean ret = false;
                    /*
                     * if ( (originalFilePath == null) || originalFilePath.equalsIgnoreCase("")) ret = saveAsFile();
                     * else ret = saveFile();
                     */

                    String saux = DateTimeUtil.dateTimeFileNameFormatter.format(new Date(System.currentTimeMillis()));
                    String aux2 = checklist.getName().replace('.', '\0');
                    aux2 = aux2.replace('<', '_');
                    aux2 = aux2.replace('>', '_');
                    aux2 = aux2.replace('\\', '_');
                    aux2 = aux2.replace('*', '_');
                    aux2 = aux2.replace(':', '_');
                    aux2 = aux2.replace('|', '_');
                    aux2 = aux2.replace('?', '_');
                    aux2 = aux2.replace('.', '_');
                    aux2 = aux2.replace('/', '_');
                    aux2 = aux2.replace('"', '_');

                    if (workingDir == null)
                        workingDir = ".";

                    aux2 = CheckListExe.this.workingDir + "/" + aux2 + " " + saux + ".pdf";

                    String pdf = aux2;

                    File pdfFx = new File(aux2);

                    Object[] optionsNew = { "1 column", "2 column", "3 column" /*
                                                                                * , "Using XSL-FO"
                                                                                */};
                    int choNew = JOptionPane.showOptionDialog(produceReport, "Choose one to continue", "Choose",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optionsNew, optionsNew[2]);

                    if (choNew == 0 || choNew == 1 || choNew == 2) {
                        ret = GeneratorChecklistPDF.generateReport(checklist, pdfFx, (short) (choNew + 1));
                    }

                    if (ret) {
                        JOptionPane.showMessageDialog(produceReport, "PDF created with success to file \"" + pdf
                                + "\".");

                        try {
                            if (OsInfo.getName() == OsInfo.Name.WINDOWS)
                                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + pdf);
                            else {
                                String[] readers = { "acroread", "xpdf" };
                                String reader = null;

                                for (int count = 0; count < readers.length && reader == null; count++)
                                    if (Runtime.getRuntime().exec(new String[] { "which", readers[count] }).waitFor() == 0)
                                        reader = readers[count];
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
                    else {
                        JOptionPane
                                .showMessageDialog(produceReport, "<html>PDF <b>was not</b> created to file.</html>");
                    }
                }
            });
            produceReport.setToolTipText("Produce PDF Report");
            produceReport.setText("Produce PDF Report");

            JButton runUnCheck = new JButton("run Uncheck items");
            runUnCheck = new JButton(new ImageIcon(ImageUtils.getScaledImage("images/buttons/rev.png", 16, 16)));
            runUnCheck.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    skipGroup.setEnabled(true);
                    skipItem.setEnabled(true);
                    forceAdvanceItem.setEnabled(true);
                    currentItemText.setEnabled(true);

                    currentItem = 0;
                    currentGroup = 0;
                    chronometer.resume();
                    closeCurrentItem();

                    if (getCurrentItem() != null)
                        if (getCurrentItem().isChecked() == false)
                            loadCurrent();
                        else
                            advance();

                }
            });
            runUnCheck.setToolTipText("Re Check");
            runUnCheck.setText("Run again on unchecked Items");
            runUnCheck.setMargin(new java.awt.Insets(20, 2, 20, 2));

            JPanel panelAux = new JPanel();

            panelAux.setLayout(new BoxLayout(panelAux, BoxLayout.Y_AXIS));

            if (checkFlag == false) {
                panelAux.add(runUnCheck);
            }
            panelAux.add(produceReport);
            panelAux.add(close);
            JLabel msg = new JLabel("");
            msg.setHorizontalAlignment(JLabel.CENTER);
            if (checkFlag == true) {
                msg.setText("Check Ok   ");
                msg.setForeground(new Color(0, 128, 0));
            }
            else {
                msg.setText("Check Incomplete   ");
                msg.setForeground(Color.RED);
            }

            msg.setFont(new Font("Arial", Font.BOLD, 30));

            endPanel.add(msg, BorderLayout.CENTER);
            endPanel.add(panelAux, BorderLayout.SOUTH);

        }
        return endPanel;

    }

    private void end() {
        chronometer.pause();
        skipGroup.setEnabled(false);

        skipItem.setEnabled(false);
        forceAdvanceItem.setEnabled(false);
        forceRetreatItem.setEnabled(false);
        currentItemText.setEnabled(false);

        currentItemText.setText("Time :" + chronometer.getFormattedTime());

        this.listSubItems.add(getEndPanel());
        repaintCheck();

        NeptusLog.pub().info("<###>Check List Ended");
    }

    private void skipItem() {
        getCurrentItem().setSkiped(true);
        advance();
    }

    private void skipGroup() {
        // ciclo para skip group

        // getCurrentItem().setSkiped(true);
        // getCurrentGroup().
        LinkedList<CheckItem> list = checklist.getGroupList().get(getCurrentGroup());
        for (CheckItem ci : list) {
            ci.setChecked(true);
        }
        currentItem = getCurrentItemLength();
        advance();
    }

    private void forceAdvanceItem() {
        checkFlag = false;
        getCurrentItem().setSkiped(false);
        advance();
    }

    private void forceRetreatItem() {
        // checkFlag = false;
        getCurrentItem().setSkiped(false);
        Retreat();
    }

    public void repaintCheck() {
        listSubItems.doLayout();
        listSubItems.invalidate();
        listSubItems.validate();
        Component cmp = listSubItems.getParent();
        while (cmp != null) {
            cmp.doLayout();
            cmp.invalidate();
            cmp.validate();
            cmp = cmp.getParent();
        }
        listSubItems.repaint();
    }

    public String getCurrentGroup() {
        if (checklist.isFlat()) {
            return ChecklistType.FLAT_ID;
        }
        else {
            return (String) checklist.getGroupList().keySet().toArray()[currentGroup];
        }
    }

    public int getGroupLength() {
        if (checklist.isFlat()) {
            return 1;
        }
        else {
            return checklist.getGroupList().keySet().toArray().length;
        }
    }

    public CheckItem getCurrentItem() {
        String currGroup = getCurrentGroup();

        NeptusLog.pub().info("<###>CurrentGroup : " + currentGroup + "   item :" + currentItem);
        NeptusLog.pub().info("<###>getCurrentGroup() : " + getCurrentGroup());
        NeptusLog.pub().info("<###>checklist.getGroupList() : " + checklist.getGroupList());
        NeptusLog.pub().info("<###>(CheckItem) checklist.getGroupList().get(currGroup) : " + checklist.getGroupList());

        NeptusLog.pub().info("<###>contain: : " + checklist.getGroupList().containsKey(currGroup));

        NeptusLog.pub().info("<###>contain: : " + checklist.getGroupList().keySet().toArray()[0]);

        CheckItem ci = (CheckItem) checklist.getGroupList().get(currGroup).toArray()[currentItem];
        // ci.getAutoSubItems().toArray()[0].
        return ci;
    }

    public int getCurrentItemLength() {
        String currGroup = getCurrentGroup();

        return checklist.getGroupList().get(currGroup).toArray().length;
        // ci.getAutoSubItems().toArray()[0].

    }

    @Override
    public void checkSubItemChange(CheckSubItemExe cbie) {
        // System.err.println("Fui avisado");

        checkedAdvance();
    }

    // public static void main(String[] args) {
    // ConfigFetch.initialize();
    // GuiUtils.setLookAndFeel();
    // ChecklistType cl = new ChecklistType("c:/teste27.nchk");
    // CheckListExe cexe = new CheckListExe(null, cl, null, ".");
    // cexe.setVisible(true);
    // cexe.setSize(400, 500);
    // }

}
