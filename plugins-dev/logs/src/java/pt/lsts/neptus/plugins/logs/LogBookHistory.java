/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 19, 2013
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import com.kitfox.svg.SVGDiagram;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.logs.HistoryMessage.msg_type;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageSvgUtilsFast;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 * 
 */
public class LogBookHistory extends AbstractListModel<HistoryMessage> implements ListCellRenderer<HistoryMessage> {

    private static final long serialVersionUID = 2382030731540409061L;

    private static final int sizeIcon = 20;

    static final Color criticalFgColor = new Color(255, 255, 128);;

    private static final Color criticalColor = Color.black;
    private static final Color criticalOnBackgroundColor = new Color(112, 112, 112, 255);
    private static final Color errorColor = new Color(255, 128, 128);
    private static final Color errorOnBackgroundColor = new Color(157, 42, 42);;
    private static final Color warningColor = new Color(255, 255, 128);
    private static final Color warningOnBackgroundColor = new Color(114, 114, 15);;
    private static final Color infoColor = new Color(200, 255, 200);
    private static final Color infoOnBackgroundColor = new Color(92, 138, 32, 255);;
    private static final Color debugColor = new Color(217, 217, 217);
    private static final Color debugOnBackgroundColor = new Color(83, 83, 83);;

    private static final String borderUpImagePath = "svg/border_up_down_48.svg";
    private static final String criticalImagePath = "svg/cancel_48.svg";
    private static final String errorImagePath = "svg/report_48.svg";
    private static final String warningImagePath = "svg/warning_48.svg";
    private static final String infoImagePath = "svg/info_48.svg";
    private static final String debugImagePath = "svg/bug_report_48.svg";
    private static final String unknownImagePath = "svg/question_mark_48.svg";

    protected LinkedList<HistoryMessage> messages = new LinkedList<>();
    protected String sysname;
    protected int maxSize = 250;
    
    static final LinkedHashMap<HistoryMessage.msg_type, Color> bgColors = new LinkedHashMap<>();
    static {
        bgColors.put(msg_type.critical, criticalColor);
        bgColors.put(msg_type.error, errorColor);
        bgColors.put(msg_type.warning, warningColor);
        bgColors.put(msg_type.info, infoColor);
        bgColors.put(msg_type.debug, debugColor);
    }
    static final LinkedHashMap<HistoryMessage.msg_type, Color> onBgColors = new LinkedHashMap<>();
    static {
        onBgColors.put(msg_type.critical, criticalOnBackgroundColor);
        onBgColors.put(msg_type.error, errorOnBackgroundColor);
        onBgColors.put(msg_type.warning, warningOnBackgroundColor);
        onBgColors.put(msg_type.info, infoOnBackgroundColor);
        onBgColors.put(msg_type.debug, debugOnBackgroundColor);
    }

