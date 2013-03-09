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
import java.io.IOException;
import java.io.Reader;

public class TabulatedColorMap extends InterpolationColorMap {

    public TabulatedColorMap(Reader reader) throws IOException {
        super(reader);
    }	    

   public Color getColor(double value) {	
        int val = (int)(value*values.length);
        if (val >= values.length)
            val = values.length-1;
        return colors[val];
    }
}
