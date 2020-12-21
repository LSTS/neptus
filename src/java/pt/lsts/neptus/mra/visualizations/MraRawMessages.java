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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 30, 2014
 */
package pt.lsts.neptus.mra.visualizations;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.jdesktop.swingx.JXBusyLabel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.Announce;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.gui.Java2sAutoTextField;
import pt.lsts.neptus.util.llf.MraMessageLogTablePopupMenu;
import pt.lsts.neptus.util.llf.MessageHtmlVisualization;
import pt.lsts.neptus.util.llf.RawMessagesTableModel;
import pt.lsts.neptus.util.llf.SortedComboBoxModel;


/**
 * @author zp
 * @author Manuel Ribeiro
 */

@PluginDescription(icon = "pt/lsts/neptus/mra/visualizations/doc-search.png")
public class MraRawMessages extends SimpleMRAVisualization {

    private static final long serialVersionUID = 1L;
    private static final String ANY_TXT = I18n.text("<ANY>");
    private static final String SHOW_ICON = "images/buttons/show.png";
    private static final String LIGHTS_ICON = "images/buttons/lights.png";
    private static InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("");
    private JTable table;
    private ArrayList<Integer> resultList = new ArrayList<>();
    private int finderNextIndex = -1;
    private int currFinderIndex = -1;
    private FinderDialog find = null;
    private boolean findOpenState = false;
    private boolean closingUp = false;
    private boolean finished = false;
    private AbstractAction finderAction = null;
    private JToggleButton highlightBtn;

    public MraRawMessages(MRAPanel panel) {
        super(panel);
    }

