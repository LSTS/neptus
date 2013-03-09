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

import java.awt.Color;

public interface ColorMap {
	public Color getColor(double value);
	public String toString();
	
	public static ColorMap[] cmaps = new ColorMap[] {
		ColorMapFactory.createGrayScaleColorMap(),
		ColorMapFactory.createJetColorMap(),
		ColorMapFactory.createCoolColorMap(),
		ColorMapFactory.createHotColorMap(),
		ColorMapFactory.createCopperColorMap(),
		ColorMapFactory.createBoneColorMap(),
		ColorMapFactory.createSpringColorMap(),
		ColorMapFactory.createSummerColorMap(),
		ColorMapFactory.createAutumnColorMap(),
		ColorMapFactory.createWinterColorMap(),
		ColorMapFactory.createRedGreenBlueColorMap(),
		ColorMapFactory.createBlueToRedColorMap(),
		ColorMapFactory.createPinkColorMap(),
		ColorMapFactory.createGreenRadarColorMap(),
		ColorMapFactory.createRedYellowGreenColorMap(),
		ColorMapFactory.createRainbowColormap(),
		ColorMapFactory.createBronzeColormap(),
        ColorMapFactory.createStoreDataColormap()
	};
}
