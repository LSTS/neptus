/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: tsmarques
 * 30 Jun 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.HyperSpecData;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author tsmarques
 *
 */
@LayerPriority(priority=-10)
@PluginDescription(icon="pt/lsts/neptus/mra/replay/globe.png")
public class HyperspectralReplay extends JFrame implements LogReplayLayer {
    /* frames sent from a higher or lower altitude will be drawn on the map scaled up or down, respectively */
    private static final double DEFAULT_ALTITUDE = 100; /* in meters */

    /* wavelength selection panel */
    private JPanel mainPanel;
    private JComboBox<Double> wavelengths;
    private JButton selectButton;

    private boolean firstPaint = true;
    private BufferedImage verticalDisplay = HyperspecUtils.initVerticalDisplay(640, 250);
    private final AlphaComposite composite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, HyperspectralViewer.FRAME_OPACITY);
    private final AffineTransform transform = new AffineTransform();

    private boolean dataParsed = false;
    public double selectedWavelength = 0;
    private final HashMap<Double, List<HyperspectralData>> dataset = new HashMap<>();
    List<HyperspectralData> dataList;

    public HyperspectralReplay() {
        super();
        initWavelenSelectionPanel();        
    }

    private void initWavelenSelectionPanel() {
        this.setSize(new Dimension(300, 70));
        this.setTitle("Select wavelength");

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setPreferredSize(this.getPreferredSize());

        wavelengths = new JComboBox<Double>();
        wavelengths.setSize(new Dimension(100, 40));
        ((JLabel)wavelengths.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);

        selectButton = new JButton("Show data");
        selectButton.setSize(new Dimension(100, 40));
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!dataParsed)
                    return;
                Object selection = (double) wavelengths.getSelectedItem();

                if(selection != null) {
                    synchronized(verticalDisplay) {
                        selectedWavelength = (double) selection;
                        dataList = dataset.get(selectedWavelength);
                        verticalDisplay = HyperspecUtils.initVerticalDisplay(640, 250);
                    }
                }
            }
        });

        this.add(mainPanel);
        mainPanel.add(wavelengths, BorderLayout.NORTH);
        mainPanel.add(selectButton, BorderLayout.SOUTH);
        this.setLocationRelativeTo(null);
    }

    @Override
    public String getName() {
        return I18n.text("Hyperspectral Replay");
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(firstPaint) {
            this.setVisible(true);
            firstPaint = false;

            int newX = -((HyperspectralViewer.MAX_FREQ / 2)) + (HyperspectralViewer.FRAME_HEIGHT / 2);
            int newY = (renderer.getHeight() - HyperspectralViewer.FRAME_HEIGHT) / 2;

            transform.translate(newX, newY);
            transform.rotate(Math.toRadians(-90), verticalDisplay.getWidth() / 2, verticalDisplay.getHeight() / 2);

            firstPaint = false;
        }
        else {
            synchronized(verticalDisplay) {
                g.setTransform(transform);
                g.setComposite(composite);
                g.drawImage(verticalDisplay, 0, 0, renderer);
            }
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        return source.getLog("HyperSpecData") != null;
    }


    @Override
    public void parse(IMraLogGroup source) {
        Thread t = new Thread(HyperspectralReplay.class.getSimpleName() + " " + source.getDir().getParent()) {

            @Override
            public void run() {
                IMraLog hyperspecLog = source.getLog("HyperSpecData");
                IMraLog esLog = source.getLog("EstimatedState");

                HyperSpecData msg = (HyperSpecData) hyperspecLog.firstLogEntry();
                while(msg != null)  {
                    EstimatedState closestState = (EstimatedState)esLog.getEntryAtOrAfter(msg.getTimestampMillis());
                    double dataWavelen = msg.getWavelen();

                    List<HyperspectralData> dataList;
                    if(dataset.containsKey(dataWavelen))
                        dataList = dataset.get(dataWavelen);
                    else {
                        dataList = new LinkedList<>();
                        dataset.put(dataWavelen, dataList);
                        /* add to combo box */
                        wavelengths.addItem(dataWavelen);
                    }
                    dataList.add(new HyperspectralData(msg, closestState, false));
                    msg = (HyperSpecData) hyperspecLog.nextLogEntry();
                }
            }
        };
        t.setDaemon(true);
        t.start();

        if(t.isAlive())
            dataParsed = true;
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "HyperSpecData" };
    }

    @Override
    public void onMessage(IMCMessage message) {
        synchronized(verticalDisplay) {
            verticalDisplay = HyperspecUtils.updateVerticalDisplay(verticalDisplay, message.getRawData("data"), 
                    HyperspectralViewer.MAX_FREQ, 
                    HyperspectralViewer.FRAME_HEIGHT);
        }
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }
}
