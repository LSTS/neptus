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
 * 22/Jul/2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import pt.up.fe.dceg.neptus.types.texture.TextureType;
import pt.up.fe.dceg.neptus.types.texture.TexturesHolder;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 *
 */
public class TextureComboChooser 
extends JComboBox<Object>
{
    private static final long serialVersionUID = -888237723263535970L;

    ImageIcon[] images;
    String[] texNames;


    private String getSelectedName() {
    	int selection = getSelectedIndex();
    	return texNames[selection];
    }
    
    public TextureType getCurrentlySelectedTexture() {
    	return TexturesHolder.getTextureByName(getSelectedName());
    }
    
    /**
     * @param intArray
     */
    public TextureComboChooser(Integer[] intArray)
    {
        super(intArray);
    }
    
    /**
     * @author Paulo Dias
     *
     */
    class TextureComboBoxRenderer
    extends JLabel
    implements ListCellRenderer<Object>
    {
        private static final long serialVersionUID = 5863479457884534402L;
        private Font uhOhFont;

        /**
         * 
         */
        public TextureComboBoxRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(LEFT);
            setVerticalAlignment(CENTER);
        }


        /*
         * This method finds the image and text corresponding to the selected
         * value and returns the label, set up to display the text and image.
         */
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            // Get the selected index. (The index param isn't
            // always valid, so just use the value.)
            int selectedIndex = ((Integer) value).intValue();

            if (isSelected)
            {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else
            {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            // Set the icon and text. If icon was null, say so.
            ImageIcon icon = images[selectedIndex];
            String pet = texNames[selectedIndex];
            setIcon(icon);
            if (icon != null)
            {
                setText(pet);
                setFont(list.getFont());
            }
            else
            {
                //setUhOhText(pet + " (no image available)", list.getFont());
                setUhOhText(pet, list.getFont());
            }

            return this;
        }

        // Set the font and text when no image was found.
        protected void setUhOhText(String uhOhText, Font normalFont)
        {
            if (uhOhFont == null)
            { // lazily create this font
                uhOhFont = normalFont.deriveFont(Font.ITALIC);
            }
            setFont(uhOhFont);
            setText(uhOhText);
        }
    }

    


    public static TextureComboChooser createTextureComboChooser()
    {
        LinkedHashMap<String, TextureType> tlh = TexturesHolder.getTexturesList();
        ImageIcon[] images = new ImageIcon[tlh.keySet().size() + 1];
        Integer[] intArray = new Integer[tlh.keySet().size() + 1];
        
        String[] texNames = new String[tlh.keySet().size() + 1];
        texNames[0] = "none";
        Iterator<?> it = tlh.keySet().iterator();
        int i = 1;
        
        while(it.hasNext())
        {
            texNames[i] = (String) it.next();
            i++;
        }
            
        for (i = 0; i < texNames.length; i++) 
        {
            intArray[i] = new Integer(i);
            TextureType texType = TexturesHolder.getTextureByName(texNames[i]);
            if (texType != null)
                images[i] = new ImageIcon(ImageUtils.getScaledImage(texType.getTextureImage(),50, 30, false));
            else
                images[i] = null;
            if (images[i] != null) {
                images[i].setDescription(texNames[i]);
            }
        }

        TextureComboChooser texturesList = new TextureComboChooser(intArray);
        texturesList.images = images;
        texturesList.texNames = texNames;
        TextureComboBoxRenderer renderer= texturesList.new TextureComboBoxRenderer();
        renderer.setPreferredSize(new Dimension(150, 35));
        texturesList.setRenderer(renderer);
        texturesList.setMaximumRowCount(3);

        return texturesList;
    }
    
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // TODO Auto-generated method stub

    }

	public void setSelectedTexture(TextureType selectedTexture) {
		setSelectedIndex(0);
		if (selectedTexture == null) { 
			System.err.println("Setting- selected index to 0");
			return;
		}
		
		for (int i = 1; i < texNames.length; i++) {
			if (texNames[i].equals(selectedTexture.getName())) {
				System.err.println("Setting selected index to "+i);		
				setSelectedIndex(i);
				return;
			}
		}
		
		System.err.println("Setting selected index to 0");
		
	}

    
}
