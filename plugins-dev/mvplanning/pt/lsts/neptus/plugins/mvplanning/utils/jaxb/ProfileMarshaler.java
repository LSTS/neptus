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

import pt.lsts.neptus.plugins.mvplanning.utils.Payload;

/**
 * @author tsmarques
 *
 */
public class ProfileMarshaler {
    private static final String PAYLOADS_DIR = System.getProperty("user.dir") + "/plugins-dev/mvplanning/etc/";
    private Map<String, Profile> allProfiles;
    
    public ProfileMarshaler() {
        allProfiles = unmarshalAll();
    }
    
    public void addProfile(String id, Profile profile) {
        if(profileIsValid(profile))
            allProfiles.put(id, profile);
    }
    
    public boolean profileIsValid(Profile profile) {
        boolean valid = true;
        
        if(profile.getProfileVehicles().isEmpty()) {
            System.out.println("[mvplanning/PayloadMarshaler]: ERROR profile without vehicles");
            valid = false;
        }
        
        if(profile.getProfileAltitude() == -1) {
            System.out.println("[mvplanning/PayloadMarshaler]: ERROR profile altitude not set");
            valid = false;
        }
        
        if(profile.getProfileVelocity() == -1) {
            System.out.println("[mvplanning/PayloadMarshaler]: ERROR profile velocity not set");
            valid = false;
        }
        
        if(profile.getPayload().isEmpty()) {
            System.out.println("[mvplanning/PayloadMarshaler]: ERROR profile has not payload");
            valid = false;
        }
        
        return valid;
    }
    
    
    public Map<String, Profile> getAllProfiles() {
        return this.allProfiles;
    }
      
   
    public void marshal(String type) {
        if(allProfiles.containsKey(type)) {
            JAXBContext jaxbContext;        
            try {
                jaxbContext = JAXBContext.newInstance(Profile.class);
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
    
    private Map<String, Profile> unmarshalAll() {
        Map<String, Profile> profiles = new HashMap<String, Profile>();
        File directory = new File(PAYLOADS_DIR);
        
        for(File file : directory.listFiles()) {
            String filename = file.getName();
            if(filename.endsWith(".xml")) {
                String type = filename.substring(0, filename.indexOf('.'));
                Profile profile = unmarshal(type);
                
                profiles.put(type, profile);
            }
        }
        
        return profiles;
    }
            
    /* Reads all available payload of a specific type,
     * from xml files and builds a PayloadProfiles objects */
    public Profile unmarshal(String type) {
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        Profile pProfiles;
        try {
            jaxbContext = JAXBContext.newInstance(Profile.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            pProfiles = (Profile) jaxbUnmarshaller.unmarshal(new File(PAYLOADS_DIR + type + ".xml"));
            
            return pProfiles;
        }
        catch (JAXBException e) {
            e.printStackTrace();
        }
        
        // TODO: improve
        return new Profile();
    }
           
    /* Use to add new payload or testing */
    public static void main(String[] args) {
    }
}
