
package pt.lsts.neptus.console.plugins.airos;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Airmax {

    @SerializedName("priority")
    @Expose
    public int priority;
    @SerializedName("quality")
    @Expose
    public int quality;
    @SerializedName("beam")
    @Expose
    public int beam;
    @SerializedName("signal")
    @Expose
    public int signal;
    @SerializedName("capacity")
    @Expose
    public int capacity;

}
