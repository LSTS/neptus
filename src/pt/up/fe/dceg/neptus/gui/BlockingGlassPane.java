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
 * 20??/??/??
 * $Id:: BlockingGlassPane.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.util.GuiUtils;

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

	public BlockingGlassPane() {
	    this(200);
	}

	/**
	 * Constructor.
	 */
	public BlockingGlassPane(int size) {
		setVisible(false);
		setOpaque (false);

		ipp = InfiniteProgressPanel.createInfinitePanelBeans("");
		ipp.setOpaque(false);
		setLayout(new BorderLayout());
		add(ipp);
		addMouseListener(blockMouse);
	}

	/**
     * 
     */
    public void setText(String message) {
        ipp.setText(message);
    }

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
