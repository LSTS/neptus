package org.necsave.simulation;
import java.io.File;

public class Platform {
	
	private String platform_type;
	private String configPath;
	private String path;
	
	public Platform(String path) {
		this.configPath = path;
		this.path = new File(path).getParent();
	}

	public String getPlatformType() {
		return platform_type;
	}

	public void setPlatformType(String platform_type) {
		this.platform_type = platform_type;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	
	
}
