
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for csvRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="csvRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://service.dataxmldistribution.argos.cls.fr/types}xmlRequestType">
 *       &lt;sequence>
 *         &lt;element name="showHeader" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "csvRequestType", propOrder = {
    "showHeader"
})
public class CsvRequestType
    extends XmlRequestType
{

    protected Boolean showHeader;

    /**
     * Gets the value of the showHeader property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isShowHeader() {
        return showHeader;
    }

    /**
     * Sets the value of the showHeader property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setShowHeader(Boolean value) {
        this.showHeader = value;
    }

}
