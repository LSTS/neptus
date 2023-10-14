/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 1, 2013
 */
package pt.lsts.neptus.comm.iridium;

import java.util.Collection;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.neptus.types.coord.LocationType;

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
        out.writeInt((int)Math.round(loc.getLatitudeDegs()* 1000000));
        out.writeInt((int)Math.round(loc.getLongitudeDegs()* 1000000));
        return 10;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        asset_imc_id = in.readUnsignedShort();
        loc = new LocationType();
        loc.setLatitudeDegs(in.readInt() / 1000000.0);
        loc.setLongitudeDegs(in.readInt() / 1000000.0);
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
        sensorInfo.setLat(getLocation().getLatitudeRads());
        sensorInfo.setLon(getLocation().getLongitudeRads());
        sensorInfo.setSensorClass("Target Position");
        sensorInfo.setAlt(0);
        sensorInfo.setId("TP_"+IMCDefinition.getInstance().getResolver().resolve(asset_imc_id));
        sensorInfo.setSrc(getSource());
        sensorInfo.setDst(getDestination());
        msgs.add(sensorInfo);        

        return msgs;
    }
    
    @Override
    public String toString() {
        String s = super.toString();
        return s + "\tAsset: "+IMCDefinition.getInstance().getResolver().resolve(getAssetImcId())+"\n" + 
        "\tLocation: "+getLocation()+"\n";
    }
}
