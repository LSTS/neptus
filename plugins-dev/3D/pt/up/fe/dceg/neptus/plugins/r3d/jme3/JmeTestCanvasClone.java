/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Feb 6, 2013
 * $Id:: JmeTestCanvasClone.java 9914 2013-02-12 03:32:18Z robot                $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import com.jme3.app.SimpleApplication;

/**
 * @author Margarida Faria
 *
 */
public class JmeTestCanvasClone extends SimpleApplication {



    @Override
    public void simpleInitApp() {
        DummyState sate = new DummyState();
        stateManager.attach(sate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.jme3.app.SimpleApplication#update()
     */
    @Override
    public void update() {
        super.update();

    }
}
