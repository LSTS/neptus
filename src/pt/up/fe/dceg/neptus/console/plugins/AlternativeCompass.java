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
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ImageUtils;

@PluginDescription(icon = "images/buttons/compassbutt.png", name = "Alternative Compass")
public class AlternativeCompass extends SimpleSubPanel implements MainVehicleChangeListener, NeptusMessageListener {
    private static final long serialVersionUID = 1L;
    @NeptusProperty
    float factor = 1.0f;
    private Image image = ImageUtils.getImage("images/grad_compass.png");
    private double yaw_degs = 0;

    public AlternativeCompass(ConsoleLayout console) {
        super(console);
        initialize();

        getMainpanel().getConsole().addMainVehicleListener(this);
    }

    public float getFactor() {
        return factor;
    }

    public void setFactor(float factor) {
        this.factor = factor;
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;

        double northLocation = ((double) getWidth() / (double) 2) - yaw_degs;

        int curLoc = (int) northLocation;

        while (curLoc < getWidth()) {
            g.drawImage(image, curLoc, 1, getBackground(), this);
            curLoc += 360;
        }

        curLoc = (int) northLocation;
        while (curLoc > 0) {
            g.drawImage(image, curLoc - 360, 1, getBackground(), this);
            curLoc -= 360;
        }

        Polygon p = new Polygon(new int[] { 0, 4, -4 }, new int[] { 10, 25, 25 }, 3);

        g.setColor(new Color(255, 0, 0, 100));
        g.translate(getWidth() / 2, 0);
        g.fillPolygon(p);
        g.setColor(Color.black);
        g.drawPolygon(p);
        Font oldFont = g2d.getFont();

        String txt = "" + ((int) yaw_degs) + "\u00B0";
        Rectangle2D r = g2d.getFontMetrics().getStringBounds(txt, g2d);
        g2d.setColor(Color.gray);
        g2d.drawString(txt, (int) (-r.getWidth() / 2) + 1, 36 + 1);
        g2d.setColor(Color.black);
        g2d.drawString(txt, (int) (-r.getWidth() / 2), 36);
        g2d.setFont(oldFont);

    }

    private void initialize() {
        this.setBounds(new Rectangle(0, 0, 50, 30));
        this.setPreferredSize(new Dimension(50, 30));
        this.setSize(getPreferredSize());
        this.setDoubleBuffered(true);
    }

    public double getYawDegs() {
        return yaw_degs;
    }

    public void setYawDegs(double yaw_degs) {

        double last_degs = this.yaw_degs;
        this.yaw_degs = (int) (yaw_degs % 360.0);

        if (!(last_degs == this.yaw_degs))
            repaint();
    }

    public double getYawRads() {
        return Math.toRadians(yaw_degs);
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        setYawRads(message.getDouble("psi"));
    }

    public void setYawRads(double yaw_degs) {
        setYawDegs(Math.toDegrees(yaw_degs));
    }

    @Override
    public void cleanSubPanel() {

    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
