/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * Created by pdias
 * Created in 12/Oct/2008
 */
package pt.up.fe.dceg.neptus.gui.checklist;

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

import pt.up.fe.dceg.neptus.types.checklist.CheckAutoSubItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoUserActionItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoUserLogItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoVarIntervalItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckItem;
import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
import pt.up.fe.dceg.neptus.util.BarCodesUtil;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.SvgUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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

			doc.addTitle("Checklist - "+clist.getName());
			doc.addCreationDate();
			doc.addCreator("Neptus "+ConfigFetch.getNeptusVersion());
			doc.addProducer();
			doc.addAuthor(System.getProperty("user.name"));			
			
//			PdfContentByte cb = writer.getDirectContent();
//			int page = 1;
//			writeFirstPage(cb, source);
//			page++;
//
//			doc.newPage();
//			writePageNumber(cb, page++, 10);
//			writeHeader(cb, source);
//			writeFooter(cb, source);

			//BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			//cb.beginText();
			//cb.setFontAndSize(bf, 18);
			//cb.setColorFill(new Color(31, 73, 125));
			//cb.showText(source.getName());   
			//doc.add(
			//		new Phrase("Quick brown fox jumps over the lazy dog. "));
			//cb.endText();                                       
			
			
			Color BLUE_1 = new Color(31, 73, 125);
			
			BaseFont bf  = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			BaseFont bfz = BaseFont.createFont(BaseFont.ZAPFDINGBATS, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
			
			Font font14BoldBlue = new Font(bf, 14, Font.BOLD, BLUE_1);
			Font font10BoldBlue = new Font(bf, 10, Font.BOLD, BLUE_1);
			Font font9BoldBlue = new Font(bf, 9, Font.BOLD, BLUE_1);
			
			Font font8BoldDarkGray = new Font(bf, 8, Font.BOLD, Color.DARK_GRAY);
			
			//Font font8 = new Font(bf, 8, Font.NORMAL);
			Font font8NormalBlack = new Font(bf, 8, Font.NORMAL, Color.BLACK);
			Font font8NormalGray = new Font(bf, 8, Font.NORMAL, Color.GRAY);
			Font font8NormalLightGray = new Font(bf, 8, Font.NORMAL, Color.LIGHT_GRAY.darker());
			
			Font font7 = new Font(bf, 7, Font.NORMAL);
			
			Font fontZ14 = new Font(bfz, 14, Font.NORMAL);
			
			Paragraph titleP = new Paragraph();
			titleP.setSpacingBefore(10);
			titleP.setSpacingAfter(10);
			Phrase title = new Phrase();
			//font14B.setColor(BLUE_1);
			title.setFont(font14BoldBlue);
			title.add(clist.getName());
			title.add(Chunk.NEWLINE);
			if (clist.getVersion() != null && !"".equalsIgnoreCase(clist.getVersion())) {
			    title.setFont(font10BoldBlue);
			    title.add("Version: " + clist.getVersion());
			    title.add(Chunk.NEWLINE);
			}

//			BarcodePDF417 pdf417 = new BarcodePDF417();
			String text = clist.getName()
			        + " | " + "version: " + clist.getVersion() 
					+ " | " + "By:" + System.getProperty("user.name") + " | " + "@" 
					+ DateTimeUtil.dateTimeFormater.format(new Date(System
							.currentTimeMillis())) + " | "
					+ ConfigFetch.getVersionSimpleString();
//			pdf417.setText(text);
//			Image img = pdf417.getImage();
//			img.scalePercent(50, 50 * pdf417.getYHeight());
			BufferedImage imgB = BarCodesUtil.createQRCodeImage(text, 50, 50);
			Image img = Image.getInstance(imgB, null);
			titleP.add(img);

			titleP.add(title);
			doc.add(titleP);
			

			if (!"".equalsIgnoreCase(clist.getDescription())) {
				Paragraph descP = new Paragraph();
				descP.setSpacingBefore(10);
				descP.setSpacingAfter(5);
				descP.setAlignment(Paragraph.ALIGN_JUSTIFIED);
				Phrase desc = new Phrase();
				//font10B.setColor(BLUE_1);
				desc.setFont(font10BoldBlue);
				desc.add("Description:");
				desc.add(Chunk.NEWLINE);
				descP.add(desc);
				doc.add(descP);
				Paragraph descP2 = new Paragraph();
				descP2.setSpacingBefore(0);
				descP2.setSpacingAfter(10);
				descP2.setAlignment(Paragraph.ALIGN_JUSTIFIED);
				descP2.setLeading(11);
				Phrase descText = new Phrase();
				//font8.setColor(Color.GRAY);
				descText.setFont(font8NormalGray);
				descText.add(clist.getDescription().replaceAll("\n\n", "\n"));
				descText.add(Chunk.NEWLINE);
				descP2.add(descText);
				doc.add(descP2);
			}
			
			MultiColumnText mct = new MultiColumnText();   
			mct.addRegularColumns(doc.left(),  
					doc.right(), 10f, columns);

			
			//Paragraph clP = new Paragraph();
			//clP.setSpacingBefore(10);
			//clP.setSpacingAfter(10);
			//font8.setColor(Color.BLACK);
			//fontZ14.setColor(Color.DARK_GRAY);
			//Font fctx  = font8;
			//Font fcval = fontZ14;
			//clP.setFont(font8);
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
					//ciP.setSpacingBefore(10);
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
					//font8.setColor(Color.BLACK);
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
						//font8B.setColor(Color.DARK_GRAY);
						noteF.setFont(font8BoldDarkGray);
						noteF.add("Note: ");
						//font8.setColor(Color.LIGHT_GRAY);
						noteF.setFont(font8NormalLightGray);
						noteF.add(ci.getNote().replaceAll("\n\n", "\n"));
						noteP.add(noteF);
						mct.addElement(noteP);
					}
					if (!ci.getAutoSubItems().isEmpty()) {
						//Phrase ttF = new Phrase();
						//ttF.setFont(font8BoldDarkGray);
						//ttF.add("SubItems");
						
						PdfPTable table = new PdfPTable(1);
						//PdfPCell cell = new PdfPCell(new Paragraph(ttF));
						////cell.setColspan(2);
						//cell.setBorder(Rectangle.NO_BORDER);
						//table.addCell(cell);
						
						for (CheckAutoSubItem si : ci.getAutoSubItems()) {
							if (UserActionItem.TYPE_ID.equals(si.getSubItemType())) {
								CheckAutoUserActionItem it = (CheckAutoUserActionItem) si;
								Phrase uaF = new Phrase();
								//font8B.setColor(Color.DARK_GRAY);
								uaF.setFont(font8BoldDarkGray);
								uaF.add("Action: ");
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
								//font8B.setColor(Color.DARK_GRAY);
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
								//font8B.setColor(Color.DARK_GRAY);
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
						//subItemsP.add(table);
						
//						Phrase noteF = new Phrase();
//						//font8B.setColor(Color.DARK_GRAY);
//						noteF.setFont(font8BoldDarkGray);
//						noteF.add("Note: ");
//						//font8.setColor(Color.LIGHT_GRAY);
//						noteF.setFont(font8NormalLightGray);
//						noteF.add(ci.getNote().replaceAll("\n\n", "\n"));
//						subItemsP.add(noteF);
						mct.addElement(table);
					}					
				}
			}
			//mct.addElement(clP);
			
			doc.add(mct);
			
			doc.close();
			out.flush();
			out.close();

