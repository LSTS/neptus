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
 * 20??/??/??
 */
package pt.lsts.neptus.colormap;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import pt.lsts.neptus.util.GuiUtils;

public class ColorBar extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int HORIZONTAL_ORIENTATION = 0, VERTICAL_ORIENTATION = 1;
	int orientation = HORIZONTAL_ORIENTATION;
	ColorMap cmap = ColorMapFactory.createGrayScaleColorMap();
	BufferedImage cachedImage = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
	boolean colorMapChanged = false;

	
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
		boolean cacheInvalidated = colorMapChanged ||
				getWidth() != cachedImage.getWidth() ||
				getHeight() != cachedImage.getHeight();

		if (cacheInvalidated) {
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
					//NeptusLog.pub().info("<###> "+colors.getGraphics().getColor());
					g2d.setColor(cmap.getColor((double)i/(double)cachedImage.getHeight()));
					g2d.drawLine(0, cachedImage.getHeight()-i, cachedImage.getWidth(), cachedImage.getHeight()-i);
				}
			}
			colorMapChanged = false;
		}
		g.drawImage(cachedImage, 0, 0, null);
	}

	public ColorMap getCmap() {
		return cmap;
	}

	public void setCmap(ColorMap cmap) {
		this.cmap = cmap;
		colorMapChanged = true;
	}
	
	public static void main(String args[]) {
//		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createAllColorsColorMap()), "Spring", 400, 75);
//		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createHotColorMap()), "Hot", 400, 75);
//		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createJetColorMap()), "Jet", 400, 75);
//		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createBoneColorMap()), "Bone", 400, 75);
//		GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.createGrayScaleColorMap()), "Gray", 400, 75);
		
		for (String cmName : ColorMapFactory.colorMapNamesList) {
		    GuiUtils.testFrame(new ColorBar(HORIZONTAL_ORIENTATION, ColorMapFactory.getColorMapByName(cmName)), cmName, 400, 75);
        }
	}
}
