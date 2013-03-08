/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2009/03/25
 * $Id:: LockConsolePanel.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.MiniButton;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(icon = "images/menus/lock.png", name = "Console Lock Button", documentation = "lock/lock-button.html")
public class LockConsolePanel extends SubPanel implements ActionListener {

    public static final ImageIcon DEFAULT_ICON = new ImageIcon(ImageUtils.getImage("images/menus/lock.png"));
    public static final ImageIcon LOCK_ICON_FILENAME = ImageUtils.getIcon("images/menus/lock.png");

    protected MiniButton lockButton = null;

    /**
     * @param name
     */
    public LockConsolePanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        imageIcon = DEFAULT_ICON;
        this.setLayout(new BorderLayout());
        this.add(getLockButton(), BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(40, 40));
        this.setSize(40, 40);
    }

    @Override
    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    /**
     * @return the lockButton
     */
    protected MiniButton getLockButton() {
        if (lockButton == null) {
            lockButton = new MiniButton();
            lockButton.setToggle(true);
            lockButton.setPreferredSize(new Dimension(32, 32));
            lockButton.setIcon(LOCK_ICON_FILENAME);
            lockButton.setActionCommand("lock");
            lockButton.addActionListener(this);
            lockButton.setToolTipText("Lock the lockable components");
        }
        return lockButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getConsole() == null)
            return;

        boolean lock = (getLockButton().getState()) ? true : false;
        for (LockableSubPanel lsp : getConsole().getSubPanelsOfInterface(LockableSubPanel.class)) {
            try {
                // LockableSubPanel lsp = (LockableSubPanel) sp;
                if (lock)
                    lsp.lock();
                else
                    lsp.unLock();
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
}
