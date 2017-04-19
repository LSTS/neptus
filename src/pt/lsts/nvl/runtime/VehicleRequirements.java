package pt.lsts.nvl.runtime;

import java.util.ArrayList;
import java.util.List;

public class VehicleRequirements implements Filter<NVLVehicle> {

  private NVLVehicleType requiredType;
  private String requiredName = null;
  private Availability requiredAvailability;
  private List<PayloadComponent> requiredPayload;
  private Position areaCenter;
  private double areaRadius;
  
  public VehicleRequirements() {
      requiredName = null;
      requiredAvailability = null;
      requiredPayload = null;
      areaCenter = null;
      areaRadius = 0;
      
  }


  public VehicleRequirements type(NVLVehicleType type) {
    requiredType = type;
    System.out.println("Changed Required Type to: "+type);
    return this; // for chained: http://blog.crisp.se/2013/10/09/perlundholm/another-builder-pattern-for-java
  }
  
  public VehicleRequirements name(String name) {
      requiredName = name;
      System.out.println("Changed Required Name to: "+name);
      return this; 
    }

  public VehicleRequirements payload(List<PayloadComponent> components) {
    requiredPayload = components;
    return this;
  }

  public VehicleRequirements availability(Availability av) {
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
      //debug version
      boolean result1,result2,result3, result4,result5;
          result1 = (requiredType != null && (v.getType() == requiredType || requiredType.equals(NVLVehicleType.ANY))); 
          result2 = (requiredAvailability != null && requiredAvailability == v.getAvailability());
          result3 = (requiredPayload != null && v.getPayload().containsAll(requiredPayload));
          result4 = (areaCenter != null && v.getPosition().near(areaCenter, areaRadius) );
          result5 = (requiredName != null && v.getId().equals(requiredName));
          if(!result1)
              System.out.println(v.getId()+" Failed in requiredType: "+requiredType);
          if(!result2)
              System.out.println(v.getId()+" Failed in requiredAvailability: "+requiredAvailability);
          if(!result3){
              for(PayloadComponent p: requiredPayload)
                  System.out.println(v.getId()+" Failed in requiredPayload: "+p.getName());
          }
          if(!result4)
              System.out.println(v.getId()+" Failed in areaCenter and radius: "+areaCenter+" "+areaRadius+"\nPosition latitude: "+v.getPosition().latitude() + " and longitude: "+ v.getPosition().latitude() );
          if(!result5){
              System.out.println(v.getId()+" Failed in requiredName: "+ requiredName);
          }
         return result1 && result2 && result3 && result4 && result5;    
      
//   return     (requiredName!= null && requiredName.equals(v.getId()))
//           && (requiredType == NVLVehicleType.ANY || (requiredType != null && (v.getType() == requiredType)))
//           && ((requiredAvailability != null && requiredAvailability == v.getAvailability())) 
//           && ((requiredPayload != null && v.getPayload().containsAll(requiredPayload)))
//           && ((areaCenter != null && v.getPosition().near(areaCenter, areaRadius) ));
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
  
  @Override
  public String toString() {
      String result="";
      if(requiredName != null)
          result+="Vehicle Name: "+requiredName+"\n";
      if(requiredAvailability != null ){
          result+="AVAILABILITY: ";
          switch(requiredAvailability){
            case AVAILABLE:
                result+="AVAILABLE";
                break;
            case BUSY:
                result+="BUSY";
                break;
            case NOT_OPERATIONAL:
                result+="NOT_OPERATIONAL";
                break;
            default:
                break;
              
          }
          result+="\n";
      }
      if(requiredType != null ){
          result+="Type: ";
          switch(requiredType){
            case UAV:
                result+="Unmanned Aerial Vehicle";
                break;
            case ASV:
                result+="Autonomous Surface Vehicle";
                break;
            case ROV:
                result+="Remotely Operated Underwater Vehicle";
                break;
            case AUV:
                result+="Autonomous Underwater Vehicle";
                break;
            case ANY:
                result+="Any Vehicle";
                break;
            default:
                break;
              
          }
          result+="\n";
      }
      if(requiredPayload != null){
          result+="Payloads: \n";
          for(PayloadComponent p: requiredPayload){
            result+=p.getName()+"\n";
            
          }
      }
      
      return result;
  }
  

}
