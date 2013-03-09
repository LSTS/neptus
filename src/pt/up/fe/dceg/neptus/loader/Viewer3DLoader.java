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
package pt.up.fe.dceg.neptus.loader;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.gui.Loader;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.viewer3d.Viewer3D;

/** 
 * @author RJPG
 * @author Paulo Dias
 */

public class Viewer3DLoader {
	
    public void run()
    {
        Loader loader = new Loader();
        loader.start();
        ConfigFetch.initialize();
        run(loader);
    }

    
    /**
     * The main procedure of this class: Launches a new Viewer3D.
     * @param loader
     */
    public void run (Loader loader)
    {
        Viewer3D app = new Viewer3D();
        
        
        //ConfigFetch.setSuperParentFrame(app.getFrame());
        GuiUtils.centerOnScreen(app.getFrame());
        app.getFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
        app.getFrame().addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                System.exit(0);
            }
        });
        app.setVisible(true);
        app.show();
        loader.waitMoreAndEnd(1000);
    }
    
    

    public static void main(String[] args)
    {
        new Viewer3DLoader().run();
    }

    
    /*
    public static void main(final String[] args) {
	
		Viewer3D app=new Viewer3D();
		app.setVisible(true);
		app.show();
	}
    */
}
