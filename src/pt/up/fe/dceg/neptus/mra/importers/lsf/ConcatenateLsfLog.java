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
 * Jul 17, 2012
 */
package pt.up.fe.dceg.neptus.mra.importers.lsf;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class ConcatenateLsfLog {

    public static File[] chooseFolders(Component parent, String initialDir) {
        
        JFileChooser chooser = new JFileChooser(initialDir);
        
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle(I18n.text("Select folders  to concatenate"));        
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int option = chooser.showOpenDialog(parent);
        
        if (option == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFiles();
        }
        
        return null;
    }
    
    public static void concatenateFolders(File[] folders, File destination, LinkedHashMap<String, Object> options)
            throws Exception {
        destination.mkdirs();
        Arrays.sort(folders);
        
        System.out.println(I18n.textf("Copying %filename", new File(folders[0], "IMC.xml").getAbsolutePath()));
        FileUtil.copyFile(new File(folders[0], "IMC.xml").getAbsolutePath(),
                new File(destination, "IMC.xml").getAbsolutePath());
        
        System.out.println(I18n.textf("Copying %filename", new File(folders[0], "Config.ini").getAbsolutePath()));
        FileUtil.copyFile(new File(folders[0], "Config.ini").getAbsolutePath(),
                new File(destination, "Config.ini").getAbsolutePath());

        for (File folder : folders) {
            File src = new File(folder, "Data.lsf");
            if (src.canRead()) {
                FileUtil.appendToFile(new File(destination, "Data.lsf"), new File(folder, "Data.lsf"));
                System.out.println(I18n.textf("Concatenating %filename",new File(folder, "Data.lsf").getAbsolutePath()));
            }
            else {
                src = new File(folder, "Data.lsf.gz");
                if (src.canRead()) {
                    FileUtil.appendToFile(new File(destination, "Data.lsf"), new GZIPInputStream(new FileInputStream(
                            new File(folder, "Data.lsf.gz"))));
                    
                    System.out.println(I18n.textf("Concatenating %filename", new File(folder, "Data.lsf.gz").getAbsolutePath()));
                }
            }
        }
    }    
    
    public static void main(String[] args) throws Exception {
        File[] folders = ConcatenateLsfLog.chooseFolders(null, new File(".").getAbsolutePath());        
        concatenateFolders(folders, new File("/home/zp/Desktop/output"), null);
    }    
}
