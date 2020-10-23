/*
 * GISCoordinate.java
 *
 * Created on May 17, 2004, 2:23 PM
 */

package pt.lsts.neptus.types.coord;

import pt.lsts.neptus.NeptusLog;

/**
 *
 * @author  Tomer Petel (tomer@pacbell.net)
 * Credits:
 * Calculation routines (direct() and direct_ell()) are based on work done by 
 * Ed Williams. Ed's Code is at: http://williams.best.vwh.net/gccalc.htm
 */
public class GISCoordinate {
    public static final int SPHERE=0;
    public static final int WGS84=1;
    public static final int NAD27=2;
    public static final int International=3;
    public static final int Krasovsky=4;
    public static final int Bessel=5;
    public static final int WGS72=6;
    public static final int WGS66=7;
    public static final int FAI_sphere=8;
    public static final int User=9;
    public static final int NAD83=WGS84;
    public static final int GRS80=WGS84;
    public static class Ellipsoid {
        public Ellipsoid(String dispName, String name, double a, double invf) {
            this.dispName=dispName;
            this.name=name;
            this.a=a;
            this.invf=invf;
        }
        public String dispName;
        public String name;
        public double a;
        public double invf;
    }
    private static Ellipsoid ells[]=new Ellipsoid[10];
    static {
        ells[0]= new Ellipsoid("Spherical (1'=1nm)", "Sphere", 180*60/Math.PI,Double.POSITIVE_INFINITY);
        ells[1]= new Ellipsoid("WGS84/NAD83/GRS80","WGS84",6378.137/1.852,298.257223563);
        ells[2]= new Ellipsoid("Clarke (1866)/NAD27", "NAD27",6378.2064/1.852,294.9786982138);
        ells[3]= new Ellipsoid("International","International",6378.388/1.852,297.0);
        ells[4]= new Ellipsoid("Krasovsky","Krasovsky",6378.245/1.852,298.3);
        ells[5]= new Ellipsoid("Bessel (1841)", "Bessel",6377.397155/1.852,299.1528);
        ells[6]= new Ellipsoid("WGS72","WGS72",6378.135/1.852,298.26);
        ells[7]= new Ellipsoid("WGS66", "WGS66",6378.145/1.852,298.25);
        ells[8]= new Ellipsoid("FAI sphere", "FAI sphere",6371.0/1.852,1000000000);
        ells[9]= new Ellipsoid("User Defined","User",0,0);  // last one!
    }
    private double _lat=0.0; //in decimal degrees
    private double _lon=0.0; //in decimal degrees
    private String _sep=","; //separator for printing out the coordinates
    private boolean _printLonFirst=false;
    
    /*
     * in degrees minutes format
     * lat Format - DD.DD, DD:MM.MM or DD:MM:SS.SS
     * lon Format - DD.DD, DD:MM.MM or DD:MM:SS.SS
     * latDir - N/S
     * lonDir - W/E
     **/
    public GISCoordinate(String lat, String latDir, String lon, String lonDir) throws Exception {
        setCoordinates(lat,latDir,lon,lonDir);
    }
    
    /** in decimal degrees/rads
     * if rad is true, the entered lat and lon are in radians rather
     * than degrees. otherwise they are in degrees
     */
    public GISCoordinate(double lat, double lon, boolean rad) {
        if (rad) {
            _lat=lat*(180/Math.PI);
            _lon=lon*(180/Math.PI);
        } else {
            _lat=lat;
            _lon=lon;
        }
        verify();
    }

    public void setCoordinates(String lat, String latDir, String lon, String lonDir) throws Exception {
        _lat=parselatlon(lat) * ((latDir.equals("N"))?1:-1);
        _lon=parselatlon(lon) * ((lonDir.equals("E"))?1:-1);
        verify();
    }
    //returns latitude in radians
    public double getLatInRad() {
        return _lat*(Math.PI/180);
    }
    
    //returns longtitude in radians
    public double getLonInRad() {
        return _lon*(Math.PI/180);
    }
    
