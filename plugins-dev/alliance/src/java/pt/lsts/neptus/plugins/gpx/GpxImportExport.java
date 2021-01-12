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
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jul 25, 2014
 */
package pt.lsts.neptus.plugins.gpx;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.vecmath.Point3d;

import org.alternativevision.gpx.GPXParser;
import org.alternativevision.gpx.beans.GPX;
import org.alternativevision.gpx.beans.Track;
import org.alternativevision.gpx.beans.Waypoint;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
@PluginDescription(name="GPX Support")
public class GpxImportExport extends ConsolePanel {

    private static final long serialVersionUID = 1L;

    /**
     * @param console
     */
    public GpxImportExport(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        removeMenuItem(I18n.text("Tools") + ">" + I18n.text("GPX") + ">" + I18n.text("Import"));
        removeMenuItem(I18n.text("Tools") + ">" + I18n.text("GPX") + ">" + I18n.text("Export"));
    }
    
    private MapType getMap(MissionType mt) {
        MapGroup mg = MapGroup.getMapGroupInstance(mt);
        return mg.getMaps()[0];     
    }

    private void addPath(Collection<LocationType> points, String name) {
        if (points.isEmpty())
            return;
        MissionType mission = getConsole().getMission();
        MapType map = getMap(mission);
        MapGroup mg = MapGroup.getMapGroupInstance(mission);
        AbstractElement existing = null;
        AbstractElement[] elems = mg.getMapObjectsByID(name);
        if (elems != null && elems.length > 0)
            existing = elems[0];
        
        PathElement el = new PathElement();
        
        if (existing != null) {
            NeptusLog.pub().warn("Object with name "+name+" was replaced.");
            existing.getParentMap().remove(name);
        }
        
        el.setCenterLocation(points.iterator().next());
        
        for (LocationType loc : points) {
            double[] offsets = loc.getOffsetFrom(el.getCenterLocation());
            el.addPoint(offsets[1], offsets[0], 0, false);
        }
        el.setFilled(false);
        el.addPoint(0, 0, 0, false);
        el.setMyColor(Color.magenta);        
        el.setId(name);
        map.addObject(el);    
    }
    
    private void addMark(LocationType loc, String name) {
        MissionType mission = getConsole().getMission();
        MapType map = getMap(mission);
        MapGroup mg = MapGroup.getMapGroupInstance(mission);
        AbstractElement existing = null;
        AbstractElement[] elems = mg.getMapObjectsByID(name);
        if (elems != null && elems.length > 0)
            existing = elems[0];
        
        MarkElement el = new MarkElement(map.getMapGroup(), map);
        
        if (existing != null) {
            if (existing instanceof MarkElement) {
                el = (MarkElement)existing;
                NeptusLog.pub().warn("Object with name "+name+" got a position update");
            }
            else {
                NeptusLog.pub().warn("Object with name "+name+" was removed from the map");
                existing.getParentMap().remove(name);
            }
        }
        el.setId(name);
        el.setCenterLocation(loc);
        map.addObject(el);        
    }
    
    @Override
    public void initSubPanel() {
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("GPX") + ">" + I18n.text("Import"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                JFileChooser chooser = GuiUtils.getFileChooser(ConfigFetch.getUserHomeFolder(), I18n.text("GPX files"), "gpx");
                chooser.setDialogTitle(I18n.text("Select GPX file to import"));
                int op = chooser.showOpenDialog(getConsole());
                if (op != JFileChooser.APPROVE_OPTION)
                    return;
                
                try {
                    GPXParser parser = new GPXParser();
                    GPX gpx = parser.parseGPX(new FileInputStream(chooser.getSelectedFile()));
                    
                    for (Waypoint wpt : gpx.getWaypoints()) {
                        LocationType loc = new LocationType(wpt.getLatitude(), wpt.getLongitude());
                        addMark(loc, wpt.getName());
                    }
                    
                    for (Track t : gpx.getTracks()) {
                        Vector<LocationType> locs = new Vector<>();
                        for (Waypoint wpt : t.getTrackPoints()) {
                            locs.add(new LocationType(wpt.getLatitude(), wpt.getLongitude()));
                        }
                        addPath(locs, t.getName());
                    }
                    
                    getConsole().getMission().save(true);
                    getConsole().warnMissionListeners();                    
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);                    
                }
            }
        });
        
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("GPX") + ">" + I18n.text("Export"), null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select GPX file to export");
                    chooser.setFileFilter(GuiUtils.getCustomFileFilter("GPX files", "gpx"));
                    int op = chooser.showSaveDialog(getConsole());
                    if (op != JFileChooser.APPROVE_OPTION)
                        return;
                    
                    GPX gpx = new GPX();
                    
                    MapGroup map = MapGroup.getMapGroupInstance(getConsole().getMission());
                    
                    for (MarkElement el : map.getAllObjectsOfType(MarkElement.class)) {
                        Waypoint wpt = new Waypoint();
                        LocationType loc = new LocationType(el.getCenterLocation());
                        loc.convertToAbsoluteLatLonDepth();
                        wpt.setLatitude(loc.getLatitudeDegs());
                        wpt.setLongitude(loc.getLongitudeDegs());
                        wpt.setName(el.getId());
                        gpx.addWaypoint(wpt);
                    }
                    
                    for (PathElement el : map.getAllObjectsOfType(PathElement.class)) {
                        Track t = new Track();
                        t.setName(el.getId());
                        
                        ArrayList<Waypoint> wpts = new ArrayList<>();
                        for (Point3d p : el.getPoints()) {
                            LocationType loc = new LocationType(el.getCenterLocation());
                            loc.translatePosition(p.x, p.y, 0);
                            loc.convertToAbsoluteLatLonDepth();
                            Waypoint wpt = new Waypoint();
                            wpt.setLatitude(loc.getLatitudeDegs());
                            wpt.setLongitude(loc.getLongitudeDegs());
                            wpts.add(wpt);
                        }
                        t.setTrackPoints(wpts);
                        gpx.addTrack(t);
                    }
                    gpx.setCreator("Neptus");
                    GPXParser parser = new GPXParser();
                    parser.writeGPX(gpx, new FileOutputStream(chooser.getSelectedFile()));
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        });
    }
}
