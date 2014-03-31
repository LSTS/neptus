
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for kmlRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="kmlRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://service.dataxmldistribution.argos.cls.fr/types}baseRequestType">
 *       &lt;sequence>
 *         &lt;element name="displayDescription" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayDiagnostic" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "kmlRequestType", propOrder = {
    "displayDescription",
    "displayDiagnostic"
})
public class KmlRequestType
    extends BaseRequestType
{

    protected Boolean displayDescription;
    protected Boolean displayDiagnostic;

    /**
     * Gets the value of the displayDescription property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayDescription() {
        return displayDescription;
    }

    /**
     * Sets the value of the displayDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayDescription(Boolean value) {
        this.displayDescription = value;
    }

    /**
     * Gets the value of the displayDiagnostic property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayDiagnostic() {
        return displayDiagnostic;
    }

    /**
     * Sets the value of the displayDiagnostic property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayDiagnostic(Boolean value) {
        this.displayDiagnostic = value;
    }

}
