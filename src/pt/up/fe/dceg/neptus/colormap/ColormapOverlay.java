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
 * Dec 5, 2012
 */
package pt.up.fe.dceg.neptus.colormap;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 */
@LayerPriority(priority = -100)
public class ColormapOverlay implements Renderer2DPainter {

    protected LinkedHashMap<Point, Double> cellSums = new LinkedHashMap<>();
    protected LinkedHashMap<Point, Integer> count = new LinkedHashMap<>();
    protected LinkedHashMap<Point, Double> average = new LinkedHashMap<>();

    protected int minN = Integer.MAX_VALUE, minE = Integer.MAX_VALUE, maxN = Integer.MIN_VALUE,
            maxE = Integer.MIN_VALUE;
    protected double minVal = Double.MAX_VALUE, maxVal = -Double.MAX_VALUE;
    protected LocationType topLeft = null;

    protected int cellWidth = 3;
    protected LocationType ref = null;
    protected BufferedImage generated = null, scaled = null;
    protected String name;
    protected ColorMap colormap;
    protected ColorBar cb;
    protected boolean inverted;
    protected int transparency = 0;
    public ColormapOverlay(String name, int cellWidth, boolean inverted, int transparency) {
        this.name = name;
        this.cellWidth = cellWidth;
        this.inverted = inverted; 
        this.transparency = transparency;
    }
    
    public void addSampleUseMax(LocationType location, double value) {
        if (ref == null)
            ref = new LocationType(location.convertToAbsoluteLatLonDepth());

        double[] offsets = location.getOffsetFrom(ref);

        int northing = ((int) offsets[0]) / cellWidth * cellWidth;
        minN = Math.min(minN, northing);
        maxN = Math.max(maxN, northing);
        int easting = ((int) offsets[1]) / cellWidth * cellWidth;
        minE = Math.min(minE, easting);
        maxE = Math.max(maxE, easting);

        Point loc = new Point(northing, easting);
        if (!count.containsKey(loc)) {
            count.put(loc, 1);
            cellSums.put(loc, value);
        }
        else {
            if (cellSums.get(loc) > value)
                return;
            else
                cellSums.put(loc, value);            
        }
        
        minVal = Math.min(minVal, value);
        maxVal = Math.max(maxVal, value);
        
    }
    
    

    public void addSample(LocationType location, double value) {
        if (ref == null) {
            ref = new LocationType(location);
            ref.convertToAbsoluteLatLonDepth();
        }
        

        double[] offsets = location.getOffsetFrom(ref);

        int northing = ((int) offsets[0]) / cellWidth * cellWidth;
        minN = Math.min(minN, northing);
        maxN = Math.max(maxN, northing);
        int easting = ((int) offsets[1]) / cellWidth * cellWidth;
        minE = Math.min(minE, easting);
        maxE = Math.max(maxE, easting);

        minVal = Math.min(minVal, value);
        maxVal = Math.max(maxVal, value);

        Point loc = new Point(northing, easting);
        if (!count.containsKey(loc)) {
            count.put(loc, 1);
            cellSums.put(loc, value);
        }
        else {
            count.put(loc, count.get(loc) + 1);
            cellSums.put(loc, cellSums.get(loc) + value);
        }
    }

    public void computeAverage() {
        for (Entry<Point, Double> entry : cellSums.entrySet()) {
            average.put(entry.getKey(), entry.getValue() / count.get(entry.getKey()));
        }
    }

    public double[][] fillInData() {
        int startN = minN - cellWidth * 3, startE = minE - cellWidth * 3;
        int endN = maxN + cellWidth * 3, endE = maxE + cellWidth * 3;

        double[][] data = new double[(endE - startE) / cellWidth + 1][(endN - startN) / cellWidth + 1];

        for (int n = startN; n <= endN; n += cellWidth) {
            int y = (endN - n) / cellWidth;
            for (int e = startE; e <= endE; e += cellWidth) {
                int x = (e - startE) / cellWidth;
                Point ne = new Point(n, e);
                if (average.containsKey(ne))
                    data[x][y] = average.get(ne);
                else {
                    double sumValues = 0;
                    double sumDistance = 0;
                    int count = 0;
                    for (Entry<Point, Double> avg : average.entrySet()) {
                        double d = ne.distance(avg.getKey());
                        if (d > cellWidth * 3)
                            continue;
                        d = d * d * d;
                        count++;
                        if (d > 0.0) {
                            d = 1.0 / d;
                        }
                        else {
                            d = 1.e20;
                        }
                        sumDistance += d;
                        sumValues += d * avg.getValue();
                    }
                    if (count > 1)
                        data[x][y] = sumValues / sumDistance;
                    else
                        data[x][y] = minVal - 1;
                }
            }
        }

        return data;
    }
    
