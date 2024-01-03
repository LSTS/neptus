/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.FocusManager;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.util.GuiUtils;

/**
 * A Panel that can be blocked.
 * <br>
 * Just set an instance of this class as the glassPane
 * of your JFrame an call <code>block()</code> as needed.
 */
@SuppressWarnings("serial")
public class BlockingGlassPane extends JPanel {

    private int        blockCount = 0;
    private BlockMouse blockMouse = new BlockMouse();
    private BlockKeys  blockKeys  = new BlockKeys();

    private InfiniteProgressPanel ipp;

    /**
     * Default constructor
     */
    public BlockingGlassPane() {
        this(200);
    }

    /**
     * @param size
     */
    public BlockingGlassPane(int size) {
        this(size, false);
    }

    /**
     * @param size
     * @param isOpaque
     */
    public BlockingGlassPane(int size, boolean isOpaque) {
        setVisible(false);
        setOpaque(isOpaque);

        ipp = InfiniteProgressPanel.createInfinitePanelBeans("");
        ipp.setOpaque(false);
        setLayout(new BorderLayout());
        add(ipp);
        addMouseListener(blockMouse);
    }

    public String getText() {
        return ipp.getText();
    }
    
    /**
     * @param message
     */
    public void setText(String message) {
        ipp.setText(message);
    }

    @Override
    public void setToolTipText(String text) {
        ipp.setToolTipText(text);
    }

    /**
     * Start or end blocking.
     *
     * @param block   should blocking be started or ended
     */
    public void block(boolean block) {
        if (block) {
            if (blockCount == 0) {
                ipp.start();
                setVisible(true);

                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                FocusManager.getCurrentManager().addKeyEventDispatcher(blockKeys);
            }
            blockCount++;
        }
        else {
            if (blockCount != 0)
                blockCount--;
            if (blockCount == 0) {
                FocusManager.getCurrentManager().removeKeyEventDispatcher(blockKeys);

                setCursor(Cursor.getDefaultCursor());

                setVisible(false);
                ipp.stop();
            }
        }		
    }

    /**
     * Test if this glasspane is blocked.
     *
     * @return    <code>true</code> if currently blocked
     */
    public boolean isBlocked() {
        return blockCount > 0;
    }

    /**
     * The key dispatcher to block the keys.
     */
    private class BlockKeys implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent ev) {
            Component source = ev.getComponent();
            if (source != null &&
                    SwingUtilities.isDescendingFrom(source, getParent())) {
                Toolkit.getDefaultToolkit().beep();
                ev.consume();
                return true;
            }
            return false;
        }
    }

    /**
     * The mouse listener used to block the mouse.
     */
    private class BlockMouse extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent ev) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void main(String[] args) {
        BlockingGlassPane bgp = new BlockingGlassPane();
        GuiUtils.testFrame(new JButton("Test"), "Test", 790, 560).setGlassPane(bgp);
        bgp.block(true);
    }
}
