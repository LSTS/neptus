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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jun 28, 2013
 */
package pt.up.fe.dceg.neptus.comm.iridium;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCInputStream;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.RemoteSensorInfo;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
public class DeviceUpdate extends IridiumMessage {

    protected LinkedHashMap<Integer, Position> positions = new LinkedHashMap<>();

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        int read = 0;
        for (Position p : positions.values()) {
            out.writeUnsignedShort(p.id);
            read+=2;
            out.writeUnsignedInt(Math.round(p.timestamp));
            read+=4;
            out.writeUnsignedInt(Math.round(Math.toDegrees(p.latitude) * 1000000.0));
            read+=4;
            out.writeUnsignedInt(Math.round(Math.toDegrees(p.longitude) * 1000000.0));
            read+=4;            
        }
        return read;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        positions.clear();
        int read = 0;
        while (in.available() > 14) {
            Position pos = new Position();
            pos.id = in.readUnsignedShort();
            read+=2;
            pos.timestamp = in.readUnsignedInt();
            read+=4;
            pos.latitude = Math.toRadians(in.readUnsignedInt() / 1000000.0);
            read+=4;
            pos.longitude = Math.toRadians(in.readUnsignedInt() / 1000000.0);
            read+=4;
            positions.put(pos.id, pos);
        }
        return read;
    }
    
    @Override
    public Collection<IMCMessage> asImc() {
        Vector<IMCMessage> msgs = new Vector<>();
        
        for (Position pos : positions.values()) {
            RemoteSensorInfo sensorInfo = new RemoteSensorInfo();
            sensorInfo.setLat(pos.latitude);
            sensorInfo.setLon(pos.longitude);
            sensorInfo.setAlt(0);
            sensorInfo.setId(ImcSystemsHolder.translateImcIdToSystemName(pos.id));
            sensorInfo.setSrc(getSource());
            sensorInfo.setDst(getDestination());
            msgs.add(sensorInfo);
        }
        
        return msgs;
    }

    public class Position {
        public int id;
        public double latitude, longitude, timestamp;
    }
}
