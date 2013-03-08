/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/05
 * $Id:: EchoSounderMRA.java 9730 2013-01-18 14:49:49Z jqcorreia          $:
 */
package pt.up.fe.dceg.neptus.plugins.echosounder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.imc.Distance;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.SonarData;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(author="zp", name="Echo Sounder Analysis", icon="pt/up/fe/dceg/neptus/plugins/echosounder/echosounder.png")
public class EchoSounderMRA extends JPanel implements MRAVisualization {

    private static final long serialVersionUID = 1L;

    private IMraLogGroup source;
    protected MRAPanel mraPanel;

    @NeptusProperty
    public ColorMap colormap = ColorMapFactory.createJetColorMap();

    private BufferedImage image = null;
    int imageWidth;
    int imageHeight;

    int maxRange;

    double yscale;

    public EchoSounderMRA(MRAPanel panel) {
        mraPanel = panel;

        this.addComponentListener(new ComponentAdapter() {
            /* (non-Javadoc)
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
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        this.source = source;
        generateImage();

        return this;
    }

    public void generateImage() {
        System.out.println("Generating Echo sounder image");
        int c = 0;
        Iterator<IMCMessage> i= source.getLsfIndex().getIterator("SonarData");
        for(IMCMessage msg = i.next(); i.hasNext(); msg = i.next()) {
            if(msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                c++;
                imageHeight = msg.getRawData("data").length;
                maxRange = msg.getInteger("max_range");
            }
        }
        imageWidth = c;
        image = ImageUtils.createCompatibleImage(imageWidth, imageHeight, Transparency.OPAQUE);
        Graphics2D g2d = (Graphics2D) image.getGraphics();

        i= source.getLsfIndex().getIterator("SonarData");
        int x = 0;
        for(IMCMessage msg = i.next(); i.hasNext(); msg = i.next()) {
            if(msg.getInteger("type") == SonarData.TYPE.ECHOSOUNDER.value()) {
                int y = 0;
                for(byte b : msg.getRawData("data")) {
                    //                    System.out.println(x + " " + y + " " + b + " " + new Byte(b).doubleValue() + " " + colormap.getColor(new Byte(b).doubleValue()).getBlue());
                    image.setRGB(x, imageHeight - y - 1, colormap.getColor(new Byte(b).doubleValue() * 2 / 255).getRGB());
                    y++;
                }
                x++;
            }
        }

        i = source.getLsfIndex().getIterator("Distance");
        x = 0;

        int prevX = 0, prevY = 0;

        for (int j = source.getLsfIndex().getFirstMessageOfType(Distance.ID_STATIC); j != -1; j = source.getLsfIndex().getNextMessageOfType(Distance.ID_STATIC, j)) {
            if(source.getLsfIndex().entityNameOf(j).equals("Echo Sounder")) {
                // In case there is some more Distance messages that Echo Sounder sonar data points
                if(x >= imageWidth)
                    break;
                double y = imageHeight - (500 / maxRange * source.getLsfIndex().getMessage(j).getDouble("value")) - 1;
                image.setRGB(x, (int)y, Color.BLACK.getRGB());

                if(x != 0) {
                    g2d.setColor(Color.BLACK);
                    g2d.drawLine(prevX, prevY, x, (int)y);
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
    @Override
    public void paint(Graphics g) {
        System.out.println(this.getWidth() + " " + this.getHeight());
        g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), 0, 0, imageWidth, imageHeight,null);
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("SonarData") != null && source.getLog("Distance") != null;
    }

    @Override
    public Double getDefaultTimeStep() {
        return NeptusMRA.defaultTimestep;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(this.getClass()), 16, 16);
    }

    public String getName() 
    {		
        return PluginUtils.getPluginName(this.getClass());
    }

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

    public void onShow() {
        //nothing
    }

}
