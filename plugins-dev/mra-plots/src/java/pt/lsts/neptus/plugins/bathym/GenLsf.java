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
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 13/11/2020
 */
package pt.lsts.neptus.plugins.bathym;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.TreeSet;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCOutputStream;

/**
 * @author zp
 *
 */
public class GenLsf {

    private LinkedHashMap<Long, Point2D.Double> points = new LinkedHashMap<Long, Point2D.Double>();
    private TreeSet<Long> timestamps = new TreeSet<Long>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public long startTimestamp() {
        return timestamps.first();
    }

    public long endTimestamp() {
        return timestamps.last();
    }

    final static String log =
            "[2020/09/10 12:23:21] 41.59575 -8.81464\n" +
            "[2020/09/10 12:28:27] 41.59575 -8.81018\n" +
            "[2020/09/10 12:28:52] 41.59545 -8.81007\n" +
            "[2020/09/10 12:33:57] 41.59545 -8.81449\n" +
            "[2020/09/10 12:34:32] 41.59515 -8.81435\n" +
            "[2020/09/10 12:34:32] 41.59515 -8.81435\n" +
            "[2020/09/10 12:34:42] 41.59530 -8.81439\n" +
            "[2020/09/10 12:35:12] 41.59541 -8.81446\n" +
            "[2020/09/10 12:35:15] 41.59540 -8.81446\n" +
            "[2020/09/10 12:36:01] 41.59515 -8.81435\n" +
            "[2020/09/10 12:45:55] 41.59515 -8.80996\n" +
            "[2020/09/10 12:47:00] 41.59485 -8.80984\n" +
            "[2020/09/10 12:51:47] 41.59485 -8.81420\n" +
            "[2020/09/10 12:52:22] 41.59455 -8.81405\n" +
            "[2020/09/10 12:52:22] 41.59455 -8.81405\n" +
            "[2020/09/10 12:52:38] 41.59463 -8.81420\n" +
            "[2020/09/10 12:53:01] 41.59453 -8.81419\n" +
            "[2020/09/10 12:53:04] 41.59454 -8.81418\n" +
            "[2020/09/10 12:53:12] 41.59455 -8.81405\n" +
            "[2020/09/10 12:58:00] 41.59455 -8.80973\n" +
            "[2020/09/10 12:58:28] 41.59425 -8.80962\n" +
            "[2020/09/10 13:03:24] 41.59425 -8.81390\n" +
            "[2020/09/10 13:03:56] 41.59395 -8.81376\n" +
            "[2020/09/10 13:03:56] 41.59395 -8.81376\n" +
            "[2020/09/10 13:04:07] 41.59409 -8.81378\n" +
            "[2020/09/10 13:04:36] 41.59418 -8.81385\n" +
            "[2020/09/10 13:04:39] 41.59418 -8.81385\n" +
            "[2020/09/10 13:04:54] 41.59395 -8.81376\n" +
            "[2020/09/10 13:09:35] 41.59395 -8.80951\n" +
            "[2020/09/10 13:10:05] 41.59365 -8.80940\n" +
            "[2020/09/10 13:14:57] 41.59365 -8.81361\n" +
            "[2020/09/10 13:15:30] 41.59335 -8.81346\n" +
            "[2020/09/10 13:15:30] 41.59335 -8.81346\n" +
            "[2020/09/10 13:15:41] 41.59348 -8.81352\n" +
            "[2020/09/10 13:16:10] 41.59358 -8.81360\n" +
            "[2020/09/10 13:16:13] 41.59358 -8.81359\n" +
            "[2020/09/10 13:16:27] 41.59335 -8.81346\n" +
            "[2020/09/10 13:21:03] 41.59335 -8.80929\n" +
            "[2020/09/10 13:21:31] 41.59305 -8.80918\n" +
            "[2020/09/10 13:26:30] 41.59323 -8.81334\n" +
            "[2020/09/10 13:35:07] 41.59311 -8.81322\n" +
            "[2020/09/10 13:35:17] 41.59304 -8.81318\n";

    public GenLsf() throws Exception {
        String[] lines = log.split("\n");
        for (String line : lines) {
            String[] parts = line.split(" ");
            String date = parts[0].replaceAll("\\[", "");
            String time = parts[1].replaceAll("\\]", "");
            String lat = parts[2];
            String lon = parts[3];
            long timestamp = sdf.parse(date+" "+time).getTime();
            points.put(timestamp, new Point2D.Double(Double.parseDouble(lat), Double.parseDouble(lon)));
            timestamps.add(timestamp);
        }
    }

    public void getPosition(String date) throws Exception {
        long timestamp = sdf.parse(date).getTime();
        System.out.println(getPosition(timestamp));
    }

    public Point2D.Double getPosition(long timestamp) {
        Long prev = timestamps.floor(timestamp);
        Long next = timestamps.ceiling(timestamp);

        if (prev == null && next == null)
            return null;

        if (next == prev) {
            return points.get(next);
        }

        if (prev == null && next != null)
            return points.get(next);

        if (prev != null && next == null)
            return points.get(prev);

        Point2D.Double ptPrev = points.get(prev);
        Point2D.Double ptNext = points.get(next);

        double distX = ptNext.x - ptPrev.x;
        double distY = ptNext.y - ptPrev.y;
        double dist = (timestamp - prev) / (double)(next - prev);
        return new Point2D.Double(ptPrev.x + dist * distX, ptPrev.y + dist * distY);
    }

    public static void main(String[] args) throws Exception {
        GenLsf gen = new GenLsf();
        IMCOutputStream ios = new IMCOutputStream(new FileOutputStream(new File("/home/zp/Desktop/Data.lsf")));
        for (long timestamp = gen.startTimestamp(); timestamp < gen.endTimestamp(); timestamp += 500) {
            Point2D.Double pt = gen.getPosition(timestamp);
            EstimatedState state = new EstimatedState();
            state.setLat(Math.toRadians(pt.x));
            state.setLon(Math.toRadians(pt.y));
            state.setTimestamp(timestamp/1000.0);
            state.setSrc(28);
            ios.writeMessage(state);
        }
        ios.close();
    }
}
