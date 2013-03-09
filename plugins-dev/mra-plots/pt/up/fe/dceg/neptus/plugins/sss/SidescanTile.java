/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Dec 14, 2012
 */
package pt.up.fe.dceg.neptus.plugins.sss;

import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

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
        dos.writeDouble(downCenter.getLatitudeAsDoubleValue());
        dos.writeDouble(downCenter.getLongitudeAsDoubleValue());
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
