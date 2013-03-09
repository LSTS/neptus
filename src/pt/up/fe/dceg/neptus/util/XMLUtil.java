/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 7 de Jul de 2011
 */
package pt.up.fe.dceg.neptus.util;

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

import pt.up.fe.dceg.neptus.NeptusLog;

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
            System.out.println("nodeToString Transformer Exception");
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
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        String test = "<Profile name=\"P1\">\n<Component id=\"1\"/>\n</Profile>\n" +
                "<Profile name=\"P2\">\n<Component id=\"2\"/>\n</Profile>";
        System.out.println("'"+getAsCompactFormatedXMLString(test)+"'");
        System.out.println("'"+getAsPrettyPrintFormatedXMLString(test)+"'");
        
        Document doc = createDocumentFromXmlString("<root>fd" + test + "</root>");
        System.out.println(nodeToString(doc.getDocumentElement()));
        System.out.println(nodeChildsToString(doc.getDocumentElement()));
    }
}
