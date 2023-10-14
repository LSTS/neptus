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
 * Author: Paulo Dias
 * 7 de Jul de 2011
 */
package pt.lsts.neptus.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pt.lsts.neptus.NeptusLog;

/**
 * @author pdias
 * 
 */
public class XMLUtil {
    
    /**
     * @param isXML
     * @param schemaFile
     * @return
     * @throws ParserConfigurationException
     */
    public static String[] validate(InputStream isXML, File schemaFile)
            throws ParserConfigurationException {
        return validate(isXML, getAsSchema(schemaFile));
    }

    /**
     * @param xmlStr
     * @param schemaFile
     * @return
     * @throws ParserConfigurationException
     */
    public static String[] validate(String xmlStr, File schemaFile)
            throws ParserConfigurationException {
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlStr.getBytes());
        return validate(bais, getAsSchema(schemaFile));
    }

    /**
     * @param isXML
     * @param schema
     * @return
     * @throws ParserConfigurationException
     */
    public static String[] validate(InputStream isXML, Schema schema)
            throws ParserConfigurationException {
        String[] vmsgs = validateXMLWorker(isXML, schema);
        try {
            isXML.reset();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        if (vmsgs.length != 0) {
            String strMsg = "Invalid XML!\n";
            for (String str : vmsgs)
                strMsg += "\n" + str;
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + 
                    "XML Validator: " + strMsg);
            return vmsgs;
        }
        return new String[0];
    }
    
    /**
     * 
     */
    public static Schema getAsSchema(File schemaFile) {
      SchemaFactory sm = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      try {
          Schema schema = sm.newSchema(schemaFile);
          return schema;
      } catch (Exception e) {
          NeptusLog.pub().warn(ReflectionUtil.getCallerStamp() + e.getMessage());
          return null;
      }
    }
    
    private static String[] validateXMLWorker(InputStream is, Schema schema) {
        final Vector<String> validationMsgs = new Vector<String>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                validationMsgs.add("WARNING: " + exception.getMessage());
            }
            @Override
            public void error(SAXParseException exception) throws SAXException {
                validationMsgs.add("ERROR: " + exception.getMessage());
            }
            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                validationMsgs.add("FATAL: " + exception.getMessage());
            }
        });
        try {
            validator.validate(new StreamSource(is));
        } catch (Exception e) {
            validationMsgs.add("SOURCE: " + e.getMessage());
        }
        return validationMsgs.toArray(new String[validationMsgs.size()]);
    }

    /**
     * @param xmlStr
     * @return
     */
    public static String getAsPrettyPrintFormatedXMLString(String xmlStr) {
        String tmp1 = FileUtil.getAsPrettyPrintFormatedXMLString("<AAzZ>"+xmlStr+"</AAzZ>", true);
        return tmp1.replaceAll("^[[\\s]*?]?<AAzZ>", "")
                    .replaceAll("[[\\s]]*?]?</AAzZ>$", "")
                    .replaceAll("(?m)^  ", "").replaceAll("^\\n", "");
        //return FileUtil.getAsPrettyPrintFormatedXMLString(xmlStr, true);
    }

    /**
     * @param tmpStr
     * @return
     */
    public static String getAsCompactFormatedXMLString(String tmpStr) {
        String tmp1 = FileUtil.getAsCompactFormatedXMLString("<AAzZ>"+tmpStr+"</AAzZ>", true);
        return tmp1.replaceAll("^<AAzZ>", "").replaceAll("</AAzZ>$", "");
        //return FileUtil.getAsCompactFormatedXMLString(tmpStr, true);
    }

    /**
     * @param node
     * @return
     */
    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        }
        catch (TransformerException te) {
            NeptusLog.pub().info("<###>nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    /**
     * @param node
     */
    public static String nodeChildsToString(Node node) {
        String nodeName = node.getNodeName();
        String ret = nodeToString(node);
        return ret.replaceAll("^<" + nodeName + ">", "").replaceAll("</" + nodeName + ">$", "");
    }

    /**
     * @param xmlStr
     * @return the Document or null upon error.
     */
    public static Document createDocumentFromXmlString(String xmlStr) {
        try {
            DocumentBuilderFactory docBuilderFactory;
            docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setNamespaceAware(false);
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(xmlStr.getBytes());
            Document doc = builder.parse(bais);
            return doc;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Document createEmptyDocument() {
        try {
            DocumentBuilderFactory docBuilderFactory;
            docBuilderFactory = DocumentBuilderFactory.newInstance();
            docBuilderFactory.setIgnoringComments(true);
            docBuilderFactory.setIgnoringElementContentWhitespace(true);
            docBuilderFactory.setNamespaceAware(false);
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            Document doc = builder.newDocument();
            return doc;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String test = "<Profile name=\"P1\">\n<Component id=\"1\"/>\n</Profile>\n" +
                "<Profile name=\"P2\">\n<Component id=\"2\"/>\n</Profile>";
        NeptusLog.pub().info("<###>'"+getAsCompactFormatedXMLString(test)+"'");
        NeptusLog.pub().info("<###>'"+getAsPrettyPrintFormatedXMLString(test)+"'");
        
        Document doc = createDocumentFromXmlString("<root>fd" + test + "</root>");
        NeptusLog.pub().info("<###> "+nodeToString(doc.getDocumentElement()));
        NeptusLog.pub().info("<###> "+nodeChildsToString(doc.getDocumentElement()));
    }
}
