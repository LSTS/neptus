/*
 * Copyright 2022 OceanScan - Marine Systems & Technology, Lda.
 *
 * This file is subject to the terms and conditions defined in file
 * 'LICENCE.md', which is part of this source code package.
 *
 * Author: José Pinto
*/
package pt.omst.neptus.plugins.reports;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.AssetReport;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

@PluginDescription(name = "Asset Reports Panel", author = "Jose Pinto")
public class AssetReportsPanel extends ConsolePanel implements Renderer2DPainter {
    @NeptusProperty(description = "Speak up incoming reports")
    private static boolean useAudioAlerts = false;

    @NeptusProperty(name = "Report caching duration", description = "Minutes for how long to store reports.")
    private static int cacheMinutes = 15;

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Set<AssetReport> reports = new TreeSet<>(new Comparator<AssetReport>() {
        @Override
        public int compare(AssetReport o1, AssetReport o2) {
            int diff = o1.getMediumStr().compareTo(o2.getMediumStr());
            if (diff == 0)
                return Double.valueOf(o1.getReportTime()).compareTo(o2.getReportTime());
            else
                return diff;
        }
    });

    private Map<String, AssetReport> lastReports = new LinkedHashMap<>();

    public AssetReportsPanel(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
        reports.clear();
        lastReports.clear();
    }
    
    @Periodic(millisBetweenUpdates = 10_000)
    void removeOldReports() {
        double oldestTimestamp = (System.currentTimeMillis() / 1000.0) - (cacheMinutes * 60);
        reports.removeIf(r -> r.getReportTime() < oldestTimestamp);        
    }

    @Subscribe
    public void on(AssetReport report) {
        String text = report.getMediumStr().toLowerCase() + " report";
        text = text.replaceAll("sms", "gsm");
        LocationType position = new LocationType(Math.toDegrees(report.getLat()), Math.toDegrees(report.getLon()));
        position.setDepth(report.getDepth());        
        String notificationText = "Received " + text + " from " + report.getName()+" sent " +
                (int) getReportAgeInSeconds(report) + " seconds ago.";
        if (getReportAgeInSeconds(report) > 60) {
            notificationText = "Received " + text + " from " + report.getName() + " sent at "
                    + sdf.format(new Date((long) (1000.0 * report.getReportTime())));
        }

        getConsole().post(Notification.info(text + " from " + report.getName(), notificationText));
       
        text = text.toLowerCase();
        boolean newReport = reports.add(report);
        if (newReport && useAudioAlerts) {
            SpeechUtil.readSimpleText(text.replaceAll("wifi", "wyi-fi"));
        }
        lastReports.put(report.getName(), report);
    }

    private double getReportAgeInSeconds(AssetReport report) {
        return (System.currentTimeMillis() / 1000.0) - report.getReportTime();
    }

    private Color getColor(AssetReport report) {
        double ageInSeconds = getReportAgeInSeconds(report);
        double ageAlpha = ((60 * cacheMinutes) - ageInSeconds) / (60 * cacheMinutes);
        MathMiscUtils.clamp(ageAlpha, 0, 1);
        ageAlpha = (int) (ageAlpha * 128) + 127;
        switch (report.getMedium()) {
            case ACOUSTIC:
                return new Color(255, 0, 0, (int) ageAlpha);
            case SATELLITE:
                return new Color(255, 0, 255, (int) ageAlpha);
            case SMS:
                return new Color(0, 0, 255, (int) ageAlpha);
            default:
                return new Color(64, 64, 64, (int) ageAlpha);
        }
    }

    private String asShortString(AssetReport report) {
        String str = sdf.format(new Date((long) (1000.0 * report.getReportTime())));
        switch (report.getMedium()) {
            case ACOUSTIC:
                str += " (ac)";
                break;
            case SATELLITE:
                str += " (sat)";
                break;
            case SMS:
                str += " (gsm)";
                break;
            case WIFI:
                str += " (wifi)";
                break;
            default:
                break;
        }
        return str;
    }

    private void paintReport(AssetReport r, Graphics2D g, StateRenderer2D renderer, boolean highlight) {
        LocationType position = new LocationType(Math.toDegrees(r.getLat()), Math.toDegrees(r.getLon()));
        Point2D pt = renderer.getScreenPosition(position);
        String str = asShortString(r);

        Font t = g.getFont();

        if (highlight) {
            g.setFont(new Font(t.getName(), Font.BOLD, (int) (t.getSize() * 0.85)));
            g.setColor(Color.black);  
        } else {
            g.setFont(new Font(t.getName(), Font.PLAIN, (int) (t.getSize() * 0.85)));
            g.setColor(Color.gray.darker());
        }

        g.drawString(r.getName(), (int) pt.getX() + 16, (int) pt.getY() + 15 * 2);
        g.drawString(str, (int) pt.getX() + 16, (int) pt.getY() + 15 * 3);

        if (highlight) {
            g.setColor(getColor(r));
        } else {
            g.setColor(getColor(r).darker().darker());
        }
        g.fillRect((int) pt.getX() - 2, (int) pt.getY() - 2, 4, 4);
        g.drawString(str, (int) pt.getX() + 15, (int) pt.getY() + 15 * 3 - 1);
        g.setFont(new Font(t.getName(), Font.PLAIN, t.getSize()));        
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        List<AssetReport> existing = new ArrayList<>();
        existing.addAll(reports);
        existing.removeAll(lastReports.values());

        existing.forEach(r -> {
            paintReport(r, g, renderer, false);
        });

        for (Entry<String, AssetReport> lastReport : lastReports.entrySet()) {
            paintReport(lastReport.getValue(), g, renderer, true);
        }
    }
}