    //returns latitude in decimal degrees
    public double getLatInDecDeg() {
        return _lat;
    }
    
    //returns longtitude in decimal degrees
    public double getLonInDecDeg() {
        return _lon;
    }
    
    public String getLatDeg() {
        String latS=degtodm(Math.abs(_lat));
        return latS.substring(0,latS.indexOf(':'));
    }
    public String getLatMin() {
        String latS=degtodm(Math.abs(_lat));
        return latS.substring(latS.indexOf(':')+1,latS.lastIndexOf(':'));
    }
    public String getLatSec() {
        String latS=degtodm(Math.abs(_lat));
        return latS.substring(latS.lastIndexOf(':')+1);
    }
    public String getLonDeg() {
        String lonS=degtodm(Math.abs(_lon));
        return lonS.substring(0,lonS.indexOf(':'));
    }
    public String getLonMin() {
        String lonS=degtodm(Math.abs(_lon));
        return lonS.substring(lonS.indexOf(':')+1,lonS.lastIndexOf(':'));
    }
    public String getLonSec() {
        String lonS=degtodm(Math.abs(_lon));
        return lonS.substring(lonS.lastIndexOf(':')+1);
    }

    //print out in degree decimal (West and South are negative)
    public void printDEGDEC(java.io.PrintStream ps) {
        if (_printLonFirst) {
            ps.print(getLonInDecDeg()+_sep+getLatInDecDeg());
        } else {
            ps.print(getLatInDecDeg()+_sep+getLonInDecDeg());
        }
    }
    
    //print out in degree minute
    public void printDEGMIN(java.io.PrintStream ps) {
        String lats=degtodm(Math.abs(_lat));
        String lons=degtodm(Math.abs(_lon));
        if (_printLonFirst) {
           ps.print(lons+"("+(_lon<0?"W":"E")+")"+_sep+lats+"("+(_lat<0?"S":"N")+")");
        } else {
           ps.print(lats+"("+(_lat<0?"S":"N")+")"+_sep+lons+"("+(_lon<0?"W":"E")+")");
        }
    }   
    
    public String getPrintSeparator() {
        return _sep;
    }
    //used to set what the separator is for printing a coordinate pair
    public void setPrintSeparator(String sep) {
        _sep=sep;
    }
    
    public boolean getPrintLonFirst() {
        return _printLonFirst;
    }
    
    public void setPrintLonFirst(boolean f) {
        _printLonFirst=f;
    }

    /*
     * moves this GISCoordinate by distft Feet, to the d
     * distft - how far in feet to move
     * direction - which directions in degrees to move to
     * model - which calculation datum to use
     */
    public void move(double distft, double direction, int model) throws Exception {
        double distNM=distft/(185200.0/30.48); //convert to nm (does that stand for nautical miles??)
        
        double dirRAD=direction*Math.PI/180; // radians
        
        Ellipsoid ellipse=ells[model];
        
        if (ellipse.name.equals("Sphere")) {
            // spherical code
            distNM /=(180*60/Math.PI);  // in radians
            direct(getLatInRad(),-getLonInRad(),dirRAD,distNM); //EAST is negative for these calcs, not West as is normally accepted.
        } else {
            //elliptic code
            direct_ell(getLatInRad(),getLonInRad(),dirRAD,distNM,ellipse);  // ellipse uses East negative
        }
    }
    
    public String toString() {
        try {
            boolean bLatFirst=getPrintLonFirst();
            String sep=getPrintSeparator();
            setPrintLonFirst(false);
            setPrintSeparator(",");
            java.io.ByteArrayOutputStream baos=new java.io.ByteArrayOutputStream();
            printDEGMIN(new java.io.PrintStream(baos));
            baos.close();
            setPrintLonFirst(bLatFirst);
            setPrintSeparator(sep);
            return baos.toString();
        } catch (Throwable t) {
            return t.toString();
        }
    }
    
