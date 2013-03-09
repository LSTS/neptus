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

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.colormap.ColorMap;

public class DataColorizer extends JPanel {

    private static final long serialVersionUID = -1062805551472608993L;

    int numRows = 10, numCols = 10;
	ColorMap colorMap;
	BufferedImage cachedImage = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
	
	public DataColorizer(ColorMap colorMap) { 
		this.colorMap = colorMap;
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		if (cachedImage.getWidth() != getWidth() || cachedImage.getHeight() != getHeight()) {
			rebuildCachedImage();
		}
	}

	public void rebuildCachedImage() {
		
	}
	
	class DataCell {
		int dataCount;
		double value;
	}
	
	
	public ColorMap getColorMap() {
		return colorMap;
	}
	public void setColorMap(ColorMap colorMap) {
		this.colorMap = colorMap;
	}
	public int getNumCols() {
		return numCols;
	}
	public void setNumCols(int numCols) {
		this.numCols = numCols;
	}
	public int getNumRows() {
		return numRows;
	}
	public void setNumRows(int numRows) {
		this.numRows = numRows;
	}
}

