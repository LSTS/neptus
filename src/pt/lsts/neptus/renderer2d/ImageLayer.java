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
 * Author: zp
 * Nov 25, 2013
 */
package pt.lsts.neptus.renderer2d;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ImageLayer implements Serializable, Renderer2DPainter {

    private static final long serialVersionUID = -3596078283131291222L;
    private String name;
    private LocationType topLeft;
    private double zoom, transparency = 0.3;
    transient private BufferedImage image;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Point2D tl = renderer.getScreenPosition(topLeft);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.translate(tl.getX(), tl.getY());
        g.draw(new Line2D.Double(-3, -3, 3, 3));
        g.draw(new Line2D.Double(-3, 3, 3, -3));
        g.scale(renderer.getZoom()*zoom, renderer.getZoom()*zoom);
        g.rotate(-renderer.getRotation());
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)transparency));
        g.drawImage(image, 0, 0, null);
    }
    
    public ImageLayer(String name, BufferedImage img, LocationType topLeft, LocationType bottomRight) {
        this.name = name;
        this.topLeft = new LocationType(topLeft);
        this.image = img;
        this.zoom = topLeft.getOffsetFrom(bottomRight)[0] / img.getHeight();
    }
    
    
    /**
     * @return the image
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * @return the topLeft
     */
    public LocationType getTopLeft() {
        return new LocationType(topLeft);
    }
    
    public LocationType getBottomRight() {
        LocationType loc = new LocationType(topLeft);
        loc.translatePosition( - image.getHeight() * zoom, image.getWidth() * zoom, 0);
        return loc;
    }

    public static ImageLayer read(File f) throws Exception {
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
        ImageLayer imgLayer = (ImageLayer) is.readObject();
        is.close();
        return imgLayer;        
    }
    
    public void saveToFile(File f) throws Exception {
        ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f));
        os.writeObject(this);
        os.close();
    }

    
    /**
     * @return the transparency
     */
    public double getTransparency() {
        return transparency;
    }

    /**
     * @param transparency the transparency to set
     */
    public void setTransparency(double transparency) {
        this.transparency = transparency;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        image = ImageIO.read(in);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageIO.write(image, "PNG", out);
    }    
}
