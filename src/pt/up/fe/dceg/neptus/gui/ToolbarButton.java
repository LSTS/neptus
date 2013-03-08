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
 * 8/Fev/2005
 * $Id:: ToolbarButton.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.jdesktop.swingx.JXButton;

import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

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
