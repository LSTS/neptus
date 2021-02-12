/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 2006/11/11
 */
package pt.lsts.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.gui.objparams.ParallelepipedParameters;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Refactored in 06/11/2006.
 * @author Paulo Dias
 * @author Ze Carlos
 */
public class CylinderElement extends GeometryElement
{
    protected Ellipse2D.Double thisEllipse;
    protected boolean isLoadOk = true;
    
    @Override
    public String getType()
    {
        return "Cylinder";
    }
    
    public CylinderElement(MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
    }
    
    public CylinderElement() {
        super();
    }
    
    
    @Override
    public void initialize(ParametersPanel paramsPanel)
    {
        super.initialize(paramsPanel);

        if (!(paramsPanel instanceof ParallelepipedParameters)) {
        }
        else {            
            double pos[] = centerLocation.getOffsetFrom(new LocationType());
            thisEllipse = new Ellipse2D.Double(pos[0]-width/2, pos[1]-height/2, width, length);
        }
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_CYLINDER;
    }
    
    @Override
    public String getTypeAbbrev() {
        return "cyl";
    }
    
    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        double[] offsets = lt.getOffsetFrom(getCenterLocation());
        Point2D pt = new Point2D.Double(offsets[1], -offsets[0]);
        AffineTransform t = AffineTransform.getRotateInstance(getYawRad());
        Shape s = t.createTransformedShape(new Ellipse2D.Double(-getWidth()/2, -getLength()/2, getWidth(), getLength()));
        return s.contains(pt);
    }
    
    //ConsoleLayoutSE
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {
        Point2D pt = renderer.getScreenPosition(getCenterLocation());
        g.translate(pt.getX(), pt.getY());
        g.rotate(getYawRad()-renderer.getRotation());
        
        double widthScaled = width * renderer.getZoom();
        double lengthScaled = length * renderer.getZoom();
        
        Ellipse2D.Double tmp = new Ellipse2D.Double(-widthScaled/2, -lengthScaled/2, widthScaled, lengthScaled);
        
        if (isSelected())
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        else
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        
        if (isFilled())
            g.fill(tmp);
        
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        g.draw(tmp);             
    }
}
