/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;

import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class SamplePointTask extends MVPlannerTask {

    private MarkElement elem;
    private Image poiImg = null;
    
    public SamplePointTask(LocationType loc) {
        elem = new MarkElement();
        elem.setId(getName());
        elem.setCenterLocation(loc);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        String payloads = getPayloadsAbbreviated();
        if (poiImg == null)
            poiImg = ImageUtils.getImage("pt/lsts/neptus/plugins/pddl/led.png");
        Point2D pt = renderer.getScreenPosition(elem.getCenterLocation());
        g.drawImage(poiImg, (int)pt.getX()-8, (int)pt.getY()-8, null);
        g.setColor(Color.black);
        g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+8, (int)pt.getY()+8);
        g.setColor(Color.green.brighter().brighter());
        g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+7, (int)pt.getY()+7);
    }

    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        return elem.containsPoint(lt, renderer);
    }

    @Override
    public void translate(double offsetNorth, double offsetEast) {
        elem.translate(offsetNorth, offsetEast, 0);
    }
    
    public LocationType getLocation() {
        return elem.getCenterLocation().convertToAbsoluteLatLonDepth();
    }


    @Override
    public void rotate(double amountRads) {
        // nothing
    }
    
    @Override
    public void setYaw(double yawRads) {
        // nothing
    }

    @Override
    public void growWidth(double amount) {
        // nothing
    }

    @Override
    public void growLength(double amount) {
        // nothing
    }
    
    @Override
    public LocationType getCenterLocation() {
        return getLocation();
    }
}
