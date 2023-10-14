/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Nov 13, 2012
 */
package pt.lsts.neptus.mra.plots;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.types.LblConfigAdapter;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 * 
 */
@PluginDescription
public class LblRangesPlot extends MRATimeSeriesPlot {

    protected LinkedHashMap<Integer, Color> beaconColors = new LinkedHashMap<>();
    protected LinkedHashMap<Integer, Shape> beaconShapes = new LinkedHashMap<>();
    protected Vector<Color> acceptedColors = new Vector<>();
    protected Vector<Color> rejectedColors = new Vector<>();
    protected Vector<Shape> possibleShapes = new Vector<>();

    {
        acceptedColors.add(Color.blue);
        acceptedColors.add(Color.green.darker());
        acceptedColors.add(Color.cyan);
        acceptedColors.add(new Color(128, 0, 255));
        rejectedColors.add(Color.red);
        rejectedColors.add(new Color(255, 160, 0));
        rejectedColors.add(Color.magenta);
        rejectedColors.add(Color.black);

        possibleShapes.add(new Ellipse2D.Double(0, 0, 5, 5));
        possibleShapes.add(new Rectangle2D.Double(0, 0, 5, 5));

        GeneralPath gp = new GeneralPath();
        gp.moveTo(0, 2.5);
        gp.lineTo(2.5, -2.5);
        gp.lineTo(-2.5, -2.5);
        gp.closePath();
        possibleShapes.add(gp);

        gp = new GeneralPath();
        gp.moveTo(0, 2.5);
        gp.lineTo(2.5, 0);
        gp.lineTo(0, -2.5);
        gp.lineTo(-2.5, 0);
        gp.closePath();
        possibleShapes.add(gp);

    }

    public LblRangesPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public boolean canBeApplied(LsfIndex index) {
        return index.containsMessagesOfType("LblRangeAcceptance");
    }

    @Override
    public String getName() {
        return I18n.text("LBL ranges");
    }

    @Override
    public String getTitle() {
        return getName();
    }

    @Override
    public void process(LsfIndex source) {

        IMCMessage config = source.getMessage(source.getLastMessageOfType("LblConfig"));
        LinkedHashMap<Integer, String> beaconNames = new LinkedHashMap<>();

        LblConfigAdapter adapter = new LblConfigAdapter();
        adapter.setData(config);
        int i = 0;
        for (LblBeacon b : adapter.getBeacons()) {
            if (!acceptedColors.isEmpty()) {
                beaconColors.put(i * 2, acceptedColors.remove(0));
                beaconColors.put(i * 2 + 1, rejectedColors.remove(0));
                beaconShapes.put(i * 2, possibleShapes.get(0));
                beaconShapes.put(i * 2 + 1, possibleShapes.remove(0));
            }
            beaconNames.put(i++, b.getBeacon());
            addTrace(I18n.text("Accepted.") + b.getBeacon());
            addTrace(I18n.text("Rejected.") + b.getBeacon());
        }

        for (IMCMessage msg : source.getIterator("LblRangeAcceptance")) {
            String beaconName = msg.getString("id");
            if (beaconNames.containsKey(msg.getInteger("id")))
                beaconName = beaconNames.get(msg.getInteger("id"));

            if (msg.getString("acceptance").equals("ACCEPTED"))
                addValue(msg.getTimestampMillis(), I18n.text("Accepted.") + beaconName, msg.getDouble("range"));
            else
                addValue(msg.getTimestampMillis(), I18n.text("Rejected.") + beaconName, msg.getDouble("range"));
        }
    }

    @Override
    public JFreeChart createChart() {
        JFreeChart chart = super.createChart();

        for (int i : beaconColors.keySet())
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesPaint(i, beaconColors.get(i));

        for (int i : beaconShapes.keySet())
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesShape(i, beaconShapes.get(i));

        return chart;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }
}
