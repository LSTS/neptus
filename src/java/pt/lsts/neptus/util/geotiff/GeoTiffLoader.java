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
package pt.lsts.neptus.util.geotiff;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.tiff.TiffContents;
import org.apache.commons.imaging.formats.tiff.TiffDirectory;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.TiffReader;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

public class GeoTiffLoader {

	private File tiffFile;
	private TiffDirectory tiffRoot;
	private TiffImageParser tiffImageParser = new TiffImageParser();
	private int width, height;
	private double xCorner;
	private double yCorner;
	private double xScale;
	private double yScale;
	private String crs;

	/**
	 * Create a geotiff loader object that loads the given TIF file
	 * @param geotiff The file to read
	 * @throws Exception In case the file cannot be read or is not a proper geotiff
	 */
	public GeoTiffLoader(File geotiff) throws Exception {
		this.tiffFile = geotiff;
		final TiffReader tiffReader = new TiffReader(true);
		TiffContents contents = tiffReader.readFirstDirectory(new ByteSourceFile(geotiff), new TreeMap<String, Object>(), true,
				new FormatCompliance(""));
		tiffRoot = contents.directories.get(0);
		this.width = tiffRoot.getSingleFieldValue(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH);
		this.height = tiffRoot.getSingleFieldValue(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH);

		double[] tiePoint = (double[])tiffRoot.getFieldValue(GeoTiffTagConstants.EXIF_TAG_MODEL_TIEPOINT_TAG);
		this.xCorner = tiePoint[3];
		this.yCorner = tiePoint[4];

		double[] pixScale = (double[])tiffRoot.getFieldValue(GeoTiffTagConstants.EXIF_TAG_MODEL_PIXEL_SCALE_TAG);
		this.xScale = pixScale[0];
		this.yScale = pixScale[1];

		short[] geokey = (short[])tiffRoot.getFieldValue(GeoTiffTagConstants.EXIF_TAG_GEO_KEY_DIRECTORY_TAG);

		this.crs = "EPSG:"+geokey[15];

		System.out.println(crs);
		System.out.println("("+xCorner+","+yCorner+") ("+xScale+","+yScale+")");
	}

	/**
	 * Retrive a partial section of the image
	 * @param x X corner in pixel coordinates
	 * @param y Y corner in pixel coordinates
	 * @param width Width in pixel coordinates
	 * @param height Height in pixel coordinates
	 * @return A buffered image with the selected image portion
	 * @throws Exception In case the given coordinates are not valid
	 */
	public BufferedImage getSubImage(int x, int y, int width, int height) throws Exception {
		LinkedHashMap<String, Object> subImgParams = new LinkedHashMap<String, Object>();
		subImgParams.put(TiffConstants.PARAM_KEY_SUBIMAGE_X, x);
		subImgParams.put(TiffConstants.PARAM_KEY_SUBIMAGE_Y, x);
		subImgParams.put(TiffConstants.PARAM_KEY_SUBIMAGE_WIDTH, width);
		subImgParams.put(TiffConstants.PARAM_KEY_SUBIMAGE_HEIGHT, height);
		return tiffImageParser.getBufferedImage(tiffFile, subImgParams);
	}

	/**
	 * Retrieve this geotiff as a buffered image
	 * @return A buffered image with all data loaded
	 * @throws Exception in case the data cannot be loaded from disk
	 */
	public BufferedImage getImage() throws Exception {
		return tiffImageParser.getBufferedImage(tiffFile, new LinkedHashMap<String, Object>());
	}

	/**
	 * @return the width of the image, in pixels
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height of the image, in pixels
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * @return the real world X coordinate of the bottom left corner
	 */
	public double getCornerX() {
		return xCorner;
	}

	/**
	 * @return the real world Y coordinate of the bottom left corner
	 */
	public double getCornerY() {
		return yCorner;
	}

	/**
	 * @return the real world pixel scale in the X dimension
	 */
	public double getScaleX() {
		return xScale;
	}

	/**
	 * @return the real world pixel scale in the Y dimension
	 */
	public double getScaleY() {
		return yScale;
	}

	/**
	 * @return the EPSG CRS read from the tiff meta data
	 */
	public String getCoordinateReferenceSystem() {
		return crs;
	}

	/**
	 * Convers pixel coordinates into real world coordinates
	 * @param x The X coordinate (real world coordinates, matching the TIF's CRS)
	 * @param y The Y coordinate (real world coordinates, matching the TIF's CRS)
	 * @return The resulting image pixel coordinates (possibly outside the available data)
	 */
	public double[] getWorldCoordinates(int x, int y) {
		double[] ret = new double[2];
		ret[0] = xCorner + x * xScale;
		ret[1] = yCorner + y * yScale;
		return ret;
	}

	/**
	 * Convert real world coordinates into image (pixel) coordinates
	 * @param x The real world x coordinate
	 * @param y The real world y coordinate
	 * @return The corresponding image (pixel) coordinates, possibly outside the image
	 */
	public int[] getPixelCoordinates(double x, double y) {
		System.out.println(y-yCorner);
		int[] ret = new int[2];
		ret[0] = (int)((x-xCorner)/xScale);
		ret[1] = (int)((yCorner-y)/yScale);
		return ret;
	}
}
