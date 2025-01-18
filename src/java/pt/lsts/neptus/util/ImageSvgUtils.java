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
 * 12/Jan/2025
 */
package pt.lsts.neptus.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pt.lsts.neptus.NeptusLog;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.StringReader;
import java.util.function.BiFunction;

public class ImageSvgUtils {
    private ImageSvgUtils() {
    }

    /**
     * Gets the SVG image from the path.
     * @param path the path to the SVG image
     * @return the SVG image
     */
    public static Document getSvgImage(String path) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = StreamUtil.copyStreamToString(FileUtil.getResourceAsStream(path));
            Document wDoc = f.createDocument(null, new StringReader((String) data));
            wDoc = SvgUtil.cleanInkscapeSVG(wDoc);
            return wDoc;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        catch (NoClassDefFoundError e) {
            System.out.print("Batik missing in the classpath. Proceding without worldmap.");
            return null;
        }
        catch (Error e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Gets the SVG image from the file.
     *
     * @param file the file to get the SVG image
     * @return the SVG image
     */
    public static Document getSvgImage(File file) {
        return getSvgImage(file.getAbsolutePath());
    }

    /**
     * Gets the SVG image as a buffered image.
     * @param path the path to the SVG image
     * @param graphicModifier a function to modify the graphics2D before painting the SVG image
     * @return the buffered image
     */
    public static BufferedImage getSvgImageAsBufferedImage(String path, BiFunction<Graphics2D, Document, Void> graphicModifier) {
        Document diagram = getSvgImage(path);
        if (diagram == null)
            return null;

        double width = SvgUtil.getWidth(diagram);
        double height = SvgUtil.getHeight(diagram);
        BufferedImage bufferedImage = ImageUtils.createCompatibleImage((int) Math.floor(width), (int) Math.floor(height),
                Transparency.TRANSLUCENT);
        try {
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (graphicModifier != null)
                graphicModifier.apply(graphics2D, diagram);
            paintSvgImageToBufferedImage(graphics2D, diagram);
            graphics2D.dispose();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        return bufferedImage;
    }

    /**
     * Paints the SVG image to the buffered image.
     * @param graphics2D the graphics2D to paint the SVG image
     * @param diagram the SVG diagram to paint
     */
    public static void paintSvgImageToBufferedImage(Graphics2D graphics2D, Document diagram) {
        if (diagram == null || graphics2D == null)
            return;
        try {
            double width = SvgUtil.getWidth(diagram);
            double height = SvgUtil.getHeight(diagram);
            PrintTranscoder prm = new PrintTranscoder();
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Double.valueOf(width).floatValue());
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, Double.valueOf(height).floatValue());
            TranscoderInput ti = new TranscoderInput(diagram);
            prm.transcode(ti, null);

            Paper paper = new Paper();
            paper.setSize(width, height);
            paper.setImageableArea(0, 0, width, height);
            PageFormat page = new PageFormat();
            page.setPaper(paper);

            prm.print(graphics2D, page, 0);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    /**
     * Paints the SVG image to the buffered image with the scale of the buffered image.
     *
     * @param bufferedImage the buffered image to paint the SVG image
     * @param scaleToBufferedImageSize if true the SVG image will be scaled to the buffered image size
     * @param graphicModifier a function to modify the graphics2D before painting the SVG image (non-cumulative).
     *                        The first parameter is the graphics2D and the second is the SVG element index, starts at 0.
     * @param diagram the SVG diagram(s) to paint
     */
    public static void paintSvgImageToBufferedImage(BufferedImage bufferedImage, boolean scaleToBufferedImageSize,
                                                         BiFunction<Graphics2D, Integer, Void> graphicModifier,
                                                         Document... diagram) {
        if (diagram == null || diagram.length == 0 || bufferedImage == null)
            return;

        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        for (int i = 0; i < diagram.length; i++) {
            Document d = diagram[i];
            double svgWidth = SvgUtil.getWidth(d);
            double svgHeight = SvgUtil.getHeight(d);
            Graphics2D gScaled = (Graphics2D) graphics2D.create();
            if (scaleToBufferedImageSize) {
                double maxWH = Math.max(svgWidth, svgHeight);
                gScaled.scale(width / maxWH, height / maxWH);
            }
            if (graphicModifier != null)
                graphicModifier.apply(gScaled, i);
            paintSvgImageToBufferedImage(gScaled, d);
            gScaled.dispose();
        }
        graphics2D.dispose();
    }

    /**
     * Paints the SVG image to the buffered image with the scale of the buffered image.
     * @param element the SVG element to paint
     * @param attribute the attribute to change
     * @param value the value to set
     * @param inlineOrStyleType if true the attribute is inline, otherwise is style
     */
    public static void fillSvgElementAttribute(Element element, String attribute, String value, boolean inlineOrStyleType) {
        int attribType = inlineOrStyleType ? 0 : 1; // FIXME: 0 is inline, 1 is style
        try {
            element.setAttribute(attribute, value);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }
}
