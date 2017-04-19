package pt.lsts.nvl.runtime;

// Utility class to group methods directly called by Groovy DSL / other code
public final class API {
	

	static NVLRuntime runtime() {
		return null;
	}

	static VehicleRequirements require() {
		return new VehicleRequirements();
	}

	
	private API() { }
}
