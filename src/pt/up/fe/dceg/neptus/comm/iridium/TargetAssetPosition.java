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
 * Jul 1, 2013
 */
package pt.up.fe.dceg.neptus.comm.iridium;

import java.util.Collection;
import java.util.Vector;

import pt.up.fe.dceg.neptus.imc.IMCInputStream;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.RemoteSensorInfo;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author zp
 *
 */
public class TargetAssetPosition extends IridiumMessage {

    LocationType loc = new LocationType();
    public int asset_imc_id;

    public TargetAssetPosition() {
        super(2007);
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.writeUnsignedShort(asset_imc_id);
        loc.convertToAbsoluteLatLonDepth();
        out.writeUnsignedInt(Math.round(loc.getLatitudeAsDoubleValue()* 1000000));
        out.writeUnsignedInt(Math.round(loc.getLongitudeAsDoubleValue()* 1000000));
        return 10;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        asset_imc_id = in.readUnsignedShort();
        loc = new LocationType();
        loc.setLatitude(in.readUnsignedInt() / 1000000.0);
        loc.setLongitude(in.readUnsignedInt() / 1000000.0);
        return 10;
    }

    public final LocationType getLocation() {
        return loc;
    }

    public final void setLocation(LocationType loc) {
        this.loc = loc;
    }

    public final int getAssetImcId() {
        return asset_imc_id;
    }

    public final void setAssetImcId(int asset_imc_id) {
        this.asset_imc_id = asset_imc_id;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        Vector<IMCMessage> msgs = new Vector<>();

        RemoteSensorInfo sensorInfo = new RemoteSensorInfo();
        sensorInfo.setLat(getLocation().getLatitudeAsDoubleValueRads());
        sensorInfo.setLon(getLocation().getLongitudeAsDoubleValueRads());
        sensorInfo.setAlt(0);
        sensorInfo.setId("TP_"+ImcSystemsHolder.translateImcIdToSystemName(asset_imc_id).replaceAll(":", ""));
        sensorInfo.setSrc(getSource());
        sensorInfo.setDst(getDestination());
        msgs.add(sensorInfo);        

        return msgs;
    }
}
