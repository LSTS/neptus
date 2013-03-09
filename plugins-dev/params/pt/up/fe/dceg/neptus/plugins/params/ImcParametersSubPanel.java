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
 * Dec 14, 2012
 */
package pt.up.fe.dceg.neptus.plugins.params;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;

/**
 * @author zp
 */

public class ImcParametersSubPanel extends SimpleSubPanel {
    private static final long serialVersionUID = 1L;
    SystemConfiguration config;
    
    public ImcParametersSubPanel(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void initSubPanel() {
    }
    
    @Override
    public void cleanSubPanel() {
        
    }
}
