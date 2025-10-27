import java.util.Random;
import java.util.*;
import java.io.*; // <-- added


public class MM1 {


    static final Random RNG = new Random("jad".hashCode());
    static double lambda = 0.01;
    static final double mu     = 1;
    static final int iterations = 1000000;


    static double expRate(double rate) {
        double u = RNG.nextDouble();
        return -Math.log(1.0 - u) / rate;
    }


    public static class State{
        double MC;
        double CLA;
        double CL4; //departure time
        double departed;
        boolean repairman; //if true means busy
        Deque<Integer> Q = new ArrayDeque<>();

        double lastMC;
        double areaNsys = 0.0;
        double areaNq   = 0.0;
        double totalSys = 0.0;
        double totalWait = 0.0;
        Deque<Double> arrTimes = new ArrayDeque<>();
        double curArr = Double.NaN;
        double curSrvStart = Double.NaN;
    }

    public static void main(String[] args) {
        String csvPath = "mm1_results.csv";
        try (PrintWriter out = new PrintWriter(new FileWriter(csvPath))) {
            out.println("lambda,mu,rho,sim_L,exp_L,sim_W,exp_W,sim_Lq,exp_Lq,sim_Wq,exp_Wq,sim_time_T,departed");

            for (int k = 0; k < 90; k++) {

                State s = new State();
                s.MC = 0;
                s.CLA = s.MC + expRate(lambda);
                s.CL4 = Double.NaN;
                s.repairman = false;
                s.Q.clear();
                s.lastMC = 0.0;

                lambda += 0.01;

                for (int i = 0; i <= iterations; i++) {
                    double nextArrTime;
                    if (Double.isNaN(s.CLA)) {
                        nextArrTime = Double.POSITIVE_INFINITY;
                    } else {
                        nextArrTime = s.CLA;
                    }

                    double nextDepTime;
                    if (Double.isNaN(s.CL4)) {
                        nextDepTime = Double.POSITIVE_INFINITY;
                    } else {
                        nextDepTime = s.CL4;
                    }
                    if (nextArrTime == Double.POSITIVE_INFINITY && nextDepTime == Double.POSITIVE_INFINITY) {
                        break;
                    }
                    if (nextDepTime <= nextArrTime) {
                        double dt = nextDepTime - s.lastMC;
                        s.areaNsys += dt * (s.Q.size() + (s.repairman ? 1 : 0));
                        s.areaNq += dt * (s.Q.size());
                        s.lastMC = nextDepTime;
                        s.MC = nextDepTime;
                        handleDeparture(s);
                    } else {
                        double dt = nextArrTime - s.lastMC;
                        double busy = s.repairman ? 1.0 : 0.0;
                        s.areaNsys += dt * (s.Q.size() + busy);
                        s.areaNq += dt * s.Q.size();
                        s.lastMC = nextArrTime;
                        s.MC = nextArrTime;
                        handleArrival(s);
                    }

                }

                double T = s.lastMC;
                double Ns_hat = s.areaNsys / T;
                double Nq_hat = s.areaNq / T;
                double Ds_hat = s.totalSys / s.departed;
                double Dq_hat = s.totalWait / s.departed;


                double rho = lambda / mu;
                double L_exp = rho / (1.0 - rho);
                double W_exp = 1.0 / (mu - lambda);
                double Lq_exp = (rho * rho) / (1.0 - rho);
                double Wq_exp = rho / (mu - lambda);


                System.out.printf(Locale.US, "Parameters: λ=%.4f, μ=%.4f, ρ=%.4f%n", lambda, mu, rho);
                System.out.printf(Locale.US, "%-42s %15s %15s%n", "", "Simulated", "Expected");
                System.out.printf(Locale.US, "%-42s %15.6f %15.6f%n", "Average number of packets in the system", Ns_hat, L_exp);
                System.out.printf(Locale.US, "%-42s %15.6f %15.6f%n", "Average system delay per packet (time units)", Ds_hat, W_exp);
                System.out.printf(Locale.US, "%-42s %15.6f %15.6f%n", "Average number of packets in the queue", Nq_hat, Lq_exp);
                System.out.printf(Locale.US, "%-42s %15.6f %15.6f%n", "Average queueing delay per packet (time units)", Dq_hat, Wq_exp);

                out.printf(Locale.US,
                        "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.0f%n",
                        lambda, mu, rho,
                        Ns_hat, L_exp,
                        Ds_hat, W_exp,
                        Nq_hat, Lq_exp,
                        Dq_hat, Wq_exp,
                        T, s.departed);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void handleArrival(State s) {
        s.CLA = s.MC + expRate(lambda);     //posisson arrivals

        if (!s.repairman) {
            s.repairman = true;
            s.curArr = s.MC;
            s.curSrvStart = s.MC;
            s.CL4 = s.MC + expRate(mu);  //exponential service
        } else {
            s.Q.addLast(1);
            s.arrTimes.addLast(s.MC);
        }
    }


    static void handleDeparture(State s) {
        s.departed++;

        if (!Double.isNaN(s.curArr) && !Double.isNaN(s.curSrvStart)) {
            s.totalSys  += (s.MC - s.curArr);
            s.totalWait += (s.curSrvStart - s.curArr);
        }

        if (!s.Q.isEmpty()) {
            s.Q.removeFirst();
            Double a = s.arrTimes.removeFirst();
            s.curArr = a;
            s.curSrvStart = s.MC;
            s.CL4 = s.MC + expRate(mu);
            s.repairman = true;
        } else {
            s.repairman = false;
            s.CL4 = Double.NaN;
            s.curArr = Double.NaN;
            s.curSrvStart = Double.NaN;
        }
    }

}



