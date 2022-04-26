package pt.lsts.neptus.plugins.aismanager.layer;

import pt.lsts.aismanager.ShipAisSnapshot;
import pt.lsts.aismanager.api.AisContactManager;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@PluginDescription(name = "Ais Path Monitor", description = "Display Ais contact's trajectory")
public class AisPathMonitor extends ConsoleInteraction {
    @NeptusProperty(name = "Time Offset (minutes)", description = "All the the time offsets in which to project a position")
    public long[] timeOffsetsMinutes = {5, 10, 15, 20, 30};

    private final int clickDistanceError = 10;
    private final AisContactManager aisManager = AisContactManager.getInstance();
    private final HashSet<Integer> targetAisMmsi = new HashSet<>();

    @Override
    public void initInteraction() {

    }

    @Override
    public void cleanInteraction() {

    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        synchronized (targetAisMmsi) {
            List<ShipAisSnapshot> ships = aisManager.getShips();
            for (ShipAisSnapshot ais : ships) {
                if (!targetAisMmsi.contains(ais.getMmsi()))
                    continue;

                double latDegs = Math.toDegrees(ais.getLatRads());
                double lonDegs = Math.toDegrees(ais.getLonRads());

                LocationType aisLoc = new LocationType(latDegs, lonDegs);
                Point2D aisPoint = source.getScreenPosition(aisLoc.getNewAbsoluteLatLonDepth());

                Graphics2D g2 = (Graphics2D) g.create();

                g2.setColor(Color.RED);

                // reset graphics
                AffineTransform transform = new AffineTransform();
                g2.setTransform(transform);

                g2.translate(aisPoint.getX(), aisPoint.getY());

                paintFuturePositions(ais, aisPoint, g, source);

                g2.dispose();
            }
        }
    }

    private void paintFuturePositions(ShipAisSnapshot ais, Point2D aisPoint, Graphics2D g, StateRenderer2D source) {
        List<ShipAisSnapshot> future = aisManager.getFutureSnapshot(ais.getMmsi(), Arrays.stream(timeOffsetsMinutes)
                .map(v -> v*60*1000).parallel().toArray());

        for(ShipAisSnapshot f : future) {
            LocationType loc = new LocationType(f.getLatDegs(), f.getLonDegs());
            Point2D p = source.getScreenPosition(loc);

            AffineTransform transform = new AffineTransform();
            g.setTransform(transform);
            g.translate(p.getX(), p.getY());
            g.rotate(Math.PI + ais.getCog() - source.getRotation());

            g.setColor(Color.GREEN.darker());
            g.drawOval(-5, -5, 10, 10);
            g.setColor(Color.RED.darker());
            g.fillOval(-5, -5, 10, 10);
        }
    }

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        synchronized (targetAisMmsi) {
            if (event.getClickCount() >= 2)
                targetAisMmsi.clear();

            for (ShipAisSnapshot ais : aisManager.getShips()) {
                LocationType loc = new LocationType(Math.toDegrees(ais.getLatRads()), Math.toDegrees(ais.getLonRads()));
                Point2D aisLoc = source.getScreenPosition(loc);

                double error = aisLoc.distance(event.getPoint());
                NeptusLog.pub().info(error);
                System.out.println(ais.getLabel() + " " + error);
                if (error <= clickDistanceError) {
                    int mmsi = ais.getMmsi();
                    if(targetAisMmsi.contains(mmsi))
                        targetAisMmsi.remove(mmsi);
                    else
                        targetAisMmsi.add(ais.getMmsi());
                    return;
                }
            }
        }
    }
}
