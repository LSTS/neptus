/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.util.xsl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import pt.lsts.neptus.NeptusLog;


/**
 *
 * @author  Paulo Dias
 * @version 1.1  13/01/2005
 * @version 1.0  3/07/2001
 * @see <a href="http://java.sun.com/xml/">Sun's</a> JAXP 1.1
 * @see <a href="http://www.saxproject.org/">Megginson Technologies's</a> SAX2
 * @see <a href="http://xml.apache.org">Apache's</a> Xerces 2.6.2
 * @see <a href="http://xml.apache.org">Apache's</a> Xalan 2.6.0
 */

public class TransformDocument
{
	protected boolean _setDebug = false;

	public short OK = 0, FATAL_ERROR = 3, ERROR = 2, WARNING = 1;
	public short transformationResult = OK;
	public String transformationMessage = "";

	public ByteArrayOutputStream outputMsg = new ByteArrayOutputStream();
	
	/**
	 * O método <em>DoTransformation</em> serve para processar documentos XML.
	 * @param xmlSource	Documento XML.
	 * @param xslSheet	Documento XSL (pode ser "null", e assim procura a embebida no XML).
	 * @param xmlResult	Documento resultante.
	 * @param styleSheetParam	Parametros para a StyleSheet (pode ser "null").
	 * @return true	Se a transformação for realizada com sucesso.
	 */

	public boolean doTransformation (Source xmlSource,
									 Source xslSheet,
									 Result xmlResult,
									 Hashtable<String, String> styleSheetParam)
	{
		outputMsg.reset();
		
		transformationResult = OK;
		transformationMessage = "";
		
		Date data = new Date();
		outwrite("\nTransformation ini at "+ data.toString() + "\n");

		if (xmlSource == null || xmlResult == null)
		{
			outwrite("==NOTE== Notting to transform (bad command)!");
			return false;
		}

		if (styleSheetParam == null)
			styleSheetParam = new Hashtable<String, String>();


		try
		{
			// Use XSLTProcessorFactory to instantiate an XSLTProcessor.
			//XSLTProcessor processor = XSLTProcessorFactory.getProcessor();

			// Create a transform factory instance.
			TransformerFactory tfactory = TransformerFactory.newInstance();

			// Create a transformer for the stylesheet.
			//Transformer transformer = tfactory.newTransformer (xslSheet);
			Transformer transformer;
			try
			{
				transformer = tfactory.newTransformer (xslSheet);
			}
			catch (NullPointerException e)
			{
				try
				{
					Source embedxsl = tfactory.getAssociatedStylesheet(xmlSource, null, null, null);
					transformer = tfactory.newTransformer (embedxsl);
				}
				catch (TransformerConfigurationException e1)
				{
					//e1.printStackTrace();
					outwrite("==ERRO== "+ e.getMessage() + "\n javax.xml.transform.TransformerConfigurationException"+ e1.getMessage() );
					return false;
				}
			}
			transformer.setErrorListener(new ErrorListener(){

				
				public void error(TransformerException exception) throws TransformerException {
					transformationResult = ERROR;
					//System.err.println(transformationResult);
					transformationMessage +=  "\nERROR: " + exception.getMessageAndLocation();
					//System.err.println("---" + transformationMessage);
				
				}

				public void fatalError(TransformerException exception) throws TransformerException {
					transformationResult = FATAL_ERROR;
					//System.err.println(transformationResult);
					transformationMessage +=  "\nFATAL_ERROR: " + exception.getMessageAndLocation();
					//System.err.println("---" + transformationMessage);
				}

				public void warning(TransformerException exception) throws TransformerException {
					transformationResult = WARNING;
					//System.err.println(transformationResult);
					transformationMessage +=  "\nWARNING: " + exception.getMessageAndLocation();
					//System.err.println("---" + transformationMessage);
				}
				
			});

			//transformer.setURIResolver( (javax.xml.transform.URIResolver) (new URIResolver()));



			// Set the parameters for the Style Sheet if there are any
			outwrite("     > Number of parameters = "  + styleSheetParam.size() + "\n");
			if (!(styleSheetParam.isEmpty()))
			{
				Enumeration<?> keys = styleSheetParam.keys();
				Enumeration<?> elements = styleSheetParam.elements();

				try
				{
					for (; elements.hasMoreElements(); )
					{
						String pa1 = (String) keys.nextElement();
						String pa2 = (String) elements.nextElement();
						//processor.setStylesheetParam (pa1, pa2);
						transformer.setParameter (pa1, pa2);
						outwrite("            Key: "  + pa1.toString()
								+ "\n              Elem: "  + pa2.toString() + "\n");
					}
				}
				catch (NoSuchElementException e)
				{}
			}

			// Perform the transformation.
			//processor.process(xmlSource, xslSheet, xmlResult);

			// Transform the source XML
			//outGrabber.startGrabbing();
			//outGrabber.startGrabbingOut();
			
			transformer.transform (xmlSource,
								   xmlResult);
			
			data = new java.util.Date();
			outwrite("     > Transformation (<text/xml> into <"+
							transformer.getOutputProperty (OutputKeys.MEDIA_TYPE)
							+">) out ok at\n            " + data.toString() + "\n");
			
			if (transformationResult > WARNING)
				return false;
			return true;
		}
		catch (TransformerConfigurationException e)
		{
		   e.printStackTrace();
		   outwrite("==ERRO== javax.xml.transform.TransformerConfigurationException"+ e.getMessage() );
		   return false;
		}
		catch (TransformerException e)
		{
		   //e.printStackTrace();
		   outwrite("==ERRO== javax.xml.transform.TransformerException"+ e.getMessage() );
		   return false;
		}
		catch (java.lang.Exception e)
		{
		   //e.printStackTrace();
		   //return null;
		   outwrite("==ERRO== java.lang.Exception"+ e.getMessage() );
		   return false;
		}
	}






