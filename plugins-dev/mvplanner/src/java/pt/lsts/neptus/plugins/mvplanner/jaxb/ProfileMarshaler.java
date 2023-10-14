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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm
 * 27 Jan 2017
 */

package pt.lsts.neptus.plugins.mvplanner.jaxb;

/**
 * @author tsmarques
 * @date 1/29/17
 */
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tsmarques
 *
 */
public class ProfileMarshaler {
    public final static String PROFILES_DIR = "conf/mvplanner/profiles/";
    private Map<String, Profile> allProfiles;

    public ProfileMarshaler() {
        allProfiles = unmarshalAll();
    }

    private ArrayList<File> fetchProfilesFiles() {
        ArrayList<File> profilesPaths = new ArrayList<>();

        File profilesDir = new File(PROFILES_DIR);
        for(File path : profilesDir.listFiles((d, name) -> name.endsWith(".xml")))
            profilesPaths.add(path);

        return profilesPaths;
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
        Map<String, Profile> profiles = new HashMap<>();
        ArrayList<File> profilesPaths = fetchProfilesFiles();

        for(File file : profilesPaths) {
            String filename = file.getName();
            String type = filename.substring(0, filename.indexOf('.'));
            Profile profile = unmarshal(file);

            if(profile == null) {
                NeptusLog.pub().error("Couldn't unrmarshal profile at " + filename);
                continue;
            }

            profiles.put(type, profile);
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

            return pProfiles;
        }
        catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* Use to add new payload or testing */
    public static void main(String[] args) {
        ProfileMarshaler marsh = new ProfileMarshaler();

        Payload pld1 = new Payload("Sidescan");
        pld1.addPayloadParamater("Active", "True");
        pld1.addPayloadParamater("Frequency", "770");
        pld1.addPayloadParamater("Range", "30");

        Profile prf1 = new Profile("Bathymetry");
        prf1.setProfileZ(3);
        prf1.setProfileSpeed(1);
        prf1.setSpeedUnits("m/s");
        prf1.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        prf1.addPayload(pld1);
        prf1.addVehicle("lauv-noptilus-1");
        prf1.addVehicle("lauv-noptilus-2");
        prf1.addVehicle("lauv-noptilus-4");

        marsh.addProfile(prf1.getId(), prf1);
        marsh.marshal(prf1.getId());
    }
}