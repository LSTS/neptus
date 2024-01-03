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
 * Author: Paulo Dias
 * 2008/08/10
 */
package pt.lsts.neptus.util.bathymetry;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 * FIXME Change this code to {@link ColorMapUtils}
 */
public class XYZDataUtil {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//41N09'35.293'' 08W41'35.721''
		FileInputStream fis = new FileInputStream("..\\Batimetria\\batimetria_leixoes.xyz");
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		Vector<Double> xvec = new Vector<Double>(), yvec = new Vector<Double>(), zvec = new Vector<Double>();
		String line = br.readLine();
		while (line != null) {
			if (line.startsWith("#")) {}				
			else {
				//NeptusLog.pub().info("<###> "+line);
				String[] xt = line.split("[\t ,]");
				if (xt.length == 3) {
					try {
						double xx = Double.parseDouble(xt[1]);
						double yy = Double.parseDouble(xt[0]);
						double zz = Double.parseDouble(xt[2]);
						xvec.add(xx);
						yvec.add(yy);
						zvec.add(zz);
					} catch (NumberFormatException e) {
					    e.printStackTrace();
					}
				}
			}			
			line = br.readLine();
		}
		br.close();
		NeptusLog.pub().info("<###>Number of points: " + xvec.size());
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		for (double xt : xvec) {
			if (xt < minX)
				minX = xt;
			if (xt > maxX)
				maxX = xt;
		}

		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (double yt : yvec) {
			if (yt < minY)
				minY = yt;
			if (yt > maxY)
				maxY = yt;
		}

		double minZ = Double.POSITIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;
		for (double zt : zvec) {
			if (zt < minZ)
				minZ = zt;
			if (zt > maxZ)
				maxZ = zt;
		}

		NeptusLog.pub().info("<###>x = ["+minX+","+maxX+"]"+"  \tdelta x = "+(Math.round(maxX-minX)));
		NeptusLog.pub().info("<###>y = ["+minY+","+maxY+"]"+"  \tdelta y = "+(Math.round(maxY-minY)));
		NeptusLog.pub().info("<###>z = ["+minZ+","+maxZ+"]"+"  \t\tdelta z = "+(Math.round(maxZ-minZ)));
		
		double[] dim = {maxX-minX, maxY-minY};
		double scale = 1;
		int defaultWidth = 1024;
        int defaultHeight = 768;
        int width = defaultWidth, height = defaultHeight;
		if (dim[1] < dim[0]) {
			height = defaultWidth;
			width = (int) (dim[1]*height/dim[0]);
			scale = dim[0] / height;
		}
		else {
			width = defaultHeight;
			height = (int) (dim[0]*width/dim[1]);
            scale = dim[1] / width;
		}
		new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		Point2D[] points = new Point2D[xvec.size()];
		
		NeptusLog.pub().info("<###>Scale: " + scale);
		
		LocationType baseLoc = new LocationType();
		baseLoc.setLatitudeStr("41N09'35.293''");//41º09'35.293"N
		baseLoc.setLongitudeStr("08W41'35.721''");
		baseLoc.translatePosition(-465778.48, -152987.42, 0);
		//baseLoc = (LocationType) baseLoc.convertToAbsoluteLatLonDepth();
		NeptusLog.pub().info("<###>Base location = " + baseLoc);
		
		LocationType topCorner = new LocationType(baseLoc);
		topCorner.translatePosition(maxX, minY, 0);
		NeptusLog.pub().info("<###>Top Left Corner location = " + topCorner);
//		NeptusLog.pub().info("<###> "+topCorner.getNewAbsoluteLatLonDepth().getLatitudeAsPrettyString() + "  " + topCorner.getNewAbsoluteLatLonDepth().getLongitudeAsPrettyString());