    public static GISCoordinate FromString(String s) throws Exception {
        String lat=s.substring(0,s.indexOf('('));
        String latDir=Character.toString(s.charAt(s.indexOf('(')+1));
        
        String lon=s.substring(s.indexOf(",")+1,s.lastIndexOf('('));
        String lonDir=Character.toString(s.charAt(s.lastIndexOf("(")+1));
	GISCoordinate g=new GISCoordinate(lat,latDir,lon,lonDir);
        return g;
    }
    
    //the regular clone method does not support exceptions
    public GISCoordinate makeClone() throws Exception {
        return new GISCoordinate(_lat,_lon, false);
    }

    /*******************************************************************
     *******************************************************************
     *******************************************************************
     PRIVATE IMPLMENTATION METHODS BELOW. 
     *******************************************************************
     *******************************************************************
     *******************************************************************/

    private void verify() {
        if (Math.abs(_lat)>90) {
            _lat = 0;
            System.err.println("invalid latitude: "+_lat+" setting as 0.");
        }
        if (Math.abs(_lon)>180) {
            _lon = 0;
            System.err.println("invalid longitude: "+_lon+" setting as 0.");
        }
    }

    private void direct(double latRad, double lonRad, double dirRad, double distNM) throws Exception {
        double EPS= 0.00000000005;
        double dlon,lat,lon;
        if ((Math.abs(Math.cos(latRad))<EPS) && !(Math.abs(Math.sin(dirRad))<EPS)){
            throw new Exception("Only N-S courses are meaningful, starting at a pole!");
        }
        
        lat=Math.asin(Math.sin(latRad)*Math.cos(distNM)+Math.cos(latRad)*Math.sin(distNM)*Math.cos(dirRad));
        if (Math.abs(Math.cos(lat))<EPS){
            lon=0; //endpoint a pole
        }else{
            dlon=Math.atan2(Math.sin(dirRad)*Math.sin(distNM)*Math.cos(latRad), Math.cos(distNM)-Math.sin(latRad)*Math.sin(lat));
            lon=mod( lonRad-dlon+Math.PI,2*Math.PI )-Math.PI;
        }
        
        _lat=lat*(180/Math.PI);
        _lon=lon*(180/Math.PI);
    }
    private void direct_ell(double glat1, double glon1, double faz, double s, Ellipsoid ellipse) throws Exception {
        // glat1 initial geodetic latitude in radians N positive
        // glon1 initial geodetic longitude in radians E positive
        // faz forward azimuth in radians
        // s distance in units of a (=nm)
        
        double EPS= 0.00000000005;
        double r=0, tu=0, sf=0, cf=0, b=0, cu=0, su=0, sa=0, c2a=0, x=0, c=0, d=0, y=0, sy=0, cy=0, cz=0, e=0,a=0;
        double glat2,glon2,f;
        @SuppressWarnings("unused")
		double baz;
        
        if ((Math.abs(Math.cos(glat1))<EPS) && !(Math.abs(Math.sin(faz))<EPS)){
            throw new Exception("Only N-S courses are meaningful, starting at a pole!");
        }
        
        a=ellipse.a;
        f=1/ellipse.invf;
        r = 1 - f;
        tu = r * Math.tan(glat1);
        sf = Math.sin(faz);
        cf = Math.cos(faz);
        if (cf==0){
            b=0.;
        }else{
            b=2. * Math.atan2(tu, cf);
        }
        cu = 1. / Math.sqrt(1 + tu * tu);
        su = tu * cu;
        sa = cu * sf;
        c2a = 1 - sa * sa;
        x = 1. + Math.sqrt(1. + c2a * (1. / (r * r) - 1.));
        x = (x - 2.) / x;
        c = 1. - x;
        c = (x * x / 4. + 1.) / c;
        d = (0.375 * x * x - 1.) * x;
        tu = s / (r * a * c);
        y = tu;
        c = y + 1;
        while (Math.abs(y - c) > EPS) {
            sy = Math.sin(y);
            cy = Math.cos(y);
            cz = Math.cos(b + y);
            e = 2. * cz * cz - 1.;
            c = y;
            x = e * cy;
            y = e + e - 1.;
            y = (((sy * sy * 4. - 3.) * y * cz * d / 6. + x) * d / 4. - cz) * sy * d + tu;
        }
        
        b = cu * cy * cf - su * sy;
        c = r * Math.sqrt(sa * sa + b * b);
        d = su * cy + cu * sy * cf;
        glat2 = modlat(Math.atan2(d, c));
        c = cu * cy - su * sy * cf;
        x = Math.atan2(sy * sf, c);
        c = ((-3. * c2a + 4.) * f + 4.) * c2a * f / 16.;
        d = ((e * cy * c + cz) * sy * c + y) * sa;
        glon2 = modlon(glon1 + x - (1. - c) * d * f);	// fix date line problems
        baz = modcrs(Math.atan2(sa, b) + Math.PI);
        _lat=glat2*(180/Math.PI);
        _lon=glon2*(180/Math.PI);
    }
    private double mod(double x, double y){
        return x-y*Math.floor(x/y);
    }
    private double modlat(double x) {
        return mod(x+Math.PI/2,2*Math.PI)-Math.PI/2;
    }
    private double modlon(double x){
        return mod(x+Math.PI,2*Math.PI)-Math.PI;
    }
    private double modcrs(double x){
        return mod(x,2*Math.PI);
    }
    
