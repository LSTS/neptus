/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/05/19
 * $Id:: ColorUtils.java 9615 2012-12-30 23:08:28Z pdias                        $:
 */

package pt.up.fe.dceg.neptus.util;

import java.awt.Color;

/**
 * @author pdias
 *
 */
public class ColorUtils {
	public static final Color setTransparencyToColor (Color c, int transparency) {
		return new Color (c.getRed(), c.getGreen(), c.getBlue(), transparency);
	}

	public static final Color invertColor (Color c) {
		return invertColor(c, 255);
	}	

	public static final Color invertColor (Color c, int transparency) {
		return new Color (255-c.getRed(), 255-c.getGreen(), 255-c.getBlue(), transparency);
	}	
}
