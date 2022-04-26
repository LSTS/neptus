/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Image;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.ImageUtils;

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