    public double[][] generateGrid(Point2D min, Point2D max) {
        computeAverage();
        min.setLocation(minN, minE);
        max.setLocation(maxN, maxE);
        return fillInData();
    }

    public BufferedImage generateImage(ColorMap colormap) {
        computeAverage();
        this.colormap = colormap;
        if (inverted)
            this.colormap = ColorMapUtils.invertColormap(this.colormap, 255);
        
        double[][] data = fillInData();
        topLeft = new LocationType(ref);
        topLeft.translatePosition(maxN + cellWidth * 3.5, minE - cellWidth * 3.5, 0);

        BufferedImage image;
        if (transparency == 0) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            image = gc.createCompatibleImage(data.length, data[0].length, Transparency.BITMASK);
        }
        else
            image = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
        
        maxVal = Math.ceil(maxVal);
        minVal = Math.floor(minVal);
        
        double amplitude = maxVal - minVal;
        Color c;
        for (int x = 0; x < data.length; x++)
            for (int y = 0; y < data[0].length; y++)
                if (data[x][y] >= minVal) {
                    c = this.colormap.getColor((data[x][y] - minVal) / (amplitude));
                    if (transparency != 0)
                        image.setRGB(x, y, new Color(c.getRed(), c.getGreen(), c.getBlue(), transparency).getRGB());
                    else
                        image.setRGB(x, y, c.getRGB());
                }
                
        return image;
    }

    double lastZoom = -1;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (generated == null)
            generated = generateImage(ColorMapFactory.createJetColorMap());

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        Point2D corner = renderer.getScreenPosition(topLeft);
        g.translate(corner.getX(), corner.getY());
        g.rotate(-renderer.getRotation());
            g.drawImage(generated, 0, 0, (int)(generated.getWidth()*cellWidth * renderer.getZoom()), (int)(generated.getHeight()*cellWidth*renderer.getZoom()),  0, 0, generated.getWidth(), generated.getHeight(),renderer);        
        g.rotate(renderer.getRotation());
        g.translate(-corner.getX(), -corner.getY());
        
        drawLegend(g);        
        
    }
    
    protected void drawLegend(Graphics2D g) {
        if (cb == null) {
            if (inverted)
                cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, ColorMapUtils.invertColormap(colormap, 80));
            else
                cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, colormap);
            cb.setSize(15, 80);
        }
        
        g.setColor(Color.black);
        Font prev = g.getFont();
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        
        g.setFont(prev);
        
        g.translate(5, 50);
        
        cb.paint(g);
        
        try {
            if (minVal != Double.MAX_VALUE)
                g.drawString(GuiUtils.getNeptusDecimalFormat(2).format((maxVal+minVal)/2), 15, 45);
            if (inverted) {
                if (minVal == Double.MAX_VALUE) {
                    g.drawString("+\u221E", 15, 80);            
                    g.drawString("-\u221E", 15, 10);
                }
                else {
                    g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(maxVal), 15, 80);      
                    g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(minVal), 15, 10);
                }
            }
            else {
                if (minVal == Double.MAX_VALUE) {
                    g.drawString("-\u221E", 15, 80);            
                    g.drawString("+\u221E", 15, 10);
                }
                else {
                    g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(maxVal), 15, 10);            
                    g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(minVal), 15, 80);
                } 
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }    

    public static void main(String[] args) throws Exception {
        LsfIndex index = new LsfIndex(new File(
                "/home/zp/Desktop/logs-noptilus/lauv-noptilus-1/20121106/154008_rows_noptilus/Data.lsf"));
        ColormapOverlay overlay = new ColormapOverlay("Test", 20, false, 0);
        for (EstimatedState x : index.getIterator(EstimatedState.class)) {
            LocationType loc = new LocationType(Math.toDegrees(x.getLat()), Math.toDegrees(x.getLon()));
            loc.translatePosition(x.getX(), x.getY(), 0);
            overlay.addSample(loc, x.getTimestamp());
        }

        StateRenderer2D renderer = new StateRenderer2D(overlay.ref);
        renderer.addPostRenderPainter(overlay, "Overlay");

        GuiUtils.testFrame(renderer);
    }
}
