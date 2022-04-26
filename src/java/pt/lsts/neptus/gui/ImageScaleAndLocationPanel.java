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
 * Author: José Pinto
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

public class ImageScaleAndLocationPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JDialog parentDialog = null;
	private MapGroup mg = MapGroup.getNewInstance(new CoordinateSystem());
	private StateRenderer2D r2d = new StateRenderer2D(mg);
	{
	    r2d.setShowWorldMapOnScreenControls(false);
	    r2d.setShowWorldMapOnScreen(false);
	    r2d.setLegendShown(false);
	    r2d.removePaintersOfType(Object.class);
	}
	private MapType map = new MapType();
	private ImageElement tmp, imgObject;
	private LocationType lt1 = new LocationType(), lt2 = new LocationType();
	
	private MarkElement mark1 = null;
	private MarkElement mark2 = null;
	
	private boolean userCancel = true;
	
	private JButton okBtn = null;
	
	@SuppressWarnings("serial")
    public ImageScaleAndLocationPanel(ImageElement obj) {
		
		this.imgObject = obj;
		r2d.setLegendShown(false);
		r2d.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent mevt) {		
				if (mevt.getButton() != MouseEvent.BUTTON3)
					return;
				
				JPopupMenu popup = new JPopupMenu();
				final Point2D clickedPoint = mevt.getPoint();
				
				AbstractAction point1 = new AbstractAction("Set Location 1") {	
					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent a) {		
						
                        LocationType tmp = LocationPanel.showLocationDialog(
                                SwingUtilities.getWindowAncestor(parentDialog), "Set Location 1",
                                lt1, null, true);
						if (tmp != null) {
							lt1 = tmp;
							map.remove("Location 1");							
							mark1 = new MarkElement(null, null);
							mark1.setCenterLocation(r2d.getRealWorldLocation(clickedPoint));	
							mark1.setId("Location 1");
							map.addObject(mark1);
							if (mark2 != null)
							    okBtn.setEnabled(true);
							r2d.repaint();
						}
					}
				};
				
				AbstractAction point2 = new AbstractAction("Set Location 2") {
					private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent arg0) {						
                        LocationType tmp = LocationPanel.showLocationDialog(
                                SwingUtilities.getWindowAncestor(parentDialog), "Set Location 2",
                                lt2, null, true);
						if (tmp != null) {
							lt2 = tmp;
							map.remove("Location 2");							
							mark2 = new MarkElement(null, null);
							mark2.setCenterLocation(r2d.getRealWorldLocation(clickedPoint));	
							mark2.setId("Location 2");
							map.addObject(mark2);
							if (mark2 != null)
                                okBtn.setEnabled(true);
                            r2d.repaint();
						}
					}
				};
				
				popup.add(point1);
				popup.add(point2);
				
				popup.show(r2d, mevt.getX(), mevt.getY());
			}
		});
		mg.addMap(map);
		tmp = new ImageElement(mg, map);
		tmp.setImage(obj.getImage());
		
		map.addObject(tmp);
		
		this.setLayout(new BorderLayout());
		this.add(r2d, BorderLayout.CENTER);
		
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 
		okBtn = new JButton(new AbstractAction("OK") {
			public void actionPerformed(ActionEvent arg0) {
				calculate();
				userCancel = false;
				parentDialog.setVisible(false);
				parentDialog.dispose();
			};			
		});
		okBtn.setPreferredSize(new Dimension(100, 30));
		okBtn.setEnabled(false);
		buttonsPanel.add(okBtn);
		
		JButton cancelBtn =	new JButton(new AbstractAction("Cancel") {
			public void actionPerformed(ActionEvent arg0) {
				parentDialog.setVisible(false);
				parentDialog.dispose();
			};			
		});
		cancelBtn.setPreferredSize(new Dimension(100, 30));
		buttonsPanel.add(cancelBtn);
		
		this.add(buttonsPanel, BorderLayout.SOUTH);
	}
	
	public static boolean showDialog(ImageElement obj, Window parent) {
		ImageScaleAndLocationPanel panel = new ImageScaleAndLocationPanel(obj);
		JDialog dialog = new JDialog(parent);
		panel.setParentDialog(dialog);
		dialog.setContentPane(panel);
		dialog.setTitle("Image "+obj.getId()+" position and scale" + " [image must be north oriented]");
		dialog.setSize(500,500);
		//GuiUtils.centerOnScreen(dialog);
		GuiUtils.centerParent(dialog, parent);
		//dialog.setAlwaysOnTop(true);
		dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		return !panel.userCancel;
	}
	
	private void calculate() {
		double pixelDistance = mark1.getCenterLocation().getHorizontalDistanceInMeters(mark2.getCenterLocation());
		double meterDistance = lt1.getHorizontalDistanceInMeters(lt2);		
		double scale = meterDistance / pixelDistance;
		
		double[] pixelOffsets = (new LocationType()).getOffsetFrom(mark1.getCenterLocation());
		
		LocationType finalLoc = new LocationType(lt1);
		finalLoc.translatePosition(pixelOffsets[0]*scale, pixelOffsets[1]*scale, pixelOffsets[2]*scale);		
		
		imgObject.setCenterLocation(finalLoc);
		imgObject.setImageScale(scale);
	}

	public void setParentDialog(JDialog parentDialog) {
	    this.parentDialog = parentDialog;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ImageElement tmp = new ImageElement();
		tmp.setImage(ImageUtils.getImage("images/neptus-icon1.png"));
		ImageScaleAndLocationPanel.showDialog(tmp, new JFrame());
	}
}
