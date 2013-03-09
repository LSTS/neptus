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
 * 5/Mar/2005
 */
package pt.up.fe.dceg.neptus.test;

import java.io.IOException;

/**
 * @author Paulo
 *
 */
public class TestRun
{

    /**
     * @throws IOException
     * @throws InterruptedException
     * 
     */
    public TestRun() throws IOException, InterruptedException
    {
        super();
    }

    public static void main(String[] args) throws IOException, InterruptedException
    {
        System.out.println(System.getProperty("os.name"));
        Runtime rt = Runtime.getRuntime();
        Process ps = rt.exec("notepad");
        ps.waitFor();
        System.out.println(ps.exitValue());
    }
}
