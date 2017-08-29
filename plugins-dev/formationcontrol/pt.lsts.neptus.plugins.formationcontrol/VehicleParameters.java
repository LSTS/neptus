package pt.lsts.neptus.plugins.formationcontrol;//package elias.kth.MyFirstPlugin;

/**
 * Created by elias on 8/19/16.
 */
public class VehicleParameters {
    public double d = 0;
    public double delta = 0;
    public double alpha = 0;

    public void setParams(double radius, double delta, double alpha) {
        this.d = radius;
        this.delta = delta;
        this.alpha = alpha;
    }

    public void setRadius(double radius) {
        this.d = radius;
    }

    public void setDelta(double Delta) {
        this.delta = Delta;
    }

    public void setAlpha(double Alpha) {
        this.alpha = Alpha;
    }

    public double getD() { return d; }
    public double getDelta() { return delta; }
    public double getAlpha() { return alpha; }

    public boolean paramsAreSet()
    {
        return ((d != 0) && (delta != 0) && (alpha != 0));
    }
}
