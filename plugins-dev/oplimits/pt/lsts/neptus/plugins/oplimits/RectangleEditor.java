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
 * Sep 24, 2010
 */
package pt.lsts.neptus.plugins.oplimits;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@LayerPriority(priority=60)
public class RectangleEditor extends JPanel implements Renderer2DPainter, StateRendererInteraction {

	private static final long serialVersionUID = 107770334195610476L;
	protected InteractionAdapter adapter = new InteractionAdapter(null);
	protected StateRenderer2D renderer;
	protected PathElement rectangle = null;
	protected ParallelepipedElement pp = null, selection = null;
    protected JDialog parentDialog = null;
    protected LocationType[] points = new LocationType[4];     
    protected double width = Double.NaN, length = Double.NaN, rotationRads = Double.NaN, latDegs = Double.NaN, lonDegs = Double.NaN;
    protected JButton btnOk, btnCancel, btnClear;
	protected int clickCount = 0;
	protected Point2D lastDragPoint = null;
	
	public ParallelepipedElement getSelectedRectangle() {
		return selection;
	}
	
	public RectangleEditor(MissionType mission) {
		renderer = new StateRenderer2D(MapGroup.getMapGroupInstance(mission));
		renderer.setActiveInteraction(this);
		renderer.addPostRenderPainter(this, "Rectangle Editor");
		renderer.setCursor(Cursor.getDefaultCursor());
		setLayout(new BorderLayout());
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
		btnOk = new JButton(I18n.text("OK"));
		btnCancel = new JButton(I18n.text("Cancel"));
		btnClear = new JButton(I18n.text("Clear"));
		btnOk.setPreferredSize(new Dimension(80,22));
		btnCancel.setPreferredSize(new Dimension(80, 22));
		btnClear.setPreferredSize(new Dimension(80, 22));
		btnClear.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				btnOk.setEnabled(false);
				rectangle = null;
				pp = null;
				repaint();
			}
		});
		btnOk.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				selection = pp;
				if (parentDialog != null)
					parentDialog.dispose();
			}
		});
		btnCancel.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				pp = null;
				if (parentDialog != null)
					parentDialog.dispose();
			}
		});
		bottom.add(btnClear);
		bottom.add(btnCancel);
		bottom.add(btnOk);
		btnOk.setEnabled(false);
		add(renderer, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);
	}
	
	public ParallelepipedElement showDialog(Component caller) {
		parentDialog = new JDialog(SwingUtilities.getWindowAncestor(caller));
		parentDialog.add(this);
		parentDialog.setModal(true);
		parentDialog.setSize(600, 400);
		parentDialog.setVisible(true);
		
		return getSelectedRectangle();
	}
	
	
	@Override
	public void paint(Graphics2D g2, StateRenderer2D renderer) {
	    Graphics2D g = (Graphics2D) g2.create();
	    if (clickCount == 1) {
            g.setColor(Color.red.darker());
            Point2D pt = renderer.getScreenPosition(points[0]);
            g.draw(new Line2D.Double(pt.getX()-5, pt.getY()-5, pt.getX()+5, pt.getY()+5));
            g.draw(new Line2D.Double(pt.getX()-5, pt.getY()+5, pt.getX()+5, pt.getY()-5));
            g.dispose();
            return;
        }
        
		//g.translate(renderer.getWidth() / 2, renderer.getHeight() / 2);
	    //g.scale(1,-1);
		//g.rotate(renderer.getRotation());
		if (pp != null) {
			pp.paint((Graphics2D)g.create(), renderer, -renderer.getRotation());
		}
		else if (rectangle != null) {
			rectangle.setMyColor(Color.red);
			rectangle.setFilled(true);
			rectangle.paint((Graphics2D)g.create(), renderer, -renderer.getRotation());
		}
		g.dispose();
	}

	@Override
	public Image getIconImage() {
		return adapter.getIconImage();
	}

	@Override
	public Cursor getMouseCursor() {
		return Cursor.getDefaultCursor();
	}

	@Override
	public boolean isExclusive() {
		return true;
	}

	
	@Override
	public void mouseClicked(MouseEvent event, StateRenderer2D source) {
		adapter.mouseClicked(event, source);
		if (event.getButton() == MouseEvent.BUTTON3) {
			rectangle = null;
			pp = null;
			clickCount = 0;
			repaint();
			return;
		}
	
		if (rectangle == null) {
			points[0] = source.getRealWorldLocation(event.getPoint());
			rectangle = new PathElement(source.getMapGroup(), null, points[0]);
			rectangle.setShape(true);	
			rectangle.setFinished(true);					
			rectangle.setStroke(new BasicStroke(2.0f));
			rectangle.addPoint(0, 0, 0, false);
			clickCount=1;
		}
		else if (clickCount == 1){
			clickCount++;
			points[1] = source.getRealWorldLocation(event.getPoint());			
			double[] offsets = points[1].getOffsetFrom(rectangle.getCenterLocation());
			rectangle.addPoint(offsets[1], offsets[0], 0, false);			
		}
		else if (clickCount == 2) {
			clickCount++;
			LocationType loc = source.getRealWorldLocation(event.getPoint());
			double[] offsets = loc.getOffsetFrom(rectangle.getCenterLocation());
			double[] offsets2 = points[1].getOffsetFrom(points[0]);
			
			double px = offsets[1];
			double py = offsets[0];
			
			double lx1 = 0;
			double ly1 = 0;
			double lx2 = offsets2[1];
			double ly2 = offsets2[0];
			
			double angle = points[0].getXYAngle(points[1])+Math.PI/2;
			double dist = MathMiscUtils.pointLineDistance(px, py, lx1, ly1, lx2, ly2);
			
			points[2] = new LocationType(points[0]);
			points[3] = new LocationType(points[1]);
			points[2].translatePosition(-Math.cos(angle)*dist, -Math.sin(angle)*dist, 0);
			points[3].translatePosition(-Math.cos(angle)*dist, -Math.sin(angle)*dist, 0);
           
			double inc = Math.PI/2;
			
			if ((int)angle != (int)points[2].getXYAngle(loc))
				inc = 3*Math.PI/2;
			
			rectangle.addPoint(lx2+Math.sin(angle+inc)*dist, ly2+Math.cos(angle+inc)*dist, 0, false);
			rectangle.addPoint(Math.sin(angle+inc)*dist, Math.cos(angle+inc)*dist, 0, false);
			
			pp = new ParallelepipedElement(rectangle.getMapGroup(), rectangle.getParentMap());
			pp.setCenterLocation(centroid(points));
			pp.setWidth(points[0].getDistanceInMeters(points[1]));
			pp.setLength(points[0].getDistanceInMeters(points[2]));
			pp.setHeight(0);
			pp.setYaw(Math.toDegrees(points[0].getXYAngle(points[2])));
			pp.setMyColor(Color.red);

			//special case...
			double d = centroid(points).getDistanceInMeters(loc)-centroid(points[0],points[1]).getDistanceInMeters(loc);
			if (d > 0)
			    pp.setCenterLocation(new LocationType(pp.getCenterLocation().translatePosition(points[0].getOffsetFrom(points[2]))));
			
			btnOk.setEnabled(true);
		}
		repaint();
	}

	
	@Override
	public void mousePressed(MouseEvent event, StateRenderer2D source) {
		adapter.mousePressed(event, source);
		lastDragPoint = event.getPoint();
	}

	@Override
	public void mouseDragged(MouseEvent event, StateRenderer2D source) {
		
		if (pp != null && pp.containsPoint(source.getRealWorldLocation(lastDragPoint), source)) {
			double mx = event.getPoint().getX()-lastDragPoint.getX();
			double my = event.getPoint().getY()-lastDragPoint.getY();
			
			if (event.isShiftDown())
				pp.rotateRight(my);
			else			
				pp.getCenterLocation().translatePosition(-my/source.getZoom(), mx/ source.getZoom(), 0);
			
		}
		else {
		    if (!event.isShiftDown())
		        adapter.mouseDragged(event, source);	
		}
		lastDragPoint = event.getPoint();
		repaint();		
	}

	@Override
	public void mouseMoved(MouseEvent event, StateRenderer2D source) {
		adapter.mouseMoved(event, source);
	}

	@Override
	public void mouseReleased(MouseEvent event, StateRenderer2D source) {
		adapter.mouseReleased(event, source);
		lastDragPoint = null;
	}

	@Override
	public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
		adapter.wheelMoved(event, source);
	}

	@Override
	public void keyPressed(KeyEvent event, StateRenderer2D source) {
		adapter.keyPressed(event, source);
	}

	@Override
	public void keyReleased(KeyEvent event, StateRenderer2D source) {
		adapter.keyReleased(event, source);
	}

	@Override
	public void keyTyped(KeyEvent event, StateRenderer2D source) {
		adapter.keyTyped(event, source);
	}
	
	@Override
	public void mouseExited(MouseEvent event, StateRenderer2D source) {
	    adapter.mouseExited(event, source);
	}
    
    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
        adapter.focusGained(event, source);        
    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
        adapter.focusLost(event, source);
    }
	
	public static LocationType centroid(LocationType ... points) {
		double latSum = 0, lonSum = 0, depthSum = 0;
		for (LocationType l : points) {
			double[] lld = l.getAbsoluteLatLonDepth();
			latSum += lld[0];
			lonSum += lld[1];
			depthSum += lld[2];
		}
		LocationType l = new LocationType();
		l.setLatitudeDegs(latSum/points.length);
		l.setLongitudeDegs(lonSum/points.length);
		l.setDepth(depthSum/points.length);
		return l;
	}
	
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        adapter.setActive(mode, source);
    }
	
    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
        
    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
    }

	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		GuiUtils.setLookAndFeel();
		MissionType mission = new MissionType("/home/zp/workspace/Neptus/missions/APDL/missao-apdl.nmisz");
		RectangleEditor editor = new RectangleEditor(mission);
		
		NeptusLog.pub().info("<###> "+editor.showDialog(new JFrame()));
	}
}