		LocationType centerLocation = new LocationType(baseLoc);
		centerLocation.translatePosition(maxX-((maxX-minX)/2), minY+((maxY-minY)/2), 0);
		NeptusLog.pub().info("<###>Center location = " + centerLocation);
//		NeptusLog.pub().info("<###> "+centerCorner.getNewAbsoluteLatLonDepth().getLatitudeAsPrettyString() + "  " + centerCorner.getNewAbsoluteLatLonDepth().getLongitudeAsPrettyString());

		
//		LocationType centerUsedCorner = new LocationType();
//		centerUsedCorner.setLatitude("41N11.057");
//		centerUsedCorner.setLongitude("8W42.251");
//		centerUsedCorner.setOffsetNorth(-823.643);
//		centerUsedCorner.setOffsetEast(73.975);
//		NeptusLog.pub().info("<###>CenterUsed location = " + centerUsedCorner);
//		NeptusLog.pub().info("<###> "+centerUsedCorner.getNewAbsoluteLatLonDepth().getLatitudeAsPrettyString() + "  " + centerUsedCorner.getNewAbsoluteLatLonDepth().getLongitudeAsPrettyString());

		LocationType bottomRightCorner = new LocationType(baseLoc);
		bottomRightCorner.translatePosition(maxX-(maxX-minX), minY+(maxY-minY), 0);
		NeptusLog.pub().info("<###>Bottom Right Corner location = " + bottomRightCorner);
//		NeptusLog.pub().info("<###> "+bottomRightCorner.getNewAbsoluteLatLonDepth().getLatitudeAsPrettyString() + "  " + bottomRightCorner.getNewAbsoluteLatLonDepth().getLongitudeAsPrettyString());

		for (int i = 0; i < xvec.size(); i++) {
			LocationType pLoc = new LocationType(baseLoc);
			baseLoc.translatePosition(xvec.get(i), yvec.get(i), 0);
			double[] offsets = pLoc.getOffsetFrom(topCorner);
			points[i] = new Point2D.Double(offsets[0], offsets[1]);
			points[i] = new Point2D.Double(xvec.get(i)-minX, yvec.get(i)-minY);
		}

		//Rectangle2D bounds = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		Rectangle2D bounds = new Rectangle2D.Double(0, 0, maxX-minX, maxY-minY);
		NeptusLog.pub().info("<###>BoundingBox: " + (maxX-minX) + " x " + (maxY-minY));

//		ColorMap colormap = ColorMapFactory.createJetColorMap();
//		double[] values = new double[zvec.size()];
//		for (int i = 0; i < zvec.size(); i++) {
//			values[i] = zvec.get(i);
//		}
//		generateInterpolatedColorMap(bounds, points,values,img,255,colormap,minZ, maxZ);
		
//		BufferedImage tmp = new BufferedImage((int)(maxX-minX)+1, (int)(maxY-minY)+1, BufferedImage.TYPE_INT_ARGB);
//		Graphics2D bg1 = img.createGraphics();
//		//bg.scale(1,-1);
//		bg1.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//
//		ColorMap colormap = ColorMapFactory.createJetColorMap();
//		
//		for (int i = 0; i < points.length; i++) {
//			Color c = colormap.getColor((zvec.get(i)-minZ)/(maxZ-minZ));
//			c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
//
//			//tmp.setRGB(i, j, c.getRGB());
//			try {
//				tmp.setRGB((int)(xvec.get(i)-minX), (int)((maxY-minY)-(yvec.get(i)-minY)), c.getRGB());
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		Graphics2D bg = img.createGraphics();
//		//bg.scale(1,-1);
//		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//		bg.scale((double)img.getWidth()/(double)tmp.getWidth(),
//				(double)img.getHeight()/(double)tmp.getHeight());
//
//		bg.drawImage(tmp, 0, 0, null);
//		//bg.scale(1,-1);
//		
//		JLabel lbl = new JLabel(new ImageIcon(img));		
//		
//		try {
//			ImageIO.write(img, "png", new File("teste.png"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		GuiUtils.testFrame(lbl, "", 800, 600);
		
		
		BufferedImage destination = new BufferedImage(width,
				height, BufferedImage.TYPE_INT_ARGB);

		BufferedImage destination1 = new BufferedImage(width,
				height, BufferedImage.TYPE_INT_ARGB);

