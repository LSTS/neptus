/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 22/Jul/2005
 */
package pt.lsts.neptus.gui;

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

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.texture.TextureType;
import pt.lsts.neptus.types.texture.TexturesHolder;
import pt.lsts.neptus.util.ImageUtils;

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
                setText(I18n.text(pet));
                setFont(list.getFont());
            }
            else
            {
                //setUhOhText(pet + " (no image available)", list.getFont());
                setUhOhText(I18n.text(pet), list.getFont());
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
            intArray[i] = Integer.valueOf(i);
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
