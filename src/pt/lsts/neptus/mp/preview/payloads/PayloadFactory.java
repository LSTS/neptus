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
 * Aug 7, 2013
 */
package pt.lsts.neptus.mp.preview.payloads;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;

/**
 * @author zp
 *
 */
public class PayloadFactory {

    public static Collection<PayloadFingerprint> getPayloads(Maneuver m) {
        Vector<PayloadFingerprint> payloads = new Vector<>();
        IMCMessage[] msgs = m.getStartActions().getAllMessages();

        for (IMCMessage msg : msgs) {
            if (msg instanceof SetEntityParameters)
                payloads.addAll(parse((SetEntityParameters)msg));
        }
        
        return payloads;
    }
    
    public static LinkedHashMap<String, Collection<PayloadFingerprint>> getPayloads(PlanType plan) {
        LinkedHashMap<String, Collection<PayloadFingerprint>> payloads = new LinkedHashMap<>();
        
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            payloads.put(m.getId(), getPayloads(m));
        }
        
        return payloads;
    }
    
    private static Collection<PayloadFingerprint> parse(SetEntityParameters msg) {
        
        Vector<PayloadFingerprint> pf = new Vector<>();
        if (msg.getName().equals("Sidescan")) {
            boolean active = false;
            Vector<PayloadFingerprint> sidescanRanges = new Vector<>();
            for (EntityParameter p : msg.getParams()) {
                if (p.getName().equals("Range") ||
                        p.getName().equals("High-Frequency Range") ||
                        p.getName().equals("Low-Frequency Range")) {
                    sidescanRanges.add(new SidescanFingerprint(Double.parseDouble(p.getValue())));
                }
                else if (p.getName().equals("Active")) {
                    active = p.getValue().equalsIgnoreCase("true");
                }
            }
            if (active)
                pf.addAll(sidescanRanges);
        }
        else if (msg.getName().equals("Multibeam")) {
            double range = 0;
            boolean active = false;
            for (EntityParameter p : msg.getParams()) {
                if (p.getName().equals("Active")) {
                    active = p.getValue().equalsIgnoreCase("true");
                }
                else if (p.getName().equals("Range")) {
                    range = Double.parseDouble(p.getValue());
                }
            }
            if (active) {
                pf.add(new MultibeamFingerprint(range, Math.toRadians(60)));
            }
        }
        return pf;
    }    
}
