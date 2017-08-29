package pt.lsts.neptus.plugins.formationcontrol;

import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.*;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import ucar.unidata.geoloc.Earth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;


/**
 * Created by elias on 8/8/16. Tjena
 */
@PluginDescription(author="Elias", category= PluginDescription.CATEGORY.UNSORTED, name="My First Plugin")
public class MyFirstPlugin extends SimpleRendererInteraction {
    public MyFirstPlugin(ConsoleLayout console) {
        super(console);
    }

    double R_triple = 0.000005;
    public double[][] y_agents = new double[4][2];

    int controller = 1;
    double[] w = {1, 1, 1};
    double tau = 10;
    double gamma_T = .05; double gamma_ij = 0.5*gamma_T;

    private Vehicle vehicle1 = new Vehicle(getConsole(),"duck1", Color.cyan, R_triple, R_triple, 0.01);
    private Vehicle vehicle2 = new Vehicle(getConsole(),"duck3", Color.magenta, R_triple, R_triple, 0.01);
    private Vehicle vehicle3 = new Vehicle(getConsole(),"duck4", Color.green, R_triple, R_triple, 0.01);

    SingleVehicleController SVControl = new SingleVehicleController();
    DualVehicleController DVcontroller = new DualVehicleController();

    private boolean hasFishLocation = false;
    private long fishConnectionTimeout = 120000;
    private long fishLocationTime;
    private RemoteSensorInfo fish_rsi = new RemoteSensorInfo();
    private boolean controllerIsActive = false;

    Settingswindow settingsWindow = new Settingswindow(this);

    @Subscribe
    public void consume(Announce msg){
        if (msg.getSysName().equals(vehicle1.getSystemName())) {
            vehicle1.newAnnouce(msg);
            vehicle1.setAddress(msg.getSrc());
        }
        else if (msg.getSysName().equals(vehicle2.getSystemName())){
            vehicle2.newAnnouce(msg);
            vehicle2.setAddress(msg.getSrc());
        }
        else if (msg.getSysName().equals(vehicle3.getSystemName())) {
            vehicle3.newAnnouce(msg);
            vehicle3.setAddress(msg.getSrc());
        }
    }

    @Subscribe
    public void on(EstimatedState msg) {
        if (msg.getSourceName().equals(vehicle1.getSystemName())) {
            vehicle1.setEstimatedState(msg);
        }
        else if (msg.getSourceName().equals(vehicle2.getSystemName())){
            vehicle2.setEstimatedState(msg);
        }
        else if (msg.getSourceName().equals(vehicle3.getSystemName())) {
            vehicle3.setEstimatedState(msg);
        }
    }

    @Subscribe
    public void consume(RemoteSensorInfo msg) {
        fishLocationTime = System.currentTimeMillis();

        if (!msg.getId().equals("fish_module"))
            return;

        if (!hasFishLocation)
            post(Notification.success("Multiple-vehicle controller","Got fish location!"));

        hasFishLocation = true;
        fish_rsi = msg;
    }

    @Periodic(millisBetweenUpdates = 2000)
    public void loop(){
        if (System.currentTimeMillis() - fishLocationTime > fishConnectionTimeout && hasFishLocation) {
            post(Notification.error("Multiple-vehicle controller", "Fish location is old!"));
            hasFishLocation = false;
        }
        vehicle1.update();
        vehicle2.update();
        vehicle3.update();

        if (controllerIsActive && !hasFishLocation) {
            deactivateControllerAndFollowReference();
        }

        if (controllerIsActive && hasFishLocation) {
            if (countVehicles() == 1)
                test_singleVehicleController();
            else if (countVehicles() == 2)
                test_doubleVehicleController();
            else if (countVehicles() == 3)
                if (controller == 1)
                    test_tripleVehicleControllerOpt();
                else if (controller == 2)
                    test_tripleVehicleControllerAssignment();
                // ArrayList<Vehicle> vehicles = getConnectedVehicles();
                // double gotoLat1 = vehicles.get(0).getLat(), gotoLat2 = vehicles.get(1).getLat(), gotoLat3 = vehicles.get(2).getLat();
                // double gotoLon1 = vehicles.get(0).getLon(), gotoLon2 = vehicles.get(1).getLon(), gotoLon3 = vehicles.get(2).getLon();
        }

    }

