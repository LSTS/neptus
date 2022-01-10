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
 * 8/Fev/2005
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.jdesktop.swingx.JXButton;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * A WindowsXP style button (the margins only appear when the mouse is over it) Also has a convenient contructor that
 * loads an image an sets the tooltiptext for you.
 * 
 * @author Zé Carlos
 */
public class ToolbarButton extends JXButton {

    public static final long serialVersionUID = 17;
    private Color bgColor;

    /**
     * The class contructor
     * 
     * @param imageURL The url of the icon to be displayed on the button
     * @param toolTipText The text shown to the user when the mouse is over it
     * @param actionCommand The action to be fired when the user clicks this button
     * @param cl The ClassLoader that will load the image file
     */
    public ToolbarButton(String imageURL, String toolTipText, String actionCommand) {
        super(new ImageIcon(ImageUtils.getImage(imageURL)));

        setToolTipText(toolTipText);
        setActionCommand(actionCommand);
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

    }

    /**
     * The class contructor
     * 
     * @param image The button's icon
     * @param text The text to be shown
     * @param toolTipText The text shown to the user when the mouse is over it
     * @param actionCommand The action to be fired when the user clicks this button
     * @param cl The ClassLoader that will load the image file
     */
    public ToolbarButton(ImageIcon image, String text, String toolTipText, String actionCommand) {
        super(text);

        if (image != null)
            setIcon(image);

        setToolTipText(toolTipText);
        setActionCommand(actionCommand);
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

    }

    /**
     * The class contructor
     * 
     * @param imageURL The url of the icon to be displayed on the button
     * @param toolTipText The text shown to the user when the mouse is over it
     * @param actionCommand The action to be fired when the user clicks this button
     * @param cl The ClassLoader that will load the image file
     */
    public ToolbarButton(ImageIcon image, String toolTipText, String actionCommand) {
        super(image);

        setToolTipText(toolTipText);
        setActionCommand(actionCommand);
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
    }

    public ToolbarButton(AbstractAction action) {
        super(action);
        setText("");
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

        if (action.getValue(AbstractAction.SHORT_DESCRIPTION) != null) {
            setToolTipText(action.getValue(AbstractAction.SHORT_DESCRIPTION).toString());
        }
        else {
            setToolTipText(action.getValue(AbstractAction.NAME).toString());
        }
    }

    public void setBackColor(Color c) {
        bgColor = c;
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jdesktop.swingx.JXButton#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        if (bgColor != null) {
            g.setColor(bgColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        else
            setOpaque(false);
        super.paintComponent(g);
    }

    @SuppressWarnings("serial")
    public static void main(String[] args) {
        ToolbarButton tb = new ToolbarButton(new AbstractAction("Test", ImageUtils.getScaledIcon(
                "images/buttons/clear.png", 16, 16)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub

            }
        });
        GuiUtils.testFrame(tb);
    }
}
