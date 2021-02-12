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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.types.map;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.vecmath.Point3d;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.element.IPlanElement;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;
import pt.lsts.neptus.util.coord.MapTileUtil;

@LayerPriority(priority = 50)
public class PlanElement extends AbstractElement implements Renderer2DPainter, PreferencesListener {

    private PlanType plan = null;
    private static GeneralPath arrow1 = new GeneralPath();
    private static GeneralPath arrow2 = new GeneralPath();
    private StateRenderer2D renderer = null;
    private boolean showDistances = true;
    private boolean showManNames = true;
    private boolean showVelocities = false;
    private boolean saveAsTrajectory = false;
    private double lastRendererZoom = 1.0;
    private double lastRotationAngle = 0.0;
    private double transp2d = 0.7;
    private Point2D lastRendererCenter = new Point2D.Double();
    private LinkedHashMap<String, Point2D> maneuverLocations = new LinkedHashMap<String, Point2D>();
    private LinkedHashMap<String, Point2D> startManeuverLocations = new LinkedHashMap<String, Point2D>();
    private LinkedHashMap<String, Point2D> endManeuverLocations = new LinkedHashMap<String, Point2D>();
    private Color color = GeneralPreferences.rendererPlanColor;
    private String selectedManeuver = null;

    private String activeManeuver = null;

    private int snapPixels = 5;

    private JLabel lbl = new JLabel();

    private boolean beingEdited = false;

    protected LinkedHashMap<String, DefaultProperty> lastSetProperties = new LinkedHashMap<String, DefaultProperty>();

    static {
        arrow1.moveTo(0, -5);
        arrow1.lineTo(-3.5, -11);
        arrow1.lineTo(3.5, -11);
        arrow1.closePath();

        arrow2.moveTo(0, -6);
        arrow2.lineTo(-4.5, -12);
        arrow2.lineTo(4.5, -12);
        arrow2.closePath();

    }

    private LocationType center = new LocationType();

    public PlanElement() {
        super();
    }

    public PlanElement(MapGroup mg, MapType map) {
        super(mg, map);
        lbl.setBackground(new Color(255, 200, 200, 75));
        lbl.setBorder(BorderFactory.createLineBorder(new Color(255, 100, 100, 150)));
        lbl.setOpaque(true);
        lbl.setBounds(0, 0, 0, 0);
        GeneralPreferences.addPreferencesListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        GeneralPreferences.removePreferencesListener(this);
        super.finalize();
    }

