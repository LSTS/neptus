/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.beans.PropertyEditor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.ColorMapPropertyEditor;
import pt.lsts.neptus.plugins.PropertyType;

public class InterpolationColorMap implements ColorMap, PropertyType {

    protected double[] values = new double[] {0f, 1f};
    protected Color[] colors = new Color[] {Color.BLACK, Color.WHITE};
    protected boolean debug = false;
    protected String name;
    
    @Override
    public void fromString(String value) {
        ColorMap cmap = ColorMapFactory.getColorMapByName(value);
        if (cmap instanceof InterpolationColorMap) {
            InterpolationColorMap icm = ((InterpolationColorMap)cmap);
            this.colors = icm.colors;
            this.values = icm.values;
            this.name = icm.name;			
        }
    }

    @Override
    public Class<? extends PropertyEditor> getPropertyEditor() {
        return ColorMapPropertyEditor.class;
    }


    public InterpolationColorMap(double[] values, Color[] colors) {
        this("Unknown", values, colors);
    }

    public InterpolationColorMap(String name, double[] values, Color[] colors) {
        this.name = name;
        if (values.length != colors.length) {
            System.err.println("The values[] and colors[] sizes don't match!");
            return;
        }
        this.values = values;
        this.colors = colors;
        
    }
    @Override
    public String toString() {
        return name;
    }

    public InterpolationColorMap(Reader reader) throws IOException {
        this(reader, false);
    }       

    public InterpolationColorMap(String name, Reader reader) throws IOException {
        this(reader, false);
        this.name = name;
    }       

    public InterpolationColorMap(Reader reader, boolean is255) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        String line;
        Vector<Color> colorsV = new Vector<Color>();

        while ((line = br.readLine()) != null) {
            if (line.trim().charAt(0) == '#')
                continue;

            String[] parts = line.trim().split("[ \t,]+");

            if (parts.length < 3)
                continue;
            try {
                int r = (int)(Double.parseDouble(parts[parts.length - 3]) * (is255 ? 1 : 255));
                int g = (int)(Double.parseDouble(parts[parts.length - 2]) * (is255 ? 1 : 255));
                int b = (int)(Double.parseDouble(parts[parts.length - 1]) * (is255 ? 1 : 255));
                
                colorsV.add(new Color(r,g,b));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        this.colors = colorsV.toArray(new Color[0]);        
        this.values = new double[colorsV.size()];
        for (int i = 0; i < values.length; i++)     
            values[i] = (double)i/(double)(values.length-1);        
    }	    

    public InterpolationColorMap(File file) throws FileNotFoundException, IOException {
        this (new FileReader(file));

    }

    public InterpolationColorMap(String filename) throws FileNotFoundException, IOException {
        this(new File(filename));

    }

    public static void main(String args[]) throws Exception {
        ColorMap cmap = new InterpolationColorMap("c:/cmap.txt");
        NeptusLog.pub().info("<###> "+cmap.getColor(0.1f));
    }

    public Color getColor(double value) {	
        if (debug)
            NeptusLog.pub().info("<###>getColor()");
        
        if (value >= values[values.length-1])
            return colors[values.length-1];
        
        if (value <= values[0])
            return colors[0];
        
        value = Math.min(value, values[values.length-1]);
        value = Math.max(value, values[0]);

        int pos = 0;
        while (pos < values.length && value > values[pos])
            pos++;

        
        if (pos == 0) 
            return colors[0];
        else if (pos == values.length)
            return colors[colors.length-1];
        else {
            return interpolate(values[pos-1], colors[pos-1], value, values[pos], colors[pos]);
        }
    }

    private Color interpolate(double belowValue, Color belowColor, double value, double aboveValue, Color aboveColor) {

        if (debug)
            NeptusLog.pub().info("<###>interpolate()");

        double totalDist = aboveValue - belowValue;

        double aboveDist = (value - belowValue) / totalDist;
        double belowDist = (aboveValue - value) / totalDist;

        if (debug)
            NeptusLog.pub().info("<###>aboveDist="+aboveDist+", belowDist="+belowDist);

        return new Color(
                (int) (belowColor.getRed() * belowDist + aboveColor.getRed() * aboveDist),
                (int) (belowColor.getGreen() * belowDist + aboveColor.getGreen() * aboveDist),
                (int) (belowColor.getBlue() * belowDist + aboveColor.getBlue() * aboveDist)
                );
    } 

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the values
     */
    public double[] getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(double[] values) {
        this.values = values;
    }

    /**
     * @return the colors
     */
    public Color[] getColors() {
        return colors;
    }

    /**
     * @param colors the colors to set
     */
    public void setColors(Color[] colors) {
        this.colors = colors;
    }
}
