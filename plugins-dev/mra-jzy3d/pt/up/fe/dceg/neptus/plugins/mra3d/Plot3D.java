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
 * Dec 15, 2012
 * $Id:: Plot3D.java 9671 2013-01-07 11:04:55Z mfaria                           $:
 */
package pt.up.fe.dceg.neptus.plugins.mra3d;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.global.Settings;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.legends.colorbars.ColorbarLegend;

import pt.up.fe.dceg.neptus.colormap.DataDiscretizer;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer.DataPoint;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

/**
 * @author zp
 */
@PluginDescription(name = "3D Plot", icon = "pt/up/fe/dceg/neptus/plugins/mra3d/3d.png")
public class Plot3D extends SimpleMRAVisualization implements LogMarkerListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected boolean inited = false;
    protected Chart chart = null;
    protected JToggleButton zExaggerationToggle, bathymetryToggle, pathToggle, markersToggle; 
    protected Shape surface = null;
    protected LineStrip path = null;
    protected Scatter gpsScatter = null;
    protected LocationType ref;
    protected Vector<Marker3d> markers = new Vector<>();
    
    public Plot3D(MRAPanel panel) {
        super(panel);
        this.panel = panel;
        setLayout(new BorderLayout());
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("EstimatedState") != null;
    }


    protected void addChart() {
        Settings.getInstance().setHardwareAccelerated(true);
        chart = new Chart(Quality.Advanced, "swing");
        ref = IMCUtils.getLocation(source.getLsfIndex().getMessage(
                source.getLsfIndex().getFirstMessageOfType("EstimatedState")));

        // VEHICLE PATH
        path = new LineStrip();
        Coord3d lastCoord = new Coord3d();

        for (IMCMessage m : source.getLsfIndex().getIterator("EstimatedState", 0, 1000)) {
            LocationType loc = IMCUtils.getLocation(m);
            double offsets[] = loc.getOffsetFrom(ref);
            double phi = Math.min(Math.toDegrees(m.getDouble("theta")), 10);
            phi = Math.max(phi, -10);
            int sb = phi <= 0 ? 0 : (int) ((phi / 10) * 255);
            int bb = phi >= 0 ? 0 : (int) ((phi / -10) * 255);
            double depth = -loc.getDepth();
            if (m.getTypeOf("ref") != null)
                depth = -m.getDouble("z");

            lastCoord = new Coord3d(-offsets[0], offsets[1], depth);
            path.add(new Point(lastCoord, new Color(bb, sb, 0), 1f));
        }

        path.add(new Point(lastCoord, new Color(0, 0, 0, 0), 0f));
        path.add(new Point(new Coord3d(0, 0, 0), new Color(0, 0, 0, 0), 0f));
        path.setWidth(2f);
        chart.getScene().add(path);

        // BATHYMETRY
        Thread t = new Thread(new Runnable() {
            int bathymCellWidth = 1;
            DataDiscretizer dd = new DataDiscretizer(bathymCellWidth);

            @Override
            public void run() {

                // legacy code...
                if (source.getLog("BottomDistance") != null) {
                    int dvlId = source.getLsfIndex().getEntityId("DVL");
                    int bDistanceId = source.getLsfIndex().getDefinitions().getMessageId("BottomDistance");
                    int eStateId = source.getLsfIndex().getDefinitions().getMessageId("EstimatedState");
                    int lastStateIndex = 0;

                    for (int i = source.getLsfIndex().getNextMessageOfEntity(bDistanceId, dvlId, 0); i != -1; i = source
                            .getLsfIndex().getNextMessageOfEntity(bDistanceId, dvlId, i)) {

                        IMCMessage bDistance = source.getLsfIndex().getMessage(i);

                        int stateIndex = source.getLsfIndex().getMessageAtOrAfer(eStateId, 0xFF, lastStateIndex,
                                bDistance.getTimestamp());
                        if (stateIndex == -1)
                            continue;
                        IMCMessage state = source.getLsfIndex().getMessage(stateIndex);

                        LocationType l = new LocationType();
                        l.setLatitudeRads(state.getDouble("lat"));
                        l.setLongitudeRads(state.getDouble("lon"));
                        l.translatePosition(state.getDouble("x"), state.getDouble("y"), 0);
                        double offsets[] = l.getOffsetFrom(ref);
                        lastStateIndex = stateIndex;
                        dd.addPoint(-offsets[0], offsets[1], -state.getDouble("z") - bDistance.getDouble("value"));

                    }
                }
                else {
                    for (IMCMessage state : source.getLsfIndex().getIterator("EstimatedState", 0, 100)) {
                        if (state.getTypeOf("alt") != null) {
                            double alt = state.getDouble("alt");
                            double depth = state.getDouble("depth");
                            double pitch = Math.toDegrees(state.getDouble("theta"));

                            if (alt > 1 && depth > 1 && Math.abs(pitch) < 4) {
                                LocationType loc = IMCUtils.getLocation(state);
                                double offsets[] = loc.getOffsetFrom(ref);
                                dd.addPoint(-offsets[0], offsets[1], -alt - depth);
                            }
                        }
                    }
                }

                DataPoint[] data = dd.getDataPoints();
                if (data.length == 0)
                    return;
                ArrayList<Coord3d> coords = new ArrayList<>();

                for (DataPoint p : data) {
                    coords.add(new Coord3d(p.getPoint2D().getX(), p.getPoint2D().getY(), p.getValue()));
                }
                surface = Builder.buildDelaunay(coords);
                surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface
                        .getBounds().getZmax(), new Color(1, 0.5f, 0.5f, 0.5f)));
                surface.setWireframeDisplayed(false);
                surface.setLegend(new ColorbarLegend(surface, chart.getView().getAxe().getLayout().getZTickProvider(),
                        chart.getView().getAxe().getLayout().getZTickRenderer()));
                surface.setLegendDisplayed(true);

                //chart.getScene().add(surface);
                bathymetryToggle.setEnabled(true);
            };
        });

        t.setDaemon(true);
        t.start();

        // GPS FIXES
        Vector<Coord3d> gpsFixes = new Vector<>();
        for (IMCMessage fix : source.getLsfIndex().getIterator("GpsFix")) {
            if (fix.getBitmask("validity").get("VALID_POS")) {
                LocationType l = new LocationType();
                l.setLatitudeRads(fix.getDouble("lat"));
                l.setLongitudeRads(fix.getDouble("lon"));
                double offsets[] = l.getOffsetFrom(ref);
                if (!path.getBounds().getXRange().contains(offsets[0]))
                    continue;
                if (!path.getBounds().getYRange().contains(offsets[1]))
                    continue;

                gpsFixes.add(new Coord3d(-offsets[0], offsets[1], 0));
            }
        }
        gpsScatter = new Scatter(gpsFixes.toArray(new Coord3d[0]), new Color(100, 100, 200, 128), 5);
        chart.getScene().add(gpsScatter);

        add((Component) chart.getCanvas());
        chart.getView().setMaximized(true);
        chart.render();
        
        ChartLauncher.configureControllers(chart, "chart", true, false);
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        return this;
    }
    
    protected JPanel createToolbar() {
        JPanel toolbar = new JPanel();
        zExaggerationToggle = new JToggleButton(I18n.text("Z Exaggeration"));
        zExaggerationToggle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                chart.getView().setSquared(zExaggerationToggle.isSelected());
            }
        });
        zExaggerationToggle.setSelected(true);
        toolbar.add(zExaggerationToggle);

        bathymetryToggle = new JToggleButton(I18n.text("Bathymetry"));
        bathymetryToggle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (bathymetryToggle.isSelected())
                    chart.getScene().add(surface);
                else
                    chart.getScene().remove(surface);
            }
        });
        bathymetryToggle.setEnabled(false);
        toolbar.add(bathymetryToggle);

        pathToggle = new JToggleButton(I18n.text("Vehicle path"));
        pathToggle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pathToggle.isSelected()) {
                    chart.getScene().add(path);
                    chart.getScene().add(gpsScatter);
                }
                else {
                    chart.getScene().remove(path);
                    chart.getScene().remove(gpsScatter);
                }
            }
        });
        pathToggle.setSelected(true);
        toolbar.add(pathToggle);
        
        
        markersToggle = new JToggleButton(I18n.text("Markers"));
        markersToggle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (markersToggle.isSelected()) {
                    for (Marker3d m : markers)
                        chart.getScene().add(m);
                }
                else {
                    for (Marker3d m : markers)
                        chart.getScene().remove(m);
                }
            }
        });
        markersToggle.setSelected(true);
        toolbar.add(markersToggle);
        return toolbar;
    }

    @Override
    public void onCleanup() {
        super.onCleanup();
        if (chart != null) {
            chart.stopAnimator();
            chart.clear();
            chart.dispose();
        }
    }

    @Override
    public void onHide() {
        super.onHide();
        chart.stopAnimator();
        chart.clear();
        chart.dispose();
        removeAll();
        chart = null;
    }

    @Override
    public void onShow() {
        super.onShow();
        addChart();
        if (super.panel != null) {
            for (LogMarker m : super.panel.getMarkers())
                addLogMarker(m);
        }
        add(createToolbar(), BorderLayout.SOUTH);        
    }
    
    @Override
    public void addLogMarker(LogMarker marker) {
        if (chart == null)
            return;
        IMCMessage state = source.getLsfIndex().getMessageAtOrAfter("EstimatedState", 0, 0xFF, marker.timestamp/1000);
        if (state == null)
            return;
        double depth = state.getDouble("depth");
        if (state.getTypeOf("alt") == null)
            depth = state.getDouble("z");
        
        LocationType location = IMCUtils.getLocation(state);
        // System.out.println("[Plot3D] Location for " + marker.label + " " + marker.timestamp + ": ("
        // + location.getLatitude() + ", "
        // + location.getLongitude() + ")");
        double[] xyz = location.getOffsetFrom(ref);
        Marker3d m = new Marker3d(marker.label, new Coord3d(-xyz[0], xyz[1], -depth), java.awt.Color.black);
        markers.add(m);
        chart.getScene().add(m);
    }
    
    @Override
    public void GotoMarker(LogMarker marker) {
        
    }
    
    @Override
    public void removeLogMarker(LogMarker marker) {
        for (int i = 0; i < markers.size(); i++) {
            if (markers.get(i).label.equals(marker.label)) {
                chart.getScene().remove(markers.get(i));
                markers.remove(i);
                i--;
            }
        }
    }
    
    
}
