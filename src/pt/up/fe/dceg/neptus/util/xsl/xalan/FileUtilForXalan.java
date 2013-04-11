/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 19/Jan/2005
 */
package pt.up.fe.dceg.neptus.util.xsl.xalan;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.transform.TransformerException;

import org.apache.xalan.extensions.ExpressionContext;
import org.apache.xalan.extensions.XSLProcessorContext;
import org.apache.xalan.templates.ElemExtensionCall;
import org.apache.xml.dtm.DTM;
import org.apache.xpath.XPathContext;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.FileUtil;

/**
 * Implements one extension elements and function to allow
 * the insertion of a text file into a XLST transformation.
 *
 *
 * <p>Example:</p>
 * <PRE>
 * &lt;?xml version="1.0"?>
 * &lt;xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
 *                 version="1.0"
 *                 xmlns:fileutil="Neptus.FileUtilForXalan" 
 *                 extension-element-prefixes="fileutil" >
 *
 * 	&lt;xsl:output method="text" encoding="UTF-8" indent="no" media-type="text/plain" />
 *	&lt;xalan:component prefix="fileutil" elements="init" functions="getFileAsString">
 *		&lt;xalan:script lang="javaclass" src="pt.up.fe.dceg.neptus.util.xsl.xalan.FileUtilForXalan"/>
 *	&lt;/xalan:component>
 *
 *   &ltxsl:param name="prelude-file">../conf/isurus-preludio.conf&lt/xsl:param>
 *
 *   &lt;xsl:template match="/">
 *     &lt;fileutil:init />
 *     &lt;xsl:variable name="prel" select="fileutil:getFileAsString($prelude-file)"/>
 *     &lt;xsl:value-of select="$prel"/>
 *   &lt;/xsl:template>
 *
 * &lt;/xsl:stylesheet>
 * </PRE>
 * 
 * <br/><b>Be very carful when using document() function. If the path is relative
 * if you don't use the function call inside a for-each element selecting the all 
 * document like this:
 * <pre>
 * 		&lt;xsl:for-each select="document($vehicle-file)">
 *			&lt;xsl:variable name="prelfx" select="/vehicle/configuration-files/misc/file/href[../id='pre']"/>
 *			&lt;xsl:variable name="prel" select="fileutil:getFileAsString($prelfx)"/>
 *			&lt;xsl:value-of select="string($prel)"/>
 *		&lt;/xsl:for-each>		
 * </pre>
 * the function cannot use the document path as a resolver, and so fails to find the file.</b>  
 *
 * @author Paulo Dias
 * @version 1.0   01/2005
 */
public class FileUtilForXalan
{
    protected static String uriBase = ".";
    protected static String uriXML = ".";
    protected static String uriXSL = ".";
    protected static String fxSep = System.getProperty("file.separator", "/");
    
    
    /**
     * @param context
     * @param extensionElement
     * @throws TransformerException
     */
    public void init (XSLProcessorContext context, 
            ElemExtensionCall extensionElement) 
    throws TransformerException
    {
        uriXML = context.getTransformer().getBaseURLOfSource();
        try
        {
            new URI(uriXML);
        }
        catch (Exception e) 
        {
            uriXML = new File (uriXML).toURI().toASCIIString();
        }
        
        uriXSL = context.getStylesheet().getHref();
        try
        {
            new URI(uriXSL);
        }
        catch (Exception e) 
        {
            uriXSL = new File (uriXSL).toURI().toString();
        }

        String sysId = extensionElement.getSystemId();
        try
        {
            new URI(sysId);
        }
        catch (Exception e) 
        {
            sysId = new File (sysId).toURI().toString();
        }
         uriBase = sysId;
        //context.getTransformer().getMsgMgr().error(extensionElement, "ssdfgg");
    }
    
    /**
     * @param expContext
     * @param url
     * @return
     */
    public String getFileAsString (ExpressionContext expContext, String url)
    {
        String urlG = url;
        String uriCSrc = "";
        URI uriB;
        NeptusLog.pub().info("<###>url---".concat(url));
        NeptusLog.pub().info("<###>uriBase---".concat(uriBase));
        NeptusLog.pub().info("<###>uriXML---".concat(uriXML));
        NeptusLog.pub().info("<###>uriXSL---".concat(uriXSL));

        try
        {
            XPathContext xctxt = expContext.getXPathContext();
            uriCSrc = getCurrentSrcDocLocation(xctxt);
            try
            {
                new URI(uriCSrc);
            }
            catch (Exception e) 
            {
                uriCSrc = new File (uriCSrc).toURI().toString();
            }
        }
        catch (TransformerException e1)
        {
            NeptusLog.pub().error(this, e1);
        }

        
        //String uriCSrc = 
        NeptusLog.pub().info("<###>uriCSrc---".concat(uriCSrc));

        if (!new File(url).isAbsolute())
        {
            try
            {
                uriB = new URI (uriBase);
                urlG = new File (uriB.resolve(url)).getAbsolutePath();
                if (!new File(urlG).exists())
                {
                    uriB = new URI (uriXML);
                    urlG = new File (uriB.resolve(url)).getAbsolutePath();
                    if (!new File(urlG).exists())
                    {
                        uriB = new URI (uriCSrc);
                        urlG = new File (uriB.resolve(url)).getAbsolutePath();
                        if (!new File(urlG).exists())
                        {
                            uriB = new URI (uriXSL);
                            urlG = new File (uriB.resolve(url)).getAbsolutePath();
                            if (!new File(urlG).exists())
                                return urlG;
                        }
                    }
                }
            }
            catch (URISyntaxException e)
            {
                NeptusLog.pub().error(this, e);
                return urlG;
            }
        }
        String res = FileUtil.getFileAsString(urlG);
        res = res.replaceAll("\r", "");
        //NeptusLog.pub().info("<###> "+res);
        /*
        char[] ca = new char[2];
        ca[0] = 0xD;
        ca[1] = 0xA;
        char[] cb = new char[1];
        ca[0] = 0xA;
        return res.replaceAll(new String(ca), new String(cb));
        */
        return res;
    }
    

