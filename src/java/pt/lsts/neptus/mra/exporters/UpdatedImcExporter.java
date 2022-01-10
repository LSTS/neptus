/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Apr 29, 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileOutputStream;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.ImcStringDefs;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
@PluginDescription(name="Log using updated IMC")
public class UpdatedImcExporter implements MRAExporter {

    public UpdatedImcExporter(IMraLogGroup source) {       
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        IMCDefinition updated = IMCDefinition.getInstance();
        LsfIndex index = source.getLsfIndex();
        int max = index.getNumberOfMessages();
        pmonitor.setMaximum(max);
        pmonitor.setProgress(0);        
        String version = updated.getVersion();
        File out = new File(source.getDir(), "mra/imc_"+version);
        out.mkdirs();
        try {
            FileUtil.saveToFile(new File(source.getDir(), "mra/imc_"+version+"/IMC.xml").getAbsolutePath(), ImcStringDefs.getDefinitions());
            IMCOutputStream ios = new IMCOutputStream(new FileOutputStream(new File(out, "Data.lsf")));
            for (int i = 0; i < index.getNumberOfMessages(); i++) {
                if (pmonitor.isCanceled()) {
                    return I18n.text("Cancelled by the user");
                }
                pmonitor.setProgress(i);
                pmonitor.setNote(I18n.textf("Message %num of %total",i,max));
                if (updated.getMessageName(index.typeOf(i)) != null) {
                    IMCMessage msg = updated.create(updated.getMessageName(index.typeOf(i)));
                    msg.setValues(index.getMessage(i).getValues());
                    msg.setTimestamp(index.timeOf(i));
                    msg.setSrc(index.sourceOf(i));
                    msg.setSrcEnt(index.entityOf(i));
                    ios.writeMessage(msg);
                }
                else {
                   NeptusLog.pub().warn("Skipping unrecognized message of type "+index.typeOf(i));
                }
            }
            ios.close();
            pmonitor.close();
        }
        catch(Exception e) {
            NeptusLog.pub().error(e);
            return I18n.textf("Error while converting: %error", e.getMessage());
        }
        return  I18n.textf("Log writen to %path", out.getAbsolutePath());
    }
}
