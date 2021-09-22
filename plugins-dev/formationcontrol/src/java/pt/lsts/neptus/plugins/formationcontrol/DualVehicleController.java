//package elias.kth.MyFirstPlugin;
package pt.lsts.neptus.plugins.formationcontrol;

import java.util.ArrayList;

public class DualVehicleController {
	static private double c = 0.001;

	public static double[] calculateDesiredVelocity(double[] fishLocation, double[] fishVelocity,
													ArrayList<Vehicle> vehicles,
													boolean flipXY) {

		// x: estimation of fish position,
		// x_dot: estimation of fish velocity,
		// p1, p2, p3: positions of AUVs (2 by 1 vectors)

		// d, delta, alpha, c are tuning parameters
		// d : distance to be maintained between AUVs and fish
		// delta : margin about distance 'd'
		// alpha : control 'speed' to go around a circle
		// c : control 'speed' for maintaining interval between UAVs
		// default of delta = d*0.8
		// default of alpha = 1
		// default of c = 1

		double longitudeFactor = 0.45;

		double[] x = new double[2];
		double[] x_dot = new double[2];
		double[] p1 = new double[2];
		double[] p2 = new double[2];

		// input and assume initial values
		if (flipXY) {
			x[0] = longitudeFactor*fishLocation[1]; // Math.cos(Math.abs(fishLocation[0]));
			x[1] = fishLocation[0];
			x_dot[0] = longitudeFactor*fishVelocity[1]; // Math.cos(Math.abs(fishLocation[0]));
			x_dot[1] = fishVelocity[0];
			p1[0] = longitudeFactor*vehicles.get(0).getLon(); // Math.cos(Math.abs(vehicles.get(0).getLat()));
			p1[1] = vehicles.get(0).getLat();
			p2[0] = longitudeFactor*vehicles.get(1).getLon(); // Math.cos(Math.abs(vehicles.get(1).getLat()));
			p2[1] = vehicles.get(1).getLat();
		} else {
			x[0] = fishLocation[0];
			x[1] = longitudeFactor*fishLocation[1];
			x_dot[0] = fishVelocity[0];
			x_dot[1] = longitudeFactor*fishVelocity[1]; // Math.cos(Math.abs(fishLocation[0]));
			p1[0] = vehicles.get(0).getLat();
			p1[1] = longitudeFactor*vehicles.get(0).getLon(); // Math.cos(Math.abs(vehicles.get(0).getLat()));
			p2[0] = vehicles.get(1).getLat();
			p2[1] = longitudeFactor*vehicles.get(1).getLon(); // Math.cos(Math.abs(vehicles.get(1).getLat()));
		}

		double D = vehicles.get(0).params.getD();
		double delta = vehicles.get(0).params.getDelta();
		double alpha = vehicles.get(0).params.getAlpha();

		double D2 = vehicles.get(1).params.getD();
		double delta2 = vehicles.get(1).params.getDelta();
		double alpha2 = vehicles.get(1).params.getAlpha();

		// output
		double[] P1_velocity = new double[2];
		double[] P2_velocity = new double[2];

		//Let's begin
		double d1 = (x[0] - p1[0]) * (x[0] - p1[0]) + (x[1] - p1[1]) * (x[1] - p1[1]);
		d1 = Math.sqrt(d1);
		double d2 = (x[0] - p2[0]) * (x[0] - p2[0]) + (x[1] - p2[1]) * (x[1] - p2[1]);
		d2 = Math.sqrt(d2);

		// for Agent 1
		double[] phi1 = new double[2];
		double[] phi1_bar = new double[2];
		phi1[0] = (x[0] - p1[0]);
		phi1[1] = (x[1] - p1[1]);
		phi1_bar[0] = phi1[1];
		phi1_bar[1] = -phi1[0];

		double Dist1;

		Dist1 = Math.abs(d1 - D);
		System.out.println("Dist1: " + Dist1 + " Delta: " + delta);

		if (Dist1 > delta) {// && !alwaysCalculateBeta) {
			double beta = 0;//Nei(p1, p2, 0, x);
			P1_velocity[0] = x_dot[0] + 10e7*(d1 * d1 - D * D) * phi1[0] + (2*alpha + c * beta)*phi1_bar[0];
			P1_velocity[1] = x_dot[1] + 10e7*(d1 * d1 - D * D) * phi1[1] + (2*alpha + c * beta)*phi1_bar[1];
		} else {
			double beta = Nei(p1, p2, 0, x); // Label=0 for the first agent.
			P1_velocity[0] = (x_dot[0] + 5e7*(d1 * d1 - D * D) * phi1[0] + (2*alpha + c * beta)*phi1_bar[0]);
			P1_velocity[1] = (x_dot[1] + 5e7*(d1 * d1 - D * D) * phi1[1] + (2*alpha + c * beta)*phi1_bar[1]);
			System.out.println("c*BETA1: " + c*beta);
		}

		// for Agent 2
		double[] phi2 = new double[2];
		double[] phi2_bar = new double[2];
		phi2[0] = (x[0] - p2[0]);
		phi2[1] = (x[1] - p2[1]);
		phi2_bar[0] = phi2[1];
		phi2_bar[1] = -phi2[0];

		double Dist2;
		Dist2 = Math.abs(d2 - D2);

		System.out.println("Dist2: " + Dist2);

		if (Dist2 > delta2) {// && !alwaysCalculateBeta) {
			double beta = 0; //Nei(p1, p2, 1, x);
			P2_velocity[0] = (x_dot[0] + 10e7*(d2 * d2 - D2 * D2) * phi2[0] + (2*alpha2 + c * beta)*phi2_bar[0]);
			P2_velocity[1] = (x_dot[1] + 10e7*(d2 * d2 - D2 * D2) * phi2[1] + (2*alpha2 + c * beta)*phi2_bar[1]);
			System.out.println("First term: " + 10e7*(d2 * d2 - D2 * D2)*phi2[0] + " Second term: " + alpha2 * phi2_bar[0]);
			System.out.println("First term: " + 10e7*(d2 * d2 - D2 * D2)*phi2[1] + " Second term: " + alpha2 * phi2_bar[1]);
		} else {
			double beta = Nei(p1, p2, 1, x);
			P2_velocity[0] = (x_dot[0] + 5e7*(d2 * d2 - D2 * D2) * phi2[0] + (2*alpha2 + c * beta) * phi2_bar[0]);
			P2_velocity[1] = (x_dot[1] + 5e7*(d2 * d2 - D2 * D2) * phi2[1] + (2*alpha2 + c * beta) * phi2_bar[1]);
			System.out.println( "c*BETA2: " + c*beta);
		}

		double[] rvalues = new double[]{P1_velocity[0], P1_velocity[1], P2_velocity[0], P2_velocity[1]};
		return rvalues;
	}

	public static double Nei(double[] p1, double[] p2, int Index, double fishPosition[]) {
		double[] theta = new double[2];
		theta[0] = Math.atan2(p1[1] - fishPosition[1], p1[0] - fishPosition[0]);
		theta[1] = Math.atan2(p2[1] - fishPosition[1], p2[0] - fishPosition[0]);
		double max = theta[0];
		int Index_max = 0;
		double min = theta[0];
		int Index_min = 0;

		if (theta[0] > theta[1]) {
			max = theta[0];
			Index_max = 0;
			min = theta[1];
			Index_min = 1;
		} else {
			max = theta[1];
			Index_max = 1;
			min = theta[0];
			Index_min = 0;
		}

		double BETA;
		if (Index == Index_max) {
			BETA = 2 * Math.PI - (max - min);
		} else {
			BETA = max - min;
		}

		return BETA;
	}

	public void setC(double C) {
		c = C;
	}
}
