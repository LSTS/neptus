/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 21/Jul/2005
 * $Id:: TexturesHolder.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.types.texture;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JPanel;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.TextureComboChooser;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.StreamUtil;

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
