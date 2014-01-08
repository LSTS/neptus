/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Jul 17, 2012
 */
package pt.lsts.neptus.mra.importers.lsf;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;

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
        
        NeptusLog.pub().info("<###> "+I18n.textf("Copying %filename", new File(folders[0], "IMC.xml").getAbsolutePath()));
        FileUtil.copyFile(new File(folders[0], "IMC.xml").getAbsolutePath(),
                new File(destination, "IMC.xml").getAbsolutePath());
        
        NeptusLog.pub().info("<###> "+I18n.textf("Copying %filename", new File(folders[0], "Config.ini").getAbsolutePath()));
        FileUtil.copyFile(new File(folders[0], "Config.ini").getAbsolutePath(),
                new File(destination, "Config.ini").getAbsolutePath());

        for (File folder : folders) {
            File src = new File(folder, "Data.lsf");
            if (src.canRead()) {
                FileUtil.appendToFile(new File(destination, "Data.lsf"), new File(folder, "Data.lsf"));
                NeptusLog.pub().info("<###> "+I18n.textf("Concatenating %filename",new File(folder, "Data.lsf").getAbsolutePath()));
            }
            else {
                src = new File(folder, "Data.lsf.gz");
                if (src.canRead()) {
                    FileUtil.appendToFile(new File(destination, "Data.lsf"), new GZIPInputStream(new FileInputStream(
                            new File(folder, "Data.lsf.gz"))));
                    
                    NeptusLog.pub().info("<###> "+I18n.textf("Concatenating %filename", new File(folder, "Data.lsf.gz").getAbsolutePath()));
                }
            }
        }
    }    
    
    public static void main(String[] args) throws Exception {
        File[] folders = ConcatenateLsfLog.chooseFolders(null, new File(".").getAbsolutePath());        
        concatenateFolders(folders, new File("/home/zp/Desktop/output"), null);
    }    
}
