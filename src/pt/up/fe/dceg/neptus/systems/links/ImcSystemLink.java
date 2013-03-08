/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 31, 2012
 * $Id:: ImcSystemLink.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.systems.links;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.dceg.neptus.imc.Announce;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;

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
