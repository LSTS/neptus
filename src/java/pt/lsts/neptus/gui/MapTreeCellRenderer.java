/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * May 25, 2005
 */
package pt.lsts.neptus.gui;

import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import pt.lsts.neptus.types.map.CylinderElement;
import pt.lsts.neptus.types.map.EllipsoidElement;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.Model3DElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.util.ImageUtils;


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
