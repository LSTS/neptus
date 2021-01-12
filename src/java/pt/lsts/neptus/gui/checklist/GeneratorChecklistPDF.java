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
 * Author: Paulo Dias
 * Created in 12/Oct/2008
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.LinkedList;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.MultiColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserActionItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;
import pt.lsts.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.lsts.neptus.types.checklist.CheckItem;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.util.BarCodesUtil;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.SvgUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class GeneratorChecklistPDF {

    protected static org.w3c.dom.Document logoDoc = null;

    public static org.w3c.dom.Document getLogoDoc() {
        if (logoDoc == null) {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = FileUtil.getFileAsString(FileUtil
                    .getResourceAsFile("/images/neptus_logo_ns.svg"));
            try {
                logoDoc = f.createDocument(null, new StringReader(data));
                logoDoc = SvgUtil.cleanInkscapeSVG(logoDoc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return logoDoc;
    }

    public static boolean generateReport(ChecklistType clist, File destination) {
        return generateReport(clist, destination, (short) 3);
    }

    public static boolean generateReport(ChecklistType clist, File destination, short columns) {
        columns = (short) Math.max(columns, 1);
        columns = (short) Math.min(columns, 3);

        Rectangle pageSize = PageSize.A4;
        try {
            FileOutputStream out = new FileOutputStream(destination);

            Document doc = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            writer.setViewerPreferences(PdfWriter.PageLayoutOneColumn);
            writer.setPageEvent(new PageEvents(clist));  
            doc.setMargins(54, 54, 54, 54);  

            doc.open();

            doc.addTitle(I18n.textf("Checklist - %name", clist.getName()));
            doc.addCreationDate();
            doc.addCreator("Neptus " + ConfigFetch.getNeptusVersion());
            doc.addProducer();
            doc.addAuthor(System.getProperty("user.name"));			

            Color BLUE_1 = new Color(31, 73, 125);

            BaseFont bf  = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            BaseFont bfz = BaseFont.createFont(BaseFont.ZAPFDINGBATS, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);

            Font font14BoldBlue = new Font(bf, 14, Font.BOLD, BLUE_1);
            Font font10BoldBlue = new Font(bf, 10, Font.BOLD, BLUE_1);
            Font font9BoldBlue = new Font(bf, 9, Font.BOLD, BLUE_1);

            Font font8BoldDarkGray = new Font(bf, 8, Font.BOLD, Color.DARK_GRAY);

            Font font8NormalBlack = new Font(bf, 8, Font.NORMAL, Color.BLACK);
            Font font8NormalGray = new Font(bf, 8, Font.NORMAL, Color.GRAY);
            Font font8NormalLightGray = new Font(bf, 8, Font.NORMAL, Color.LIGHT_GRAY.darker());

            Font font7 = new Font(bf, 7, Font.NORMAL);

            Font fontZ14 = new Font(bfz, 14, Font.NORMAL);

            Paragraph titleP = new Paragraph();
            titleP.setSpacingBefore(10);
            titleP.setSpacingAfter(10);
            Phrase title = new Phrase();
            title.setFont(font14BoldBlue);
            title.add(clist.getName());
            title.add(Chunk.NEWLINE);
            if (clist.getVersion() != null && !"".equalsIgnoreCase(clist.getVersion())) {
                title.setFont(font10BoldBlue);
                title.add(I18n.textf("Version: %version", clist.getVersion()));
                title.add(Chunk.NEWLINE);
            }

            String text = clist.getName()
                    + " | " + I18n.textf("Version: %version", clist.getVersion()) 
                    + " | " + I18n.textf("By: %info", System.getProperty("user.name") + " | " + "@" 
                    + DateTimeUtil.dateTimeFormatter.format(new Date(System
                            .currentTimeMillis())) + " | "
                            + ConfigFetch.getVersionSimpleString());
            BufferedImage imgB = BarCodesUtil.createQRCodeImage(text, 50, 50);
            Image img = Image.getInstance(imgB, null);
            titleP.add(img);

            titleP.add(title);
            doc.add(titleP);

            if (clist.getDescription() != null && !clist.getDescription().isEmpty()) {
                Paragraph descP = new Paragraph();
                descP.setSpacingBefore(10);
                descP.setSpacingAfter(5);
                descP.setAlignment(Paragraph.ALIGN_JUSTIFIED);
                Phrase desc = new Phrase();
                desc.setFont(font10BoldBlue);
                desc.add(I18n.text("Description:"));
                desc.add(Chunk.NEWLINE);
                descP.add(desc);
                doc.add(descP);
                Paragraph descP2 = new Paragraph();
                descP2.setSpacingBefore(0);
                descP2.setSpacingAfter(10);
                descP2.setAlignment(Paragraph.ALIGN_JUSTIFIED);
                descP2.setLeading(11);
                Phrase descText = new Phrase();
                descText.setFont(font8NormalGray);
                descText.add(clist.getDescription().replaceAll("\n\n", "\n"));
                descText.add(Chunk.NEWLINE);
                descP2.add(descText);
                doc.add(descP2);
            }

            MultiColumnText mct = new MultiColumnText();   
            mct.addRegularColumns(doc.left(),  
                    doc.right(), 10f, columns);

            for (String grpName : clist.getGroupList().keySet()) {
                if (!clist.isFlat()) {
                    Paragraph grpP = new Paragraph();
                    grpP.setSpacingBefore(8);
                    Phrase grpText = new Phrase();
                    //font9B.setColor(BLUE_1);
                    grpText.setFont(font9BoldBlue);
                    grpText.add(grpName);
                    grpText.add(Chunk.NEWLINE);
                    grpP.add(grpText);
                    mct.addElement(grpP);
                }
                LinkedList<CheckItem> gplist = clist.getGroupList().get(grpName);
                for (CheckItem ci : gplist) {
                    Paragraph ciP = new Paragraph();
                    ciP.setLeading(15);
                    Phrase ciText = new Phrase();
                    Chunk cv;
                    if (ci.isSkiped())
                        cv = new Chunk('n'); // n or m
                    else if (ci.isChecked())
                        cv = new Chunk('4');//4
                    else
                        cv = new Chunk('o'); //'q'
                    fontZ14.setColor(Color.DARK_GRAY);
                    ciText.setFont(fontZ14);
                    ciText.add(cv);
                    ciText.setFont(font8NormalBlack);

                    ciText.add(" ");
                    ciText.add(ci.getName());
                    ciP.add(ciText);
                    if (ci.isChecked() && !ci.isSkiped()) {
                        Phrase dateF = new Phrase(8);
                        font7.setColor(Color.GRAY);
                        dateF.setFont(font7);
                        if (columns > 2)
                            dateF.add(Chunk.NEWLINE);
                        dateF.add(" ("+ci.getDateChecked()+")");
                        ciP.add(dateF);
                    }
                    ciP.add(Chunk.NEWLINE);
                    mct.addElement(ciP);
                    if (!"".equalsIgnoreCase(ci.getNote())) {
                        Paragraph noteP = new Paragraph();
                        noteP.setSpacingBefore(3);
                        noteP.setIndentationLeft(10);
                        noteP.setAlignment(Paragraph.ALIGN_JUSTIFIED);
                        noteP.setLeading(9);
                        Phrase noteF = new Phrase();
                        noteF.setFont(font8BoldDarkGray);
                        noteF.add(I18n.text("Note:") + " ");
                        noteF.setFont(font8NormalLightGray);
                        noteF.add(ci.getNote().replaceAll("\n\n", "\n"));
                        noteP.add(noteF);
                        mct.addElement(noteP);
                    }
                    if (!ci.getAutoSubItems().isEmpty()) {
                        PdfPTable table = new PdfPTable(1);

                        for (CheckAutoSubItem si : ci.getAutoSubItems()) {
                            if (UserActionItem.TYPE_ID.equals(si.getSubItemType())) {
                                CheckAutoUserActionItem it = (CheckAutoUserActionItem) si;
                                Phrase uaF = new Phrase();
                                uaF.setFont(font8BoldDarkGray);
                                uaF.add(I18n.text("Action:") + " ");
                                uaF.setFont(font8NormalLightGray);
                                uaF.add(it.getAction());
                                PdfPCell uaCell = new PdfPCell(new Paragraph(uaF));
                                //uaCell.setColspan(2);
                                //uaCell.setBorder(Rectangle.NO_BORDER);
                                uaCell.setBorderWidthLeft(6f);
                                uaCell.setBorderWidthBottom(0f);
                                uaCell.setBorderWidthRight(0f);
                                uaCell.setBorderWidthTop(0f);
                                if (!ci.isSkiped()) {
                                    if (it.isChecked())
                                        uaCell.setBorderColorLeft(ChecklistPanel.BLUE_1);
                                    else
                                        uaCell.setBorderColorLeft(ChecklistPanel.RED_1);
                                }
                                table.addCell(uaCell);
                            }
                            else if (UserCommentItem.TYPE_ID.equals(si.getSubItemType())) {
                                CheckAutoUserLogItem it = (CheckAutoUserLogItem) si;
                                Phrase uaF = new Phrase();
                                uaF.setFont(font8BoldDarkGray);
                                uaF.add(it.getLogRequest()+": ");
                                uaF.setFont(font8NormalLightGray);
                                uaF.add(it.getLogMessage());
                                PdfPCell uaCell = new PdfPCell(new Paragraph(uaF));
                                //uaCell.setColspan(2);
                                //uaCell.setBorder(Rectangle.NO_BORDER);
                                uaCell.setBorderWidthLeft(6f);
                                uaCell.setBorderWidthBottom(0f);
                                uaCell.setBorderWidthRight(0f);
                                uaCell.setBorderWidthTop(0f);
                                if (!ci.isSkiped()) {
                                    if (it.isChecked())
                                        uaCell.setBorderColorLeft(ChecklistPanel.BLUE_1);
                                    else
                                        uaCell.setBorderColorLeft(ChecklistPanel.RED_1);
                                }
                                table.addCell(uaCell);
                            }
                            else if (VariableIntervalItem.TYPE_ID.equals(si.getSubItemType())) {
                                CheckAutoVarIntervalItem it = (CheckAutoVarIntervalItem) si;
                                Phrase uaF = new Phrase();
                                uaF.setFont(font8BoldDarkGray);
                                uaF.add(it.getVarName()+"=");
                                uaF.setFont(font8NormalLightGray);
                                uaF.add(""+it.getVarValue());
                                PdfPCell uaCell = new PdfPCell(new Paragraph(uaF));
                                //uaCell.setColspan(2);
                                //uaCell.setBorder(Rectangle.NO_BORDER);
                                uaCell.setBorderWidthLeft(6f);
                                uaCell.setBorderWidthBottom(0f);
                                uaCell.setBorderWidthRight(0f);
                                uaCell.setBorderWidthTop(0f);
                                if (!ci.isSkiped()) {
                                    if (it.isChecked())
                                        uaCell.setBorderColorLeft(ChecklistPanel.BLUE_1);
                                    else
                                        uaCell.setBorderColorLeft(ChecklistPanel.RED_1);
                                }
                                table.addCell(uaCell);
                            }
                        }
                        table.setWidthPercentage(95);
                        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.setSpacingBefore(8f);
                        table.setSpacingAfter(8f);

                        mct.addElement(table);
                    }					
                }
            }

            doc.add(mct);

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


    /**
     * @param args
     */
    public static void main(String[] args) {

        ConfigFetch.initialize();

        generateReport(new ChecklistType("checklists/check3.nchk"), new File(
                "checklists/check2_test.pdf"));

        generateReport(new ChecklistType("checklists/check4.nchk"), new File(
                "checklists/check2_test1.pdf"));
    }
}

class PageEvents extends PdfPageEventHelper {
    protected ChecklistType clist = null;
    protected PdfTemplate total;   
    protected BaseFont helv; 

    public PageEvents(ChecklistType clist) {
        this.clist = clist;
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        total = writer.getDirectContent().createTemplate(100, 100);   
        total.setBoundingBox(new Rectangle(-20, -20, 100, 100));
        try {
            helv = BaseFont.createFont(BaseFont.HELVETICA,   
                    BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); 
        }
        catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }
    
    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();

        cb.setColorFill(Color.gray);

        //String text = "Page " + writer.getPageNumber() + " of ";
        String text = I18n.text("Document generated by") + " Neptus ";//+ConfigFetch.getNeptusVersion();//+" on "+new Date();
        float textBase = document.bottom() - 20;
        float textSize = helv.getWidthPoint(text, 8);
        cb.beginText();
        cb.setFontAndSize(helv, 8);

        cb.setTextMatrix(document.left(), textBase);  
        cb.showText(text);                       
        cb.endText();                                       
        cb.addTemplate(total, document.left() + textSize, textBase);   

        text = "" + writer.getPageNumber();
        textSize = helv.getWidthPoint(text, 8);
        cb.beginText();
        float adjust = helv.getWidthPoint("0", 8);  
        cb.setTextMatrix(document.right() - textSize - adjust, textBase);                
        cb.showText(text);                                                
        cb.endText();                                       
        cb.addTemplate(total, document.right() - adjust, textBase);   

        //text = ""+new Date();
        text = (clist != null) ? clist.getName() : "";
        textBase = document.top() + 20;
        textSize = helv.getWidthPoint(text, 8);
        cb.beginText();
        cb.setFontAndSize(helv, 8);
        cb.setTextMatrix(document.left(), textBase);  
        cb.showText(text);                       
        cb.endText();                                       
        cb.addTemplate(total, document.left() + textSize, textBase);   

        text = "" + DateTimeUtil.dateTimeFormatterNoSegs.format(new Date());
        textBase = document.top() + 20;
        textSize = helv.getWidthPoint(text, 8);
        cb.beginText();
        cb.setFontAndSize(helv, 8);
        cb.setTextMatrix(document.right() - textSize, textBase);  
        cb.showText(text);                       
        cb.endText();                                       
        cb.addTemplate(total, document.right(), textBase);   

        cb.setColorStroke(Color.GRAY);
        cb.setLineWidth(1);
        cb.moveTo(document.left(), document.top() + 20-2);
        cb.lineTo(document.right(), document.top() + 20-2);
        cb.stroke();

        cb.setColorStroke(Color.GRAY);
        cb.setLineWidth(1);
        cb.moveTo(document.left(), document.bottom() - 20+10);
        cb.lineTo(document.right(), document.bottom() - 20+10); 
        cb.stroke();

        cb.restoreState();
    }
    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        //	  total.beginText();
        //	  total.setFontAndSize(helv, 8);                            
        //	  total.setTextMatrix(0, 0);                                 
        //	  total.showText(String.valueOf(writer.getPageNumber() - 1));   
        //	  total.endText();   
    }
}
