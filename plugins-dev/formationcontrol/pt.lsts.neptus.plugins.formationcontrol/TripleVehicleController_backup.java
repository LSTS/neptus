package pt.lsts.neptus.plugins.formationcontrol;//package elias.kth.MyFirstPlugin;

import java.util.*;

/**
 * Created by rasmu on 17/04/04.
 */
public class TripleVehicleController_backup {
    static private double c = 0.001;


    public static double[] calculateDesiredVelocity(double[] fishLocation, double[] fishVelocity,
                                                    ArrayList<Vehicle> vehicles,
                                                    boolean flipXY, double[] w, double r, double[][] y_agents) {

        // x: estimation of fish position,
        // x_dot: estimation of fish velocity,
        // p1, p2, p3: positions of AUVs (2 by 1 vectors)

        // w is tuning parameter, perhaps vehicles.get(i).params.getw();
        // r : distance to be maintained between AUVs and fish

        double scaling = 1;

        double longitudeFactor = 1;

        double[] x = new double[2];
        double[] x_dot = new double[2];
        double[] p1 = new double[2];
        double[] p2 = new double[2];
        double[] p3 = new double[2];


        // input and assume initial values

        x[0] = fishLocation[0];
        x[1] = fishLocation[1];
        x_dot[0] = fishVelocity[0];
        x_dot[1] = fishVelocity[1]; // Math.cos(Math.abs(fishLocation[0]));
        p1[0] = vehicles.get(0).getLat();
        p1[1] = vehicles.get(0).getLon(); // Math.cos(Math.abs(vehicles.get(0).getLat()));
        p2[0] = vehicles.get(1).getLat();
        p2[1] = vehicles.get(1).getLon(); // Math.cos(Math.abs(vehicles.get(1).getLat()));
        p3[0] = vehicles.get(2).getLat();
        p3[1] = vehicles.get(2).getLon();

        // output
        double[] P0_velocity = new double[2];
        double[] P1_velocity = new double[2];
        double[] P2_velocity = new double[2];
        double[] P3_velocity = new double[2];

        //Let's begin

        //Starting graph. y should be outside and then xy_agents as input and output
        double[][] x_agents = {x,p1,p2,p3};
        double[][] xy_agents = new double[8][2];
        for (int i = 0; i < 4; i++) {
            xy_agents[2*i][0] = x_agents[i][0]*scaling;
            xy_agents[2*i][1] = x_agents[i][1]*scaling;
            xy_agents[2*i+1][0] = (y_agents[i][0])*scaling;
            xy_agents[2*i+1][1] = (y_agents[i][1])*scaling;
        }


        //Converge loop
        double theta = 10; double theta_prev = -10; int count = 0;
        while (Math.abs(theta-theta_prev) > 0.01 || Math.abs(xy_agents[0][0]-xy_agents[1][0]) > 0.01 && count < 20) {
            count++;

            // Define cost matrix
            float[][] CostMat = new float[4][4]; //Init zeros
            double d = 0; float f;
            for (int i = 1; i < 4; i++) {
                d = 10000; //Large number
                f = (float)d;
                CostMat[0][i] = f;
                CostMat[i][0] = f;
            }
            for (int i = 1; i < 4; i++) {
                for (int j = 1; j < 4; j++) {
                    d = w[i-1]*((xy_agents[2*i][0]-xy_agents[2*j+1][0])*(xy_agents[2*i][0]-xy_agents[2*j+1][0])+(xy_agents[2*i][1]-xy_agents[2*j+1][1])*(xy_agents[2*i][1]-xy_agents[2*j+1][1]));
                    f = (float)d;
                    CostMat[i][j] = f;
                }
            }

            // Assign targets to agents
            Hungarian H = new Hungarian(CostMat);
            int[][] H_result = H.execute();
            double[][] xy_temp = new double[8][2];
            for (int i = 0; i < 4; i++) {
                xy_temp[2*i][0] = xy_agents[2*i][0]; //Positions, x
                xy_temp[2*i][1] = xy_agents[2*i][1];
                xy_temp[2*H_result[i][0]+1][0] = xy_agents[2*H_result[i][1]+1][0]; //Targets, y
                xy_temp[2*H_result[i][0]+1][1] = xy_agents[2*H_result[i][1]+1][1];
            }
            xy_agents = xy_temp;
            System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHH");
            for (int i = 0; i<4;i++) {
                for (int j=0;j<2;j++) {
                    System.out.println(H_result[i][j]);
                }
            }


            // Calculate new formation
            double[] tau = {xy_agents[0][0]-xy_agents[1][0],xy_agents[0][1]-xy_agents[1][1]};
            System.out.println("Tau");
            System.out.println(tau[0]); System.out.println(tau[1]);
            double W1 = 0; double W2 = 0;
            for (int i = 1; i < 4; i++) {
                W1 = W1 + w[i-1]*((xy_agents[2*i][0]-xy_agents[0][0])*(xy_agents[2*i+1][0]-xy_agents[1][0])+(xy_agents[2*i][1]-xy_agents[0][1])*(xy_agents[2*i+1][1]-xy_agents[1][1]));
                W2 = W2 + w[i-1]*((xy_agents[2*i][1]-xy_agents[0][1])*(xy_agents[2*i+1][0]-xy_agents[1][0])-(xy_agents[2*i][0]-xy_agents[0][0])*(xy_agents[2*i+1][1]-xy_agents[1][1]));
            }
            theta_prev = theta;
            theta = Math.atan2(W2,W1);
            System.out.println("Theta");
            System.out.println(theta);

            System.out.println("Points");
            for (int i = 0; i<4;i++) {
                for (int j=0;j<2;j++) {
                    System.out.println(xy_agents[2*i+1][j]);
                }
            }

            double[] y_centroid = {0,0};
            for (int i = 1; i<4; i++) {
                y_centroid[0] = y_centroid[0] + xy_agents[2*i+1][0];
                y_centroid[1] = y_centroid[1] + xy_agents[2*i+1][1];
            }
            y_centroid[0] = y_centroid[0]/3;
            y_centroid[1] = y_centroid[1]/3;

            System.out.println("Centroid");
            System.out.println(y_centroid[0]);
            System.out.println(y_centroid[1]);

            //double[][] xy_agents_temp = xy_agents;
            double[][] R = {{Math.cos(theta),-Math.sin(theta)},{Math.sin(theta),Math.cos(theta)}};
            for (int i = 0; i < 4; i++) {
                double valuex = Math.cos(theta)*(xy_agents[2*i+1][0]-y_centroid[0])-Math.sin(theta)*(xy_agents[2*i+1][1]-y_centroid[1]);
                double valuey = Math.sin(theta)*(xy_agents[2*i+1][0]-y_centroid[0])+Math.cos(theta)*(xy_agents[2*i+1][1]-y_centroid[1]);
                xy_agents[2*i+1][0] = valuex;
                xy_agents[2*i+1][1] = valuey;
            }
            System.out.println("Test");
            System.out.println(xy_agents[1][0]-y_centroid[0]);
            System.out.println(xy_agents[1][1]-y_centroid[1]);
            System.out.println(Math.sin(theta));
            System.out.println(Math.cos(theta));
            //System.out.println(xy_agents_temp[1][0]);
            //System.out.println(xy_agents_temp[1][1]);

            //xy_agents = xy_agents_temp;

            System.out.println("Rotation");
            for (int i = 0; i<4;i++) {
                for (int j=0;j<2;j++) {
                    System.out.println(xy_agents[2*i+1][j]);
                }
            }

            for (int i = 0; i < 4; i++) {
                xy_agents[2*i+1][0] = xy_agents[2*i+1][0]+y_centroid[0]+tau[0];
                xy_agents[2*i+1][1] = xy_agents[2*i+1][1]+y_centroid[1]+tau[1];
            }


        }

        // Outputs
        P0_velocity[0] = xy_agents[1][0]/scaling;
        P0_velocity[1] = xy_agents[1][1]/scaling;
        P1_velocity[0] = xy_agents[3][0]/scaling;
        P1_velocity[1] = xy_agents[3][1]/scaling;
        P2_velocity[0] = xy_agents[5][0]/scaling;
        P2_velocity[1] = xy_agents[5][1]/scaling;
        P3_velocity[0] = xy_agents[7][0]/scaling;
        P3_velocity[1] = xy_agents[7][1]/scaling;
        double[] rvalues = new double[]{P0_velocity[0], P0_velocity[1], P1_velocity[0], P1_velocity[1], P2_velocity[0], P2_velocity[1], P3_velocity[0], P3_velocity[1]};
        return rvalues;


    }
}
