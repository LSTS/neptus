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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 30, 2014
 */
package pt.lsts.neptus.mra.visualizations;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

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
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.gui.Java2sAutoTextField;
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
    private static final int MAX_TIME_DEVIATION = 2; //2 seconds
    private JTable table;
    private ArrayList<Integer> resultList = new ArrayList<>();
    private int finderNextIndex = -1;
    private int currFinderIndex = -1;
    private FinderDialog find = null;
    private boolean findOpenState = false;
    private AbstractAction finderAction;
    private JToggleButton highlightBtn;
    private boolean closingUp = false;

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
        find.setVisible(false);
    }

    @Override
    public void onShow() {
        mraPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
        mraPanel.getActionMap().put("finder", finderAction);
        if (findOpenState)
            find.setVisible(true);
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

        find = setupFinder();

        finderAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                find.setLocationOnLeft();
                find.setVisible(true);
                findOpenState = true;
            }
        };

        mraPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
        mraPanel.getActionMap().put("finder", finderAction);

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

        highlightBtn = new JToggleButton();
        highlightBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        highlightBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        highlightBtn.setIcon(ImageUtils.createScaleImageIcon("images/buttons/lights.png", 13, 13));
        highlightBtn.setSelected(false);
        highlightBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                find.toggleHighlight();
            }
        });

        panel1.add(highlightBtn, BorderLayout.EAST);

        JButton findBtn = new JButton();
        findBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        findBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        findBtn.setIcon(ImageUtils.createScaleImageIcon("images/buttons/show.png", 13, 13));
        findBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!find.isVisible()) {
                    find.setVisible(true);
                    closingUp = false;
                }
                else {
                    find.setVisible(false);
                    closingUp = true;
                    find.dispose();
                }
            }
        });

        panel1.add(findBtn, BorderLayout.WEST);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // currSelectedIndex = table.getSelectedRow();
                table.setSelectionBackground(defColor);
                find.setHighlighted(false);
                highlightBtn.setSelected(false);

                if (e.getClickCount() == 2) {
                    mraPanel.loadVisualization(new MessageHtmlVisualization(index.getMessage(table.getSelectedRow())),
                            true);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        return contentPane;
    }

    /**
     * Compares two Date objects, evaluating by a specific operator.
     * @param date1, the date to check against the base date
     * @param operator
     * @param base, the base date
     */
    private static boolean compareTime(Date date1, String operator, Date base)
    {
        if (base == null)
        {
            return false;
        }
        try
        {
            SimpleDateFormat parser = new SimpleDateFormat("HH:mm:ss");
            base = parser.parse(parser.format(base));
            date1 = parser.parse(parser.format(date1));
        }
        catch (ParseException ex)
        {
            ex.printStackTrace();
        }
        switch (operator)
        {
            case "==":
                return date1.compareTo(base) == 0;
            case "<":
                return date1.compareTo(base) < 0;
            case ">":
                return date1.compareTo(base) > 0;
            case "<=":
                return date1.compareTo(base) <= 0;
            case ">=":
                return date1.compareTo(base) >= 0;
            default:
                throw new IllegalArgumentException();

        }
    }
    private static Date parseTime(String time) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        Date dateTime = null;
        try {
            dateTime = parser.parse(time);
        }
        catch (ParseException e) {
            return null;
        }

        return dateTime;
    }
    /**
     * Checks if there are messages in the list that have a specific type, source, source_entity, 
     * destination and are within specified time limits 
     * @param src, TYPE of the message
     * @param srcEnt, SOURCE_ENTITY of the message
     * @return true if there is at least one element, false otherwise
     */
    private boolean findMessage(String type, String src, String srcEnt, String dest, Date time1, Date time2) {
        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        find.busyLbl.setBusy(true);
        find.busyLbl.setVisible(true);

        if (type.equals(ANY_TXT) && src.equals(ANY_TXT) && srcEnt.equals(ANY_TXT) 
                && dest.equals(ANY_TXT) && find.hasDefaultTS()) {
            find.busyLbl.setBusy(false);
            find.busyLbl.setVisible(false);
            return true;
        }
        
        int low = 0;
        int mid = -1;
        int high = table.getRowCount() - 1;
        String rowTime = null;
        String rowType = null;
        String rowSrc = null;

        int last = high - (high - low) / 2;
        int first = 0;
        Date parsedTime = null;

        while(low <= high) {
            if (closingUp)
                break;
            mid = high - (high - low) / 2;
            rowTime = (String) table.getValueAt(mid, 1); //Time

            parsedTime = parseTime(rowTime);

            if (compareTime(parsedTime, ">", time1)) {
                high = mid - 1;
                // System.out.println("["+mid+"] parsedTime:"+ parsedTime  + " > time: "+time1 );
            } 
            else if (compareTime(parsedTime, "<", time1)) {
                first = mid;
                low = mid + 1;
                // System.out.println("["+mid+"] parsedTime:"+ parsedTime  + " < time: "+time1 );
            }
            else {
                // System.out.println("["+mid+"] parsedTime:"+ parsedTime  + " == time: "+time1 );
                last = mid;
              //  System.out.println("first: "+first);
                if (first == 0) {
                    first = last;
                    last = high;
                    // System.out.println("first: "+first + " last:"+high);
                }
                break;
            }

        }


        int indexFirst = findFirstOcc(first, table.getRowCount() / 2, time1, type);
        //  System.out.println("indexFirst: " + indexFirst);
        if (indexFirst == -1) {
            indexFirst = table.getRowCount() / 2;
        }

        low = table.getRowCount() / 2;
        high = table.getRowCount() - 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time2);
        int seconds = calendar.get(Calendar.SECOND);
        // we add MAX_TIME_DEVIATION seconds because of unsorted Time column
        calendar.set(Calendar.SECOND, seconds+MAX_TIME_DEVIATION);
        Date advT2 = calendar.getTime();
        last = high;

        while(low <= high) {
            if (closingUp)
                break;

            mid = high - (high - low) / 2;
            rowTime = (String) table.getValueAt(mid, 1); //Time
            rowType = (String) table.getValueAt(mid, 2); //Type
            rowSrc = (String) table.getValueAt(mid, 3); //Source
            parsedTime = parseTime(rowTime);

            if (compareTime(parsedTime, ">", advT2))
                high = mid - 1;
            else if (compareTime(parsedTime, "<", advT2)) 
                low = mid + 1;
            else {
                last = mid;
                break;
            }
        }

        int indexLast = last;
        //System.out.println("indexLast: " + indexLast);
        int total = indexLast - indexFirst;
        //System.out.println("total: " + total);
        int count=0;
        for (int row = indexFirst; row < indexLast; row++) {
            count++;
            rowTime = (String) table.getValueAt(row, 1); //Time
            rowType = (String) table.getValueAt(row, 2); //Type
            rowSrc = (String) table.getValueAt(row, 3); //Source
            String rowSrcEnt = (String) table.getValueAt(row, 4); //SourceEntity
            String rowDest = (String) table.getValueAt(row, 5); //Destination
            if (rowSrcEnt == null || rowSrcEnt.isEmpty())
                rowSrcEnt = "empty";

            Date tableTime = parseTime(rowTime);
            if (tableTime == null) {
                NeptusLog.pub().warn("Unable to parse timestamp from message list.");
                continue;
            }

            if (rowType.equals(type) || type.equals(ANY_TXT)) {
                if (rowSrc.equals(src) || src.equals(ANY_TXT)) {
                    if (rowSrcEnt.equals(srcEnt) || srcEnt.equals(ANY_TXT)) {
                        if (rowDest.equals(dest) || dest.equals(ANY_TXT) || 
                                (rowDest.contains("null") && dest.equals("UNADDRESSABLE"))){
                            if (compareTime(tableTime, ">=", time1) && compareTime(tableTime, "<=", time2))
                                resultList.add(row);
                        }
                    }
                }
            }
            int state = (count * 100) / total;
            find.statusLbl.setText(state+"%");
            find.repaint();
            if (closingUp)
                break;
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
            return true;
        }

        return false;
    }

    private int findFirstOcc(int first, int last, Date time1, String type) {
        if (first == last)
            last = table.getRowCount() / 2;
        for (int row = first; row < last; row++) {
            String rowTime = (String) table.getValueAt(row, 1); //Time
            String rowType = (String) table.getValueAt(row, 2); //Type
            Date parsedTime = parseTime(rowTime);

            if (compareTime(parsedTime, ">=", time1) && (rowType.equals(type) || type.equals(ANY_TXT))) {
                return row;
            }
        }
        return -1;
    }

    private void highLightRow() {
        /*
         FIXME: when pressing hasPrev / hasNext, clear hightlight button state !!!
         */
        currFinderIndex = resultList.get(finderNextIndex);
        // currSelectedIndex = currFinderIndex;

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
        } else {
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

    private FinderDialog setupFinder(){
        FinderDialog find = new FinderDialog(SwingUtilities.windowForComponent(this.mraPanel));
        ArrayList<String> typeList = new ArrayList<>();

        for (int row = 0; row < table.getRowCount(); row++) {
            String type = (String) table.getValueAt(row, 2); //Type
            String src = (String) table.getValueAt(row, 3); //Source
            String srcEntity = (String) table.getValueAt(row, 4); //SourceEntity
            String dest = (String) table.getValueAt(row, 5); //Destination

            if (!typeList.contains(type))
                typeList.add(type);

            find.addItemToBox(find.sourceCBox, src);
            find.addItemToBox(find.sourceEntCBox, srcEntity);
            if (dest.equals("null")) {
                find.addItemToBox(find.destCBox, "UNADDRESSABLE");
            } 
            else
                find.addItemToBox(find.destCBox, dest);
        }
        typeList.add(ANY_TXT);
        Collections.sort(typeList, String.CASE_INSENSITIVE_ORDER);
        find.initTypeField(typeList);
        find.sourceEntCBox.setSelectedItem(ANY_TXT);

        String t1 = (String) table.getValueAt(0, 1); //get first timestamp from table
        String t2 = (String) table.getValueAt(table.getRowCount() - 1, 1); //get last timestamp from table
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try {
            Date dt1 = format.parse(t1);
            Date dt2 = format.parse(t2);

            find.setTimestamp(dt1, dt2);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return find;
    }

    private class FinderDialog extends JDialog {
        private static final long serialVersionUID = 1L;

        private Java2sAutoTextField typeTxt;
        private JComboBox<String> sourceCBox, sourceEntCBox, destCBox;
        private JSpinner ts1, ts2;
        private Date defTS1, defTS2;
        private JButton prevBtn, nextBtn, findBtn;
        private JLabel statusLbl;
        private JXBusyLabel busyLbl;
        private boolean hightlighted = false;
        private Window parent;

        public FinderDialog(Window parent) {
            super(parent, ModalityType.MODELESS);
            this.parent = parent;
            initComponents();
        }

        public boolean hasDefaultTS() {
            if (compareTime((Date) ts1.getValue(), "==", defTS1) &&
                    compareTime((Date) ts2.getValue(), "==", defTS2))
                return true;
            else
                return false;
        }

        public void setLocationOnLeft() {
            setLocationRelativeTo(null);
            setLocation(parent.getLocation().x, getLocation().y);
        }

        private void setTimestamp(Date t1, Date t2) {
            ts1.setValue(t1);
            ts2.setValue(t2);
            defTS1 = t1;
            defTS2 = t2;
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
            setSize(290, 205);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);
            setLocationOnLeft();

            getContentPane().setLayout(new MigLayout("", "[][grow]", "[][][][][][]"));
            SortedComboBoxModel<String> model1 = new SortedComboBoxModel<String>(new String[] {ANY_TXT});
            SortedComboBoxModel<String> model2 = new SortedComboBoxModel<String>(new String[] {ANY_TXT});
            SortedComboBoxModel<String> model3 = new SortedComboBoxModel<String>(new String[] {ANY_TXT});

            JLabel typeLabel = new JLabel(I18n.text("Type"));
            JLabel sourceLabel = new JLabel(I18n.text("Source"));
            JLabel sourceEntCLabel = new JLabel(I18n.textc("Src. Ent", "Source Entity"));
            sourceCBox = new JComboBox<String>(model1);
            sourceEntCBox = new JComboBox<String>(model2);
            JLabel destLabel = new JLabel(I18n.textc("Dest.", "Destination"));
            destCBox = new JComboBox<String>(model3);
            JLabel timeLbl = new JLabel(I18n.text("Time"));
            ts1 = new JSpinner( new SpinnerDateModel() );
            JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(ts1, "HH:mm:ss");
            ts1.setEditor(timeEditor);
            ts1.setUI(new javax.swing.plaf.basic.BasicSpinnerUI(){
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
            ts1.setBorder(null);
            JTextField tf = ((JSpinner.DefaultEditor)ts1.getEditor()).getTextField();
            ((JComponent)tf.getParent()).setBorder(BorderFactory.createLineBorder(Color.GRAY));

            JLabel separatorLbl = new JLabel("-");
            ts2 = new JSpinner( new SpinnerDateModel() );
            JSpinner.DateEditor timeEditor2 = new JSpinner.DateEditor(ts2, "HH:mm:ss");
            ts2.setEditor(timeEditor2);
            ts2.setUI(new javax.swing.plaf.basic.BasicSpinnerUI(){
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
            ts2.setBorder(null);
            JTextField tf1 = ((JSpinner.DefaultEditor)ts2.getEditor()).getTextField();
            ((JComponent)tf1.getParent()).setBorder(BorderFactory.createLineBorder(Color.GRAY));

            prevBtn = new JButton(I18n.textc("Prev.", "Previous"));
            findBtn = new JButton(I18n.text("Find"));
            nextBtn = new JButton(I18n.text("Next"));
            nextBtn.setEnabled(false);
            prevBtn.setEnabled(false);
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
            destLabel.setFont(defFont);
            destCBox.setFont(defFont);
            timeLbl.setFont(defFont);
            separatorLbl.setFont(defFont);
            statusLbl.setFont(defFont);

            prevBtn.setHorizontalAlignment(SwingConstants.RIGHT);
            nextBtn.setHorizontalAlignment(SwingConstants.RIGHT);
            findBtn.setHorizontalAlignment(SwingConstants.RIGHT);

            getContentPane().add(typeLabel, "cell 0 0,alignx trailing");
            getContentPane().add(sourceLabel, "cell 0 1,alignx trailing");
            getContentPane().add(sourceCBox, "cell 1 1,growx");
            getContentPane().add(sourceEntCLabel, "cell 0 2,alignx trailing");
            getContentPane().add(sourceEntCBox, "cell 1 2,growx");
            getContentPane().add(destLabel, "cell 0 3,alignx trailing");
            getContentPane().add(destCBox, "cell 1 3,growx");
            getContentPane().add(timeLbl, "cell 0 4,alignx trailing");
            getContentPane().add(ts1, "flowx,cell 1 4");
            getContentPane().add(separatorLbl, "cell 1 4");
            getContentPane().add(ts2, "flowx,cell 1 4");

            JPanel panel = new JPanel();
            getContentPane().add(panel, "cell 0 5 2 1,grow");
            panel.setLayout(new BorderLayout());

            statusLbl.setHorizontalAlignment(SwingConstants.RIGHT);
            panel.add(statusLbl);

            JPanel bottomPanel = new JPanel();
            panel.add(bottomPanel, BorderLayout.EAST);

            bottomPanel.add(prevBtn);
            bottomPanel.add(nextBtn);
            bottomPanel.add(findBtn);
            bottomPanel.add(busyLbl);

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
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            Date t1 = (Date) ts1.getValue(); 
                            Date t2 = (Date) ts2.getValue();

                            boolean found = findMessage(typeTxt.getText(), (String) sourceCBox.getSelectedItem(), 
                                    (String)sourceEntCBox.getSelectedItem(), (String) destCBox.getSelectedItem(),
                                    t1, t2);

                            return found;
                        }
                        @Override
                        protected void done() {
                            try {
                                boolean found = get();
                                if (found) {
                                    nextBtn.setEnabled(true);
                                    prevBtn.setEnabled(true);

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



            getRootPane().setDefaultButton(findBtn);

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    findOpenState = false;
                }
            });
        }

        private void clear() {
            clearSelection();
            findBtn.setText(I18n.text("Find"));
            prevBtn.setEnabled(false);
            statusLbl.setText("");
            statusLbl.setVisible(true);
            highlightBtn.setSelected(false);
            toggleHighlight();
        }

        /**
         *  Updates status label with number of found elements 
         */
        private void updateStatus() {
            if (resultList.size() != 0) {
                statusLbl.setVisible(true);
                statusLbl.setText(finderNextIndex+1 + " of "+resultList.size());
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

            getContentPane().add(typeTxt, "cell 1 0,growx");
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