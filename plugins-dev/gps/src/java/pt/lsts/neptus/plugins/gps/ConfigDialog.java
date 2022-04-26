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
 * 14/04/2011
 */
package pt.lsts.neptus.plugins.gps;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.gps.device.Device;
import pt.lsts.neptus.util.GuiUtils;

/**
 * Configuration Dialog to specify the configuration parameters of a serial port
 * GPS device.
 * 
 * @author Ricardo Martins
 * 
 */
@SuppressWarnings("serial")
public class ConfigDialog extends JDialog {
    /** Serial port selection combo box. */
    private JComboBox<String> portComboBox;
    /** Serial port baud rate selection combo box. */
    private JComboBox<?> baudComboBox;
    /** Serial port frame type selection combo box. */
    private JComboBox<?> frameComboBox;
    /** Selected serial port device. */
    private String port;
    /** Selected serial port baud rate. */
    private String baud;
    /** Selected serial port frame type. */
    private String frame;
    /** True if dialog was canceled. */
    private boolean canceled = false;

    public ConfigDialog(Component owner, String title) throws Exception {
        super(SwingUtilities.getWindowAncestor(owner), title);

        // Port.
        Vector<String> devices = Device.enumerate();
        if (devices.size() == 0)
            throw new Exception(I18n.text("No serial ports available"));

        // Port.
        portComboBox = new JComboBox<String>(devices);

        // Baud Rate.
        baudComboBox = new JComboBox<Object>(Device.BAUD_RATES);

        // Frame.
        frameComboBox = new JComboBox<Object>(Device.FRAME_TYPES);

        // OK Button.
        final JButton okButton = new JButton(I18n.text("OK"));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                baud = (String) baudComboBox.getSelectedItem();
                port = (String) portComboBox.getSelectedItem();
                frame = (String) frameComboBox.getSelectedItem();
                close();
            }
        });
        GuiUtils.reactEnterKeyPress(okButton);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                okButton.requestFocusInWindow();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                canceled = true;
            }
        });
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Cancel Button.
        JButton cancelButton = new JButton(I18n.text("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                close();
            }
        });
        GuiUtils.reactEscapeKeyPress(cancelButton);

        // Layout.
        GridLayout layout = new GridLayout(4, 2, 10, 10);

        // Container Panel.
        JPanel panel = new JPanel(layout);
        EmptyBorder border = new EmptyBorder(20, 20, 20, 20);
        panel.setBorder(border);
        panel.add(new JLabel(I18n.text("Serial Port")));
        panel.add(portComboBox);
        panel.add(new JLabel(I18n.text("Baud Rate")));
        panel.add(baudComboBox);
        ///Serial port frame type
        panel.add(new JLabel(I18n.text("Frame Type")));
        panel.add(frameComboBox);
        panel.add(okButton);
        panel.add(cancelButton);

        setContentPane(panel);
        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
    }

    /**
     * Get selected serial port.
     * 
     * @return serial port.
     */
    public String getPort() {
        return port;
    }

    /**
     * Get selected baud rate.
     * 
     * @return baud rate.
     */
    public String getBaud() {
        return baud;
    }

    /**
     * Get selected frame type.
     * 
     * @return frame type.
     */
    public String getFrame() {
        return frame;
    }

    /**
     * Open dialog.
     * 
     * @return true if the user pressed OK, false otherwise.
     * @throws Exception
     *             if no serial ports are available.
     */
    public boolean open(String aPort, String aBaud, String aFrame) {
        portComboBox.setSelectedItem(aPort);
        if (portComboBox.getSelectedIndex() == -1)
            portComboBox.setSelectedIndex(0);

        baudComboBox.setSelectedItem(aBaud);
        if (baudComboBox.getSelectedIndex() == -1)
            baudComboBox.setSelectedIndex(0);

        frameComboBox.setSelectedItem(aFrame);
        if (frameComboBox.getSelectedIndex() == -1)
            frameComboBox.setSelectedIndex(0);

        GuiUtils.centerParent(this, this.getOwner());
        setVisible(true);

        return !canceled;
    }

    /**
     * Close dialog.
     */
    public void close() {
        setVisible(false);
        dispose();
    }
}
