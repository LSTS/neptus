/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.imc.IMCMessage;

@PluginDescription(icon = "images/buttons/compassbutt.png", name = "Alternative Compass")
public class AlternativeCompass extends ConsolePanel implements MainVehicleChangeListener, NeptusMessageListener {
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
