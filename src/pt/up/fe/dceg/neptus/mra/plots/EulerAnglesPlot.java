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
 * $Id:: EulerAnglesPlot.java 9835 2013-02-01 17:24:48Z jqcorreia               $:
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
public class EulerAnglesPlot extends MraCombinedPlot {

    public EulerAnglesPlot(MRAPanel panel) {
        super(panel);
    }
    
    @Override
    public String getName() {
        return I18n.text("Euler Angles");
    }
    
    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("EstimatedState");
    }

    @Override
    public void process(LsfIndex source) {
        if (source.getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
            for (IMCMessage msg : source.getIterator("EstimatedState", 0, (long)(timestep*1000))) {
                addValue(msg.getTimestampMillis(), "Phi (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("phi")));
                addValue(msg.getTimestampMillis(), "Theta (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("theta")));
                addValue(msg.getTimestampMillis(), "Psi (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("psi")));
            }  
        }
        else {
            for (IMCMessage msg : source.getIterator("EulerAngles", 0, (long)(timestep*1000))) {
                addValue(msg.getTimestampMillis(), "Phi (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("roll")));
                addValue(msg.getTimestampMillis(), "Theta (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("pitch")));
                addValue(msg.getTimestampMillis(), "Psi (deg)."+msg.getSourceName(), Math.toDegrees(msg.getDouble("yaw")));
            }
        }
    }
}
