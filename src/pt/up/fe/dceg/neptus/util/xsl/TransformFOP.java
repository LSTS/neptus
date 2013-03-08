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
 * 5/7/2005
 * $Id:: TransformFOP.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.util.xsl;

// Java
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 * 
 */
public class TransformFOP
{

    /**
     * @param xml
     * @param xslt
     * @param pdf
     * @return
     */
    public static boolean convertXML2PDF(String xml, String xslt, String pdf)
    {
        File xmlFx, xsltFx, pdfFx;
        xmlFx = new File(xml);
        xsltFx = new File(xslt);
        pdfFx = new File(pdf);
        return convertXML2PDF(xmlFx, xsltFx, pdfFx);
    }
    /**
     * @param xml
     * @param xslt
     * @param pdf
     * @return
     */
    public static boolean convertXML2PDF(File xml, File xslt, File pdf)
    // throws IOException, FOPException, TransformerException
    {
        // configure fopFactory as desired
        FopFactory fopFactory = FopFactory.newInstance();

        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        // configure foUserAgent as desired
        foUserAgent.setAuthor("Neptus " + ConfigFetch.getVersionSimpleString());

        OutputStream out = null;

        try
        {
            // Setup output
            out = new java.io.FileOutputStream(pdf);
        } catch (FileNotFoundException e3)
        {
            // TODO Auto-generated catch block
            e3.printStackTrace();
            return false;
        }

        try
        {
            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

//            // Setup logger
//            // Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
//            Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_DISABLED);
//            // Logger logger = Logger.getLogger("FOP");
//            driver.setLogger(logger);
//            org.apache.fop.messaging.MessageHandler.setScreenLogger(logger);
//
//            // Setup Renderer (output format)
//            driver.setRenderer(Driver.RENDER_PDF);
//
//            // Setup output
//            driver.setOutputStream(out);

            // Setup XSLT
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(
                    xslt));

            // Setup input for XSLT transformation
            Source src = new StreamSource(xml);

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
        } catch (IllegalArgumentException e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
            return false;
        } catch (TransformerException e)
        {
            return false;
        }
		catch (FOPException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally
        {
            try
            {
                out.close();
            } catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return true;
    }

    
//    public static boolean convertXML2PDF(File xml, File xslt, File pdf)
//    // throws IOException, FOPException, TransformerException
//    {
//        // Construct driver
//    	org.apache.fop.apps.Driver driver;
//        OutputStream out = null;
//
//        try
//        {
//            // Setup output
//            out = new java.io.FileOutputStream(pdf);
//        } catch (FileNotFoundException e3)
//        {
//            // TODO Auto-generated catch block
//            e3.printStackTrace();
//            return false;
//        }
//
//        try
//        {
//            driver = new Driver();
//
//            // Setup logger
//            // Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
//            Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_DISABLED);
//            // Logger logger = Logger.getLogger("FOP");
//            driver.setLogger(logger);
//            org.apache.fop.messaging.MessageHandler.setScreenLogger(logger);
//
//            // Setup Renderer (output format)
//            driver.setRenderer(Driver.RENDER_PDF);
//
//            // Setup output
//            driver.setOutputStream(out);
//
//            // Setup XSLT
//            TransformerFactory factory = TransformerFactory.newInstance();
//            Transformer transformer = factory.newTransformer(new StreamSource(
//                    xslt));
//
//            // Setup input for XSLT transformation
//            Source src = new StreamSource(xml);
//
//            // Resulting SAX events (the generated FO) must be piped through to
//            // FOP
//            Result res = new SAXResult(driver.getContentHandler());
//
//            // Start XSLT transformation and FOP processing
//            transformer.transform(src, res);
//        } catch (IllegalArgumentException e2)
//        {
//            // TODO Auto-generated catch block
//            e2.printStackTrace();
//            return false;
//        } catch (TransformerException e)
//        {
//            return false;
//        } finally
//        {
//            try
//            {
//                out.close();
//            } catch (IOException e)
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        return true;
//    }

    public static void main(String[] args)
    {
        try
        {
            System.out.println("FOP ExampleXML2PDF\n");
            System.out.println("Preparing...");

            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            File xmlfile = new File("checklists/rov-checklist.xml");
            File xsltfile = new File("conf/checklist-fo.xsl");
            File pdffile = new File("checklists/teste.pdf");

            System.out.println("Input: XML (" + xmlfile + ")");
            System.out.println("Stylesheet: " + xsltfile);
            System.out.println("Output: PDF (" + pdffile + ")");
            System.out.println();
            System.out.println("Transforming...");

            //TransformFOP app = new TransformFOP();
            convertXML2PDF(xmlfile, xsltfile, pdffile);

            System.out.println("Success!");
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
