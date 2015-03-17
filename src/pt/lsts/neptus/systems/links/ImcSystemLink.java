/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Hugo Dias
 * Oct 31, 2012
 */
package pt.lsts.neptus.systems.links;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;

/**
 * @author Hugo
 * 
 */
public class ImcSystemLink extends SystemLink {
    private long owner;
    private Map<String, URI> services = new HashMap<String, URI>();
    
    public ImcSystemLink() {

    }

    public ImcSystemLink update(IMCMessage msg) {
        if (msg instanceof Announce) {
            processAnnounce((Announce) msg);
        }
        if (msg instanceof EstimatedState) {
            // processEstimatedState
        }
        return this;
    }

    private void processAnnounce(Announce msg) {
        owner = msg.getOwner();
        String announceServices = msg.getServices();
        String[] servicesList = announceServices.split(",");
        for (String service : servicesList) {
            URI uri = URI.create(service);
            services.put(uri.getScheme(), uri);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ImcSystemLink [owner=" + owner + ", services=" + services.toString() + "]";
    }
}
