
package pt.lsts.neptus.console.plugins.airos;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Station {

    @SerializedName("mac")
    @Expose
    public String mac;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("lastip")
    @Expose
    public String lastip;
    @SerializedName("associd")
    @Expose
    public int associd;
    @SerializedName("aprepeater")
    @Expose
    public int aprepeater;
    @SerializedName("tx")
    @Expose
    public int tx;
    @SerializedName("rx")
    @Expose
    public int rx;
    @SerializedName("signal")
    @Expose
    public int signal;
    @SerializedName("ccq")
    @Expose
    public int ccq;
    @SerializedName("idle")
    @Expose
    public int idle;
    @SerializedName("uptime")
    @Expose
    public int uptime;
    @SerializedName("ack")
    @Expose
    public int ack;
    @SerializedName("distance")
    @Expose
    public int distance;
    @SerializedName("txpower")
    @Expose
    public int txpower;
    @SerializedName("noisefloor")
    @Expose
    public int noisefloor;
    @SerializedName("airmax")
    @Expose
    public Airmax airmax;
    @SerializedName("stats")
    @Expose
    public Stats stats;
    @SerializedName("rates")
    @Expose
    public List<String> rates = new ArrayList<String>();
    @SerializedName("signals")
    @Expose
    public List<Integer> signals = new ArrayList<Integer>();
}
