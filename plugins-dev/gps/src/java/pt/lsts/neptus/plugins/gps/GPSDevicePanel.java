/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: rasm
 * Apr 12, 2011
 */
package pt.lsts.neptus.plugins.gps;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginsLoader;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.gps.device.Device;
import pt.lsts.neptus.plugins.gps.device.Fix;
import pt.lsts.neptus.plugins.gps.device.FixListener;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ConsoleParse;

/**
 * Main class of the GPS Device Panel plugin. The main GUI elements are created
 * here.
 *
 * @author Ricardo Martins
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.RIGHT, width = 200, height = 100, accelerator = 'G')
@PluginDescription(name = "GPS Device Panel", author = "Ricardo Martins", icon = "images/buttons/gpsbutton.png",
    description = "Panel that reads NMEA sentences from a GPS device",
    documentation = "gps-panel/gps-panel.html")
public class GPSDevicePanel extends ConsolePanel implements ActionListener, FixListener {
    /** Not connected message string. */
    private static final String MSG_NOT_CONNECTED = "<html>" + I18n.text("Not Connected") + "</html>";
    /** Waiting for data message string. */
    private static final String MSG_WAIT_DATA = "<html>" + I18n.text("Waiting for Data...") + "</html>";
    /** No valid data message string. */
    private static final String ERR_NO_INPUT = "<html><font color='red'>" + I18n.text("No Valid Data") + "</font></html>";
    /** No valid solution message string. */
    private static final String WAR_NO_FIX = "<html><font color='#ec5114'>" + I18n.text("No Valid Solution") + "</font></html>";
    /** Default decimal format. */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    @NeptusProperty(name = "Input Timeout (s)")
    public int watchDogTimeoutSegs = 5;
    @NeptusProperty(name = "Preferred Width")
    public int preferredWidth = 175;
    @NeptusProperty(name = "Preferred Height")
    public int preferredHeight = 75;
    @NeptusProperty(name = "Serial Port Device")
    public String uartDevice = null;
    @NeptusProperty(name = "Serial Port Baud Rate")
    public String uartBaudRate = "4800";
    @NeptusProperty(name = "Serial Port Frame Type")
    public String uartFrameType = "8n1";
    @NeptusProperty(name = "Use speed to filter heading")
    public boolean useSpeedToFilterHeading = true;
    @NeptusProperty(name = "Minimum speed to accept heading (m/s)")
    public double headingSpeedMinimum = 0.1;

    /** GPS device. */
    private final Device gpsDevice = new Device(this);
    /** Watch dog timer. */
    private final Timer watchDog = new Timer(watchDogTimeoutSegs * 1000, this);

    // GUI
    /** Action button. */
    private final JButton actionButton = new JButton();
    /** Main panel. */
    private final JPanel mainPanel = new JPanel();
    /** Label with GPS data. */
    private JLabel dataLabel;

    /**
     * Default constructor.
     */
    public GPSDevicePanel(ConsoleLayout console) {
        super(console);

        removeAll();
        setLayout(new BorderLayout());

        // Watchdog.
        watchDog.setActionCommand(I18n.text("Timeout"));

        // Label.
        dataLabel = new JLabel(MSG_NOT_CONNECTED, SwingConstants.CENTER);
        Font dataLabelFont = new Font(dataLabel.getFont().getName(), Font.BOLD, dataLabel.getFont()
                .getSize());
        dataLabel.setFont(dataLabelFont);

        // Button.
        actionButton.addActionListener(this);
        changeButton(I18n.text("Connect"), true);

        // Main Panel.
        BorderLayout layout = new BorderLayout();
        layout.setVgap(5);
        mainPanel.setLayout(layout);
        mainPanel.add(dataLabel, BorderLayout.CENTER);
        mainPanel.add(actionButton, BorderLayout.SOUTH);

        // Adjust panel dimensions.
        Dimension dim = mainPanel.getPreferredSize();
        dim.width = preferredWidth;
        mainPanel.setPreferredSize(dim);
        mainPanel.setSize(dim);

        this.setName(I18n.text("GPS Device"));
        add(mainPanel, BorderLayout.CENTER);
        setSize(preferredWidth, preferredHeight);
    }

    @Override
    public void cleanSubPanel() {
        if (gpsDevice.isConnected())
            gpsDevice.disconnect();
    }

    /**
     * Handle a GPS fix.
     *
     * @param fix
     *            GPS fix.
     */
    @Override
    public void onFix(Fix fix) {
        watchDog.restart();

        String latitude = CoordinateUtil.latitudeAsString(fix.getLatitude());
        String longitude = CoordinateUtil.longitudeAsString(fix.getLongitude());

        dataLabel.setToolTipText("<html><b>" + I18n.text("Latitude") + "</b>: " + latitude + "<br>"
                + "<b>" + I18n.text("Longitude") + "</b>: " + longitude + "<br><b>" + I18n.text("Height") + "</b>: "
                + DECIMAL_FORMAT.format(fix.getHeight()) + "<br><b>" + I18n.text("Satellites") + "</b>: "
                + fix.getSatellites() + "<br>" + "<b>"
                /// GPS HDOP
                + I18n.text("HDOP") + "</b>: "
                + DECIMAL_FORMAT.format(fix.getHorizontalDilution()) + "<br>" + "<b>"
                /// GPS VDOP
                + I18n.text("VDOP") + "</b>: "
                + DECIMAL_FORMAT.format(fix.getVerticalDilution()) + "<br>"
                + "<b>" + I18n.text("Vertical Accuracy") + "</b>: " + DECIMAL_FORMAT.format(fix.getVerticalAccuracy())
                + "<br>" + "<b>" + I18n.text("Horizontal Accuracy") + "</b>: "
                + DECIMAL_FORMAT.format(fix.getHorizontalAccuracy()) + "<br>"
                + "<b>" + I18n.text("Ground Speed") + "</b>: " + DECIMAL_FORMAT.format(fix.getSog()) + "<br>"
                + "<b>" + I18n.text("Course Over Ground") + "</b>: " + DECIMAL_FORMAT.format(fix.getCog()) + "<br>"
                + "</html>");

        if (fix.isValid()) {
            LocationType loc = new LocationType();
            loc.setLatitudeDegs(fix.getLatitude());
            loc.setLongitudeDegs(fix.getLongitude());
            loc.setHeight(fix.getHeight());

            if (!(useSpeedToFilterHeading && fix.getCog() <= headingSpeedMinimum))
                MyState.setHeadingInDegrees(fix.getCog());
            MyState.setLocation(loc);
            dataLabel.setText(latitude + " / " + longitude);
        }
        else {
            dataLabel.setText(WAR_NO_FIX);
        }
    }

    /**
     * Handle actions.
     *
     * @param e
     *            action event.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (I18n.text("Connect").equals(e.getActionCommand())) {
            if (setup()) {
                connect();
            }
        }
        else if (I18n.text("Disconnect").equals(e.getActionCommand())) {
            disconnect(MSG_NOT_CONNECTED);
        }
        else if (I18n.text("Timeout").equals(e.getActionCommand())) {
            disconnect(ERR_NO_INPUT);
        }
    }

    /**
     * Connect the currently configured GPS device.
     */
    private void connect() {
        HashMap<Device.Parameter, String> params = new HashMap<Device.Parameter, String>();
        params.put(Device.Parameter.DEV, uartDevice);
        params.put(Device.Parameter.BAUD, uartBaudRate);
        params.put(Device.Parameter.FRAME, uartFrameType);

        changeButton(I18n.text("Connecting"), false);

        try {
            gpsDevice.connect(params);
            changeButton(I18n.text("Disconnect"), true);
            dataLabel.setText(MSG_WAIT_DATA);
            watchDog.setDelay(watchDogTimeoutSegs * 1000);
            watchDog.start();
        }
        catch (Exception e) {
            NeptusLog.pub().info("<###> "+e);
        }
    }

    /**
     * Disconnect the currently connected GPS device.
     *
     * @param msg
     *            message to display in the main label.
     */
    private void disconnect(String msg) {
        changeButton(I18n.text("Disconnecting"), false);
        gpsDevice.disconnect();
        dataLabel.setText(msg);
        changeButton(I18n.text("Connect"), true);
        watchDog.stop();
    }

    /**
     * Open the configuration dialog.
     *
     * @return true if a new configuration exists, false if the dialog was
     *         cancelled.
     */
    private boolean setup() {
        try {
            ConfigDialog dialog = new ConfigDialog(this, I18n.text("GPS Device Configuration"));

            if (dialog.open(uartDevice, uartBaudRate, uartFrameType)) {
                uartDevice = dialog.getPort();
                uartBaudRate = dialog.getBaud();
                uartFrameType = dialog.getFrame();
                return true;
            }
        }
        catch (Exception e) {
            dataLabel.setText("<html><font color='red'>" + e.getMessage() + "</html>");
        }

        return false;
    }

    /**
     * Change the label and state of the action button.
     *
     * @param text
     *            text to display on the action button.
     * @param enabled
     *            true to enable the button, false otherwise.
     */
    private void changeButton(String text, boolean enabled) {
        actionButton.setText(text);
        actionButton.setActionCommand(text);
        actionButton.setEnabled(enabled);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    public static void main(String[] args) {
        PluginsLoader.load();
        ConsoleParse.testSubPanel(GPSDevicePanel.class);
    }
}
