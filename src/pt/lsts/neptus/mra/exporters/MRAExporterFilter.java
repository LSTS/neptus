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

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author Manuel R.
 *
 */
@PluginDescription
public class MRAExporterFilter implements MRAExporter {

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

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        String[] logs = source.listLogs();
        LsfIndex index = source.getLsfIndex();
        File outputFile = new File("C://Data.lsf");
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
                int secondPos = index.getNextMessageOfType(mgid, firstPos);
                int thPos = index.getNextMessageOfType(mgid, secondPos);

                int lastPos = index.getLastMessageOfType(mgid);

                System.out.println("First pos "+ firstPos + " - Last:  "+ lastPos);

                IMCMessage entry1 = index.getMessage(firstPos);
                System.out.println(entry1.toString());

                IMCMessage entry2 = index.getMessage(secondPos);
                System.out.println(entry2.toString());

                IMCMessage entry3 = index.getMessage(thPos);
                System.out.println(entry3.toString());

                IMCMessage entryLast = index.getMessage(lastPos);
                System.out.println(entryLast.toString());
                System.out.println("-------------------");
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

        return I18n.text("Process complete");
    }

    @Override
    public String getName() {
        return I18n.text("Export filtered");
    }

}