	/**
	 * O método <em>DoTransformation</em> serve para processar documentos XML.
	 * Usa a stylesheet embebida no XML.
	 * @param xmlSource	Documento XML.
	 * @param xmlResult	Documento resultante.
	 * @param styleSheetParam	Parametros para a StyleSheet (pode ser "null").
	 * @return true	Se a transformação for realizada com sucesso.
	 */
	public boolean doTransformation (Source xmlSource,
									 Result xmlResult,
									 Hashtable<String, String> styleSheetParam)
	{
		return doTransformation (xmlSource, null, xmlResult, styleSheetParam);
	}


	/**
	 * O método <em>DoTransformation</em> serve para processar documentos XML.
	 * Usa a stylesheet embebida no XML.
	 * @param xmlSource	Documento XML.
	 * @param xmlResult	Documento resultante.
	 * @param styleSheetParam	Parametros para a StyleSheet (pode ser "null").
	 * @return true	Se a transformação for realizada com sucesso.
	 */
	public boolean doTransformation (Source xmlSource,
									 Result xmlResult)
	{
		return doTransformation (xmlSource, null, xmlResult, null);
	}


	/**
	 * O método <em>DoTransformation</em> serve para processar documentos XML.
	 * @param xmlSource	Documento XML.
	 * @param xslSheet	Documento XSL (pode ser "null", e assim procura a embebida no XML).
	 * @param xmlResult	Documento resultante.
	 * @return true	Se a transformação for realizada com sucesso.
	 */
	public boolean doTransformation (Source xmlSource,
									 Source xslSheet,
									 Result xmlResult)
	{
		return doTransformation (xmlSource, xslSheet, xmlResult, null);
	}



    /**
     * O método <em>setDebug</em> indica se se quer info de debug a se impressa no System.err.
	 * @param setDebug   true se se quer info de debug.
     */

	public void setDebug (boolean setDebug)
    {
		_setDebug = setDebug;
	}


	private void outwrite (String st)
    {
		try
		{
			outputMsg.write(st.getBytes());
			outputMsg.flush();
		}
		catch(IOException e)
		{}

		if (_setDebug)
    	{
			try
			{
				Writer out = new OutputStreamWriter(System.err);
				out.write(st);
				out.flush();
			}
			catch(IOException e)
			{}
		}
	}








	//_______________  Helpers _______________

    /**
     * Construct a StreamSource from a File.
     */
	public StreamSource createStreamSource (File f)
	{
		return new StreamSource (f);
	}


    /**
     * Construct a StreamSource from a byte stream.

     */
	public StreamSource createStreamSource (InputStream inputStream)
	{
		return new StreamSource (inputStream);
	}


    /**
     * Construct a StreamSource from a byte stream.
     */
	public StreamSource createStreamSource (InputStream inputStream, String systemId)
	{
		return new StreamSource (inputStream, systemId);
	}


    /**
     * Construct a StreamSource from a character reader.
     */
	public StreamSource createStreamSource (Reader reader)
	{
		return new StreamSource (reader);
	}


    /**
     * Construct a StreamSource from a character reader.
     */
	public StreamSource createStreamSource (Reader reader, String systemId)
	{
		return new StreamSource (reader, systemId);
	}


    /**
     * Construct a StreamSource from a URL.
     */
	public StreamSource createStreamSource (String systemId)
	{
		return new StreamSource (systemId);
	}




    /**
     * Construct a StreamResult from a File.
     */
	public StreamResult createStreamResult (File f)
	{
		return new StreamResult (f);
	}


    /**
     * Construct a StreamResult from a byte stream.
     */
	public StreamResult createStreamResult (OutputStream outputStream)
	{
		return new StreamResult (outputStream);
	}


    /**
     * Construct a StreamResult from a character stream.
     */
	public StreamResult createStreamResult (Writer writer)
	{
		return new StreamResult (writer);
	}


