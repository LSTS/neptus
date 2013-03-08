/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 200?/??/??
 * $Id:: ColorMapVisualization.java 9913 2013-02-11 19:11:17Z pdias       $:
 */
package pt.up.fe.dceg.neptus.mra;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorBar;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.ColorMapUtils;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer.DataPoint;
import pt.up.fe.dceg.neptus.gui.ColorMapListRenderer;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;

/**
 * @author ZP
 */
@SuppressWarnings("serial")
public class ColorMapVisualization extends JPanel implements MRAVisualization, ActionListener {
    public static final int CONDUCTIVITY = 0, TEMPERATURE = 1, SALINITY = 2, BATHYMETRY = 3;
    private IMraLogGroup logSource;

    private int defaultWidth = 800;
    private int defaultHeight = 600;

    private JLabel image = new JLabel();
    private JComboBox<?> cmapCombo = new JComboBox<Object>(ColorMap.cmaps);
    private JComboBox<String> entCombo = new JComboBox<String>();
    
    private JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);

    private JTextField cmapMinValue = new JTextField();
    private JTextField cmapMaxValue = new JTextField();
    private JPanel container = new JPanel(new MigLayout());
    
    private JButton redrawButton = new JButton("Redraw");
    private JButton savePng = new JButton(new AbstractAction("Save PNG") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            savePNG();
        }
    });
   private JButton savePdf = new JButton(new AbstractAction("Save PDF") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            savePDF();
        }
    });
    
    private DataDiscretizer dd = null;
    private DataPoint[] dps;
    
    private double scaleY;// = img.getWidth() / bounds.getWidth();
    private double scaleX;// = img.getHeight() / bounds.getHeight();
    
    private Vector<String> entityList = new Vector<String>();
    
    BufferedImage bufImage;
    String messageName;
    String varName;
    String curEntity = "ALL";
    
    boolean started = false;
    
    public ColorMapVisualization(MRAPanel panel, String fieldToPlot)  {
        this.logSource = panel.getSource();
        setLayout(new MigLayout());

        // Parse field to plot
        String part[] = fieldToPlot.split("\\.");
        messageName = part[0];
        varName = part[1];
        
        // Initialize entityList with the ALL shorthand
        entityList.add("ALL");
        
        // Misc setups and interface build
        redrawButton.addActionListener(this);
        entCombo.setModel(new DefaultComboBoxModel<String>(entityList));
        
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(cmapMinValue);
        toolbar.add(cmapMaxValue);
        toolbar.add(cmapCombo);
        toolbar.add(entCombo);
        toolbar.add(redrawButton);
        toolbar.add(savePng);
        toolbar.add(savePdf);
        
        add(toolbar, "w 100%, wrap");
        add(container, "w 100%, h 100%");
        
        cmapCombo.setSelectedItem(ColorMapFactory.createJetColorMap());
        cmapCombo.setRenderer(new ColorMapListRenderer());
        
        revalidate();		
    }
    
    public void redraw(final boolean parseNeeded, final String entityName) {
        AsyncTask task = new AsyncTask() {
            public Object run() throws Exception {
                try {
                    if(parseNeeded) {
                        dd = parseLog(entityName);
                        dps = dd.getDataPoints();
                    }
                    bufImage = buildColorMap(0);
                    return null;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

            }           
            public void finish() {
                try {
                    image.setIcon(new ImageIcon(bufImage));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    image.setText("Error creating colormap: "+e.getMessage());
                }
                container.removeAll();
                container.add(new JScrollPane(image), "w 100%, h 100%");
                revalidate();
            }       
        };
        AsyncWorker.getWorkerThread().postTask(task);
    }
    
    private void savePDF() {
        File f = new File(logSource.getFile("IMC.xml").getParent()+"/"+logSource.name()+ "-" + messageName +"." + varName + "." + curEntity + "-"+cmapCombo.getSelectedItem().toString()+".pdf");

        Rectangle pageSize = PageSize.A4.rotate();
        try {
            FileOutputStream out = new FileOutputStream(f);

            Document doc = new Document(pageSize);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();
            PdfContentByte cb = writer.getDirectContent();
            java.awt.Graphics2D g2 = cb.createGraphicsShapes(pageSize.getWidth(), pageSize.getHeight());
            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            int prevWidth = defaultWidth;
            int prevHeight = defaultHeight;

            defaultWidth = width;
            defaultHeight = height;

            BufferedImage img = bufImage;

            com.lowagie.text.Image pdfImage = com.lowagie.text.Image.getInstance(writer, img, 0.8f);
            pdfImage.setAbsolutePosition(0, 0);
            cb.addImage(pdfImage);

            double maxX = dd.maxX + 5;
            double maxY = dd.maxY + 5;
            double minX = dd.minX - 5;
            double minY = dd.minY - 5;

            //width/height
            double dx = maxX - minX;
            double dy = maxY - minY;

            double ratio1 = (double)defaultWidth/(double)defaultHeight;
            double ratio2 = dx/dy;

            if (ratio2 < ratio1)		
                dx = dy * ratio1;
            else
                dy = dx/ratio1;

            drawLegend(g2, (ColorMap)cmapCombo.getSelectedItem(), 0);			

            doc.close();		
            defaultWidth = prevWidth;
            defaultHeight = prevHeight;
            
            JOptionPane.showMessageDialog(this, "PDF saved to log directory");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePNG() {
        int prevWidth = defaultWidth;
        int prevHeight = defaultHeight;

        defaultWidth = 1024;
        defaultHeight = 768;		
        BufferedImage img = bufImage;
        File f = new File(logSource.getFile("IMC.xml").getParent()+"/"+logSource.name()+"-" + messageName +"." + varName + "." + curEntity + "-"+cmapCombo.getSelectedItem().toString()+".png");

        try {
            ImageIO.write(img, "png", f);
            JOptionPane.showMessageDialog(this, "PNG saved to log directory");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        defaultWidth = prevWidth;
        defaultHeight = prevHeight;
    }

    @Override
    
    public void actionPerformed(ActionEvent e) {
        if(((String)entCombo.getSelectedItem()).equals(curEntity)) {
            redraw(false, "");
        }
        else {
            curEntity = (String)entCombo.getSelectedItem();
            redraw(true, curEntity);
        }
    }

    private void drawPath(Graphics2D g, double scaleX, double scaleY, double minX, double minY, double timeStep) {

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setTransform(new AffineTransform());
        g.setColor(new Color(0,0,0,10));

        IMraLog stateParser = logSource.getLog("EstimatedState");
        IMCMessage stateEntry;
        Point2D lastPt = null;
        stateEntry = stateParser.nextLogEntry();

        while (stateEntry != null) {

            double north = stateEntry.getDouble("x");
            double east =  stateEntry.getDouble("y");
            Point2D pt = new Point2D.Double((east - minY) * scaleY, (-minX-north) * scaleX);

            if (timeStep == 0)
                g.setColor(new Color(0,0,0,20));
            else
                g.setColor(Color.black);

            if (lastPt != null && pt != null)				
                g.draw(new Line2D.Double(lastPt, pt));
            lastPt = pt;

            if (timeStep == 0)
                stateEntry = stateParser.nextLogEntry();
            else {
                stateParser.advance((long)(timeStep*1000));
                stateEntry = stateParser.getCurrentEntry();
            }
        }

    }

    private void drawLegend(Graphics2D g, ColorMap cmap, int var) {
        if (dd.maxVal == null)
            return;
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setTransform(new AffineTransform());

        g.setColor(new Color(255,255,255,100));
        g.fillRoundRect(10, 10, 100, 170, 10, 10);

        ColorBar cb = new ColorBar(ColorBar.VERTICAL_ORIENTATION, cmap);
        cb.setSize(15, 80);
        g.setColor(Color.black);
        Font prev = g.getFont();
        g.setFont(new Font("Helvetica", Font.BOLD, 14));
//        g.drawString(varNames[var], 15, 25);
        g.setFont(prev);

        g.translate(15, 45);

        cb.paint(g);

        g.translate(-10, -15);
        
        try {
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(dd.maxVal[var]), 28, 20);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format((dd.maxVal[var]+dd.minVal[var])/2), 28, 60);
            g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(dd.minVal[var]), 28, 100);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
        }
        g.translate(10, 120);

        g.drawLine(0, -3, 0, 3);
        g.drawLine(0, 0, 90, 0);
        g.drawLine(90, -3, 90, 3);

        //double meters = scaleX * 90;
        g.drawString(GuiUtils.getNeptusDecimalFormat(2).format(90d/scaleX) + " m", 25, 15);
    }

    private DataDiscretizer parseLog(String entity) {
        DataDiscretizer dd = new DataDiscretizer(10);

        IMraLog parser = logSource.getLog(messageName);
        IMraLog stateParser = logSource.getLog("EstimatedState");

        if (parser == null || stateParser == null) {
            return dd;
        }

        IMCMessage entry = parser.nextLogEntry();
        IMCMessage stateEntry;

        entityList.clear();
        entityList.add("ALL");
        while (entry != null) {
            parser.advance(1);
            entry = parser.getCurrentEntry();
            if (entry != null) {
                String entName = logSource.getEntityName(entry.getSrc(), entry.getSrcEnt());
            
                if(!entityList.contains(entName))
                    entityList.add(entName);
                
                if(!entity.equalsIgnoreCase("ALL") && !entName.equalsIgnoreCase(entity))
                    continue;
            
                stateEntry = stateParser.getEntryAtOrAfter(parser.currentTimeMillis());

                double[] vals = new double[4];

                vals[0] = Double.NaN;

                if (stateEntry != null) {
                    vals[0] = entry.getDouble(varName);
                    dd.addPoint(stateEntry.getDouble("y"), -stateEntry.getDouble("x"), vals);
                }
            }
        }
        entCombo.setModel(new DefaultComboBoxModel<>(entityList));
        
        return dd;
    }

    private BufferedImage buildColorMap(int var) {
        return buildColorMap(var, false);
    }

    private BufferedImage buildColorMap(int var, boolean clean) {
        double maxX = dd.maxX + 5;
        double maxY = dd.maxY + 5;
        double minX = dd.minX - 5;
        double minY = dd.minY - 5;
 
        //width/height
        double dx = maxX - minX;
        double dy = maxY - minY;

        double ratio1 = (double)defaultWidth/(double)defaultHeight;
        double ratio2 = dx/dy;

        if (ratio2 < ratio1)		
            dx = dy * ratio1;
        else
            dy = dx/ratio1;

        //center
        double cx = (maxX + minX)/2;
        double cy = (maxY + minY)/2;

        Rectangle2D bounds = new Rectangle2D.Double(cx-dx/2, cy-dy/2, dx, dy);

        BufferedImage img = new BufferedImage(defaultWidth,defaultHeight,BufferedImage.TYPE_INT_ARGB);
        System.out.println(img);

        try {
            ColorMapUtils.generateInterpolatedColorMap(bounds, dps, var, img.createGraphics(), img.getWidth(), img.getHeight(), 255, (ColorMap)cmapCombo.getSelectedItem(), dd.minVal[var]*0.995, dd.maxVal[var]*1.005);
        }
        catch (NullPointerException e) {
            System.out.println(bounds+","+
                    dps+","+
                    img+","+
                    cmapCombo);
        }

        scaleY = img.getWidth() / bounds.getWidth();
        scaleX = img.getHeight() / bounds.getHeight();
        minY = bounds.getMinX();
        minX = bounds.getMinY();
        Graphics2D g = (Graphics2D) img.getGraphics();

        if (!clean) {
            drawPath(g, scaleX, scaleY, minX, minY, 0);
            drawLegend(g, (ColorMap)cmapCombo.getSelectedItem(), var);
        }
        return img;

    }

    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        redraw(true, curEntity); // At this time curEnity is "ALL" which means to accept all entities
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("Conductivity") != null && source.getLog("EstimatedState") != null;       
    }

    @Override
    public String getName() {
        return I18n.textf("%1 Colormap", messageName + "." + varName);
    }

    @Override
    public ImageIcon getIcon() {
        return new ImageIcon(ImageUtils.getScaledImage("images/buttons/colormap.png", 16, 16));
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    public Type getType() {
        return Type.VISUALIZATION;
    }

    public void onCleanup() {
        removeAll();
    }
    
    @Override
    public void onHide() {
        // TODO Auto-generated method stub
        
    }
    
    public void onShow() {
        //nothing
    }

}
