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
 * 2006/11/11
 * $Id:: EllipsoidElement.java 9845 2013-02-01 19:53:46Z pdias            $:
 */
package pt.up.fe.dceg.neptus.types.map;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.gui.objparams.ParallelepipedParameters;
import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * Refactored in 06/11/2006.
 * @author Paulo Dias
 * @author Ze Carlos
 */
public class EllipsoidElement extends GeometryElement
{
    protected Ellipse2D.Double thisEllipse;
    
    @Override
    public String getType()
    {
        return "Ellipsoid";
    }

    public EllipsoidElement()
    {
        super();
    }
    
    public EllipsoidElement(MapGroup mg, MapType parentMap) {
        super(mg, parentMap);
        if (mg != null)
            setCenterLocation(new LocationType(mg.getHomeRef().getCenterLocation()));
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
        return ELEMENT_TYPE.TYPE_ELLIPSOID;
    }


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
        
        g.fill(tmp);
        
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        g.draw(tmp);   
    }
}
