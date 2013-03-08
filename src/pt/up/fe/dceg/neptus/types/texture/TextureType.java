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
 * $Id:: TextureType.java 9616 2012-12-30 23:23:22Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.types.texture;

import java.awt.Image;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 *
 */
public class TextureType
{
    public static float DEFAULT_SHININNESS = 0.3f; 
    
    private String name = "";
    private String imageFileName = "";
    private Image textureImage = null;
    private float shininess = DEFAULT_SHININNESS;
    
    protected Document doc;

    /**
     * @param xml
     */
    public TextureType(String xml)
    {
        super();
        load(xml);
        loadImage();
    }

    


    /**
     * @param xml
     * @return
     */
    public boolean load (String xml)
    {
        try
        {
            //NeptusLog.pub().debug("XML:\n" + xml);
            doc = DocumentHelper.parseText(xml);
            
            Node node;
            
            name = doc.selectSingleNode("/texture/@name").getText();
            imageFileName = doc.selectSingleNode("/texture/@image-file").getText();
            
            node = doc.selectSingleNode("/texture/@shininess");
            if (node != null) 
                shininess = Float.parseFloat(node.getText());
            else
                shininess = DEFAULT_SHININNESS;

        } catch (DocumentException e)
        {
           NeptusLog.pub().error(this, e);
           return false;
        }
        return true;
    }

    /**
     * @return
     */
    private boolean loadImage()
    {
        textureImage = ImageUtils.getImage("images/textures/" + getImageFileName());
        //ImagePanel J =new ImagePanel(textureImage);
        //GuiUtils.testFrame(J,"image,");
        if (textureImage != null)
            return true;
        else
            return false;
    }




    /**
     * @return Returns the imageFileName.
     */
    public String getImageFileName()
    {
        return imageFileName;
    }




    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }




    /**
     * @return Returns the shine.
     */
    public float getShininess()
    {
        return shininess;
    }




    /**
     * @return Returns the textureImage.
     */
    public Image getTextureImage()
    {
        return textureImage;
    }

    
}
