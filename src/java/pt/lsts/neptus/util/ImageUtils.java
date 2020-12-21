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
 * 2009/09/26
 */
package pt.lsts.neptus.util;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferInt;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.PixelGrabber;
import java.awt.image.RescaleOp;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.ImageIcon;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDescriptor;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.jhlabs.image.ContrastFilter;

import pt.lsts.neptus.NeptusLog;

/**
 * @author ZP
 * @author pdias
 * 
 */
public class ImageUtils {
    /*
     * Taken from: http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
     */
    public static Image getFasterScaledInstance(Image img, int targetWidth, int targetHeight) {
        return getFasterScaledInstance(toBufferedImage(img), targetWidth, targetHeight);
    }

    public static Image getFasterScaledInstance(BufferedImage img, int targetWidth, int targetHeight) {
        int iw = img.getWidth();
        int ih = img.getHeight();

        Object hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        while (iw > targetWidth * 2 || ih > targetHeight * 2) {
            iw = (iw > targetWidth * 2) ? iw / 2 : iw;
            ih = (ih > targetHeight * 2) ? ih / 2 : ih;
            img = scaleImage(img, type, hint, iw, ih);
        }

        if (iw > targetWidth) {
            int iw2 = iw / 2;
            BufferedImage img2 = scaleImage(img, type, hint, iw2, ih);
            if (iw2 < targetWidth) {
                img = scaleImage(img, type, hint, targetWidth, ih);
                img2 = scaleImage(img2, type, hint, targetWidth, ih);
                interp(img2, img, iw - targetWidth, targetWidth - iw2);
            }
            img = img2;
            iw = targetWidth;
        }

        if (ih > targetHeight) {
            int ih2 = ih / 2;
            BufferedImage img2 = scaleImage(img, type, hint, iw, ih2);
            if (ih2 < targetHeight) {
                img = scaleImage(img, type, hint, iw, targetHeight);
                img2 = scaleImage(img2, type, hint, iw, targetHeight);
                interp(img2, img, ih - targetHeight, targetHeight - ih2);
            }
            img = img2;
            ih = targetHeight;
        }

        if (iw < targetWidth && ih < targetHeight) {
            img = scaleImage(img, type, hint, targetWidth, targetHeight);
        }

        return img;
    }

    private static BufferedImage scaleImage(BufferedImage orig, int type, Object hint, int w, int h) {
        BufferedImage tmp = new BufferedImage(w, h, type);
        Graphics2D g2 = tmp.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
        g2.drawImage(orig, 0, 0, w, h, null);
        g2.dispose();
        return tmp;
    }

    private static void interp(BufferedImage img1, BufferedImage img2, int weight1, int weight2) {
        float alpha = weight1;
        alpha /= (weight1 + weight2);
        Graphics2D g2 = img1.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(img2, 0, 0, null);
        g2.dispose();
    }

    public static BufferedImage toBufferedImage(Image image) {

        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        // This code ensures that all the pixels in the image are loaded
        //image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels
        boolean hasAlpha = hasAlpha(image);

        // Create a buffered image with a format that's compatible with the
        // screen
        BufferedImage bimage = null;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;

            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();

            GraphicsConfiguration gc = gs.getDefaultConfiguration();

            bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
        }

