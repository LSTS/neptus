package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.csv.MarksCSVHandler;

/**
 * Created by tsm on 23-05-2017.
 */
public class MarksImporterPanel extends JPanel {
    public static String csvDelimiter = ",";

    private final int MAIN_WIDTH = 300;
    private final int MAIN_HEIGHT = 60;

    private final JRadioButton fromCsv = new JRadioButton(I18n.text("From CSV"));
    private final JRadioButton fromKml = new JRadioButton(I18n.text("From KML"));
    private final ButtonGroup group = new ButtonGroup();

    private final JButton kmlFromUrlBtn = new JButton(I18n.text("From URL"));
    private final JButton kmlFromFileBtn = new JButton(I18n.text("From file"));

    private final JLabel sourceLabel = new JLabel(I18n.text(""));
    private final JFileChooser fileChooser = new JFileChooser();

    private final JButton csvFromFileBtn = new JButton(I18n.text("Choose file"));

    private final JPanel importerSourcePanel = new JPanel();
    private final JPanel importerPanel = new JPanel();
    private final JPanel csvImporterPanel = new JPanel();
    private final JPanel kmlImporterPanel = new JPanel();

    private final FileFilter csvFilter = new FileNameExtensionFilter("Csv file", "csv", "Comma separated values");
    private final FileFilter kmlFilter = new FileNameExtensionFilter("kml", "kml", "Keyhole Markup Language");
    private final FileFilter txtFilter = new FileNameExtensionFilter("txt", "txt", "text");

    private List<MarkElement> importedMarks = null;

    public MarksImporterPanel() {
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        this.setBounds(0, 0, MAIN_WIDTH, MAIN_HEIGHT);
        this.setLayout(new MigLayout(""));

        initSourcesPanel();
        initImporterPanel();

        add(sourceLabel, "w 20%, h 20%, wrap");
        add(importerSourcePanel, "w 20%, h 90%");
        add(importerPanel, "w 80%, h 90%");
    }

    /**
     * Null if something went wrong
     * */
    public List<MarkElement> getImportedMarks() {
        return importedMarks;
    }

    private void initImporterPanel() {
        importerPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);
        csvImporterPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);
        kmlImporterPanel.setBounds(0, 0, MAIN_WIDTH - 100, MAIN_HEIGHT);

        //csv panel
        csvImporterPanel.setLayout(new MigLayout());
        csvFromFileBtn.addActionListener(e -> {
            int res = fileChooser.showDialog(null, "CSV Source");

            if(res == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                importedMarks = MarksCSVHandler.importCsv(filePath, csvDelimiter);
                if(importedMarks == null) {
                    sourceLabel.setForeground(Color.RED);
                    sourceLabel.setText("ERROR while importing marks");
                    return;
                }

                sourceLabel.setText(filePath + " (" + importedMarks.size() + " marks)");
            }
        });

        csvImporterPanel.add(csvFromFileBtn);

        // csv by default
        importerPanel.add(csvImporterPanel);
    }

    private void initSourcesPanel() {
        importerSourcePanel.setBounds(0, 0, MAIN_WIDTH / 5, MAIN_HEIGHT);
        group.add(fromCsv);
        group.add(fromKml);

        fromCsv.setSelected(true);

        ActionListener radioBtnListener = e -> {
          if(fromCsv.isSelected()) {
              importerPanel.remove(kmlImporterPanel);
              importerPanel.add(csvImporterPanel);

              fileChooser.removeChoosableFileFilter(kmlFilter);
              fileChooser.addChoosableFileFilter(csvFilter);
          }
          else {
              importerPanel.remove(csvImporterPanel);
              importerPanel.add(kmlImporterPanel);

              fileChooser.removeChoosableFileFilter(csvFilter);
              fileChooser.addChoosableFileFilter(kmlFilter);
          }
        };

        fromCsv.addActionListener(radioBtnListener);
        fromKml.addActionListener(radioBtnListener);

        importerSourcePanel.setLayout(new MigLayout("wrap 1"));
        importerSourcePanel.add(fromCsv);
        importerSourcePanel.add(fromKml);
    }

    public static List<MarkElement> showPanel(Component parent) {
        MarksImporterPanel panel = new MarksImporterPanel();
        JOptionPane.showOptionDialog(parent, panel, I18n.text("Marks importer"),
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[]{"OK", I18n.text("Cancel")}, null);

        return panel.getImportedMarks();
    }
}
