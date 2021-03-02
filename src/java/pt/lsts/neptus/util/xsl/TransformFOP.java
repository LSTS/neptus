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
 * 5/7/2005
 */
package pt.lsts.neptus.util.xsl;

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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
//            TransformerFactory factory = TransformerFactory.getDeclaredConstructor().newInstance();
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
            NeptusLog.pub().info("<###>FOP ExampleXML2PDF\n");
            NeptusLog.pub().info("<###>Preparing...");

            // Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            // Setup input and output files
            File xmlfile = new File("checklists/rov-checklist.xml");
            File xsltfile = new File("conf/checklist-fo.xsl");
            File pdffile = new File("checklists/teste.pdf");

            NeptusLog.pub().info("<###>Input: XML (" + xmlfile + ")");
            NeptusLog.pub().info("<###>Stylesheet: " + xsltfile);
            NeptusLog.pub().info("<###>Output: PDF (" + pdffile + ")");
            NeptusLog.pub().info("<###>Transforming...");

            //TransformFOP app = new TransformFOP();
            convertXML2PDF(xmlfile, xsltfile, pdffile);

            NeptusLog.pub().info("<###>Success!");
        } catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
