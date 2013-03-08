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
 * 15/Jan/2005
 * $Id:: CoordinateSystemsHolder.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.types.coord;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.XMLValidator;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 *
 */
public class CoordinateSystemsHolder
{
    private static LinkedHashMap<String, CoordinateSystem> coordinateSystemList = new LinkedHashMap<String, CoordinateSystem>();
    
    private Document doc;

    /**
     * 
     */
    public CoordinateSystemsHolder(String url)
    {
        super();
        load (url);
    }

    /**
     * @param url
     */
    public boolean load (String url)
    {    	
        String fileAsString = FileUtil.getFileAsString(url);

        try
        {
            String sLoc = new File (ConfigFetch.getCoordinateSystemSchemaLocation())
            	.getAbsoluteFile().toURI().toASCIIString();
            XMLValidator xmlVal = new XMLValidator(fileAsString, sLoc);
            xmlVal.validate();
        }
        catch (Exception e1)
        {
            NeptusLog.pub().error(this, e1);
            //e1.printStackTrace();
        }

        try
        {
            doc = DocumentHelper.parseText(fileAsString);            
            List<?> lst = doc.selectNodes("/coordinateSystems/coordinateSystem");
            ListIterator<?> lstIt = lst.listIterator();
            while (lstIt.hasNext())
            {
            	Element elem = (Element) lstIt.next();
            	CoordinateSystem cs = new CoordinateSystem(elem.asXML());
            	coordinateSystemList.put(cs.getId(), cs);
            }
        } catch (DocumentException e)
        {
            NeptusLog.pub().error(this, e);
        }
        return true;
    }

    public int size()
    {
        return coordinateSystemList.size();
    }
    
    /**
     * @return Returns the coordinateSystemList.
     */
    public LinkedHashMap<String, CoordinateSystem> getCoordinateSystemList()
    {
        return coordinateSystemList;
    }
    
    public static void main(String[] args)
    {
    	//TODO fix para JUnit
        new CoordinateSystemsHolder("conf/neptus-coordinateSystems.xml");
    	
    }

}
