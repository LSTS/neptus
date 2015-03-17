/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import pt.lsts.neptus.colormap.ColorMap;

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

