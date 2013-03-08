/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Christian Fuchs
 * 16.11.2012
 * $Id:: UavAirspeedCoverLayerPainter.java 9846 2013-02-02 03:32:12Z robot      $:
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
