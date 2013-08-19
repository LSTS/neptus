/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.llf;

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
