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
package pt.up.fe.dceg.neptus.colormap;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import pt.up.fe.dceg.neptus.util.GuiUtils;

public class ColorBar extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int HORIZONTAL_ORIENTATION = 0, VERTICAL_ORIENTATION = 1;
	int orientation = HORIZONTAL_ORIENTATION;
	ColorMap cmap = ColorMapFactory.createGrayScaleColorMap();
	BufferedImage cachedImage = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
	//boolean cached = false;
	
	public ColorBar() {
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	}
	
	public ColorBar(int orientation) {
		this();
		this.orientation = orientation;
	}
	
	public ColorBar(int orientation, ColorMap cmap) {
		this();
		this.orientation = orientation;
		setCmap(cmap);
	}
	
	public void paint(Graphics g) {
		
		if (getWidth() != cachedImage.getWidth() || getHeight() != cachedImage.getHeight()) {
			cachedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			Graphics2D g2d = (Graphics2D) cachedImage.getGraphics(); 
			
			if (orientation == HORIZONTAL_ORIENTATION) {
				for (int i = 0; i < cachedImage.getWidth(); i++) {
					double pos = (double)i/(double)cachedImage.getWidth();
					g2d.setColor(cmap.getColor(pos));
					g2d.drawLine(i, 0, i, cachedImage.getHeight());
				}
			}
			
			if (orientation == VERTICAL_ORIENTATION) {
				for (int i = 0; i < cachedImage.getHeight(); i++) {
					//System.out.println(colors.getGraphics().getColor());
					g2d.setColor(cmap.getColor((double)i/(double)cachedImage.getHeight()));
					g2d.drawLine(0, cachedImage.getHeight()-i, cachedImage.getWidth(), cachedImage.getHeight()-i);
				}
			}
		}
		g.drawImage(cachedImage, 0, 0, null);
	}

	public ColorMap getCmap() {
		return cmap;
	}

	public void setCmap(ColorMap cmap) {
		this.cmap = cmap;
	}
	
	public static void main(String args[]) {
		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createAllColorsColorMap()), "Spring", 400, 75);
		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createHotColorMap()), "Hot", 400, 75);
		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createJetColorMap()), "Jet", 400, 75);
		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createBoneColorMap()), "Bone", 400, 75);
		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createGrayScaleColorMap()), "Gray", 400, 75);
	}
}
