/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Dec 21, 2012
 */
package pt.up.fe.dceg.neptus.ftp;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.ProgressPanel;

/**
 * @author jqcorreia
 *
 */
@SuppressWarnings("serial")
public class FTPProgressPanel extends ProgressPanel {
    JLabel label;
    JButton downloadButton;
    FTPFile file;
    
    String sourcePath;
    
    public FTPProgressPanel(FTPFile file, String sourcePath, DefaultMutableTreeNode parent) {
        
        this.file = file;
        this.sourcePath = sourcePath;
        if(file != null) {
            label = new JLabel(file.getName() + " " + file.getSize());
        }
        
        add(label);
    }

    public String getPath() {
        return sourcePath;
    }

    public FTPFile getFile() {
        return file;
    }
    
    public void setSelected(boolean sel) {
        if(sel) {
            setBackground(Color.BLACK);
            label.setForeground(Color.WHITE);
        }
        else {
            label.setForeground(Color.BLACK);
            setBackground(Color.WHITE);
        }
    }
    public void download(String destPath, FTPClient client) {
        byte[] buffer = new byte[10240];
        try {
            new File(destPath + "/" + sourcePath).getParentFile().mkdirs();
            FileOutputStream out = new FileOutputStream(destPath + "/" + sourcePath);
            InputStream in = client.retrieveFileStream(sourcePath);
            int counter = 0;
            while (true) {
                int bytes = in.read(buffer);    
                System.out.println(bytes);
                if (bytes < 0)
                    break;

                out.write(buffer, 0, bytes);
                counter += bytes;
                System.out.println(counter);
            }
            NeptusLog.pub().info("<###>Finished Transfering");
            out.close();
            in.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
