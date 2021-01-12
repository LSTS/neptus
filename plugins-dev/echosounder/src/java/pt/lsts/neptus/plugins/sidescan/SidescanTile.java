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
 * Author: José Pinto
 * Dec 14, 2012
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 */
public class SidescanTile {

    protected static final int version = 1;

    protected BufferedImage tile;
    protected LocationType downCenter;
    protected double rotation;
    protected double width, length;

    public SidescanTile(BufferedImage image, LocationType loc, double width, double rotation, double length) {
        this.tile = image;
        this.width = width;
        this.length = length;
        this.rotation = rotation;
        downCenter = new LocationType(loc);        
    }

    public SidescanTile(File source) throws Exception {
        FileInputStream fis = new FileInputStream(source);
        DataInputStream dis = new DataInputStream(fis);
        int version = dis.readInt();
        if (!(version == SidescanTile.version)) {
            fis.close();
            throw new Exception("Incompatible format");
        }
        downCenter = new LocationType(dis.readDouble(), dis.readDouble());
        rotation = dis.readDouble();
        width = dis.readDouble();
        length = dis.readDouble();
        tile = ImageIO.read(fis);
        fis.close();
    }

    public void saveToFile(File destination) throws Exception {
        FileOutputStream output = new FileOutputStream(destination);
        DataOutputStream dos = new DataOutputStream(output);
        dos.writeInt(version);
        downCenter.convertToAbsoluteLatLonDepth();
        dos.writeDouble(downCenter.getLatitudeDegs());
        dos.writeDouble(downCenter.getLongitudeDegs());
        dos.writeDouble(rotation);
        dos.writeDouble(width);
        dos.writeDouble(length);
        ImageIO.write(Scalr.resize(tile, Mode.FIT_EXACT, 300, (int)((300/width)*length)), "png", output);
        output.close();
    }
    
    /**
     * @return the tile
     */
    public BufferedImage getTile() {
        return tile;
    }

    /**
     * @param tile the tile to set
     */
    public void setTile(BufferedImage tile) {
        this.tile = tile;
    }

    /**
     * @return the center
     */
    public LocationType getBottomCenter() {
        return downCenter;
    }

    /**
     * @param center the center to set
     */
    public void setBottomCenter(LocationType center) {
        this.downCenter = center;
    }

    /**
     * @return the rotation
     */
    public double getRotation() {
        return rotation;
    }

    /**
     * @param rotation the rotation to set
     */
    public void setRotation(double rotation) {
        this.rotation = rotation;
    }      
}
