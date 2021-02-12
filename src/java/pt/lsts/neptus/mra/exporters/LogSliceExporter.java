/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 14, 2016
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EntityList;
import pt.lsts.imc.EntityList.OP;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.LoggingControl;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.CheckboxList;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Log Slice", experimental = true)
public class LogSliceExporter implements MRAExporter {

    Date start, end;

    private String inputInterval(IMraLogGroup source) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

        start = new Date((long) (source.getLsfIndex().getStartTime() * 1000));
        end = new Date((long) (source.getLsfIndex().getEndTime() * 1000));
        Date startOfDay = new Date();
        try {
            startOfDay = Date.from(LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant());
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass() + ": " + e.getMessage();
        }

        System.out.println(startOfDay);
        System.out.println(sdf.format(startOfDay));
        try {
            System.out.println(sdf.parse(sdf.format(startOfDay)));
        }
        catch (ParseException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        String startSel = "";
        while (startSel.isEmpty()) {
            startSel = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(),
                    I18n.text("Select start time (UTC)"), sdf.format(start));
            if (startSel == null)
                return I18n.text("Cancelled by the user");
            try {
                start = sdf.parse(startSel);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
                startSel = "";
                continue;
            }
        }

        String endSel = "";
        while (endSel.isEmpty()) {
            endSel = JOptionPane.showInputDialog(ConfigFetch.getSuperParentFrame(), I18n.text("Select end time (UTC)"),
                    sdf.format(end));
            if (endSel == null)
                return I18n.text("Cancelled by the user");
            try {
                end = sdf.parse(endSel);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
                endSel = "";
                continue;
            }
        }

        if (start.after(end)) {
            return I18n.text("Start time must be before end time");
        }

        start = new Date(start.getTime());
        end = new Date(end.getTime());
        return null;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        String problem = inputInterval(source);
        if (problem != null)
            return problem;

        LsfIndex index = source.getLsfIndex();
        HashSet<String> availableMessages = new HashSet<>();

        for (int i = 0; i < index.getNumberOfMessages(); i++) {
            int type = index.typeOf(i);
            String name = index.getDefinitions().getMessageName(type);
            if (name != null)
                availableMessages.add(name);
        }

        ArrayList<String> selectedMsgs = new ArrayList<>();
        selectedMsgs.addAll(availableMessages);
        Collections.sort(selectedMsgs);
        String[] result = CheckboxList.selectOptions(ConfigFetch.getSuperParentFrame(), I18n.text("Messages to export"),
                selectedMsgs.toArray(new String[selectedMsgs.size()]));
        if (result == null)
            return I18n.text("Cancelled by user.");

        ArrayList<Integer> selectedTypes = new ArrayList<>();
        for (String r : result)
            selectedTypes.add(index.getDefinitions().getMessageId(r));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        File outputdir = new File(source.getDir(), sdf.format(start));
        int count = 1;
        while (outputdir.exists()) {
            outputdir = new File(source.getDir(), sdf.format(start) + "." + count);
            count++;
        }

        outputdir.mkdirs();
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new GZIPOutputStream(new FileOutputStream(new File(outputdir, "IMC.xml.gz")))));

            writer.write(index.getDefinitions().getSpecification());
            writer.close();

            GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(new File(outputdir, "Data.lsf.gz")));

            writeInitialMessages(index, out);
            double endTime = end.getTime() / 1000.0;
            for (int i = index.getFirstMessageAtOrAfter(start.getTime() / 1000.0); i != -1; i++) {
                if (index.timeOf(i) > endTime)
                    break;
                if (!selectedTypes.contains(index.typeOf(i)))
                    continue;

                out.write(index.getMessageBytes(i));
            }
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getClass() + ": " + e.getMessage();
        }

        return I18n.textf("Log file written to %path", outputdir.getAbsolutePath());
    }

    private void writeInitialMessages(LsfIndex index, OutputStream out) throws Exception {
        LinkedHashMap<Integer, EntityList> entities = new LinkedHashMap<>();
        LinkedHashMap<Integer, Announce> announces = new LinkedHashMap<>();

        LoggingControl log = index.getFirst(LoggingControl.class);

        if (log != null) {
            log.setName("log slice");
            log.setTimestampMillis(start.getTime());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream ios = new IMCOutputStream(baos);
            ios.writeMessage(log);
            out.write(baos.toByteArray());
            ios.close();
        }

        for (EntityList el : index.getIterator(EntityList.class)) {
            if (el.getTimestampMillis() > start.getTime())
                break;
            else if (el.getOp() == OP.REPORT)
                entities.put(el.getSrc(), el);
        }

        for (Announce an : index.getIterator(Announce.class)) {
            if (an.getTimestampMillis() > start.getTime())
                break;
            else
                announces.put(an.getSrc(), an);
        }

        ArrayList<IMCMessage> msgs = new ArrayList<>();
        msgs.addAll(entities.values());
        msgs.addAll(announces.values());

        for (IMCMessage m : msgs) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream ios = new IMCOutputStream(baos);
            m.setTimestampMillis(start.getTime());
            ios.writeMessage(m);
            out.write(baos.toByteArray());
            ios.close();
        }
    }

    public static void main(String[] args) throws ParseException {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        Date startOfDay = new Date();
        try {
            startOfDay = Date.from(LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant());
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        timeFormatter.format(startOfDay.toInstant());
        TemporalAccessor ta = timeFormatter.parse(timeFormatter.format(startOfDay.toInstant()));
        Date date = Date.from(Instant.from(ta));

        System.out.println(startOfDay);
        System.out.println(date);
    }
}
