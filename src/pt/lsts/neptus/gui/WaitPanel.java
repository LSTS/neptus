/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 1/Nov/2005
 */

package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class WaitPanel extends JPanel implements Runnable {

    private static final long serialVersionUID = -8115526495271837748L;
    private JProgressBar progressBar = null;
    private JDialog jDialog = null;
    private JPanel dialogContentPane = null;
    private JFrame jFrame = null;
    private JPanel frameContentPane = null;
    private Window parentComponent = null;
    // private boolean modal = false;
    private ModalityType modalityType = ModalityType.MODELESS;
    private Thread loaderThread;
    private JLabel jLabel = null;

    private boolean forceRepaint = false;

    /**
     * This is the default constructor
     */
    public WaitPanel() {
        super();
        initialize();
    }

    /**
     * 
     * @param forceRepaint
     */
    public WaitPanel(boolean forceRepaint) {
        super();
        this.forceRepaint = forceRepaint;
        initialize();
    }

    /**
     * This method initializes progressBar
     * 
     * @return javax.swing.JProgressBar
     */
    private JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new JProgressBar();
            progressBar.addChangeListener(new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent e) {
                    if (forceRepaint) {
                        RepaintManager rpm = RepaintManager.currentManager(getProgressBar());
                        rpm.markCompletelyDirty(getProgressBar());
                        rpm.paintDirtyRegions();
                    }
                    else
                        repaint();
                }
            });
            progressBar.setIndeterminate(true);
        }
        return progressBar;
    }

    /**
     * This method initializes jDialog
     * 
     * @return javax.swing.JDialog
     */
    private JDialog getJDialog() {
        if (jDialog == null) {
            if (parentComponent == null)
                jDialog = new JDialog();
            else if (parentComponent instanceof JDialog)
                jDialog = new JDialog((JDialog) parentComponent);
            else if (parentComponent instanceof JFrame)
                jDialog = new JDialog((JFrame) parentComponent);
            jDialog.setContentPane(getDialogContentPane());
            jDialog.setUndecorated(true);
            jDialog.setModal(true);
            jDialog.setAlwaysOnTop(true);
        }
        return jDialog;
    }

    /**
     * This method initializes dialogContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getDialogContentPane() {
        if (dialogContentPane == null) {
            dialogContentPane = new JPanel();
            dialogContentPane.setLayout(new BorderLayout());
        }
        return dialogContentPane;
    }

    /**
     * This method initializes jFrame
     * 
     * @return javax.swing.JFrame
     */
    private JFrame getJFrame() {
        if (jFrame == null) {
            jFrame = new JFrame();
            jFrame.setTitle("Wait please...");
            jFrame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            jFrame.setContentPane(getFrameContentPane());
            jFrame.setUndecorated(true);
            // jFrame.setIconImage(GuiUtils.getImage("images/neptus-icon.png"));
            jFrame.setIconImages(ConfigFetch.getIconImagesForFrames());
            jFrame.setAlwaysOnTop(true);
        }
        return jFrame;
    }

    /**
     * This method initializes frameContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setLayout(new BorderLayout());
        }
        return frameContentPane;
    }

    /**
     * @return
     */
    public String getText() {
        return jLabel.getText();
    }

    /**
     * @param text
     */
    public void setText(String text) {
        jLabel.setText(text);
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        // System.err.println("~~");
        jLabel = new JLabel();
        jLabel.setText("Wait please...");
        jLabel.setBackground(new Color(250, 250, 250));
        jLabel.setOpaque(true);
        jLabel.setForeground(new Color(164, 164, 164));
        jLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.setLayout(new BorderLayout());
        this.setSize(242, 30);
        this.setPreferredSize(new Dimension(242, 30));
        this.add(getProgressBar(), BorderLayout.CENTER);
        this.add(jLabel, BorderLayout.NORTH);
        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    int ret = JOptionPane.showConfirmDialog(WaitPanel.this,
                            "Close this wait panel?", "Wait Panel", JOptionPane.YES_NO_OPTION);
                    switch (ret) {
                        case JOptionPane.YES_OPTION:
                            WaitPanel.this.stop();
                            break;

                        default:
                            break;
                    }
                }
            }
        });
    }

    /**
     * @return
     */
    public boolean start() {
        if (loaderThread != null)
            return false;
        loaderThread = new Thread((Runnable) this);
        loaderThread.setPriority(Thread.MAX_PRIORITY);
        parentComponent = null;
        // timer.start();
        loaderThread.start();
        return true;
    }

    /**
     * @param parent
     * @param modal
     * @return
     */
    public boolean start(JDialog parent, boolean modal) {
        return start((Window) parent, modal ? Dialog.DEFAULT_MODALITY_TYPE
                : Dialog.ModalityType.MODELESS);
    }

    /**
     * @param parent
     * @param modalityType
     * @return
     */
    public boolean start(JDialog parent, ModalityType modalityType) {
        return start((Window) parent, modalityType);
    }

    /**
     * @param parent
     * @param modal
     * @return
     */
    public boolean start(JFrame parent, boolean modal) {
        return start((Window) parent, modal ? Dialog.DEFAULT_MODALITY_TYPE
                : Dialog.ModalityType.MODELESS);
    }

    /**
     * @param parent
     * @param modalityType
     * @return
     */
    public boolean start(JFrame parent, ModalityType modalityType) {
        return start((Window) parent, modalityType);
    }

    /**
     * @param parent
     * @param modal
     * @return
     */
    private boolean start(Window parent, ModalityType modalityType) {
        if (loaderThread != null)
            return false;
        loaderThread = new Thread((Runnable) this);
        parentComponent = parent;
        // this.modal = modal;
        this.modalityType = modalityType;
        // timer.start();
        loaderThread.start();
        return true;
    }

    /**
     * @return
     */
    public boolean stop() {
        if (loaderThread == null)
            return false;
        loaderThread = null;
        // timer.stop();
        // getJDialog().setVisible(false);
        getJDialog().dispose();
        getJFrame().dispose();
        jDialog = null;
        jFrame = null;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // System.err.println("--");
        if (parentComponent != null) {
            JDialog dialog = getJDialog();
            dialog.getContentPane().add(this);
            // dialog.setModal(modal);
            dialog.setModalityType(modalityType);
            dialog.pack();
            // GuiUtils.centerOnScreen(dialog);
            GuiUtils.centerParent(dialog, parentComponent);
            dialog.setVisible(true);
        }
        else {
            JFrame frame = getJFrame();
            frame.getContentPane().add(this);
            frame.pack();
            GuiUtils.centerOnScreen(frame);
            frame.setVisible(true);
        }

        while (isRunning() && forceRepaint) {
            try {

                RepaintManager rpm = RepaintManager.currentManager(this);
                rpm.markCompletelyDirty(this);
                rpm.paintDirtyRegions();
            }
            catch (RuntimeException e1) {
                // System.err.println("WaitPanel ;-)");
            }
            // System.err.println(";-)");
            try {
                Thread.sleep(200);
            }
            catch (InterruptedException e) {
            }
        }
    }

    public boolean isRunning() {
        if (loaderThread == null)
            return false;
        return true;
    }

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        GuiUtils.setLookAndFeel();
        WaitPanel wait = new WaitPanel();
        JPanel panel = new JPanel();
        panel.setSize(new Dimension(123, 34));
        // panel.setPreferredSize(new Dimension(123,34));
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("North"), BorderLayout.NORTH);
        panel.add(new JLabel("Center"), BorderLayout.CENTER);
        panel.add(new JLabel("South"), BorderLayout.SOUTH);
        panel.add(new JLabel("East"), BorderLayout.EAST);
        panel.add(new JLabel("West"), BorderLayout.WEST);
        JFrame frame = new JFrame();
        frame.add(panel);
        frame.setSize(new Dimension(123, 34));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // GuiUtils.centerOnScreen(frame);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
        wait.start(frame, true);
        Thread.sleep(5000);
        wait.stop();

        wait.start();
        Thread.sleep(5000);
        wait.stop();
    }
}
