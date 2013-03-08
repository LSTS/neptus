/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 13, 2012
 * $Id:: GenericPlot.java 9835 2013-02-01 17:24:48Z jqcorreia                   $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.util.Arrays;
import java.util.Vector;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class GenericPlot extends MraTimeSeriesPlot {

    protected String[] fieldsToPlot = null;

    public GenericPlot(String[] fieldsToPlot, MRAPanel panel) {
        super(panel);
        this.fieldsToPlot = fieldsToPlot;
    }
    
    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/graph.png");
    }
    
    @Override
    public String getTitle() {
        return getName();
    }
    
    @Override
    public String getName() {
        return Arrays.toString(fieldsToPlot);
    }    
    
    @Override
    public boolean canBeApplied(LsfIndex index) {        
        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            if (index.getFirstMessageOfType(messageName) == -1)
                return false;
        }
        return true;
    }
    
    public Vector<String> getForbiddenSeries() {
        return forbiddenSeries;
    }

    @Override
    public void process(LsfIndex source) {
        System.out.println("Generic Plot process");
        long t = System.currentTimeMillis();
        for (String field : fieldsToPlot) {
            String messageName = field.split("\\.")[0];
            String variable = field.split("\\.")[1];
            
            for (IMCMessage m : source.getIterator(messageName, 0, (long)(timestep * 1000))) {
                String seriesName = "";

                if(m.getValue("id") != null) {
                    seriesName = m.getSourceName()+"."+source.getEntityName(m.getSrc(), m.getSrcEnt())+"."+field+"."+m.getValue("id");
                } 
                else { 
//                    System.out.println(m.getAbbrev() + " " + source.getEntityName(m.getSrc(), m.getSrcEnt()));
                    seriesName = m.getSourceName()+"."+source.getEntityName(m.getSrc(), m.getSrcEnt())+"."+field;
                }
                
                if (m.getMessageType().getFieldUnits(variable) != null && m.getMessageType().getFieldUnits(variable).startsWith("rad")) {
                    // Special case for angles in radians
                    addValue(m.getTimestampMillis(), seriesName, Math.toDegrees(m.getDouble(variable)));
                }
                else
                    addValue(m.getTimestampMillis(), seriesName, m.getDouble(variable));
            }
        }
        System.out.println("Processed in " + (System.currentTimeMillis() - t));
    }
}
