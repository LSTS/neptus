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
 * 1/Mar/2005
 */
package pt.up.fe.dceg.neptus.test;

import java.io.File;

import pt.up.fe.dceg.neptus.util.ZipUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class TestWorkspace
{

    public void run ()
    {
        ConfigFetch.initialize("config.xml");
        if (null == ConfigFetch.resolvePath(ConfigFetch.getConfigFile()))
        {
            String fxSep = System.getProperty("file.separator", "/");
            String workspacePath = System.getProperty("user.home", ".") + fxSep
                    + ".neptus";
            File wsDir = new File(workspacePath).getAbsoluteFile();
            if (!wsDir.exists())
            {
                String fxWsPath = ConfigFetch.resolvePath("dist/workspace.jar");
                System.out.println(fxWsPath);
                wsDir.mkdirs();
                ZipUtils.unZip(fxWsPath, wsDir.getAbsolutePath());
                //cf = ConfigFetch.initialize();
                ConfigFetch.INSTANCE.load();
            }
        }
    }
    
    
    public static void main(String[] args)
    {
        new TestWorkspace().run();
    }
}
