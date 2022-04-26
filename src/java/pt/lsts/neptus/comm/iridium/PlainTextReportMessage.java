/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Collection;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zp
 *
 */
public class PlainTextReportMessage extends IridiumMessage {

    private static Pattern p = Pattern.compile("\\((.)\\) \\((.*)\\) (.*) / (.*), (.*) / .*");

    String report;

    String vehicle;
    String timeOfDay;
    int source = 0xFFFF;
    double latDeg;
    double lonDeg;

    public PlainTextReportMessage() {
        super(-1);
    }

    @Override
    public int serializeFields(IMCOutputStream out) throws Exception {
        out.write(report.getBytes("ISO-8859-1"));
        out.close();
        return report.getBytes("ISO-8859-1").length;
    }

    @Override
    public int deserializeFields(IMCInputStream in) throws Exception {
        int bav = in.available();
        bav = bav < 0 ? 0 : bav;
        byte[] data = new byte[bav];
        int len = in.read(data);
        report = new String(data, "ISO-8859-1");
        parse();
        return len;
    }

    @Override
    public Collection<IMCMessage> asImc() {
        Vector<IMCMessage> msgs = new Vector<>();
        msgs.add(new TextMessage("iridium", report));
        return msgs;
    }

    @Override
    public String toString() {
        return "Report: " + report + "\n";
    }

    private void parse() {
        Matcher matcher = p.matcher(report);
        if (!matcher.matches()) {
            return;
        }
        vehicle = matcher.group(2);
        timeOfDay = matcher.group(3);
        String latMins = matcher.group(4);
        String lonMins = matcher.group(5);
        source = IMCDefinition.getInstance().getResolver().resolve(vehicle);
        if (source == -1) {
            return;
        }
        String latParts[] = latMins.split(" ");
        String lonParts[] = lonMins.split(" ");
        latDeg = getCoords(latParts);
        lonDeg = getCoords(lonParts);
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
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
        }
    }
}
