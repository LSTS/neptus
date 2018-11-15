/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel Ribeiro
 * 12/11/2018
 */
package pt.lsts.neptus.plugins.djiimporter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Manuel Ribeiro
 *
 */
@PluginDescription(name = "DJIPhotoImporter", icon="pt/lsts/neptus/plugins/djiimporter/blueled.png")
public class PhotoImporter extends SimpleRendererInteraction {

    private static final long serialVersionUID = 1L;
    private Map<String, ArrayList<ImageMetadata>> list = Collections.synchronizedMap(new HashMap<String, ArrayList<ImageMetadata>>());
    private Map<String, Color> colorList = Collections.synchronizedMap(new HashMap<String, Color>());
    private Set<String> datesSelected = new HashSet<String>();
    private Map<String, Point> datesList = new HashMap<String, Point>();
    private ImageMetadata selected = null;
    private boolean enable = false;
    private String prevDir = "";
    private TableDialog tableDialog = null;
    private StateRenderer2D renderer = null;

    public PhotoImporter(ConsoleLayout console) {
        super(console);
    }

    private void addPhotoMarker(String file) {
        Metadata metadata = null;
        File jpegFile = new File(file);
 

        try {
            metadata = ImageMetadataReader.readMetadata(jpegFile);
        }
        catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
        
        // obtain the Make/Model
        ExifIFD0Directory exifDir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        String make = exifDir.getString( ExifIFD0Directory.TAG_MAKE);
        String model = exifDir.getString( ExifIFD0Directory.TAG_MODEL);
        if (!make.equals("DJI"))
            return;
        
        ImageMetadata imgMD = new ImageMetadata();
        imgMD.setMake(make);
        imgMD.setModel(model);
        
        //set Path to actual file
        imgMD.setPath(file);

        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        // obtain the Name
        String name = jpegFile.getName();
        imgMD.setName(name);

        // obtain the Width / Height
        JpegDirectory jpgDir = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        int width = -1;
        int height = -1;
        try {
            width = jpgDir.getImageWidth();
            height = jpgDir.getImageHeight();
        }
        catch (MetadataException e1) {
            e1.printStackTrace();
        }
        imgMD.setImgWidth(width);
        imgMD.setImgHeight(height);

        // obtain the Date directory
        Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
        imgMD.setDate(date);

        // obtain the GPS directory
        GpsDirectory GpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        GeoLocation geogps = GpsDirectory.getGeoLocation();
        double lat = geogps.getLatitude();
        double lon = geogps.getLongitude();
        imgMD.setLocation(new LocationType(lat, lon));

        for (XmpDirectory xmpDirectory : metadata.getDirectoriesOfType(XmpDirectory.class)) {

            XMPMeta xmpMeta = xmpDirectory.getXMPMeta();
            XMPIterator itr = null;
            try {
                itr = xmpMeta.iterator();
            }
            catch (XMPException e) {
                e.printStackTrace();
            }

            while (itr.hasNext()) {
                XMPPropertyInfo property = (XMPPropertyInfo) itr.next();
                if (property.getPath() != null) {
                    if (property.getPath().equals("drone-dji:RelativeAltitude"))
                        imgMD.setAGL(Double.parseDouble(property.getValue()));
                    if (property.getPath().equals("drone-dji:AbsoluteAltitude"))
                        imgMD.setMSL(Double.parseDouble(property.getValue()));
                }
            }
        }
        synchronized (list) {
            String dateStr = getYearMonthDay(date);
            if (list.containsKey(dateStr))
                list.get(dateStr).add(imgMD);
            else {
                ArrayList<ImageMetadata> imageList = new ArrayList<>();
                imageList.add(imgMD);
                list.put(dateStr, imageList);

                Random random = new Random();
                final float hue = random.nextFloat();
                final float saturation = 1.0f;//1.0 for brilliant, 0.0 for dull
                final float luminance = 1.0f; //1.0 for brighter, 0.0 for black
                Color c = Color.getHSBColor(hue, saturation, luminance);

                if (!colorList.containsKey(dateStr)) {
                    colorList.put(dateStr, c);
                }
            }
        }
    }

