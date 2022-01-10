/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 24/11/2017
 */
package pt.lsts.neptus.plugins.envdisp.painter;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.plugins.envdisp.datapoints.BaseDataPoint;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.coord.MapTileRendererCalculator;

public class DataCollector<T extends BaseDataPoint<?>> implements
        Collector<T, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> {
    
    public MapTileRendererCalculator rendererCalculator; 
    public int offScreenBufferPixel;
    
    public int gridSpacing = 8;
    
    public Function<T, Boolean> acceptor;
    public Function<T, ArrayList<Object>> extractor;
    public BinaryOperator<ArrayList<Object>> merger;

    public LongAccumulator visiblePts = new LongAccumulator((r, i) -> r += i, 0);
    public LongAccumulator toDatePts = new LongAccumulator((r, i) -> r = i > r ? i : r, 0);
    public LongAccumulator fromDatePts = new LongAccumulator((r, i) -> r = i < r ? i : r, Long.MAX_VALUE);
    
    private AtomicBoolean abortIndicator = null;

    /**
     * Data collector class, see {@link java.util.stream.Collector}, to process data
     * for painting.
     * 
     * @param renderer The renderer where it will be painted.
     * @param offScreenBufferPixel The off-screen pixels to consider.
     * @param gridSpacing The grid spacing to use, in pixels.
     * @param acceptor This will be called to decide if data point is accepted or not for processing.
     * @param extractor This will be called to extract data from the data point.
     * @param merger This will be called to merge 2 data points data.
     */
    public DataCollector(StateRenderer2D renderer, 
            int offScreenBufferPixel, int gridSpacing, Function<T, Boolean> acceptor,
            Function<T, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger) {
        this(new MapTileRendererCalculator(renderer), offScreenBufferPixel,
                gridSpacing, acceptor, extractor, merger, null);
    }

    public DataCollector(StateRenderer2D renderer, 
            int offScreenBufferPixel, int gridSpacing, Function<T, Boolean> acceptor,
            Function<T, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger,
            AtomicBoolean abortIndicator) {
        this(new MapTileRendererCalculator(renderer), offScreenBufferPixel,
                gridSpacing, acceptor, extractor, merger, abortIndicator);
    }

    public DataCollector(MapTileRendererCalculator rendererCalculator, 
            int offScreenBufferPixel, int gridSpacing, Function<T, Boolean> acceptor,
            Function<T, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger) {
        this(rendererCalculator, offScreenBufferPixel, gridSpacing, acceptor,
                extractor, merger, null);
    }

    /**
     * Data collector class, see {@link java.util.stream.Collector}, to process data
     * for painting.
     * 
     * @param rendererCalculator The renderer calculator where it will be painted.
     * @param offScreenBufferPixel The off-screen pixels to consider.
     * @param gridSpacing The grid spacing to use, in pixels.
     * @param acceptor This will be called to decide if data point is accepted or not for processing.
     * @param extractor This will be called to extract data from the data point.
     * @param merger This will be called to merge 2 data points data.
     * @param abortIndicator
     */
    public DataCollector(MapTileRendererCalculator rendererCalculator, 
            int offScreenBufferPixel, int gridSpacing, Function<T, Boolean> acceptor,
            Function<T, ArrayList<Object>> extractor, BinaryOperator<ArrayList<Object>> merger,
            AtomicBoolean abortIndicator) {
        this.rendererCalculator = rendererCalculator;
        this.offScreenBufferPixel = offScreenBufferPixel;
        this.gridSpacing = gridSpacing;
        this.acceptor = acceptor;
        this.extractor = extractor;
        this.merger = merger;
        this.abortIndicator = abortIndicator != null ? abortIndicator : new AtomicBoolean();
    }

    @Override
    public Supplier<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> supplier() {
        return new Supplier<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>>() {
            @Override
            public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> get() {
                return new ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>();
            }
        };
    }

    @Override
    public BiConsumer<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, T> accumulator() {
        return new BiConsumer<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, T>() {
            @Override
            public void accept(ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> res, T dp) {
                try {
                    if (res.isEmpty()) {
                        res.add(new ConcurrentHashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                        res.add(new ConcurrentHashMap<Point2D, Pair<ArrayList<Object>, Date>>());
                    }
                    
                    if (abortIndicator.get() || !acceptor.apply(dp))
                        return;
                    
                    double latV = dp.getLat();
                    double lonV = dp.getLon();
                    ArrayList<Object> vals = extractor.apply(dp); // dp.getAllDataValues();
                    
                    if (Double.isNaN(latV) || Double.isNaN(lonV))
                        return;
                    for (Object object : vals) {
                        if (object instanceof Number) {
                            double dv = ((Number) object).doubleValue();
                            if (Double.isNaN(dv) || !Double.isFinite(dv))
                                return;
                        }
                    }
                    
                    Date dateV = new Date(dp.getDateUTC().getTime());

                    LocationType loc = new LocationType();
                    loc.setLatitudeDegs(latV);
                    loc.setLongitudeDegs(lonV);
                    
                    Point2D pt = rendererCalculator.getScreenPosition(loc);
                    
                    if (!EnvDataPaintHelper.isVisibleInRender(pt, rendererCalculator.getSize(), offScreenBufferPixel))
                        return;
                    
                    visiblePts.accumulate(1);
                    
                    toDatePts.accumulate(dateV.getTime());
                    fromDatePts.accumulate(dateV.getTime());
                    
                    ArrayList<Point2D> pts = new ArrayList<>();
                    //if (true || renderer.getLevelOfDetail() >= EnvDataPaintHelper.filterUseLOD)
                        pts.add((Point2D) pt.clone());

                    double x = pt.getX();
                    double y = pt.getY();
                    x =  Math.round(x / gridSpacing) * gridSpacing;
                    y =  Math.round(y / gridSpacing) * gridSpacing;
                    pt.setLocation(x, y);
                    pts.add(0, pt);

                    for (int idx = 0; idx < pts.size(); idx++) {
                        Point2D ptI = pts.get(idx);
                        
//                        System.out.println(Thread.currentThread().getName() + " :: DataCollector::idx-" + idx + " :: " + res.get(idx).containsKey(ptI));
//                        if (abortIndicator.get())
//                            System.out.println("abortIndicator " + abortIndicator.get());
                        if (abortIndicator.get())
                            break;

                        Map<Point2D, Pair<ArrayList<Object>, Date>> rd = res.get(idx);
                        if (!rd.containsKey(ptI)) {
                            rd.put(ptI, new Pair<>(vals, dateV));
                        }
                        else {
                            Pair<ArrayList<Object>, Date> pval = rd.get(ptI);
                            ArrayList<Object> pvals = pval.first();
                            vals = merger.apply(vals, pvals);
                            if (dateV.after(pval.second()))
                                rd.put(ptI, new Pair<>(vals, dateV));
                            else
                                rd.put(ptI, new Pair<>(vals, pval.second()));
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().debug(e);
                }
            }
        };
    }

    @Override
    public BinaryOperator<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> combiner() {
        return new BinaryOperator<ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>>() {
            @Override
            public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> apply(
                    ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> res,
                    ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> resInt) {
                
                if (abortIndicator.get())
                    return res;
                
                for (int idxc = 0; idxc < 2; idxc++) {
                    if (abortIndicator.get())
                        break;

                    final int idx = idxc;
                    resInt.get(idx).keySet().stream().forEach(k1 -> {
                        if (abortIndicator.get())
                            return;

                        try {
                            Pair<ArrayList<Object>, Date> sI = resInt.get(idx).get(k1);
                            if (res.get(idx).containsKey(k1)) {
                                Pair<ArrayList<Object>, Date> s = res.get(idx).get(k1);
                                ArrayList<Object> vals = merger.apply(s.first(), sI.first());
                                Date valDate = sI.second().after(s.second()) ? new Date(sI.second().getTime())
                                        : s.second();
                                res.get(idx).put(k1, new Pair<ArrayList<Object>, Date>(vals, valDate));
                            }
                            else {
                                res.get(idx).put(k1, sI);
                            }
                        }
                        catch (Exception e) {
                            NeptusLog.pub().debug(e);
                        }
                    });
                }
                return res;
            }
        };
    }

    @Override
    public Function<ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>, ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>>> finisher() {
        return new Function<ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>, ArrayList<Map<Point2D,Pair<ArrayList<Object>,Date>>>>() {
            @Override
            public ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> apply(
                    ArrayList<Map<Point2D, Pair<ArrayList<Object>, Date>>> t) {
                return t;
            }
        };
    }

    /* (non-Javadoc)
     * @see java.util.stream.Collector#characteristics()
     */
    @Override
    public Set<Collector.Characteristics> characteristics() {
        return EnumSet.of(Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED);
    }
}