    public void test_doubleVehicleController() {

        double[] fish_pos = new double[] {fish_rsi.getLat(), fish_rsi.getLon()};
        double[] fish_vel = new double[] {0,0};
        boolean flipXY = true;

        System.out.println("#################################");

        ArrayList<Vehicle> vehicles = getConnectedVehicles();

        double[] desiredVelocities;
        desiredVelocities = DualVehicleController.calculateDesiredVelocity(fish_pos, fish_vel, vehicles, flipXY);

        double dt = 40;
        double vlat1, vlat2, vlon1, vlon2;
        double gotoLat1, gotoLat2, gotoLon1, gotoLon2;
        double speed1, speed2;

        if (flipXY) {
            vlat1 = desiredVelocities[1];
            vlon1 = desiredVelocities[0];
            vlat2 = desiredVelocities[3];
            vlon2 = desiredVelocities[2];
        }
        else {
            vlat1 = desiredVelocities[0];
            vlon1 = desiredVelocities[1];
            vlat2 = desiredVelocities[2];
            vlon2 = desiredVelocities[3];
        }

        speed1 = Earth.getRadius()*(Math.sqrt(vlat1*vlat1 + vlon1*vlon1));
        speed2 = Earth.getRadius()*(Math.sqrt(vlat2*vlat2 + vlon2*vlon2));

        if (true) {//(speed1 < 10) {
            //dt = 0.001 / Math.sqrt(vlat1*vlat1 + vlon1*vlon1);
            gotoLat1 = vehicles.get(0).getLat() + vlat1 * dt;
            gotoLon1 = vehicles.get(0).getLon() + vlon1 * dt;
        }
        else {
            gotoLat1 = fish_rsi.getLat();
            gotoLon1 = fish_rsi.getLon();
            speed1 = 5;
        }

        if (true) {// (speed2 < 10) {
            //dt = 0.001 / Math.sqrt(vlat2*vlat2 + vlon2*vlon2);
            gotoLat2 = vehicles.get(1).getLat() + vlat2 * dt;
            gotoLon2 = vehicles.get(1).getLon() + vlon2 * dt;
        }
        else {
            gotoLat2 = fish_rsi.getLat();
            gotoLon2 = fish_rsi.getLon();
            speed2 = 5;
        }

        System.out.println("Speed1: " + speed1 + " Speed2:" + speed2);

        Reference ref1 = vehicles.get(0).createReference(gotoLat1, gotoLon1, speed1, 2);
        Reference ref2 = vehicles.get(1).createReference(gotoLat2, gotoLon2, speed2, 2);

        send(vehicles.get(0).getSystemName(), ref1);
        send(vehicles.get(1).getSystemName(), ref2);
    }


    public void test_tripleVehicleControllerAssignment() {

        double[] fish_pos = new double[]{fish_rsi.getLon(), fish_rsi.getLat()};
        double[] fish_vel = new double[]{0, 0};

        System.out.println(fish_pos[0]);
        System.out.println(fish_pos[1]);

        System.out.println("#################################");

        ArrayList<Vehicle> vehicles = getConnectedVehicles();

        double[] desiredVelocities;
        desiredVelocities = TripleVehicleControllerAssignment.calculateDesiredVelocity(fish_pos, fish_vel, vehicles, w, y_agents);

        System.out.println("#################################");

        for (double vel : desiredVelocities) System.out.println(vel);

        double vlat1, vlat2, vlat3, vlon1, vlon2, vlon3;
        double gotoLat1, gotoLat2, gotoLat3, gotoLon1, gotoLon2, gotoLon3;
        double speed1, speed2, speed3;

        vlon1 = desiredVelocities[2];
        vlat1 = desiredVelocities[3];
        vlon2 = desiredVelocities[4];
        vlat2 = desiredVelocities[5];
        vlon3 = desiredVelocities[6];
        vlat3 = desiredVelocities[7];
        speed1 = 1;
        speed2 = 1;
        speed3 = 1;

        gotoLat1 = vlat1; gotoLat2 = vlat2; gotoLat3 = vlat3;
        gotoLon1 = vlon1; gotoLon2 = vlon2; gotoLon3 = vlon3;

        Reference ref1 = vehicles.get(0).createReference(gotoLat1, gotoLon1, speed1, 2);
        Reference ref2 = vehicles.get(1).createReference(gotoLat2, gotoLon2, speed2, 2);
        Reference ref3 = vehicles.get(2).createReference(gotoLat3, gotoLon3, speed3, 2);

        send(vehicles.get(0).getSystemName(), ref1);
        send(vehicles.get(1).getSystemName(), ref2);
        send(vehicles.get(2).getSystemName(), ref3);
    }

