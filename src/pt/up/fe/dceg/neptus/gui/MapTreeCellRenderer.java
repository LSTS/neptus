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
 * May 25, 2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import pt.up.fe.dceg.neptus.types.map.CylinderElement;
import pt.up.fe.dceg.neptus.types.map.EllipsoidElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.ImageElement;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.Model3DElement;
import pt.up.fe.dceg.neptus.types.map.ParallelepipedElement;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.util.ImageUtils;


/**
 * @author zp
 */
public class MapTreeCellRenderer extends DefaultTreeCellRenderer {
	
	final Image ppiped = ImageUtils.getImage("images/buttons/new_rectangle.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image ellipse = ImageUtils.getImage("images/buttons/new_ellipse.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
    final Image cylinder = ImageUtils.getImage("images/buttons/cylinder.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image path2d = ImageUtils.getImage("images/buttons/new_drawing.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image transponder = ImageUtils.getImage("images/transponder.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image waypoint = ImageUtils.getImage("images/buttons/addpoint.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image image = ImageUtils.getImage("images/buttons/new_image.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image sensor = ImageUtils.getImage("images/buttons/netdevice.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image model3d = ImageUtils.getImage("images/buttons/model3d.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);	
	final Image settings = ImageUtils.getImage("images/buttons/settings.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);
	final Image open = ImageUtils.getImage("images/buttons/open.png").getScaledInstance(16,16,Image.SCALE_DEFAULT);	
	
	private static final long serialVersionUID = 1L;

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		
		super.getTreeCellRendererComponent(
				tree, value, sel,
				expanded, leaf, row,
				hasFocus);
		
		setToolTipText(null); //no tool tip
		
		if (leaf) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			
			if (node.getUserObject() instanceof ParallelepipedElement) {
				setIcon(new ImageIcon(ppiped));
				setToolTipText("Parallelepiped Object");	
			}

			if (node.getUserObject() instanceof EllipsoidElement) {
				setIcon(new ImageIcon(ellipse));
				setToolTipText("Ellipsoid Object");	
			}

            if (node.getUserObject() instanceof CylinderElement) {
                setIcon(new ImageIcon(cylinder));
                setToolTipText("Cylinder Object"); 
            }

			if (node.getUserObject() instanceof PathElement) {
				setIcon(new ImageIcon(path2d));
				setToolTipText("Path2D Object");	
			}

			if (node.getUserObject() instanceof TransponderElement) {
				setIcon(new ImageIcon(transponder));
				setToolTipText("Transponder");
			}
			
			if (node.getUserObject() instanceof MarkElement) {
				setIcon(new ImageIcon(waypoint));
				setToolTipText("Mark");	
			}
			
			if (node.getUserObject() instanceof ImageElement) {
				setIcon(new ImageIcon(image));
				setToolTipText("Image");	
			}
			
//			if (node.getUserObject() instanceof SensorElement) {
//				setIcon(new ImageIcon(sensor));
//				setToolTipText("Sensor Device");	
//			}
				
			if (node.getUserObject() instanceof HomeReferenceElement) {
				setIcon(new ImageIcon(settings));
				setToolTipText("Home Reference");	
			}	
			
			if (node.getUserObject() instanceof Model3DElement) {
				setIcon(new ImageIcon(model3d));
				setToolTipText("3D Model");	
			}	
		}
		else
		{
			setIcon(new ImageIcon(open));
		}
		return this;
	}
}