//
//			
//			for (int k = 1; k <= 300; ++k) {  
//				doc.add(
//						new Phrase("Quick brown fox jumps over the lazy dog. "));
//			}
//
//			bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
//			cb.setFontAndSize(bf, 9);
//			cb.setColorFill(new Color(50, 100, 200));

//			MultiColumnText mct = new MultiColumnText();   
//			mct.addRegularColumns(doc.left(),  
//					doc.right(), 10f, 3);
			
//			for (int i = 0; i < 30; i++) {                             
//				mct.addElement(new Paragraph(String.valueOf(i + 1)));   
//				mct.addElement(new Phrase("Quick brown fox jumps over the lazy dog. "));
//				for (int j = 0; j < 4; j++) { 
//					mct.addElement(new Phrase("Quick brown fox jumps over the lazy dog. "));
//				}           
//				mct.addElement(new Phrase("Quick brown fox jumps over the lazy dog. "));  
//				mct.addElement(new Phrase("\n\n"));            
//			}                                                          
//			doc.add(mct);
			
//			LLFChart[] charts = LLFChartFactory.getAutomaticCharts(source);
//
//			for (int i = 0; i < charts.length; i++) {
//
//				doc.newPage();
//
//				java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
//				int width = (int) pageSize.getWidth();
//				int height = (int) pageSize.getHeight();
//
//				JFreeChart chart = charts[i].getChart(source, charts[i].getDefaultTimeStep());
//				chart.setBackgroundPaint(Color.white);
//				chart.draw(g2, new Rectangle2D.Double(25, 25, width-50, height-50));
//
//				g2.dispose();
//				writePageNumber(cb, page++, charts.length+3);
//				writeHeader(cb, source);
//				writeFooter(cb, source);
//			}
//
//			// Renderer2D
//			doc.newPage();
//
//			int width = (int) pageSize.getWidth();
//			int height = (int) pageSize.getHeight();
//
//			PdfTemplate tp = cb.createTemplate(width-100, height-100);
//
//			java.awt.Graphics2D g2 = tp.createGraphicsShapes(width-100, height-100);
//
//
//			MissionType mt = LLFUtils.generateMission(source);
//			IndividualPlanType plan = LLFUtils.generatePlan(mt, source);
//			PathElement path = LLFUtils.generatePath(mt, source);
//			StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mt));
//			PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
//			po.setTransp2d(0.5);
//			po.setPlan(plan);
//			po.setRenderer(r2d);
//			po.setColor(new Color(255,255,255,128));
//			po.setShowDistances(false);
//			po.setShowManNames(false);
//			r2d.addPostRenderPainter(po);
//
//			r2d.setSize(width-100, height-100);
//			r2d.focusLocation(path.getCenterPoint());
//			r2d.update(g2);
//			g2.dispose();
//			cb.addTemplate(tp, 50, 50);
//			writePageNumber(cb, page++, charts.length+3);
//			writeHeader(cb, source);
//			writeFooter(cb, source);
//			/*
//			Camera3D cam =new Camera3D(Camera3D.USER);
//			Camera3D[]  cams={cam};
//			Renderer3D r3d = new Renderer3D(cams,(short)1,(short)1);
//
//			r3d.setMapGroup(MapGroup.getMapGroupInstance(mt));
//			r3d.focusLocation(path.getCenterLocation());
//
//			doc.newPage();
//
//			tp = cb.createTemplate(width-100, height-100);
//
//			g2 = tp.createGraphicsShapes(width-100, height-100);
//
//			r3d.setSize(width-100, height-100);
//			r3d.cams[0].getCanvas3DPanel().paint(g2);
//			//r3d.paint(g2);
//
//			g2.dispose();
//			cb.addTemplate(tp, 50, 50);
//			writePageNumber(cb, page++, charts.length+3);
//			writeHeader(cb, source);
//			writeFooter(cb, source);
//			*/
//			doc.newPage();
//			writeDetailsPage(cb, source);
//			writePageNumber(cb, page++, charts.length+3);
//			writeHeader(cb, source);
//			writeFooter(cb, source);
						
			//writer.addAnnotation(PdfAnnotation.createFileAttachment(writer, new Rectangle(100f, 100f), "sample file", "test".getBytes(), null, "texto")); 			