    /**
     * Construct a StreamResult from a URL.
     */
	public StreamResult createStreamResult (String systemId)
	{
		return new StreamResult (systemId);
	}



	//_______________  Main  _______________

  /**
   * Transformar um documento através da linha de comandos.
   * <pre>
   *    -IN inputXMLURL
   *    [-XSL XSLTransformationURL]
   *    [-OUT outputFileName]
   *    [-PARAM name expression (Set a stylesheet parameter)]
   * </pre>
   *  <p>To set stylesheet parameters from the command line, use -PARAM name expression. If
   *  you want to set the parameter to a string value, enclose the string in single quotes (')</p>
   */


	public static void main(String[] args)
    throws IOException,
           MalformedURLException//,
           //SAXException
	{
		NeptusLog.pub().info("<###>\n-- Start Processing ----------------\n");
		System.out.flush();

		//XSLTErrorResources resbundle = (XSLTErrorResources)(XSLMessages.loadResourceBundle(Constants.ERROR_RESOURCES));

		if(args.length < 1)
		{
			//printArgOptions(resbundle);
			printArgOptions();
			NeptusLog.pub().info("<###>\n   Tranformation done: false");
			NeptusLog.pub().info("<###>\n-- End Processing ------------------");
			System.out.flush();
			return;
		}

		String inFileName = "";
		String xslFileName = "";
		String outFileName = "";

		Hashtable<String, String> styleSheetParam = new Hashtable<String, String>();


		TransformDocument processor = new TransformDocument();

		processor.setDebug (true);

	    for (int i = 0;  i < args.length;  i ++)
	    {
			if ("-IN".equalsIgnoreCase(args[i]))
			{
			  if ( i+1 < args.length)
				inFileName = args[++i];
			  else
				System.err.println("ER_MISSING_ARG_FOR_OPTION {\"-IN\"}"); //"Missing argument for);
			}
			else if ("-OUT".equalsIgnoreCase(args[i]))
			{
			  if ( i+1 < args.length)
				outFileName = args[++i];
			  else
				System.err.println("ER_MISSING_ARG_FOR_OPTION, new Object[] {\"-OUT\"}"); //"Missing argument for);
			}
			else if ("-XSL".equalsIgnoreCase(args[i]))
			{
			  if ( i+1 < args.length)
				xslFileName = args[++i];
			  else
				System.err.println("ER_MISSING_ARG_FOR_OPTION, new Object[] {\"-XSL\"}"); //"Missing argument for);
			}
			else if ("-PARAM".equalsIgnoreCase(args[i]))
			{
			  if ( i+2 < args.length)
			  {
				String name = args[++i];
				String expression = args[++i];
				styleSheetParam.put (name, expression);
			  }
			  else
				System.err.println("ER_MISSING_ARG_FOR_OPTION, new Object[] {\"-PARAM\"}"); //"Missing argument for);
			}
	    }


		//XSLTInputSource in;
		//XSLTInputSource xsl;
		//XSLTResultTarget out;

		StreamSource in;
		StreamSource xsl;
		StreamResult out;

		if ( inFileName.compareTo("") != 0)
		{
			in = new StreamSource (inFileName);
			if ( xslFileName.compareTo("") != 0)
				xsl = new StreamSource (new File(xslFileName));
			else
				xsl = null;
			if ( outFileName.compareTo("") != 0)
			{
			    //Tive de transformar num Stream já que com o JDK5 dá erro de
			    //não encontrar o ficheiro!!!
			    FileOutputStream fx = new FileOutputStream(new File(outFileName));
			    //NeptusLog.pub().info("<###> "+fx.createNewFile());
			    out = new StreamResult (fx);
			}
			else
				out = new StreamResult (System.out);
		}
		else
		{
			printArgOptions();
			NeptusLog.pub().info("<###>\n   Tranformation done: false");
			NeptusLog.pub().info("<###>\n-- End Processing ------------------");
			System.out.flush();
			return;
		}

		//boolean bl = processor.processDocuments(in, xsl, out, styleSheetParam);
		boolean bl = processor.doTransformation(in, xsl, out, styleSheetParam);
		NeptusLog.pub().info("<###>\n   Tranformation done: " + bl);
		NeptusLog.pub().info("<###>\n   " + processor.transformationMessage);
		NeptusLog.pub().info("<###>\n-- End Processing ------------------");
		System.out.flush();
	}



  protected static void printArgOptions()
  {
    NeptusLog.pub().info("<###>xslproc options: ");
    NeptusLog.pub().info("<###>    -IN inputXMLURL");
    NeptusLog.pub().info("<###>   [-XSL XSLTransformationURL]");
    NeptusLog.pub().info("<###>   [-OUT outputFileName]");
    NeptusLog.pub().info("<###>   [-PARAM name expression (Set a stylesheet parameter)]");
    System.out.flush();
  }


}

