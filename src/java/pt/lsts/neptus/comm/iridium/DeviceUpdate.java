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
 * Jun 28, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class DeviceUpdate extends IridiumMessage {

    protected LinkedHashMap<Integer, Position> positions = new LinkedHashMap<>();

    /**
     * @return the positions
     */
    public LinkedHashMap<Integer, Position> getPositions() {
        return positions;
    }

    public DeviceUpdate() {
        super(2001);
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        int read = 0;
        for (Position p : positions.values()) {
            out.writeUnsignedShort(p.id);
            read+=2;
            out.writeUnsignedInt(Math.round(p.timestamp));
            read+=4;
            out.writeInt((int)Math.round(Math.toDegrees(p.latRads) * 1000000.0));
            read+=4;
            out.writeInt((int)Math.round(Math.toDegrees(p.lonRads) * 1000000.0));
            read+=4;            
        }
        return read;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        positions.clear();
        int read = 0;
        while (true) {
            try {
                Position pos = new Position();
                pos.id = in.readUnsignedShort();
                read+=2;
                pos.timestamp = in.readUnsignedInt();
                read+=4;
                pos.latRads = Math.toRadians(in.readInt() / 1000000.0);
                read+=4;
                pos.lonRads = Math.toRadians(in.readInt() / 1000000.0);
                read+=4;
                positions.put(pos.id, pos);
            }
            catch (Exception e) {
                break;
            }
        }
        return read;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        Vector<IMCMessage> msgs = new Vector<>();

        for (Position pos : positions.values()) {
            RemoteSensorInfo sensorInfo = new RemoteSensorInfo();
            sensorInfo.setLat(pos.latRads);
            sensorInfo.setLon(pos.lonRads);
            sensorInfo.setTimestamp(pos.timestamp);
            sensorInfo.setAlt(0);
            sensorInfo.setId(IMCDefinition.getInstance().getResolver().resolve(pos.id));
            sensorInfo.setSrc(getSource());
            sensorInfo.setDst(getDestination());
            sensorInfo.setSensorClass(IMCUtils.getSystemType(pos.id));
            msgs.add(sensorInfo);
        }
        return msgs;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        s +=  "\t["+positions.size()+" positions]\n";
        for (Position p : positions.values()) {
            s += "\t * ("+IMCDefinition.getInstance().getResolver().resolve(p.id)+") --> "+new LocationType(Math.toDegrees(p.latRads), Math.toDegrees(p.lonRads))+"\n";
        }
        return s;
    }

}
