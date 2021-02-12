/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ImageScaleAndLocationPanel;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * Use {@link ImageScaleAndLocationPanel} instead.
 */
@Deprecated
public class ImageLocatorPanel extends JPanel implements MouseListener {

    private static final long serialVersionUID = -8382828403279503811L;

    LocationType location1 = new LocationType(), location2 = new LocationType();
    Point2D point1, point2;
    Image image;
    int curLocation = 1;
    double scale = 1.0;
    LocationType center = new LocationType();
    private JButton okBtn, cancelBtn;
    boolean isCancel = true;

    public ImageLocatorPanel(Image image) {
        this.image = image;
        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        addMouseListener(this);
    }

    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double zoom = Math.min((double) getWidth() / (double) image.getWidth(null), (double) getHeight()
                / (double) image.getHeight(null));
        g2d.translate(getWidth() / 2, getHeight() / 2);
        g2d.scale(zoom, zoom);
        g2d.translate(-image.getWidth(null) / 2, -image.getHeight(null) / 2);
        g2d.drawImage(image, 0, 0, null);

        if (point1 != null) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fill(new Ellipse2D.Double(point1.getX() - 7, point1.getY() - 7, 14, 14));

            g2d.setColor(Color.RED);
            g2d.draw(new Ellipse2D.Double(point1.getX() - 7, point1.getY() - 7, 14, 14));
            g2d.drawString("1", (int) point1.getX() - 4, (int) point1.getY() + 4);
        }

        if (point2 != null) {
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fill(new Ellipse2D.Double(point2.getX() - 7, point2.getY() - 7, 14, 14));

            g2d.setColor(Color.RED);
            g2d.draw(new Ellipse2D.Double(point2.getX() - 7, point2.getY() - 7, 14, 14));
            g2d.drawString("2", (int) point2.getX() - 4, (int) point2.getY() + 4);
        }
    }

    public void mouseClicked(MouseEvent e) {
        final Point2D position = e.getPoint();
        JPopupMenu menu = new JPopupMenu();
        AbstractAction setLoc1 = new AbstractAction(I18n.text("Set Location 1")) {
            private static final long serialVersionUID = -8936197276937360980L;

            public void actionPerformed(java.awt.event.ActionEvent e) {
                LocationType lt = LocationPanel.showLocationDialog(I18n.text("Set Location 1"), location1, null);
                if (lt != null) {
                    location1 = lt;
                    point1 = position;
                    if (point2 != null)
                        okBtn.setEnabled(true);
                    repaint();
                }
            };
        };
        menu.add(setLoc1);

        AbstractAction setLoc2 = new AbstractAction(I18n.text("Set Location 2")) {
            private static final long serialVersionUID = 395764026001138275L;

            public void actionPerformed(java.awt.event.ActionEvent e) {
                LocationType lt = LocationPanel.showLocationDialog(I18n.text("Set Location 2"), location2, null);
                if (lt != null) {
                    location2 = lt;
                    point2 = position;
                    if (point1 != null)
                        okBtn.setEnabled(true);
                    repaint();
                }
            };
        };
        menu.add(setLoc2);

        menu.show(this, e.getX(), e.getY());
    }

    public boolean performCalculations() {
        double screenDiff = Math.abs(point2.getX() - point1.getX());
        double locationsDiff = Math.abs(MapTileUtil.getOffsetInPixels(location1, location2)[1]);
        if (locationsDiff == 0)
            return false;

        this.scale = locationsDiff / screenDiff;

        Point2D.Double meanpoint = new Point2D.Double((double) getWidth() / 2, (double) getHeight() / 2);
        double xDiff = Math.abs(point1.getX() - meanpoint.getX());
        double yDiff = Math.abs(point1.getY() - meanpoint.getY());

        if (xDiff == 0)
            return false;

        this.center.setLocation(location1);
        center.translatePosition(xDiff * scale, yDiff * scale, 0);
        return true;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public static void main(String args[]) {
        ImageLocatorPanel ilp = new ImageLocatorPanel(ImageUtils.getImage("images/lsts.png"));
        if (ilp.showDialog()) {
            NeptusLog.pub().info("<###>Scale: " + ilp.getScale());
            NeptusLog.pub().info("<###>Center: " + ilp.getCenter().getDebugString());
        }
    }

    public boolean showDialog() {
        final JDialog positionDialog = new JDialog(new JFrame(), I18n.text("Set 2 locations"), true);
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        okBtn = new JButton(I18n.text("OK"));
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (performCalculations()) {
                    positionDialog.setVisible(false);
                    positionDialog.dispose();
                    isCancel = false;
                }
                else {
                    okBtn.setEnabled(false);
                    GuiUtils.errorMessage(positionDialog, I18n.text("Error in the locations"),
                            I18n.text("The entered locations are not valid"));
                }
            };
        });
        okBtn.setEnabled(false);

        cancelBtn = new JButton(I18n.text("Cancel"));
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                positionDialog.setVisible(false);
                positionDialog.dispose();
                isCancel = true;
            };
        });

        controlPanel.add(okBtn);
        controlPanel.add(cancelBtn);

        mainPanel.add(this, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        positionDialog.setContentPane(mainPanel);
        positionDialog.setSize(400, 500);
        GuiUtils.centerOnScreen(positionDialog);
        positionDialog.setVisible(true);

        return !isCancel;
    }

    public LocationType getCenter() {
        return center;
    }

    public void setCenter(LocationType center) {
        this.center = center;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

}
