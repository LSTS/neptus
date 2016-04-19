/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Jun 28, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name="Export to CSV")
public class CSVExporter implements MRAExporter {

    IMraLogGroup source;
    ProgressMonitor pmonitor;
    LinkedHashMap<Short, String> entityNames = new LinkedHashMap<>();

    public CSVExporter(IMraLogGroup source) {
        this.source = source;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    public String getHeader(String messageType) {
        IMCMessageType type = source.getLsfIndex().getDefinitions().getType(messageType);
        String ret = "timestamp (seconds since 01/01/1970), system, entity ";
        for (String field : type.getFieldNames()) {
            if (type.getFieldUnits(field) != null)
                ret += ", " + field + " (" + type.getFieldUnits(field) + ")";
            else
                ret += ", " + field;
        }
        return ret + "\n";
    }

    public String getLine(IMCMessage m) {
        NumberFormat doubles = GuiUtils.getNeptusDecimalFormat(8);
        NumberFormat floats = GuiUtils.getNeptusDecimalFormat(3);
        String entity = entityNames.get(m.getSrcEnt());

        if (entity == null)
            entity = "" + m.getSrcEnt();

        String ret = floats.format(m.getTimestamp()) + ", " + m.getSourceName() + ", " + entity;

        for (String field : m.getFieldNames()) {
            switch (m.getTypeOf(field)) {
                case "fp32_t":
                    ret += ", " + floats.format(m.getDouble(field));
                    break;
                case "fp64_t":
                    ret += ", " + doubles.format(m.getDouble(field));
                    break;
                default:
                    ret += ", " + m.getAsString(field);
                    break;
            }
        }
        return ret + "\n";
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        //pmonitor = new ProgressMonitor(ConfigFetch.getSuperParentFrame(), I18n.text("Exporting to CSV"),
        //        I18n.text("Starting up"), 0, source.listLogs().length);
        File dir = new File(source.getFile("mra"), "csv");

        dir.mkdirs();

        entityNames.clear();
        LsfIterator<EntityInfo> it = source.getLsfIndex().getIterator(EntityInfo.class);
        if (it != null)
            for (EntityInfo ei : it)
                entityNames.put(ei.getId(), ei.getLabel());

        int i = 0;
        for (String message : source.listLogs()) {
            if (pmonitor.isCanceled())
                return I18n.text("Cancelled by the user");
            
            try {
                File out = new File(dir, message + ".csv");
                BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                pmonitor.setNote(I18n.textf("Exporting %message data to %csvfile...", message, out.getAbsolutePath()));
                pmonitor.setProgress(++i);
                bw.write(getHeader(message));

                for (IMCMessage m : source.getLsfIndex().getIterator(message)) {
                    bw.write(getLine(m));
                }
                bw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                pmonitor.close();
                return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        return I18n.text("Process complete");
    }
}