//			doc.close();
//			out.flush();
//			out.close();

		}
		catch (Exception e) {			
			e.printStackTrace();
			GuiUtils.errorMessage("Error generating report", e.getMessage());
			return false;
		}
		return true;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		ConfigFetch.initialize();

//		if (args.length == 0) {
//			System.out.println("Usage: LLFReport <directory>");
//			System.exit(1);
//		}

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
	
	public void onOpenDocument(PdfWriter writer, Document document) {
		  total = writer.getDirectContent().createTemplate(100, 100);   
		  total.setBoundingBox(new Rectangle(-20, -20, 100, 100));
		  try {
		    helv = BaseFont.createFont(BaseFont.HELVETICA,   
		      BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); 
		  } catch (Exception e) {
		    throw new ExceptionConverter(e);
		  }
		}
	@Override
	public void onEndPage(PdfWriter writer, Document document) {
		PdfContentByte cb = writer.getDirectContent();
		cb.saveState();

		cb.setColorFill(Color.gray);

		//String text = "Page " + writer.getPageNumber() + " of ";
		String text = "Document generated by Neptus ";//+ConfigFetch.getNeptusVersion();//+" on "+new Date();
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
		cb.setTextMatrix(                                                 
				document.right() - textSize - adjust, textBase);                
		cb.showText(text);                                                
		cb.endText();                                       
		cb.addTemplate(total, document.right() - adjust, textBase);   

		
		//text = ""+new Date();
		text = (clist != null)?clist.getName():"";
		textBase = document.top() + 20;
		textSize = helv.getWidthPoint(text, 8);
		cb.beginText();
		cb.setFontAndSize(helv, 8);
		cb.setTextMatrix(document.left(), textBase);  
		cb.showText(text);                       
		cb.endText();                                       
		cb.addTemplate(total, document.left() + textSize, textBase);   

		text = ""+DateTimeUtil.dateTimeFormaterNoSegs.format(new Date());
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
	public void onCloseDocument(PdfWriter writer, Document document) {
//	  total.beginText();
//	  total.setFontAndSize(helv, 8);                            
//	  total.setTextMatrix(0, 0);                                 
//	  total.showText(String.valueOf(writer.getPageNumber() - 1));   
//	  total.endText();   
	}
}
