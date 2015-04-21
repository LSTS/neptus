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
 * Author: Manuel
 * Apr 15, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
/**
 * @author Manuel R.
 *
 */
@PluginDescription
public class MRAExporterFilter implements MRAExporter {

    private IMraLogGroup source;
    private ProgressMonitor pmonitor;
    private ArrayList<String> defaultLogs = new ArrayList<String>();
    private FilterList list;

    /**
     * @wbp.parser.entryPoint
     */
    public MRAExporterFilter(IMraLogGroup source) {
        super();
        this.source = source;

        defaultLogs.add("EstimatedState");
        defaultLogs.add("Temperature");
        defaultLogs.add("Salinity");
        defaultLogs.add("Conductivity");
        defaultLogs.add("Pressure");

    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    private File chooseSaveFile(String path) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(path.concat("/Data_filtered.lsf")));
        fileChooser.setFileView(new NeptusFileView());
        fileChooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"),
                new String[] { "lsf", FileUtil.FILE_TYPE_LSF_COMPRESSED, 
            FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2 }));

        fileChooser.setAcceptAllFileFilterUsed(false);

        int status = fileChooser.showSaveDialog(null);
        String fileName = null;

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                fileName = selectedFile.getCanonicalPath();
                if (!fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName + ".lsf");
                }
                if (fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        //list of messages in this log source
        String[] logs = source.listLogs();

        //create JFrame with default logs selected and the rest of available logs
        list = new FilterList(defaultLogs, logs);

        LsfIndex index = source.getLsfIndex();

        String path = source.getFile("Data.lsf").getParent();
        File outputFile = chooseSaveFile(path);
        if (outputFile == null) {
            return "Cancelled by the user";
        }
        OutputStream fos = null;
        if(!outputFile.exists()) {
            try {
                outputFile.createNewFile();
                fos = new FileOutputStream(outputFile, true);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        } 

        System.out.println("Filtering... " + defaultLogs.toString());
        for (String logName : logs) {
            if (defaultLogs.contains(logName)) {
                int mgid = index.getDefinitions().getMessageId(logName);
                int firstPos = index.getFirstMessageOfType(mgid);
                int lastPos = index.getLastMessageOfType(mgid);
                int j = firstPos;

                try {
                    while (j < lastPos) {
                        //  IMCMessage entry = index.getMessage(j);
                        //  System.out.println(entry.toString());
                        //  System.out.println("pos "+ j);

                        //write msg bytes
                        byte[] by = index.getMessageBytes(j);
                        fos.write(by);

                        j = index.getNextMessageOfType(mgid, j);
                    }
                    //append last message
                    byte[] lastMsg = index.getMessageBytes(lastPos);
                    fos.write(lastMsg);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        try {
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return I18n.text("Process complete");
    }

    @Override
    public String getName() {
        return I18n.text("Export filtered");
    }


    class FilterList extends JFrame {

        protected JList  m_list;
        private JTextField textField = null;
        
        public FilterList(ArrayList<String> defaultLogs, String[] logs) {

            super("MRA Exporter");
            setType(Type.UTILITY);
            setSize(230, 300);
            setAlwaysOnTop(true);
            getContentPane().setLayout(new MigLayout("", "[240px]", "[300px]"));

            ArrayList<LogItem> options = new ArrayList<>();

            for (String log : logs ) {
                if (defaultLogs.contains(log)) 
                    options.add(new LogItem(log, true));
                else
                    options.add(new LogItem(log, false));
            }
            Arrays.sort(options.toArray());

            m_list = new JList(options.toArray());
            CheckListCellRenderer renderer = new CheckListCellRenderer();
            m_list.setCellRenderer(renderer);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            CheckListener lst = new CheckListener(this);
            m_list.addMouseListener(lst);
            m_list.addKeyListener(lst);

            JScrollPane ps = new JScrollPane();
            ps.setViewportView(m_list);
            ps.setMaximumSize(new Dimension(200, 200));
            ps.setMinimumSize (new Dimension (200,200));

            JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.add(ps, BorderLayout.CENTER);
            p.setBorder(new TitledBorder(new EtchedBorder(), "Filter messages:") );

            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

            final ActionListener entAct = new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = m_list.getNextMatch(textField.getText(), 0, javax.swing.text.Position.Bias.Forward);
                    m_list.setSelectedIndex(index);
                    m_list.ensureIndexIsVisible(index);
                    
                }
            };
            
            AbstractAction finder = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (textField != null) {
                        getContentPane().remove(textField);
                        textField = null;
                    } else {
                        textField = new JTextField();
                        textField.setColumns(10);
                        getContentPane().add(textField, "cell 0 0,alignx center");
                        textField.addActionListener(entAct);
                    }
                    getContentPane().revalidate();
                    getContentPane().repaint();
                }
            };


            p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
            p.getActionMap().put("finder", finder);

            getContentPane().add(p, "cell 0 1,alignx left,aligny top");

            JButton saveBtn = new JButton("Save File");
            getContentPane().add(saveBtn, "cell 0 2,alignx center");
            
            this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
            setResizable(false);
            setVisible(true);

        }

        class CheckListCellRenderer extends JCheckBox implements ListCellRenderer {

            protected Border m_noFocusBorder = new EmptyBorder(1, 1, 1, 1);

            public CheckListCellRenderer() {
                super();
                setOpaque(true);
                setBorder(m_noFocusBorder);
            }

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                setText(value.toString());
                setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                LogItem data = (LogItem)value;
                setSelected(data.isSelected());
                setFont(list.getFont());
                setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : m_noFocusBorder);

                return this;

            }
        }

        class CheckListener implements MouseListener, KeyListener {

            protected FilterList m_parent;
            protected JList m_list;
            public CheckListener(FilterList parent) {
                m_parent = parent;
                m_list = parent.m_list;
            }

            public void mouseClicked(MouseEvent e) {
                if (e.getX() < 20)
                    doCheck();
            }

            public void mousePressed(MouseEvent e) {}

            public void mouseReleased(MouseEvent e) {}

            public void mouseEntered(MouseEvent e) {}

            public void mouseExited(MouseEvent e) {}

            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == ' ')
                    doCheck();
            }

            public void keyTyped(KeyEvent e) {}

            public void keyReleased(KeyEvent e) {}

            protected void doCheck() {

                int index = m_list.getSelectedIndex();
                if (index < 0)
                    return;
                LogItem data = (LogItem)m_list.getModel().getElementAt(index);
                data.invertSelected();
                m_list.repaint();
            }

        }

        class LogItem implements Comparable<LogItem> {

            protected String logName;

            protected boolean m_selected;

            public LogItem(String name, boolean selected) {

                logName = name;

                m_selected = selected;

            }

            public String getName() { return logName; }

            public void setSelected(boolean selected) {
                m_selected = selected;
            }

            public void invertSelected() { 
                m_selected = !m_selected; 
            }

            public boolean isSelected() { 
                return m_selected; 
            }

            public String toString() { 
                return logName; 
            }

            @Override
            public int compareTo(LogItem anotherLog) {
                return logName.compareTo(anotherLog.logName);
            }

        }
    }
}