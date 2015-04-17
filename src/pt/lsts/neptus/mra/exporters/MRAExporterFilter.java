/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel
 * Apr 15, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author Manuel R.
 *
 */
@PluginDescription
@Popup(icon = "images/menus/settings.png", name = "MRA Exporter")
public class MRAExporterFilter implements MRAExporter, PropertiesProvider {

    IMraLogGroup source;
    ProgressMonitor pmonitor;
    ArrayList<String> defaultLogs = new ArrayList<String>();

    public MRAExporterFilter(IMraLogGroup source) {
        this.source = source;
        defaultLogs.add("Voltage");
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    private File chooseSaveFile(String path) {

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(path.concat("/Data_filtered.lsf")));
        
        
        fileChooser.setFileView(new NeptusFileView());
        fileChooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("LSF log files"),
                new String[] { "lsf", FileUtil.FILE_TYPE_LSF_COMPRESSED, 
            FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2 }));
        
        fileChooser.setAcceptAllFileFilterUsed(false);
        

        int status = fileChooser.showSaveDialog(null);

        String fileName = null;

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                fileName = selectedFile.getCanonicalPath();
                if (!fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName + ".lsf");
                }
                if (fileName.endsWith(".lsf")) {
                    return selectedFile = new File(fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
    
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        //list of messages in this log source
        String[] logs = source.listLogs();
        LsfIndex index = source.getLsfIndex();
        
        String path = source.getFile("Data.lsf").getParent();
        File outputFile = chooseSaveFile(path);
        if (outputFile == null) {
            return "Cancelled by the user";
        }
        OutputStream fos = null;
        if(!outputFile.exists()) {
            try {
                outputFile.createNewFile();
                fos = new FileOutputStream(outputFile, true);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        } 

        System.out.println("Filtering... " + defaultLogs.toString());
        for (String logName : logs) {
            if (defaultLogs.contains(logName)) {

                int mgid = index.getDefinitions().getMessageId(logName);
                int firstPos = index.getFirstMessageOfType(mgid);
                int lastPos = index.getLastMessageOfType(mgid);
                int j = firstPos;
                
                try {
                    while (j < lastPos) {
                        //  IMCMessage entry = index.getMessage(j);
                        //  System.out.println(entry.toString());
                        //  System.out.println("pos "+ j);

                        //write msg bytes
                        byte[] by = index.getMessageBytes(j);
                        fos.write(by);

                        j = index.getNextMessageOfType(mgid, j);
                    }
                    //append last message
                    byte[] lastMsg = index.getMessageBytes(lastPos);
                    fos.write(lastMsg);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        try {
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return I18n.text("Process complete");
    }

    @Override
    public String getName() {
        return I18n.text("Export filtered");
    }

    @Override
    public DefaultProperty[] getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setProperties(Property[] properties) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getPropertiesDialogTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        // TODO Auto-generated method stub
        return null;
    }

}