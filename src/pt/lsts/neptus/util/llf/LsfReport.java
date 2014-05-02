/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
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

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
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
import pt.lsts.neptus.plugins.sidescan.SidescanAnalyzer;
import pt.lsts.neptus.plugins.sidescan.SidescanConfig;
import pt.lsts.neptus.plugins.sidescan.SidescanPanel;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.SvgUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.chart.LLFChart;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
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

        if (!MRAProperties.printPageNumbers)
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

            JFreeChart chart = llfChart.getChart(source, MRAProperties.defaultTimestep);
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
            createTable(cb,doc,source,panel);//table with Marks
            writePageNumber(cb, page++, llfCharts.size()+4);
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
    
    public static void createTable(PdfContentByte cb, Document doc, IMraLogGroup source, MRAPanel panel) throws DocumentException {
        
        try {
            Rectangle pageSize = PageSize.A4.rotate();
            PdfPTable table = new PdfPTable(4);
            
            float  tableWidth = pageSize.getWidth()*2/3;
            table.setTotalWidth(tableWidth);
            float[] columnWidth = {0.15f, 0.30f, 0.15f, 0.40f};
            table.setWidthPercentage(columnWidth, pageSize);
            ArrayList<LogMarker> markers = panel.getMarkers();
            
            //header
            table.addCell("Timestamp");table.addCell("Label");table.addCell("Location");table.addCell("Image");
            SidescanLogMarker sd;
            for (LogMarker m : markers){
                String dateAsText = new SimpleDateFormat("HH:mm:ss.ms").format(m.timestamp);
                table.addCell(dateAsText);
                table.addCell(m.label);
                LocationType loc = new LocationType(Math.toDegrees(m.lat), Math.toDegrees(m.lon));
                String locString = loc.toString();
                table.addCell(locString);
                
                if (m.getClass()==SidescanLogMarker.class){//sidescanImages
                    sd = (SidescanLogMarker) m;
                    table.addCell("w="+sd.w+" | h="+sd.h);
                    //com.lowagie.text.Image itextImage;//iText image type
                    BufferedImage image;
                    SidescanParser ssParser = SidescanParserFactory.build(source);
                    image = getSidescanMark(source, ssParser, sd);
                    if (image!=null){//debug of image
                        String path = "/home/miguel/lsts/sidescanImages/";
                        ImageIO.write(image, "PNG", new File(path, "test("+sd.label+").png"));
                    }              
                }else//not in sidescan
                    table.addCell("");
            }          
            
            //write to pdf
            cb.beginText();
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            cb.setFontAndSize(bf, 24);
            cb.setColorFill(new Color(50, 100, 200));
            cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Marks' Table"), pageSize.getWidth()/2, pageSize.getHeight()-100, 0);
            cb.endText();
            
            float xpos = pageSize.getWidth()/6;
            float ypos = pageSize.getHeight()-160;
            
            //headers:
            cb.setColorFill(Color.red.brighter());
            ypos = table.writeSelectedRows(0, 1, xpos, ypos, cb);
            
            //data:
            ArrayList rows = table.getRows();
            for (int i=1;i<rows.size();i++){
                if (ypos<100){//check ypos within page range
                    doc.newPage();
                    cb.beginText();
                    cb.setFontAndSize(bf, 24);
                    cb.setColorFill(new Color(50, 100, 200));
                    cb.showTextAligned(PdfContentByte.ALIGN_CENTER, I18n.text("Marks' Table"), pageSize.getWidth()/2, pageSize.getHeight()-100, 0);
                    cb.endText();
                    ypos = pageSize.getHeight()-160;
                    cb.setColorFill(Color.red.brighter());
                    ypos = table.writeSelectedRows(0, 1, xpos, ypos, cb);
                }
                if (i%2==0)
                    cb.setColorFill(Color.gray.darker());
                else
                    cb.setColorFill(Color.black);
                ypos = table.writeSelectedRows(i, i+1, xpos, ypos, cb);  
            }
            
        }catch (Exception e) {
            e.printStackTrace();
            GuiUtils.errorMessage(I18n.text("Error createTable()"), e.getMessage());
        }
    }
    
    //new Method
    public static BufferedImage getSidescanMark(IMraLogGroup source, SidescanParser ssParser, SidescanLogMarker mark) throws DocumentException {
        BufferedImage img = null;
        
        int h = mark.h;
        int w = mark.w;
        
        //adjustments:
        if (w<100 || h<100){
            if (w<100)
                w=100;
            if( h<100)
                h=100;
        }else if (w<150 || h<150){
            if (w<150)
                w *= 1.2;
            if (h<150)
                h *= 1.2;
        }else if (w<200 || h<200){
            if (w<200)
                w *= 1.1;
            if (h<200)
                h *= 1.1;
        }
        
        //times
        long t,t1,t2;
        t = (long) mark.timestamp;
        long avgTBP;// = 250;//check whether this is a constant for this vehicle/log and get it
        long firstTimestamp = ssParser.firstPingTimestamp();
        long lastTimestamp = ssParser.lastPingTimestamp();
        //LinkedHashMap<Integer, String> hash = LogUtils.getEntities(source);
        
        int totalMsg =1;
        LsfIndex index = source.getLsfIndex();
        //totalMsg = index.;//get the totalMsg of sidescan type here
        avgTBP = (lastTimestamp - firstTimestamp)/totalMsg;

        
        t1 = t - 250*(h/2);
        t2 = t + 250*(h/2);
        
        //get the lines
        SidescanConfig config = new SidescanConfig();
        SidescanParameters sidescanParams = new SidescanParameters(0, 0);
        sidescanParams.setNormalization(config.normalization);
        sidescanParams.setTvgGain(config.tvgGain);
        ArrayList<SidescanLine> list =ssParser.getLinesBetween(t1, t2, ssParser.getSubsystemList().get(0), sidescanParams);
        
        if (list.isEmpty())
            throw new DocumentException("list of lines empty");
        
        
        
        return img;
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


