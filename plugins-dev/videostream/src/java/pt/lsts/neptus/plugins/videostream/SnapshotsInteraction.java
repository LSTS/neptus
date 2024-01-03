/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Camera Snapshots Interaction")
public class SnapshotsInteraction extends ConsoleInteraction {

    private StoredSnapshot pivot = null;
            
    @Override
    public void initInteraction() {
        try {
            StoredSnapshot.loadLogSnapshots();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void cleanInteraction() {
        
    }
    
    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        ArrayList<StoredSnapshot> snaps = new ArrayList<>();
        snaps.addAll(StoredSnapshot.getSnapshots());
        
        for (StoredSnapshot snap : snaps) {
            LocationType loc = new LocationType(snap.latDegs, snap.lonDegs);
            LocationType mouse = source.getRealWorldLocation(event.getPoint());
            double dist = loc.getPixelDistanceTo(mouse, source.getLevelOfDetail());
            if (dist < 5) {
                pivot = snap;
                return;
            }
        }
        pivot = null;
    }
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(source.getIdentity());
        ArrayList<StoredSnapshot> snaps = new ArrayList<>();
        snaps.addAll(StoredSnapshot.getSnapshots());
        
        for (StoredSnapshot snap : snaps) {
            LocationType loc = new LocationType(snap.latDegs, snap.lonDegs);
            Point2D pt = source.getScreenPosition(loc);
            g.setColor(Color.white);
            g.fill(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));
            g.setColor(Color.blue.darker());
            g.fill(new Ellipse2D.Double(pt.getX()-2.5, pt.getY()-2.5, 5, 5));            
        }
        
        if (pivot != null) {
            Graphics2D g2 = (Graphics2D) pivot.capture.getGraphics();
            g2.setStroke(new BasicStroke(4f));
            g2.setColor(Color.white);
            g2.draw(new Line2D.Double(pivot.imgPoint.x - 10, pivot.imgPoint.y, pivot.imgPoint.x + 10, pivot.imgPoint.y));
            g2.draw(new Line2D.Double(pivot.imgPoint.x, pivot.imgPoint.y - 10, pivot.imgPoint.x, pivot.imgPoint.y + 10));
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(Color.red);
            g2.draw(new Line2D.Double(pivot.imgPoint.x - 8, pivot.imgPoint.y, pivot.imgPoint.x + 8, pivot.imgPoint.y));
            g2.draw(new Line2D.Double(pivot.imgPoint.x, pivot.imgPoint.y - 8, pivot.imgPoint.x, pivot.imgPoint.y + 8));
            
            if (pivot.groundQuad != null) {
                PathElement groundQuad = new PathElement();
                groundQuad.setMyColor(Color.blue);
                ArrayList<LocationType> locs = new ArrayList<>();
                for (Point2D.Double pt : pivot.groundQuad)
                    locs.add(new LocationType(pt.getX(), pt.getY()));
                
                groundQuad.setCenterLocation(locs.get(0));
                for (LocationType l : locs)
                    groundQuad.addPoint(l);
                groundQuad.paint(g, source, source.getRotation());   
            }
            
            Image scaled = ImageUtils.getScaledImage(pivot.capture, 200, 200, false);
            g.drawImage(scaled, source.getWidth() - scaled.getWidth(source) - 5,
                    source.getHeight() - scaled.getHeight(source) - 5, source);
        }
    }

}