    static final ImageIcon criticalImageIcon;
    static final ImageIcon errorImageIcon;
    static final ImageIcon warningImageIcon;
    static final ImageIcon infoImageIcon;
    static final ImageIcon debugImageIcon;
    static final ImageIcon unknownImageIcon;
    static {
        List<SVGDiagram> svgs = new ArrayList<>();

        AtomicReference<Color> fillOnBgColorRef = new AtomicReference<>(Color.black);
        BiFunction<Graphics2D, Integer, Void> graphicsModifier = (g, i) -> {
            SVGDiagram s = svgs.get(i);
            String fillColorStr = String.format("#%02X%02X%02X", fillOnBgColorRef.get().getRed(),
                    fillOnBgColorRef.get().getGreen(), fillOnBgColorRef.get().getBlue()) ;//"#000000";
            if (i == 0) {
                g.setColor(fillOnBgColorRef.get());
            }
            else if (i == 1) {
                fillColorStr = "#FEFEFE";
                g.translate(sizeIcon * 0.1, sizeIcon * 0.1);
                g.scale(0.9, 0.9);
            }
            ImageSvgUtilsFast.fillSvgElementAttribute(s.getRoot(), "fill", fillColorStr, false);
            ImageSvgUtilsFast.fillSvgElementAttribute(s.getRoot(), "fill-opacity", "1.0", false);
            return null;
        };

        SVGDiagram svgBorderUp = ImageSvgUtilsFast.getSvgImage(borderUpImagePath);

        fillOnBgColorRef.set(onBgColors.get(msg_type.critical));
        BufferedImage imageCritical = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(criticalImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageCritical, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        criticalImageIcon = new ImageIcon(imageCritical);

        fillOnBgColorRef.set(onBgColors.get(msg_type.error));
        BufferedImage imageError = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.clear();
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(errorImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageError, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        errorImageIcon = new ImageIcon(imageError);

        fillOnBgColorRef.set(onBgColors.get(msg_type.warning));
        BufferedImage imageWarning = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.clear();
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(warningImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageWarning, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        warningImageIcon = new ImageIcon(imageWarning);

        fillOnBgColorRef.set(onBgColors.get(msg_type.info));
        BufferedImage imageZoomIn = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.clear();
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(infoImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageZoomIn, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        infoImageIcon = new ImageIcon(imageZoomIn);

        fillOnBgColorRef.set(onBgColors.get(msg_type.debug));
        BufferedImage imageDebug = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.clear();
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(debugImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageDebug, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        debugImageIcon = new ImageIcon(imageDebug);

        fillOnBgColorRef.set(onBgColors.get(msg_type.debug));
        BufferedImage imageUnknown = ImageUtils.createCompatibleImage(sizeIcon, sizeIcon, Transparency.TRANSLUCENT);
        svgs.clear();
        svgs.add(svgBorderUp);
        svgs.add(ImageSvgUtilsFast.getSvgImage(unknownImagePath));
        ImageSvgUtilsFast.paintSvgImageToBufferedImage(imageUnknown, true, graphicsModifier, svgs.toArray(new SVGDiagram[0]));
        unknownImageIcon = new ImageIcon(imageUnknown);
    }

    public LogBookHistory(String sysname) {
        this.sysname = sysname;
    }

    public void add(HistoryMessage msg) {
        if (!messages.contains(msg)) {
            messages.add(msg);
            Collections.sort(messages);
            int idx = Collections.binarySearch(messages, msg);
            fireIntervalAdded(this, idx, idx);
        }
        if (getSize() > maxSize) {
            messages.removeFirst();
            fireIntervalRemoved(this, 0, 0);
        }
    }
    
    public void clear() {
        int size = getSize();
        messages.clear();
        fireIntervalRemoved(this, 0, size);
    }
    
    public Collection<HistoryMessage> add(Collection<HistoryMessage> msgs) {
        
        Vector<HistoryMessage> notExisting = new Vector<>();
        
        for (HistoryMessage msg : msgs) {
            if (!messages.contains(msg)) {
                messages.add(msg);
                notExisting.add(msg);
            }
        }
        
        if (notExisting.isEmpty())
            return notExisting;
        
        Collections.sort(messages);
        
        while (getSize() > maxSize) {
            messages.removeFirst();
        }
        
        fireContentsChanged(this, 0, getSize());
        Collections.sort(notExisting);
        return notExisting;
        
    }

    public long lastMessageTimestamp() {
        return messages.getLast().timestamp;
    }

    @Override
    public HistoryMessage getElementAt(int index) {
        return messages.get(index);
    }

    @Override
    public int getSize() {
        return messages.size();
    }

    private JLabel l = new JLabel("", JLabel.LEFT);
    private Color lFg = l.getForeground();
    @Override
    public Component getListCellRendererComponent(JList<? extends HistoryMessage> list, HistoryMessage value,
            int index, boolean isSelected, boolean cellHasFocus) {
        l.setIcon(getIcon(value.type));
        l.setText(value.toString());
        l.setToolTipText(I18n.textf("Received on %timeStamp (%context)", new Date(value.timestamp), value.context));
        l.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 3));
        l.setOpaque(true);
        l.setBackground(bgColors.get(value.type));
        l.setForeground(lFg);
        if (value.type == msg_type.critical)
            l.setForeground(criticalFgColor);
        return l;
    }

    public ImageIcon getIcon(HistoryMessage.msg_type type) {
        switch (type) {
            case info:
                return infoImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/info.png");
            case warning:
                return warningImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/warning.png");
            case error:
                return errorImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/error.png");
            case critical:
                return criticalImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/error.png");
	        case debug:
                return debugImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/unknown.png");
            default:
                return unknownImageIcon; //ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/queue2.png");
        }
    }

    public static void main(String[] args) throws Exception {
        LogBookHistory hist = new LogBookHistory("lauv");
        JList<HistoryMessage> panel = new JList<>(hist);
        panel.setCellRenderer(hist);
        GuiUtils.testFrame(new JScrollPane(panel));
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(30);
            
            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste1", "ctx", true, HistoryMessage.msg_type.error));
            Thread.sleep(30);
            
            hist.add(new HistoryMessage(System.currentTimeMillis(), "teste2", "ctx", true, HistoryMessage.msg_type.info));
            Thread.sleep(30);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.critical));
            Thread.sleep(30);
            hist.add(new HistoryMessage(System.currentTimeMillis()-5000, "teste3", "ctx", true, HistoryMessage.msg_type.warning));
        }
    }

}
