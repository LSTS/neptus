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
 * Author: José Correia
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.importers.jsf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Collection;
import java.util.LinkedHashMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.neptus.mra.importers.IMraLog;

/**
 * @author jqcorreia
 *
 */
public class JsfLog implements IMraLog {

    private MappedByteBuffer buffer;
    private FileInputStream fis;
    
    public JsfLog(String fileName) {
        File f = new File(fileName);
        try {
            fis = new FileInputStream(f);
            buffer = fis.getChannel().map(MapMode.READ_ONLY, 0, f.length());
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

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        fis.close();        
    }
}
