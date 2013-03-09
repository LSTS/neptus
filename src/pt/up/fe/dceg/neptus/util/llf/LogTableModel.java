/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCMessageType;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;

@SuppressWarnings("serial")
public class LogTableModel extends AbstractTableModel {

	//RandomAccessFile raf = null;
	private LinkedHashMap<Integer, Vector<Object>> cache = new LinkedHashMap<Integer, Vector<Object>>();
	protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
	protected int rowCount = 1;
	protected IMraLog parser;
	protected IMraLogGroup source;
	protected IMCMessageType msgType;
	
	protected void load(IMraLog parser) {

        IMCMessage msg = parser.firstLogEntry();
        int rowIndex = 0;
        while (msg != null) {
            Vector<Object> values = new Vector<Object>();
            values.add(msg.getTimestampMillis());
            int src = msg.getInteger("src");
            int src_ent = msg.getInteger("src_ent");
            int dst = msg.getInteger("dst");
            int dst_ent = msg.getInteger("dst_ent");

            values.add(source.getSystemName(src));
            values.add(source.getEntityName(src, src_ent));
            values.add(source.getSystemName(dst));
            values.add(source.getEntityName(dst, dst_ent));

            for (String key : msg.getMessageType().getFieldNames())
                values.add(msg.getString(key));

            cache.put(rowIndex, values);
            msg = parser.nextLogEntry();
            rowIndex++;
        }
	 
	}
	
	public LogTableModel(IMraLogGroup source, IMraLog log) {
	    this.source = source;
	    parser = log;
	    try {						
			msgType = parser.firstLogEntry().getMessageType();
			rowCount = parser.getNumberOfEntries();
			load(parser);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public int getColumnCount() {		
		if (msgType == null)
		    return 1;
		
		return 5+msgType.getFieldNames().size();
	}

	
	public int getRowCount() {		
		if (parser == null)
			return 1;
		return rowCount;
	}


	public Object getValueAt(int rowIndex, int columnIndex) {		
		
	    if (parser == null) {
			return "Unable to load data";
		}
		if (cache.containsKey(rowIndex)) {
			return cache.get(rowIndex).get(columnIndex);		    
		}
		return null;					
	}
	
	@Override
	public String getColumnName(int column) {
		if (parser == null)
			return "Error";
		
		Vector<String> names =  new Vector<String>();
		names.add("time");
		names.add("src");
		names.add("src_ent");
		names.add("dst");
		names.add("dst_ent");
		names.addAll(msgType.getFieldNames());
		return names.get(column);
		
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
	    return false;
	}
}
