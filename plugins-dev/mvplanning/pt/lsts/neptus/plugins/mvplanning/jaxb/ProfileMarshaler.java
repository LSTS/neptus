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
package pt.lsts.neptus.plugins.mvplanning.jaxb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.MVPlanning;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Payload;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author tsmarques
 *
 */
public class ProfileMarshaler {
    private final String PROFILES_NAMES[] = {"Batimetria"};
    private final String PROFILES_DIR = getProfilesDir();
    
    private Map<String, Profile> allProfiles;
    
    public ProfileMarshaler() {
        allProfiles = unmarshalAll();
    }

    private ArrayList<File> fetchProfilesFiles() {
        ArrayList<File> profilesPaths = new ArrayList<>();
        
        for(String profile : PROFILES_NAMES) {
            String prfPath = FileUtil.getResourceAsFileKeepName(PROFILES_DIR + profile + ".xml");
            
            if(prfPath == null)
                NeptusLog.pub().warn("Profile " + profile + " does not exist");
            else
                profilesPaths.add(new File(prfPath));
        }
        return profilesPaths;
    }

    private String getProfilesDir() {
        return FileUtil.getPackageAsPath(MVPlanning.class) + "/etc/";
    }

    public void addProfile(String id, Profile profile) {
        if(profileIsValid(profile))
            allProfiles.put(id, profile);       
    }

    public boolean profileIsValid(Profile profile) {
        boolean valid = true;
        
        if(profile.getProfileVehicles().isEmpty()) {
            NeptusLog.pub().warn("Profile without vehicles");
            valid = false;
        }
        
        if(profile.getProfileZ() == -1) {
            NeptusLog.pub().warn("Profile altitude not set");
            valid = false;
        }
        
        if(profile.getProfileSpeed() == -1) {
            NeptusLog.pub().warn("Profile Speed not set");
            valid = false;
        }
        
        if(profile.getPayload().isEmpty()) {
            NeptusLog.pub().warn("Profile has no payload");
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

                NeptusLog.pub().info("Marshaling payload of type " + type);
                jaxbMarshaller.marshal(allProfiles.get(type), System.out);
                jaxbMarshaller.marshal(allProfiles.get(type), new File(PROFILES_DIR + type + ".xml"));
            }
            catch (JAXBException e) {
                NeptusLog.pub().warn("Failed payload marshaling");
                e.printStackTrace();
            }
        }
        else
            NeptusLog.pub().warn("No payload profiles of type <" + type + "> to marshal");
    }

    private Map<String, Profile> unmarshalAll() {
        Map<String, Profile> profiles = new HashMap<String, Profile>();
        ArrayList<File> profilesPaths = fetchProfilesFiles();

        for(File file : profilesPaths) {
            String filename = file.getName();
            if(filename.endsWith(".xml")) {
                String type = filename.substring(0, filename.indexOf('.'));
                Profile profile = unmarshal(file);

                profiles.put(type, profile);
            }
        }     
        return profiles;
    }

    /* Reads all available payload of a specific type,
     * from xml files and builds a PayloadProfiles objects */
    public Profile unmarshal(File file) {
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        Profile pProfiles;
        try {
            jaxbContext = JAXBContext.newInstance(Profile.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            pProfiles = (Profile) jaxbUnmarshaller.unmarshal(file);

            NeptusLog.pub().info("[" + pProfiles.getId() + "] : " + pProfiles.getProfileSpeed() + " " + pProfiles.getSpeedUnits() + " | " + pProfiles.getProfileZ() + " " + pProfiles.getZUnits());

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
        ProfileMarshaler marsh = new ProfileMarshaler();
        
        Payload pld1 = new Payload("sidescan");
        pld1.addPayloadParamater("Frequency", "500");
        pld1.addPayloadParamater("BLA BLA", "20");
        
        Profile prf1 = new Profile("Low scan");
        prf1.setProfileZ(1000);
        prf1.setProfileSpeed(35);
        prf1.addPayload(pld1);
        prf1.addVehicle("lauv-noptilus-1");
        prf1.addVehicle("lauv-noptilus-2");
        
        marsh.addProfile("Low scan", prf1);
        marsh.marshal("Low scan");
    }
}
