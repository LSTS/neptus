/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

/**
 * @author hfq
 *
 */
public abstract class VisAction extends AbstractAction {

    private static final long serialVersionUID = 1985465758698046515L;

    /**
     * 
     */
    public VisAction() {
        super();
    }

    /**
     * @param text
     */
    public VisAction(String text) {
        super(text);
    }

    /**
     * @param text
     * @param icon
     */
    public VisAction(String text, ImageIcon icon) {
        super(text, icon);
    }

    /**
     * @param text
     * @param icon
     * @param description
     */
    public VisAction(String text, ImageIcon icon, String description) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, description);
    }

    /**
     * 
     * @param text
     * @param icon
     * @param description
     * @param mnemonic
     */
    public VisAction(String text, ImageIcon icon, String description, Integer mnemonic) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, description);
        putValue(Action.MNEMONIC_KEY, mnemonic);
    }

    /**
     * 
     * @param text
     * @param icon
     * @param description
     * @param mnemonic
     * @param accelerator
     */
    public VisAction(String text, ImageIcon icon, String description, Integer mnemonic, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, description);
        putValue(Action.MNEMONIC_KEY, mnemonic);
        putValue(Action.ACCELERATOR_KEY, accelerator);        
    }

    /**
     * 
     * @param text
     * @param icon
     * @param description
     * @param accelerator
     */
    public VisAction(String text, ImageIcon icon, String description, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, description);
        putValue(Action.ACCELERATOR_KEY, accelerator);
    }

    /**
     * 
     * @param text
     * @param icon
     * @param accelerator
     */
    public VisAction(String text, ImageIcon icon, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.ACCELERATOR_KEY, accelerator);
    }
}
