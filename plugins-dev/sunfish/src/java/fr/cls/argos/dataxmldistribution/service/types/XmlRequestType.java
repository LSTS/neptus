
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for xmlRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="xmlRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://service.dataxmldistribution.argos.cls.fr/types}baseRequestType">
 *       &lt;sequence>
 *         &lt;element name="displayLocation" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayDiagnostic" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayMessage" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayCollect" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayRawData" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displaySensor" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="argDistrib" type="{http://service.dataxmldistribution.argos.cls.fr/types}argDistribType" minOccurs="0"/>
 *         &lt;element name="displayImageLocation" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="displayHexId" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "xmlRequestType", propOrder = {
    "displayLocation",
    "displayDiagnostic",
    "displayMessage",
    "displayCollect",
    "displayRawData",
    "displaySensor",
    "argDistrib",
    "displayImageLocation",
    "displayHexId"
})
@XmlSeeAlso({
    CsvRequestType.class
})
public class XmlRequestType
    extends BaseRequestType
{

    protected Boolean displayLocation;
    protected Boolean displayDiagnostic;
    protected Boolean displayMessage;
    protected Boolean displayCollect;
    protected Boolean displayRawData;
    protected Boolean displaySensor;
    protected ArgDistribType argDistrib;
    protected Boolean displayImageLocation;
    protected Boolean displayHexId;

    /**
     * Gets the value of the displayLocation property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayLocation() {
        return displayLocation;
    }

    /**
     * Sets the value of the displayLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayLocation(Boolean value) {
        this.displayLocation = value;
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

    /**
     * Gets the value of the displayMessage property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayMessage() {
        return displayMessage;
    }

    /**
     * Sets the value of the displayMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayMessage(Boolean value) {
        this.displayMessage = value;
    }

    /**
     * Gets the value of the displayCollect property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayCollect() {
        return displayCollect;
    }

    /**
     * Sets the value of the displayCollect property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayCollect(Boolean value) {
        this.displayCollect = value;
    }

    /**
     * Gets the value of the displayRawData property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayRawData() {
        return displayRawData;
    }

    /**
     * Sets the value of the displayRawData property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayRawData(Boolean value) {
        this.displayRawData = value;
    }

    /**
     * Gets the value of the displaySensor property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplaySensor() {
        return displaySensor;
    }

    /**
     * Sets the value of the displaySensor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplaySensor(Boolean value) {
        this.displaySensor = value;
    }

    /**
     * Gets the value of the argDistrib property.
     * 
     * @return
     *     possible object is
     *     {@link ArgDistribType }
     *     
     */
    public ArgDistribType getArgDistrib() {
        return argDistrib;
    }

    /**
     * Sets the value of the argDistrib property.
     * 
     * @param value
     *     allowed object is
     *     {@link ArgDistribType }
     *     
     */
    public void setArgDistrib(ArgDistribType value) {
        this.argDistrib = value;
    }

    /**
     * Gets the value of the displayImageLocation property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayImageLocation() {
        return displayImageLocation;
    }

    /**
     * Sets the value of the displayImageLocation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayImageLocation(Boolean value) {
        this.displayImageLocation = value;
    }

    /**
     * Gets the value of the displayHexId property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDisplayHexId() {
        return displayHexId;
    }

    /**
     * Sets the value of the displayHexId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDisplayHexId(Boolean value) {
        this.displayHexId = value;
    }

}
