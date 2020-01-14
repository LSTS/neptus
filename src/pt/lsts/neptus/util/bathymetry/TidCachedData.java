/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 04/12/2015
 */
package pt.lsts.neptus.util.bathymetry;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TreeSet;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.tid.TidReader;
import pt.lsts.neptus.util.tid.TidReader.Data;
import pt.lsts.neptus.util.tid.TidWriter;

/**
 * @author Paulo Dias
 *
 */
public class TidCachedData extends CachedData {

    /**
     * Loads the Tide file.
     * @param file
     */
    public TidCachedData(File file) {
        try {
            loadFile(file);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // NeptusLog.pub().info("Loading finished");
        loading = false;
    }
    
    @Override
    public void loadFile(File f) throws Exception {
        cachedData = new TreeSet<>();
        if (f == null || ! f.canRead()) {
            NeptusLog.pub().error(new Exception("Tides file is not valid: "+f));
            return;
        }
        
        BufferedReader br = new BufferedReader(new FileReader(f));
        TidReader tidReader = new TidReader(br);
        
        Data data = tidReader.readData();

        while (data != null) {
            long unixTimeMillis = data.timeMillis;
            double height = data.height;
            Date d = new Date(unixTimeMillis);
            cachedData.add(new TidePeak(d, height));
            data = tidReader.readData();
        }
        
        name = tidReader.getHarbor();
        br.close();
    }

    @Override
    public void saveFile(String port, File f) throws Exception {
        if (!f.getParentFile().exists())
            f.getParentFile().mkdirs();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        TidWriter tidWriter = new TidWriter(writer, 2);
        if (port == null || port.isEmpty())
            tidWriter.writeHeader("Tides Data", name);
        else
            tidWriter.writeHeader("Tides Data", port);
        for (TidePeak tp : cachedData) {
            tidWriter.writeData(tp.date.getTime(), tp.height);
        }
        writer.close();
    }

    /**
     * @param portName
     * @return
     */
    @Override
    protected File getFileToSave(String portName) {
        return new File(ConfigFetch.getConfFolder() + "/tides/" + portName + ".tid");
    }

    @Override
    public Date fetchData(String portName, Date aroundDate) throws Exception {
        return super.fetchData(portName, aroundDate);
    }
    
    public static void convertTideTxtIntoTid() {
        String path = ConfigFetch.getConfFolder() + "/tides";
        File tidesFolder = new File(path);
        File[] txtFiles = tidesFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return FileUtil.checkFileForExtensions(name, "txt") != null;
            }
        });
        
        for (File txtFx : txtFiles) {
            CachedData txtData = new CachedData(txtFx);

            String tidFP = FileUtil.replaceFileExtension(txtFx.getAbsolutePath(), "tid");
            File tidFx = new File(tidFP);
            TidCachedData tidData = new TidCachedData(tidFx);
            tidData.update(txtData.getTidePeaks());
            try {
                tidData.saveFile(txtData.getName(), tidFx);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void test(String[] args) throws Exception {
        TidReader.main(args);

        JFreeChart timeSeriesChart = null;
        TimeSeriesCollection tsc = new TimeSeriesCollection();
        ValueMarker marker = new ValueMarker(System.currentTimeMillis());
        ValueMarker levelMarker = new ValueMarker(0);

        String tmpFolder = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator", "/");
        File tidFx = new File(tmpFolder + "tmp.tid");
        
        TidCachedData tide = new TidCachedData(tidFx);

        TimeSeries ts = new TimeSeries(I18n.text("Tide level"));
        tsc.addSeries(ts);

        Date sDate = new GregorianCalendar(1993, 9, 28).getTime();
        for (double i = -12; i < 12; i+= 0.25) {
            Date d = new Date((long) (sDate.getTime() + (i * 6.45 * 1E3 * 60 * 60)));
            ts.addOrUpdate(new Millisecond(d), tide.getTidePrediction(d, false));
        }

        JPanel panel = new JPanel(new BorderLayout());
        timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        panel.add(new ChartPanel(timeSeriesChart), BorderLayout.CENTER);

        timeSeriesChart.getXYPlot().addDomainMarker(marker);
        levelMarker.setValue(TidePredictionFactory.getTideLevel(new Date()));
        timeSeriesChart.getXYPlot().addRangeMarker(levelMarker);
        
        GuiUtils.testFrame(panel);
        
        System.out.println("\n________________________________________");
        long start = System.currentTimeMillis();
        TidCachedData tcd = new TidCachedData(new File(ConfigFetch.getConfFolder() + "mra/Leixoes.tid"));
        System.out.println("Loading of " + tcd.getName() + " took "
                + MathMiscUtils.parseToEngineeringNotation((System.currentTimeMillis() - start) / 1E3, 2) + "s");
    }
    
    public static void main(String[] args) throws Exception {
        // convertTideTxtIntoTid();

        test(args);
    }
}