    @Override
    public String getType() {
        return "Plan";
    }

    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (transp2d < 1.0) {

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) transp2d));
            paint(g, renderer, renderer.getRotation());
            g.setComposite(AlphaComposite.SrcOver);
        }
        else
            paint(g, renderer, renderer.getRotation());
    }

    public Point2D translateManeuverPosition(String maneuverID, double transX, double transY) {
        Point2D loc = maneuverLocations.get(maneuverID);
        loc.setLocation(loc.getX() + transX, loc.getY() + transY);
        Point2D locS = startManeuverLocations.get(maneuverID);
        locS.setLocation(locS.getX() + transX, locS.getY() + transY);
        Point2D locE = endManeuverLocations.get(maneuverID);
        locE.setLocation(locE.getX() + transX, locE.getY() + transY);

        Point2D nearerX = new Point2D.Double(0, 0);
        Point2D nearerY = new Point2D.Double(0, 0);
        double distX = Integer.MAX_VALUE;
        double distY = Integer.MAX_VALUE;

        Point2D homePt = renderer.getScreenPosition(renderer.getMapGroup().getHomeRef().getCenterLocation());

        double dx = Math.abs(homePt.getX() - loc.getX());
        double dy = Math.abs(homePt.getY() - loc.getY());

        double dxS = locS.getX() - loc.getX();
        double dyS = locS.getY() - loc.getY();
        double dxE = locE.getX() - loc.getX();
        double dyE = locE.getY() - loc.getY();

        if (dx < distX) {
            distX = dx;
            nearerX = homePt;
        }

        if (dy < distY) {
            distY = dy;
            nearerY = homePt;
        }

        for (String s : maneuverLocations.keySet()) {
            Point2D otherP = maneuverLocations.get(s);
            if (s.equals(maneuverID) || !(plan.getGraph().getManeuver(s) instanceof LocatedManeuver))
                continue;

            dx = Math.abs(otherP.getX() - loc.getX());
            dy = Math.abs(otherP.getY() - loc.getY());

            if (dx < distX) {
                distX = dx;
                nearerX = otherP;
            }

            if (dy < distY) {
                distY = dy;
                nearerY = otherP;
            }
        }

        if (distX <= 5) {
            loc.setLocation(nearerX.getX(), loc.getY());
        }

        if (distY <= 5)
            loc.setLocation(loc.getX(), nearerY.getY());

        maneuverLocations.put(maneuverID, loc);
        Point2D ls = new Point2D.Double(loc.getX() + dxS, loc.getY() + dyS);
        startManeuverLocations.put(maneuverID, ls);
        ls = new Point2D.Double(loc.getX() + dxE, loc.getY() + dyE);
        endManeuverLocations.put(maneuverID, ls);
        return loc;
    }

    public void recalculateManeuverPositions(StateRenderer2D renderer) {
        maneuverLocations.clear();
        startManeuverLocations.clear();
        endManeuverLocations.clear();

        if (plan == null)
            return;

        LinkedList<Maneuver> unknownLocs = new LinkedList<Maneuver>();
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m instanceof LocatedManeuver) {
                LocationType lt = new LocationType(((LocatedManeuver) m).getManeuverLocation());
                maneuverLocations.put(m.getId(), renderer.getScreenPosition(lt));
                lt = new LocationType(((LocatedManeuver) m).getStartLocation());
                startManeuverLocations.put(m.getId(), renderer.getScreenPosition(lt));
                lt = new LocationType(((LocatedManeuver) m).getEndLocation());
                endManeuverLocations.put(m.getId(), renderer.getScreenPosition(lt));
            }
            else {
                maneuverLocations.remove(m.getId());
                startManeuverLocations.remove(m.getId());
                endManeuverLocations.remove(m.getId());
                unknownLocs.add(m);
            }
        }

        for (Maneuver m : unknownLocs) {
            Point2D vpoint = calculateManeuverPositionWorker(m, renderer);
            Point2D ptM = new Point2D.Double();
            ptM.setLocation(vpoint);
            maneuverLocations.put(m.getId(), ptM);
            Point2D ptS = new Point2D.Double();
            ptS.setLocation(vpoint);
            startManeuverLocations.put(m.getId(), ptS);
            Point2D ptE = new Point2D.Double();
            ptE.setLocation(vpoint);
            endManeuverLocations.put(m.getId(), ptE);
        }
    }

    private Point2D calculateManeuverPositionWorker(Maneuver m, StateRenderer2D renderer) {
        if (maneuverLocations.containsKey(m.getId())) {
            return maneuverLocations.get(m.getId());
        }
        else {
            Maneuver[] previousMans = plan.getGraph().getPreviousManeuvers(m.getId());
            Maneuver previousMan = (previousMans.length > 0) ? previousMans[0] : null;

            if (previousMan == null) {
                Point2D pt = renderer.getScreenPosition(new LocationType(plan.getMissionType().getHomeRef()));
                maneuverLocations.put(m.getId(), pt);
                return pt;
            }
            else {
                Point2D pt = new Point2D.Double();
                pt.setLocation(calculateManeuverPositionWorker(previousMan, renderer));

                double angle = 0;

                Maneuver tmp = m;
                while (tmp != null && !(tmp instanceof LocatedManeuver))
                    tmp = plan.getGraph().getFollowingManeuver(tmp.getId());

                if (tmp != null) {
                    Point2D otherPoint = maneuverLocations.get(tmp.getId());
                    angle = Math.atan2(otherPoint.getY() - pt.getY(), otherPoint.getX() - pt.getX());
                }
                pt.setLocation(pt.getX() + Math.cos(angle) * 50, pt.getY() + Math.sin(angle) * 50);
                maneuverLocations.put(m.getId(), pt);
                return pt;
            }
        }
    }

    public Maneuver[] getAllInterceptedManeuvers(Point2D pt) {

        Vector<Maneuver> mans = new Vector<Maneuver>();

        double rad = Maneuver.circleDiam;
        for (String manId : maneuverLocations.keySet()) {
            if (maneuverLocations.get(manId).distance(pt) < rad) {
                mans.add(plan.getGraph().getManeuver(manId));
            }
        }
        return mans.toArray(new Maneuver[0]);
    }

    public Maneuver iterateManeuverBack(Point2D pt) {
        double rad = Maneuver.circleDiam;
        Vector<String> interceptedManeuvers = new Vector<String>();
        for (String manId : maneuverLocations.keySet()) {
            if (maneuverLocations.get(manId).distance(pt) < rad) {
                interceptedManeuvers.add(manId);
            }
        }
        if (interceptedManeuvers.size() == 0) {
            selectedManeuver = null;
            return null;
        }
        else {
            if (!interceptedManeuvers.contains(selectedManeuver)) {
                selectedManeuver = interceptedManeuvers.firstElement();
                return plan.getGraph().getManeuver(selectedManeuver);
            }
            else {
                int index = interceptedManeuvers.indexOf(selectedManeuver);
                index--;
                if (index < 0)
                    index = interceptedManeuvers.size() - 1;

                selectedManeuver = interceptedManeuvers.get(index);
                return plan.getGraph().getManeuver(selectedManeuver);
            }
        }
    }

    public String[] getManeuversUnder(Point2D pt) {
        double rad = Maneuver.circleDiam;
        Vector<String> interceptedManeuvers = new Vector<String>();
        for (String manId : maneuverLocations.keySet()) {
            if (maneuverLocations.get(manId).distance(pt) < rad && plan.getGraph().getManeuver(manId) != null) {
                interceptedManeuvers.add(manId);
            }
        }

        return interceptedManeuvers.toArray(new String[0]);
    }

    /**
     * This method tries to find a maneuver that is intercepted by the given point<br>
     * If more than one maneuvers are intercepted and the last intercepted maneuver still exists in the current
     * intercepted maneuvers, returns another maneuver (iterating all intercepted maneuvers)
     * 
     * @param pt The point where to look for maneuvers
     * @return The intercepted maneuver or <b>null</b> if no maneuver was intercepted
     */
    public Maneuver iterateManeuverUnder(Point2D pt) {
        double rad = Maneuver.circleDiam;
        Vector<String> interceptedManeuvers = new Vector<String>();
        for (String manId : maneuverLocations.keySet()) {
            if (maneuverLocations.get(manId).distance(pt) < rad) {
                interceptedManeuvers.add(manId);
            }
        }
        if (interceptedManeuvers.size() == 0) {
            selectedManeuver = null;
            return null;
        }
        else {
            if (!interceptedManeuvers.contains(selectedManeuver)) {
                selectedManeuver = interceptedManeuvers.firstElement();
                return plan.getGraph().getManeuver(selectedManeuver);
            }
            else {
                int index = interceptedManeuvers.indexOf(selectedManeuver);
                index++;
                if (index >= interceptedManeuvers.size())
                    index = 0;

                selectedManeuver = interceptedManeuvers.get(index);
                return plan.getGraph().getManeuver(selectedManeuver);
            }
        }
    }

    /**
     * This method tries to find a maneuver that is intercepted by the given point<br>
     * If more than one maneuvers are intercepted and the last intercepted maneuver still exists in the current
     * intercepted maneuvers, returns another maneuver (iterating all intercepted maneuvers)
     * 
     * @param pt The point where to look for maneuvers
     * @return The intercepted maneuver or <b>null</b> if no maneuver was intercepted
     */
    public Maneuver getFirstInterceptedManeuver(Point2D pt) {
        double rad = Maneuver.circleDiam;
        for (String manId : maneuverLocations.keySet()) {
            if (maneuverLocations.get(manId).distance(pt) < rad) {
                return plan.getGraph().getManeuver(manId);
            }
        }
        return null;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer, double rotation) {

        if (getPlan() == null)
            return;

        boolean recalculatePositions = true;

        if (renderer.getZoom() == lastRendererZoom) {
            if (lastRendererCenter != null && renderer.getRotation() == 0) {
                Point2D curCenter = renderer.getScreenPosition(new LocationType(plan.getMissionType().getHomeRef()));
                if (!curCenter.equals(lastRendererCenter)) {

                    double diffX = curCenter.getX() - lastRendererCenter.getX();
                    double diffY = curCenter.getY() - lastRendererCenter.getY();

                    for (String key : maneuverLocations.keySet()) {
                        Point2D pt = maneuverLocations.get(key);
                        pt.setLocation(pt.getX() + diffX, pt.getY() + diffY);
                    }

                    for (String key : startManeuverLocations.keySet()) {
                        Point2D pt = startManeuverLocations.get(key);
                        pt.setLocation(pt.getX() + diffX, pt.getY() + diffY);
                    }

                    for (String key : endManeuverLocations.keySet()) {
                        Point2D pt = endManeuverLocations.get(key);
                        pt.setLocation(pt.getX() + diffX, pt.getY() + diffY);
                    }
                }
                recalculatePositions = false;
            }
        }

        if (recalculatePositions || renderer.getRotation() != lastRotationAngle)
            recalculateManeuverPositions(renderer);

        if (renderer == null || plan.getMissionType() == null) {
            return;
        }
        lastRendererCenter = renderer.getScreenPosition(new LocationType(plan.getMissionType().getHomeRef()));
        lastRendererZoom = renderer.getZoom();
        lastRotationAngle = renderer.getRotation();

        Maneuver[] maneuvers = getPlan().getGraph().getAllManeuvers();
        Object[] pElementsObjs = getPlan().getPlanElements().getPlanElements().toArray();

        LocationType start = null;

        if (getPlan().getStartMode() == PlanType.INIT_HOMEREF) {
            start = new LocationType(getMapGroup().getCoordinateSystem());
        }

        if (getPlan().getStartMode() == PlanType.INIT_START_WPT) {
            if (getPlan().getMapGroup().getMapObjectsByID("start").length > 0) {
                start = new LocationType(getPlan().getMapGroup().getMapObjectsByID("start")[0].getCenterLocation());
            }
            else if (getPlan().getMapGroup().getMapObjectsByID("home").length > 0) {
                start = new LocationType(getPlan().getMapGroup().getMapObjectsByID("home")[0].getCenterLocation());
            }
            else if ((start = MyState.getLocation()) != null) {
                // start = start; Already in the test to not duplicate the location instances created 
            }
            else {
                start = new LocationType(getPlan().getMapGroup().getCoordinateSystem());
            }
        }

        if (new LocationType().getDistanceInMeters(start) == 0) {
            start = null;
        }

        AffineTransform oldTransform = g.getTransform();

        AffineTransform identity = g.getTransform();

        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue());

        Vector<String> drawnLocations = new Vector<String>();
        Vector<String> drawnTransitions = new Vector<String>();

        Graphics2D gElements = (Graphics2D) g.create();
        
        for (Maneuver man : maneuvers) {
            
            for (Maneuver previousMan : plan.getGraph().getPreviousManeuvers(man.getId())) {
                g.setColor(c);
                g.setTransform(identity);
                Point2D previousPoint = null;
                boolean isTrajectoryKnown = false;

                if (previousMan == null) {
                    previousPoint = null;// (start != null)? renderer.getScreenPosition(start) : null;
                }
                else {
                    previousPoint = endManeuverLocations.get(previousMan.getId());
                    if (man instanceof LocatedManeuver && previousMan instanceof LocatedManeuver) {
                        isTrajectoryKnown = true;
                    }
                }
                if (isTrajectoryKnown)
                    g.setStroke(new BasicStroke());
                else
                    g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, new float[] { 2f,
                            5f, 2f, 5f }, 0f));

                Point2D loc = maneuverLocations.get(man.getId());
                Point2D sLoc = startManeuverLocations.get(man.getId());

                if (loc != null && previousPoint != null) {

                    String trans = previousPoint.toString() + "->" + loc.toString();
                    if (!drawnTransitions.contains(trans)) {
                        drawnTransitions.add(trans);
                        if (beingEdited) {
                            g.setColor(Color.black);
                            g.setStroke(new BasicStroke(4f));
                            g.draw(new Line2D.Double(previousPoint, sLoc));
                        }
                        else {
                            g.setColor(Color.gray.darker());
                            g.setStroke(new BasicStroke(2f));
                            g.draw(new Line2D.Double(previousPoint, sLoc));
                        }
                        if (beingEdited) {
                            g.setColor(Color.yellow);
                            g.setStroke(new BasicStroke(2f));
                            g.draw(new Line2D.Double(previousPoint, sLoc));
                        }
                        else {
                            g.setColor(Color.white);
                            g.setStroke(new BasicStroke(1f));
                            g.draw(new Line2D.Double(previousPoint, sLoc));
                        }

                        g.setStroke(new BasicStroke());

                        g.translate(sLoc.getX(), sLoc.getY());

                        double angle = Math.atan2(sLoc.getY() - previousPoint.getY(),
                                sLoc.getX() - previousPoint.getX());

                        g.rotate(angle - Math.PI / 2);

                        if (isBeingEdited()) {
                            g.scale(1.5, 1.5);
                            g.setColor(Color.black);
                            g.fill(arrow2);
                            g.setColor(Color.yellow);
                            g.fill(arrow1);
                            g.scale(1 / 1.5, 1 / 1.5);
                        }
                        else {
                            g.setColor(Color.gray);
                            g.fill(arrow2);
                            g.setColor(Color.white);
                            g.fill(arrow1);
                        }

                        if (showDistances && isTrajectoryKnown) {
                            String txt = "";
                            double dist = loc.distance(previousPoint);
                            g.translate(0, -dist / 2);
                            g.rotate(Math.PI / 2);

                            LocationType sPos = renderer.getRealWorldLocation(sLoc);
                            LocationType pPos = renderer.getRealWorldLocation(previousPoint);

                            txt += GuiUtils.getNeptusDecimalFormat(0).format(sPos.getDistanceInMeters(pPos)) + " m";

                            if (Math.abs(angle) > Math.PI / 2) {
                                g.rotate(Math.PI);
                            }

                            Font oldFont = g.getFont();
                            g.setFont(new Font("Arial", Font.PLAIN, 10));

                            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(txt, g);
                            g.setColor(Color.BLACK);
                            g.drawString(txt, -(int) stringBounds.getWidth() / 2 + 1, -2);
                            g.setColor(Color.WHITE);
                            g.drawString(txt, -(int) stringBounds.getWidth() / 2, -3);
                            g.setFont(oldFont);
                        }
                    }
                }
            }
        }

        g.setStroke(new BasicStroke());

        for (Maneuver man : maneuvers) {

            Point2D loc = maneuverLocations.get(man.getId());
            if (loc == null) {
                recalculateManeuverPositions(renderer);
                loc = maneuverLocations.get(man.getId());
            }
            if (drawnLocations.contains(loc.toString()) && man instanceof Goto) {
                if (getActiveManeuver() == null || !getActiveManeuver().equals(man.getId()))
                    continue;
            }

            drawnLocations.add(loc.toString());
            g.setTransform(identity);

            loc = maneuverLocations.get(man.getId());

            if (loc == null)
                continue;

//            if (man.isInitialManeuver() && !(man instanceof LocatedManeuver)) {
//                LocationType cp = renderer.getCenter();
//                Point2D pt = renderer.getScreenPosition(cp);
//                g.translate(pt.getX(), pt.getY());
//                loc.setLocation(pt);
//            }
//            else {    
//                g.translate(loc.getX(), loc.getY());
//            }
            g.translate(loc.getX(), loc.getY());
            man.paintOnMap(g, this, renderer);
        }

        g.setTransform(oldTransform);
        
        for (Object pElmObj : pElementsObjs) {
            Graphics2D gT = (Graphics2D) gElements.create();
            ((IPlanElement<?>) pElmObj).getPainter().paint(gT, renderer);
            gT.dispose();
        }
        gElements.dispose();
    }

    public void setPlanZ(double z, ManeuverLocation.Z_UNITS units) {
        for (String key : maneuverLocations.keySet()) {
            Maneuver man = plan.getGraph().getManeuver(key);
            if (man != null && man instanceof LocatedManeuver) {
                ManeuverLocation lt = ((LocatedManeuver) man).getManeuverLocation();
                lt.setZ(z);
                lt.setZUnits(units);
                ((LocatedManeuver) man).setManeuverLocation(lt);
            }
        }
    }

    public void setPlanProperty(DefaultProperty property) {
        lastSetProperties.put(property.getName(), property);

        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            try {
                man.setProperties(new Property[] { property });
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
        }
    }

    public void translatePlan(double offsetNorth, double offsetEast, double offsetDown) {
        for (String key : maneuverLocations.keySet()) {
            Maneuver man = plan.getGraph().getManeuver(key);
            if (man != null && man instanceof LocatedManeuver) {
                ((LocatedManeuver) man).translate(offsetNorth, offsetEast, offsetDown);
            }
        }
        recalculateManeuverPositions(renderer);
        
        plan.getPlanElements().getPlanElements().stream()
                .forEach(pe -> pe.translate(offsetNorth, offsetEast, offsetDown));
    }

    public void rotatePlan(LocatedManeuver center, double ammount) {
        for (Maneuver m : plan.getGraph().getAllManeuvers()) {
            if (m != center && m instanceof LocatedManeuver) {
                LocatedManeuver satellite = (LocatedManeuver) m;
                double[] top = center.getManeuverLocation().getDistanceInPixelTo(satellite.getManeuverLocation(),
                        MapTileUtil.LEVEL_OFFSET);
                double[] topR = AngleUtils.rotate(2 * ammount, top[0], top[1], false);
                double deltaX = topR[0]; // distPx * Math.cos(anglePx);
                double deltaY = topR[1]; // distPx * Math.sin(anglePx);
                ManeuverLocation lt = new ManeuverLocation(center.getManeuverLocation());
                lt.translateInPixel(deltaX, deltaY, MapTileUtil.LEVEL_OFFSET);
                lt.setAbsoluteDepth(satellite.getManeuverLocation().getAllZ());
                lt.setZ(satellite.getManeuverLocation().getZ());
                lt.setZUnits(satellite.getManeuverLocation().getZUnits());
                satellite.setManeuverLocation(lt);
            }
        }
        recalculateManeuverPositions(renderer);
    }

    @Override
    public ParametersPanel getParametersPanel(boolean editable, MapType map) {
        return null;
    }

    @Override
    public void initialize(ParametersPanel paramsPanel) {

    }

    @Override
    public boolean containsPoint(LocationType point, StateRenderer2D renderer) {
        return false;
    }

    @Override
    public LocationType getCenterLocation() {
        return center;
    }

    @Override
    public void setCenterLocation(LocationType l) {
        this.center.setLocation(l);
    }

    public Point3d[] getPath() {

        Vector<Point3d> offsets3D = new Vector<Point3d>();
        Vector<Point3d> points = offsets3D;

        if (points == null || points.size() <= 1)
            return new Point3d[] {};

        Point3d[] pts = new Point3d[(points.size() * 2) - 2];
        int i = 1;
        int x = 1;
        pts[0] = (Point3d) points.firstElement();
        while (i < points.size() - 1) {
            pts[x] = (Point3d) points.get(i);
            x++;
            pts[x] = (Point3d) points.get(i);
            i++;
            x++;
        }
        pts[pts.length - 1] = (Point3d) points.get(points.size() - 1);

        return pts;
    }

    @Override
    public int getLayerPriority() {
        return 127;
    }

    public PlanType getPlan() {
        return plan;
    }

    public void setPlan(PlanType plan) {
        this.plan = plan;
        if (plan == null)
            return;
        if (renderer != null)
            recalculateManeuverPositions(renderer);
    }

    public StateRenderer2D getRenderer() {
        return renderer;
    }

    public void setRenderer(StateRenderer2D renderer) {
        this.renderer = renderer;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        if (renderer != null)
            renderer.repaint();
    }

    public boolean isShowDistances() {
        return showDistances;
    }

    public void setShowDistances(boolean showDistances) {
        this.showDistances = showDistances;
        if (renderer != null)
            renderer.repaint();
    }

    public boolean isShowManNames() {
        return showManNames;
    }

    public void setShowManNames(boolean showManNames) {
        this.showManNames = showManNames;
    }

    public boolean isShowVelocities() {
        return showVelocities;
    }

    public void setShowVelocities(boolean showVelocities) {
        this.showVelocities = showVelocities;
    }

    public boolean isSaveAsTrajectory() {
        return saveAsTrajectory;
    }

    public void setSaveAsTrajectory(boolean saveAsTrajectory) {
        this.saveAsTrajectory = saveAsTrajectory;
        plan.setSaveGotoSequenceAsTrajectory(saveAsTrajectory);
    }

    public void preferencesUpdated() {
        setColor(GeneralPreferences.rendererPlanColor);
    }

    public String getSelectedManeuver() {
        return selectedManeuver;
    }

    public void setSelectedManeuver(String selectedManeuver) {
        this.selectedManeuver = selectedManeuver;
    }

    public int getSnapPixels() {
        return snapPixels;
    }

    public void setSnapPixels(int snapPixels) {
        this.snapPixels = snapPixels;
    }

    public String getActiveManeuver() {
        return activeManeuver;
    }

    public void setActiveManeuver(String activeManeuver) {
        this.activeManeuver = activeManeuver;
        // setSelectedManeuver(activeManeuver);
    }

    public void setTransp2d(double transp2d) {
        this.transp2d = transp2d;
    }

    public double getTransp2d() {
        return transp2d;
    }

    public VehicleType getVehicleType() {
        return plan.getVehicleType();
    }

    @Override
    public ELEMENT_TYPE getElementType() {
        return ELEMENT_TYPE.TYPE_OTHER;
    }

    /**
     * @return the beingEdited
     */
    public boolean isBeingEdited() {
        return beingEdited;
    }

    /**
     * @param beingEdited the beingEdited to set
     */
    public void setBeingEdited(boolean beingEdited) {
        this.beingEdited = beingEdited;
    }

    /**
     * @return the lastSetProperties
     */
    public LinkedHashMap<String, DefaultProperty> getLastSetProperties() {
        return lastSetProperties;
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        MissionType mt = new MissionType("c:/APDL/missao-apdl.xml");
        MapGroup mg = MapGroup.getMapGroupInstance(mt);
        StateRenderer2D r2d = new StateRenderer2D(mg);
        MapType map = new MapType(new LocationType(mg.getCoordinateSystem()));
        mg.addMap(map);
        PlanElement po = new PlanElement(mg, map);
        PlanType plan = mt.getIndividualPlansList().values().iterator().next();
        po.setPlan(plan);
        map.addObject(po);

        GuiUtils.testFrame(r2d, "dsafs");
    }
}
