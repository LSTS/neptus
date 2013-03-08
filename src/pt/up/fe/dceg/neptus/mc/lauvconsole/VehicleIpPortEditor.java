/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: VehicleIpPortEditor.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.mc.lauvconsole;

/**
 * @author ZP
 */

import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.dom4j.Document;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.types.comm.CommMean;
import pt.up.fe.dceg.neptus.types.comm.protocol.IMCArgs;
import pt.up.fe.dceg.neptus.types.comm.protocol.ProtocolArgs;
import pt.up.fe.dceg.neptus.types.vehicle.TemplateFileVehicle;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.FileUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
                            new Integer(nArgs.getPort()), true);
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
                else if (prop.getName().equals("user name")) {
                    cm.setUserName((String) prop.getValue());
                }
                else if (prop.getName().equals("is password saved")) {
                    cm.setPasswordSaved((Boolean) prop.getValue());
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
