/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Apr 16, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.ProgressMonitor;

import com.google.common.collect.Lists;
import com.jmatio.io.MatFileWriter;
import com.jmatio.types.MLArray;
import com.jmatio.types.MLCell;
import com.jmatio.types.MLChar;
import com.jmatio.types.MLDouble;
import com.jmatio.types.MLInt16;
import com.jmatio.types.MLInt64;
import com.jmatio.types.MLInt8;
import com.jmatio.types.MLSingle;
import com.jmatio.types.MLStructure;
import com.jmatio.types.MLUInt64;
import com.jmatio.types.MLUInt8;

import pt.lsts.imc.IMCFieldType;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.StringListEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author pdias
 * @author jqcorreia
 */
@PluginDescription(name = "MatLab format .MAT")
public class MatExporter implements MRAExporter {
    @NeptusProperty(editable = false)
    private static final int MAX_PLAINTEXT_RAWDATA_LENGHT = 256; //256; 0xFFFF
    @NeptusProperty(editable = false)
    private static final int MAX_ENUMERATED_LENGHT = 50;
    @NeptusProperty(editable = false)
    private static final int MAX_BITFIELD_LENGHT = 256;

    private IMraLogGroup source;

    @NeptusProperty(name = "Ignore Header Fields", editable = false)
    private String[] ignoreHeaderFields = { "sync", "mgid", "size" };
    @NeptusProperty(name = "Write Header Fields for Inline Messages", editable = true)
    private boolean flagWriteHeaderFieldsForInlineMessages = false;
    @NeptusProperty(name = "Write Enumerated and Bitfield as String", editable = true)
    private boolean flagWriteEnumeratedAndBitfieldAsString = true;
    @NeptusProperty(name = "Message List to Export", editorClass = StringListEditor.class,
            description = "List of messages to export (comma separated values, no spaces). Use '!' at the begining to make it an exclude list.")
    public String msgList = "";

    public MatExporter(IMraLogGroup source) {
        this.source = source;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup logSource, ProgressMonitor pmonitor) {
        this.source = logSource;

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

        Collection<String> logList = source.getLsfIndex().getDefinitions().getMessageNames();
        IMraLog parser;

        File outFolder = new File(source.getDir(), "mra/matlab");
        outFolder.mkdir();
        
        MatFileWriter writer = new MatFileWriter();

        MLStructure struct;

        double messageLogPartialPerc = 1. / logList.size();
        double progress = 0;
        final double structFullPrec = 60;
        final double writeFullPrec = 100 - structFullPrec;
        for(String messageLog : logList) {
            try {
                if (pmonitor != null && pmonitor.isCanceled())
                    break;

                boolean acceptMsg = true;
                if (includeList)
                    acceptMsg = messagesList.contains(messageLog);
                else
                    acceptMsg = !messagesList.contains(messageLog);
                if (!acceptMsg)
                    continue;

                parser = source.getLog(messageLog);

                if(parser == null || parser.getNumberOfEntries() == 0) {
                    NeptusLog.pub().debug("Reading nothing for " + messageLog);
                    if (pmonitor != null)
                        pmonitor.setNote(I18n.textf("Reading nothing for %message", messageLog));
                    progress += (structFullPrec + writeFullPrec) * messageLogPartialPerc;
                    if (pmonitor != null)
                        pmonitor.setProgress((int) progress);
                    continue;
                }

                NeptusLog.pub().debug("Reading " + messageLog);
                if (pmonitor != null)
                    pmonitor.setNote(I18n.textf("Reading %message", messageLog));
                
                struct = new MLStructure(messageLog, new int[] {1, 1});
                int numEntries = parser.getNumberOfEntries();
                int numInserted = 0;

                LinkedHashMap<String, MLArray> fieldMap = new LinkedHashMap<String, MLArray>();

                // Setup arrays for struct
                IMCMessage m = parser.firstLogEntry();
                do {
                    if (pmonitor.isCanceled())
                        break;
                    // Getting the header
                    for(String field : m.getHeader().getFieldNames()) {
                        processField(field, m, numEntries, numInserted, fieldMap);
                    }
                    // Getting the fields
                    for(String field : m.getFieldNames()) {
                        processField(field, m, numEntries, numInserted, fieldMap);
                    }
                    numInserted++;
                } while ((m = parser.nextLogEntry()) != null);

                // Adding Field values to struct
                for(String field : fieldMap.keySet()) {
                    if (pmonitor.isCanceled())
                        break;
                    struct.setField(field, fieldMap.get(field));
                }

                if (pmonitor.isCanceled())
                    break;

                progress += structFullPrec * messageLogPartialPerc;
                if (pmonitor != null)
                    pmonitor.setProgress((int) progress);

                Collection<MLArray> baseMatLabDataToWrite = new ArrayList<MLArray>();

                baseMatLabDataToWrite.add(struct);

                NeptusLog.pub().debug("Writing " + messageLog);
                if (pmonitor != null)
                    pmonitor.setNote(I18n.textf("Writing %message", messageLog));

                File out = new File(outFolder, messageLog + ".mat");
                try {
                    writer.write(out, baseMatLabDataToWrite);
                }
                catch (Exception e) {
                    String msg = e.getClass().getSimpleName() + " while exporting to MAT: " + e.getMessage();
                    NeptusLog.pub().error(msg, e);
                }

                progress += writeFullPrec * messageLogPartialPerc;
                if (pmonitor != null)
                    pmonitor.setProgress((int) progress);
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error processing " + messageLog + ". " + e.getMessage() + "!");
                String txt = I18n.textf("Error processing %message. %error error.", messageLog, e.getMessage());
                if (pmonitor != null)
                    pmonitor.setNote(txt);
                GuiUtils.errorMessage(MatExporter.class.getSimpleName(), txt);
            }
            catch (OutOfMemoryError e) {
                NeptusLog.pub().error("Error processing " + messageLog + ". OutOfMemoryError!");
                String txt = I18n.textf("Error processing %message. %error error.", messageLog, "OutOfMemoryError");
                if (pmonitor != null)
                    pmonitor.setNote(txt);
                GuiUtils.errorMessage(MatExporter.class.getSimpleName(), txt);
            }
        }

        if (pmonitor != null) {
            pmonitor.setNote(I18n.text("Log exported to MAT successfully"));
            pmonitor.setProgress(100);
        }
        return I18n.text("Log exported to MAT successfully");
    }

