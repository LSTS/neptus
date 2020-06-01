package pt.lsts.neptus.plugins.mvplanner.jaxb;

/**
 * @author tsmarques
 * @date 1/29/17
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author tsmarques
 *
 */

@XmlAccessorType(XmlAccessType.NONE)
public class Payload {
    @XmlAttribute
    private String type; /* sidescan, multibeam, etc */


    @XmlJavaTypeAdapter(PayloadParametersAdapter.class)
    private Map<String, String> parameters; /* <parameter, value> */


    public Payload() {
        type = "";
        parameters = new HashMap<>();
    }

    public Payload(String type) {
        this.type = type;
    }

    public String getPayloadType() {
        return type;
    }


    public void setPayloadParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }


    public void addPayloadParamater(String parameter, String value) {
        if(parameters == null)
            parameters = new HashMap<>();
        parameters.put(parameter, value);
    }


    public String getPayloadParameter(String parameter) {
        if(parameters == null)
            return null;

        return parameters.get(parameter);
    }

    public Map<String, String> getPayloadParameters() {
        if(parameters == null)
            parameters = new HashMap<>();
        return parameters;
    }

    public void printPayloadParameters() {
        for(Entry<String, String> param : parameters.entrySet())
            System.out.println(param.getKey() + " : " + param.getValue());
    }
}