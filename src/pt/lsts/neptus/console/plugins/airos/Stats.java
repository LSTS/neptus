
package pt.lsts.neptus.console.plugins.airos;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Stats {

    @SerializedName("rx_data")
    @Expose
    public int rxData;
    @SerializedName("rx_bytes")
    @Expose
    public int rxBytes;
    @SerializedName("rx_pps")
    @Expose
    public int rxPps;
    @SerializedName("tx_data")
    @Expose
    public int txData;
    @SerializedName("tx_bytes")
    @Expose
    public int txBytes;
    @SerializedName("tx_pps")
    @Expose
    public int txPps;

}
