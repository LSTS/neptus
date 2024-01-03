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
 * Author: zp
 * Jan 21, 2014
 */
package pt.lsts.neptus.console;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;

import org.dom4j.Attribute;
import org.dom4j.Element;

import pt.lsts.neptus.renderer2d.StateRenderer2D;

/**
 * @author zp
 *
 */
public abstract class ConsoleLayer extends AbstractConsolePlugin implements IConsoleLayer {

    private float opacity = 1.0f;
    
    public abstract boolean userControlsOpacity();
    public abstract void initLayer();
    public abstract void cleanLayer();

    @Override
    public float getOpacity() {
        return opacity;
    }
    
    public void setVisible(boolean visible) {
        if (!visible)
            setOpacity(0f);
        else if (opacity <= 0)
            opacity = 1.0f;
    }
    
    public boolean isVisible() {
        return getOpacity() > 0;
    }    

    @Override
    public void setOpacity(float opacity) {
        this.opacity = opacity;                 
    }

    @Override
    protected final void initPlugin(ConsoleLayout console) {
        initLayer();
    }
    
    @Override
    public final void clean() {
        super.clean();
        cleanLayer();
    }
    
    public AlphaComposite getComposite() {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (opacity <= 0)
            return;
        if (opacity < 1) {
            g.setComposite(getComposite());
        }
    }
    
    @Override
    public Element asElement(String rootElement) {        
        Element el = super.asElement(rootElement);
        el.addAttribute("opacity", ""+getOpacity());
        return el;
    }
    
    @Override
    public void parseXmlElement(Element elem) {
        Attribute attr = elem.attribute("opacity");
        if (attr != null)
            setOpacity(Float.parseFloat(attr.getText()));
        
        super.parseXmlElement(elem);      
    }
}
