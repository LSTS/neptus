package pt.lsts.neptus.plugins.position;


import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.MarksKMLHandler;
import pt.lsts.neptus.util.csv.MarksCSVHandler;

/**
 * Created by tsm on 23-05-2017.
 */
public class MarksExporterPanel extends JPanel {
    public static String csvDelimiter = ",";

    private final int MAIN_WIDTH = 300;
    private final int MAIN_HEIGHT = 300;

    private final JButton fromCsv = new JButton(I18n.text("As CSV"));
    private final JButton fromKml = new JButton(I18n.text("As KML"));

    private final JLabel sourceLabel = new JLabel(I18n.text(""));
    private final JFileChooser fileChooser = new JFileChooser();

    private final CheckboxList marksList = new CheckboxList();
    private JScrollPane listScroller = new JScrollPane(marksList);

    private final JPanel exporterSourcePanel = new JPanel();

    private HashMap<String, MarkElement> marksToExport = null;

    private static boolean validOperation = false;

    public MarksExporterPanel(List<MarkElement> marks) {
        loadAvailableMarks(marks);

        this.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        this.setLayout(new MigLayout(""));


        exporterSourcePanel.setBounds(0, 0, MAIN_WIDTH / 5, MAIN_HEIGHT);
        marksList.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        listScroller.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));

        fromCsv.setSelected(true);

        exporterSourcePanel.setLayout(new MigLayout("wrap 2"));
        exporterSourcePanel.add(fromCsv);
        exporterSourcePanel.add(fromKml);

        fromCsv.addActionListener(e ->{
            if(fileChooser.showDialog(null, I18n.text("To CSV")) != JFileChooser.APPROVE_OPTION)
                return;

            String exportPath = fileChooser.getSelectedFile().getAbsolutePath();
            validOperation = MarksCSVHandler.exportCsv(exportPath, fetchSelectedMarks(), csvDelimiter);
        });

        fromKml.addActionListener(e ->{
            if(fileChooser.showDialog(null, I18n.text("To KML")) != JFileChooser.APPROVE_OPTION)
                return;

            String exportPath = fileChooser.getSelectedFile().getAbsolutePath();
            validOperation = MarksKMLHandler.exportKML(exportPath, fetchSelectedMarks());
        });

        add(sourceLabel, "w 20%, h 10%, wrap");
        add(exporterSourcePanel, "w 100%, h 20%, wrap");
        add(listScroller, "w 100%, h 70%");
    }

    private List<MarkElement> fetchSelectedMarks() {
        ArrayList<MarkElement> marks = new ArrayList<>();
        Arrays.stream(marksList.getSelectedStrings())
                .forEach(id -> marks.add(marksToExport.get(id)));

        return marks;
    }

    private void loadAvailableMarks(List<MarkElement> marks) {
        marksToExport = new HashMap<>();
        JCheckBox[] checks = new JCheckBox[marks.size()];
        for (int i = 0; i < marks.size(); i++) {
            MarkElement m = marks.get(i);
            String id = m.getId();

            checks[i] = new JCheckBox(id);
            marksToExport.put(id, m);
        }

        marksList.setListData(checks);
    }

    public static boolean showPanel(Component parent, List<MarkElement> marks) {
        MarksExporterPanel panel = new MarksExporterPanel(marks);
        JOptionPane.showOptionDialog(parent, panel, I18n.text("Marks Exporter"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"OK", I18n.text("Cancel")}, null);

        return validOperation;
    }
}
