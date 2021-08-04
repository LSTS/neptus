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
 * Author: zepinto
 * 09/01/2018
 */
package pt.lsts.neptus.endurance;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Date;

import pt.lsts.imc.Maneuver;
import pt.lsts.neptus.types.coord.LocationType;

import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.DateTimeUtil;

public class Waypoint implements Comparable<Waypoint> {

    private int id, duration = 0;
    private float latitude, longitude;
    private Date arrivalTime = null;

    public static LocationType locationOf(Maneuver man) {
        return new LocationType(Math.toDegrees(man.getDouble("lat")), Math.toDegrees(man.getDouble("lon")));
    }

    public Waypoint(int id, Maneuver man) throws Exception {
        this.id = id;
        this.latitude = (float) Math.toDegrees(man.getDouble("lat"));
        this.longitude = (float) Math.toDegrees(man.getDouble("lon"));

        // if (man.getInteger("duration") != null)
        this.duration = man.getInteger("duration");

        if (man.getInteger("arrival_time") != 0)
            this.arrivalTime = new Date(man.getInteger("arrival_time") * 1000l);
    }

    public Waypoint(int id, float lat, float lon) {
        this.latitude = lat;
        this.longitude = lon;
        this.id = id;
    }

    public Waypoint clone() {
        Waypoint wpt = new Waypoint(id, latitude, longitude);
        wpt.setDuration(duration);
        if (arrivalTime != null)
            wpt.setArrivalTime(new Date(arrivalTime.getTime()));

        return wpt;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Date getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Date arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getId() {
        return id;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    @Override
    public int compareTo(Waypoint o) {

        if (arrivalTime == null && o.arrivalTime == null)
            return Long.valueOf(getId()).compareTo(Long.valueOf(o.getId()));

        if (arrivalTime == null && o.arrivalTime != null)
            return 1;

        if (arrivalTime != null && o.arrivalTime == null)
            return -1;

        return arrivalTime.compareTo(o.arrivalTime);
    }

    private Date nextSchedule() {
        return arrivalTime;
    }

    public void paint(Graphics2D g, Waypoint wpt, Point2D pt2d, Color color, int radius, boolean smallLabel) {
        if (wpt.getArrivalTime() != null && wpt.getArrivalTime().before(new Date())) {
            paintCircle(pt2d, color, 8, g);
            g.setColor(Color.black);
            String minsToEta = smallLabel ? DateTimeUtil.timeFormatterNoSegs.format(wpt.getArrivalTime()) : "ETA: -" + DateTimeUtil
                    .milliSecondsToFormatedString(-wpt.getArrivalTime().getTime() + System.currentTimeMillis());
            g.drawString(minsToEta, (int) pt2d.getX() + 6, (int) pt2d.getY() - 3);
        }
        else {
            String minsToEta = "ETA: ?";
            if (wpt.getArrivalTime() != null)
                minsToEta =  smallLabel ? DateTimeUtil.timeFormatterNoSegs.format(wpt.getArrivalTime()) : "ETA: " + DateTimeUtil
                        .milliSecondsToFormatedString(wpt.getArrivalTime().getTime() - System.currentTimeMillis());
            paintCircle(pt2d, Color.green.darker(), radius, g);
            g.setColor(Color.black);
            g.drawString(minsToEta, (int) pt2d.getX() + 6, (int) pt2d.getY() - 3);
        }
    }

    private void paintCircle(Point2D pt2d, Color color, int radius, Graphics2D g) {
        g.setPaint(new GradientPaint((float) pt2d.getX() - radius, (float) pt2d.getY(), color,
                (float) pt2d.getX() + radius, (float) pt2d.getY()+radius, color.darker().darker().darker()));
        g.fill(new Ellipse2D.Double(pt2d.getX() - radius, pt2d.getY() - radius, radius*2, radius*2));
        g.setStroke(new BasicStroke(0.5f));
        g.setColor(Color.black);
        g.draw(new Ellipse2D.Double(pt2d.getX() - radius, pt2d.getY() - radius, radius*2, radius*2));
    }
    

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        Waypoint wpt = new Waypoint(0, 41, -8);
        wpt.arrivalTime = new Date(17, 8, 24, 17, 42, 00);
        System.out.println(wpt.nextSchedule());
    }

}
