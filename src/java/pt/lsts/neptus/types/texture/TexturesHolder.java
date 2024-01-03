/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 21/Jul/2005
 */
package pt.lsts.neptus.types.texture;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JPanel;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.TextureComboChooser;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.StreamUtil;

/**
 * @author Paulo Dias
 *
 */
public class TexturesHolder
{

    private static final String TEXTURES_FILE_NAME = "/images/textures/textures.xml";

    /**
     * <String, TextureType>
     */
    private static LinkedHashMap<String, TextureType> texturesList = new LinkedHashMap<String, TextureType>();
    private static boolean isLoaded = false;
        
    protected static Document doc;


    /**
     * @return
     */
    public static boolean load()
    {
        String xml = null;
        InputStream ist = TexturesHolder.class.getResourceAsStream(TEXTURES_FILE_NAME);
        xml = StreamUtil.copyStreamToString(ist);
        isLoaded = load(xml);
        return isLoaded;
    }

    
    /**
     * @param xml
     * @return
     */
    public static boolean load(String xml)
    {
        try
        {
            //NeptusLog.pub().debug("XML:\n" + xml);
            doc = DocumentHelper.parseText(xml);
            
            List<?> lst = doc.selectNodes("//texture");
            Iterator<?> it = lst.iterator();
            while(it.hasNext())
            {
                Node nd = (Node) it.next();
                TextureType texture = new TextureType(nd.asXML());
                if (texture != null)
                {
                    texturesList.put(texture.getName().toLowerCase(), texture);
                }
            }
            isLoaded =  true;

        } catch (DocumentException e)
        {
           NeptusLog.pub().error(TexturesHolder.class, e);
           isLoaded =  false;
        }

        return isLoaded;        
    }
    
    
    public static int size()
    {
        if(!isLoaded)
            load();
        
        return texturesList.size();
    }



    /**
     * @return Returns the texturesList.
     */
    public static LinkedHashMap<String, TextureType> getTexturesList()
    {
        if(!isLoaded)
            load();
        
        return texturesList;
    }

    
    /**
     * @param name
     * @return TextureType object or null if none found
     */
    public static TextureType getTextureByName(String name)
    {
        if(!isLoaded)
            load();
        
        TextureType texture = texturesList.get(name);
        return texture;
    }
    
    /**
     * @return Returns the isLoaded.
     */
    public static boolean isLoaded()
    {
        return isLoaded;
    }


    /**
     * @return
     */
    public static TextureComboChooser getTextureListChooser()
    {
        TextureComboChooser tch = TextureComboChooser.createTextureComboChooser();
        return tch;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        boolean ret = TexturesHolder.load();
        System.err.println("Load: " + (ret?"ok":"not ok") + ".");
        System.err.println("Size: " + TexturesHolder.size() + ".");
        TextureComboChooser tch = TextureComboChooser.createTextureComboChooser();
        JPanel jp = new JPanel();
        jp.add(tch);
        //JLabel[] data = {new JLabel("one"), new JLabel("two"), new JLabel("three"), new JLabel("four")};
        //JList dataList = new JList(data);
        //jp.add(dataList);
        GuiUtils.testFrame(jp, "test");
    }

}
