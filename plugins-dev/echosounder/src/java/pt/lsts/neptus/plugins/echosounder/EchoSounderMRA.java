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
 * Author: José Pinto
 * 2009/09/05
 */
package pt.lsts.neptus.plugins.echosounder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import pt.lsts.imc.Distance;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(author = "zp", active = false, name = "Echo Sounder Analysis", icon = "pt/lsts/neptus/plugins/echosounder/echosounder.png")
public class EchoSounderMRA extends JPanel implements MRAVisualization {

    private static final long serialVersionUID = 1L;

    protected IMraLogGroup source;
    protected MRAPanel mraPanel;

    @NeptusProperty
    public ColorMap colormap = ColorMapFactory.createJetColorMap();

    @NeptusProperty(name = "Draw distance", description = "Draw black line indicating measured distance")
    public boolean drawLine = false;

    protected BufferedImage image = null;
    protected int imageWidth;
    protected int imageHeight;

    protected int maxRange;
    protected int minRange;

    private EchoSounderMRARuler ruler;

    public EchoSounderMRA(MRAPanel panel) {
        mraPanel = panel;

        this.addComponentListener(new ComponentAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.ComponentAdapter#componentResized(java.awt.event.ComponentEvent)
             */
            @Override
            public void componentResized(ComponentEvent e) {
                generateImage();
            }
        });
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return true;
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        this.source = source;

        setLayout(new BorderLayout());

        getSonarDataValues();
        generateImage();
        ruler = new EchoSounderMRARuler(this);

        return this;
    }

    /**
     * Gets maxRange and minRange from SonarData msgs sets up buffered image width and height
     */
    private void getSonarDataValues() {
        int c = 0;
        for (IMCMessage msg : source.getLsfIndex().getIterator("SonarData")) {
            if (msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                c++;
                imageHeight = msg.getRawData("data").length;
                maxRange = msg.getInteger("max_range");
                minRange = msg.getInteger("min_range");
            }
        }
        imageWidth = c;
    }

    /**
     * Generates buffered image from echousounder data Old msgs - SonarData msgs Current msgs Distance
     */
    public void generateImage() {
        NeptusLog.pub().info("<###>Generating Echo sounder image");

        image = ImageUtils.createCompatibleImage(imageWidth, imageHeight, Transparency.OPAQUE);
        Graphics2D g2d = (Graphics2D) image.getGraphics();

        // for old messages
        int x = 0;

        for (IMCMessage msg : source.getLsfIndex().getIterator("SonarData")) {
            if (msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                int y = 0;
                for (byte b : msg.getRawData("data")) {
                    image.setRGB(x, y,
                            colormap.getColor(new Byte(b).doubleValue() * 2 / 255).getRGB());
                    y++;
                }
                x++;
            }
        }

        if (drawLine) {
            // Sonar Data is now stored on Distance msgs
            x = 0;

            int prevX = 0, prevY = 0;

            for (int j = source.getLsfIndex().getFirstMessageOfType(Distance.ID_STATIC); j != -1; j = source
                    .getLsfIndex().getNextMessageOfType(Distance.ID_STATIC, j)) {
                if (source.getLsfIndex().entityNameOf(j).equals("Echo Sounder")) {
                    // In case there is some more Distance messages that Echo Sounder sonar data points
                    if (x >= imageWidth)
                        break;
                    double y = imageHeight - (500 / maxRange * source.getLsfIndex().getMessage(j).getDouble("value"))
                            - 1;
                    image.setRGB(x, (int) y, Color.BLACK.getRGB());

                    if (x != 0) {
                        g2d.setColor(Color.BLACK);
                        g2d.drawLine(prevX, prevY, x, (int) y);
                        prevX = x;
                        prevY = (int) y;
                    }
                    else {
                        prevX = x;
                        prevY = (int) y;
                    }
                    x++;
                }
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(image, 0 + EchoSounderMRARuler.RULER_WIDTH + 1, 0, this.getWidth(), this.getHeight(), 0, 0,
                imageWidth, imageHeight, null);
        ruler.paintComponent(g);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SonarData") != null && source.getLsfIndex().getEntityId("Echo Sounder") != 255;
    }

    @Override
    public Double getDefaultTimeStep() {
        return MRAProperties.defaultTimestep;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(this.getClass()), 16, 16);
    }

    @Override
    public String getName() {
        return I18n.text(PluginUtils.getPluginName(this.getClass()));
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void onCleanup() {
        mraPanel = null;
    }

    @Override
    public void onHide() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onShow() {
        // nothing
    }
}
