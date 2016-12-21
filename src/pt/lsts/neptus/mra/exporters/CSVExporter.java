/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ProgressMonitor;

import com.google.common.collect.Lists;

import pt.lsts.imc.EntityInfo;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.neptus.gui.editor.StringListEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name="Export to CSV")
public class CSVExporter implements MRAExporter {
    
    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";
    
    @NeptusProperty(name = "Message List to Export", editorClass = StringListEditor.class,
            description = "List of messages to export. Use '!' at the begining to make it an exclude list.")
    public String msgList = "";

    @NeptusProperty(name = "Textualize enumerations and bitfields",
            description = "If true will transform the enumerations and bitfields into the textual representation.")
    public boolean textualizeEnum = true;

    private IMraLogGroup source;
    private LinkedHashMap<Short, String> entityNames = new LinkedHashMap<>();

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
        return ret + LINE_ENDING;
    }

    public String getLine(IMCMessage m) {
        NumberFormat doubles = GuiUtils.getNeptusDecimalFormat(8);
        NumberFormat floats = GuiUtils.getNeptusDecimalFormat(3);
        String entity = entityNames.get(m.getSrcEnt());

        if (entity == null)
            entity = "" + m.getSrcEnt();

        String ret = floats.format(m.getTimestamp()) + ", " + m.getSourceName() + ", " + entity;

        for (String field : m.getFieldNames()) {
            Object v = m.getValue(field);
            if (textualizeEnum && v instanceof Number
                    && m.getMessageType().getFieldPossibleValues(field) != null) {
                if (m.getUnitsOf(field).equals("tuplelist")
                        || m.getUnitsOf(field).equals("enumerated")) {
                    return m.getMessageType().getFieldPossibleValues(field).get(
                            ((Number) v).longValue());
                }
                else {

                    long val = m.getLong(field);
                    String str = "";
                    for (int i = 0; i < 16; i++) {
                        long bitVal = (long) Math.pow(2, i);
                        if ((val & bitVal) > 0)
                            str += m.getMessageType().getFieldPossibleValues(field).get(bitVal) + "|";
                    }
                    str = str.replaceAll("null\\|", "");
                    str = str.replaceAll("\\|null", "");
                    if (str.length() > 0) // remove last "|"
                        str = str.substring(0, str.length() - 1);
                    ret += ", " + str;
                }
            }
            else {
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
        }
        return ret + LINE_ENDING;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        //pmonitor = new ProgressMonitor(ConfigFetch.getSuperParentFrame(), I18n.text("Exporting to CSV"),
        //        I18n.text("Starting up"), 0, source.listLogs().length);
        
        if (PluginUtils.editPluginProperties(this, true))
            return I18n.text("Cancelled by the user.");

        String tmpList = msgList.trim();
        boolean includeList = true;
        if (tmpList.isEmpty() || tmpList.startsWith("!")) {
            includeList = false;
            if (!tmpList.isEmpty())
                tmpList = tmpList.replaceFirst("!", "");
        }
        String[] lst = tmpList.split(",");
        List<String> messagesList = Lists.newArrayList(lst);
        
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
            
            boolean acceptMsg = true;
            if (includeList)
                acceptMsg = messagesList.contains(message);
            else
                acceptMsg = !messagesList.contains(message);
            if (!acceptMsg)
                continue;
            
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