    private double parselatlon(String str) throws Exception {
        // Parse string in the format: dd:mm:ss.sssss
        double ret=0;
        java.util.StringTokenizer tok=new java.util.StringTokenizer(str,":");
        if (tok.hasMoreTokens()) {
            try {
                ret+=Integer.parseInt(tok.nextToken()); //degrees
            } catch (NumberFormatException e) {
                throw new Exception("Degrees must be an integer");
            }
            if (tok.hasMoreTokens()) {
                int min=0;
                try {
                    min=Integer.parseInt(tok.nextToken());//minutes
                } catch (NumberFormatException e) {
                    throw new Exception("minutes must be an integer");
                }
                if (min>=60) {
                    throw new Exception("minutes must be less than 60");
                }
                ret+=min/60.0;
                if (tok.hasMoreTokens()) {
                    double sec=0;
                    try {
                        sec=Double.parseDouble(tok.nextToken());//seconds
                    } catch (NumberFormatException e) {
                        throw new Exception("minutes must be in decimal or integer format");
                    }
                    if (sec>=60) {
                        throw new Exception("seconds must be less than 60");
                    }
                    ret+=sec/3600.0;
                }
            }
        } else {
            throw new Exception(str+" is not a valid coordinate format");
        }
        return ret;
    }
    
    //print out in degree radians
    private String degtodm(double degdec){
        // returns a rounded string DD:MM:SS.SSSSS
        int deg=(int)Math.floor(degdec);
        int min=(int)Math.floor(60.0*(degdec-deg));
        double sec=((60.0*(degdec-deg))-min)*60;
        java.text.NumberFormat nf = new java.text.DecimalFormat();
        nf.setMaximumFractionDigits(5);
        return Integer.toString(deg)+":"+Integer.toString(min)+":"+nf.format(sec);
    }
    
    //tester:
    public static void main(String args[]) {
        try {
            GISCoordinate g = new GISCoordinate("37:55:15","N","122:20:59","W");
            System.out.print("Original location in decimal degrees:");
            g.printDEGDEC(System.out);
            System.out.println();
            System.out.print("Original location in degrees:");
            g.printDEGMIN(System.out);
            System.out.println();
            double movedist=2640;//5280.0;
            double movedeg=167; 
            NeptusLog.pub().info("<###>Moving "+movedist+" Feet, in a direction of "+movedeg+" degrees...");
            g.move(movedist, movedeg, NAD83);
            System.out.print("New location in decimal degrees:");
            g.printDEGDEC(System.out);
            System.out.println();
            System.out.print("New location in degrees:");
            g.printDEGMIN(System.out);
            NeptusLog.pub().info("<###>\ntesting the toString function:"+GISCoordinate.FromString(g.toString()).toString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
