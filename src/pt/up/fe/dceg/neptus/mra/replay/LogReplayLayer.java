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
 * Dec 12, 2011
 */
package pt.up.fe.dceg.neptus.mra.replay;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;

/**
 * @author zp
 *
 */
public interface LogReplayLayer extends Renderer2DPainter {
    
    public boolean canBeApplied(IMraLogGroup source);
    public String getName();
    public void parse(IMraLogGroup source);
    public String[] getObservedMessages();
    public void onMessage(IMCMessage message);
    public boolean getVisibleByDefault();
    public void cleanup();
}
