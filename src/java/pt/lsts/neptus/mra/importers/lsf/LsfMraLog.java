/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * May 4, 2012
 */
package pt.lsts.neptus.mra.importers.lsf;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLog;

/**
 * @author ZP
 * @author jqcorreia
 * 
 */
public class LsfMraLog implements IMraLog {

    LsfIndex index;
    String name;
    int curIndex = -1;
    int type;

    public LsfMraLog(LsfIndex index, String name) {
        this.index = index;
        this.name = name;
        type = index.getDefinitions().getMessageId(name);
        curIndex = index.getFirstMessageOfType(type);
    }

    protected void advanceUntil(long timestamp) {
        while (curIndex != -1 && currentTimeMillis() < timestamp)
            curIndex = index.getNextMessageOfType(name, curIndex);
    }

    @Override
    public IMCMessage getEntryAtOrAfter(long timestamp) {

        if (curIndex == -1)
            return null;

        if (timestamp / 1000 > index.timeOf(index.getLastMessageOfType(type)))
            return null;

        advanceUntil(timestamp);

        if (curIndex != -1)
            return index.getMessage(curIndex);

        return null;
    }

    public IMCMessage getEntryAtOrAfter(long timestamp, String entityName) {

        if (curIndex == -1)
            return null;

        if (timestamp / 1000 > index.timeOf(index.getLastMessageOfType(type)))
            return null;

        advanceUntil(timestamp);
        if (curIndex != -1) {
            IMCMessage msg = index.getMessage(curIndex);
            while (msg != null) {
                if (index.getEntityName(msg.getHeader().getInteger("src_ent")).equals(entityName)) {
                    return msg;
                }
                msg = nextLogEntry();
            }
        }
        
        return null;
    }
    
    public IMCMessage getEntryAtOrAfter(long timestamp, int source) {

        if (curIndex == -1)
            return null;

        if (timestamp / 1000 > index.timeOf(index.getLastMessageOfType(type)))
            return null;

        advanceUntil(timestamp);
        if (curIndex != -1) {
            IMCMessage msg = index.getMessage(curIndex);
            while (msg != null) {
                if (msg.getSrc()== source) {
                    return msg;
                }
                msg = nextLogEntry();
            }
        }
        
        return null;
    }
    @Override
    public String name() {
        return name;
    }

    @Override
    public IMCMessage getLastEntry() {
        curIndex = index.getLastMessageOfType(index.getDefinitions().getMessageId(name));
        if (curIndex == -1)
            return null;
        return index.getMessage(curIndex);
    }

    @Override
    public IMCMessageType format() {
        return index.getMessage(index.getFirstMessageOfType(name)).getMessageType();
    }

    @Override
    public LinkedHashMap<String, Object> metaInfo() {
        return new LinkedHashMap<String, Object>(); // For now return empty list //TODO
    }

    @Override
    public long currentTimeMillis() {
        if (curIndex == -1)
            return (long) (index.timeOf(index.getLastMessageOfType(name)) * 1000);
        return (long) (index.timeOf(curIndex) * 1000);
    }

    @Override
    public IMCMessage nextLogEntry() {

        curIndex = index.getNextMessageOfType(name, curIndex);

        if (curIndex == -1)
            return null;

        return index.getMessage(curIndex);
    }

    @Override
    public IMCMessage firstLogEntry() {
        curIndex = index.getFirstMessageOfType(name);
        return index.getMessage(curIndex);
    }

    @Override
    public void advance(long millis) {
        if (curIndex == -1)
            return;
        double before = currentTimeMillis();

        advanceUntil(currentTimeMillis() + millis);

        if (curIndex != -1 && currentTimeMillis() <= before) {
            // System.err.println("Messages of type "+name+" are not correctly sorted");
            curIndex = index.getNextMessageOfType(name, curIndex + 1);
        }
    }

    @Override
    public IMCMessage getCurrentEntry() {
        if (curIndex == -1)
            return null;
        return index.getMessage(curIndex);
    }

    @Override
    public Collection<IMCMessage> getExactTimeEntries(long timeStampMillis) {

        advanceUntil(timeStampMillis);

        Vector<IMCMessage> messages = new Vector<IMCMessage>();

        while (currentTimeMillis() == timeStampMillis)
            messages.add(getCurrentEntry());

        return messages;
    }

    private int numberOfEntries = -1;

    @Override
    public int getNumberOfEntries() {
        if (numberOfEntries == -1) {

            int count = 0;
            int type = index.getDefinitions().getMessageId(name);
            for (int i = index.getFirstMessageOfType(type); i != -1; i = index.getNextMessageOfType(type, i)) {

                count++;
            }
            numberOfEntries = count;
        }
        return numberOfEntries;
    }

    public static void main(String[] args) throws Exception {
        LsfIndex index = new LsfIndex(new File("/home/jqcorreia/Desktop/merge2/Data.lsf"), new IMCDefinition(
                new FileInputStream(new File("/home/jqcorreia/Desktop/merge2/IMC.xml"))));
        LsfMraLog log = new LsfMraLog(index, "EstimatedState");

        NeptusLog.pub().info("<###> "+index.getMessage(0).getTimestamp());
        NeptusLog.pub().info("<###> "+log.getCurrentEntry().getTimestamp());
        NeptusLog.pub().info("<###> "+index.getMessage(1).getTimestamp());
        NeptusLog.pub().info("<###> "+index.getMessage(2).getTimestamp());

        NeptusLog.pub().info("<###> "+index.getMessage(0).getTimestamp());

        for (int i = 0; i < 200; i++) {
            IMCMessage m = index.getMessage(i);
            NeptusLog.pub().info("<###> "+m.getAbbrev() + " " + m.getTimestamp());
        }
        // for(double i = index.timeOf(0); i < index.timeOf(index.getNumberOfMessages()-1); i+=100)
        // {
        // NeptusLog.pub().info("<###> "+i);
        // log.advanceUntil((long)i*1000);
        // NeptusLog.pub().info("<###> "+log.getCurrentEntry().getTimestamp());
        // }
    }
}
