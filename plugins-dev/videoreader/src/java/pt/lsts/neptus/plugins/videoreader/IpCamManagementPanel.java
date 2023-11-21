/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Pedro Gonçalves
 */
package pt.lsts.neptus.plugins.videoreader;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public class IpCamManagementPanel extends JPanel {
    private final VideoReader videoReader;
    private final BiFunction<String, String, Void> setValidUrlFunction;
    private final Runnable connectStreamFunction;
    private ArrayList<Camera> cameraList;
    // JPanel for color state of ping to host IPCam
    private JPanel colorStateIPCam;
    // JButton to confirm IPCam
    private JButton selectIPCam;
    // JComboBox for list of IPCam in ipUrl.ini
    private JComboBox<Camera> ipCamList;
    // row select from string matrix of IPCam List
    private int selectedItemIndex;
    // JLabel for text IPCam Ping
    private JLabel onOffIndicator;
    // JTextField for IPCam name
    private final JTextField fieldName = new JTextField(I18n.text("Name"));
    // JTextField for IPCam ip
    private final JTextField fieldIP = new JTextField(I18n.text("IP"));
    // JTextField for IPCam url
    private final JTextField fieldUrl = new JTextField(I18n.text("URL"));

    public IpCamManagementPanel(VideoReader videoReader, BiFunction<String, String, Void> setValidUrlFunction,
                                Runnable connectStreamFunction) {
        this.videoReader = videoReader;
        this.setValidUrlFunction = setValidUrlFunction;
        this.connectStreamFunction = connectStreamFunction;
        setLayout(new MigLayout());
    }

    public void show(String camUrl) {
        removeAll();
        setLayout(new MigLayout());

        repaintParametersTextFields();
        cameraList = readIPUrl();

        URI uri = Util.getCamUrlAsURI(camUrl);

        JDialog ipCamPing = new JDialog(SwingUtilities.getWindowAncestor(videoReader), I18n.text("Select IPCam"));
        ipCamPing.setResizable(true);
        ipCamPing.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        ipCamPing.setSize(440, 200);
        ipCamPing.setLocationRelativeTo(videoReader);

        ImageIcon imgIPCam = ImageUtils.createImageIcon("images/menus/camera.png");
        if (imgIPCam != null) {
            ipCamPing.setIconImage(imgIPCam.getImage());
        }
        ipCamPing.setResizable(false);
        //ipCamPing.setBackground(Color.GRAY);

        int sel = 0;
        if (uri != null && uri.getScheme() != null) {
            String host = uri.getHost();
            String name = "Stream " + uri.getScheme() + "@" + uri.getPort();
            Camera cam = new Camera(name, host, camUrl);
            NeptusLog.pub().info("Cam > " + cam +  " | host " + host+ " | URI " + camUrl + " | " + cam.getUrl());
            Camera matchCam = cameraList.stream().filter(c -> c.getUrl().equalsIgnoreCase(cam.getUrl()))
                    .findAny().orElse(null);

            if (matchCam == null) {
                cameraList.add(1, cam);
                sel = 1;
            }
            else {
                int index = -1;
                for (int i = 0; i < cameraList.size(); i++) {
                    Camera c = cameraList.get(i);
                    if (c == matchCam) {
                        index = i;
                        break;
                    }
                }
                sel = index;
                if (index < 0) {
                    cameraList.add(1, cam);
                    sel = 1;
                }
            }
        }

        ipCamList = new JComboBox(cameraList.toArray());
        ipCamList.setSelectedIndex(0);
        ipCamList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ipCamList.setEnabled(false);
                selectIPCam.setEnabled(false);
                selectedItemIndex = ipCamList.getSelectedIndex();
                if (selectedItemIndex > 0) {
                    Camera selectedCamera = cameraList.get(selectedItemIndex);
                    colorStateIPCam.setBackground(Color.LIGHT_GRAY);
                    onOffIndicator.setText("---");

                    repaintParametersTextFields(selectedCamera.getName(), selectedCamera.getIp(), selectedCamera.getUrl());

                    AsyncTask task = new AsyncTask() {
                        boolean reachable;

                        @Override
                        public Object run() throws Exception {
                            reachable = Util.hostIsReachable(selectedCamera.getIp());
                            return null;
                        }

                        @Override
                        public void finish() {
                            if (reachable) {
                                selectIPCam.setEnabled(true);
                                //setValidUrlFunction.apply(selectedCamera.getName(), selectedCamera.getUrl());
                                colorStateIPCam.setBackground(Color.GREEN);
                                onOffIndicator.setText("ON");
                                ipCamList.setEnabled(true);
                            }
                            else {
                                selectIPCam.setEnabled(false);
                                colorStateIPCam.setBackground(Color.RED);
                                onOffIndicator.setText("OFF");
                                ipCamList.setEnabled(true);
                            }
                            selectIPCam.validate();
                            selectIPCam.repaint();
                        }
                    };
                    AsyncWorker.getWorkerThread().postTask(task);
                }
                else {
                    colorStateIPCam.setBackground(Color.RED);
                    onOffIndicator.setText("OFF");
                    ipCamList.setEnabled(true);
                    repaintParametersTextFields();
                }
            }
        });
        add(ipCamList, "split 3, width 50:250:250, center");

        colorStateIPCam = new JPanel();
        onOffIndicator = new JLabel(I18n.text("OFF"));
        onOffIndicator.setFont(new Font("Verdana", Font.BOLD, 14));
        colorStateIPCam.setBackground(Color.RED);
        colorStateIPCam.add(onOffIndicator);
        add(colorStateIPCam, "h 30!, w 30!");

        selectIPCam = new JButton(I18n.text("Connect"), imgIPCam);
        selectIPCam.setEnabled(false);
        selectIPCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NeptusLog.pub().info("IPCam Select: " + cameraList.get(selectedItemIndex));
                ipCamPing.setVisible(false);
                Camera selectedCamera = cameraList.get(selectedItemIndex);
                setValidUrlFunction.apply(selectedCamera.getName(), selectedCamera.getUrl());
                videoReader.service.execute(connectStreamFunction);
            }
        });dis
        fieldIP.setEditable(false);
        fieldIP.setFocusable(false);
        add(selectIPCam, "h 30!, wrap");

        JButton addNewIPCam = new JButton(I18n.text("Add New IPCam"));
        addNewIPCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Execute when button is pressed
                if (fieldName.getText().trim().isEmpty()) return;
                if (fieldIP.getText().trim().isEmpty()) return;
                if (fieldUrl.getText().trim().isEmpty()) return;
                if (Util.getHostFromURI(fieldUrl.getText().trim()) == null) return;

                Camera camToAdd = Util.parseLineCamera(String.format("%s#%s#%s\n", fieldName.getText().trim(),
                        fieldIP.getText().trim(), fieldUrl.getText().trim()));
                if (camToAdd != null) {
                    String ipUrlFilename = ConfigFetch.getConfFolder() + "/" + VideoReader.BASE_FOLDER_FOR_URL_INI;
                    Util.addCamToFile(camToAdd, ipUrlFilename);
                    reloadIPCamList();
                }
            }
        });

        JButton removeIpCam = new JButton(I18n.text("Remove IPCam"));
        removeIpCam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Camera camToRemove = (Camera) ipCamList.getSelectedItem();
                String ipUrlFilename = ConfigFetch.getConfFolder() + "/" + VideoReader.BASE_FOLDER_FOR_URL_INI;
                // Execute when button is pressed
                Util.removeCamFromFile(camToRemove, ipUrlFilename);
                reloadIPCamList();
            }
        });

        fieldUrl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateIPFieldFromUrlField();
            }
        });
        fieldUrl.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateIPFieldFromUrlField();
            }
        });

        add(fieldName, "w 410!, wrap");
        add(fieldIP, "w 410!, wrap");
        add(fieldUrl, "w 410!, wrap");
        add(addNewIPCam, "split 2, width 120!, center, gap related");
        add(removeIpCam, "w 120!");

        ipCamPing.add(this);
        ipCamPing.pack();

        if (sel > 0) {
            ipCamList.setSelectedIndex(sel);
        }

        // Show dialog
        ipCamPing.setVisible(true);
    }

    private void updateIPFieldFromUrlField() {
        String host = Util.getHostFromURI(fieldUrl.getText());
        if (host != null) {
            fieldIP.setText(host);
            fieldIP.validate();
            fieldIP.repaint(200);
        }
    }

    private void repaintParametersTextFields(String name, String ip, String url) {
        fieldName.setText(name);
        fieldName.validate();
        fieldName.repaint();
        fieldIP.setText(ip);
        fieldIP.validate();
        fieldIP.repaint();
        fieldUrl.setText(url);
        fieldUrl.validate();
        fieldUrl.repaint();
    }

    private void repaintParametersTextFields() {
        repaintParametersTextFields("NAME", "IP", "URL");
    }

    private void reloadIPCamList() {
        AsyncTask task = new AsyncTask() {
            @Override
            public Object run() throws Exception {
                cameraList = readIPUrl();
                return null;
            }

            @Override
            public void finish() {
                int itemCount = ipCamList.getItemCount();
                ipCamList.removeAllItems();
                for (Camera camera : cameraList) {
                    ipCamList.addItem(camera);
                }

                // If an item was added select that item
                if (itemCount < ipCamList.getItemCount()) {
                    ipCamList.setSelectedIndex(ipCamList.getItemCount() - 1);
                }
            }
        };
        AsyncWorker.getWorkerThread().postTask(task);
    }

    // Read file
    private ArrayList<Camera> readIPUrl() {
        Util.createIpUrlFile();
        File confIni = new File(ConfigFetch.getConfFolder() + "/" + VideoReader.BASE_FOLDER_FOR_URL_INI);
        return Util.readIpUrl(confIni);
    }

    public String getStreamName() {
        return fieldName.getText();
    }

    public String getStreamIp() {
        return fieldIP.getText();
    }

    public String getStreamUrl() {
        return fieldUrl.getText();
    }
}
