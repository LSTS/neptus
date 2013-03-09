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
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
