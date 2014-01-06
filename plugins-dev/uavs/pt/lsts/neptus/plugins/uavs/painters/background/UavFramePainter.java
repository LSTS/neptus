/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: canasta
 * 23 de Mai de 2012
 */
package pt.lsts.neptus.plugins.uavs.painters.background;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;

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
