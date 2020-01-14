/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 1, 2019
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import com.google.common.eventbus.Subscribe;

import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.Checksum;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.TalkerId;
import net.sf.marineapi.nmea.util.GpsFixQuality;
import net.sf.marineapi.nmea.util.Position;
import net.sf.marineapi.nmea.util.Time;
import net.sf.marineapi.nmea.util.Units;
import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticOperation.OP;
import pt.lsts.imc.UsblFixExtended;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="BlueROV USBL")
public class BlueRovUSBL extends ConsoleLayer {

    @NeptusProperty(description="System to range", userLevel=LEVEL.REGULAR)
    public String system = "squirtle";

    @NeptusProperty(description="Gateway", userLevel=LEVEL.REGULAR)
    public String gateway = "manta-21";

    @NeptusProperty(description="Range period (seconds). Use -1 for disabling automatic range.", userLevel=LEVEL.REGULAR)
    public int range_period = 3;

    @NeptusProperty(description="Send GPS to Vehicle", userLevel=LEVEL.REGULAR)
    public boolean sendToVehicle = false;

    @NeptusProperty(description="USBL Angle Bias")
    public double angleBias = 0;

    @NeptusProperty(description="Positions to store")
    int posSize = 20;

    @NeptusProperty(description="ROV Hostname", userLevel=LEVEL.REGULAR)
    public String rovHost = "192.168.2.2";

    @NeptusProperty(description="ROV Port", userLevel=LEVEL.REGULAR)
    public int rovPort = 27000;

    @NeptusProperty(description="Fixes color", userLevel=LEVEL.REGULAR)
    public Color fixesColor = Color.RED.darker();


    private ArrayList<LocationType> positions = new ArrayList<>();

    private int range_counter = 0;

    private static String getGGASentence(LocationType loc) {
        GGASentence sentence = (GGASentence) SentenceFactory.getInstance().createParser(TalkerId.GA, SentenceId.GGA);
        sentence.setPosition(new Position(loc.getLatitudeDegs(), loc.getLongitudeDegs()));
        sentence.setFixQuality(GpsFixQuality.ESTIMATED);
        sentence.setSatelliteCount(8);
        sentence.setHorizontalDOP(1.5);
        sentence.setAltitude(0);
        sentence.setAltitudeUnits(Units.METER);
        sentence.setGeoidalHeight(0);
        sentence.setGeoidalHeightUnits(Units.METER);
        sentence.setTime(new Time());
        String nmea = sentence.toSentence();
        nmea = "$" + nmea.substring(3);
        nmea = Checksum.add(nmea);
        
        return nmea;
    }
    
    @Subscribe
    public void on(UsblFixExtended usbl) {
        
        System.err.println("Received fix from "+usbl.getSourceName());
        
        /*ImcSystem gateway = ImcSystemsHolder.getSystemWithName(usbl.getSourceName());
        if (gateway == null) {
            NeptusLog.pub().error("Unable to find address of "+usbl.getSourceName());
            return;
        }*/
        LocationType loc = new LocationType(Math.toDegrees(usbl.getLat()), Math.toDegrees(usbl.getLon()));

        synchronized (positions) {
            positions.add(loc);   

            if (positions.size() > posSize)
                positions.remove(0);
        }

        if (sendToVehicle) {
            try {
                byte[] nmea = getGGASentence(loc).getBytes(StandardCharsets.US_ASCII);
                DatagramPacket packet = new DatagramPacket(nmea, nmea.length);
                packet.setSocketAddress(new InetSocketAddress(rovHost, rovPort));
                DatagramSocket socket = new DatagramSocket();

                socket.send(packet);
                socket.close();

                System.out.println("sent '" + new String(nmea) + "' to " + rovHost + " / " + rovPort);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Periodic(millisBetweenUpdates=1000)
    public void rangeSystem() {
        
        if (range_period < 0)
            return;
        
        if (range_counter-- == 0) {
            AcousticOperation op = new AcousticOperation();
            op.setOp(OP.RANGE);
            op.setSystem(system);
            ImcMsgManager.getManager().sendMessageToSystem(op, gateway);
            NeptusLog.pub().info("Ranging "+system+" via "+gateway);
        }

        if (range_counter < 0)
            range_counter = range_period;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        ArrayList<LocationType> posCopy = new ArrayList<>();

        synchronized (positions) {
            posCopy.addAll(positions);
        }

        g.setColor(new Color(255-fixesColor.getRed(), 255-fixesColor.getGreen(), 255-fixesColor.getGreen(), 128));
        
        for (LocationType pos : posCopy) {
            Point2D pt = renderer.getScreenPosition(pos);
            g.draw(new Ellipse2D.Double(pt.getX()-4, pt.getY()-4, 8, 8));
        }
        
        if (!posCopy.isEmpty()) {
            Point2D pt = renderer.getScreenPosition(posCopy.get(posCopy.size() -1));
            g.setColor(fixesColor);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Line2D.Double(pt.getX()-4, pt.getY()-4, pt.getX()+4, pt.getY()+4));
            g.draw(new Line2D.Double(pt.getX()+4, pt.getY()-4, pt.getX()-4, pt.getY()+4));
        }
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
   
    }

    @Override
    public void cleanLayer() {
   
    }
    
    public static void main(String[] args) {
        LocationType loc = new LocationType(41, -8);
        String gga = getGGASentence(loc);
        System.out.println(gga);
    }
}
