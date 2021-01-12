/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 16/Jan/2005
 */
package pt.lsts.neptus.types.vehicle;

import java.util.LinkedHashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.misc.FileType;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author Paulo
 * 
 */
public class TemplateFileVehicle extends FileType {
    protected String parametersToPass = "";
    protected String outputFileName = "";

    protected LinkedHashMap<String, String> parametersToPassList = new LinkedHashMap<String, String>();

    /**
     * 
     */
    public TemplateFileVehicle() {
        super();
        this.setType("xslt");
    }

    /**
     * @param xml
     */
    public TemplateFileVehicle(String xml) {
        // super(xml);
        load(xml);
        this.setType("xslt");
    }

    /**
     * @see pt.lsts.neptus.types.misc.FileType#load(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public boolean load(String xml) {
        if (!super.load(xml))
            return false;

        try {
            // doc = DocumentHelper.parseText(xml);
            Node nd = doc.selectSingleNode("/file/parameters-to-pass");
            if (nd != null) {
                setParametersToPass(""); // nd.asXML());
                List<Node> paramNodesList = nd.selectNodes("./param");
                for (Node param : paramNodesList) {
                    String name = param.selectSingleNode("name").getText();
                    String value = param.selectSingleNode("value").getText();
                    parametersToPassList.put(name, value);
                    setParametersToPass(getParametersToPass() + " " + name + "=" + value);
                }
            }

            this.setOutputFileName(doc.selectSingleNode("/file/output-file-name").getText());
        }
        catch (Exception e) {
            NeptusLog.pub().error(this, e);
            return false;
        }

        return true;
    }

    /**
     * @return Returns the parametersToPass.
     */
    public String getParametersToPass() {
        return parametersToPass;
    }

    /**
     * @param parametersToPass The parametersToPass to set.
     */
    public void setParametersToPass(String parametersToPass) {
        this.parametersToPass = parametersToPass;
    }

    /**
     * @return
     */
    public LinkedHashMap<String, String> getParametersToPassList() {
        return parametersToPassList;
    }

    /**
     * @return Returns the outputFileName.
     */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
     * @param outputFileName The outputFileName to set.
     */
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML()
     */
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asXML(java.lang.String)
     */
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.types.XmlOutputMethods#asDocument(java.lang.String)
     */
    public Document asDocument(String rootElementName) {
        Document document = super.asDocument(rootElementName);
        Element root = document.getRootElement();

        if (!parametersToPass.equalsIgnoreCase("")) {
            // root.addElement( "parameters-to-pass" ).addText(getParametersToPass());
            Element paramToPass = root.addElement("parameters-to-pass");
            for (String key : getParametersToPassList().keySet()) {
                String value = getParametersToPassList().get(key);
                Element paramT = paramToPass.addElement("param");
                paramT.addElement("name").addText(key);
                paramT.addElement("value").addText(value);
            }

        }
        if ("".equals(originalFilePath))
            root.addElement("output-file-name").addText(getOutputFileName());
        else
            root.addElement("output-file-name").addText(
                    FileUtil.relativizeFilePathAsURI(originalFilePath, this.getOutputFileName()));

        return document;
    }

}