        catch (HeadlessException e) {
            NeptusLog.waste().debug("[toBufferedImage] HeadlessException "+ e.getMessage());
        } // No screen

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;

            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }

            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);

        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);

        g.dispose();

        return bimage;
    }
    /**
     * Create an image which is compatible with the screen configuration (faster rendering)
     * @param width image width
     * @param height image height
     * @param transparency transparency as Transparency class constants
     * @return
     */
    public static BufferedImage createCompatibleImage(int width, int height, int transparency) {

        BufferedImage bimage = null;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        try {
            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();

            GraphicsConfiguration gc = gs.getDefaultConfiguration();

            bimage = gc.createCompatibleImage(width, height, transparency);
        }

        catch (HeadlessException e) {
            NeptusLog.waste().debug("[toBufferedImage] HeadlessException "+ e.getMessage());
        } // No screen

        if (bimage == null) {
            int type = BufferedImage.TYPE_INT_RGB;

            if (transparency != Transparency.OPAQUE) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(width, height, type);
        }
        return bimage;
    }
    
    public static boolean hasAlpha(Image image) {
        // If buffered image, the color model is readily available
        if (image instanceof BufferedImage) {
            return ((BufferedImage) image).getColorModel().hasAlpha();
        }

        // Use a pixel grabber to retrieve the image's color model;
        // grabbing a single pixel is usually sufficient
        PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);

        try {
            pg.grabPixels();
        }
        catch (InterruptedException e) {
        }

        // Get the image's color model
        return pg.getColorModel().hasAlpha();
    }

    /**
     * Class that provides graphical interface utilities
     * 
     * @author Ze Carlos
     * @author Paulo Dias
     * @author Sergio Fraga
     */
    private static class ImageLoader {

        Image image;
        ImageIcon imgIcon;

        public ImageLoader(String imageUrl) {

            if (imageUrl == null) {
                image = null;
                return;
            }

            try {
                imgIcon = new ImageIcon(this.getClass().getClassLoader().getResource(imageUrl));
                image = imgIcon.getImage();
            }
            catch (Exception e) {
                imgIcon = new ImageIcon(imageUrl);
                image = imgIcon.getImage();
            }
            if (image == null) {
                NeptusLog.waste().debug("[ImageLoader] Loading image " + imageUrl + " failed");
            }
            else {
                if (image.getWidth(null) <= 0 || image.getHeight(null) <= 0) {
                    String addInfo = "Image loaded with width and height wrong (" + image.getWidth(null) 
                    + ", " + image.getHeight(null) + ")";
                    NeptusLog.waste().error("[ImageLoader] Loading image " + imageUrl + " succeeded. " + addInfo);
                }
                NeptusLog.waste().debug("[ImageLoader] Loading image " + imageUrl + " succeeded");
            }
        }

        public Image getImage() {
            if (image == null)
                return null;

            if (image.getWidth(null) < 0)
                return null;
            else
                return image;
        }
        
        @SuppressWarnings("static-access")
        public Image getImageWaitLoad() {
            if (image == null)
                return null;
            while (imgIcon.getImageLoadStatus() == java.awt.MediaTracker.LOADING) {
                Thread.currentThread().yield();
            }
            if (image.getWidth(null) < 0)
                return null;
            else
                return image;
        }

    }

    /**
     * Loads and returns an <b>Image</b>
     * 
     * @param imageURL The URL from where the image is to be loaded
     * @return The loaded <b>Image</b> or <b>null</b> if the image does not exist
     */
    public static Image getImage(String imageURL) {
        ImageLoader loader = new ImageLoader(imageURL);
        if (loader.getImage() == null) {
            NeptusLog.pub().error("Image " + imageURL + " was not found!");
        }
        return loader.getImage();
    }

    public static Image getImageWaitLoad(String imageURL) {
        ImageLoader loader = new ImageLoader(imageURL);
        if (loader.getImage() == null) {
            NeptusLog.pub().error("Image " + imageURL + " was not found!");
        }
        return loader.getImageWaitLoad();
    }

    public static ImageIcon getIcon(String iconURL) {
        Image img = getImage(iconURL);
        if (img != null)
            return new ImageIcon(img);
        else
            return new ImageIcon(getImage("images/menus/no.png"));
    }

    public static Image getImage(URL url) {
        Image img = Toolkit.getDefaultToolkit().createImage(url);
        return img;
    }

    public static ImageIcon getScaledIcon(Image img, int maxWidth, int maxHeight) {
        img = ImageUtils.getScaledImage(img, maxWidth, maxHeight, false);
        try {
            return new ImageIcon(img);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return new ImageIcon(getImage("images/menus/no.png"));
        }
    }

    public static Image getScaledImage(String imagePath, int maxWidth, int maxHeight) {
        Image img = getImage(imagePath);
        return ImageUtils.getScaledImage(img, maxWidth, maxHeight, false);
    }

    /**
     * This method scales a given image according to the maximum value of width and height given
     * 
     * @param originalImage The image to be scaled
     * @param maxWidth The maximum allowed width for the image
     * @param maxHeight The maximum allowed height for the image
     * @param mayDistort Selects whether the image may be distorted (in case maxWidth/maxHeight != imgWidth/imgHeight)
     * @return
     */
    public static Image getScaledImage(Image originalImage, int maxWidth, int maxHeight, boolean mayDistort) {
        if (originalImage == null)
            return null;

        if (mayDistort) {
            return originalImage.getScaledInstance(maxWidth, maxHeight, Image.SCALE_SMOOTH);
        }
        else {

            if (originalImage.getWidth(null) < 0) {
                return originalImage.getScaledInstance(maxWidth, maxHeight, Image.SCALE_SMOOTH);
            }

            double imgRatio = (double) originalImage.getWidth(null) / (double) originalImage.getHeight(null);
            double desiredRatio = (double) maxWidth / (double) maxHeight;
            int width = maxWidth;
            int height = maxHeight;

            if (desiredRatio > imgRatio) {
                height = maxHeight;
                width = (int) (maxHeight * imgRatio);
            }
            else {
                width = maxWidth;
                height = (int) (maxWidth / imgRatio);
            }
            return originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        }
    }
    
    public static ImageIcon getScaledIcon(String imagePath, int maxWidth, int maxHeight) {
        Image img = getImage(imagePath);
        img = getScaledImage(img, maxWidth, maxHeight, false);
        return getScaledIcon(img, maxWidth, maxHeight);
    }
    
    public static ImageIcon getScaledIcon(ImageIcon icon, int maxWidth, int maxHeight) {
        Image img = icon.getImage();
        img = getScaledImage(img, maxWidth, maxHeight, false);
        return new ImageIcon(img);
    }
    
    // NEW 
    
    public static Image getFastScaledImage(Image originalImage, int maxWidth, int maxHeight, boolean mayDistort) {
        if (originalImage == null)
            return null;

        if (mayDistort) {
            return ImageUtils.getFasterScaledInstance(originalImage, maxWidth, maxHeight);
        }
        else {

            if (originalImage.getWidth(null) < 0) {
                return ImageUtils.getFasterScaledInstance(originalImage, maxWidth, maxHeight);
            }

            double imgRatio = (double) originalImage.getWidth(null) / (double) originalImage.getHeight(null);
            double desiredRatio = (double) maxWidth / (double) maxHeight;
            int width = maxWidth;
            int height = maxHeight;

            if (desiredRatio > imgRatio) {
                height = maxHeight;
                width = (int) (maxHeight * imgRatio);
            }
            else {
                width = maxWidth;
                height = (int) (maxWidth / imgRatio);
            }
            return ImageUtils.getFasterScaledInstance(originalImage, width, height);
        }
    }
    
    public static RescaleOp whiteBalanceOp(int r, int g, int b) {

        float rm = 255.0f / r;
        float gm = 255.0f / g;
        float bm = 255.0f / b;

        return new RescaleOp(
                new float[] { rm, gm, bm }, 
                new float[] { 0, 0, 0 }, null);
    }
    
    public static RescaleOp colorizeOp(int r, int g, int b, int alpha) {

        return new RescaleOp(
                new float[] { 1, 1, 1}, 
                new float[] { (alpha-r), alpha-g, alpha-b }, null);
    }

    public static ConvolveOp sharpenOp() {
        float[] sharpenMatrix = { 0.0f, -1.0f, 0.0f, -1.0f, 5.0f, -1.0f, 0.0f, -1.0f, 0.0f };
        return new ConvolveOp(new Kernel(3, 3, sharpenMatrix), ConvolveOp.EDGE_NO_OP, null);
    }
    
    public static ContrastFilter contrastOp() {
        ContrastFilter filter = new ContrastFilter();
        filter.setContrast(2.0f);
        filter.setBrightness(1.0f);
        return filter;
    }
    
    public static ContrastFilter identityOp() {
        ContrastFilter filter = new ContrastFilter();
        filter.setContrast(1.0f);
        filter.setBrightness(1.0f);
        return filter;
    }

    public static LookupOp invertOp() {
        byte[] invertArray = new byte[256];

        for (int counter = 0; counter < 256; counter++)
            invertArray[counter] = (byte) (255 - counter);

        return new LookupOp(new ByteLookupTable(0, invertArray), null);
    }

    public static RescaleOp brightenOp(float mult, int add) {
        return new RescaleOp(mult, add, null);
    }

    public static ColorConvertOp grayscaleOp() {
        return new ColorConvertOp(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
    }
    
    
    public static void copySrcIntoDstAt(final BufferedImage src,
            final BufferedImage dst, final int dx, final int dy) {
        int[] srcbuf = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int[] dstbuf = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        int width = src.getWidth();
        int height = src.getHeight();
        int dstoffs = dx + dy * dst.getWidth();
        int srcoffs = 0;
        for (int y = 0 ; y < height ; y++ , dstoffs+= dst.getWidth(), srcoffs += width ) {
            System.arraycopy(srcbuf, srcoffs , dstbuf, dstoffs, width);
        }
    }
    
    public static void copySrcIntoDst(final BufferedImage src, final BufferedImage dst, 
            final int sx, final int sy, final int sw, final int sh, 
            final int dx, final int dy, final int dw, final int dh) {
        
        int[] srcbuf = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int[] dstbuf = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        int width = sw;
        int height = sh;
        int dstoffs = dx + dy * dw;
        int srcoffs = sx + sy * sw;
        
        for (int y = 0 ; y < height ; y++ , dstoffs+= dst.getWidth(), srcoffs += width ) {
            System.arraycopy(srcbuf, srcoffs , dstbuf, dstoffs, width);
        }
    }
    /**
     * 
     * @param path
     * @return
     */
    public static ImageIcon createImageIcon(String path) {
        URL location = ImageUtils.class.getClassLoader().getResource(path);
        if (location != null) {
            return new ImageIcon(location);
        }
        else if(new File(path).exists()) {
            return new ImageIcon(path);
        }     
        else {
            NeptusLog.waste().debug("[ImageLoader] Loading image " + location + " failed");
            return null;
        }
    }
    
    private static final int EXIF_DATE_TIME = 0x0132;
    private static final int EXIF_SUBSEC_TIME = 37520;

    /**
     * This method extracts date from a given image file with EXIF information
     * @param jpegFile The file to be processed
     * @return The date this picture was taken
     * @throws Exception In case the picture cannot be read or does not provide required info
     */    
    public static Date getExifDate(File jpegFile) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
        String date = null, subsec = null;
        for (Directory d : metadata.getDirectories()) {
            if (d.containsTag(EXIF_DATE_TIME))
                date = d.getString(EXIF_DATE_TIME);
            if (d.containsTag(EXIF_SUBSEC_TIME)) {
                subsec = d.getString(EXIF_SUBSEC_TIME);
            }
        }

        if (date == null)
            throw new Exception("File does not contain required EXIF information");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        if (subsec != null) {
            date += "."+subsec.substring(0, 3);
            sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss.SSS");
        }
        
        return sdf.parse(date);
    }
    
    public static String readExifComment(File jpegFile) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);
        ExifSubIFDDescriptor descriptor = new ExifSubIFDDescriptor((ExifSubIFDDirectory) metadata.getDirectoriesOfType(ExifSubIFDDirectory.class));
        return descriptor.getUserCommentDescription();
    }
    
    public static ImageIcon createScaleImageIcon(String path, int width, int height ) {
        ImageIcon icon = ImageUtils.createImageIcon(path);
        if(icon != null){
            Image img = ImageUtils.getFastScaledImage(icon.getImage(), width, height, false);
            icon.setImage(img);
            return icon;
        }
        else{
            return null;
        }
    }
}
