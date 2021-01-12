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
package pt.lsts.neptus.colormap;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.neptus.colormap.DataDiscretizer.DataPoint;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.ws.Location;

/**
 * @author ZP
 */

public class ColorMapUtils {

    public static final int POWER = 2;
    public static final int K_NEIGHBORS = 25000;
    
    
    static class NeighborDistance implements Comparable<NeighborDistance> {
        
        public double distance = 0;
        public double value = 0;
        
        
        public NeighborDistance(double value, double distance) {
            this.value = value;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(NeighborDistance o) {
            return ((Double)distance).compareTo(o.distance);
        }
    }
    
	private static void generateInterpolatedColorMap(Rectangle2D bounds, Point2D[] points, Double[] values, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap, double min, double max) {
		if (points.length != values.length || points.length == 0) {
			//NeptusLog.pub().info("<###> "+points.length+", "+values.length);
			
			System.err.println("Number of locations must be positive and match the number of values!");
			return;			
		}
	
		int numX = width >= 100 ? 100 : (int)width;
		int numY = height >= 100 ? 100 : (int) height;
		int numValues = numX * numY;

		double[] xGrid = new double[numValues];
		double[] yGrid = new double [numValues];
		double[] zGrid = new double [numValues];
		
		double minX = bounds.getMinX();
		double maxX = bounds.getMaxX();
		double minY = bounds.getMinY();
		double maxY = bounds.getMaxY();
		
		double dxGrid = (maxX - minX) / (numX);
		double dyGrid = (maxY - minY) / (numY);			
		
		double x = 0.0;
		for (int i = 0; i < numX; i++) {
			if (i == 0) {
				x = minX;
			}
			else {
				x += dxGrid;
			}
			double y = 0.0;
			for (int j = 0; j < numY; j++) {
				int k = numY * i + j;
				xGrid[k] = x;
				if (j == 0) {
					y = minY;
				}
				else {
					y += dyGrid;
				}
				yGrid[k] = y;
			}
		}

        double xPt, yPt, d, dTotal;
        
       for (int kGrid = 0; kGrid < xGrid.length; kGrid++) {
            dTotal = 0.0;
			zGrid[kGrid] = 0.0;
			Vector<NeighborDistance> neighbors = new Vector<ColorMapUtils.NeighborDistance>();
			
			for (int k = 0; k < values.length; k++) {
				
				if (values[k] == null)
					continue;
				
                xPt = points[k].getX();
                yPt = points[k].getY();
                d = Point2D.distance(xPt, yPt, xGrid[kGrid], yGrid[kGrid]);
                neighbors.add(new NeighborDistance(values[k], d));
			}
			
			Collections.sort(neighbors);
			
			for (int i = 0; i < neighbors.size() && i < K_NEIGHBORS; i++) {
			    d = neighbors.get(i).distance;
			    double val = neighbors.get(i).value;
			    
			    if (POWER != 1) {
                    d = Math.pow(d, POWER);
                }
                //d = Math.sqrt(d);
                if (d > 0.0) {
                    d = 1 / d;
                }
                else { // if d is real small set the inverse to a large number 
                    // to avoid INF
                    d = 1.e20;
                }
                zGrid[kGrid] += val * d; 

                dTotal += d;
			}
			zGrid[kGrid] = zGrid[kGrid] / dTotal;  //remove distance of the sum
		}
		
		
		//double min = zGrid[0], max = zGrid[0];
		for (int i = 0; i < zGrid.length; i++) {
			if (zGrid[i] < min)
				min = zGrid[i];
			if (zGrid[i] > max)
				max = zGrid[i];				
		}
		//System.out.pr

		for (int i = 0; i < zGrid.length; i++) {
			zGrid[i] = (zGrid[i] - min) / (max-min);
			//NeptusLog.pub().info("<###> "+zGrid[i]);
		}
		
		double vals[][] = new double[numX][numY];
		
		for (int i = 0; i < numX; i++) {
			for (int j = 0; j < numY; j++) {
				vals[i][j] = zGrid[i*numX+j];
			}
		}
		
		getInterpolatedData(vals, colorMap, destination, width, height, alpha);		

	}
	
