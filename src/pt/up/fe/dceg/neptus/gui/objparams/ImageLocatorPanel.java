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
package pt.up.fe.dceg.neptus.gui.objparams;

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

import pt.up.fe.dceg.neptus.gui.ImageScaleAndLocationPanel;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

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
		double zoom = Math.min((double)getWidth()/(double)image.getWidth(null), (double)getHeight()/(double)image.getHeight(null));
		g2d.translate(getWidth()/2, getHeight()/2);
		g2d.scale(zoom, zoom);
		g2d.translate(-image.getWidth(null)/2, -image.getHeight(null)/2);			
		g2d.drawImage(image, 0, 0, null);
		
		
		
		if (point1 != null) {
			g2d.setColor(new Color(255,255,255,100));
			g2d.fill(new Ellipse2D.Double(point1.getX()-7, point1.getY()-7, 14, 14));
			
			g2d.setColor(Color.RED);
			g2d.draw(new Ellipse2D.Double(point1.getX()-7, point1.getY()-7, 14, 14));
			g2d.drawString("1", (int) point1.getX()-4, (int) point1.getY()+4);
		}
		
		if (point2 != null) {
			g2d.setColor(new Color(255,255,255,100));
			g2d.fill(new Ellipse2D.Double(point2.getX()-7, point2.getY()-7, 14, 14));
			
			g2d.setColor(Color.RED);
			g2d.draw(new Ellipse2D.Double(point2.getX()-7, point2.getY()-7, 14, 14));
			g2d.drawString("2", (int) point2.getX()-4, (int) point2.getY()+4);
		}
	}
	
	public void mouseClicked(MouseEvent e) {
		final Point2D position = e.getPoint();
		JPopupMenu menu = new JPopupMenu();
		AbstractAction setLoc1 = new AbstractAction("Set Location 1") {
			private static final long serialVersionUID = -8936197276937360980L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				LocationType lt = LocationPanel.showLocationDialog("Set Location 1", location1, null);
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
		
		AbstractAction setLoc2 = new AbstractAction("Set Location 2") {
			private static final long serialVersionUID = 395764026001138275L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				LocationType lt = LocationPanel.showLocationDialog("Set Location 2", location2, null);
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
		
		Point2D.Double meanpoint = new Point2D.Double((double)getWidth()/2, (double)getHeight()/2);
		double xDiff = Math.abs(point1.getX()-meanpoint.getX());
		double yDiff = Math.abs(point1.getY()-meanpoint.getY());
		
		if (xDiff == 0) 
			return false;
		
		this.center.setLocation(location1);
		center.translatePosition(xDiff*scale, yDiff*scale, 0);
		return true;
	}
	
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) {	}
	
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	
	public static void main(String args[]) {
		ImageLocatorPanel ilp = new ImageLocatorPanel(ImageUtils.getImage("images/lsts.png"));
		if (ilp.showDialog()) {
			System.out.println("Scale: "+ilp.getScale());
			System.out.println("Center: "+ilp.getCenter().getDebugString());
		}
	}
	
	public boolean showDialog() {
		final JDialog positionDialog = new JDialog(new JFrame(), "Set 2 locations", true);
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				if (performCalculations()) {
					positionDialog.setVisible(false);
					positionDialog.dispose();
					isCancel = false;
				}
				else {
					okBtn.setEnabled(false);
					GuiUtils.errorMessage(positionDialog, "Error in the locations", "The entered locations are not valid");
				}
			};
		});
		okBtn.setEnabled(false);
		
		cancelBtn = new JButton("Cancel");
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
