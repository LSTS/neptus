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
 * Author: zp
 * Oct 10, 2020
 */
package pt.lsts.neptus.mra.api;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 *
 */
public class SidescanHistogramNormalizer implements Serializable {
    
    public static SidescanParameters HISTOGRAM_DEFAULT_PARAMATERS = new SidescanParameters(0.1, 255);
    private static final long serialVersionUID = -6926787167196556272L;
    private static final int LINES_TO_COMPUTE_HISTOGRAM = 1000;

    private LinkedHashMap<Integer, float[]> histograms = new LinkedHashMap<Integer, float[]>();
    private LinkedHashMap<Integer, Float> averages = new LinkedHashMap<Integer, Float>();
    private static final Random random = new Random(System.currentTimeMillis());    
    
    
    public double[] normalize(double[] data, int subsys) {
        double[] ret = new double[data.length];
        
        if (!histograms.containsKey(subsys)) {
            NeptusLog.pub().warn("No histogram calculated for subsystem "+subsys);
            return data;
        }
        float[] hist = histograms.get(subsys);
        float avg = averages.get(subsys);
        for (int i = 0; i < data.length; i++) {
            ret[i] = data[i] * (avg/hist[i]);
        }
        return ret;
    }
    
    public static void preview(IMraLogGroup source) {
        SidescanParser ssParser = SidescanParserFactory.build(source);
        SidescanHistogramNormalizer hist = SidescanHistogramNormalizer.create(source);
        
        
        for (int subId : ssParser.getSubsystemList()) {
            
            BufferedImage img = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);

            JLabel lbl = new JLabel() {
                private static final long serialVersionUID = 1L;

                @Override
                public void paint(java.awt.Graphics g) {
                    g.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
                };
            };
            
            JFrame frm = GuiUtils.testFrame(lbl, "Histogram Preview");
            frm.getContentPane().setBackground(Color.white);
            frm.setSize(1024, 1024);
            GuiUtils.centerOnScreen(frm);
            ColorMap cmap = ColorMapFactory.createBronzeColormap();
            
            float[] factor = hist.histograms.get(subId);
            float avg = hist.averages.get(subId);
            int count = 0;
            long time;
            try {
                for (time = ssParser.firstPingTimestamp(); count < 512 && time < ssParser.lastPingTimestamp(); time += 1000) {
                    ArrayList<SidescanLine> lines = ssParser.getLinesBetween(time, time + 1000, subId, HISTOGRAM_DEFAULT_PARAMATERS);
                    
                    for (int l = 0; l < lines.size(); l++) {
                        double[] data = lines.get(l).getData();
                        
                        for (int i = 0; i < data.length; i++) {
                            float advance = img.getWidth()/(float)data.length;
                            int x = (int)(i * advance);
                            int y = count;
                            Color color = cmap.getColor(data[i]);
                            img.setRGB(x, y, color.getRGB());                                
                        }                    
                        count++;
                        lbl.repaint();
                    }              
                }
                
                for (time = ssParser.firstPingTimestamp(); count < 1024 && time < ssParser.lastPingTimestamp(); time += 1000) {
                    ArrayList<SidescanLine> lines = ssParser.getLinesBetween(time, time + 1000, subId, HISTOGRAM_DEFAULT_PARAMATERS);
                    
                    for (int l = 0; l < lines.size(); l++) {
                        double[] data = lines.get(l).getData();
                        for (int i = 0; i < data.length; i++) {
                            float advance = img.getWidth()/(float)data.length;
                            int x = (int)(i * advance);
                            int y = count;
                            Color color = cmap.getColor(data[i] * (avg/factor[i]));
                            img.setRGB(x, y, color.getRGB());                                
                        }                    
                        count++;
                        lbl.repaint();
                    }              
                }
            }
            catch (Exception e) {
                continue;
            }
        }    
    }
    
    public static SidescanHistogramNormalizer create(IMraLogGroup source) {
        synchronized (source) {
            File cache = new File(source.getDir(), "mra/histogram.cache");
            try {
                if (source.getFile("mra/histogram.cache").canRead()) {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
                    SidescanHistogramNormalizer histogram = (SidescanHistogramNormalizer) ois.readObject();
                    ois.close();
                    NeptusLog.pub().info("Read histogram from cache file.");
                    if (histogram.averages.isEmpty())
                        throw new Exception();
                    return histogram;
                }
            }
            catch (Exception e) {
                
            }
            SidescanParser ssParser = SidescanParserFactory.build(source);
            if (ssParser == null)
                return new SidescanHistogramNormalizer();
            
            NeptusLog.pub().info("Histogram cache not found. Creating new one.");
            SidescanHistogramNormalizer hist = new SidescanHistogramNormalizer();
            
            for (int subId : ssParser.getSubsystemList()) {
                NeptusLog.pub().info("Calculating histogram for subsystem "+subId);
                try {
                    SidescanLine pivot = ssParser.getLinesBetween(ssParser.lastPingTimestamp()-1000, ssParser.lastPingTimestamp(), subId, HISTOGRAM_DEFAULT_PARAMATERS).get(0);
                    float[] avg = new float[pivot.getData().length];
                    int count = 0;
                    int logSeconds = (int) ((ssParser.lastPingTimestamp() - ssParser.firstPingTimestamp()) / 1000);
                    while (count < LINES_TO_COMPUTE_HISTOGRAM) {
                        long randomPosition = ssParser.firstPingTimestamp() + random.nextInt(logSeconds) * 1000;
                        ArrayList<SidescanLine> lines = ssParser.getLinesBetween(randomPosition, randomPosition + 1000, subId, HISTOGRAM_DEFAULT_PARAMATERS);
                        for (int l = 0; l < lines.size(); l++) {
                            double data[] = lines.get(l).getData();
                            for (int i = 0; i < data.length; i++)
                                avg[i] = (float) ((avg[i] * count) + data[i]) / (count+1);
                            count++;
                        };                    
                    }
                    
                    
                    double sum = 0;
                    for (int i = 0; i < avg.length; i++) {
                        if (!Float.isFinite(avg[i]))
                            avg[i] = 0;
                        sum += avg[i];
                    }
                    hist.histograms.put(subId, avg);
                    hist.averages.put(subId, (float) (sum / avg.length));                    
                }
                catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }       
            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cache));
                oos.writeObject(hist);
                oos.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            NeptusLog.pub().info("Histogram cache saved to "+cache.getPath()+".");
            
            return hist;
        }
        
    }
    
    boolean hasHistogram() {
        return !histograms.isEmpty();
    }
    private SidescanHistogramNormalizer() {}
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (float[] hist : histograms.values())
            for (int i = 0; i < hist.length; i++) {
                builder.append(hist[i]+"\n");
            }
        builder.append("\n");
        return builder.toString();

    }
    
    public static class HistogramExporter implements MRAExporter {
        
        public HistogramExporter(IMraLogGroup source) {
        }
        
        @Override
        public boolean canBeApplied(IMraLogGroup source) {
            return SidescanParserFactory.build(source) != null;
            
        }

        @Override
        public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
            return "" + SidescanHistogramNormalizer.create(source);            
        }
    }
    
    public static void main(String[] args) throws Exception {      
        args = new String[] {
             //"/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Raw/lauv-noptilus-2/20180711/110508_A042_NP2/"   
             //"/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Raw/lauv-noptilus-3/20190712/140525_A75-NP3-FP13"
             "/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Raw/lauv-xtreme-2/20190114/134424_A73_XT2"
        };
        
        SidescanHistogramNormalizer.preview(new LsfLogSource("/media/zp/5e169b60-ba8d-47db-b25d-9048fe40eed1/OMARE/Raw/lauv-noptilus-2/20180711/110508_A042_NP2/Data.lsf", null));
        
//        MRAProperties.batchMode = true;
//        GuiUtils.setLookAndFeelNimbus();
//        if (args.length == 0)
//            BatchMraExporter.apply(HistogramExporter.class);
//        else {
//            File[] roots = new File[args.length];
//            for (int i = 0; i < roots.length; i++)
//                roots[i] = new File(args[i]);
//
//            LsfTreeSet set = new LsfTreeSet(roots);
//            BatchMraExporter.apply(set, HistogramExporter.class);
//        }
    }
}