		BufferedImage destination2 = new BufferedImage(width,
				height, BufferedImage.TYPE_INT_ARGB);

		int gridSize = 297;
		double[][] kdkd = discretizeQuadMatrix(gridSize, bounds, points, zvec.toArray(new Double[0]));
		//Double[] kdkd = discretizeQuadMatrix(100, bounds, points, zvec.toArray(new Double[0]));
		
		double minZ1 = Double.POSITIVE_INFINITY;
		double maxZ1 = Double.NEGATIVE_INFINITY;
		for (double[] zt1 : kdkd) {
			for (double zt : zt1) {
			if (Double.isNaN(zt))
				continue;
			if (zt < minZ1)
				minZ1 = zt;
			if (zt > maxZ1)
				maxZ1 = zt;
			}
		}
		for (int i = 0; i < kdkd.length; i++) {
			for (int j = 0; j < kdkd.length; j++) {
				if (!Double.isNaN(kdkd[i][j]))
					kdkd[i][j] = 1 - (kdkd[i][j] - minZ1)/(maxZ1-minZ1);
				else
					kdkd[i][j] = Double.NaN;
			}
		}
		
		double[][] kdkdinv = new double[kdkd.length][kdkd[0].length];
		for (int i = 0; i < kdkd.length; i++) {
			for (int j = 0; j < kdkd.length; j++) {
				if (!Double.isNaN(kdkd[i][j]))
					kdkdinv[i][j] = 1 - kdkd[i][j];
				else
					kdkdinv[i][j] = Double.NaN;
			}
		}
		
		
		getInterpolatedData(kdkd, ColorMapFactory.createJetColorMap(), destination, 255);
		JLabel lblw = new JLabel(new ImageIcon(destination));
		GuiUtils.testFrame(lblw, "", 800, 600);

		getInterpolatedData(kdkd, ColorMapFactory.createGrayScaleColorMap(), destination1, 255);
		JLabel lblw1 = new JLabel(new ImageIcon(destination1));
		GuiUtils.testFrame(lblw1, "", 800, 600);

