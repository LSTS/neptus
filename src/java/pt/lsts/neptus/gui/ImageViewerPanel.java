/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author ZP
 */
public class ImageViewerPanel extends JPanel implements Scrollable {

	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
	public Dimension getPreferredScrollableViewportSize() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public boolean getScrollableTracksViewportHeight() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean getScrollableTracksViewportWidth() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	JLabel imageLabel = null;
	JScrollPane scrollPane = null;
	Image bigImage;
	double curZoom = 1.0;
	
	public ImageViewerPanel(Image bigImage) {
		this.bigImage = bigImage;
		imageLabel = new JLabel(new ImageIcon(bigImage));
		setLayout(new BorderLayout(2,2));
		scrollPane = new JScrollPane(imageLabel);
		add(scrollPane, BorderLayout.CENTER);
		imageLabel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				double lastZoom = curZoom;
				if (e.getWheelRotation() > 0) {
					curZoom *= 1.1;
				}
				else {
					curZoom *= 0.9;
				}
				
				curZoom = Math.min(2, curZoom);
				curZoom = Math.max(0.01, curZoom);
				
				if (curZoom == lastZoom)
					return;
				
				imageLabel.setIcon(new ImageIcon(ImageViewerPanel.this.bigImage.getScaledInstance((int)(ImageViewerPanel.this.bigImage.getWidth(ImageViewerPanel.this)*curZoom), 
						(int)(ImageViewerPanel.this.bigImage.getHeight(ImageViewerPanel.this)*curZoom),
						Image.SCALE_FAST)));				
			}
		});
	}
	
	public static void main(String[] args) {
		GuiUtils.testFrame(new ImageViewerPanel(ImageUtils.getImage("images/novideo.png")));
	}
}
