
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for baseRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="baseRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="username" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;choice>
 *           &lt;element name="programNumber" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="platformId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/choice>
 *         &lt;element name="nbPassByPtt" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="period" type="{http://service.dataxmldistribution.argos.cls.fr/types}periodType"/>
 *           &lt;element name="nbDaysFromNow" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;/choice>
 *         &lt;element name="referenceDate" type="{http://service.dataxmldistribution.argos.cls.fr/types}referenceDateType" minOccurs="0"/>
 *         &lt;element name="locClass" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="geographicArea" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="compression" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="mostRecentPassages" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "baseRequestType", propOrder = {
    "username",
    "password",
    "programNumber",
    "platformId",
    "nbPassByPtt",
    "period",
    "nbDaysFromNow",
    "referenceDate",
    "locClass",
    "geographicArea",
    "compression",
    "mostRecentPassages"
})
@XmlSeeAlso({
    XmlRequestType.class,
    KmlRequestType.class
})
public class BaseRequestType {

    @XmlElement(required = true)
    protected String username;
    @XmlElement(required = true)
    protected String password;
    protected String programNumber;
    protected String platformId;
    protected Integer nbPassByPtt;
    protected PeriodType period;
    protected Integer nbDaysFromNow;
    protected ReferenceDateType referenceDate;
    protected String locClass;
    protected String geographicArea;
    protected Integer compression;
    protected Boolean mostRecentPassages;

    /**
     * Gets the value of the username property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the value of the username property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUsername(String value) {
        this.username = value;
    }

    /**
     * Gets the value of the password property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the value of the password property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassword(String value) {
        this.password = value;
    }

    /**
     * Gets the value of the programNumber property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProgramNumber() {
        return programNumber;
    }

    /**
     * Sets the value of the programNumber property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProgramNumber(String value) {
        this.programNumber = value;
    }

    /**
     * Gets the value of the platformId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlatformId() {
        return platformId;
    }

    /**
     * Sets the value of the platformId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlatformId(String value) {
        this.platformId = value;
    }

    /**
     * Gets the value of the nbPassByPtt property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNbPassByPtt() {
        return nbPassByPtt;
    }

    /**
     * Sets the value of the nbPassByPtt property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNbPassByPtt(Integer value) {
        this.nbPassByPtt = value;
    }

    /**
     * Gets the value of the period property.
     * 
     * @return
     *     possible object is
     *     {@link PeriodType }
     *     
     */
    public PeriodType getPeriod() {
        return period;
    }

    /**
     * Sets the value of the period property.
     * 
     * @param value
     *     allowed object is
     *     {@link PeriodType }
     *     
     */
    public void setPeriod(PeriodType value) {
        this.period = value;
    }

    /**
     * Gets the value of the nbDaysFromNow property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNbDaysFromNow() {
        return nbDaysFromNow;
    }

    /**
     * Sets the value of the nbDaysFromNow property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNbDaysFromNow(Integer value) {
        this.nbDaysFromNow = value;
    }

    /**
     * Gets the value of the referenceDate property.
     * 
     * @return
     *     possible object is
     *     {@link ReferenceDateType }
     *     
     */
    public ReferenceDateType getReferenceDate() {
        return referenceDate;
    }

    /**
     * Sets the value of the referenceDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ReferenceDateType }
     *     
     */
    public void setReferenceDate(ReferenceDateType value) {
        this.referenceDate = value;
    }

    /**
     * Gets the value of the locClass property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocClass() {
        return locClass;
    }

    /**
     * Sets the value of the locClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocClass(String value) {
        this.locClass = value;
    }

    /**
     * Gets the value of the geographicArea property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGeographicArea() {
        return geographicArea;
    }

    /**
     * Sets the value of the geographicArea property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGeographicArea(String value) {
        this.geographicArea = value;
    }

    /**
     * Gets the value of the compression property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCompression() {
        return compression;
    }

    /**
     * Sets the value of the compression property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCompression(Integer value) {
        this.compression = value;
    }

    /**
     * Gets the value of the mostRecentPassages property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isMostRecentPassages() {
        return mostRecentPassages;
    }

    /**
     * Sets the value of the mostRecentPassages property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setMostRecentPassages(Boolean value) {
        this.mostRecentPassages = value;
    }

}
