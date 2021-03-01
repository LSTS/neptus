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
package pt.lsts.neptus.util.llf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jfree.chart.JFreeChart;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.plugins.propertiesproviders.SidescanConfig;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.SidescanLogMarker;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.ScriptedPlot;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.SvgUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.chart.LLFChart;

/**
 * @author ZP
 * @author pdias logo work
 */
public class LsfReport {

    protected static org.w3c.dom.Document logoDoc = null;

    private static int page = 1;

    public static org.w3c.dom.Document getLogoDoc() {
        if (logoDoc == null) {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = FileUtil.getFileAsString(FileUtil.getResourceAsFile("/images/neptus_logo_ns.svg"));
            try {
                // logoDoc = f.createSVGDocument(null, new java.io.StringReader((String)data));
                logoDoc = f.createDocument(null, new StringReader((String) data));
                logoDoc = SvgUtil.cleanInkscapeSVG(logoDoc);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logoDoc;
    }

    private static void writeDetailsPage(PdfContentByte cb, IMraLogGroup source) {
        Rectangle pageSize = PageSize.A4.rotate();

        try {

            cb.beginText();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            cb.setFontAndSize(bf, 24);
            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Mission Statistics"), pageSize.getWidth() / 2,
                    pageSize.getHeight() - 100, 0);

            cb.setFontAndSize(bf, 14);
            cb.setColorFill(Color.gray.darker());

            float xpos = pageSize.getWidth() / 4;
            float ypos = pageSize.getHeight() - 160;

            LinkedHashMap<String, String> stats = LogUtils.generateStatistics(source);

            for (String field : stats.keySet()) {
                cb.setColorFill(Color.blue.darker());
                cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, field + ":", xpos - 10, ypos, 0);
                cb.setColorFill(Color.black);
                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, stats.get(field), xpos + 10, ypos, 0);

                ypos -= 20;
            }
            cb.endText();

            VehicleType vehicle = LogUtils.getVehicle(source);

            if (vehicle != null) {
                Image vehicleImage = vehicle.getPresentationImageHref().equalsIgnoreCase("") ? ImageUtils
                        .getScaledImage(vehicle.getSideImageHref(), 300, 300) : ImageUtils.getScaledImage(
                                vehicle.getPresentationImageHref(), 300, 300);

                        PdfTemplate tp = cb.createTemplate(300, 300);

                        java.awt.Graphics2D g2 = tp.createGraphicsShapes(300, 300);
                        g2.drawImage(vehicleImage, 0, 0, null);
                        g2.dispose();

                        cb.addTemplate(tp, pageSize.getWidth() - 350, pageSize.getHeight() - 460);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void writeFirstPage(PdfContentByte cb, IMraLogGroup source) {
        Rectangle pageSize = PageSize.A4.rotate();

        try {

            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            if (getLogoDoc() != null) {
                PrintTranscoder prm = new PrintTranscoder();
                prm.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Float.valueOf(500));
                prm.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, Float.valueOf(193));
                TranscoderInput ti = new TranscoderInput(getLogoDoc());
                prm.transcode(ti, null);
                PdfTemplate tp = cb.createTemplate(500, 200);
                java.awt.Graphics2D g2;
                g2 = tp.createGraphicsShapes(500, 200);
                Paper paper = new Paper();
                paper.setSize(pageSize.getWidth(), pageSize.getHeight());
                paper.setImageableArea(0, 0, 500, 193);
                PageFormat page = new PageFormat();
                page.setPaper(paper);
                prm.print(g2, page, 0);
                g2.dispose();
                cb.addTemplate(tp, pageSize.getWidth() / 2 - 250, pageSize.getHeight() / 2 - 91 + 120);
            }

            cb.beginText();

            cb.setFontAndSize(bf, 18);

            Date d = new Date(source.getLog("EstimatedState").currentTimeMillis());

            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, source.name() + "  (" + LogUtils.getVehicle(source) + ")",
                    pageSize.getWidth() / 2, pageSize.getHeight() / 2 - 40, 0);

            cb.setColorFill(new Color(200, 200, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.textf("Mission executed on %date", d.toString()),
                    pageSize.getWidth() / 2, pageSize.getHeight() / 2 - 140, 0);
            cb.setFontAndSize(bf, 32);

            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Neptus Mission Report"),
                    pageSize.getWidth() / 2, pageSize.getHeight() / 2, 0);

            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writePageNumber(PdfContentByte cb, int curPage) {

        if (LsfReportProperties.printPageNumbers==false)
            return;

        Rectangle pageSize = PageSize.A4.rotate();

        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.beginText();
            cb.setFontAndSize(bf, 12);

            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, Integer.toString(curPage), pageSize.getWidth() - 10, 575, 0);
            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void writeHeader(PdfContentByte cb, IMraLogGroup source) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.beginText();
            cb.setFontAndSize(bf, 12);

            // cb.moveTo(200,200);
            cb.moveText(10, 575);

            cb.showText(I18n.text("Neptus Mission Report") + " - " + source.name());
            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeFooter(PdfContentByte cb, IMraLogGroup source) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.setColorFill(Color.gray);
            cb.beginText();
            cb.setFontAndSize(bf, 12);
            Rectangle pageSize = PageSize.A4.rotate();

            // cb.moveTo(200,200);
            cb.moveText(10, 10);
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.textf("Document generated by %generator on %date.",
                    "Neptus " + ConfigFetch.getNeptusVersion(), new Date().toString()), pageSize.getWidth() / 2, 10, 0);

            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean savePdf(IMraLogGroup source, LLFChart llfChart, File destination) {
        Rectangle pageSize = PageSize.A4.rotate();
        try {
            FileOutputStream out = new FileOutputStream(destination);

            Document doc = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            doc.addTitle(llfChart.getName());
            doc.addCreationDate();
            doc.addCreator("Neptus " + ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));

            PdfContentByte cb = writer.getDirectContent();
            java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            JFreeChart chart = llfChart.getChart(source, MRAProperties.defaultTimestep);
            chart.setTitle("");
            chart.setBackgroundPaint(Color.white);
            chart.draw(g2, new Rectangle2D.Double(25, 25, width - 50, height - 50));

            g2.dispose();
            doc.close();
            out.flush();
            out.close();
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean generateReport(IMraLogGroup source, File destination, MRAPanel panel) {

        LogTree tree = panel.getLogTree();
        Vector<LLFChart> llfCharts = new Vector<>();
        for (int i = 0; i < tree.getChartsNode().getChildCount(); i++) {
            try {
                LLFChart chart = (LLFChart) ((DefaultMutableTreeNode) tree.getChartsNode().getChildAt(i)).getUserObject();
                llfCharts.add(chart);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }

        Rectangle pageSize = PageSize.A4.rotate();
        try {
            FileOutputStream out = new FileOutputStream(destination);

            Document doc = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            doc.addTitle(I18n.text("Neptus Mission Report") + " - " + source.name());
            doc.addCreationDate();
            doc.addCreator("Neptus " + ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));

            PdfContentByte cb = writer.getDirectContent();
            page = 1;
            writeFirstPage(cb, source);
            page++;

            doc.newPage();

            for (LLFChart llfChart : llfCharts) {
                if (!(llfChart instanceof ScriptedPlot)) {
                    java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
                    int width = (int) pageSize.getWidth();
                    int height = (int) pageSize.getHeight();
                    
                    JFreeChart chart = llfChart.getChart(source, llfChart.getDefaultTimeStep());
                    chart.setBackgroundPaint(Color.white);
                    chart.draw(g2, new Rectangle2D.Double(25, 25, width - 50, height - 50));
                    
                    g2.dispose();
                    writePageNumber(cb, page++);
                    writeHeader(cb, source);
                    writeFooter(cb, source);
                    doc.newPage();
                }
                else {
                    NeptusLog.pub().warn("ScriptedPlot skiped!");
                }
            }

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width - 100, height - 100);

            final Graphics2D g2 = tp.createGraphicsShapes(width - 100, height - 100);

            MissionType mt = LogUtils.generateMission(source);
            PlanType plan = LogUtils.generatePlan(mt, source);
            PathElement path = LogUtils.generatePath(mt, source);
            final StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mt));
            r2d.setShowWorldMapOnScreenControls(false);
            PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
            po.setTransp2d(0.5);
            po.setPlan(plan);
            po.setRenderer(r2d);
            po.setColor(new Color(255, 255, 255, 128));
            po.setShowDistances(false);
            po.setShowManNames(false);
            r2d.addPostRenderPainter(po, "Plan Painter");

