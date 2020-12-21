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
 * 15/Jan/2005
 */
package pt.lsts.neptus.types.coord;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.XMLValidator;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
