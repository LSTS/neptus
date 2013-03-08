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
 * $Id:: CtdPlot.java 9615 2012-12-30 23:08:28Z pdias                           $:
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
public class CtdPlot extends MraCombinedPlot {

    public CtdPlot(MRAPanel panel) {
        super(panel);
    }
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("Conductivity");
    }
    
    @Override
    public String getName() {
        return I18n.text("CTD");
    }
    
    @Override
    public void process(LsfIndex source) {
        int rightEntity = source.getMessage(source.getFirstMessageOfType(("Conductivity"))).getSrcEnt();
        
        for (IMCMessage c : source.getIterator("Conductivity"))
            addValue(c.getTimestampMillis(), "Conductivity."+c.getSourceName(), c.getDouble("value"));
            
        for (IMCMessage c : source.getIterator("Temperature"))
            if (c.getSrcEnt() != rightEntity)
                continue;
            else                
                addValue(c.getTimestampMillis(), "Temperature."+c.getSourceName(), c.getDouble("value"));
        
        for (IMCMessage c : source.getIterator("Pressure"))
            if (c.getSrcEnt() != rightEntity)
                continue;
            else                
                addValue(c.getTimestampMillis(), "Pressure."+c.getSourceName(), c.getDouble("value"));
        
    }

}
