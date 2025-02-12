package pt.lsts.neptus.plugins.sim;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

public class GpsFixDialog extends JDialog {

    private SimulationActionsPlugin plugin;
    private Dimension prefSize = new Dimension(350, 160);
    private JComboBox<String> comboBox = new JComboBox<>();
    private JPanel setLatLonPanel;
    private JTextField latTextField = new JTextField(6);
    private JTextField lonTextField = new JTextField(6);

    public GpsFixDialog(SimulationActionsPlugin plugin) {
        super();
        this.plugin = plugin;
        initComponents();
        this.setAlwaysOnTop(true);
    }

    private void initComponents() {
        setLayout(new MigLayout("fill", "[]", "[]"));
        setSize(prefSize);
        this.setTitle(I18n.text("GPS Fix"));

        createComboPanel();
        createCoordinatePanel();
        createButtonsPanel();
        GuiUtils.centerOnScreen(this);
    }

    private void createComboPanel() {
        JPanel comboPanel = new JPanel(new MigLayout("insets 10 10 0 10, fill", "[][]", "[]"));
        JLabel comboLabel = new JLabel("GPS Fix to:");
        String[] options = {"Center of Map", "Coordinates"};
        comboBox = new JComboBox<>(options);
        comboPanel.add(comboLabel);
        comboPanel.add(comboBox);
        add(comboPanel, "align center, wrap");

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedIndex() == 1) {
                    setLatLonPanel.setVisible(true);
                }
                else {
                    setLatLonPanel.setVisible(false);
                }
                revalidate();
                repaint();
            }
        });
    }

    private void createCoordinatePanel() {
        setLatLonPanel = new JPanel(new MigLayout("insets 0 10 5 10, fill", "[][]", "[]"));
        JLabel latLabel = new JLabel(I18n.text("Latitude:"));
        setLatLonPanel.add(latLabel);
        setLatLonPanel.add(latTextField, "");
        JLabel lonLabel = new JLabel(I18n.text("Longitude:"));
        setLatLonPanel.add(lonLabel);
        setLatLonPanel.add(lonTextField, "");
        LocationCopyPastePanel copyPastePanel = new LocationCopyPastePanel() {
            @Override
            public void setLocationType(LocationType locationType) {
                LocationType loc = locationType.getNewAbsoluteLatLonDepth();
                latTextField.setText(String.valueOf(loc.getLatitudeDegs()));
                latTextField.setCaretPosition(0);
                lonTextField.setText(String.valueOf(loc.getLongitudeDegs()));
                lonTextField.setCaretPosition(0);
            }

            @Override
            public LocationType getLocationType() {
                LocationType lt = null;
                try {
                    double latDeg = Double.parseDouble(latTextField.getText());
                    double lonDeg = Double.parseDouble(lonTextField.getText());
                    lt = new LocationType(latDeg, lonDeg);
                }
                catch (Exception exp) {
                    System.out.println("Unable to convert values to latitude and longitude");
                }
                return lt;
            }
        };
        copyPastePanel.setBorder(null);
        setLatLonPanel.add(copyPastePanel);
        setLatLonPanel.setVisible(false);
        add(setLatLonPanel, "align center, wrap, hidemode 3");
    }

    private void createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new MigLayout("insets 0 10 10 10, fill", "[][]", "[]"));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton, "sizegroup btn, gapright 5");
        buttonPanel.add(cancelButton, "sizegroup btn, gapleft 5");
        add(buttonPanel, "align center");

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (comboBox.getSelectedItem().equals("Coordinates")) {
                    String latText = latTextField.getText();
                    String lonText = lonTextField.getText();
                    if (Objects.equals(latText, "") || Objects.equals(lonText, "")) {
                        throw new IllegalArgumentException("Latitude and longitude cannot be empty when setting a position.");
                    }
                    LocationType lt = null;
                    try {
                        double latDeg = Double.parseDouble(latTextField.getText());
                        double lonDeg = Double.parseDouble(lonTextField.getText());
                        lt = new LocationType(latDeg, lonDeg);
                    }
                    catch (Exception exp) {
                        System.out.println("Unable to convert values to latitude and longitude");
                    }
                    if (lt != null) {
                        plugin.sendGpsFix(lt);
                    }
                }
                else {
                    plugin.sendGpsFixToCenter();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });
    }
}
