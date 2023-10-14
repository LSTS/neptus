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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * May 8, 2018
 */
package pt.lsts.neptus.gui.swing;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSpinnerUI;

/**
 * UI to be added to {@link JSpinner} in order to check before a {@link ChangeListener} to check 
 * if the spinner is being changed or the value is final (similar as the {@link JSlider}).
 * 
 * <p>
 * Use as (e.g.):
 * </p>
 * <br/>
 * <code>
 *   JSpinner spinnerMax = new JSpinner(new SpinnerNumberModel(50, -0, 100, 10));<br/>
 *   SpinnerIsAdjustingUI spinnerMaxUIIsAdjustingUI = new SpinnerIsAdjustingUI();<br/>
 *   spinnerMax.setUI(spinnerMaxUIIsAdjustingUI);<br/>
 *   spinnerMax.addChangeListener(new ChangeListener() {<br/>
 *   &nbsp;&nbsp;@Override<br/>
 *   &nbsp;&nbsp;public void stateChanged(ChangeEvent e) {<br/>
 *   &nbsp;&nbsp;&nbsp;&nbsp;double val = (double) spinnerMax.getValue();<br/>
 *   &nbsp;&nbsp;&nbsp;&nbsp;if (!spinnerMaxUIIsAdjustingUI.getValueIsAdjusting()) {<br/>
 *   &nbsp;&nbsp;&nbsp;&nbsp;...
 * </code>
 * 
 * @author pdias
 *
 */
public class SpinnerIsAdjustingUI extends BasicSpinnerUI {
    private boolean valueIsAdjusting = false;;

    public boolean getValueIsAdjusting() {
        return valueIsAdjusting;
    }
    
    @Override
    protected Component createNextButton() {
        JButton nextbutton = (JButton) super.createNextButton();
        MouseListener[] ml = nextbutton.getMouseListeners();
        for (MouseListener l : ml)
            nextbutton.removeMouseListener(l);
        nextbutton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                valueIsAdjusting = true;
            }
            public void mouseReleased(MouseEvent me) {
                valueIsAdjusting = false;
            }
        });
        for (MouseListener l : ml)
            nextbutton.addMouseListener(l);
        return nextbutton;
    }

    @Override
    protected Component createPreviousButton() {
        JButton previousButton = (JButton) super.createPreviousButton();
        MouseListener[] ml = previousButton.getMouseListeners();
        for (MouseListener l : ml)
            previousButton.removeMouseListener(l);
        previousButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                valueIsAdjusting = true;
            }

            public void mouseReleased(MouseEvent me) {
                valueIsAdjusting = false;
            }
        });
        for (MouseListener l : ml)
            previousButton.addMouseListener(l);
        return previousButton;
    }
}