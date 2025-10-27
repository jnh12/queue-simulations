import java.util.Random;
import java.util.*;
import java.io.*; // <-- added
public class MMNK_timer {


    static final Random RNG = new Random("jad".hashCode());
    static double lambda = 0.01;
    static final double mu     = 1;
    static final int iterations = 1000000;
    static final int QMAX = 5;
    static final double theta = 0.5; // rate for deadlines
    static final int N_SERVERS = 1;


    static double expRate(double rate) {
        double u = RNG.nextDouble();
        return -Math.log(1.0 - u) / rate;
    }

    public static class State{
        double MC;
        double CLA;
        double[] CL4         = new double[N_SERVERS];
        boolean[] busy       = new boolean[N_SERVERS];
        double departed;
        Deque<Integer> Q = new ArrayDeque<>();

        double lastMC;
        double areaNsys = 0.0;
        double areaNq   = 0.0;
        double totalSys = 0.0;
        double totalWait = 0.0;
        Deque<Double> arrTimes = new ArrayDeque<>();


        double arrived = 0.0;
        double lost    = 0.0;

        Deque<Double> deadlineTimes = new ArrayDeque<>();
        double expiredAny = 0.0;
        double[] curArr      = new double[N_SERVERS];   // per-server arrival time
        double[] curSrvStart = new double[N_SERVERS];   // per-server service start
        double[] curDeadline = new double[N_SERVERS];   // per-server absolute deadline

    }

    public static void main(String[] args) {
        String csvPath = "mm1_results.csv";
        try (PrintWriter out = new PrintWriter(new FileWriter(csvPath))) {
            out.println("lambda,mu,rho,sim_L,exp_L,sim_W,exp_W,sim_Lq,exp_Lq,sim_Wq,exp_Wq,sim_time_T,departed,theta,p_expired");

            for (int k = 0; k < 90; k++) {

                State s = new State();
                s.MC = 0;
                s.CLA = s.MC + expRate(lambda);
                s.lastMC = 0.0;

                for (int sv = 0; sv < N_SERVERS; sv++) {
                    s.CL4[sv]         = Double.NaN;
                    s.busy[sv]        = false;
                    s.curArr[sv]      = Double.NaN;
                    s.curSrvStart[sv] = Double.NaN;
                    s.curDeadline[sv] = Double.NaN;
                }

                s.Q.clear();
                s.arrTimes.clear();
                s.deadlineTimes.clear();
                s.arrived = 0.0;
                s.lost = 0.0;
                s.expiredAny = 0.0;
                s.departed = 0.0;
                s.areaNsys = 0.0;
                s.areaNq = 0.0;
                s.totalSys = 0.0;
                s.totalWait = 0.0;

                lambda += 0.01;

                for (int i = 0; i <= iterations; i++) {
                    double nextArrTime = (Double.isNaN(s.CLA) ? Double.POSITIVE_INFINITY : s.CLA);

                    double nextDepTime = Double.POSITIVE_INFINITY;
                    int depServer = -1;
                    for (int sv = 0; sv < N_SERVERS; sv++) {
                        if (!Double.isNaN(s.CL4[sv]) && s.CL4[sv] < nextDepTime) {
                            nextDepTime = s.CL4[sv];
                            depServer = sv;
                        }
                    }

                    if (nextArrTime == Double.POSITIVE_INFINITY && nextDepTime == Double.POSITIVE_INFINITY) {
                        break;
                    }

                    int busyCount = 0;
                    for (int sv = 0; sv < N_SERVERS; sv++) if (s.busy[sv]) busyCount++;

                    if (nextDepTime <= nextArrTime) {
                        double dt = nextDepTime - s.lastMC;
                        s.areaNsys += dt * (s.Q.size() + busyCount);
                        s.areaNq   += dt *  s.Q.size();
                        s.lastMC = nextDepTime;
                        s.MC     = nextDepTime;
                        handleDeparture(s, depServer);   // NOTE: new signature
                    } else {
                        double dt = nextArrTime - s.lastMC;
                        s.areaNsys += dt * (s.Q.size() + busyCount);
                        s.areaNq   += dt *  s.Q.size();
                        s.lastMC = nextArrTime;
                        s.MC     = nextArrTime;
                        handleArrival(s);
                    }



                    //printRow(s);
                }

                double T = s.lastMC;
                double Ns_hat = s.areaNsys / T;
                double Nq_hat = s.areaNq / T;
                double Ds_hat = s.totalSys / s.departed;
                double Dq_hat = s.totalWait / s.departed;
                double p_block     = (s.arrived > 0.0) ? (s.lost / s.arrived) : Double.NaN;
                double lambda_eff  = s.departed / T;
                double p_expired = (s.departed > 0.0) ? (s.expiredAny / s.departed) : Double.NaN;



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
                System.out.printf(Locale.US, "%-42s %15.6f%n", "Blocking probability", p_block);
                System.out.printf(Locale.US, "%-42s %15.6f%n", "Effective arrival rate", lambda_eff);
                System.out.printf(Locale.US, "%-42s %15.6f%n", "Expiry probability (served)", p_expired);


                out.printf(Locale.US,
                        "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.0f,%.6f,%.6f%n",
                        lambda, mu, rho,
                        Ns_hat, L_exp,
                        Ds_hat, W_exp,
                        Nq_hat, Lq_exp,
                        Dq_hat, Wq_exp,
                        T, s.departed,
                        theta, p_expired
                );

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void handleArrival(State s) {
        s.CLA = s.MC + expRate(lambda);
        s.arrived++;

        int idle = -1;
        for (int sv = 0; sv < N_SERVERS; sv++) {
            if (!s.busy[sv]) { idle = sv; break; }
        }

        if (idle != -1) {
            s.busy[idle]        = true;
            s.curArr[idle]      = s.MC;
            s.curSrvStart[idle] = s.MC;
            s.curDeadline[idle] = s.MC + expRate(theta);
            s.CL4[idle]         = s.MC + expRate(mu);
            return;
        }

        if (s.Q.size() >= QMAX) {
            s.lost++;
            return;
        }

        s.Q.addLast(1);
        s.arrTimes.addLast(s.MC);
        s.deadlineTimes.addLast(s.MC + expRate(theta));
    }

    static void handleDeparture(State s, int sv) {
        if (!Double.isNaN(s.curDeadline[sv]) && s.curDeadline[sv] <= s.MC) {
            s.expiredAny++;
        }
        s.departed++;

        if (!Double.isNaN(s.curArr[sv]) && !Double.isNaN(s.curSrvStart[sv])) {
            s.totalSys  += (s.MC - s.curArr[sv]);
            s.totalWait += (s.curSrvStart[sv] - s.curArr[sv]);
        }

        if (!s.Q.isEmpty()) {
            s.Q.removeFirst();
            double a = s.arrTimes.removeFirst();
            double d = s.deadlineTimes.removeFirst();
            s.curArr[sv]      = a;
            s.curSrvStart[sv] = s.MC;
            s.curDeadline[sv] = d;
            s.CL4[sv]         = s.MC + expRate(mu);
            s.busy[sv]        = true;
        } else {
            s.busy[sv]        = false;
            s.CL4[sv]         = Double.NaN;
            s.curArr[sv]      = Double.NaN;
            s.curSrvStart[sv] = Double.NaN;
            s.curDeadline[sv] = Double.NaN;
        }
    }

}



