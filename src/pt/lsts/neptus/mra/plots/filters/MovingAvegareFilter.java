package pt.lsts.neptus.mra.plots.filters;

import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.util.MovingAverage;

@MraFilterDescription(name = "Moving Average", abbrev = "MovingAverageFilter", description = "Moving average on time series")
public class MovingAvegareFilter extends MraFilter {
    @NeptusProperty(name = "Windows Size", description = "Window size for the moving average")
    public short windowSize = 3;

    @Override
    public TimeSeries apply(TimeSeries timeSeries) {
        MovingAverage mavg = new MovingAverage(windowSize);
        TimeSeries newSeries = new TimeSeries("", Millisecond.class);

        for(int i = 0; i < timeSeries.getItemCount(); i++) {
            TimeSeriesDataItem item = timeSeries.getDataItem(i);
            RegularTimePeriod tp = item.getPeriod();

            mavg.update(item.getValue().doubleValue());
            newSeries.add(new TimeSeriesDataItem(tp, mavg.mean()));
        }
        return newSeries;
    }

    @Override
    public String getFilterName() {
        return "Moving Average";
    }
}