    public void test_tripleVehicleControllerOpt() {

        double[] fish_pos = new double[]{fish_rsi.getLon(), fish_rsi.getLat()};
        double[] fish_vel = new double[]{0, 0};

        System.out.println(fish_pos[0]);
        System.out.println(fish_pos[1]);

        System.out.println("#################################");

        ArrayList<Vehicle> vehicles = getConnectedVehicles();

        double[] desiredVelocities;
        desiredVelocities = TripleVehicleControllerOpt.calculateDesiredVelocity(fish_pos, fish_vel, vehicles,
                                                                                w, R_triple, tau, gamma_T, gamma_ij);

        double vlat1, vlat2, vlat3, vlon1, vlon2, vlon3;
        double speed1, speed2, speed3;

        System.out.println("Points");
        vlon1 = desiredVelocities[0];
        vlat1 = desiredVelocities[1];
        vlon2 = desiredVelocities[2];
        vlat2 = desiredVelocities[3];
        vlon3 = desiredVelocities[4];
        vlat3 = desiredVelocities[5];
        speed1 = 1; speed2 = 1; speed3 = 1;

        Reference ref1 = vehicles.get(0).createReference(vlat1, vlon1, speed1, 2);
        Reference ref2 = vehicles.get(1).createReference(vlat2, vlon2, speed2, 2);
        Reference ref3 = vehicles.get(2).createReference(vlat3, vlon3, speed3, 2);

        send(vehicles.get(0).getSystemName(), ref1);
        send(vehicles.get(1).getSystemName(), ref2);
        send(vehicles.get(2).getSystemName(), ref3);
    }


    public void test_singleVehicleController() {
        int n_connected = countVehicles();

        if (n_connected == 1) {
            boolean flipXY = true;

            ArrayList<Vehicle> connectedVehicle = getConnectedVehicles();

            double scale = Math.cos(connectedVehicle.get(0).getLat());

            System.out.println("RSI: " + Math.toDegrees(fish_rsi.getLat()) + ", " + Math.toDegrees(fish_rsi.getLon()));

            double[] fish_pos = new double[] {fish_rsi.getLat(), fish_rsi.getLon()*scale};//*Math.cos(fish_rsi.getLat())};
            double[] fish_vel = new double[] {0,0};
            double[] vehicle_pos = new double[] {connectedVehicle.get(0).getLat(),
                                                 connectedVehicle.get(0).getLon()*scale};//*Math.cos(connectedVehicle.get(0).getLat())};

            // caravela: d = 0.0001, delta = 0.00002, alpha = 0.005
            // anka: d = 0.000025, delta = 0.0000075, alpha = 0.005
            double d = connectedVehicle.get(0).params.getD();
            double delta = connectedVehicle.get(0).params.getDelta();
            double alpha = connectedVehicle.get(0).params.getAlpha();

            double[] desiredVelocity = SingleVehicleController.calculateDesiredVelocity(
                    fish_pos,
                    fish_vel,
                    vehicle_pos,
                    d,
                    delta,
                    alpha,
                    flipXY);

            double dt = 40;
            double vlat, vlon;

            if (flipXY) {
                vlat = desiredVelocity[1];
                vlon = desiredVelocity[0];
            }
            else {
                vlat = desiredVelocity[0];
                vlon = desiredVelocity[1];
            }

            double speed = Earth.getRadius()*(Math.sqrt(vlat*vlat + vlon*vlon));

            double dlat = vlat*dt;
            double dlon = vlon*dt;

            double gotoLat = connectedVehicle.get(0).getLat()+dlat;
            double gotoLon = connectedVehicle.get(0).getLon()+dlon;

            System.out.println("REFERENCE: " + Math.toDegrees(gotoLat) + ", " + Math.toDegrees(gotoLon));
            System.out.println("Speed: " + speed);

            Reference ref;
            ref = connectedVehicle.get(0).createReference(gotoLat, gotoLon, speed, 2);
            send(connectedVehicle.get(0).getSystemName(), ref);
        }
    }

    public ArrayList<Vehicle> getConnectedVehicles(){
        ArrayList<Vehicle> vehicles = new ArrayList<>();
        if (vehicle1.isConnected()) {
            vehicles.add(vehicle1);
        }
        if (vehicle2.isConnected()) {
            vehicles.add(vehicle2);
        }
        if (vehicle3.isConnected()) {
            vehicles.add(vehicle3);
        }
        return vehicles;
    }
    public int countVehicles(){
        int count = 0;
        if (vehicle1.isConnected())
            count++;
        if (vehicle2.isConnected())
            count++;
        if (vehicle3.isConnected())
            count++;
        return count;
    }

