/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2009/09/01
 */
package pt.up.fe.dceg.neptus.plugins.echosounder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ColorMapListRenderer;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageEditorImc;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", name = "Echo Sounder Panel", icon = "pt/up/fe/dceg/neptus/plugins/echosounder/echosounder.png", description = "This panel is used to control and receive data from an Echo Sounder sensor")
public class EchoSounderPanel extends SimpleSubPanel implements ConfigurationListener, NeptusMessageListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(description = "The colormap to be used in the plot", name = "Color Map")
    public ColorMap cmap = ColorMapFactory.createCopperColorMap();

    protected MultibeamPanel beamPanel = new MultibeamPanel();
    protected JComboBox<?> mapcombo = new JComboBox<Object>(ColorMap.cmaps);
    protected IMCMessage settingsMessage = IMCDefinition.getInstance().create("ImagenexSonarConfig");
    private boolean configReceived = false;
    protected Thread reqThread;

    @Override
    public void initSubPanel() {

        reqThread = new Thread() {
            long millisBetweenRetries = 30000;

            @Override
            public void run() {
                while (!configReceived && getConsole() != null) {
                    try {
                        Thread.sleep(millisBetweenRetries);
                        if (getConsole().getMainSystem() != null)
                            send(IMCDefinition.getInstance().create("RequestImagenexSonarConfig"));
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn("EchoSounderPanel thread stopped. : " + e);
                    }
                }
            }
        };

        reqThread.start();
    }

    @Override
    public void cleanSubPanel() {
        reqThread.interrupt();
    }

    public EchoSounderPanel(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        add(beamPanel, BorderLayout.CENTER);
        JPanel aux = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        JButton settings = new JButton("Settings");
        ToolbarButton snap = new ToolbarButton(new AbstractAction("Save snapshot", ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/echosounder/snapshot.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Temporarily with no text due to VarTree removal @jqcorreia
                beamPanel.paintImmediately(beamPanel.getVisibleRect());

                GuiUtils.takeSnapshot(beamPanel, "EchoSounder");

                beamPanel.setTextToDisplay(null);
                beamPanel.paintImmediately(beamPanel.getVisibleRect());
            }
        });
        settings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean cancel = MessageEditorImc.showProperties(settingsMessage,
                        SwingUtilities.getWindowAncestor(EchoSounderPanel.this), true);

                if (!cancel) {
                    send(settingsMessage);
                    send(IMCDefinition.getInstance().create("RequestImagenexSonarConfig"));
                }
            }
        });
        mapcombo.setRenderer(new ColorMapListRenderer());
        mapcombo.setPreferredSize(new Dimension(150, (int) settings.getPreferredSize().getHeight()));
        mapcombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                cmap = (ColorMap) mapcombo.getSelectedItem();
                beamPanel.setColorMap(cmap);
            }
        });
        aux.add(snap);
        aux.add(mapcombo);
        aux.add(settings);
        add(aux, BorderLayout.SOUTH);
    }

    @Override
    public void propertiesChanged() {
        mapcombo.setSelectedItem(cmap);
    }

    @Override
    public String[] getObservedMessages() {
        // FIXME ImagenexReturnData and ImagenexSonarConfig don't exist
        return new String[] { "BottomDistance", "ImagenexReturnData", "ImagenexSonarConfig" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        boolean configChanged = false;
        boolean drawData = false;
        double depth = 0;
        double[] vals = null;

        if (message.getAbbrev().equals("ImagenexSonarConfig")) {
            // FIXME This is seriously outdated incorrect
            // settingsMessage.setValue(variableNames[i].substring(variableNames[i].lastIndexOf(".") + 1),
            // currentValues[i]);
            configChanged = true;
        }
        else if (message.getAbbrev().equals("ImagenexReturnData")) {
            // FIXME This is seriously outdated and incorrect
            // if ("ImagenexReturnData.angle".equals(variableNames[i])) {
            // depth = vt.getValueAsDouble("ImagenexReturnData.angle");
            // drawData = true;
            // }
            //
            // if ("ImagenexReturnData.echo_points".equals(variableNames[i])) {
            // // shouldn't this be transferred?
            // int bitsperpoint = 8;
            // double maxVal = Math.pow(2, bitsperpoint) - 1;
            //
            // byte[] data = ((RawData) vt.getValue("ImagenexReturnData.echo_points")).getAsByteArray();
            // vals = new double[data.length];
            //
            // for (int j = 0; j < data.length; j++)
            // vals[j] = (double) data[j] / maxVal;
            //
            // drawData = true;
            // }
        }
        else if (message.getAbbrev().equals("BottomDistance")) {
            depth = message.getDouble("BottomDistance");
            drawData = true;

            int bitsperpoint = 8;
            double maxVal = Math.pow(2, bitsperpoint) - 1;

            byte[] data = message.getRawData("data_points");
            vals = new double[data.length];

            for (int j = 0; j < data.length; j++)
                vals[j] = data[j] / maxVal;

            drawData = true;
        }

        if (configChanged) {
            configReceived = true;
            beamPanel.setRange(settingsMessage.getDouble("range"));
        }

        if (drawData)
            beamPanel.updateBeams(vals, depth);
    }
}
