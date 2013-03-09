/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20/Jun/2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;


/**
 * @author Paulo Dias
 */
public class PasswordPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private String title = "Enter password";
    private String username = "root";
    private String password = "";
    private boolean savePassword = false;

    private JPasswordField passwordField = null;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;
    private JTextField usernameField = null;
    private JCheckBox savePasswordCheckBox = null;
    private JButton okButton = null;
    private JPanel jContentPane = null;
    private JDialog jDialog = null;

    /**
     * 
     */
    public PasswordPanel() {
        super();
        initialize();
    }

    /**
     * @param title
     * @param values
     */
    public PasswordPanel(String title, String username, String password, boolean savePassword) {
        super();
        this.title = title;
        this.username = username;
        this.password = password;
        this.savePassword = savePassword;
        initialize();
    }

    /**
     * This method initializes jPasswordField
     * 
     * @return javax.swing.JPasswordField
     */
    private JPasswordField getPasswordField() {
        if (passwordField == null) {
            passwordField = new JPasswordField();
            passwordField.setPreferredSize(new java.awt.Dimension(150, 20));
            passwordField.setLocation(92, 38);
            passwordField.setSize(150, 20);
            passwordField.setText(password);
            // passwordField.setFocusable(true);
            passwordField.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    // System.out.println("keyPressed()");
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        okAction();
                }
            });
            passwordField.setSelectionStart(0);
            passwordField.setSelectionEnd(passwordField.getPassword().length);
        }
        return passwordField;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getUsernameField() {
        if (usernameField == null) {
            usernameField = new JTextField();
            usernameField.setPreferredSize(new java.awt.Dimension(150, 20));
            usernameField.setLocation(92, 13);
            usernameField.setSize(150, 20);
            usernameField.setEditable(false);
            usernameField.setText(username);
            // usernameField.setFocusable(false);
            usernameField.setSelectionStart(0);
            usernameField.setSelectionEnd(usernameField.getText().length());
        }
        return usernameField;
    }

    /**
     * This method initializes jCheckBox
     * 
     * @return javax.swing.JCheckBox
     */
    private JCheckBox getSavePasswordCheckBox() {
        if (savePasswordCheckBox == null) {
            savePasswordCheckBox = new JCheckBox();
            savePasswordCheckBox.setBounds(23, 61, 121, 21);
            savePasswordCheckBox.setSelected(savePassword);
            savePasswordCheckBox.setText("Save password");
        }
        return savePasswordCheckBox;
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setBounds(179, 89, 79, 22);
            okButton.setText("Ok");
            okButton.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    // System.out.println("keyPressed()");
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        okAction();
                }
            });
            okButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    // System.out.println("actionPerformed()");
                    okAction();
                }
            });
        }
        return okButton;
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
     * This method initializes jDialog
     * 
     * @return javax.swing.JDialog
     */
    private JDialog getJDialog() {
        if (jDialog == null) {
            jDialog = new JDialog();
            jDialog.setContentPane(getJContentPane());
            jDialog.setTitle(title);
            jDialog.setSize(this.getWidth() + 5, this.getHeight() + 35);
            jDialog.setLayout(new BorderLayout());
            jDialog.getContentPane().add(this, BorderLayout.CENTER);
            jDialog.setModal(true);
            jDialog.setAlwaysOnTop(true);
            GuiUtils.centerOnScreen(jDialog);
            jDialog.setResizable(false);
            jDialog.setAlwaysOnTop(true);
            jDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            jDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    JOptionPane.showMessageDialog(jDialog, "You must enter a password!");
                }
            });
            // getJContentPane().paintImmediately(0, 0, 300, 300);
            jDialog.setIconImage(ImageUtils.getImage("images/neptus-icon.png"));
            jDialog.toFront();
            jDialog.setFocusTraversalPolicy(new PasswordPanelFocusTraversalPolicy());
            jDialog.setVisible(true);
        }
        return jDialog;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        jLabel1 = new JLabel();
        jLabel = new JLabel();
        this.setLayout(null);
        this.setSize(278, 122);
        this.setPreferredSize(new java.awt.Dimension(150, 20));
        jLabel.setText("Password");
        jLabel.setLocation(23, 39);
        jLabel.setSize(58, 16);
        jLabel1.setText("Username");
        jLabel1.setLocation(23, 15);
        jLabel1.setSize(59, 16);
        this.add(getPasswordField(), null);
        this.add(jLabel, null);
        this.add(jLabel1, null);
        this.add(getUsernameField(), null);
        this.add(getSavePasswordCheckBox(), null);
        this.add(getOkButton(), null);
    }

    /**
     * 
     */
    private String getPassword() {
        return new String(getPasswordField().getPassword());
    }

    /**
     * 
     */
    private boolean isPasswordSave() {
        return getSavePasswordCheckBox().isSelected();
    }

    /**
	 * 
	 */
    public void okAction() {
        jDialog.setVisible(false);
        jDialog.dispose();
    }

    /**
     * @param title
     * @param values
     * @return
     */
    public static String[] showPasswordDialog(String title, String username, String password, boolean savePassword) {
        PasswordPanel pp = new PasswordPanel(title, username, password, savePassword);
        pp.getJDialog();
        String[] ret = new String[2];
        ret[0] = pp.getPassword();
        ret[1] = Boolean.toString(pp.isPasswordSave());
        return ret;
    }

    public class PasswordPanelFocusTraversalPolicy extends FocusTraversalPolicy {

        public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
            if (aComponent.equals(usernameField)) {
                return passwordField;
            }
            else if (aComponent.equals(passwordField)) {
                return savePasswordCheckBox;
            }
            else if (aComponent.equals(savePasswordCheckBox)) {
                return okButton;
            }
            else if (aComponent.equals(okButton)) {
                return usernameField;
            }
            return passwordField;
        }

        public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
            if (aComponent.equals(usernameField)) {
                return okButton;
            }
            else if (aComponent.equals(passwordField)) {
                return usernameField;
            }
            else if (aComponent.equals(savePasswordCheckBox)) {
                return passwordField;
            }
            else if (aComponent.equals(okButton)) {
                return savePasswordCheckBox;
            }
            return passwordField;
        }

        public Component getDefaultComponent(Container focusCycleRoot) {
            return passwordField;
        }

        public Component getLastComponent(Container focusCycleRoot) {
            return okButton;
        }

        public Component getFirstComponent(Container focusCycleRoot) {
            return passwordField;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        showPasswordDialog("Enter password", "root", "djhgjg", false);
    }
}
