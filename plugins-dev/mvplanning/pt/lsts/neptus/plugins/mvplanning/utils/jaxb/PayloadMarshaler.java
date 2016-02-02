/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 26 Jan 2016
 */
package pt.lsts.neptus.plugins.mvplanning.utils.jaxb;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import pt.lsts.neptus.plugins.mvplanning.utils.PayloadProfile;

/**
 * @author tsmarques
 *
 */
public class PayloadMarshaler {
    private static final String PAYLOADS_DIR = System.getProperty("user.dir") + "/plugins-dev/mvplanning/etc/";
    private Map<String, PayloadProfiles> allProfiles;
    
    public PayloadMarshaler() {
        allProfiles = unmarshalAll();
    }
    
    public void addProfile(String type, PayloadProfile payload) {
        if(payload.getPayloadVehicles().isEmpty())
            System.out.println("[mvplanning/PayloadMarshaler: #Error#, can't add profile without vehicles!");
        else {
            if(!allProfiles.containsKey(type)) {
                PayloadProfiles prf = new PayloadProfiles(type);
                prf.addProfile(payload);
                allProfiles.put(type, prf);
            }
            else {
                PayloadProfiles prf = allProfiles.get(type);
                int duplicatePos = checkDuplicateProfile(payload, prf);

                /* not duplicate */
                if(duplicatePos == -1)
                    allProfiles.get(type).addProfile(payload);
                else
                    mergeProfiles(payload, prf, duplicatePos);
            }
        }
    }
    
    /* Profiles are considered duplicates, if they apply to the same vehicles
     * If they are duplicates, return position of the original one */
    private int checkDuplicateProfile(PayloadProfile p, PayloadProfiles profiles) {
        int pos = 0;
        for(PayloadProfile pld : profiles.getProfiles()) {
            if(pld.getPayloadVehicles().containsAll(p.getPayloadVehicles()))
                return pos;
            pos++;           
        }
        return -1;
    }
    
    /* If two profiles are duplicates, check if the 'new one' has new/different 
     * parameters. If yes, merge them in the original one */
    private void mergeProfiles(PayloadProfile newProfile, PayloadProfiles profiles, int origPos) {
        PayloadProfile origProfile = profiles.getProfiles().get(origPos);
        origProfile.getPayloadParameters().putAll(newProfile.getPayloadParameters());
    }
    
    public Map<String, PayloadProfiles> getAllProfiles() {
        return this.allProfiles;
    }
      
   
    public void marshal(String type) {
        if(allProfiles.containsKey(type)) {
            JAXBContext jaxbContext;        
            try {
                jaxbContext = JAXBContext.newInstance(PayloadProfiles.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

                System.out.println("[mvplanning/PayloadMarshaler] Marshaling payload of type " + type);
                jaxbMarshaller.marshal(allProfiles.get(type), System.out);
                jaxbMarshaller.marshal(allProfiles.get(type), new File(PAYLOADS_DIR + type + ".xml"));
            }
            catch (JAXBException e) {
                System.out.println("[mvplanning/PayloadMarshaler] #Error# while marshaling payload");
                e.printStackTrace();
            }
        }
        else
            System.out.println("[mvplanning/PayloadMarshaler] No payload profiles of type <" 
        + type + "> to marshal");
    }
    
    private Map<String, PayloadProfiles> unmarshalAll() {
        Map<String, PayloadProfiles> profiles = new HashMap<String, PayloadProfiles>();
        File directory = new File(PAYLOADS_DIR);
        
        for(File file : directory.listFiles()) {
            String filename = file.getName();
            if(filename.endsWith(".xml")) {
                String type = filename.substring(0, filename.indexOf('.'));
                PayloadProfiles profile = unmarshal(type);
                
                profiles.put(type, profile);
            }
        }
        
        return profiles;
    }
            
    /* Reads all available payload of a specific type,
     * from xml files and builds a PayloadProfiles objects */
    public PayloadProfiles unmarshal(String type) {
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        PayloadProfiles pProfiles;
        try {
            jaxbContext = JAXBContext.newInstance(PayloadProfiles.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            pProfiles = (PayloadProfiles) jaxbUnmarshaller.unmarshal(new File(PAYLOADS_DIR + type + ".xml"));
            
            return pProfiles;
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
        
        // TODO: improve
        return new PayloadProfiles();
    }
           
    /* Use to add new payload or testing */
    public static void main(String[] args) {
        PayloadProfile pld = new PayloadProfile("Sidescan");
        pld.addPayloadParamater("Range", "500");
        pld.addPayloadParamater("Frequency", "20");
        pld.addPayloadParamater("Altitude", "100");
        pld.addPayloadVehicle("lauv-seacon-1");
        pld.addPayloadVehicle("lauv-seacon-3");
        pld.addPayloadVehicle("laux-seacon-2");
        
        PayloadProfile pld2 = new PayloadProfile("Sidescan");
        pld2.addPayloadParamater("Range", "200");
        pld2.addPayloadParamater("Frequency", "20");
        pld2.addPayloadParamater("Altitude", "100");
        pld2.addPayloadParamater("SKIJ", "1.00");
        pld2.addPayloadVehicle("x8-01");
        
//        Payload.marshalPayload(pld);
//        
        PayloadMarshaler pldM = new PayloadMarshaler();
        pldM.addProfile("sidescan", pld);
        pldM.addProfile("sidescan", pld2);
//        PayloadProfile pld3 = new PayloadProfile("sidescan");
//        pld3.addPayloadVehicle("x8-01");
//        pld3.addPayloadVehicle("lauv-seacon3");
//        pld3.addPayloadParamater("Frequency", "2000");
//        pld3.addPayloadParamater("Range", "100");
//        pldM.addProfile("sidescan", pld3);
//////        pldM.addProfile("sidescan", pld);
//////        pldM.addProfile("sidescan", pld2);
        pldM.marshal("sidescan");
        
//        pldM.unmarshalAll();
    }
}
