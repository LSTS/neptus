/*
package elias.kth.MyFirstPlugin;

import java.util.ArrayList;

public class dev_DualVehicleController {
    static private double c = 0.001;

    public static double[][] calculateDesiredVelocity(double[] fishLocation, double[] fishVelocity,
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

        double longitudeFactor = Math.cos(fishLocation[1]);

        int n_vehicles = vehicles.size();

        double[] x = new double[2];
        double[] x_dot = new double[2];
        double[][] p = new double[n_vehicles][2];

        double D[] = new double[n_vehicles];
        double delta[] = new double[n_vehicles];
        double alpha[] = new double[n_vehicles];

        for (int i = 0; i < n_vehicles; i++) {
            D[i] = vehicles.get(i).params.getD();
            delta[i] = vehicles.get(i).params.getDelta();
            alpha[i] = vehicles.get(i).params.getAlpha();

            if (flipXY) {
                p[i][0] = longitudeFactor*vehicles.get(i).getLon();
                p[i][1] = vehicles.get(i).getLat();
            } else {
                p[i][0] = vehicles.get(i).getLat();
                p[i][1] = longitudeFactor*vehicles.get(i).getLon();
            }
        }


        if (flipXY) {
            x[0] = longitudeFactor*fishLocation[1]; // Math.cos(Math.abs(fishLocation[0]));
            x[1] = fishLocation[0];
            x_dot[0] = longitudeFactor*fishVelocity[1]; // Math.cos(Math.abs(fishLocation[0]));
            x_dot[1] = fishVelocity[0];

        } else {
            x[0] = fishLocation[0];
            x[1] = longitudeFactor*fishLocation[1];
            x_dot[0] = fishVelocity[0];
            x_dot[1] = longitudeFactor*fishVelocity[1]; // Math.cos(Math.abs(fishLocation[0]));
        }

        // output
        double[][] P_velocity = new double[n_vehicles][2];

        double d[] = new double[n_vehicles];
        double dist[] = new double[n_vehicles];
        double phi[][] = new double[n_vehicles][2];
        double phi_bar[][] = new double[n_vehicles][2];

        for (int i = 0; i < n_vehicles; i++) {
            d[i] = (x[0] - p[i][0]) * (x[0] - p[i][0]) + (x[1] - p[i][1]) * (x[1] - p[i][1]);
            d[i] = Math.sqrt(d[i]);
            phi[i][0] = x[0] - p[i][0];
            phi[i][1] = x[1] - p[i][1];
            phi_bar[i][0] = phi[i][0];
            phi_bar[i][1] = - phi[i][0];
            dist[i] = Math.abs(d[i] - D[i]);

            if (dist[i] > delta[i]) {
                double beta = 0;//Nei(p1, p2, 0, x);
                P_velocity[i][0] = x_dot[0] + 10e7*(d[i] * d[i] - D[i] * D[i]) * phi[i][0] + (2*alpha[i] + c * beta)*phi_bar[i][0];
                P_velocity[i][1] = x_dot[1] + 10e7*(d[i] * d[i] - D[i] * D[i]) * phi[i][1] + (2*alpha[i] + c * beta)*phi_bar[i][1];
            } else {
                double beta = Nei(p1, p2, 0, x); // Label=0 for the first agent.
                P_velocity[i][0] = (x_dot[0] + 5e7*(d[i] * d[i] - D[i] * D[i]) * phi[i][0] + (2*alpha[i] + c * beta)*phi_bar[i][0]);
                P_velocity[i][1] = (x_dot[1] + 5e7*(d[i] * d[i] - D[i] * D[i]) * phi[i][1] + (2*alpha[i] + c * beta)*phi_bar[i][1]);
            }
        }
        return P_velocity;
    }

    public static double Nei(double p[][], int Index, double fishPosition[]) {
        int n_vehicles = p.length;
        double[] theta = new double[n_vehicles];



        for (int i = 0; i < n_vehicles; i++) {
            theta[i] = Math.atan2(p[i][1] - fishPosition[1], p[i][0] - fishPosition[0]);
        }

        double max = theta[0];
        double min = theta[0];
        int index_max = 0;
        int index_min = 0;

        // WHAT TO DO FROM HERE!!!???

        for (int i = 1; i < n_vehicles; i++) {
            if (theta[i] >= max) {
                max = theta[i];
                index_max = i;
            }
            else if (theta[i] < min) {
                min = theta[i];
                index_min = i;
            }
        }

        // #################################################

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
*/
