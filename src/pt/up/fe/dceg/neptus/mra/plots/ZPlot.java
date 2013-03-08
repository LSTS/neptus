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
 * $Id:: ZPlot.java 9847 2013-02-04 14:10:47Z jqcorreia                         $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 * 
 */
public class ZPlot extends MraTimeSeriesPlot {

    public ZPlot(MRAPanel panel) {
        super(panel);
    }
    
    @Override
    public String getTitle() {
        return I18n.text("Z Plot");
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {

        if (source.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
            for (IMCMessage es : source.getIterator("EstimatedState", 0, (long)(timestep * 1000))) {
                double depth = es.getDouble("depth");
                double alt = es.getDouble("alt");
                
                if (depth != -1)
                    addValue(es.getTimestampMillis(), es.getSourceName()+".Depth", depth);
                
                if (alt != -1) {
                    addValue(es.getTimestampMillis(), es.getSourceName()+".Altitude", alt);
                }
                if(depth != -1 && alt != -1) {
                    addValue(es.getTimestampMillis(), es.getSourceName()+".Bathymetry", Math.max(0, depth) + Math.max(0,alt));
                }
            }    
        }
        else {
            for (IMCMessage es : source.getIterator("depth")) {
                addValue(es.getTimestampMillis(), es.getSourceName()+".Depth", es.getDouble("value"));
            }
            for (int i = source.getFirstMessageOfType("BottomDistance"); i != -1; i = source.getNextMessageOfType("BottomDistance", i)) {
                IMCMessage m = source.getMessage(i);
                String entity = source.getEntityName(m.getSrc(), m.getSrcEnt());
                if (entity.equals("DVL")) {
                    addValue(m.getTimestampMillis(), m.getSourceName()+".Altitude", m.getDouble("value"));
                }
            }
        }
    }
    
    @Override
    public String getVerticalAxisName() {
        return I18n.text("meters");
    }
    
    @Override
    public String getName() {
        return I18n.text("Z");
    }
}