            r2d.setSize(width - 100, height - 100);
            r2d.focusLocation(path.getCenterPoint());
            
            r2d.setLevelOfDetail(16); // FIXME
            
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    r2d.update(g2);
                }
            });
            g2.dispose();
            cb.addTemplate(tp, 50, 50);
            writePageNumber(cb, page++);
            writeHeader(cb, source);
            writeFooter(cb, source);

            doc.newPage();
            createTable(cb, doc, source, panel);// table with Marks
            writeHeader(cb, source);
            writeFooter(cb, source);

            doc.newPage();
            writeDetailsPage(cb, source);
            writePageNumber(cb, page++);
            writeHeader(cb, source);
            writeFooter(cb, source);

            doc.close();
            out.flush();
            out.close();

        }
        catch (Exception e) {
            e.printStackTrace();
            GuiUtils.errorMessage(I18n.text("Error generating report"), e.getMessage());
            return false;
        }
        return true;
    }

    public static void createTable(PdfContentByte cb, Document doc, IMraLogGroup source, MRAPanel panel)
            throws DocumentException {

        try {
            SidescanParser ssParser = SidescanParserFactory.build(source);
            if (ssParser == null) {
                NeptusLog.pub().warn("No sidescan to use for PDF");
                return;
            }
            
            int nSubsys = ssParser.getSubsystemList().size();
            SidescanConfig config = new SidescanConfig();
            int colorMapCode = LsfReportProperties.sidescanColorMap;
            boolean globalColorMap = true;
            if (colorMapCode == -1) {
                globalColorMap = false;
            }
            else {
                config.colorMap = getColorMapFromCode(colorMapCode);
            }
            SidescanParameters sidescanParams = new SidescanParameters(0, 0);
            sidescanParams.setNormalization(config.normalization);
            sidescanParams.setTvgGain(config.tvgGain);

            PdfPTable table = new PdfPTable(3 + nSubsys);

            //            Rectangle pageSize = PageSize.A4.rotate();

            setRowsWidth(table,nSubsys);

            writeHeader(table, ssParser.getSubsystemList());

            ArrayList<LogMarker> markers = panel.getMarkers();
            for (LogMarker m : markers) {
                createPdfMarksRows(table, m);
                if (m.getClass() == SidescanLogMarker.class) {// sidescanImages
                    createPdfSidescanMarks(table, m, nSubsys, ssParser, config, source, sidescanParams, globalColorMap);
                }
                else {// not in sidescan
                    for (int i = 0; i < nSubsys; i++) {
                        table.addCell("");
                    }
                }
            }

            actualWriteToPdf(source, cb, table, doc);


        }
        catch (Exception e) {
            e.printStackTrace();
            GuiUtils.errorMessage(I18n.text("Error creating PDF table for sidescan"), e.getMessage());
        }
    }

    public static void actualWriteToPdf(IMraLogGroup source, PdfContentByte cb, PdfPTable table, Document doc){
        Rectangle pageSize = PageSize.A4.rotate();
        cb.beginText();
        writePageNumber(cb, page++);
        writeHeader(cb, source);
        writeFooter(cb, source);
        try{
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.setFontAndSize(bf, 24);
            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Marks' Table"), pageSize.getWidth() / 2,
                    pageSize.getHeight() - 100, 0);

            cb.endText();

            float xpos = pageSize.getWidth() / 18;
            float ypos = pageSize.getHeight() - 175;

            // headers:
            cb.setColorFill(new Color(50, 100, 200));
            ypos = table.writeSelectedRows(0, 1, xpos, ypos, cb);

            // data:
            for (int i = 1; i < table.getRows().size(); i++) {
                if (ypos - (table.getRow(i).getMaxHeights()) < 75) {// check ypos within page range
                    doc.newPage();
                    cb.beginText();
                    writePageNumber(cb, page++);
                    writeHeader(cb, source);
                    writeFooter(cb, source);
                    cb.setFontAndSize(bf, 24);
                    cb.setColorFill(new Color(50, 100, 200));
                    cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Marks' Table"), pageSize.getWidth() / 2,
                            pageSize.getHeight() - 100, 0);

                    cb.endText();
                    ypos = pageSize.getHeight() - 160;
                    cb.setColorFill(new Color(50, 100, 200));

                    ypos = table.writeSelectedRows(0, 1, xpos, ypos, cb);
                }
                if (i % 2 == 0)
                    cb.setColorFill(Color.gray.darker());
                else
                    cb.setColorFill(Color.black);
                PdfPCell[] cells = table.getRow(i).getCells();
                for (PdfPCell cell : cells) {
                    if (cell.getHeight() > 250) {
                        table.getRow(i).setMaxHeights(250);
                        break;
                    }
                }
                ypos = table.writeSelectedRows(i, i + 1, xpos, ypos, cb);
            }
        }catch(Exception e){
            NeptusLog.pub().error(e.getMessage());
        }
    }

    public static void createPdfSidescanMarks(PdfPTable table, LogMarker m, int nSubsys, SidescanParser ssParser,
            SidescanConfig config, IMraLogGroup source, SidescanParameters sidescanParams,
            boolean globalColorMap){
        SidescanLogMarker sd = (SidescanLogMarker) m;
        sd.setDefaults(ssParser.getSubsystemList().get(0));//setDefaults if they are N/A
        // table.addCell("w="+sd.w+" | h="+sd.h);
        for (int i = 0; i < nSubsys; i++) {
            com.lowagie.text.Image iTextImage=null;// iText image type
            BufferedImage image = null;
            try {
                image = getSidescanMarkImage(source, ssParser, sidescanParams, config, globalColorMap, sd, i);
            }catch(Exception e){
                NeptusLog.pub().error(e.getMessage());
            }

            if (image != null) {

                // debug of image
                //                 String path = "C://";
                //                 try {
                //                    ImageIO.write(image, "PNG", new File(path, sd.getLabel() + ".png"));
                //                }
                //                catch (IOException e1) {
                //                    e1.printStackTrace();
                //                }

                try {
                    ImageIO.write(image, "png", new File("tmp.png"));
                    iTextImage = com.lowagie.text.Image.getInstance("tmp.png");
                    File file = new File("tmp.png");
                    Boolean deleted = file.delete();
                    if (!deleted)
                        throw new DocumentException("file.delete() failed");
                }catch(Exception e){
                    NeptusLog.pub().error(e.getMessage());
                }
                PdfPCell cell = new PdfPCell(iTextImage, true);
                table.addCell(cell);
            }
            else {// no image to display
                table.addCell("");
            }
        }
    }

    public static void createPdfMarksRows(PdfPTable table, LogMarker m){
        table.addCell(DateTimeUtil.formatTime((long)m.getTimestamp()));
        table.addCell(m.getLabel());
        String lat = CoordinateUtil.latitudeAsPrettyString(Math.toDegrees(m.getLatRads()));
        String lon = CoordinateUtil.longitudeAsPrettyString(Math.toDegrees(m.getLonRads()));
        table.addCell(lat + " " + lon);
    }

    public static void setRowsWidth(PdfPTable table, int nSubsys){

        Rectangle pageSize = PageSize.A4.rotate();
        float tableWidth = pageSize.getWidth() * 5 / 6;
        table.setTotalWidth(tableWidth);
        float[] columnWidth = new float[3 + nSubsys];
        if (nSubsys == 0) {
            columnWidth[0] = 0.30f;
            columnWidth[1] = 0.40f;
            columnWidth[2] = 0.30f;
        }
        if (nSubsys > 0) {
            columnWidth[0] = 0.15f;
            columnWidth[1] = 0.25f;
            columnWidth[2] = 0.15f;
            for (int i = 3; i < 3 + nSubsys; i++) {
                columnWidth[i] = 0.45f / nSubsys;
            }
        }
        try{
            table.setWidths(columnWidth);
        }catch(Exception e){
            NeptusLog.pub().error(e.getMessage());
        }

    }

    public static void writeHeader(PdfPTable table, ArrayList<Integer> subSysList){
        // header
        table.addCell(I18n.text("Timestamp"));
        table.addCell(I18n.text("Label"));
        table.addCell(I18n.text("Location"));
        int nSubsys = subSysList.size();
        for (int i = 0; i < nSubsys; i++)
            table.addCell(I18n.textf("Image %number", subSysList.get(i)));
    }

    /**
     *
     * @param subSysN = 0/1 index in list, subSys = ssParser.getSubsystemList().get(subSysN)
     */
    public static BufferedImage getSidescanMarkImage(IMraLogGroup source, SidescanParser ssParser,
            SidescanParameters sidescanParams, SidescanConfig config, boolean globalColorMap, SidescanLogMarker mark,
            int subSysN) throws DocumentException {
        BufferedImage result = null;

        SidescanLogMarker adjustedMark = adjustMark(mark);
        int subSys = ssParser.getSubsystemList().get(subSysN);
        double wMeters = adjustedMark.getwMeters();
        boolean point = adjustedMark.isPoint();

        // get the lines
        ArrayList<SidescanLine> list = null;
        list = getLines(ssParser, subSys, sidescanParams, adjustedMark);

        if (list.isEmpty())
            throw new DocumentException("list of lines empty");

        list = adjustLines(list,adjustedMark);

        float range = list.get(list.size() / 2).getRange();
        if (wMeters == -1)
            wMeters = (list.get(list.size() / 2).getRange() / 5);

        double x, x1, x2;
        x = adjustedMark.getX();
        x += range;
        x1 = (x - (wMeters / 2));
        x2 = (x + (wMeters / 2));

        // check limits & double frequency problems
        if (x > 2 * range || x < 0)// image outside of available range
            return null;
        if (x1 < 0) {
            x1 = 0;
        }
        if (x2 > 2 * range) {
            x2 = 2 * range;
        }

        if (x1 > x2)
            throw new DocumentException("x1>x2");

        int size = list.get(list.size() / 2).getData().length;
        int i1 = convertMtoIndex(x1, range, size);
        int i2 = convertMtoIndex(x2, range, size);

        if (i2 > size) {
            i2 = size;
        }
        if (i1 < 0) {
            i1 = 0;
        }

        if (globalColorMap == false) {
            config.colorMap = ColorMapFactory.getColorMapByName(adjustedMark.getColorMap());
        }

        if (point) {
            Color color = getColor(adjustedMark,ssParser,sidescanParams,config);
            result = createImgLineList(list, i1, i2, config, adjustedMark);
            result = paintPointHighlight(result, (result.getWidth()/2), (result.getHeight()/2), color, config.colorMap);
        }
        else {
            result = createImgLineList(list, i1, i2, config, adjustedMark);
        }

        return result;
    }

    public static Color getColor(SidescanLogMarker mark, SidescanParser ssParser, SidescanParameters sidescanParams,
            SidescanConfig config){

        Color color = null;
        int d = 1;
        long t = (long) mark.getTimestamp();

        ArrayList<SidescanLine> list2 = ssParser.getLinesBetween(t - d, t + d, mark.getSubSys(), sidescanParams);
        while (list2.isEmpty()) {
            d += 10;
            list2 = ssParser.getLinesBetween(t - d, t + d, mark.getSubSys(), sidescanParams);
        }
        SidescanLine l = list2.get(list2.size() / 2);
        int index = convertMtoIndex(mark.getX() + l.getRange(), l.getRange(), l.getData().length);

        color = config.colorMap.getColor(l.getData()[index]);
        return color;
    }

    public static int getIndexX(SidescanLogMarker mark, SidescanParser ssParser, SidescanParameters sidescanParams,
            boolean border, int i1, int i2){
        int indexX=-1;
        int d = 1;
        long t = (long) mark.getTimestamp();
        ArrayList<SidescanLine> list2 = ssParser.getLinesBetween(t - d, t + d, mark.getSubSys(), sidescanParams);
        while (list2.isEmpty()) {
            d += 10;
            list2 = ssParser.getLinesBetween(t - d, t + d, mark.getSubSys(), sidescanParams);
        }
        SidescanLine l = list2.get(list2.size() / 2);
        int index = convertMtoIndex(mark.getX() + l.getRange(), l.getRange(), l.getData().length);
        if (border == true) {
            if (index > (i2 - i1)) {
                index = index - i1;
            }
            indexX = (int) (((double) ((double) index / (double) (i2 - i1))) * 100);
        }
        else {
            indexX = 50;
        }
        return indexX;
    }

    public static int getIndexY(ArrayList<SidescanLine> list, SidescanLogMarker mark, int subSys){
        if (mark.getSubSys() !=subSys)
            return 50;
        double t = mark.getTimestamp();
        for (int i = 0; i < list.size(); i++) {
            SidescanLine l = list.get(i);
            if (l.getTimestampMillis() == t)
                return i;
        }
        return -1;
    }

    public static BufferedImage drawImage(ArrayList<BufferedImage> imgLineList, SidescanLogMarker mark){

        int w = mark.getW();
        int h = mark.getH();

        BufferedImage result;
        BufferedImage imgScalled = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgScalled.createGraphics();
        int y = imgLineList.size();
        for (BufferedImage imgLine : imgLineList) {
            if (y < 0)
                return null;
            g2d.drawImage(ImageUtils.getScaledImage(imgLine, imgScalled.getWidth(), imgLine.getHeight(), true), 0, y,
                    null);
            y--;
        }

        result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        if (imgScalled.getWidth() != result.getWidth() || imgScalled.getHeight() != result.getHeight()) {
            g2d = result.createGraphics();
            g2d.drawImage(ImageUtils.getScaledImage(imgScalled, result.getWidth(), result.getHeight(), true), 0, 0,
                    null);
        }
        else {
            result = imgScalled;
        }

        return result;
    }

    public static BufferedImage createImgLineList(ArrayList<SidescanLine> list, int i1, int i2, SidescanConfig config, SidescanLogMarker mark){

        int w = mark.getW();
        int h = mark.getH();

        BufferedImage imgScalled = new BufferedImage(w*3, h*3, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = imgScalled.createGraphics();

        int y = list.size();

        for (SidescanLine l : list ) {

            BufferedImage imgLine = new BufferedImage(i2 - i1, 1, BufferedImage.TYPE_INT_RGB);
            for (int c = 0; c < i2 - i1; c++) {
                int rgb = config.colorMap.getColor(l.getData()[c + i1]).getRGB();
                imgLine.setRGB(c, 0, rgb);
            }
            int vZoomScale = 3;
            Image full = ImageUtils.getScaledImage(imgLine, imgScalled.getWidth(), vZoomScale, true);
            g2d.drawImage(full, 0, imgScalled.getHeight() + h - y, null);

            y = y + vZoomScale;

        }
        return imgScalled;
    }

    public static ArrayList<SidescanLine> adjustLines(ArrayList<SidescanLine> list, SidescanLogMarker mark){

        int h = mark.getH();
        long t = (long) mark.getTimestamp();

        int yref = list.size();
        while (yref > h) {
            long tFirst = list.get(0).getTimestampMillis();
            long tLast = list.get(list.size() - 1).getTimestampMillis();
            if (tFirst == t) {
                list.remove(list.size() - 1);
                yref--;
                continue;
            }
            if (tLast == t) {
                list.remove(0);
                yref--;
                continue;
            }
            if (Math.abs(t - tFirst) < Math.abs(t - tLast)) {
                list.remove(list.size() - 1);
                yref--;
                continue;
            }
            if (Math.abs(t - tFirst) >= Math.abs(t - tLast)) {
                list.remove(0);
                yref--;
                continue;
            }

        }
        return list;

    }

    public static ArrayList<SidescanLine> getLines(SidescanParser ssParser, int subSys, SidescanParameters sidescanParams, SidescanLogMarker mark){

        long t = (long) mark.getTimestamp();
        int h = mark.getH();
        ArrayList<SidescanLine> list = new ArrayList<SidescanLine>();
        long firstTimestamp = ssParser.firstPingTimestamp();
        long lastTimestamp = ssParser.lastPingTimestamp();
        long t1, t2;
        int deviation = 0;

        int counter=0;
        while (list.size() < h && counter<10) {// get enough lines
            counter++;//infinte cicle protection
            deviation += 250;
            t1 = t - deviation * (h / 2);
            t2 = t + deviation * (h / 2);
            if (t1 < firstTimestamp) {
                t1 = firstTimestamp;
            }
            if (t2 > lastTimestamp) {
                t2 = lastTimestamp;
            }
            list = ssParser.getLinesBetween(t1, t2, subSys, sidescanParams);
        }

        return list;
    }

    public static SidescanLogMarker adjustMark(SidescanLogMarker mark){
        SidescanLogMarker newMark = new SidescanLogMarker(mark.getLabel(),mark.getTimestamp(),mark.getLatRads(),mark.getLonRads(),
                mark.getX(), mark.getY(), mark.getW(), mark.getH(), mark.getwMeters(), mark.getSubSys(),ColorMapFactory.getColorMapByName(mark.getColorMap()));
        newMark.setPoint(mark.isPoint());
        int h = newMark.getH();
        int w = newMark.getW();
        double wMeters = newMark.getwMeters();

        if (w == 0 && h == 0) {
            w = 100;
            h = 100;
            wMeters = -1;
        }

        // adjustments:
        if (w < 100 || h < 100 || wMeters < 0.05) {
            if (w < 100) {
                w = 100;
                wMeters = -1;// wMeters defined with range
            }
            if (h < 100)
                h = 100;
        }
        else if (w < 150 || h < 150) {
            if (w < 150) {
                w *= 1.2;
                wMeters *= 1.2;
            }
            if (h < 150)
                h *= 1.2;
        }
        else if (w < 200 || h < 200) {
            if (w < 200) {
                w *= 1.1;
                wMeters *= 1.1;
            }
            if (h < 200)
                h *= 1.1;
        }

        newMark.setH(h);
        newMark.setW(w);
        newMark.setwMeters(wMeters);

        return newMark;
    }

    public static BufferedImage paintPointHighlight(BufferedImage original, int x, int y, Color color, ColorMap colorMap) {
        BufferedImage result = original;
        Graphics2D g2d = result.createGraphics();

        int w = 10;
        int h = 10;
        x -= w / 2;
        y -= h / 2;
        Color c = null;
        boolean fixedColor = true;
        if (LsfReportProperties.sidescanMarksPointsFixedColor == 0) {
            fixedColor = false;
        }
        if (fixedColor == true) {
            c = getFixedColor(colorMap);
        }
        else {
            c = getContrastColor(color);
        }
        g2d.setColor(c);
        int shape = LsfReportProperties.sidescanMarksPointsShape;
        switch (shape) {
            case 0:
                g2d.drawRect(x, y, w, h);
                if (fixedColor == false) {
                    g2d.setColor(color);
                    g2d.drawRect(x - 1, y - 1, w + 2, h + 2);
                    // g2d.drawRect(x + 1, y + 1, w - 2, h - 2);
                }
                break;
            case 1:
                g2d.drawOval(x, y, w, h);
                if (fixedColor == false) {
                    g2d.setColor(color);
                    g2d.drawOval(x - 1, y - 1, w + 2, h + 2);
                    // g2d.drawOval(x + 1, y + 1, w - 2, h - 2);
                }
                break;
            default:
                NeptusLog.pub().info("Sidescan Point Marks Shape Code not found, using 0 square instead");
                g2d.drawRect(x, y, w, h);
                if (fixedColor == false) {
                    g2d.setColor(color);
                    g2d.drawRect(x - 1, y - 1, w + 2, h + 2);
                    // g2d.drawRect(x + 1, y + 1, w - 2, h - 2);
                }
                break;
        }

        return result;
    }

    public static Color getFixedColor(ColorMap colorMap) {
        String colorMapString = colorMap.toString().toLowerCase();
        Color c = null;
        switch (colorMapString) {
            case "redyellowgreen":
            case "autumn":
            case "summer":
            case "spring":
                c = new Color(0, 0, 0);// black
                break;
            case "rainbow":
                c = new Color(255, 255, 255);// white
                break;
            case "winter":
            case "cool":
            case "gray scale":
                c = new Color(255, 0, 0);// red
                break;
            case "pink":
                c = new Color(0, 255, 0);// green
                break;
            case "bronze":
            case "storedata":
            case "copper":
            case "hot":
                c = new Color(0, 0, 255);// blue
                break;
            case "blue to red":
            case "rgb":
            case "jet":
                c = new Color(255, 255, 0);// yellow
                break;
            case "greenradar":
            case "bone":
                c = new Color(255, 0, 255);// purple
                break;

            default:
                NeptusLog.pub().info("ColorMap.toString() not found, assuming bronze code");
                c = new Color(0, 0, 255);
                break;
        }
        return c;
    }

    public static Color getContrastColor(Color color) {
        Color result = null;

        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int newR = 255 - r;
        int newG = 255 - g;
        int newB = 255 - b;

        result = new Color(newR, newG, newB);

        return result;
    }

    public static ColorMap getColorMapFromCode(int colorMapCode) {
        switch (colorMapCode) {
            case 0:
                return ColorMapFactory.createBronzeColormap();
            case 1:
                return ColorMapFactory.createStoreDataColormap();
            case 2:
                return ColorMapFactory.createRainbowColormap();
            case 3:
                return ColorMapFactory.createRedYellowGreenColorMap();
            case 4:
                return ColorMapFactory.createGreenRadarColorMap();
            case 5:
                return ColorMapFactory.createPinkColorMap();
            case 6:
                return ColorMapFactory.createBlueToRedColorMap();
            case 7:
                return ColorMapFactory.createRedGreenBlueColorMap();
            case 8:
                return ColorMapFactory.createWinterColorMap();
            case 9:
                return ColorMapFactory.createAutumnColorMap();
            case 10:
                return ColorMapFactory.createSummerColorMap();
            case 11:
                return ColorMapFactory.createSpringColorMap();
            case 12:
                return ColorMapFactory.createBoneColorMap();
            case 13:
                return ColorMapFactory.createCopperColorMap();
            case 14:
                return ColorMapFactory.createHotColorMap();
            case 15:
                return ColorMapFactory.createCoolColorMap();
            case 16:
                return ColorMapFactory.createJetColorMap();
            case 17:
                return ColorMapFactory.createGrayScaleColorMap();
            default:
                NeptusLog.pub().info("colorMap code not found, using default Bronze");
                return ColorMapFactory.createBronzeColormap();
        }
    }

    /**
     *
     * @param m double in meters
     * @param range float in meters
     * @param size max index on SidescanLine.data
     * @return convert double m in meters to corresponding index within size
     */
    public static int convertMtoIndex(double m, float range, int size) {
        return (int) ((m / (2 * range)) * size);
    }

    public static void generateReport(LsfLogSource source, JFreeChart[] charts, File desFile) {
        Rectangle pageSize = PageSize.A4.rotate();
        try {
            FileOutputStream out = new FileOutputStream(desFile);

            Document doc = new Document(pageSize);

            doc.addTitle(I18n.text("Neptus Mission Report") + " - " + source.name());
            doc.addCreationDate();
            doc.addCreator("Neptus " + ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion('1');

            doc.open();

            PdfContentByte cb = writer.getDirectContent();
            int page = 1;
            writeFirstPage(cb, source);
            doc.newPage();
            page++;

            for (int i = 0; i < charts.length; i++) {
                cb.beginText();

                java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
                int width = (int) pageSize.getWidth();
                int height = (int) pageSize.getHeight();

                Paint oldPaint = charts[i].getBackgroundPaint();
                charts[i].setBackgroundPaint(Color.white);
                charts[i].draw(g2, new Rectangle2D.Double(25, 25, width - 50, height - 50));
                charts[i].setBackgroundPaint(oldPaint);
                g2.dispose();
                writePageNumber(cb, page++);
                writeHeader(cb, source);
                writeFooter(cb, source);
                doc.newPage();
            }

            doc.close();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void generateLogs(File f, MRAPanel panel) {
        try {
            LsfLogSource folder = new LsfLogSource(f, null);
            Vector<File> subFolders = new Vector<File>();
            for (File fl : f.listFiles()) {
                if (fl.isDirectory()) {
                    subFolders.add(fl);
                    try {
                        generateReport(folder, new File(f.getName() + ".pdf"), panel);
                    }
                    catch (Exception e) {
                        System.err.println(I18n.textf("Unable to generate report for %filename", fl.getAbsolutePath()));
                    }
                }
            }

            for (File fl : subFolders)
                generateLogs(fl, panel);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}