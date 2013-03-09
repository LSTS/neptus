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
 * 9/Abr/2006
 */
package pt.up.fe.dceg.neptus.util.comm.ssh;

import java.awt.BorderLayout;
import java.awt.Window;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.ShellPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class SshShellPannel extends SubPanel {
    private ShellPanel shellPannel = null;
    private JPanel contPanel = null;
    private JPanel connectPanel = null;
    private JButton connectButton = null;
    private JButton disconnectButton = null;
    protected SSHShell shell;

    protected String host = "localhost";
    protected int port = 22;
    protected String user = "";
    protected String password = "";

    /**
     * This method initializes
     * 
     */
    public SshShellPannel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.add(getContPanel(), BorderLayout.CENTER);
    }

    /**
     * This method initializes shellPannel
     * 
     * @return pt.up.fe.dceg.neptus.gui.ShellPannel
     */
    private ShellPanel getShellPannel() {
        if (shellPannel == null) {
            shellPannel = new ShellPanel();
            shellPannel.setEnabled(false);
            shellPannel.setShellPrompt("ssh $>");
            // shellPannel.setShellPrompt("");
            shellPannel.clear();
            shellPannel.add(getConnectPanel(), java.awt.BorderLayout.SOUTH);
        }
        return shellPannel;
    }

    /**
     * This method initializes contPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getContPanel() {
        if (contPanel == null) {
            contPanel = new JPanel();
            contPanel.setLayout(new BorderLayout());
            contPanel.add(getShellPannel(), BorderLayout.CENTER);
        }
        return contPanel;
    }

    /**
     * This method initializes connectPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getConnectPanel() {
        if (connectPanel == null) {
            connectPanel = new JPanel();
            connectPanel.add(getConnectButton(), null);
            connectPanel.add(getDisconnectButton(), null);
        }
        return connectPanel;
    }

    /**
     * This method initializes connectButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getConnectButton() {
        if (connectButton == null) {
            connectButton = new JButton();
            connectButton.setText(I18n.text("connect"));
            connectButton.setPreferredSize(new java.awt.Dimension(100, 16));
            connectButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Window parentWindow = SwingUtilities.windowForComponent(shellPannel);
                    String[] cData = SSHConnectionDialog.showConnectionDialog(host, user, password, port, parentWindow);
                    if (cData.length == 0) {
                        return;
                    }

                    host = cData[0];
                    user = cData[1];
                    password = cData[2];
                    try {
                        port = Integer.parseInt(cData[3]);
                    }
                    catch (NumberFormatException e2) {
                        port = 22;
                    }
                    shell = new SSHShell(host, user, password, port);
                    // shell.setHost("192.168.106.32");
                    // shell.setUser("pdias");
                    if (host.equalsIgnoreCase("") || user.equalsIgnoreCase("")) {
                        JOptionPane.showMessageDialog(SshShellPannel.this, I18n.text("Invalid data!!"));
                        return;
                    }

                    if (!shell.prepareChannel()) {
                        shell.sessionCleanup();
                        shell = null;
                    }
                    if (shell == null) {
                        JOptionPane.showMessageDialog(SshShellPannel.this, I18n.text("Connection not possible!!"));
                        return;
                    }
                    // shellPannel.prepareInStreams();
                    // shell.getChannel().setInputStream(shellPannel.getIn());
                    try {
                        shellPannel.setIn(shell.getChannel().getInputStream());
                        // shell.getChannel().setOutputStream(System.out);
                    }
                    catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    // shell.getChannel().setOutputStream(shellPannel.getOut());

                    try {
                        shellPannel.setOut(shell.getChannel().getOutputStream());
                        // shell.getChannel().setInputStream(System.in);
                    }
                    catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                    if (!shell.connectChannel()) {
                        shell.channelCleanup();
                        shell.sessionCleanup();
                        shell = null;
                        JOptionPane.showMessageDialog(SshShellPannel.this, I18n.text("Connection not possible!!"));
                        return;
                    }
                    shellPannel.startInputProcess();

                    getShellPannel().enable();
                    getConnectButton().setEnabled(false);
                    getDisconnectButton().setEnabled(true);
                }
            });
        }
        return connectButton;
    }

    /**
     * This method initializes disconnectButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getDisconnectButton() {
        if (disconnectButton == null) {
            disconnectButton = new JButton();
            disconnectButton.setText(I18n.text("disconnect"));
            disconnectButton.setEnabled(false);
            disconnectButton.setPreferredSize(new java.awt.Dimension(100, 16));
            disconnectButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (shell != null) {
                        shell.channelCleanup();
                        shell.sessionCleanup();
                    }
                    getShellPannel().disable();
                    getConnectButton().setEnabled(true);
                    getDisconnectButton().setEnabled(false);
                }
            });
        }
        return disconnectButton;
    }

}
