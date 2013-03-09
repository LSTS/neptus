/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 5, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCMessageType;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;

/**
 * @author jqcorreia
 *
 */
public class JsfLog implements IMraLog {

    MappedByteBuffer buffer;
    
    public JsfLog(String fileName) {
        File f = new File(fileName);
        try {
            buffer = new FileInputStream(f).getChannel().map(MapMode.READ_ONLY, 0, f.length());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public IMCMessage getEntryAtOrAfter(long timestamp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMCMessage getEntryAtOrAfter(long timestamp, String entityName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String name() {
        return "JSF Log";
    }

    @Override
    public IMCMessage getLastEntry() {
        return null;
    }

    @Override
    public IMCMessageType format() {
        return null;
    }

    @Override
    public LinkedHashMap<String, Object> metaInfo() {
        return null;
    }

    @Override
    public long currentTimeMillis() {
        return 0;    
    }

    @Override
    public IMCMessage nextLogEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMCMessage firstLogEntry() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void advance(long millis) {
        // TODO Auto-generated method stub

    }

    @Override
    public IMCMessage getCurrentEntry() {
        
        return null;
    }

    @Override
    public Collection<IMCMessage> getExactTimeEntries(long timeStampMillis) {
        return null;
    }

    @Override
    public int getNumberOfEntries() {
        // TODO Auto-generated method stub
        return 0;
    }

}
