package pt.lsts.neptus.nvl.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VehicleRequirements implements Filter<NVLVehicle> {

  private NVLVehicleType requiredType = null;
  

private Availability requiredAvailability = null;
  private List<PayloadComponent> requiredPayload = null;
  private Position areaCenter = null;
  private double areaRadius = 0; 


  VehicleRequirements type(NVLVehicleType type) {
    requiredType = type;
    return this; // for chained: http://blog.crisp.se/2013/10/09/perlundholm/another-builder-pattern-for-java
  }

  VehicleRequirements payload(List<PayloadComponent> components) {
    requiredPayload = components;
    return this;
  }

  VehicleRequirements availability(Availability av) {
    requiredAvailability = av;
    return this;
  }

  VehicleRequirements available() {
    return availability(Availability.AVAILABLE);
  }


  /**
   * Area to cover by the vehicle
   * @param center
   * @param radius
   * @return
   */
  VehicleRequirements area(Position center, double radius) {
    areaCenter = center;
    areaRadius = radius;
    return this;
  }

  @Override
  public boolean apply(NVLVehicle v) {

    return   (requiredType != null && v.getType() == requiredType) 
        &&
        (requiredAvailability != null && requiredAvailability == v.getAvailability())
        ;//&&
        //(requiredPayload != null && v.getPayload().containsAll(requiredPayload))
        //&&
        //(areaCenter != null && v.getPosition().near(areaCenter, areaRadius) );
  }

 // static Map<NVLVehicle,VehicleRequirements> filter(List<VehicleRequirements> reqs, List<NVLVehicle> allVehicles) {
  public static List<NVLVehicle> filter(List<VehicleRequirements> reqs, List<NVLVehicle> allVehicles) {
    List<NVLVehicle> result = new ArrayList<>();
    for (VehicleRequirements req: reqs)  
        allVehicles.stream().filter(v -> req.apply(v)).forEach(ok -> result.add(ok));
    return result;  //TODO return just the vehicles that fills the requirements?
  }
  
  /**
   * @return the requiredType
   */
  public NVLVehicleType getRequiredType() {
  	return requiredType;
  }

  /**
   * @param requiredType the requiredType to set
   */
  public void setRequiredType(NVLVehicleType requiredType) {
  	this.requiredType = requiredType;
  }

  /**
   * @return the requiredAvailability
   */
  public Availability getRequiredAvailability() {
  	return requiredAvailability;
  }

  /**
   * @param requiredAvailability the requiredAvailability to set
   */
  public void setRequiredAvailability(Availability requiredAvailability) {
  	this.requiredAvailability = requiredAvailability;
  }

  /**
   * @return the requiredPayload
   */
  public List<PayloadComponent> getRequiredPayload() {
  	return requiredPayload;
  }

  /**
   * @param requiredPayload the requiredPayload to set
   */
  public void setRequiredPayload(List<PayloadComponent> requiredPayload) {
  	this.requiredPayload = requiredPayload;
  }

  /**
   * @return the areaCenter
   */
  public Position getAreaCenter() {
  	return areaCenter;
  }

  /**
   * @param areaCenter the areaCenter to set
   */
  public void setAreaCenter(Position areaCenter) {
  	this.areaCenter = areaCenter;
  }

  /**
   * @return the areaRadius
   */
  public double getAreaRadius() {
  	return areaRadius;
  }

  /**
   * @param areaRadius the areaRadius to set
   */
  public void setAreaRadius(double areaRadius) {
  	this.areaRadius = areaRadius;
  }

}
