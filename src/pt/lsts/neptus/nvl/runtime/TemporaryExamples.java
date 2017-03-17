package pt.lsts.neptus.nvl.runtime;

import java.util.List;

import static pt.lsts.neptus.nvl.runtime.API.*;

public class TemporaryExamples {


	
    List<NVLVehicle> getResourcesExample(NVLRuntime runtime) {
		List<NVLVehicle> lista = runtime.getVehicles( v -> v.getId().startsWith("abc") );
		return lista;
	}
    
    List<NVLVehicle> getAllAUVs(NVLRuntime runtime) {
		List<NVLVehicle> lista = runtime.getVehicles( v -> v.getType() == NVLVehicleType.AUV );

		return lista;
	}
    
    List<NVLVehicle> getAllAvailableAUVs(NVLRuntime runtime) {
    	List<NVLVehicle> lista = runtime.getVehicles( v -> v.getType() == NVLVehicleType.AUV && v.getAvailability() == Availability.AVAILABLE);

		return lista;
	}
    
   
 
    List<NVLVehicle> getAllAUVs_version2(NVLRuntime runtime) {
       return runtime.getVehicles( require().type(NVLVehicleType.AUV) );
	}
    
    List<NVLVehicle> getAllAvailableAUVs_version2(NVLRuntime runtime) {
        return runtime.getVehicles( require().type(NVLVehicleType.AUV).available() );
	}
    
    List<NVLVehicle> getAllAvailableAUVs_version2(NVLRuntime runtime, List<String> payloadComp) {
        return runtime.getVehicles( require().type(NVLVehicleType.AUV)
        		                             .available()
        		                             .payload(payloadComp) );
	}
}
