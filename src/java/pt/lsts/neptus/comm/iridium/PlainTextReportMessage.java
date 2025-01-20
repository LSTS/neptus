/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 12 Dec, 2021
 */
package pt.lsts.neptus.comm.iridium;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCInputStream;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.NeptusLog;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zp
 *
 */
public class PlainTextReportMessage extends IridiumMessage {

    private static final Pattern p0 = Pattern.compile("\\((.)\\) \\((.*)\\) (.*) / (.*), (.*) / f:(\\d*) v:(\\d*) c:(\\d*) / s: ?(.)(.*?)");
    private static final Pattern p = Pattern.compile("\\((.)\\) \\((.*)\\) (.*) / (.*), (.*) / .*");

    String report;

    String vehicle;
    String vehicleAlt;
    String timeOfDay;
    int source = 0xFFFF;
    double latDeg;
    double lonDeg;

    double fuelPercentage = -1;
    double batteryVoltage = -1;
    double batteryConfidencePercentage = -1;

    String statusIndicator = "";

    public PlainTextReportMessage() {
        super(-1);
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.write(report.getBytes(StandardCharsets.ISO_8859_1));
        out.close();
        return report.getBytes(StandardCharsets.ISO_8859_1).length;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        int bav = in.available();
        bav = Math.max(bav, 0);
        byte[] data = new byte[bav];
        int len = in.read(data);
        report = new String(data, StandardCharsets.ISO_8859_1);
        parse();
        return len;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        List<IMCMessage> msgs = new ArrayList<>();
        TextMessage msg = new TextMessage("iridium", report);
        msg.setSrc(source);
        msg.setTimestampMillis(timestampMillis);
        msgs.add(msg);
        return msgs;
    }

    @Override
    public String toString() {
        return "Report: " + report + "\n"
                + "Vehicle: " + vehicle + "\n"
                + "Vehicle Alt: " + (vehicleAlt == null ? "" : vehicleAlt) + "\n"
                + "Time of day: " + timeOfDay + "\n"
                + "Lat: " + latDeg + "\n"
                + "Lon: " + lonDeg + "\n"
                + "Fuel: " + fuelPercentage + "\n"
                + "Battery: " + batteryVoltage + "\n"
                + "Battery confidence: " + batteryConfidencePercentage + "\n"
                + "Status: " + statusIndicator;

    }

    private void parse() throws Exception {
        Matcher matcher = p0.matcher(report);
        if (!matcher.matches()) {
            matcher = p.matcher(report);
            if (!matcher.matches()) {
                throw new Exception("Invalid report format: " + report);
            }
        }

        vehicle = matcher.group(2);
        String[] tks = vehicle.split(" - ");
        if (tks.length > 1) {
            vehicleAlt = vehicle.replaceFirst(tks[0], "").trim();
            vehicle = tks[0];
        }
        timeOfDay = matcher.group(3);
        String latMins = matcher.group(4);
        String lonMins = matcher.group(5);
        source = IMCDefinition.getInstance().getResolver().resolve(vehicle);
        if (source == -1) {
            return;
        }
        String[] latParts = latMins.split(" ");
        String[] lonParts = lonMins.split(" ");
        latDeg = getCoords(latParts);
        lonDeg = getCoords(lonParts);

        if (matcher.groupCount() <= 6)
            return;

        fuelPercentage = Double.parseDouble(matcher.group(6));
        batteryVoltage = Double.parseDouble(matcher.group(7)) / 10.0;
        batteryConfidencePercentage = Double.parseDouble(matcher.group(8));
        statusIndicator = matcher.group(9);
    }

    private double getCoords(String[] coordParts) {
        double coord = Double.parseDouble(coordParts[0]);
        coord += (coord > 0) ? Double.parseDouble(coordParts[1]) / 60.0 : -Double.parseDouble(coordParts[1]) / 60.0;
        return coord;
    }

    public static void main(String[] args) {
        String hexMsg = "28542920286c6175762d736561636f6e2d33292031323a32353a3433202f2034312031312e3131383035302c202d382034322e323837393530202f20663a393020763a32383920633a313030202f20733a2053";
        String textMsg = "(T) (lauv-seacon-3) 12:25:43 / 41 11.118050, -8 42.287950 / f:90 v:289 c:100 / s: S";

        HexBinaryAdapter hexAdapter = new HexBinaryAdapter();
        byte[] bytesMsh = hexAdapter.unmarshal(hexMsg);

        IMCInputStream iis = new IMCInputStream(new ByteArrayInputStream(bytesMsh), IMCDefinition.getInstance());
        iis.setBigEndian(false);
        PlainTextReportMessage txtIridium = new PlainTextReportMessage();
        try {
            txtIridium.deserializeFields(iis);
            NeptusLog.pub().info("Received a plain text from " + txtIridium.report);
            System.out.println("Received a plain text from " + txtIridium);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
        }
    }
}
