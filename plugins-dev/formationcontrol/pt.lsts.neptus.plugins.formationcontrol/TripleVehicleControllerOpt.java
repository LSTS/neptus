package pt.lsts.neptus.plugins.formationcontrol;

import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import javax.vecmath.Vector2d;
import java.util.ArrayList;

public class TripleVehicleControllerOpt {

    public static double[] calculateDesiredVelocity(double[] fishLocation, double[] fishVelocity,
                                                    ArrayList<Vehicle> vehicles, double[] w, double R,
                                                    double T, double gamma_T, double gamma_ij) {

        double lonlatToM = 111300*180/Math.PI;
        double r = R*lonlatToM;
        double d = r*Math.sqrt(3);
        double currentLat = fishLocation[1];

        double[] x_target = new double[2];
        double[] x = {0,0};
        double[] x_dot = new double[2];
        double[] p1_l = new double[2];
        double[] p1 = new double[2];
        double[] p2_l = new double[2];
        double[] p2 = new double[2];
        double[] p3_l = new double[2];
        double[] p3 = new double[2];

        //Translate all to meters, target x at (0,0).
        x_target[0] = fishLocation[0];
        x_target[1] = fishLocation[1];
        x_dot[0] = fishVelocity[0];
        x_dot[1] = fishVelocity[1];
        p1_l[0] = vehicles.get(0).getLon();
        p1_l[1] = vehicles.get(0).getLat();
        p1[0] = (p1_l[0]-x_target[0])*lonlatToM*Math.cos(currentLat);
        p1[1] = (p1_l[1]-x_target[1])*lonlatToM;
        p2_l[0] = vehicles.get(1).getLon();
        p2_l[1] = vehicles.get(1).getLat();
        p2[0] = (p2_l[0]-x_target[0])*lonlatToM*Math.cos(currentLat);
        p2[1] = (p2_l[1]-x_target[1])*lonlatToM;
        p3_l[0] = vehicles.get(2).getLon();
        p3_l[1] = vehicles.get(2).getLat();
        p3[0] = (p3_l[0]-x_target[0])*lonlatToM*Math.cos(currentLat);
        p3[1] = (p3_l[1]-x_target[1])*lonlatToM;

        double[] P1_velocity = new double[2];
        double[] P2_velocity = new double[2];
        double[] P3_velocity = new double[2];

        //CODE

        final Vector2D[] observedPoints = new Vector2D[] {
                new Vector2D(x[0],x[1]),
                new Vector2D(p1[0],p1[1]),
                new Vector2D(p2[0],p2[1]),
                new Vector2D(p3[0],p3[1])
        };

        MultivariateJacobianFunction function = new MultivariateJacobianFunction() {
            @Override
            public Pair<RealVector, RealMatrix> value(RealVector point) {
                Vector2D x1 = new Vector2D(point.getEntry(0), point.getEntry(1));
                Vector2D x2 = new Vector2D(point.getEntry(2), point.getEntry(3));
                Vector2D x3 = new Vector2D(point.getEntry(4), point.getEntry(5));

                RealVector value = new ArrayRealVector(1);
                RealMatrix jacobian = new Array2DRowRealMatrix(1,6);

                double alpha1 = Vector2D.distanceSq(x1,observedPoints[0]);
                double alpha2 = Vector2D.distanceSq(x2,observedPoints[0]);
                double alpha3 = Vector2D.distanceSq(x3,observedPoints[0]);

                double delta1 = Vector2D.distanceSq(x1,observedPoints[1]);
                double delta2 = Vector2D.distanceSq(x2,observedPoints[2]);
                double delta3 = Vector2D.distanceSq(x3,observedPoints[3]);

                double beta1 = Vector2D.distanceSq(x1,x2);
                double beta2 = Vector2D.distanceSq(x1,x3);
                double beta3 = Vector2D.distanceSq(x2,x3);

                double firstTerm = 1/Math.pow(T,2)*(w[0]*delta1+w[1]*delta2+w[2]*delta3);
                double secondTerm = gamma_ij*(Math.pow(beta1-Math.pow(d,2),2)/beta1+Math.pow(beta2-Math.pow(d,2),2)/beta2+Math.pow(beta3-Math.pow(d,2),2)/beta3);
                double thirdTerm = gamma_T*(Math.pow(alpha1-Math.pow(r,2),2)/alpha1+Math.pow(alpha2-Math.pow(r,2),2)/alpha2+Math.pow(alpha3-Math.pow(r,2),2)/alpha3);
                double model = firstTerm+secondTerm+thirdTerm;
                value.setEntry(0,model);

                double derive1 = 2*w[0]/Math.pow(T,2)*(x1.getX()-observedPoints[1].getX())+
                        gamma_T*(Math.pow(alpha1,2)-Math.pow(r,4))/Math.pow(alpha1,2)*(x1.getX()-observedPoints[0].getX())+
                        gamma_ij*(Math.pow(beta1,2)-Math.pow(d,4))/Math.pow(beta1,2)*(x1.getX()-x2.getX())+
                        gamma_ij*(Math.pow(beta2,2)-Math.pow(d,4))/Math.pow(beta2,2)*(x1.getX()-x3.getX());
                jacobian.setEntry(0,0, derive1);
                double derive2 = 2*w[0]/Math.pow(T,2)*(x1.getY()-observedPoints[1].getY())+
                        gamma_T*(Math.pow(alpha1,2)-Math.pow(r,4))/Math.pow(alpha1,2)*(x1.getY()-observedPoints[0].getY())+
                        gamma_ij*(Math.pow(beta1,2)-Math.pow(d,4))/Math.pow(beta1,2)*(x1.getY()-x2.getY())+
                        gamma_ij*(Math.pow(beta2,2)-Math.pow(d,4))/Math.pow(beta2,2)*(x1.getY()-x3.getY());
                jacobian.setEntry(0,1, derive2);
                double derive3 = 2*w[1]/Math.pow(T,2)*(x2.getX()-observedPoints[2].getX())+
                        gamma_T*(Math.pow(alpha2,2)-Math.pow(r,4))/Math.pow(alpha2,2)*(x2.getX()-observedPoints[0].getX())-
                        gamma_ij*(Math.pow(beta1,2)-Math.pow(d,4))/Math.pow(beta1,2)*(x1.getX()-x2.getX())+
                        gamma_ij*(Math.pow(beta3,2)-Math.pow(d,4))/Math.pow(beta3,2)*(x2.getX()-x3.getX());
                jacobian.setEntry(0,2, derive3);
                double derive4 = 2*w[1]/Math.pow(T,2)*(x2.getY()-observedPoints[2].getY())+
                        gamma_T*(Math.pow(alpha2,2)-Math.pow(r,4))/Math.pow(alpha2,2)*(x2.getY()-observedPoints[0].getY())-
                        gamma_ij*(Math.pow(beta1,2)-Math.pow(d,4))/Math.pow(beta1,2)*(x1.getY()-x2.getY())+
                        gamma_ij*(Math.pow(beta3,2)-Math.pow(d,4))/Math.pow(beta3,2)*(x2.getY()-x3.getY());
                jacobian.setEntry(0,3, derive4);
                double derive5 = 2*w[2]/Math.pow(T,2)*(x3.getX()-observedPoints[3].getX())+
                        gamma_T*(Math.pow(alpha3,2)-Math.pow(r,4))/Math.pow(alpha3,2)*(x3.getX()-observedPoints[0].getX())-
                        gamma_ij*(Math.pow(beta2,2)-Math.pow(d,4))/Math.pow(beta2,2)*(x1.getX()-x3.getX())-
                        gamma_ij*(Math.pow(beta3,2)-Math.pow(d,4))/Math.pow(beta3,2)*(x2.getX()-x3.getX());
                jacobian.setEntry(0,4, derive5);
                double derive6 = 2*w[2]/Math.pow(T,2)*(x3.getY()-observedPoints[3].getY())+
                        gamma_T*(Math.pow(alpha3,2)-Math.pow(r,4))/Math.pow(alpha3,2)*(x3.getY()-observedPoints[0].getY())-
                        gamma_ij*(Math.pow(beta2,2)-Math.pow(d,4))/Math.pow(beta2,2)*(x1.getY()-x3.getY())-
                        gamma_ij*(Math.pow(beta3,2)-Math.pow(d,4))/Math.pow(beta3,2)*(x2.getY()-x3.getY());
                jacobian.setEntry(0,5, derive6);

                return new Pair<RealVector, RealMatrix>(value, jacobian);
            }
        };

        double[] minimumValue = {0};
        LeastSquaresProblem problem = new LeastSquaresBuilder().
                start(new double[] {p1[0],p1[1],p2[0],p2[1],p3[0],p3[1]}).
                model(function).target(minimumValue).lazyEvaluation(false).
                maxEvaluations(1000).maxIterations(1000).build();

        LeastSquaresOptimizer.Optimum optimum = new LevenbergMarquardtOptimizer().optimize(problem);
        double[] result = {optimum.getPoint().getEntry(0),optimum.getPoint().getEntry(1),
                optimum.getPoint().getEntry(2),optimum.getPoint().getEntry(3),
                optimum.getPoint().getEntry(4),optimum.getPoint().getEntry(5)};

        P1_velocity[0] = result[0]/lonlatToM/Math.cos(currentLat)+x_target[0]; P1_velocity[1] = result[1]/lonlatToM+x_target[1];
        P2_velocity[0] = result[2]/lonlatToM/Math.cos(currentLat)+x_target[0]; P2_velocity[1] = result[3]/lonlatToM+x_target[1];
        P3_velocity[0] = result[4]/lonlatToM/Math.cos(currentLat)+x_target[0]; P3_velocity[1] = result[5]/lonlatToM+x_target[1];

        double[] rvalues = new double[]{P1_velocity[0], P1_velocity[1], P2_velocity[0], P2_velocity[1], P3_velocity[0], P3_velocity[1]};
        return rvalues;
    }
}
