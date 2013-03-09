/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jun 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class LsfIndexTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    protected final String[] fields = new String[] {I18n.text("Time (UTC)"), I18n.text("Message"), I18n.text("Source"), I18n.text("Src. Entity"), I18n.text("Destination")};
    
    protected LsfIndex index = null;
    
    protected SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss.SSS ");
    {
        dateFormatUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public LsfIndexTableModel(LsfIndex index) {
        this.index = index;
    }
    @Override
    public int getRowCount() {
        return index.getNumberOfMessages(); //header
    }

    @Override
    public int getColumnCount() {
        return fields.length;
    }
    
    public String getColumnName(int column) {
        return fields[column];
    };

    LinkedHashMap<Integer, IMCMessage> cache = new LinkedHashMap<Integer, IMCMessage>() {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(java.util.Map.Entry<Integer,IMCMessage> eldest) {
            return super.size() > 100;
        };
    };
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        IMCMessage msg = (cache.containsKey(rowIndex) ? cache.get(rowIndex) : index.getMessage(rowIndex));        

        cache.put(rowIndex, msg);
        
        switch (columnIndex) {
            case 0:
                return dateFormatUTC.format(new Date(msg.getTimestampMillis()));
            case 1:
                return msg.getAbbrev();
            case 2:
                return index.getDefinitions().getResolver().resolve(msg.getSrc());
            case 3: 
                return index.getEntityName(msg.getSrc(), msg.getSrcEnt());
            case 4: 
                return index.getDefinitions().getResolver().resolve(msg.getDst());
            default:
                return "?";             
        }        
    }

    
    public static void main(String[] args) throws Exception {
        LsfIndex index = new LsfIndex(new File("/home/zp/Desktop/143245_rows_minus1.5m_1000rpm/Data.lsf"), new IMCDefinition(new FileInputStream(new File("/home/zp/Desktop/143245_rows_minus1.5m_1000rpm/IMC.xml"))));
        LsfIndexTableModel tableModel = new LsfIndexTableModel(index);
        JTable table = new JTable(tableModel);
        GuiUtils.testFrame(new JScrollPane(table));
    }
}
