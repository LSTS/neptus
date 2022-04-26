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
 * Author: 
 * 5/Mar/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import pt.lsts.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 */
public class ImagePanel 
extends JPanel
{
    private static final long serialVersionUID = -6728614478181991497L;
    protected Image image;
    protected String ImagePath = "";
    
    protected int imageWidth = 0;
    protected int imageHeight = 0;
    
    protected boolean fit_to_panel=false;

    /**
     * 
     */
    public ImagePanel()
    {
    }
    
    /** 
     * Creates a new instance of ImagePanel 
     */
    public ImagePanel(Image image)
    {
        this.image = image;
    }

    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g); //paint background

        if (imageWidth <= 0 | imageHeight <= 0)
        {
            //Draw image at its natural size.
        	if(fit_to_panel)
        		g.drawImage(image, 0, 0, this.getWidth(),this.getHeight(),this);
        	else
        		g.drawImage(image, 0, 0, this);
        }
        else
        {
            //Draw image at its natural size.
        	if(fit_to_panel)
        		g.drawImage(image, 0, 0, this.getWidth(),this.getHeight(),this);
        	else
        		g.drawImage(image, 0, 0, getImageWidth(), getImageHeight(), this);            
        }
    }

    
    /**
     * 
     */
    public void adjustImageSizeToPanelSize()
    {
        if (image == null)
            return;
        Dimension dim = getSize();
        /*
        double pW = dim.getWidth();
        //double pH = dim.getHeight();
        int iW = image.getWidth(this);
        int iH = image.getHeight(this);
        double ratioW = iW/pW;
        setImageWidth((int) (iW/ratioW));
        setImageHeight((int) (iH/ratioW));
        */
        image = ImageUtils.getScaledImage(image, (int) dim.getWidth(), (int) dim
                .getHeight(), false);
        setImageWidth(image.getWidth(this));
        setImageHeight(image.getHeight(this));
        //System.err.print("... " + getImageWidth());
        //System.err.println("  ... " + getImageHeight());
    }

    
    public void adjustImageSizeToPreferredSize()
    {
        if (image == null)
            return;
        Dimension dim = getPreferredSize();
        /*
        double pW = dim.getWidth();
        //double pH = dim.getHeight();
        int iW = image.getWidth(this);
        int iH = image.getHeight(this);
        double ratioW = iW/pW;
        setImageWidth((int) (iW/ratioW));
        setImageHeight((int) (iH/ratioW));
        */
        image = ImageUtils.getScaledImage(image, (int) dim.getWidth(), (int) dim
                .getHeight(), false);
        setImageWidth(image.getWidth(this));
        setImageHeight(image.getHeight(this));
    }

    /**
     * @param imageURL
     */
    public void setImage(String imageURL) 
    {
        this.ImagePath = imageURL;
        this.image = new ImageIcon(imageURL).getImage();
    }
    
    /**
     * @param image
     */
    public void setImage(Image image) 
    {
        this.ImagePath = null;
        this.image = image;
    }
    
    /**
     * @return
     */
    public String getImage() 
    {
        return ImagePath;
    }
    
    /**
     * @return Returns the imageHeight.
     */
    public int getImageHeight()
    {
        return imageHeight;
    }
    /**
     * @param imageHeight The imageHeight to set.
     */
    public void setImageHeight(int imageHeight)
    {
        this.imageHeight = imageHeight;
    }
    /**
     * @return Returns the imageWidth.
     */
    public int getImageWidth()
    {
        return imageWidth;
    }
    /**
     * @param imageWidth The imageWidth to set.
     */
    public void setImageWidth(int imageWidth)
    {
        this.imageWidth = imageWidth;
    }

	public boolean isFit_to_panel() {
		return fit_to_panel;
	}

	public void setFit_to_panel(boolean fit_to_panel) {
		this.fit_to_panel = fit_to_panel;
	}
}

