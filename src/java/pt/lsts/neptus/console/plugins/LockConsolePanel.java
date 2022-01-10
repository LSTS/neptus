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
 * Author: Paulo Dias
 * 2009/03/25
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.MiniButton;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(icon = "images/menus/lock.png", name = "Console Lock Button", documentation = "lock/lock-button.html")
public class LockConsolePanel extends ConsolePanel implements ActionListener {

    private final ImageIcon DEFAULT_ICON = new ImageIcon(ImageUtils.getImage("images/menus/lock.png"));
    private final ImageIcon LOCK_ICON_FILENAME = ImageUtils.getIcon("images/menus/lock.png");
    private ImageIcon icon;
    
    protected MiniButton lockButton = null;

    /**
     * @param name
     */
    public LockConsolePanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        icon = DEFAULT_ICON;
        this.setLayout(new BorderLayout());
        this.add(getLockButton(), BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(40, 40));
        this.setSize(40, 40);
    }

    @Override
    public ImageIcon getImageIcon() {
        return icon;
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

    @Override
    public void initSubPanel() {
        
    }

    @Override
    public void cleanSubPanel() {
        
    }
}