    private static String getYearMonthDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR) + "-"+ calendar.get(Calendar.MONTH) + "-" +  calendar.get(Calendar.DAY_OF_MONTH);
    }

    @NeptusMenuItem("Tools>DJI>Load Photos")
    public void importPhotosAction() {
        JFileChooser selPhotos = GuiUtils.getFileChooser(prevDir);
        selPhotos.setMultiSelectionEnabled(true);
        selPhotos.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("Image files"), "PNG", "png", "JPG", "jpg"));
        int res = selPhotos.showOpenDialog(ConfigFetch.getSuperParentFrame());

        if (res == JFileChooser.APPROVE_OPTION) {
            File[] files = selPhotos.getSelectedFiles();
            prevDir = selPhotos.getSelectedFile().getPath();
            for (File f : files) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        addPhotoMarker(f.getAbsolutePath());
                        return null;
                    }
                };
                worker.execute();
            }
            synchronized (list) {
                if (!list.isEmpty())
                    setActive(true, null);
            }
        }
    }

    @NeptusMenuItem("Tools>DJI>Clear all")
    public void clearPhotosAction() {
        synchronized (list) {
            list.clear();
            colorList.clear();
            datesSelected.clear();
            datesList.clear();
            tableDialog.dispose();
        }
    }

    @NeptusMenuItem("Tools>DJI>View Table")
    public void viewTableAction() {
        if (list.isEmpty()) {
            GuiUtils.errorMessage(I18n.text("Error"), I18n.text("No data to display"));
            return;
        }
        if (tableDialog == null) {
            tableDialog = new TableDialog(SwingUtilities.windowForComponent(getConsole()), list);
            tableDialog.setVisible(true);
        }
        else {
            tableDialog.dispose();
            tableDialog = new TableDialog(SwingUtilities.windowForComponent(getConsole()), list);
            tableDialog.setVisible(true);
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        this.renderer = renderer;
        if (!enable)
            return;

        synchronized (list) {
            for (Entry<String, ArrayList<ImageMetadata>> e : list.entrySet())
                for (ImageMetadata img : e.getValue()) {
                    if (datesSelected.contains(getYearMonthDay(img.getDate())))
                        paintImgIcon(img, g, renderer);
                }

            if (!list.isEmpty())
                printMenu(g, renderer);
        }

        ImageMetadata sel = selected;
        if (sel != null)
            paintImgDetails(sel, g, renderer);
    }

    private void printMenu(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRect(20, 20, 100, colorList.size()*20);
        int x = 55;
        int y = 35;

        SortedSet<String> keys = new TreeSet<String>(colorList.keySet());
        for (String date : keys) {
            if (datesSelected.contains(date)) {
                g.setColor(Color.BLACK);
                //Polygon poly = new Polygon(new int[] { 5, 10, 0 }, new int[] { 0, 10, 10 }, 3);
                Polygon poly = new Polygon(new int[] { 8, 8, 16 }, new int[] { 0, 10, 5 }, 3);
                g.translate(16, y-10);
                g.fill(poly);
                g.translate(-16, -(y-10));
            }

            g.setColor(colorList.get(date));
            g.fill(new Ellipse2D.Double(x-20, y-11, 12, 12));
            if (!datesList.containsKey(date))
                datesList.put(date, new Point(x-15, y));
            g.setColor(Color.BLACK);
            g.drawString(date, x, y);
            y+=20;
        }
    }

    public void paintImgIcon(ImageMetadata i, Graphics2D g, StateRenderer2D renderer) {
        Point2D pt = renderer.getScreenPosition(new LocationType(i.getLocation().getLatitudeDegs(), i.getLocation().getLongitudeDegs()));

        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(pt.getX()-9, pt.getY()-9, 18, 18));

        if (selected == i)
            g.setColor(new Color(35, 253, 0));
        else
            g.setColor(colorList.get(getYearMonthDay(i.getDate())));

        g.fill(new Ellipse2D.Double(pt.getX() - 8, pt.getY() - 8, 16, 16));
    }

    public void paintImgDetails(ImageMetadata i, Graphics2D g, StateRenderer2D renderer) {
        Point2D pt = renderer.getScreenPosition(new LocationType(i.getLocation().getLatitudeDegs(), i.getLocation().getLongitudeDegs()));

        StringBuilder html = new StringBuilder("<html>");
        DateFormat formatterUTC = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC")); // UTC timezone
        
        html.append("<b>" + i.getName() + "</b><br>" + formatterUTC.format(i.getDate()));
        html.append("</html>");

        JLabel lblHtml = new JLabel(html.toString());
        lblHtml.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        Dimension dim = lblHtml.getPreferredSize();
        lblHtml.setSize(dim);
        lblHtml.setOpaque(true);
        lblHtml.setBackground(new Color(255, 255, 255, 240));

        g.translate((int) pt.getX()+10, (int) pt.getY()-20);
        lblHtml.paint(g); 
    }

    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        synchronized (list) {
            for (Entry<String, ArrayList<ImageMetadata>> e : list.entrySet()) {
                for (ImageMetadata img : e.getValue()) {
                    Point2D pt = source.getScreenPosition(new LocationType(img.getLocation().getLatitudeDegs(), img.getLocation().getLongitudeDegs()));
                    if (pt.distance(event.getPoint()) < 5) {
                        selected = img;
                        return;
                    }
                }
            }
            selected = null;
        }
    }

    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        final ImageMetadata sel = selected;
        if (event.getClickCount() == 2) {
            for (Entry<String, Point> e : datesList.entrySet()) {
                if (e.getValue().distance(event.getPoint()) <= 9) {
                    if (datesSelected.contains(e.getKey()))
                        datesSelected.remove(e.getKey());
                    else
                        datesSelected.add(e.getKey());
                    return;
                }
            }

            if (sel != null) {
                DetailsDialog details = new DetailsDialog(SwingUtilities.windowForComponent(getConsole()), sel);

                if (details != null)
                    details.setVisible(true);
            }
        }
    }


    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        enable = mode;
    }

    private class DetailsDialog extends JDialog {

        private static final long serialVersionUID = 1L;
        private final JPanel contentPanel = new JPanel();

        public DetailsDialog(Window parent, ImageMetadata imgMetaData) {
            super(parent, ModalityType.MODELESS);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            DateFormat formatterUTC = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC")); // UTC timezone
            
            
            setTitle(imgMetaData.getName() + " - " + formatterUTC.format(imgMetaData.getDate()));
            setType(Type.POPUP);
            setResizable(false);
            setLocationRelativeTo(ConfigFetch.getSuperParentAsFrame());
            setBounds(100, 100, 405, 510);
            getContentPane().setLayout(new BorderLayout());
            contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            getContentPane().add(contentPanel, BorderLayout.CENTER);
            contentPanel.setLayout(new BorderLayout(0, 0));

            JLabel lblImage = new JLabel();
            lblImage.setSize(400, 300);
            BufferedImage img = null;
            try {
                img = ImageIO.read(new File(imgMetaData.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Image dimg = img.getScaledInstance(lblImage.getWidth(), lblImage.getHeight(),
                    Image.SCALE_SMOOTH);

            ImageIcon imageIcon = new ImageIcon(dimg);
            lblImage.setIcon(imageIcon);
            contentPanel.add(lblImage);

            JPanel infoPanel = new JPanel();
            contentPanel.add(infoPanel, BorderLayout.SOUTH);
            infoPanel.setLayout(new MigLayout("", "[50px][214px]", "[14px][14px][14px][14px][14px]"));

            JLabel lblName = new JLabel(I18n.text("Name:"));
            lblName.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblNameValue = new JLabel(imgMetaData.getName());
            infoPanel.add(lblName, "cell 0 0,grow");
            infoPanel.add(lblNameValue, "cell 1 0, grow");

            JLabel lblMake = new JLabel(I18n.text("Make:"));
            lblMake.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblMakeValue = new JLabel(imgMetaData.getMake());
            infoPanel.add(lblMake, "cell 0 1,grow");
            infoPanel.add(lblMakeValue, "cell 1 1, grow");

            JLabel lblModel = new JLabel(I18n.text("Model:"));
            lblModel.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblModelValue = new JLabel(imgMetaData.getModel());
            infoPanel.add(lblModel, "cell 0 2,grow");
            infoPanel.add(lblModelValue, "cell 1 2,grow");

            JLabel lblDate = new JLabel(I18n.text("Date:"));
            lblDate.setFont(new Font("Tahoma", Font.BOLD, 11));
   
            JLabel lblDateValue = new JLabel(formatterUTC.format(imgMetaData.getDate()));
            infoPanel.add(lblDate, "cell 0 3,grow");
            infoPanel.add(lblDateValue, "cell 1 3,grow");

            JLabel lblLocation = new JLabel(I18n.text("Location:"));
            lblLocation.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblLocationValue = new JLabel(imgMetaData.getLocation().toString());
            infoPanel.add(lblLocation, "cell 0 4,grow");
            infoPanel.add(lblLocationValue, "cell 1 4,grow");

            JLabel lblAlt = new JLabel(I18n.text("Altitude:"));
            lblAlt.setToolTipText(I18n.text("GPS Altitude"));
            lblAlt.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblAltValue = new JLabel(imgMetaData.getMSL()+"m");
            infoPanel.add(lblAlt, "cell 0 5,grow");
            infoPanel.add(lblAltValue, "cell 1 5,grow");

            JLabel lblRelAlt = new JLabel(I18n.text("AGL:"));
            lblRelAlt.setToolTipText(I18n.text("Altitude relative to ground"));
            lblRelAlt.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblRelAltValue = new JLabel(imgMetaData.getAGL()+"m");
            infoPanel.add(lblRelAlt, "cell 0 6,grow");
            infoPanel.add(lblRelAltValue, "cell 1 6,grow");

            JLabel lblResolution = new JLabel(I18n.text("Resolution:"));
            lblResolution.setFont(new Font("Tahoma", Font.BOLD, 11));
            JLabel lblResolutionValue = new JLabel(imgMetaData.getImgWidth()+"x"+imgMetaData.getImgHeight());
            infoPanel.add(lblResolution, "cell 0 7,grow");
            infoPanel.add(lblResolutionValue, "cell 1 7,grow");

        }
    }
    public static void main(String[] args) {

    }
    
    private class TableDialog extends JDialog {

        private static final long serialVersionUID = 1L;
        private final JPanel contentPanel = new JPanel();
        private JXTable table;
        private MetadataModel model;

        public TableDialog(Window parent, Map<String, ArrayList<ImageMetadata>> list) {
            super(parent, ModalityType.MODELESS);
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setTitle(I18n.text("Photos List"));
            setType(Type.POPUP);
            setBounds(100, 100, 850, 300);
            setLocationRelativeTo(ConfigFetch.getSuperParentAsFrame());
            getContentPane().setLayout(new BorderLayout());
            contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            getContentPane().add(contentPanel, BorderLayout.CENTER);
            contentPanel.setLayout(new BorderLayout(0, 0));
            ArrayList<ImageMetadata> dataArray = new ArrayList<>();
            for (Entry<String, ArrayList<ImageMetadata>> e : list.entrySet()) {
                for (ImageMetadata img : e.getValue()) {
                    dataArray.add(img);
                }
            }
            model = new MetadataModel(dataArray);
            table = new JXTable();
            table.setModel(model);
            contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);
            
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

            //set all columns to be centered
            for (int i=0 ; i< model.columnNames.size(); i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
            
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    super.mouseClicked(event);
                    
                    if (event.getClickCount() == 2) {
                        ImageMetadata imgMD = dataArray.get(table.getSelectedRow());
                        if (imgMD != null) {
                            DetailsDialog details = new DetailsDialog(SwingUtilities.windowForComponent(getConsole()), imgMD);
                            details.setVisible(true);
                        }
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    int r = table.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < table.getRowCount()) {
                        table.setRowSelectionInterval(r, r);
                    } else {
                        table.clearSelection();
                    }

                    int rowindex = table.getSelectedRow();
                    if (rowindex < 0)
                        return;
                    if (e.isPopupTrigger() && e.getComponent() instanceof JTable ) {
                        JPopupMenu popup = new JPopupMenu();
                        JMenuItem menu = new JMenuItem(I18n.text("Show on Map"));
                        ActionListener l = new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (renderer != null) {
                                    enable = true;
                                    ImageMetadata imgMD = dataArray.get(table.getSelectedRow());
                                    renderer.focusLocation(imgMD.getLocation());
                                }
                            }
                        };
                        menu.addActionListener(l);
                        popup.add(menu);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }
    }
}
