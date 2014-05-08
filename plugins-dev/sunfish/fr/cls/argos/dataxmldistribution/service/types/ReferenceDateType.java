
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for referenceDateType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="referenceDateType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="BEST_MSG_DATE"/>
 *     &lt;enumeration value="MODIFICATION_DATE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "referenceDateType")
@XmlEnum
public enum ReferenceDateType {

    BEST_MSG_DATE,
    MODIFICATION_DATE;

    public String value() {
        return name();
    }

    public static ReferenceDateType fromValue(String v) {
        return valueOf(v);
    }

}