    /**
     * 
     * @param bounds limits of the covered area
     * @param data point to use to interpolate
     * @param var
     * @param destination image to draw in
     * @param width of the target image
     * @param height of the target image
     * @param alpha transparency
     * @param colorMap the colorMap (greyScale etc)
     * @param minValue the minimum value in the dataset (affects colors)
     * @param maxValue the maximum value in the dataset (affects colors)
     */
	public static void generateInterpolatedColorMap(Rectangle2D bounds, DataPoint[] data, int var, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap, double minValue, double maxValue) {
		
		Point2D[] points = new Point2D[data.length];
		Double[] values = new Double[data.length];
		
		for (int i = 0; i < data.length; i++) {
			points[i] = data[i].getPoint2D();
			values[i] = data[i].getValues()[var];
		}
		
		generateInterpolatedColorMap(bounds, points, values, destination, width, height, alpha, colorMap, minValue, maxValue);
	}
	
	public static void generateColorMap(DataPoint[] data, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap) {
		Point2D[] points = new Point2D[data.length];
		Double[] values = new Double[data.length];
		
		for (int i = 0; i < data.length; i++) {
			points[i] = data[i].getPoint2D();
			values[i] = data[i].getValue();
		}
		
		generateColorMap(points, values, destination, width, height, alpha, colorMap, false);
	}
	
	public static ColorMap invertColormap(ColorMap original, int numColors) {
        
        double[] values = new double[numColors];
        Color[] colors = new Color[numColors];
        if (original instanceof InterpolationColorMap)
            return ColorMapFactory.createInvertedColorMap((InterpolationColorMap)original);
        
        for (int i = 0; i < numColors; i++) {
            values[i] = i / (double)numColors; 
            colors[i] = original.getColor(1-(i / (double)numColors));
        }
        
        return new InterpolationColorMap(values, colors);
    }
	
	
	private static void generateInterpolatedColorMap(Point2D[] points, Double[] values, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap) {
		
		if (points.length != values.length || points.length == 0) {
			//NeptusLog.pub().info("<###> "+points.length+", "+values.length);
			System.err.println("Number of locations must be positive and match the number of values!");
			return;			
		}
				
		double minX = points[0].getX();
		double maxX = minX;
		double minY = points[0].getY();
		double maxY = minY;
		
		
		for (int i = 0; i < points.length; i++) {
			if (points[i].getX() < minX)
				minX = points[i].getX();
			else if (points[i].getX() > maxX)
				maxX = points[i].getX();
			
			if (points[i].getY() < minY)
				minY = points[i].getY();
			else if (points[i].getY() > maxY)
				maxY = points[i].getY();
		}
		
		Rectangle2D bounds = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		
		
		
		generateInterpolatedColorMap(bounds, points, values, destination, width, height, alpha, colorMap, getMin(values), getMax(values));
	}
	
