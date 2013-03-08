/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by canasta
 * 23 de Mai de 2012
 * $Id:: UavFramePainter.java 9846 2013-02-02 03:32:12Z robot                   $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.background;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;

/**
 * @author canasta
 *
 */
public class UavFramePainter implements IUavPainter {
    
    private Hashtable<String,Object> receivedArgs;  
    private BufferedImage frameImage; 
    private int rotation = 0;
    
    //IUavPainter_BEGIN
    @SuppressWarnings("unchecked")
    @Override
    public void paint(Graphics2D g, int width, int height, Object args) {
        
        receivedArgs = (Hashtable<String, Object>) args;
        
        if(!receivedArgs.isEmpty()){
        
            frameImage = (BufferedImage) receivedArgs.get("image");
            rotation = (Integer) receivedArgs.get("image rotation");
            
            AffineTransform tx = new AffineTransform();
            tx.rotate(Math.toRadians(rotation), frameImage.getWidth() / 2, frameImage.getHeight() / 2);

            AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_BILINEAR);
            frameImage = op.filter(frameImage, null);
            
            g.drawImage(frameImage, 0, 0, width, height, null);
        }
    }
    //IUavPainter_END
}