		getInterpolatedData(kdkdinv, ColorMapFactory.createGrayScaleColorMap(), destination2, 255);
		JLabel lblw2 = new JLabel(new ImageIcon(destination2));
		GuiUtils.testFrame(lblw2, "", 800, 600);

		
//		int sizeG = (int)Math.floor(Math.sqrt(kdkd.length));
//		double[][] jjk = new double[sizeG][sizeG];
//		for (int i = 0; i < jjk.length; i++) {
//			for (int j = 0; j < jjk.length; j++) {
//				jjk[i][j] = (kdkd[i*jjk.length+j]!=null)?kdkd[i*jjk.length+j]:Double.NaN;
//			}
//		}
//		BufferedImage destination1 = new BufferedImage(width,
//				height, BufferedImage.TYPE_INT_ARGB);
//		getInterpolatedData(jjk, colormap, destination1, 255);
//		JLabel lblw1 = new JLabel(new ImageIcon(destination));
//		GuiUtils.testFrame(lblw1, "", 800, 600);
		
		
		try {
			ImageIO.write(destination , "png", new File("bathyLeixoes-jet.png"));
			ImageIO.write(destination1, "png", new File("bathyLeixoes-gray.png"));
			ImageIO.write(destination2, "png", new File("bathyLeixoes-inv-gray.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		MapType mapT = new MapType();
        mapT.setCenterLocation(centerLocation);
        CoordinateSystem cs = new CoordinateSystem();
        cs.setLocation(centerLocation);
        
        String imageNameId = "APDL-Bat";

        ImageElement imageElement = new ImageElement(MapGroup.getNewInstance(cs), mapT);
        imageElement.setId(imageNameId);
        imageElement.setCenterLocation(centerLocation);
        imageElement.setImageScale(scale);
        imageElement.setImage(destination);
        imageElement.setImageFileName("./bathyLeixoes-jet.png");
        imageElement.setOriginalFilePath(imageElement.getImageFileName());
        imageElement.setBathymetric(true);
        imageElement.setHeightImage(destination2);
        imageElement.setBathymetricImageFileName("./bathyLeixoes-inv-gray.png"); //No depth deve ser a inv-gray pois queremos valores mais altos sejam mais baixos
        imageElement.setMaxHeight(maxZ);
        imageElement.setMaxDepth(minZ);
        imageElement.setTransparency(100);

        mapT.setId("map-" + imageNameId);
        mapT.setName("map-" + imageNameId);
        mapT.setOriginalFilePath("./map-" + imageNameId + ".nmap");
        mapT.setHref("./map-" + imageNameId + ".nmap");

        imageElement.setParentMap(mapT);
        
        mapT.addObject(imageElement);
        mapT.saveFile("./map-" + imageNameId + ".nmap");
        
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(mapT.asXML()));
	}


	private static double[][] discretizeQuadMatrix(int gridSize, Rectangle2D bounds, Point2D[] points, Double[] values) {

		double dx = bounds.getWidth() / gridSize;
		double dy = bounds.getHeight() / gridSize;
		
		Double[] vals = new Double[gridSize * gridSize];
		int numPoints[] = new int[gridSize * gridSize];
				
		for (int i = 0; i < points.length; i++) {
			Point2D pt = points[i];
			
			int indX = (int) Math.floor((pt.getX() - bounds.getMinX()) / dx);
			int indY = (int) Math.floor((pt.getY() - bounds.getMinY()) / dy);
			int ind = indX * gridSize + indY;
			//System.err.println(ind);
			ind = Math.min(indX * gridSize + indY, gridSize*gridSize-1);
			
			if (vals[ind] == null)
				vals[ind] = values[i];
			else
				vals[ind] = (numPoints[ind] * vals[ind] + values[i]) / (numPoints[ind]+1);
			numPoints[ind]++;
		}
		
		int sizeG = (int)Math.floor(Math.sqrt(vals.length));
		double[][] ret = new double[sizeG][sizeG];
		for (int i = 0; i < ret.length; i++) {
			for (int j = 0; j < ret.length; j++) {
				ret[i][j] = (vals[i*ret.length+j]!=null)?vals[i*ret.length+j]:Double.NaN;
			}
		}

		return ret;
	}		

	
//	private static void generateInterpolatedColorMap(Rectangle2D bounds, Point2D[] points,
//			double[] values, BufferedImage destination, int alpha,
//			ColorMap colorMap, double minColorMapValue, double maxColorMapValue) {
//
//		if (points.length != values.length || points.length == 0) {
//			System.err.println("Number of points must be positive and match the number of values!");
//			return;			
//		}
//
//
//		double minX = bounds.getMinX();
//		double minY = bounds.getMinY();
//		double maxX = bounds.getMaxX();
//		double maxY = bounds.getMaxY();
//		
//		int numX = destination.getWidth() >= 100 ? 100 : destination.getWidth();
//		int numY = destination.getHeight() >= 100 ? 100 : destination.getHeight();
//		int numValues = numX * numY;
//
//		double[] xGrid = new double[numValues];
//		double[] yGrid = new double [numValues];
//		double[] zGrid = new double [numValues];
//
//		double dxGrid = (maxX - minX) / (numX);
//		double dyGrid = (maxY - minY) / (numY);			
//		
//		double x = 0.0;
//		for (int i = 0; i < numX; i++) {
//			if (i == 0) {
//				x = minX;
//			}
//			else {
//				x += dxGrid;
//			}
//			double y = 0.0;
//			for (int j = 0; j < numY; j++) {
//				int k = numY * i + j;
//				xGrid[k] = x;
//				if (j == 0) {
//					y = minY;
//				}
//				else {
//					y += dyGrid;
//				}
//				yGrid[k] = y;
//			}
//		}
//
//		double power = 5;
//
//		for (int kGrid = 0; kGrid < xGrid.length; kGrid++) {
//			double dTotal = 0.0;
//			zGrid[kGrid] = 0.0;
//			for (int k = 0; k < values.length; k++) {
//				double xPt = points[k].getX();
//				double yPt = points[k].getY();
//				double d = Point2D.distance(xPt, yPt, xGrid[kGrid], yGrid[kGrid]);
//				if (power != 1) {
//					d = Math.pow(d, power);
//				}
//				d = Math.sqrt(d);
//				if (d > 0.0) {
//					d = 1.0 / d;
//				}
//				else { // if d is real small set the inverse to a large number 
//					// to avoid INF
//					d = 1.e20;
//				}
//				zGrid[kGrid] += values[k] * d; 
//
//				dTotal += d;
//			}
//			zGrid[kGrid] = zGrid[kGrid] / dTotal;  //remove distance of the sum
//		}
//		
//		
//		double min = zGrid[0], max = zGrid[0];
//		for (int i = 0; i < zGrid.length; i++) {
//			if (zGrid[i] < min)
//				min = zGrid[i];
//			if (zGrid[i] > max)
//				max = zGrid[i];				
//		}
//		
//		double tmax = max, tmin = min;
//		if (maxColorMapValue != Double.NaN) {
//			tmax = maxColorMapValue;
//		}
//		if (minColorMapValue != Double.NaN) {
//			tmin = minColorMapValue;
//		}
//		if (tmax > tmin) {
//			max = tmax;
//			min = tmin;
//		}
//
//		for (int i = 0; i < zGrid.length; i++) {
//			//zGrid[i] = (zGrid[i] - min) / max;
//			zGrid[i] = (zGrid[i] - min) / (max-min);
//		//	NeptusLog.pub().info("<###> "+zGrid[i]);
//		}
//
//		//NeptusLog.pub().info("<###> "+min+" -> "+max+", "+dyGrid);
//		double vals[][] = new double[numX][numY];
//		
//		for (int i = 0; i < numX; i++) {
//			for (int j = 0; j < numY; j++) {
//				vals[i][j] = zGrid[i*numX+j];
//			}
//		}
//		
//		getInterpolatedData(vals, colorMap, destination, alpha);
//	}

	private static void getInterpolatedData(double[][] data, ColorMap colormap, BufferedImage destination, int alpha) {
		//BufferedImage tmp = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
		BufferedImage tmp = new BufferedImage(data[0].length, data.length, BufferedImage.TYPE_INT_ARGB);
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				if (Double.isNaN(data[i][j]))
					continue;
				Color c = colormap.getColor(data[i][j]);
				c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);

				//tmp.setRGB(i, j, c.getRGB());
				tmp.setRGB(j, data.length-1-i, c.getRGB());
			}
		}

		Graphics2D bg = destination.createGraphics();
		//bg.scale(1,-1);
		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		bg.scale((double)destination.getWidth()/(double)tmp.getWidth(),
				(double)destination.getHeight()/(double)tmp.getHeight());

		bg.drawImage(tmp, 0, 0, null);
		//bg.scale(1,-1);
		
	}

//
//	private static void getInterpolatedData1(Double[] data, ColorMap colormap, BufferedImage destination, int alpha) {
//		int length = (int) Math.sqrt(data.length);
//		//BufferedImage tmp = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_ARGB);
//		BufferedImage tmp = new BufferedImage(length, length, BufferedImage.TYPE_INT_ARGB);
//		for (int i = 0; i < length; i++) {
//			for (int j = 0; j < length; j++) {
//				if (data[i*length+j] == null)
//					continue;
//				Color c = colormap.getColor(data[i*length+j]);
//				c = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
//
//				//tmp.setRGB(i, j, c.getRGB());
//				tmp.setRGB(j, length-1-i, c.getRGB());
//			}
//		}
//
//		Graphics2D bg = destination.createGraphics();
//		//bg.scale(1,-1);
//		bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//		bg.scale((double)destination.getWidth()/(double)tmp.getWidth(),
//				(double)destination.getHeight()/(double)tmp.getHeight());
//
//		bg.drawImage(tmp, 0, 0, null);
//		//bg.scale(1,-1);
//		
//	}

}
