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
 * $Id:: AnglePanel.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.up.fe.dceg.neptus.gui.SelectAllFocusListener;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zepinto
 * @author pdias (adjust to suport neg values in 2008/10/11)
 *
 */
@SuppressWarnings("serial")
public class AnglePanel extends JPanel {

	private static double choosenAngle = 0;
	
	private double angleRads = 0;
	private TrigoCircle trigo = new TrigoCircle();
	private JFormattedTextField radsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(3));
	private JFormattedTextField degsField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(3));

	private boolean negValuesAllowed = false;
	
	public static double angleDialogRads(Component parentComponent, double angleRads) {
		return angleDialogRads(parentComponent, angleRads, false);
	}

	public static double angleDialogRads(Component parentComponent, double angleRads, boolean negValues) {
		choosenAngle = angleRads;
		final AnglePanel aPanel = new AnglePanel();
		if (negValues)
			aPanel.negValuesAllowed(true);
		else
			aPanel.negValuesAllowed(false);
		aPanel.setAngleRads(angleRads);
		final JDialog dialog;
		if (parentComponent != null)
			dialog = new JDialog(SwingUtilities.getWindowAncestor(parentComponent));
		else
			dialog = new JDialog();
		
		dialog.setLayout(new BorderLayout());
		dialog.add(aPanel, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JCheckBox negAngles = new JCheckBox("+/- values");
		negAngles.setSelected(false);
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		
		okButton.setPreferredSize(new Dimension(60, 25));
		cancelButton.setPreferredSize(new Dimension(60, 25));
		
		buttonsPanel.add(negAngles);
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				choosenAngle = aPanel.getAngleRads();
				dialog.dispose();
			}
		});
		
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				dialog.dispose();
			}
		});
		
		negAngles.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				//System.out.println(e.getStateChange());
				if (e.getStateChange() == ItemEvent.SELECTED) {
					aPanel.negValuesAllowed(true);
				}
				else {
					aPanel.negValuesAllowed(false);
				}
			}
		});
		
		dialog.add(buttonsPanel, BorderLayout.SOUTH);
		
		dialog.setSize(250, 200);
		dialog.setResizable(false);
		dialog.setTitle("Angle chooser");
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.setModal(true);
		GuiUtils.centerOnScreen(dialog);
		dialog.setVisible(true);
		
		return choosenAngle;
	}
	

	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		double a = AnglePanel.angleDialogRads(null, -0.3);
		System.out.print(a);
	}
	
	public AnglePanel() {
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		setLayout(new TableLayout(new double[][] {{0.2, 0.3, 0.2, 0.3},{20, TableLayout.FILL}}));
		
		add(trigo, "0,1,3,1");
		trigo.setParent(this);
		
		radsField.addFocusListener(new SelectAllFocusListener());
		degsField.addFocusListener(new SelectAllFocusListener());
		
		add(new JLabel("Rads: ", JLabel.RIGHT), "0,0");
		add(radsField, "1,0");
		add(new JLabel("Degs: ", JLabel.RIGHT), "2,0");
		add(degsField, "3,0");
	
		
		radsField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {	
				try {
					double val = Double.parseDouble(radsField.getText());
					
					while (val > Math.PI*2) {
						val -= Math.PI*2;
					}
					
					while (val < 0) {
						val += Math.PI*2;
					}
					
					if (negValuesAllowed) {
						if (val > Math.PI)
							val = val - 2*Math.PI;
					}

					degsField.setValue(Math.toDegrees(val));
					trigo.setAngleRads(val);
					
					angleRads = val;
				}
				catch (Exception ex) {					
					degsField.setValue(0);
					trigo.setAngleRads(0);					
					angleRads = 0;
				}
			}	
		});
		
		degsField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {	
				try {
					double val = Double.parseDouble(degsField.getText());
					
					while (val > 360) {
						val -= 360;
					}
					
					while (val < 0) {
						val += 360;
					}
					
					if (negValuesAllowed) {
						if (val > 180)
							val = val - 360;
					}

					radsField.setValue(Math.toRadians(val));
					trigo.setAngleRads(Math.toRadians(val));
					
					angleRads = Math.toRadians(val);
				}
				catch (Exception ex) {					
					degsField.setValue(0);
					trigo.setAngleRads(0);					
					angleRads = 0;
				}
			}	
		});
	}

	public double getAngleRads() {
		return angleRads;
	}

	public void setAngleRads(double angleRads) {
		while (angleRads > 2*Math.PI) {
			angleRads -= 2*Math.PI;
		}
		while (angleRads < 0) {
			angleRads += 2*Math.PI;
		}
		if (negValuesAllowed) {
			if (angleRads > Math.PI)
				angleRads = angleRads - 2*Math.PI;
		}
		if (angleRads == -Math.PI)
			angleRads = -angleRads;
		if (angleRads == 0)
			angleRads = 0;

		this.angleRads = angleRads;
		degsField.setValue(new Double(Math.toDegrees(angleRads)));
		radsField.setValue(angleRads);
		trigo.setAngleRads(angleRads);
	}
	
	public void negValuesAllowed(boolean negAllowed) {
		negValuesAllowed  = negAllowed;
//		double val = angleRads;
//		while (val > 2*Math.PI) {
//			val -= 2*Math.PI;
//		}
//
//		while (val < 0) {
//			val += 2*Math.PI;
//		}
//
//		if (negAllowed) {
//			if (val > Math.PI)
//				val = val - 2*Math.PI;
//		}
//		setAngleRads(val);
		setAngleRads(getAngleRads());
	}

	class TrigoCircle extends JPanel {
		private double angRads = 0;
		private double margin = 8;
		
		private AnglePanel parent = null;
		
		
		public TrigoCircle() {
			
			MouseAdapter myAdapter = new MouseAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					super.mouseDragged(e);
					calcAngle(e.getPoint());
				}
				
				@Override
				public void mousePressed(MouseEvent e) {				
					super.mousePressed(e);
					calcAngle(e.getPoint());
				}
				
				@Override
				public void mouseReleased(MouseEvent e) {				
					super.mouseReleased(e);
					calcAngle(e.getPoint());
				}
			};
			
			addMouseMotionListener(myAdapter);
			addMouseListener(myAdapter);
		}
		
		public void paint(Graphics g) {			
			super.paint(g);
			
			double diameter = (getWidth() < getHeight())? getWidth() : getHeight();
			
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			double hMargin = (getWidth()-diameter)/2+margin;
			double vMargin = (getHeight()-diameter)/2+margin;
			
			diameter = diameter - margin*2;
			Ellipse2D ellis = new Ellipse2D.Double(hMargin, vMargin, diameter, diameter);
			
			Arc2D arc = new Arc2D.Double();
			arc.setFrame(new Rectangle2D.Double(hMargin, vMargin, diameter, diameter));
			arc.setAngleStart(0);
			arc.setAngleExtent(Math.toDegrees(angRads));
			arc.setArcType(Arc2D.PIE);
			
			
			g2d.setColor(Color.white);
			g2d.fill(ellis);
			
			if (angRads >= 0)
				g2d.setColor(new Color(255,100,100));
			else
				g2d.setColor(new Color(100,100,255));
			
			
			
			if (angRads == 0)			
				g2d.draw(new Line2D.Double(getWidth()/2, getHeight()/2, getWidth()/2+diameter/2, getHeight()/2));
			else
				g2d.fill(arc);
			
			g2d.setColor(Color.black);
			g2d.draw(ellis);
			int iang = (int)Math.round(Math.toDegrees(getAngleRads()));
			String angStr = iang + "\u00B0";
			Rectangle2D r = g2d.getFontMetrics().getStringBounds("360", g2d);
			
			g2d.translate(getWidth()/2, getHeight()/2);			
			double scale = (diameter/3) / r.getWidth();
			g2d.scale(scale, scale);
			g2d.drawString(angStr, (float)-r.getWidth()/2, (float)r.getHeight()/3);
		}
		
		private void calcAngle(Point2D click) {
			Point2D pt = new Point2D.Double(getWidth()/2, getHeight()/2);
			double angRads = Math.atan2(click.getY() - pt.getY(), click.getX() - pt.getX());
			angRads = -angRads;
			if (angRads == -Math.PI)
				angRads = -angRads;
			if (angRads == 0)
				angRads = 0;
			if (!negValuesAllowed)
				if (angRads < 0)
					angRads = Math.PI*2+angRads;
			setAngleRads(angRads);
			if (parent != null)
				parent.setAngleRads(getAngleRads());
		}
		
		public double getAngleRads() {
			return angRads;
		}

		public void setAngleRads(double angleRads) {
			this.angRads = angleRads;
			repaint();
		}

		public void setParent(AnglePanel parent) {
			this.parent = parent;
		}		
	}
}
