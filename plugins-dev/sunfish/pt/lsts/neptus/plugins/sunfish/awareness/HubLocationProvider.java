/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 24, 2014
 */
package pt.lsts.neptus.plugins.sunfish.awareness;

import java.io.InputStreamReader;
import java.net.URL;

import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger;
import pt.lsts.neptus.comm.iridium.HubIridiumMessenger.HubSystemMsg;
import pt.lsts.neptus.plugins.update.Periodic;

import com.google.gson.Gson;


/**
 * @author zp
 *
 */
public class HubLocationProvider implements ILocationProvider {

    SituationAwareness parent;
    private String systemsUrl = "http://hub.lsts.pt/api/v1/systems/active";
    

    @Override
    public void onInit(SituationAwareness instance) {
        this.parent = instance;
    }

    @Periodic(millisBetweenUpdates=1000*60)
    public void pollActiveSystems() throws Exception {
        if (!enabled)
            return;
        Gson gson = new Gson();
        URL url = new URL(systemsUrl);        
        HubSystemMsg[] msgs = gson.fromJson(new InputStreamReader(url.openStream()), HubSystemMsg[].class);

        for (HubSystemMsg m : msgs) {
            AssetPosition pos = new AssetPosition(m.name, m.coordinates[0],
                    m.coordinates[1]);
            pos.setType(IMCUtils.getSystemType(m.imcid));
            pos.setTimestamp(HubIridiumMessenger.stringToDate(m.updated_at).getTime());
            pos.setSource(getName());
            parent.addAssetPosition(pos);
        }

    }
    @Override
    public String getName() {
        return "HUB (Active Systems API)";
    }

    @Override
    public void onCleanup() {        

    }

    private boolean enabled = false;
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
