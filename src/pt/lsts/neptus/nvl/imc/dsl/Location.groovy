package pt.lsts.neptus.nvl.imc.dsl

import pt.lsts.util.WGS84Utilities

class Angle {
	double rad
	public Angle(double r) {
		rad = r
	}
	def asDegrees() {Math.toDegrees rad}
	def asRadians() { rad}
	def minus (Angle a) {return new Angle(a.asRadians() - this.asRadians())}
	def plus (Angle a) {return new Angle(a.asRadians() + this.asRadians())}
	def times (double n) {return new Angle(this.asRadians() * n)}
	def div (double n) {return new Angle(this.asRadians() / n)}
	def unaryMinus () {return new Angle (-this.asRadians())}
	
	@Override
	String toString(){
		String.format("%.1fº", asDegrees())
	}
}
class Location {
	
	def final static FEUP = new Location(toDeg(41.1781918), toDeg(-8.5954308))
	def final static APDL = new Location(toDeg(41.185242), toDeg(-8.704803))
	def static toDeg(double value) {Math.toRadians value} //make it a Category of double/Integer

	//test 	println assert deg(90) == Math.PI.rad() / 2
	double latitude
	double longitude
	public Location (double la, double lo) {
		latitude = la
		longitude = lo
	}
	public Location (Angle la, Angle lo) {
		latitude = la.rad
		longitude = lo.rad
	}

	def angle (Location l) {
		def offset = offsets(l)
		Math.atan2(offset[1], offset[0])
	}



	def offsets(Location l) {
		WGS84Utilities.WGS84displacement(
				toDeg(l.latitude), toDeg(l.longitude), 0.0, toDeg(l.latitude), toDeg(l.longitude), 0.0)
		// new Tuple2(offset[0],offset[1])
	}

	def distance(Location l) {
		def offset = offsets(l)
		Math.hypot(offset[0], offset[1])
	}


	def translateBy(double n,double e )  {
		def coords =  WGS84Utilities.WGS84displace(toDeg(latitude), toDeg(longitude), 0.0, n, e, 0.0)
		new Location(toDeg(coords[0]),toDeg(coords[1]))
	}
}