    /**
     * @param field
     * @param message
     * @param totalEntries
     * @param indexToInsert
     * @param fieldMap
     */
    private void processField(String field, IMCMessage message, int totalEntries, int indexToInsert,
            LinkedHashMap<String, MLArray> fieldMap) {
        // The API only is able to write to file the following types
        // MLArray.mxUINT8_CLASS
        // MLArray.mxINT8_CLASS
        // MLArray.mxINT16_CLASS
        // MLArray.mxINT64_CLASS
        // MLArray.mxUINT64_CLASS
        //
        // MLArray.mxSINGLE_CLASS
        // MLArray.mxDOUBLE_CLASS
        //
        // MLArray.mxCHAR_CLASS
        //
        // MLArray.mxSTRUCT_CLASS
        // MLArray.mxCELL_CLASS:
        // MLArray.mxSPARSE_CLASS

        // Getting field type
        IMCFieldType filedType = message.getMessageType().getFieldType(field);
        if (filedType == null) {
            filedType = message.getHeader().getMessageType().getFieldType(field);
            for (String ignoreFieldName : ignoreHeaderFields) {
                if (ignoreFieldName.equalsIgnoreCase(field))
                    return;
            }
        }

        // Getting field unit
        String fieldUnit = message.getMessageType().getFieldUnits(field);
        if (fieldUnit == null) {
            fieldUnit = message.getHeader().getMessageType().getFieldUnits(field);
        }

        // For Enumerated and Bitfield to be written as string
        if (flagWriteEnumeratedAndBitfieldAsString
                && ("Enumerated".equalsIgnoreCase(fieldUnit) || "Bitfield".equalsIgnoreCase(fieldUnit))) {
            boolean enumeratedOrBitfield = "Enumerated".equalsIgnoreCase(fieldUnit);
            if (fieldMap.get(field) == null) {
                fieldMap.put(field, new MLChar(field, new int[] { totalEntries,
                        enumeratedOrBitfield ? MAX_ENUMERATED_LENGHT : MAX_BITFIELD_LENGHT }, MLArray.mxCHAR_CLASS, 0));
            }
            String val = message.getString(field);
            ((MLChar) fieldMap.get(field)).set(val == null ? "" : val, indexToInsert);
            return;
        }
        // Other fields
        switch (filedType) {
            case TYPE_FP32:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLSingle(field, new int[] { totalEntries, 1 }, MLArray.mxSINGLE_CLASS, 0));
                ((MLSingle) fieldMap.get(field)).set((float) message.getDouble(field), indexToInsert);
                break;
            case TYPE_FP64:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLDouble(field, new int[] { totalEntries, 1 }));
                ((MLDouble) fieldMap.get(field)).set(message.getDouble(field), indexToInsert);
                break;
            case TYPE_INT8:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLInt8(field, new int[] { totalEntries, 1 }));
                ((MLInt8) fieldMap.get(field)).set((byte) message.getInteger(field), indexToInsert);
                break;
            case TYPE_INT16:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLInt16(field, new int[] { totalEntries, 1 }));
                ((MLInt16) fieldMap.get(field)).set((short) message.getInteger(field), indexToInsert);
                break;
            case TYPE_INT32: // Not in the write to file from API
            case TYPE_INT64:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLInt64(field, new int[] { totalEntries, 1 }));
                ((MLInt64) fieldMap.get(field)).set(message.getLong(field), indexToInsert);
                break;
            case TYPE_UINT8:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLUInt8(field, new int[] { totalEntries, 1 }));
                ((MLUInt8) fieldMap.get(field)).set((byte) message.getInteger(field), indexToInsert);
                break;
            case TYPE_UINT16: // Not in the write to file from API
            case TYPE_UINT32: // Not in the write to file from API
