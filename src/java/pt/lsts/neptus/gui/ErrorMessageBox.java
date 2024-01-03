/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

@SuppressWarnings("serial")
public class ErrorMessageBox extends JDialog {

    private JPanel backPanel = null;
    private JLabel jLabel = null;
    private JLabel jLabel1 = null;
    private JLabel jLabel2 = null;
    private JLabel exception = null;
    private JLabel message = null;
    private JLabel cause = null;

    private JScrollPane jScrollPane = null;
    private JTextArea stackTrace = null;
    private JLabel jLabel11 = null;
    private JButton okButton = null;

    /**
     * This method initializes
     * 
     */
    public ErrorMessageBox() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        this.setSize(new Dimension(530, 316));
        this.setTitle(I18n.text("Exception Thrown"));
        this.setContentPane(getBackPanel());
    }

    /**
     * This method initializes backPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getBackPanel() {
        if (backPanel == null) {
            jLabel11 = new JLabel();
            jLabel11.setText("<html><b>" + I18n.text("Stack Trace"));
            jLabel11.setBounds(new Rectangle(8, 80, 73, 16));
            cause = new JLabel();
            cause.setText("");
            cause.setBounds(new Rectangle(88, 56, 425, 17));

            exception = new JLabel();
            exception.setText("");
            exception.setBounds(new Rectangle(88, 8, 425, 17));

            message = new JLabel();
            message.setText("");
            message.setBounds(new Rectangle(88, 32, 425, 17));

            jLabel1 = new JLabel();
            jLabel1.setText("<html><b>" + I18n.text("Message") + ":");
            jLabel1.setBounds(new Rectangle(8, 32, 73, 17));
            jLabel = new JLabel();
            jLabel.setText("<html><b>" + I18n.text("Exception") + ":");
            jLabel.setBounds(new Rectangle(8, 8, 73, 17));
            jLabel2 = new JLabel();
            jLabel2.setText("<html><b>" + I18n.text("Cause") + ":");
            jLabel2.setBounds(new Rectangle(8, 56, 73, 17));
            backPanel = new JPanel();
            backPanel.setLayout(null);
            backPanel.add(jLabel, null);
            backPanel.add(jLabel1, null);
            backPanel.add(jLabel2, null);
            backPanel.add(exception, null);
            backPanel.add(cause, null);
            backPanel.add(message, null);
            backPanel.add(getJScrollPane(), null);
            backPanel.add(jLabel11, null);
            backPanel.add(getOkButton(), null);
            // backPanel.add(getSaveButton(), null);
        }
        return backPanel;
    }

    /**
     * This method initializes jScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setBounds(new Rectangle(8, 96, 505, 144));
            jScrollPane.setViewportView(getStackTrace());
        }
        return jScrollPane;
    }

    /**
     * This method initializes stackTrace
     * 
     * @return javax.swing.JTextArea
     */
    private JTextArea getStackTrace() {
        if (stackTrace == null) {
            stackTrace = new JTextArea();
            stackTrace.setEditable(false);
            stackTrace.setBackground(Color.white);
        }
        return stackTrace;
    }

    /**
     * This method initializes okButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getOkButton() {
        if (okButton == null) {
            okButton = new JButton();
            okButton.setText(I18n.text("OK"));
            okButton.setBounds(new Rectangle(432, 248, 81, 25));
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    dispose();
                }
            });
        }
        return okButton;
    }

    public static void showDialog(Component parent, Exception e) {
        ErrorMessageBox msgBox = new ErrorMessageBox();
        msgBox.setTitle(I18n.textf("%className thrown", e.getClass().getSimpleName()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(baos));
        msgBox.exception.setText(e.getClass().getSimpleName());
        if (e.getMessage() != null)
            msgBox.message.setText(e.getMessage().toString());
        else
            msgBox.message.setText("(null)");

        if (e.getCause() != null)
            msgBox.cause.setText(e.getCause().toString());
        else
            msgBox.cause.setText("(null)");

        msgBox.stackTrace.setText(baos.toString());
        // msgBox.setModal(true);
        msgBox.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerOnScreen(msgBox);
        msgBox.setResizable(false);
        msgBox.setVisible(true);

    }

    public static void showDialog(Component parent, Exception e, String msg) {

        ErrorMessageBox msgBox = new ErrorMessageBox();
        msgBox.setTitle(I18n.textf("%className thrown", e.getClass().getSimpleName()));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(baos));
        msgBox.exception.setText(e.getClass().getSimpleName());
        if (msg != null)
            msgBox.message.setText(msg);
        else
            msgBox.message.setText("(null)");

        if (e.getCause() != null)
            msgBox.cause.setText(e.getCause().toString());
        else
            msgBox.cause.setText("(null)");

        msgBox.stackTrace.setText(baos.toString());
        // msgBox.setModal(true);
        msgBox.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerOnScreen(msgBox);
        msgBox.setResizable(false);
        msgBox.setVisible(true);

    }

    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        showDialog(null, new NullPointerException("sdfsdf"));
    }
}
