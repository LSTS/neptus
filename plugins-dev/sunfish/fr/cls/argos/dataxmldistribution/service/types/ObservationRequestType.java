
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for observationRequestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="observationRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="username" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="password" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;choice>
 *           &lt;element name="programNumber" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="platformId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *           &lt;element name="wmo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;/choice>
 *         &lt;element name="nbMaxObs" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;choice>
 *           &lt;element name="period" type="{http://service.dataxmldistribution.argos.cls.fr/types}periodType"/>
 *           &lt;element name="nbDaysFromNow" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "observationRequestType", propOrder = {
    "username",
    "password",
    "programNumber",
    "platformId",
    "wmo",
    "nbMaxObs",
    "period",
    "nbDaysFromNow"
})
public class ObservationRequestType {

    @XmlElement(required = true)
    protected String username;
    @XmlElement(required = true)
    protected String password;
    protected String programNumber;
    protected String platformId;
    protected String wmo;
    protected Integer nbMaxObs;
    protected PeriodType period;
    protected Integer nbDaysFromNow;

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
     * Gets the value of the wmo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWmo() {
        return wmo;
    }

    /**
     * Sets the value of the wmo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWmo(String value) {
        this.wmo = value;
    }

    /**
     * Gets the value of the nbMaxObs property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNbMaxObs() {
        return nbMaxObs;
    }

    /**
     * Sets the value of the nbMaxObs property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNbMaxObs(Integer value) {
        this.nbMaxObs = value;
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

}