//            case TYPE_UINT64: // IMC don't have uint64 yet
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLUInt64(field, new int[] { totalEntries, 1 }));
                ((MLUInt64) fieldMap.get(field)).set(message.getLong(field), indexToInsert);
                break;
            case TYPE_MESSAGE:
                {
                    if (fieldMap.get(field) == null)
                        fieldMap.put(field, new MLCell(field, new int[] {totalEntries, 1}));

                    MLStructure struct = new MLStructure(field, new int[] {1, 1});
                    IMCMessage inlineMsg = message.getMessage(field);
                    LinkedHashMap<String, MLArray> fieldMessageListMap = new LinkedHashMap<String, MLArray>();

                    if (inlineMsg != null) {
                        if (flagWriteHeaderFieldsForInlineMessages) {
                            // Getting the header
                            for(String fieldInline : inlineMsg.getHeader().getFieldNames()) {
                                processField(fieldInline, inlineMsg, 1, 0, fieldMessageListMap);
                            }
                        }
                        // Getting the fields
                        for(String fieldInline : inlineMsg.getFieldNames()) {
                            processField(fieldInline, inlineMsg, 1, 0, fieldMessageListMap);
                        }
                    }

                    // Adding Field values to struct
                    for(String fieldInline : fieldMessageListMap.keySet()) {
                        struct.setField(fieldInline, fieldMessageListMap.get(fieldInline));
                    }
                    ((MLCell) fieldMap.get(field)).set(struct, indexToInsert);
                    break;
                }
            case TYPE_MESSAGELIST:
                {
                    if (fieldMap.get(field) == null)
                        fieldMap.put(field, new MLCell(field, new int[] {totalEntries, 1}));

                    Vector<IMCMessage> inlineMsgList = message.getMessageList(field);
                    MLCell cell = new MLCell(field, new int[] {inlineMsgList.size(), inlineMsgList.size() != 0 ? 1 : 0});
                    int numInserted = 0;

                    for (IMCMessage inlineMsg : inlineMsgList) {
                        MLStructure struct = new MLStructure(field, new int[] {1, 1});
                        LinkedHashMap<String, MLArray> fieldMessageListMap = new LinkedHashMap<String, MLArray>();

                        if (inlineMsg != null) {
                            if (flagWriteHeaderFieldsForInlineMessages) {
                                // Getting the header
                                for(String fieldInline : inlineMsg.getHeader().getFieldNames()) {
                                    processField(fieldInline, inlineMsg, 1, 0, fieldMessageListMap);
                                }
                            }
                            // Getting the fields
                            for(String fieldInline : inlineMsg.getFieldNames()) {
                                processField(fieldInline, inlineMsg, 1, 0, fieldMessageListMap);
                            }
                        }

                        // Adding Field values to struct
                        for(String fieldInline : fieldMessageListMap.keySet()) {
                            struct.setField(fieldInline, fieldMessageListMap.get(fieldInline));
                        }
                        cell.set(struct, numInserted);
                        numInserted++;
                    }
                    ((MLCell) fieldMap.get(field)).set(cell, indexToInsert);
                    break;
                }
            case TYPE_PLAINTEXT:
            case TYPE_RAWDATA:
            default:
                if (fieldMap.get(field) == null)
                    fieldMap.put(field, new MLChar(field, new int[] { totalEntries, MAX_PLAINTEXT_RAWDATA_LENGHT }, MLArray.mxCHAR_CLASS, 0));
                String val = message.getAsString(field);
                ((MLChar) fieldMap.get(field)).set(val == null ? "" : val, indexToInsert);
                break;
        }
    }

    public static void main(String[] args) throws Exception {
        IMraLogGroup source = new LsfLogSource(new File("/home/pdias/Desktop/Sunfish-CTD/20140513/135733_sample_front/Data.lsf"), null);

        MatExporter me = new MatExporter(source);
        me.process(source, new ProgressMonitor(null, "", "", 0, 100));
    }
}
