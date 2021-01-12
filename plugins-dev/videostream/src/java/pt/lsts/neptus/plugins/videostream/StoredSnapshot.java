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
 * 18/05/2016
 */
package pt.lsts.neptus.plugins.videostream;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXB;

import pt.lsts.neptus.mp.preview.payloads.CameraFOV;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class StoredSnapshot implements Serializable {
    
    private static final long serialVersionUID = 6711177101700850479L;
    public double latDegs, lonDegs;
    public Point2D.Double imgPoint= new Point2D.Double(0, 0);
    public Point2D.Double groundQuad[];
    public String description = "";
    transient public BufferedImage capture;
    public Date timestamp;
    
    private static final String logDir = "log/cam/";
    private static ArrayList<StoredSnapshot> snapshots = new ArrayList<>();
    
    @SuppressWarnings("unused")
    private StoredSnapshot() {
        
    }
    
    public StoredSnapshot(String description, LocationType worldloc, Point2D imgLoc, BufferedImage image, Date timestamp) {
        worldloc.convertToAbsoluteLatLonDepth();
        latDegs = worldloc.getLatitudeDegs();
        lonDegs = worldloc.getLongitudeDegs();
        this.imgPoint.setLocation(imgLoc);
        this.timestamp = timestamp;        
        this.capture = image;
        this.description = description;
    }
    
    public void setCamFov(CameraFOV camFov) {
        
        ArrayList<LocationType> quad = camFov.getFootprintQuad();
        groundQuad = new Point2D.Double[quad.size()];
        
        for (int i = 0; i < quad.size(); i++) {
            LocationType l = quad.get(i).convertToAbsoluteLatLonDepth();
            groundQuad[i] = new Point2D.Double(l.getLatitudeDegs(), l.getLongitudeDegs());
        }
    }
    
    public void store() throws IOException {
        new File("log/cam").mkdirs();
        String filename = String.format(logDir+"%d", timestamp.getTime()/1000);
        ImageIO.write(capture, "JPEG", new File(filename+".jpeg"));
        JAXB.marshal(this, new File(filename+".xml"));
        snapshots.add(this);
    }
    
    public static Collection<StoredSnapshot> getSnapshots(File logDir) throws IOException {
        ArrayList<StoredSnapshot> snapshots = new ArrayList<>();
        if (!logDir.isDirectory())
            logDir.mkdirs();
        
        for (File f : logDir.listFiles())
            if (f.getName().endsWith(".xml"))
                snapshots.add(load(f));                                                            
        return snapshots;
    }

    /**
     * @return the snapshots
     */
    public static Collection<StoredSnapshot> getSnapshots() {
        return Collections.unmodifiableCollection(snapshots);
    }

    public static StoredSnapshot load(File xmlFile) throws IOException {
        StoredSnapshot snap = JAXB.unmarshal(xmlFile, StoredSnapshot.class);
        String imgFile = xmlFile.getAbsolutePath();
        imgFile = imgFile.substring(0, imgFile.length()-4);
        imgFile += ".jpeg";
        snap.capture = ImageIO.read(new File(imgFile));
        return snap;
    }
    
    public static void loadLogSnapshots() throws IOException {
        synchronized (snapshots) {
            snapshots.clear();
            snapshots.addAll(StoredSnapshot.getSnapshots(new File(logDir)));   
        }
    }
    
    
}
