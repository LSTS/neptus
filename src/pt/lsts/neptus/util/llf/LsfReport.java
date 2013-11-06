/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jfree.chart.JFreeChart;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.PathElement;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.SvgUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.llf.chart.LLFChart;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * @author ZP
 * @author pdias logo work
 */
public class LsfReport {

    protected static org.w3c.dom.Document logoDoc = null;

    public static org.w3c.dom.Document getLogoDoc() {
        if (logoDoc == null) {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = FileUtil.getFileAsString(FileUtil
                    .getResourceAsFile("/images/neptus_logo_ns.svg"));
            try {
                //logoDoc = f.createSVGDocument(null, new java.io.StringReader((String)data));
                logoDoc = f.createDocument(null, new StringReader((String)data));
                logoDoc = SvgUtil.cleanInkscapeSVG(logoDoc);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return logoDoc;
    }


    private static void writeDetailsPage(PdfContentByte cb,IMraLogGroup source) {
        Rectangle pageSize = PageSize.A4.rotate();

        try {

            cb.beginText();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            cb.setFontAndSize(bf, 24);
            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Mission Statistics"), pageSize.getWidth()/2, pageSize.getHeight()-100, 0);

            cb.setFontAndSize(bf, 14);
            cb.setColorFill(Color.gray.darker());

            float xpos = pageSize.getWidth()/4;
            float ypos = pageSize.getHeight()-160;

            LinkedHashMap<String, String> stats = LogUtils.generateStatistics(source);

            for (String field : stats.keySet()) {
                cb.setColorFill(Color.blue.darker());
                cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, field+":", xpos - 10, ypos, 0);
                cb.setColorFill(Color.black);
                cb.showTextAligned(PdfContentByte.ALIGN_LEFT, stats.get(field), xpos + 10, ypos, 0);

                ypos -=20;
            }
            cb.endText();

            VehicleType vehicle = LogUtils.getVehicle(source);

            if (vehicle != null) {
                Image vehicleImage = vehicle.getPresentationImageHref().equalsIgnoreCase("") ?
                        ImageUtils.getScaledImage(vehicle.getSideImageHref(), 300, 300) :
                            ImageUtils.getScaledImage(vehicle.getPresentationImageHref(), 300, 300);

                        PdfTemplate tp = cb.createTemplate(300, 300);

                        java.awt.Graphics2D g2 = tp.createGraphicsShapes(300, 300);
                        g2.drawImage(vehicleImage, 0, 0, null);
                        g2.dispose();

                        cb.addTemplate(tp, pageSize.getWidth()-350, pageSize.getHeight()-460);
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
            if (getLogoDoc() != null)
            {
                PrintTranscoder prm = new PrintTranscoder();
                prm.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, new Float(500));
                prm.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, new Float(193));
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
                cb.addTemplate(tp, pageSize.getWidth()/2-250, pageSize.getHeight()/2-91+120);
            }

            cb.beginText();

            cb.setFontAndSize(bf, 18);

            Date d = new Date(source.getLog("EstimatedState").currentTimeMillis());

            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, source.name()+ "  ("+LogUtils.getVehicle(source)+")", pageSize.getWidth()/2, pageSize.getHeight()/2-40, 0);

            cb.setColorFill(new Color(200,200,200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.textf("Mission executed on %date", d.toString()), pageSize.getWidth()/2, pageSize.getHeight()/2-140, 0);
            cb.setFontAndSize(bf, 32);

            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Neptus Mission Report"), pageSize.getWidth()/2, pageSize.getHeight()/2, 0);

            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static void writePageNumber(PdfContentByte cb, int curPage, int totalPages) {

        if (!NeptusMRA.printPageNumbers)
            return;
        
        Rectangle pageSize = PageSize.A4.rotate();

        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.beginText();
            cb.setFontAndSize(bf, 12);

            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT, curPage+"/"+totalPages, pageSize.getWidth()-10, 575, 0);
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

            //cb.moveTo(200,200);
            cb.moveText(10, 575);


            cb.showText(I18n.text("Neptus Mission Report")+" - "+source.name());
            cb.endText();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeFooter(PdfContentByte cb,IMraLogGroup source) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.setColorFill(Color.gray);
            cb.beginText();
            cb.setFontAndSize(bf, 12);
            Rectangle pageSize = PageSize.A4.rotate();

            //cb.moveTo(200,200);
            cb.moveText(10, 10);
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.textf("Document generated by %generator on %date.", "Neptus "+ConfigFetch.getNeptusVersion(), new Date().toString()), pageSize.getWidth()/2, 10, 0);

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
            doc.addCreator("Neptus "+ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));         

