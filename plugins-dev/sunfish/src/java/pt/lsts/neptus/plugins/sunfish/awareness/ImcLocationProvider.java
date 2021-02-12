/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.Announce;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.RemoteSensorInfo;

/**
 * @author zp
 * 
 */
public class ImcLocationProvider implements ILocationProvider {

    private SituationAwareness instance;
    private boolean enabled = false;
    @Override
    public void onInit(SituationAwareness instance) {
        this.instance = instance;
        instance.getConsole().getImcMsgManager().registerBusListener(this);
    }

    @Override
    public void onCleanup() {
        instance.getConsole().getImcMsgManager().unregisterBusListener(this);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    

    @Subscribe
    public void on(Announce announce) {
        if (!enabled)
            return;
        AssetPosition pos = new AssetPosition(announce.getSysName(), Math.toDegrees(announce.getLat()),
                Math.toDegrees(announce.getLon()));
        pos.setSource(getName());
        switch (announce.getSysType()) {
            case STATICSENSOR:
            case MOBILESENSOR:
            case HUMANSENSOR:
                pos.setType("Sensor");
                break;
            default:
                pos.setType(announce.getSysType().toString());
                break;
        }
        pos.setSource("IMC (Announce)");
        instance.addAssetPosition(pos);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.sunfish.awareness.ILocationProvider#getName()
     */
    @Override
    public String getName() {
        return "IMC messages";
    }
    
    @Subscribe
    public void on(RemoteSensorInfo sensor) {
        try {
            if (!enabled)
                return;
            AssetPosition pos = new AssetPosition(sensor.getId(), Math.toDegrees(sensor.getLat()),
                    Math.toDegrees(sensor.getLon()));
            pos.setTimestamp(sensor.getTimestampMillis());
            pos.setType(sensor.getSensorClass());
            if ("ais-1".equals(IMCDefinition.getInstance().getResolver().resolve(sensor.getSrc()))) {
                pos.setType("Ship");
                pos.setSource("AIS Receiver");
                if (!sensor.getSensorClass().isEmpty()) {
                    pos.putExtra("Ship Type", sensor.getSensorClass());
                }
            }
            instance.addAssetPosition(pos);
        }
        catch (Exception e) {
            e.printStackTrace();
            
        }
    }
    
    
}