	private static double getMin(Double[] values) {
		Double min = null;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null)
				continue;
			if (min == null || values[i] < min)
				min = values[i];
		}
		return min;
	}
	
	private static double getMax(Double[] values) {
		Double max = null;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null)
				continue;
			if (max == null || values[i] > max)
				max = values[i];
		}
		return max;
	}

	private static void generateInterpolatedColorMap(Location[] locs, Double[] values, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap) {

		if (locs.length != values.length || locs.length == 0) {
			//NeptusLog.pub().info("<###> "+locs.length+", "+values.length);
			System.err.println("Number of locations must be positive and match the number of values!");
			return;			
		}

		Point2D[] points = new Point2D[locs.length];
		double minLong = -locs[0].getAbsoluteLongitudeDouble();
		double maxLat = locs[0].getAbsoluteLatitudeDouble();
		
		for (int i = 0; i < locs.length; i++) {
			if (-locs[i].getAbsoluteLongitudeDouble() < minLong)
				minLong = -locs[i].getAbsoluteLongitudeDouble();
			if (locs[i].getAbsoluteLatitudeDouble() > maxLat)
				maxLat = locs[i].getAbsoluteLatitudeDouble();			
		}

		Location topCorner = new Location(maxLat, minLong);
		
		double[] of = locs[0].getNEDOffsetFrom(topCorner);
		double minX = of[1];
		double minY = -of[0];
		double maxX = of[1];
		double maxY = -of[0];

		for (int i = 0; i < locs.length; i++) {
			double[] offsets = locs[i].getNEDOffsetFrom(topCorner);
			points[i] = new Point2D.Double(offsets[1], -offsets[0]);

			if (-offsets[0] > maxY)
				maxY = -offsets[0];
			if (offsets[1] > maxX)
				maxX = offsets[1];
			if (-offsets[0] < minY)
				minY = -offsets[0];
			if (offsets[1] < minX)
				minX = offsets[1];
		}
		
		generateInterpolatedColorMap(points, values, destination, width, height, alpha, colorMap);

	}

	public static void getInterpolatedData(double[][] data, ColorMap colormap, Graphics2D bg, double width, double height, int alpha) {
        BufferedImage tmp = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
                // Color c = colormap.getColor(data[i][j]);
                // c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
                // tmp.setRGB(i, j, c.getRGB());

                tmp.setRGB(i, j, colormap.getColor(data[i][j]).getRGB());
			}
		}

		//Graphics2D bg = destination.createGraphics();
		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		bg.scale(width/tmp.getWidth(),
				height/tmp.getHeight());

		bg.drawImage(tmp, 0, 0, null);
	}
		
	
	public static void generateColorMap(Rectangle2D bounds, Point2D[] points, Double[] vals, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap, double min, double max) {
		
		generateInterpolatedColorMap(bounds, points, vals, destination, width, height, alpha, colorMap, min, max);
		double minX = bounds.getMinX();
		double maxX = bounds.getMaxX();
		double minY = bounds.getMinY();
		double maxY = bounds.getMaxY();
		
		
		Graphics2D g = destination;
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.black);
		
		double scaleX = (width) / (maxX - minX);
		double scaleY = (height) / (maxY - minY);
		for (int i = 0; i < points.length; i++) {
			double dx = (points[i].getX() - minX) * scaleX;
			double dy = (points[i].getY() - minY) * scaleY;
			g.translate(dx, dy);
			g.drawLine(-3, -3, 3, 3);
			g.drawLine(-3, 3, 3, -3);
			g.drawString(""+vals[i], 10, 10);
			g.translate(-dx,-dy);
		}
	}
	
	public static Rectangle2D getBounds(DataPoint[] points) {
		if (points.length == 0)
			return null;
		
		double minX = points[0].getPoint2D().getX();
		double maxX = minX;
		double minY = points[0].getPoint2D().getY();
		double maxY = minY;
				
		for (int i = 0; i < points.length; i++) {
			Point2D pt = points[i].getPoint2D();
			
			if (pt.getX() < minX)
				minX = pt.getX();
			else if (pt.getX() > maxX)
				maxX = pt.getX();
			
			if (pt.getY() < minY)
				minY = pt.getY();
			else if (pt.getY() > maxY)
				maxY = pt.getY();
		}
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
		
	public static Rectangle2D getBounds(Point2D[] points) {
		if (points.length == 0)
			return null;
		
		double minX = points[0].getX();
		double maxX = minX;
		double minY = points[0].getY();
		double maxY = minY;
				
		for (int i = 0; i < points.length; i++) {
			if (points[i].getX() < minX)
				minX = points[i].getX();
			else if (points[i].getX() > maxX)
				maxX = points[i].getX();
			
			if (points[i].getY() < minY)
				minY = points[i].getY();
			else if (points[i].getY() > maxY)
				maxY = points[i].getY();
		}
		return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
	}
	
	public static void generateColorMap(Point2D[] points, Double[] vals, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap, boolean drawPoints) {
		generateInterpolatedColorMap(points, vals, destination, width, height, alpha, colorMap);
				
		if (points.length == 0)
			return;
		
		double minX = points[0].getX();
		double maxX = minX;
		double minY = points[0].getY();
		double maxY = minY;
				
		for (int i = 0; i < points.length; i++) {
			if (points[i].getX() < minX)
				minX = points[i].getX();
			else if (points[i].getX() > maxX)
				maxX = points[i].getX();
			
			if (points[i].getY() < minY)
				minY = points[i].getY();
			else if (points[i].getY() > maxY)
				maxY = points[i].getY();
		}
		if (drawPoints) {
			Graphics2D g = destination;
			
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.black);
		
			double scaleX = (width) / (maxX - minX);
			double scaleY = (height) / (maxY - minY);
			for (int i = 0; i < points.length; i++) {
				double dx = (points[i].getX() - minX) * scaleX;
				double dy = (points[i].getY() - minY) * scaleY;
				g.translate(dx, dy);
				g.drawLine(-3, -3, 3, 3);
				g.drawLine(-3, 3, 3, -3);
				g.drawString(""+GuiUtils.getNeptusDecimalFormat(1).format(vals[i]), 10, 10);
				g.translate(-dx,-dy);
			}
		}
	}
	
	public static void generateColorMap(Location[] locs, Double[] vals, Graphics2D destination, double width, double height, int alpha, ColorMap colorMap) {
		generateInterpolatedColorMap(locs, vals, destination, width, height, alpha, colorMap);
		Point2D[] points = new Point2D[locs.length];
		
		double minLong = -locs[0].getAbsoluteLongitudeDouble();
		double maxLat = locs[0].getAbsoluteLatitudeDouble();
		
		for (int i = 0; i < locs.length; i++) {
			if (-locs[i].getAbsoluteLongitudeDouble() < minLong)
				minLong = -locs[i].getAbsoluteLongitudeDouble();
			if (locs[i].getAbsoluteLatitudeDouble() > maxLat)
				maxLat = locs[i].getAbsoluteLatitudeDouble();			
		}

		Location topCorner = new Location(maxLat, minLong);
		
		double[] of = locs[0].getNEDOffsetFrom(topCorner);
		double minX = of[1];
		double minY = -of[0];
		double maxX = of[1];
		double maxY = -of[0];

		for (int i = 0; i < locs.length; i++) {
			double[] offsets = locs[i].getNEDOffsetFrom(topCorner);
			points[i] = new Point2D.Double(offsets[1], -offsets[0]);

			if (-offsets[0] > maxY)
				maxY = -offsets[0];
			if (offsets[1] > maxX)
				maxX = offsets[1];
			if (-offsets[0] < minY)
				minY = -offsets[0];
			if (offsets[1] < minX)
				minX = offsets[1];
		}
		
		Graphics2D g = destination;
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.black);
		
		double scaleX = width / (maxX - minX);
		double scaleY = height / (maxY - minY);
		for (int i = 0; i < points.length; i++) {
			double dx = (points[i].getX() - minX) * scaleX;
			double dy = (points[i].getY() - minY) * scaleY;
			g.translate(dx, dy);
			g.drawLine(-3, -3, 3, 3);
			g.drawLine(-3, 3, 3, -3);
			g.drawString(""+vals[i], 10, 10);
			g.translate(-dx,-dy);
		}
	}
	public static int HORIZONTAL_ORIENTATION = 1, VERTICAL_ORIENTATION = 2;
	
	public static Image getBar(ColorMap cmap, int orientation, int width, int height) {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = (Graphics2D) img.getGraphics(); 
		
		if (orientation == HORIZONTAL_ORIENTATION) {
			for (int i = 0; i < img.getWidth(); i++) {
				double pos = (double)i/(double)img.getWidth();
				g2d.setColor(cmap.getColor(pos));
				g2d.drawLine(i, 0, i, img.getHeight());
			}
		}
		
		if (orientation == VERTICAL_ORIENTATION) {
			for (int i = 0; i < img.getHeight(); i++) {
				//NeptusLog.pub().info("<###> "+colors.getGraphics().getColor());
				g2d.setColor(cmap.getColor((double)i/(double)img.getHeight()));
				g2d.drawLine(0, i, img.getWidth(), i);
			}
		}
		
		return img;
	}
	
    /**
     * @param val
     * @param minVar
     * @param maxVar
     * @return
     */
    public static double getColorIndexZeroToOne(double val, double minVar, double maxVar) {
        double ret = (val - minVar) / (maxVar - minVar);
        return ret;
    }

    /**
     * @param val
     * @param minVar
     * @param maxVar
     * @return
     */
    public static double getColorIndexZeroToOneLog10(double val, double minVar, double maxVar) {
        double valL = Math.log10(val);
        double minL = Math.log10(minVar);
        double maxL = Math.log10(maxVar);
        
        double ret = (valL - minL) / (maxL - minL);
        return ret;
    }

    public static void main(String[] args) {
        Location loc = new Location(41 , -8);
        
        Location loc0 = new Location(loc);
        Location loc1 = new Location(loc);
        Location loc2 = new Location(loc);
        Location loc3 = new Location(loc);
        Location loc4 = new Location(loc);
        Location loc5 = new Location(loc);
        
        loc1.translate(15, 8.5, 0);
        loc2.translate(5, 10, 0);
        loc3.translate(10, 7, 0);
        loc4.translate(3, 5, 0);
        loc5.translate(00, 12, 20);
        
        Location[] locs = new Location[] {loc0, loc1,loc2};

        Double[] values = new Double[] {0d, 600d, 340d};

        BufferedImage img = new BufferedImage(800,600,BufferedImage.TYPE_INT_ARGB);

        generateColorMap(locs, values, (Graphics2D) img.getGraphics(), (double) img.getWidth(),
                (double) img.getHeight(), 255,
                ColorMapFactory.createInvertedColorMap((InterpolationColorMap) ColorMapFactory
                        .createGrayScaleColorMap()));
        //generateColorMap(locs, values, (Graphics2D)img.getGraphics(), (double)img.getWidth(), (double)img.getHeight(),255, ColorMapFactory.createJetColorMap());

        JLabel lbl = new JLabel(new ImageIcon(img));        

        GuiUtils.testFrame(lbl);

        System.out.println("------------------------------------");

        System.out.println(String.format("->> getColorIndexZeroToOne >> 0.5 = %f", getColorIndexZeroToOne(5, 0, 10)));
        System.out.println(String.format("->> getColorIndexZeroToOne >> 0.75 = %f", getColorIndexZeroToOne(5, -10, 10)));
        System.out.println(String.format("->> getColorIndexZeroToOne >> >1 = %f", getColorIndexZeroToOne(11, -10, 10)));

        System.out.println(String.format("->> getColorIndexZeroToOneLog10 >> 0.899657 = %f", getColorIndexZeroToOneLog10(0.5, 0.001, 1)));
        System.out.println(String.format("->> getColorIndexZeroToOneLog10 >> 0.899657 = %f", getColorIndexZeroToOneLog10(0.5, 0.001, 1)));
        System.out.println(String.format("->> getColorIndexZeroToOneLog10 >> >1 = %f", getColorIndexZeroToOneLog10(1.001, 0.001, 1)));

    }
}