            PdfContentByte cb = writer.getDirectContent();
            java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            JFreeChart chart = llfChart.getChart(source, NeptusMRA.defaultTimestep);
            chart.setTitle("");
            chart.setBackgroundPaint(Color.white);
            chart.draw(g2, new Rectangle2D.Double(25, 25, width-50, height-50));

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
        for (int i = 0; i < tree.chartsNode.getChildCount(); i++) {
            try {
                LLFChart chart = (LLFChart)((DefaultMutableTreeNode)tree.chartsNode.getChildAt(i)).getUserObject();
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

            doc.addTitle(I18n.text("Neptus Mission Report")+" - "+source.name());
            doc.addCreationDate();
            doc.addCreator("Neptus "+ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));			

            PdfContentByte cb = writer.getDirectContent();
            int page = 1;
            writeFirstPage(cb, source);
            page++;

            doc.newPage();

            for (LLFChart llfChart : llfCharts) {

                java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
                int width = (int) pageSize.getWidth();
                int height = (int) pageSize.getHeight();

                JFreeChart chart = llfChart.getChart(source, llfChart.getDefaultTimeStep());
                chart.setBackgroundPaint(Color.white);
                chart.draw(g2, new Rectangle2D.Double(25, 25, width-50, height-50));

                g2.dispose();
                writePageNumber(cb, page++, llfCharts.size()+3);
                writeHeader(cb, source);
                writeFooter(cb, source);
                doc.newPage();

            }

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width-100, height-100);

            final Graphics2D g2 = tp.createGraphicsShapes(width-100, height-100);

            MissionType mt = LogUtils.generateMission(source);
            PlanType plan = LogUtils.generatePlan(mt, source);
            PathElement path = LogUtils.generatePath(mt, source);
            final StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mt));
            r2d.setShowWorldMapOnScreenControls(false);
            PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
            po.setTransp2d(0.5);
            po.setPlan(plan);
            po.setRenderer(r2d);
            po.setColor(new Color(255,255,255,128));
            po.setShowDistances(false);
            po.setShowManNames(false);
            r2d.addPostRenderPainter(po, "Plan Painter");

            r2d.setSize(width-100, height-100);
            r2d.focusLocation(path.getCenterPoint());
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    r2d.update(g2);
                }
            });
            g2.dispose();
            cb.addTemplate(tp, 50, 50);
            writePageNumber(cb, page++, llfCharts.size()+3);
            writeHeader(cb, source);
            writeFooter(cb, source);

            doc.newPage();
            writeDetailsPage(cb, source);
            writePageNumber(cb, page++, llfCharts.size()+3);
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

    public static void generateReport(LsfLogSource source, JFreeChart[] charts, File desFile) {
        Rectangle pageSize = PageSize.A4.rotate();
        try {
            FileOutputStream out = new FileOutputStream(desFile);

            Document doc = new Document(pageSize);

            doc.addTitle(I18n.text("Neptus Mission Report")+" - "+source.name());
            doc.addCreationDate();
            doc.addCreator("Neptus "+ConfigFetch.getNeptusVersion());
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
                charts[i].draw(g2, new Rectangle2D.Double(25, 25, width-50, height-50));
                charts[i].setBackgroundPaint(oldPaint);
                g2.dispose();
                writePageNumber(cb, page++, charts.length+2);
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
                        generateReport(folder, new File(f.getName()+".pdf"), panel);
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
