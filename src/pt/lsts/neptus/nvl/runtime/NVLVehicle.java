package pt.lsts.neptus.nvl.runtime;

import java.util.List;

public interface NVLVehicle {
   String getId();
   NVLVehicleType getType();
   Availability getAvailability();
   Position getPosition();
   List<PayloadComponent> getPayload();
}