    private void activateControllerAndFollowReference() {
        ArrayList<Vehicle> vehicles = getConnectedVehicles();

        if (vehicles.size() == 0) {
            post(Notification.error("Formation Controller","No vehicles connected!"));
            return;
        }

        if (!hasFishLocation) {
            post(Notification.error("Formation Controller", "Don't have fish location, ignoring start request!"));
            return;
        }

        System.out.print("STARTING TO CONTROL: ");

        for (Vehicle vehicle : vehicles) {
            System.out.println("\t" + vehicle.getSystemName());
            vehicle.prev[0] = vehicle.getLat();
            vehicle.prev[1] = vehicle.getLon();
            vehicle.startFollowReferenceManeuver();
        }

        // Inititialize graph for TripeVehicleController
        y_agents[0][0] = 0; y_agents[0][1] = 0; y_agents[1][0] = 0; y_agents[1][1] = 1;
        y_agents[2][0] = -Math.sqrt(3)*0.5; y_agents[2][1] = -0.5; y_agents[3][0] = Math.sqrt(3)*0.5; y_agents[3][1] = -0.5;
        for (int i = 0; i < 4; i++) {  //Scale for r
            for (int j = 0; j < 2; j++) {
                y_agents[i][j] = y_agents[i][j] * R_triple*111300*180/Math.PI;
            }
        }

        controllerIsActive = true;
    }
    private void deactivateControllerAndFollowReference() {
        ArrayList<Vehicle> vehicles = getConnectedVehicles();
        for (Vehicle vehicle : vehicles) {
            vehicle.stopFollowReferenceManeuver();
        }
        controllerIsActive = false;
    }

    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        super.mouseClicked(event, source);

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            if (!controllerIsActive) {
                popup.add("Activate Fish Formation Control").addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        activateControllerAndFollowReference();
                    }
                });
            } else {
                popup.add("Stop Fish Formation Control").addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        deactivateControllerAndFollowReference();
                    }
                });
            }

            popup.addSeparator();

            popup.add("elias.kth.MyFirstPlugin.Vehicle Settings!").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    settingsWindow.update();
                }
            });

            popup.show((Component) event.getSource(), event.getX(), event.getY());
        }

        //source.repaint();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        ArrayList<Vehicle> vehicles = getConnectedVehicles();

        for (Vehicle vehicle : vehicles) {
            // Plot the current reference
            LocationType loc = new LocationType(Math.toDegrees(vehicle.currentReference.getLat()),
                    Math.toDegrees(vehicle.currentReference.getLon()));
            Point2D pt = renderer.getScreenPosition(loc);

            Ellipse2D ellis = new Ellipse2D.Double(pt.getX() - vehicle.radius, pt.getY() - vehicle.radius,
                    vehicle.radius * 2, vehicle.radius * 2);
            g.setColor(vehicle.referenceColor);
            g.fill(ellis);

            if (!hasFishLocation) {
                return;
            }

            // Center of circles to be plotted
            double lat_degs = Math.toDegrees(fish_rsi.getLat());
            double lon_degs = Math.toDegrees(fish_rsi.getLon());

            // Plot Main Circle
            Graphics2D g2d = (Graphics2D) g.create();
            Stroke solidStroke = new BasicStroke(3);
            g2d.setStroke(solidStroke);

            double horizontalAcc = renderer.getZoom() * 2*(vehicle.params.getD() * Earth.getRadius());

            LocationType position = new LocationType(lat_degs, lon_degs);
            Point2D screenPosition = renderer.getScreenPosition(position);

            Ellipse2D circle = new Ellipse2D.Double(
                    screenPosition.getX()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    screenPosition.getY()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    horizontalAcc,                          // diameter of the circle
                    horizontalAcc);

            g2d.setColor(vehicle.referenceColor);
            g2d.draw(circle);                        // diameter of the circle
            g2d.dispose();

            // Plot Outer Tolerance Circle
            Graphics2D g3d = (Graphics2D) g.create();
            Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            g3d.setStroke(dashed);

            horizontalAcc = renderer.getZoom() * 2*((vehicle.params.getD() + vehicle.params.getDelta()) * Earth.getRadius());

            LocationType position2 = new LocationType(lat_degs, lon_degs);
            Point2D screenPosition2 = renderer.getScreenPosition(position2);

            Ellipse2D circle2 = new Ellipse2D.Double(
                    screenPosition2.getX()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    screenPosition2.getY()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    horizontalAcc,                          // diameter of the circle
                    horizontalAcc);

            g3d.setColor(vehicle.referenceColor);
            g3d.draw(circle2);                        // diameter of the circle
            g3d.dispose();

            // Plot Inner Tolerance Circle
            Graphics2D g4d = (Graphics2D) g.create();
            g4d.setStroke(dashed);

            horizontalAcc = renderer.getZoom() * 2*((vehicle.params.getD() - vehicle.params.getDelta()) * Earth.getRadius());

            Ellipse2D circle3 = new Ellipse2D.Double(
                    screenPosition2.getX()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    screenPosition2.getY()-horizontalAcc/2,  // coordinate of top-left corner of circle
                    horizontalAcc,                          // diameter of the circle
                    horizontalAcc);

            g4d.setColor(vehicle.referenceColor);
            g4d.draw(circle3);                        // diameter of the circle
            g4d.dispose();
        }
    }

    @Override
    public void cleanSubPanel(){
        System.out.println("My First Plugin was destroyed.");
    }

    @Override
    public void initSubPanel() {
        System.out.println("Started My First Plugin");
    }

    @Override
    public boolean isExclusive() {
        return true;
    }
}

