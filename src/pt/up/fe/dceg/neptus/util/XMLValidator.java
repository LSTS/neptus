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
 * 12/Fev/2005
 * $Id:: XMLValidator.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.dom4j.util.XMLErrorHandler;
import org.xml.sax.SAXException;

import pt.up.fe.dceg.neptus.NeptusLog;

/**
 * @author Jan Woerner <woerner@ipr.uka.de> (original author)
 * @author Paulo Dias
 */
public class XMLValidator
{

	private String schemaURI;
	private Document document = org.dom4j.DocumentHelper.createDocument();
	
	private void logValidation (String log_output) 
	{
		NeptusLog.pub().info(this.toString() + log_output);
	}

	/**
     * 
     */
    public XMLValidator()
    {
        super();
    }


	public XMLValidator(String schemaURI)
    {
        this.schemaURI = schemaURI;
    }

	/**
	 * instanciates the validation of a dom4j document against 
	 * a schema available at the URI where the respective 
	 * schema can be found, e.g.:
	 * ...
	 * org.dom4j.Document document = DocumentHelper.createDocument();
	 * Validator validator = new
	 * 	Validator(document, 
	 *             "http://my.Server:0101/myApp/mySchemas/mySchema.xsd"); 
	 * ... 
	 *  	 
	 * @param document is org.dom4j.Document
	 * @param schemaURI is String
	 */
	public XMLValidator(Document document, String schemaURI)
            throws DocumentException
    {
        if (!document.equals(null))
        {
            this.document = document;
        }
        else
        {
            throw new DocumentException(
                    "validator init: dom4j Document is null");
        }
        if ((!schemaURI.equals(null)) && (!schemaURI.equals("")))
        {
            this.schemaURI = schemaURI;
        }
        else
        {
            throw new DocumentException(
                    "validator init: schema is null or empty");
        }
    }

    
	/**
	 * same as above, but with String as input parameter for the xml tree to validate  
	 * ...
	 * org.dom4j.Document document = DocumentHelper.createDocument();
	 * Validator validator = new
	 * 	Validator(xmlString, 
	 *             "http://my.Server:0101/myApp/mySchemas/mySchema.xsd"); 
	 * ... 
	 *  	 
	 * @param xmlString is an xml structure in a String
	 * @param schemaURI is String
	 */
	public XMLValidator(String xmlString, String schemaURI)
    throws DocumentException
    {
        if (!xmlString.equals(null))
        {
            this.document = DocumentHelper.parseText(xmlString);
        }
        else
        {
            throw new DocumentException(
                    "validator init: xmlString could not be parsed; xmlString is null");
        }
        if ((!schemaURI.equals(null)) && (!schemaURI.equals("")))
        {
            this.schemaURI = schemaURI;
        }
        else
        {
            throw new DocumentException(
                    "validator init: schema is null or empty");
        }
    }

    /**
     * returns the document that is set for validation
     * 
     * @return
     */
    public Document getDocument()
    {
        return document;
    }

    /**
     * returns the URI of the schema that was set for the validation of the
     * document
     * 
     * @return
     */
    public String getSchemaURI()
    {
        return schemaURI;
    }

    /**
     * sets the document for validation
     * 
     * @param document
     */
    public void setDocument(Document document)
    {
        this.document = document;
    }

    /**
     * sets the URI to the schema used for the validation against the set
     * document.
     * 
     * @param string
     */
    public void setSchemaURI(String string)
    {
        schemaURI = string;
    }

    /**
     * validates the document against the schema set by constructor or changed
     * by setSchemaURI.
     * 
     * @return
     * @throws Exception
     */
    public boolean validate() 
    throws DocumentException, IOException, SAXException
    {
        long initTime = System.currentTimeMillis();
        
        if (this.document == null)
        {
            throw new org.dom4j.DocumentException(
                    "validate: dom4j Document is null");
        }
        if ((this.schemaURI.equals(null)) || (this.schemaURI.equals("")))
        {
            throw new org.dom4j.DocumentException(
                    "validate: schema is null or empty");
        }
        String xmlDoc = this.document.asXML();
        StringReader xmlDocReader = new StringReader(xmlDoc);
        SAXReader reader = new SAXReader();
        reader.setValidation(true);

        // specify the schema to use
        reader.setFeature("http://xml.org/sax/features/validation", true);
        reader.setFeature("http://apache.org/xml/features/validation/schema",
                true);
        reader.setProperty(
                "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                this.schemaURI);

        // add an error handler which turns any errors into XML
        XMLErrorHandler errorHandler = new XMLErrorHandler();
        reader.setErrorHandler(errorHandler);

        // now lets parse the document
        Document document = reader.read(xmlDocReader);

        Element errorEle = errorHandler.getErrors();

        // configure logging output
        StringWriter logOut = new StringWriter();
        // now lets output the errors as XML
        XMLWriter writer = new XMLWriter(logOut, OutputFormat
                .createPrettyPrint());
        writer.write(errorHandler.getErrors());
        if (errorEle.hasContent())
        {
            logValidation("Validation of Document with root element \""
                    + document.getRootElement().getName()
                    + "\" against schema @ \"" + schemaURI
                    + "\" failed because of:" + logOut.toString());
        }
        long totalTime = System.currentTimeMillis() - initTime;
        logValidation("Total validation time: " + totalTime + " ms.");
        return !errorEle.hasContent();
    }

    public boolean validate(File file) 
    throws DocumentException, IOException, SAXException
    {

        this.document = this.read(file);

        if (this.document == null)
        {
            throw new DocumentException(
                    "validate: dom4j Document is null");
        }
        if ((this.schemaURI.equals(null)) || (this.schemaURI.equals("")))
        {
            throw new DocumentException(
                    "validate: schema is null or empty");
        }
        String xmlDoc = this.document.asXML();
        StringReader xmlDocReader = new StringReader(xmlDoc);
        SAXReader reader = new SAXReader();
        reader.setValidation(true);

        // specify the schema to use
        reader.setFeature("http://xml.org/sax/features/validation", true);
        reader.setFeature("http://apache.org/xml/features/validation/schema",
                true);
        reader
                .setProperty(
                        "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                        this.schemaURI);

        // add an error handler which turns any errors into XML
        XMLErrorHandler errorHandler = new XMLErrorHandler();
        reader.setErrorHandler(errorHandler);

        // now lets parse the document
        Document document = reader.read(xmlDocReader);

        Element errorEle = errorHandler.getErrors();

        // configure logging output
        StringWriter logOut = new StringWriter();
        // now lets output the errors as XML
        XMLWriter writer = new XMLWriter(logOut, OutputFormat
                .createPrettyPrint());
        writer.write(errorHandler.getErrors());
        if (errorEle.hasContent())
        {
            logValidation("Validation of the XML File with root element \""
                    + document.getRootElement().getName()
                    + "\" against schema @ \"" + schemaURI
                    + "\" failed because of:" + logOut.toString());
        }
        return !errorEle.hasContent();
    }

    public boolean isXML(File file)
    {
        if (this.read(file) != null)
        {
            return true;
        }
        return false;
    }

    /**
     * reads a xml file from fs
     * @param fileName
     * @return
     */
    final private Document read(File file)
    {
        Document ret = null;

        if (file.exists())
        {
            try
            {
                SAXReader reader = new SAXReader();
                ret = reader.read(file);
            }
            catch (Exception e)
            {
                logValidation("Validation: File was not XML");
                return null;
            }
        }
        else
        {
            System.err
                    .println("Validation: Sorry, object file not found.\n\nPlease check if it is located in: "
                            + file.getAbsolutePath());
        }
        return ret;
    }
}
