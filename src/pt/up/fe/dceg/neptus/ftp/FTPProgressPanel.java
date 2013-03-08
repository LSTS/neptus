/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Dec 21, 2012
 * $Id:: FTPProgressPanel.java 9615 2012-12-30 23:08:28Z pdias                  $:
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
            System.out.println("Finished Transfering");
            out.close();
            in.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
