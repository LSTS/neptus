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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.mc.lauvconsole;

/**
 * @author ZP
 */

import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.dom4j.Document;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.types.comm.CommMean;
import pt.lsts.neptus.types.comm.protocol.IMCArgs;
import pt.lsts.neptus.types.comm.protocol.ProtocolArgs;
import pt.lsts.neptus.types.vehicle.TemplateFileVehicle;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.FileUtil;

public class VehicleIpPortEditor implements PropertiesProvider {
    private VehicleType vehicle;

    public VehicleIpPortEditor(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    // Properties to edit
    public DefaultProperty[] getProperties() {
        LinkedList<DefaultProperty> propertiesList = new LinkedList<DefaultProperty>();

        for (CommMean cm : vehicle.getCommunicationMeans().values()) {
            String category = cm.getName();
            DefaultProperty comP = PropertiesEditor.getPropertyInstance("host name", category, String.class,
                    new String(cm.getHostAddress()), true);
            comP.setShortDescription("Set the Vehicle's IP address.");
            propertiesList.add(comP);

            for (ProtocolArgs pArgs : cm.getProtocolsArgs().values()) {
                if (pArgs instanceof IMCArgs) {
                    IMCArgs nArgs = (IMCArgs) pArgs;
                    comP = PropertiesEditor.getPropertyInstance("imc.port", category, Integer.class,
                            Integer.valueOf(nArgs.getPort()), true);
                    comP.setShortDescription("Set the Vehicle's port.");
                    propertiesList.add(comP);
                }
            }
        }
        DefaultProperty[] prop = new DefaultProperty[propertiesList.size()];
        return propertiesList.toArray(prop);
    }

    public void setProperties(Property[] properties) {
        LinkedHashMap<String, String> transFilesList = new LinkedHashMap<String, String>();
        for (String id : vehicle.getTransformationXSLTTemplates().keySet()) {
            TemplateFileVehicle tfile = vehicle.getTransformationXSLTTemplates().get(id);
            String category = id + ":" + tfile.getName() + " parameters";
            transFilesList.put(category, id);
        }

        for (Property prop : properties) {
            String cat = prop.getCategory();
            CommMean cm = vehicle.getCommunicationMeans().get(cat);
            if (cm != null) {
                if (prop.getName().equals("host name")) {
                    cm.setHostAddress((String) prop.getValue());
                }
                else {
                    for (String protocol : cm.getProtocols()) {
                        if (prop.getName().startsWith(protocol)) {
                            if (protocol.equalsIgnoreCase(CommMean.IMC)) {
                                ProtocolArgs protoArgs = cm.getProtocolsArgs().get(protocol);
                                if (protoArgs != null) {
                                    ((IMCArgs) protoArgs).setPort((Integer) prop.getValue());
                                }
                            }
                        }
                    }
                }
            }
            // Look for XSLTs parameters
            String idTFile = transFilesList.get(cat);
            if (idTFile != null) {
                TemplateFileVehicle tFile = vehicle.getTransformationXSLTTemplates().get(idTFile);
                tFile.getParametersToPassList().put(prop.getName(), (String) prop.getValue());
            }
        }

        String filePath = vehicle.getOriginalFilePath();
        Document doc = vehicle.asDocument();
        String dataToSave = FileUtil.getAsPrettyPrintFormatedXMLString(doc);
        FileUtil.saveToFile(filePath, dataToSave);
    }

    public String getPropertiesDialogTitle() {
        return "Vehicle properties";
    }

    public String[] getPropertiesErrors(Property[] properties) {
        return null;
    }

}
