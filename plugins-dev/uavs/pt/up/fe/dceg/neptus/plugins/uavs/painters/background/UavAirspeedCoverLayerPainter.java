/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Christian Fuchs
 * 16.11.2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.background;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;

/**
 * @author Christian Fuchs
 *
 */
public class UavAirspeedCoverLayerPainter implements Renderer2DPainter, IUavPainter{

    //------Implemented Interfaces------//
    
    //Renderer2DPainter_BEGIN
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        g.setPaint(Color.white.darker());        
        g.fill(new Rectangle.Double(0, 0, renderer.getWidth(), renderer.getHeight()));      
    }
    //Renderer2DPainter_END

    //IUavPainter_BEGIN
    @Override
    public void paint(Graphics2D g, int width, int heigth, Object args) {
        
        g.setPaint(Color.white.darker());         
        g.fill(new Rectangle.Double(0, 0, width, heigth));   
        
    }
    //IUavPainter_END
}
