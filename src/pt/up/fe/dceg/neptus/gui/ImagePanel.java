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
 * 5/Mar/2005
 * $Id:: ImagePanel.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.ImageUtils;

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

