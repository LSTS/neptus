package pt.lsts.neptus.plugins.videostream;

public class Camera {
    private String name;

    private String ip;
    private String url;

    public Camera(String name, String ip, String url) {
        this.name = name;
        this.ip = ip;
        this.url = url;
    }

    public Camera() {
        this.name = "Select Device";
    }

    public String getName() {
       return name;
    }

    public String getIp() {
        if (ip == null) return "";
        return ip;
    }

    public String getUrl() {
        if (url == null) return "";
        return url;
    }

    public String toString() {
        return name;
    }
}
