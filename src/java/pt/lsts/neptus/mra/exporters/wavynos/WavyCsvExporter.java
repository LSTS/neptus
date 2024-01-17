package pt.lsts.neptus.mra.exporters.wavynos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.LinkedHashMap;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.BatchMraExporter;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.MathMiscUtils;

public class WavyCsvExporter implements MRAExporter {

    private LinkedHashMap<Short, String> entityNames = new LinkedHashMap<>();
    IMraLogGroup source;
    NumberFormat doubles = MathMiscUtils.getNumberFormat(8, 6);
    NumberFormat floats = MathMiscUtils.getNumberFormat(3, 6);


    public WavyCsvExporter(IMraLogGroup source) {
        this.source = source;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("GpsFix");
    }

    public String getHeader(String messageType) {
        IMCMessageType type = source.getLsfIndex().getDefinitions().getType(messageType);
        String ret = "timestamp,system,entity";
        for (String field : type.getFieldNames()) {
            if (type.getFieldUnits(field) != null)
                ret += "," + field + " (" + type.getFieldUnits(field) + ")";
            else
                ret += "," + field;
        }
        return ret + "\n";
    }


    public String getLine(IMCMessage m) {
        
        String ret = floats.format(m.getTimestamp()) + "," + m.getSrc() + "," + m.getSrcEnt();

        for (String field : m.getFieldNames()) {
            Object v = m.getValue(field);
            if (v instanceof Number
                    && m.getMessageType().getFieldPossibleValues(field) != null) {
                if (m.getUnitsOf(field).equals("tuplelist")
                        || m.getUnitsOf(field).equals("enumerated")) {
                    String str = m.getMessageType().getFieldPossibleValues(field).get(
                            ((Number) v).longValue());
                    ret += "," + str;
                } else {

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
                    ret += "," + str;
                }
            } else {
                switch (m.getTypeOf(field)) {
                    case "fp32_t":
                        ret += "," + floats.format(m.getDouble(field));
                        break;
                    case "fp64_t":
                        ret += "," + doubles.format(m.getDouble(field));
                        break;
                    default:
                        ret += "," + m.getAsString(field);
                        break;
                }
            }
        }
        return ret + "\n";
    }

    void addLine(IMCMessage message, BufferedWriter writer) {
        try {
            writer.write(getLine(message));
        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        File dir = new File(source.getFile("mra"), "wavynos");
        dir.mkdirs();
        LsfIndex index = source.getLsfIndex();
        LinkedHashMap<String, BufferedWriter> writers = new LinkedHashMap<>();
        pmonitor.setMaximum(index.getNumberOfMessages());
        for (int i = 0; i < index.getNumberOfMessages(); i++) {
            if (pmonitor.isCanceled()) {
                try {
                    for (BufferedWriter w : writers.values())
                        w.close();
                } catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                return I18n.text("Operation canceled by user");
            }
            pmonitor.setProgress(i);
            pmonitor.setNote("Processing " + i + "/" + index.getNumberOfMessages());

            int type = index.typeOf(i);
            String msgName = index.getDefinitions().getMessageName(type);
            
            if (!writers.containsKey(msgName)) {
                try {
                    writers.put(msgName, new BufferedWriter(new FileWriter(new File(dir, msgName+".csv"))));      
                    writers.get(msgName).write(getHeader(msgName));              
                } catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
            try {
                writers.get(msgName).write(getLine(index.getMessage(i)));
            } catch (Exception e) {
                NeptusLog.pub().error(e);
            }           
        }

        try {
            for (BufferedWriter w : writers.values())
                w.close();
        } catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        return I18n.text("Done");
    }

      public static void main(String[] args) {
        BatchMraExporter.apply(WavyCsvExporter.class);
    }
}
