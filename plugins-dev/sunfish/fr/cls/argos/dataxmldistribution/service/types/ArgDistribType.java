
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for argDistribType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="argDistribType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="A"/>
 *     &lt;enumeration value="O"/>
 *     &lt;enumeration value="B"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "argDistribType")
@XmlEnum
public enum ArgDistribType {

    A,
    O,
    B;

    public String value() {
        return name();
    }

    public static ArgDistribType fromValue(String v) {
        return valueOf(v);
    }

}
