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
 * $Id:: MapLocationSelector.java 9845 2013-02-01 19:53:46Z pdias         $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.renderer2d.CursorLocationPainter;
import pt.up.fe.dceg.neptus.renderer2d.Renderer;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

public class MapLocationSelector extends JPanel {

    private static final long serialVersionUID = 1L;
    StateRenderer2D r2d = null;
    MapType dummyMap = new MapType();
    public LocationObject lo = null;
    JDialog dialog = null;

    public MapLocationSelector(final MapGroup mg) {

        setLayout(new BorderLayout());
        r2d = new StateRenderer2D(mg);
        r2d.addPostRenderPainter(new CursorLocationPainter(), "Cursor Location Painter");
        add(r2d, BorderLayout.CENTER);	
        r2d.setViewMode(Renderer.NONE);
        r2d.setIgnoreRightClicks(true);
        r2d.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LocationType lt = r2d.getRealWorldLocation(e.getPoint());
                if (lo == null)
                    lo = new LocationObject(mg, dummyMap);
                lo.setCenterLocation(lt);
                dummyMap.addObject(lo);
                mg.removeMap(dummyMap.getId());
                mg.addMap(dummyMap);
                e.consume();
            };
        });

        JPanel okCancelPanel = new JPanel();
        okCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.setPreferredSize(new Dimension(80,25));
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
                if (lo == null) {
                    GuiUtils.errorMessage(dialog, "Did not set a location", "You have to set a location first");
                    return;
                }				
                if (dialog != null)
                    dialog.dispose();				
            }			
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent arg0) {
                lo = null;
                if (dialog != null)
                    dialog.dispose();				
            }			
        });
        cancelButton.setPreferredSize(new Dimension(80,25));

        okCancelPanel.add(okButton);
        okCancelPanel.add(cancelButton);


        add(okCancelPanel, BorderLayout.SOUTH);
    }

    public static LocationType showDialog(Frame parent, MapGroup mg) {
        MapLocationSelector mls = new MapLocationSelector(mg);
        JDialog dialog = new JDialog(parent);
        mls.setDialog(dialog);
        dialog.setContentPane(mls);
        dialog.pack();
        GuiUtils.centerOnScreen(dialog);
        dialog.setModal(true);
        dialog.setVisible(true);

        if (mls.lo == null) {
            return null;
        }
        else {
            return mls.lo.getCenterLocation();
        }		
    }


    /**
     * @param args
     */
    public static void main(String[] args) {
        GuiUtils.testFrame(new MapLocationSelector(MapGroup.getNewInstance(new CoordinateSystem())), "test");
    }

    public void setDialog(JDialog dialog) {
        this.dialog = dialog;
    }

}

class LocationObject extends AbstractElement {

    public LocationObject(MapGroup mg, MapType map) {
        super(mg, map);
    }

    LocationType center = new LocationType();

    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return point.getDistanceInMeters(getCenterLocation()) < renderer.getZoom();
    }

    @Override
    public LocationType getCenterLocation() {
        return center;
    }

    @Override
    public int getLayerPriority() {		
        return 0;
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        return null;
    }
    @Override
    public String getType() {		 
        return "Location";
    }
    @Override
    public void initialize(ParametersPanel paramsPanel) {

    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        g.setTransform(new AffineTransform());
        Point2D pt = renderer.getScreenPosition(center);

        if (pt != null) {
            g.translate(pt.getX(), pt.getY());
            g.setColor(new Color(0,0,0,100));
            g.fill(new Ellipse2D.Double(-10, -10, 20, 20));
            g.setColor(Color.WHITE);
            g.draw(new Line2D.Double(-10, -10, 10, 10));
            g.draw(new Line2D.Double(-10, 10, 10, -10));
        }
    }
    @Override
    public void setCenterLocation(LocationType l) {
        center.setLocation(l);
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_OTHER;
    }


}
