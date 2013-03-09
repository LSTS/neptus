/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 3 de Out de 2012
 */
package pt.up.fe.dceg.neptus.loader.helper;

/**
 * @author pdias
 *
 */
public class CheckJavaOSArch {
    /**
     * @param args
     */
    public static void main(String[] args) {
        String osArch = System.getProperty("os.arch");
        String arch = "x86";
        if (osArch.contains("64"))
            arch = "x64";
        System.out.println(arch); 
    }
}