    @Override
    public Type getType() {
        return Type.TABLE;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String getName() {
        return I18n.text("All Messages");
    }

    @Override
    public void onHide() {
        mraPanel.getActionMap().remove("finder");
        if (find != null)
            find.setVisible(false);
    }

    @Override
    public void onShow() {
        mraPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
        mraPanel.getActionMap().put("finder", finderAction);
        if (findOpenState && find != null) {
            find.setVisible(true);
        }
    }

    @Override
    public void onCleanup() {
        super.onCleanup();
        closingUp = true;
        if (find != null)
            find.close();
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        final LsfIndex index = source.getLsfIndex();
        table = new JTable(new RawMessagesTableModel(index));
        Color defColor = table.getSelectionBackground();
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        contentPane.add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout());

        JPanel panel1 = new JPanel();
        panel.add(panel1, BorderLayout.EAST);
        panel1.setLayout(new BorderLayout(2, 0));
        panel1.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        finderAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (find == null)
                    find = new FinderDialog(SwingUtilities.windowForComponent(mraPanel));
                else {
                    find.busyLbl.setBusy(false);
                    find.busyLbl.setVisible(false);
                    find.setLocationOnLeft();
                    find.setVisible(true);
                    find.pack();
                    findOpenState = true;
                }
            }
        };

        mraPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
        mraPanel.getActionMap().put("finder", finderAction);

        highlightBtn = new JToggleButton(I18n.text("Highlight"));
        highlightBtn.setIcon(ImageUtils.createScaleImageIcon(LIGHTS_ICON, 13, 13));
        highlightBtn.setSelected(false);
        highlightBtn.setToolTipText(I18n.text("Highlight all occurrences"));
        highlightBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (find != null)
                    find.toggleHighlight();
            }
        });

        JButton findBtn = new JButton(I18n.text("Find"));
        findBtn.setIcon(ImageUtils.createScaleImageIcon(SHOW_ICON, 13, 13));
        findBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (find == null) {
                    find = new FinderDialog(SwingUtilities.windowForComponent(mraPanel));
                }

                if (!find.isVisible()) {
                    find.setLocationOnLeft();
                    find.setVisible(true);
                    find.pack();
                    closingUp = false;
                    findOpenState = true;
                }
                else {
                    findOpenState = false;
                    find.setVisible(false);
                    find.dispose();
                }
            }
        });

        panel1.add(highlightBtn, BorderLayout.WEST);
        panel1.add(findBtn, BorderLayout.EAST);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // currSelectedIndex = table.getSelectedRow();
                table.setSelectionBackground(defColor);
                if (find != null)
                    find.setHighlighted(false);
                highlightBtn.setSelected(false);

                if (e.getClickCount() == 2) {
                    mraPanel.loadVisualization(new MessageHtmlVisualization(index.getMessage(table.getSelectedRow())),
                            true);
                }
                else if(e.getButton() == MouseEvent.BUTTON3) {

                    Point point = e.getPoint();
                    int selRow = MraMessageLogTablePopupMenu.setRowSelection(table, point);
                    MraMessageLogTablePopupMenu.setAddMarkMenu(mraPanel, table, 
                            index.getMessage(selRow), point);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        find = new FinderDialog(SwingUtilities.windowForComponent(mraPanel));

        return contentPane;
    }

    /**
     * Checks if there are messages in the list that have a specific type, source, source_entity, 
     * destination and are within specified time limits 
     * @param src, TYPE of the message
     * @param srcEnt, SOURCE_ENTITY of the message
     * @return true if there is at least one element, false otherwise
     */
    private boolean findMessage(String type, String src, String srcEnt, Date time1, Date time2) {
        closingUp = false;
        finished = false;
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        find.busyLbl.setBusy(true);
        find.busyLbl.setVisible(true);
        long t1 = (long) time1.getTime() / 1000;
        long t2 = (long) time2.getTime() / 1000;

        find.validateTimestamp(t1, t2);

        String rowType = null;
        String rowSrc = null;
        String rowSrcEnt = null;

        if (type.equals(ANY_TXT) && src.equals(ANY_TXT) && 
                srcEnt.equals(ANY_TXT) && find.hasDefaultTS(t1, t2)) {
            find.busyLbl.setBusy(false);
            find.busyLbl.setVisible(false);
            find.nextBtn.setEnabled(false);
            find.prevBtn.setEnabled(false);

            return true;
        }

        int first = source.getLsfIndex().getFirstMessageAtOrAfter(t1);
        int indexFirst = findFirstOcc(first, source.getLsfIndex().getNumberOfMessages(), t1, type);

        // if there isn't at least one result...
        if (indexFirst == -1) {
            find.busyLbl.setBusy(false);
            find.busyLbl.setVisible(false);
            find.nextBtn.setEnabled(false);
            find.prevBtn.setEnabled(false);
            return false;
        }

        int low = 0;
        int high = source.getLsfIndex().getNumberOfMessages() - 1;
        int mid = -1;
        int last = high;

        while (low <= high) {
            if (closingUp) {
                find.nextBtn.setEnabled(false);
                find.prevBtn.setEnabled(false);
                break;
            }

            mid = high - (high - low) / 2;
            long rowTime2 = (long) source.getLsfIndex().timeOf(mid);
            rowType = source.getLsfIndex().getDefinitions().getMessageName(source.getLsfIndex().typeOf(mid));
            rowSrc = source.getLsfIndex().sourceNameOf(mid);

            if (rowTime2 > t2) {
                high = mid - 1;
            } else if (rowTime2 < t2) {
                low = mid + 1;
            }
            else {
                last = high;
                break;
            }
        }
        int indexLast = last;
        int total = indexLast - indexFirst;
        int count = 0;

        for (int row = indexFirst; row < indexLast; row++) {
            if (closingUp) {
                find.clear();
                return true;
            }

            long rowTime = (long) source.getLsfIndex().timeOf(row); //Time
            rowType = source.getLsfIndex().getDefinitions().getMessageName(source.getLsfIndex().typeOf(row)); //Type
            rowSrc = source.getLsfIndex().sourceNameOf(row);  //Source
            rowSrcEnt = source.getLsfIndex().entityNameOf(row); //SourceEntity
            if (rowSrcEnt == null || rowSrcEnt.isEmpty())
                rowSrcEnt = "empty";

            if (rowType.equals(type) || type.equals(ANY_TXT))
                if (rowSrc.equals(src) || src.equals(ANY_TXT))
                    if (rowSrcEnt.equals(srcEnt) || srcEnt.equals(ANY_TXT))
                        if ((rowTime >= t1) && (rowTime <= t2))
                            resultList.add(row);

            count++;
            long state = (long) count * 100 / total;
            find.statusLbl.setText(state+"%");
        }

        find.busyLbl.setBusy(false);
        find.busyLbl.setVisible(false);
        find.statusLbl.setVisible(false);

        if (!resultList.isEmpty()) {
            table.clearSelection();
            table.setSelectionBackground(Color.yellow);
            table.addRowSelectionInterval(resultList.get(0), resultList.get(0));
            table.scrollRectToVisible(new Rectangle(table.getCellRect(resultList.get(0), 0, true)));
            table.repaint();
            finderNextIndex++;
            finished = true;
            return true;
        }

        find.nextBtn.setEnabled(false);
        find.prevBtn.setEnabled(false);
        return false;
    }

    private int findFirstOcc(int first, int last, long t1, String type) {
        for (int row = first; row < last; row++) {
            long rowTime = (long) source.getLsfIndex().timeOf(row);
            String rowType = source.getLsfIndex().getDefinitions().getMessageName(source.getLsfIndex().typeOf(row)); //Type

            if ((rowTime >= t1) && (rowType.equals(type) || type.equals(ANY_TXT)))
                return row;

            if (closingUp)
                break;
        }
        return -1;
    }

    private void highLightRow() {
        if (highlightBtn.isSelected())
            find.toggleHighlight();

        currFinderIndex = resultList.get(finderNextIndex);
        table.scrollRectToVisible(new Rectangle(table.getCellRect(currFinderIndex, 0, true)));
        table.clearSelection();
        table.addRowSelectionInterval(currFinderIndex, currFinderIndex);
        table.setSelectionBackground(Color.yellow);
    }

    /**
     * Checks if there's a previous element to be show 
     * @return true if there is, false otherwise
     */
    private boolean hasPrev() {
        //to allow going to last result if we are at the first one 
        if (finderNextIndex == 0) {
            finderNextIndex = resultList.size() - 1;
            highLightRow();
            return true;
        } else {
            finderNextIndex = finderNextIndex - 1 ;
            highLightRow();
            return true;
        }
    }

    /**
     * Checks if there's a next element to be show 
     * @return true if there is, false otherwise
     */
    private boolean hasNext() {
        if (finderNextIndex >= resultList.size() - 1) {
            finderNextIndex = 0;
            highLightRow();
            return true;
        } 
        else {
            finderNextIndex++;
            highLightRow();
            return true;
        }
    }

    /** 
     * Clears every result found and table selection 
     */
    private void clearSelection() {
        currFinderIndex = -1;
        finderNextIndex = -1;
        resultList.clear();
        table.clearSelection();
        table.repaint();
    }

    private class FinderDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private Java2sAutoTextField typeTxt;
        private JComboBox<String> sourceCBox, sourceEntCBox;
        private JSpinner timestampLow, timestampHigh;
        private long defTimestampLow, defTimestampHigh;
        private JButton prevBtn, nextBtn, findBtn, resetBtn;
        private JLabel statusLbl;
        private JXBusyLabel busyLbl;
        private boolean hightlighted = false;
        private Window parent;

        public FinderDialog(Window parent) {
            super(parent, ModalityType.MODELESS);
            this.parent = parent;

            initComponents();
        }

        /** 
         * Checks if timestampLow and timestampHigh have default time values
         * @return true, if both have default values
         */
        public boolean hasDefaultTS(long t1, long t2) {
            if (t1 == defTimestampLow && t2 == defTimestampHigh)
                return true;

            return false;
        }

        /** 
         * Positions find dialog window on the left side of the screen
         * 
         */
        public void setLocationOnLeft() {
            setLocationRelativeTo(null);
            setLocation(parent.getLocation().x, getLocation().y);
        }

        /** 
         * Sets timestampLow and timestampHigh box's with a date 
         *   and also sets this values as default ones
         * @param t1, date to be used for timestampLow box
         * @param t2, date to be used for timestampHigh box
         */
        private void setTimestamp(Date t1, Date t2) {
            timestampLow.setValue(t1);
            timestampHigh.setValue(t2);
            defTimestampLow = (long) source.getLsfIndex().timeOf(0) ;
            defTimestampHigh = (long) source.getLsfIndex().timeOf(source.getLsfIndex().getNumberOfMessages() - 1);
        }

        /**
         * Highlights found rows or clears every highlighted one
         */
        private void toggleHighlight() {
            if (!hightlighted) {
                if (!resultList.isEmpty()) {
                    table.clearSelection();
                    table.setSelectionBackground(Color.yellow);
                    for (Integer row : resultList) {
                        table.addRowSelectionInterval(row, row);
                    }
                    hightlighted = true;
                    highlightBtn.setSelected(hightlighted);
                }
            }
            else {
                table.clearSelection();
                hightlighted = false;
                highlightBtn.setSelected(hightlighted);
            }
        }

        /**
         * Turn this frame invisible and dispose
         */
        private void close() {
            setVisible(false); //you can't see me!
            dispose();
        }

        private void initComponents() {
            setIconImage(MraRawMessages.this.getIcon().getImage());
            setTitle(I18n.text("Find"));
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);
            setLocationOnLeft();

            getContentPane().setLayout(new MigLayout("", "[][grow]", "[][][][][][]"));

            new Thread(new LoadComponentsThread()).start();

        }

        private class LoadComponentsThread implements Runnable {

            @Override
            public void run() {

                setSize(290,270);
                getContentPane().add(loader, "cell 0 2 3 1,grow");
                loader.setText(I18n.text("Initializing Find"));
                loader.setOpaque(false);
                loader.start();

                SortedComboBoxModel<String> model1 = new SortedComboBoxModel<String>(new String[] {ANY_TXT});
                SortedComboBoxModel<String> model2 = new SortedComboBoxModel<String>(new String[] {ANY_TXT});

                JLabel typeLabel = new JLabel(I18n.text("Type"));
                JLabel sourceLabel = new JLabel(I18n.text("Source"));
                JLabel sourceEntCLabel = new JLabel(I18n.textc("Src. Ent", "Source Entity"));
                sourceCBox = new JComboBox<String>(model1);
                sourceEntCBox = new JComboBox<String>(model2);
                JLabel timeLbl = new JLabel(I18n.text("Time"));
                timestampLow = new JSpinner( new SpinnerDateModel() );
                JLabel separatorLbl = new JLabel("-");
                timestampHigh = new JSpinner( new SpinnerDateModel() );

                JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timestampLow, "yyyy-MM-dd HH:mm:ss");
                timeEditor.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
                timestampLow.setEditor(timeEditor);
                timestampLow.setUI(new javax.swing.plaf.basic.BasicSpinnerUI(){
                    protected Component createNextButton(){
                        Component c = new JButton();
                        c.setPreferredSize(new Dimension(0,0));
                        return c;
                    }
                    protected Component createPreviousButton(){
                        Component c = new JButton();
                        c.setPreferredSize(new Dimension(0,0));
                        return c;
                    }
                });
                timestampLow.setBorder(null);

                JTextField tf = ((JSpinner.DefaultEditor)timestampLow.getEditor()).getTextField();
                ((JComponent)tf.getParent()).setBorder(BorderFactory.createLineBorder(Color.GRAY));

                JSpinner.DateEditor timeEditor2 = new JSpinner.DateEditor(timestampHigh, "yyyy-MM-dd HH:mm:ss");
                timeEditor2.getFormat().setTimeZone(TimeZone.getTimeZone("UTC"));
                timestampHigh.setEditor(timeEditor2);
                timestampHigh.setUI(new javax.swing.plaf.basic.BasicSpinnerUI(){
                    protected Component createNextButton(){
                        Component c = new JButton();
                        c.setPreferredSize(new Dimension(0,0));
                        return c;
                    }
                    protected Component createPreviousButton(){
                        Component c = new JButton();
                        c.setPreferredSize(new Dimension(0,0));
                        return c;
                    }
                });
                timestampHigh.setBorder(null);
                JTextField tf1 = ((JSpinner.DefaultEditor)timestampHigh.getEditor()).getTextField();
                ((JComponent)tf1.getParent()).setBorder(BorderFactory.createLineBorder(Color.GRAY));

                prevBtn = new JButton(I18n.textc("Prev.", "Previous"));
                findBtn = new JButton(I18n.text("Find"));
                nextBtn = new JButton(I18n.text("Next"));
                resetBtn = new JButton(I18n.text("Reset"));

                nextBtn.setEnabled(false);
                prevBtn.setEnabled(false);
                resetBtn.setEnabled(true);
                statusLbl = new JLabel();

                busyLbl = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(18);
                busyLbl.setBusy(false);
                busyLbl.setVisible(false);

                Font defFont = new Font("Dialog", Font.BOLD, 11);
                typeLabel.setFont(defFont);
                sourceLabel.setFont(defFont);
                sourceCBox.setFont(defFont);
                sourceEntCLabel.setFont(defFont);
                sourceEntCBox.setFont(defFont);
                timeLbl.setFont(defFont);
                separatorLbl.setFont(defFont);
                statusLbl.setFont(defFont);

                prevBtn.setHorizontalAlignment(SwingConstants.RIGHT);
                nextBtn.setHorizontalAlignment(SwingConstants.RIGHT);
                findBtn.setHorizontalAlignment(SwingConstants.RIGHT);
                resetBtn.setHorizontalAlignment(SwingConstants.RIGHT);

                ArrayList<String> typeList = new ArrayList<>();

                Date dtX = parseDate(0); //get first timestamp from table
                Date dtY = parseDate(table.getRowCount() - 1); //get last timestamp from table

                setTimestamp(dtX, dtY);

                //populate source combobox
                for (Announce a : source.getLsfIndex().getAvailableSystems())
                    addItemToBox(sourceCBox, a.getSysName());

                //add types to list
                for (String type : source.listLogs()) 
                    if (!typeList.contains(type))
                        typeList.add(type);

                int total = source.getLsfIndex().getNumberOfMessages();
                long state = 0;
                for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
                    if (closingUp)
                        break;

                    String srcEntity = source.getLsfIndex().entityNameOf(row); //SourceEntity

                    //populate source_entity combobox
                    addItemToBox(sourceEntCBox, srcEntity);

                    state = (long) row * 100;
                    loader.setText("Initializing Find ("+state/total+"%)");
                }

                typeList.add(ANY_TXT);
                Collections.sort(typeList, String.CASE_INSENSITIVE_ORDER);

                sourceEntCBox.setSelectedItem(ANY_TXT);
                loader.stop();
                getContentPane().remove(loader);

                //populate typeList textfield
                initTypeField(typeList);
                getContentPane().add(typeLabel, "cell 0 0,alignx trailing");
                getContentPane().add(typeTxt, "cell 1 0,growx");
                getContentPane().add(sourceLabel, "cell 0 1,alignx trailing");
                getContentPane().add(sourceCBox, "cell 1 1,growx");
                getContentPane().add(sourceEntCLabel, "cell 0 2,alignx trailing");
                getContentPane().add(sourceEntCBox, "cell 1 2,growx");
                getContentPane().add(timeLbl, "cell 0 3,alignx trailing");
                getContentPane().add(timestampLow, "flowx,cell 1 3");
                getContentPane().add(separatorLbl, "cell 1 3");
                getContentPane().add(timestampHigh, "flowx,cell 1 3");
                getContentPane().add(resetBtn, "cell 1 3,alignx right");

                JPanel panel = new JPanel();
                getContentPane().add(panel, "cell 0 4 2 1,grow");
                panel.setLayout(new BorderLayout());

                statusLbl.setHorizontalAlignment(SwingConstants.RIGHT);
                panel.add(statusLbl);

                JPanel bottomPanel = new JPanel();
                panel.add(bottomPanel, BorderLayout.EAST);
                panel.add(busyLbl, BorderLayout.WEST);

                bottomPanel.add(prevBtn);
                bottomPanel.add(nextBtn);
                bottomPanel.add(findBtn);

                resetBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        typeTxt.setText(ANY_TXT);
                        sourceCBox.setSelectedItem(ANY_TXT);
                        sourceEntCBox.setSelectedItem(ANY_TXT);
                        prevBtn.setEnabled(false);
                        nextBtn.setEnabled(false);
                        timestampLow.setValue(parseDate(0));
                        timestampHigh.setValue(parseDate(table.getRowCount() - 1));
                    }
                });

                prevBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        hasPrev();
                        updateStatus();
                    }
                });

                nextBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        hasNext();
                        updateStatus();                    
                    }
                });

                findBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        clear();
                        closingUp = true;
                        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                            @Override
                            protected Boolean doInBackground() throws Exception {
                                Date t1 = (Date) timestampLow.getValue();
                                Date t2 = (Date) timestampHigh.getValue();

                                boolean found = findMessage(typeTxt.getText(), (String) sourceCBox.getSelectedItem(), 
                                        (String)sourceEntCBox.getSelectedItem(), t1, t2);

                                return found;
                            }

                            @Override
                            protected void done() {
                                try {
                                    boolean found = get();
                                    if (found) {
                                        if (!resultList.isEmpty() && finished) {
                                            nextBtn.setEnabled(true);
                                            prevBtn.setEnabled(true);
                                        }

                                        updateStatus();
                                    } 
                                    else {
                                        GuiUtils.errorMessage(FinderDialog.this, I18n.text("Find"), 
                                                "No message with current selected filter has been found.");
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        worker.execute();
                    }
                });

                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        findOpenState = false;
                    }
                });

                requestFocus();
                getRootPane().setDefaultButton(findBtn);
                pack();
            }
        }

        private Date parseDate(int row){
            String t1 = (String) table.getValueAt(row, 1);
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date dtX = null;

            try {
                dtX = format.parse(t1);
            }
            catch (ParseException e) {
                e.printStackTrace();
            }

            return dtX;
        }

        private void validateTimestamp(long d1, long d2) {
            if (d1 < defTimestampLow)
                timestampLow.setValue(parseDate(0));

            if (d2 > defTimestampHigh)
                timestampHigh.setValue(parseDate(table.getRowCount() - 1));

            if (d1 > d2) { 
                timestampLow.setValue(parseDate(0));
                timestampHigh.setValue(parseDate(table.getRowCount() - 1));
            }
        }

        private void clear() {
            clearSelection();
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            statusLbl.setText("");
            statusLbl.setVisible(true);
            highlightBtn.setSelected(false);
        }

        /**
         *  Updates status label with number of found elements 
         */
        private void updateStatus() {
            if (resultList.size() != 0) {
                statusLbl.setVisible(true);
                statusLbl.setText(finderNextIndex+1 + " of "+resultList.size());
                find.pack();
            }
        }

        /**
         *  Setup a special JTextField (called Java2sAutoTextField) that has autocomplete feature
         * @param items, list of items that will be suggested in the autocomplete feature
         */
        private void initTypeField(ArrayList<String> items) {
            typeTxt = new Java2sAutoTextField(items);
            typeTxt.setFont(new Font("Dialog", Font.PLAIN, 11));
            typeTxt.setColumns(10);
            typeTxt.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        typeTxt.selectAll();
                    }
                }
            });
            typeTxt.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    findBtn.doClick();
                }});
        }

        /**
         *  Adds items to a combobox validating them first
         * @param box, box to be populated
         * @param item, item to be added to box contents.
         */
        private void addItemToBox(JComboBox<String> box, String item) {
            if (((DefaultComboBoxModel<String>) box.getModel()).getIndexOf(item) == -1 && item != null) {
                box.addItem((String) item);
            }
        }

        private void setHighlighted(boolean value) {
            hightlighted = value;
        }
    }
}