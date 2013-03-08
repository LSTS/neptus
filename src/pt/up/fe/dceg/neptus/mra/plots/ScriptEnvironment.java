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
 * Nov 29, 2012
 * $Id:: ScriptEnvironment.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author zp
 *
 */
public class ScriptEnvironment extends ScriptableObject {

    private static final long serialVersionUID = 1L;

    @Override
    public String getClassName() {
        return "env";
    }
    
    @Override
    public Object get(String name, Scriptable start) {
        if (!super.has(name, start))
            super.put(name, start, 0.0);
        
        return super.get(name, start);
    }
}
