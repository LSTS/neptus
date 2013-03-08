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
 * Nov 13, 2012
 * $Id:: LblRangesPlot.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.LinkedHashMap;
import java.util.Vector;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.imc.types.LblConfigAdapter;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 *
 */
public class LblRangesPlot extends MraTimeSeriesPlot {

    protected LinkedHashMap<Integer, Color> beaconColors = new LinkedHashMap<>();
    protected LinkedHashMap<Integer, Shape> beaconShapes = new LinkedHashMap<>();
    protected Vector<Color> acceptedColors = new Vector<>();
    protected Vector<Color> rejectedColors = new Vector<>();
    protected Vector<Shape> possibleShapes = new Vector<>();
    
    {
        acceptedColors.add(Color.blue);
        acceptedColors.add(Color.green.darker());
        acceptedColors.add(Color.cyan);
        acceptedColors.add(new Color(128,0,255));
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
                beaconColors.put(i*2, acceptedColors.remove(0));
                beaconColors.put(i*2+1, rejectedColors.remove(0));
                beaconShapes.put(i*2, possibleShapes.get(0));
                beaconShapes.put(i*2+1, possibleShapes.remove(0));
            }
            beaconNames.put(i++, b.getBeacon());            
            addTrace("Accepted."+b.getBeacon());
            addTrace("Rejected."+b.getBeacon());
        }
        
        
        for (IMCMessage msg : source.getIterator("LblRangeAcceptance")) {
            String beaconName = msg.getString("id");
            if (beaconNames.containsKey(msg.getInteger("id")))
                beaconName = beaconNames.get(msg.getInteger("id"));
        
            if (msg.getString("acceptance").equals("ACCEPTED"))
                addValue(msg.getTimestampMillis(), "Accepted."+beaconName, msg.getDouble("range"));
            else
                addValue(msg.getTimestampMillis(), "Rejected."+beaconName, msg.getDouble("range"));                
        }
    }
    
    
    @Override
    public JFreeChart createChart() {
          JFreeChart chart = super.createChart();

          ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setShapesVisible(true);
          ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setLinesVisible(false);

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
