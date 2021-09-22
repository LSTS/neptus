package pt.lsts.neptus.plugins.formationcontrol;//package elias.kth.MyFirstPlugin;

/**
 * Created by elias on 8/16/16.
 */
public class SingleVehicleController {
    public static double[] calculateDesiredVelocity(double[] fishLocation, double[] fishVelocity, double[] vehicleLocation,
                                                    double d, double delta, double alpha, boolean flipXY) {
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

        // input and assume initial values
        double[] p1;
        double[] x_dot;
        double[] x;

        if (flipXY)
        {
            x = new double[] {fishLocation[1],fishLocation[0]};      // Fish location
            x_dot = new double[] {fishVelocity[1],fishVelocity[0]};  // Fish velocity
            p1 = new double[] {vehicleLocation[1],vehicleLocation[0]};// elias.kth.MyFirstPlugin.Vehicle location
        }
        else {
            x = new double[] {fishLocation[0],fishLocation[1]};      // Fish location
            x_dot = new double[] {fishVelocity[0],fishVelocity[1]};  // Fish velocity
            p1 = new double[] {vehicleLocation[0],vehicleLocation[1]};// elias.kth.MyFirstPlugin.Vehicle location
        }

        // output
        double[] P1_velocity = new double[2];

        //Let's begin

        double d1 = (x[0]-p1[0])*(x[0]-p1[0]) + (x[1]-p1[1])*(x[1]-p1[1]);
        d1 = Math.sqrt(d1);

        // for Agent 1
        double[] phi1 = new double[2];
        double[] phi1_bar = new double[2];
        phi1[0] = (x[0] - p1[0]);
        phi1[1] = (x[1] - p1[1]);
        phi1_bar[0] = phi1[1] ;
        phi1_bar[1] = -phi1[0];

        double Dist1;
        Dist1 = Math.abs(d1-d);
        if (Dist1 > delta){
            // 5e5 was good for Caravela in the first term // 10e5 was good enough for anka
            P1_velocity[0] = x_dot[0] + 10e7*(d1*d1-d*d)*phi1[0] + 2*alpha*phi1_bar[0];
            P1_velocity[1] = x_dot[1] + 10e7*(d1*d1-d*d)*phi1[1] + 2*alpha*phi1_bar[1];
            System.out.println("1eX*(d1*d1-d*d)*phi1[0] = " + (d1*d1-d*d)*phi1[0] + " ALPHA*phi1_bar[0] = " + alpha*phi1_bar[1]);
        }
        else {
            double beta = 0;
            P1_velocity[0] = (x_dot[0] + 5e7*(d1*d1-d*d)*phi1[0] + 2*(alpha)*phi1_bar[0] ) ;
            P1_velocity[1] = (x_dot[1] + 5e7*(d1*d1-d*d)*phi1[1] + 2*(alpha)*phi1_bar[1] ) ;
        }

        return P1_velocity;
    }
}

