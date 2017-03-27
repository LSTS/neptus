package pt.lsts.neptus.nvl.runtime;

public interface NVLVehicle {
   String getId();
   NVLVehicleType getType();
   Availability getAvailability();
   Position getPosition();
   PayloadComponent getPayload();
}
