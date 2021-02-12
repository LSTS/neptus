/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 8/Fev/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

/**
 * A WindowsXP style toggle button (the margins only appear when the mouse is over)
 * @author Zé Carlos
 */
public class ToolbarSwitch extends JToggleButton {

	public static final long serialVersionUID = 17;
	
	public ToolbarSwitch(String text, String actionCommand) {
		super(text);
		setToolTipText(text);
		setActionCommand(actionCommand);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
	}
	
	public ToolbarSwitch(ImageIcon icon, String tooltipText, String actionCommand) {
		super(icon);
		setActionCommand(actionCommand);
		setToolTipText(tooltipText);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				if (!isSelected())
					setBorderPainted(false);
				repaint();
			}
		});
	}
	
	/**
	 * The class contructor
	 * @param imageURL The url of the icon to be displayed on the button
	 * @param toolTipText The text shown to the user when the mouse is over it
	 * @param actionCommand The action to be fired when the user clicks this button
	 * @param cl The ClassLoader that will load the image file
	 */
	public ToolbarSwitch(String imageURL, String toolTipText, String actionCommand, ClassLoader cl) {
		super(new ImageIcon(cl.getResource(imageURL)));
		setToolTipText(toolTipText);
		setActionCommand(actionCommand);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
	}
	
	public ToolbarSwitch(AbstractAction action) {
		super(action);
		setText("");
		setToolTipText(action.getValue(AbstractAction.NAME).toString());
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
		setSelected(true);
	}

    public ToolbarSwitch(String text, AbstractAction action) {
        super(action);
        setText("");
        setToolTipText(text);
        setBorderPainted(false);
        setMargin(new Insets(1, 1, 1, 1));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent arg0) {
                if (isEnabled()) {
                    setBorderPainted(true);
                    repaint();
                }
            }

            public void mouseExited(MouseEvent arg0) {
                setBorderPainted(false);
                repaint();
            }
        });
        setSelected(true);
    }
	
	public void setState(boolean value) {
	    this.setSelected(value);
	}
	
	public boolean getState() {
	    return this.isSelected();
	}
	
}
