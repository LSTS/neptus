
package fr.cls.argos.dataxmldistribution.service.types;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fr.cls.argos.dataxmldistribution.service.types package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _PlatformListResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "platformListResponse");
    private final static QName _KmlResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "kmlResponse");
    private final static QName _XsdResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "xsdResponse");
    private final static QName _CsvRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "csvRequest");
    private final static QName _XsdRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "xsdRequest");
    private final static QName _DixException_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "DixException");
    private final static QName _ObservationResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "observationResponse");
    private final static QName _PlatformListRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "platformListRequest");
    private final static QName _KmlRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "kmlRequest");
    private final static QName _XmlResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "xmlResponse");
    private final static QName _StreamXmlRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "streamXmlRequest");
    private final static QName _ObservationRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "observationRequest");
    private final static QName _StreamXmlResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "streamXmlResponse");
    private final static QName _CsvResponse_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "csvResponse");
    private final static QName _XmlRequest_QNAME = new QName("http://service.dataxmldistribution.argos.cls.fr/types", "xmlRequest");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fr.cls.argos.dataxmldistribution.service.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link StringResponseType }
     * 
     */
    public StringResponseType createStringResponseType() {
        return new StringResponseType();
    }

    /**
     * Create an instance of {@link XmlRequestType }
     * 
     */
    public XmlRequestType createXmlRequestType() {
        return new XmlRequestType();
    }

    /**
     * Create an instance of {@link ObservationRequestType }
     * 
     */
    public ObservationRequestType createObservationRequestType() {
        return new ObservationRequestType();
    }

    /**
     * Create an instance of {@link StreamResponseType }
     * 
     */
    public StreamResponseType createStreamResponseType() {
        return new StreamResponseType();
    }

    /**
     * Create an instance of {@link CsvRequestType }
     * 
     */
    public CsvRequestType createCsvRequestType() {
        return new CsvRequestType();
    }

    /**
     * Create an instance of {@link PlatformListRequestType }
     * 
     */
    public PlatformListRequestType createPlatformListRequestType() {
        return new PlatformListRequestType();
    }

    /**
     * Create an instance of {@link KmlRequestType }
     * 
     */
    public KmlRequestType createKmlRequestType() {
        return new KmlRequestType();
    }

    /**
     * Create an instance of {@link XsdRequestType }
     * 
     */
    public XsdRequestType createXsdRequestType() {
        return new XsdRequestType();
    }

    /**
     * Create an instance of {@link DixException }
     * 
     */
    public DixException createDixException() {
        return new DixException();
    }

    /**
     * Create an instance of {@link BaseRequestType }
     * 
     */
    public BaseRequestType createBaseRequestType() {
        return new BaseRequestType();
    }

    /**
     * Create an instance of {@link PeriodType }
     * 
     */
    public PeriodType createPeriodType() {
        return new PeriodType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "platformListResponse")
    public JAXBElement<StringResponseType> createPlatformListResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_PlatformListResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "kmlResponse")
    public JAXBElement<StringResponseType> createKmlResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_KmlResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "xsdResponse")
    public JAXBElement<StringResponseType> createXsdResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_XsdResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CsvRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "csvRequest")
    public JAXBElement<CsvRequestType> createCsvRequest(CsvRequestType value) {
        return new JAXBElement<CsvRequestType>(_CsvRequest_QNAME, CsvRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XsdRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "xsdRequest")
    public JAXBElement<XsdRequestType> createXsdRequest(XsdRequestType value) {
        return new JAXBElement<XsdRequestType>(_XsdRequest_QNAME, XsdRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DixException }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "DixException")
    public JAXBElement<DixException> createDixException(DixException value) {
        return new JAXBElement<DixException>(_DixException_QNAME, DixException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "observationResponse")
    public JAXBElement<StringResponseType> createObservationResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_ObservationResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PlatformListRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "platformListRequest")
    public JAXBElement<PlatformListRequestType> createPlatformListRequest(PlatformListRequestType value) {
        return new JAXBElement<PlatformListRequestType>(_PlatformListRequest_QNAME, PlatformListRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KmlRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "kmlRequest")
    public JAXBElement<KmlRequestType> createKmlRequest(KmlRequestType value) {
        return new JAXBElement<KmlRequestType>(_KmlRequest_QNAME, KmlRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "xmlResponse")
    public JAXBElement<StringResponseType> createXmlResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_XmlResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XmlRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "streamXmlRequest")
    public JAXBElement<XmlRequestType> createStreamXmlRequest(XmlRequestType value) {
        return new JAXBElement<XmlRequestType>(_StreamXmlRequest_QNAME, XmlRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ObservationRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "observationRequest")
    public JAXBElement<ObservationRequestType> createObservationRequest(ObservationRequestType value) {
        return new JAXBElement<ObservationRequestType>(_ObservationRequest_QNAME, ObservationRequestType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StreamResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "streamXmlResponse")
    public JAXBElement<StreamResponseType> createStreamXmlResponse(StreamResponseType value) {
        return new JAXBElement<StreamResponseType>(_StreamXmlResponse_QNAME, StreamResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link StringResponseType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "csvResponse")
    public JAXBElement<StringResponseType> createCsvResponse(StringResponseType value) {
        return new JAXBElement<StringResponseType>(_CsvResponse_QNAME, StringResponseType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XmlRequestType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://service.dataxmldistribution.argos.cls.fr/types", name = "xmlRequest")
    public JAXBElement<XmlRequestType> createXmlRequest(XmlRequestType value) {
        return new JAXBElement<XmlRequestType>(_XmlRequest_QNAME, XmlRequestType.class, null, value);
    }

}