    /**
     * Execute the function.  The function must return
     * a valid URI.
     * @author Xalan dev. team
     * @param xctxt The current execution context.
     * @return A valid URI of the current source.
     */
    protected String getCurrentSrcDocLocation(XPathContext xctxt)
    {
        String fileLocation = null;
        
        //NeptusLog.pub().info("<###>aaaaa---".concat(expContext.getContextNode().getNamespaceURI()));
        //XPathContext xctxt = expContext.getXPathContext();
        int whereNode = xctxt.getCurrentNode();
        fileLocation = null;
        
        if (DTM.NULL != whereNode)
        {
            DTM dtm = xctxt.getDTM(whereNode);
            
            // %REVIEW%
            if (DTM.DOCUMENT_FRAGMENT_NODE == dtm.getNodeType(whereNode))
            {
                whereNode = dtm.getFirstChild(whereNode);
            }
            
            if (DTM.NULL != whereNode)
            {        
                fileLocation = dtm.getDocumentBaseURI();
                // int owner = dtm.getDocument();
                // fileLocation = xctxt.getSourceTreeManager().findURIFromDoc(owner);
            }
        }
        
        return (null != fileLocation) ? fileLocation : "";
    }

    
    public void getString (ExpressionContext expContext, String str)
    {
        NeptusLog.pub().info("<###>->>>> ".concat(str));
    }

    
    /*
    public void writeStringToOutput (XSLProcessorContext context, 
            ElemExtensionCall extensionElement)
    {
        String ssg = "s"; //extensionElement.getAttribute("st");
        String st =  context.getTransformer().getOutputTarget().getSystemId();
        try
        {
            FileWriter fr = new FileWriter(ssg, true);
            fr.write(st);
            fr.close();
        }
        catch (IOException e)
        {
            // TO DO Auto-generated catch block
            e.printStackTrace();
        }
    }
    */

    /*
    public void init(org.apache.xalan.extensions.XSLProcessorContext context, 
                   org.w3c.dom.Element elem) 
  {
    String name = elem.getAttribute("name");
    String value = elem.getAttribute("value");
    int val;
    try 
    {
      val = Integer.parseInt (value);
    } 
    catch (NumberFormatException e) 
    {
      e.printStackTrace ();
      val = 0;
    }
    counters.put (name, new Integer (val));
  }

    
        public int read(String name) 
    { 
        Integer cval = (Integer)counters.get(name);
        return (cval == null) ? 0 : cval.intValue();
    }

    public void incr(org.apache.xalan.extensions.XSLProcessorContext context,  
                   org.w3c.dom.Element elem) {
    String name = elem.getAttribute("name");
    Integer cval = (Integer) counters.get(name);
    int nval = (cval == null) ? 0 : (cval.intValue () + 1);
    counters.put (name, new Integer (nval));
  }
    */

    /*
<?xml version="1.0"?> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:counter="MyCounter"
                extension-element-prefixes="counter"
                version="1.0">


  <xalan:component prefix="counter"
                   elements="init incr" functions="read">
    <xalan:script lang="javaclass" src="MyCounter"/>
  </xalan:component>

  <xsl:template match="/">
    <HTML>
      <H1>Java Example</H1>
      <counter:init name="index" value="1"/>
      <p>Here are the names in alphabetical order by last name:</p>
      <xsl:for-each select="doc/name">
        <xsl:sort select="@last"/>
        <xsl:sort select="@first"/>
        <p>
        <xsl:text>[</xsl:text>
        <xsl:value-of select="counter:read('index')"/>
        <xsl:text>]. </xsl:text>
        <xsl:value-of select="@last"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="@first"/>
        </p>
        <counter:incr name="index"/>
      </xsl:for-each>
    </HTML>
  </xsl:template>
 
</xsl:stylesheet>
     */
}
