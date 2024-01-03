/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * Jun 5, 2012
 */
package pt.lsts.neptus.plugins.mraplots;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

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
