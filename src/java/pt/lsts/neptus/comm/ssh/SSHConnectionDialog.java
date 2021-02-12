/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 15/Abr/2006
 */
package pt.lsts.neptus.comm.ssh;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;

import pt.lsts.neptus.gui.ImagePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class SSHConnectionDialog extends JPanel {
    public static final Image SSH_CONNECT_IMAGE = ImageUtils.getImage("images/ssh-connect.png");

    private static final NumberFormat iFormat = new IntFormat();

    private String title = I18n.text("Connect to Remote Host");
    private String host = "localhost";
    private String username = "root";
    private String password = "";
    private int port = 22;

    private boolean isCanceled = false;

    private JDialog jDialog = null;
    private JPanel jContentPane = null;
    private ImagePanel imageConnectSSH = null;

    private JLabel hostLabel = null;
    private JLabel userLabel = null;
    private JLabel passwordLabel = null;
    private JLabel portLabel = null;

    private JTextField usernameField = null;
    private JTextField hostField = null;
    private JPasswordField passwordField = null;
    private JFormattedTextField portField = null;

    private JButton okButton = null;
    private JButton cancelButton = null;

    private Window parentWindow = null;
    
    private boolean hostEnable = true;
    private boolean portEnable = true;
    private boolean userEnable = true;
    private boolean passwordEnable = true;
    
    /**
     * This is the default constructor
     */
    public SSHConnectionDialog() {
        super();
        initialize();
    }

    public SSHConnectionDialog(String host, String username, String password, int port) {
        this(host, username, password, port, null);
    }

    public SSHConnectionDialog(String host, String username, String password, int port, String title) {
        super();
        if (title != null)
            this.title = title;
        this.host = host;
        this.username = username;
        this.password = password;
        if (port > 0)
            this.port = port;
        else
            this.port = 22;
        initialize();
    }

    public SSHConnectionDialog(String host, String username, String password, int port, String title,
            boolean hostEnable, boolean portEnable, boolean userEnable, boolean passwordEnable) {
        super();
        if (title != null)
            this.title = title;
        this.host = host;
        this.username = username;
        this.password = password;
        if (port > 0)
            this.port = port;
        else
            this.port = 22;
        
        this.hostEnable = hostEnable;
        this.portEnable = portEnable;
        this.userEnable = userEnable;
        this.passwordEnable = passwordEnable;
        
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        portLabel = new JLabel();
        portLabel.setBounds(new Rectangle(63, 90, 59, 16));
        portLabel.setText(I18n.text("Port"));
        hostLabel = new JLabel();
        hostLabel.setText(I18n.text("Host"));
        hostLabel.setLocation(63, 15);
        hostLabel.setSize(59, 16);
        userLabel = new JLabel();
        userLabel.setText(I18n.text("Username"));
        userLabel.setLocation(63, 40);
        userLabel.setSize(59, 16);
        passwordLabel = new JLabel();
        passwordLabel.setText(I18n.text("Password"));
        passwordLabel.setLocation(63, 65);
        passwordLabel.setSize(58, 16);

        this.setSize(514, 123);

        GroupLayout layout = new GroupLayout(this);
        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(getImageConnectSSH(), GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(hostLabel)
                                .addComponent(portLabel)
                                .addComponent(userLabel)
                                .addComponent(passwordLabel))
                                .addPreferredGap(ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(Alignment.CENTER, false)
                                        .addComponent(getHostField(), Alignment.LEADING, 220, 220, Short.MAX_VALUE)
                                        .addComponent(getPortField(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(getUsernameField(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(getPasswordField(), Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(layout.createParallelGroup(Alignment.CENTER)
                                                .addComponent(getOkButton(), GroupLayout.PREFERRED_SIZE, 98, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(getCancelButton(), GroupLayout.PREFERRED_SIZE, 98, GroupLayout.PREFERRED_SIZE)))
                );
        layout.setVerticalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(getImageConnectSSH(), GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                .addComponent(hostLabel)
                                                .addComponent(getHostField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                        .addComponent(portLabel)
                                                        .addComponent(getPortField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                                .addComponent(userLabel)
                                                                .addComponent(getUsernameField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                                                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                                        .addComponent(passwordLabel)
                                                                        .addComponent(getPasswordField(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                                                        .addGroup(layout.createSequentialGroup()
                                                                                .addComponent(getOkButton())
                                                                                .addPreferredGap(ComponentPlacement.RELATED)
                                                                                .addComponent(getCancelButton())))
                                                                                .addGap(15))
                );
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        this.setLayout(layout);
        
        getHostField().setEnabled(hostEnable);
        getPortField().setEditable(portEnable);
        getUsernameField().setEnabled(userEnable);
        getPasswordField().setEnabled(passwordEnable);
    }

    /**
     * This method initializes jDialog
     * 
     * @return javax.swing.JDialog
     */
    private JDialog getJDialog() {
        if (jDialog == null) {
            if (parentWindow == null)
                jDialog = new JDialog();
            else if (parentWindow instanceof Frame)
                jDialog = new JDialog((Frame) parentWindow);
            else if (parentWindow instanceof Dialog)
                jDialog = new JDialog((Dialog) parentWindow);
            jDialog.setContentPane(getJContentPane());
            jDialog.setTitle(title);
            jDialog.setSize(this.getWidth() + 5, this.getHeight() + 35);
            jDialog.getContentPane().setLayout(new BorderLayout());
            jDialog.getContentPane().add(this, BorderLayout.CENTER);
            GuiUtils.centerOnScreen(jDialog);
            //jDialog.setModal(true);
            jDialog.setModalityType(parentWindow != null ? ModalityType.DOCUMENT_MODAL : ModalityType.APPLICATION_MODAL);
            jDialog.setAlwaysOnTop(true);
            jDialog.setResizable(false);
            jDialog.setAlwaysOnTop(true);
            jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            jDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    cancelAction();
                }
            });
            jDialog.toFront();
            jDialog.setFocusTraversalPolicy(new SSHConnectionFocusTraversalPolicy());
            jDialog.setVisible(true);
        }
        return jDialog;
    }

    public Window getParentWindow() {
        return parentWindow;
    }

    public void setParentWindow(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
        }
        return jContentPane;
    }

    /**
     * This method initializes jPasswordField
     * 
     * @return javax.swing.JPasswordField
     */
    private JPasswordField getPasswordField() {
        if (passwordField == null) {
            passwordField = new JPasswordField();
            passwordField.setPreferredSize(new Dimension(150, 20));
            passwordField.setLocation(132, 65);
            passwordField.setSize(150, 20);
            passwordField.setText(password);
            // passwordField.setFocusable(true);
            passwordField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    // NeptusLog.pub().info("<###>keyPressed()");
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        okAction();
                }
            });
            passwordField.setSelectionStart(0);
            passwordField.setSelectionEnd(passwordField.getPassword().length);
            //passwordField.setEchoChar(passwordField.getEchoChar());
        }
        return passwordField;
    }

    private String getHost() {
        return new String(getHostField().getText());
    }

    private String getUsername() {
        return new String(getUsernameField().getText());
    }

    /**
     * 
     */
    private String getPassword() {
        return new String(getPasswordField().getPassword());
    }

    /**
     * @return
     */
    private String getPort() {
        return new String(getPortField().getText());
    }

    /**
     * 
     */
    public void okAction() {
        jDialog.setVisible(false);
        jDialog.dispose();
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * 
     */
    public void cancelAction() {
        jDialog.setVisible(false);
        jDialog.dispose();
        isCanceled = true;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getUsernameField() {
        if (usernameField == null) {
            usernameField = new JTextField();
            usernameField.setPreferredSize(new Dimension(150, 20));
            usernameField.setLocation(132, 40);
            usernameField.setSize(150, 20);
            usernameField.setEditable(true);
            usernameField.setText(username);
            usernameField.setFocusable(true);
            usernameField.setSelectionStart(0);
            usernameField.setSelectionEnd(usernameField.getText().length());
        }
        return usernameField;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setBounds(294, 15, 91, 22);
            okButton.setText(I18n.text("OK"));
            //            okButton.addKeyListener(new KeyAdapter() {
            //                public void keyPressed(KeyEvent e) {
            //                    // NeptusLog.pub().info("<###>keyPressed()");
            //                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
            //                        okAction();
            //                }
            //            });
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    okAction();
                }
            });
            GuiUtils.reactEnterKeyPress(okButton);
        }
        return okButton;
    }

    /**
     * This method initializes cancelButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getCancelButton() {
        if (cancelButton == null) {
            cancelButton = new JButton();
            cancelButton.setBounds(294, 50, 91, 22);
            cancelButton.setText(I18n.text("Cancel"));
            //            cancelButton.addKeyListener(new KeyAdapter() {
            //                public void keyPressed(KeyEvent e) {
            //                    NeptusLog.pub().info("<###>keyPressed()");
            //                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            //                        cancelAction();
            //                }
            //            });
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    cancelAction();
                }
            });
            GuiUtils.reactEscapeKeyPress(cancelButton);
        }
        return cancelButton;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getHostField() {
        if (hostField == null) {
            hostField = new JTextField();
            // hostField.setBounds(new java.awt.Rectangle(132,13,150,20));
            hostField.setSize(150, 20);
            hostField.setText(host);
            hostField.setEditable(true);
            hostField.setPreferredSize(new Dimension(150, 20));
            hostField.setLocation(132, 15);
            hostField.setSelectionStart(0);
            hostField.setSelectionEnd(hostField.getText().length());
        }
        return hostField;
    }

    /**
     * This method initializes imageConnectSSH
     * 
     * @return pt.lsts.neptus.gui.ImagePanel
     */
    private ImagePanel getImageConnectSSH() {
        if (imageConnectSSH == null) {
            imageConnectSSH = new ImagePanel(SSH_CONNECT_IMAGE);
            imageConnectSSH.setBounds(new Rectangle(7, 14, 48, 48));
        }
        return imageConnectSSH;
    }

    /**
     * This method initializes portField
     * 
     * @return javax.swing.JFormattedTextField
     */
    private JFormattedTextField getPortField() {
        if (portField == null) {
            portField = new JFormattedTextField(iFormat);
            portField.setText("" + port);
            portField.setBounds(new Rectangle(132, 90, 150, 20));
            portField.setSelectionStart(0);
            portField.setSelectionEnd(portField.getText().length());
        }
        return portField;
    }

    /**
     * @param host
     * @param username
     * @param password
     * @param port
     * @return A String array with [host, username, password, port] or an empty String array if user canceled.
     */
    public static String[] showConnectionDialog(String host, String username, String password, int port) {
        return showConnectionDialog(host, username, password, port, (Window) null);
    }

    public static String[] showConnectionDialog(String host, String username, String password, int port,
            boolean hostEnable, boolean portEnable, boolean usernameEnable, boolean passwordEnable) {
        return showConnectionDialog(host, username, password, port, (Window) null,
                hostEnable, portEnable, usernameEnable, passwordEnable);
    }

    /**
     * @param host
     * @param username
     * @param password
     * @param port
     * @param title
     * @return A String array with [host, username, password, port] or an empty String array if user canceled.
     */
    public static String[] showConnectionDialog(String host, String username, String password, int port, String title) {
        return showConnectionDialog(host, username, password, port, title, null);
    }

    public static String[] showConnectionDialog(String host, String username, String password, int port, String title,
            boolean hostEnable, boolean portEnable, boolean usernameEnable, boolean passwordEnable) {
        return showConnectionDialog(host, username, password, port, title, null,
                hostEnable, portEnable, usernameEnable, passwordEnable);
    }

    /**
     * @param host
     * @param username
     * @param password
     * @param port
     * @param parentWindow
     * @return A String array with [host, username, password, port] or an empty String array if user canceled.
     */
    public static String[] showConnectionDialog(String host, String username, String password, int port,
            Window parentWindow) {
        return showConnectionDialog(host, username, password, port, null, parentWindow);
    }

    public static String[] showConnectionDialog(String host, String username, String password, int port,
            Window parentWindow, boolean hostEnable, boolean portEnable, boolean usernameEnable, boolean passwordEnable) {
        return showConnectionDialog(host, username, password, port, null, parentWindow,
                hostEnable, portEnable, usernameEnable, passwordEnable);
    }

    /**
     * @param host
     * @param username
     * @param password
     * @param port
     * @param title
     * @param parentWindow
     * @return A String array with [host, username, password, port] or an empty String array if user canceled.
     */
    public static String[] showConnectionDialog(String host, String username, String password, int port,
            String title, Window parentWindow) {

        return showConnectionDialog(host, username, password, port, title, parentWindow, true, true, true,true);
    }

    public static String[] showConnectionDialog(String host, String username, String password, int port,
            String title, Window parentWindow, boolean hostEnable, boolean portEnable, boolean usernameEnable,
            boolean passwordEnable) {

        SSHConnectionDialog scd = new SSHConnectionDialog(host, username, password, port, title,
                hostEnable, portEnable, usernameEnable, passwordEnable);
        scd.setParentWindow(parentWindow);
        scd.getJDialog();
        if (scd.isCanceled) {
            return new String[0];
        }
        String[] ret = new String[4];
        ret[0] = scd.getHost();
        ret[1] = scd.getUsername();
        ret[2] = scd.getPassword();
        ret[3] = scd.getPort();
        return ret;
    }

    public class SSHConnectionFocusTraversalPolicy extends FocusTraversalPolicy {

        @Override
        public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
            if (aComponent.equals(hostField)) {
                return usernameField;
            }
            else if (aComponent.equals(usernameField)) {
                return passwordField;
            }
            else if (aComponent.equals(passwordField)) {
                return portField;
            }
            else if (aComponent.equals(portField)) {
                return okButton;
            }
            else if (aComponent.equals(okButton)) {
                return cancelButton;
            }
            else if (aComponent.equals(cancelButton)) {
                return hostField;
            }
            return passwordField;
        }

        @Override
        public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
            if (aComponent.equals(hostField)) {
                return cancelButton;
            }
            else if (aComponent.equals(usernameField)) {
                return hostField;
            }
            else if (aComponent.equals(passwordField)) {
                return usernameField;
            }
            else if (aComponent.equals(portField)) {
                return passwordField;
            }
            else if (aComponent.equals(okButton)) {
                return portField;
            }
            else if (aComponent.equals(cancelButton)) {
                return okButton;
            }
            return passwordField;
        }

        @Override
        public Component getDefaultComponent(Container focusCycleRoot) {
            return passwordField;
        }

        @Override
        public Component getLastComponent(Container focusCycleRoot) {
            return cancelButton;
        }

        @Override
        public Component getFirstComponent(Container focusCycleRoot) {
            return passwordField;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        showConnectionDialog("localhost", "root", "djhgjg", 80, "Proxy Configuration");
    }
}

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
class IntFormat extends DecimalFormat {
    public IntFormat() {
        super();
        this.setGroupingUsed(false);
        this.setParseIntegerOnly(true);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        StringBuffer valBuf = super.format(number, result, fieldPosition);
        long val = Long.parseLong(valBuf.toString());
        if (val < 0)
            valBuf = valBuf.deleteCharAt(0);
        return valBuf;
    }
}
