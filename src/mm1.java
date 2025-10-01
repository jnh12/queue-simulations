import java.util.Random;
import java.util.*;


public class mm1 {


    static final Random RNG = new Random(12345);
    static final double lambda = 0.15;
    static final double mu     = 0.20;

    static double expRate(double rate) {
        double u = RNG.nextDouble();
        return -Math.log(1.0 - u) / rate;
    }


    public static class State{
        double MC;
        double CLA;
        double CL4;
        double departed;
        boolean repairman; //if true means busy
        Deque<Integer> Q = new ArrayDeque<>();
    }

    public static void main(String[] args) {

        State s = new State();
        s.MC = 0;
        s.CLA = s.MC + expRate(lambda);
        s.CL4 = Double.NaN;
        s.repairman = false;
        s.Q.clear();


        for(int i =0; i<=15; i++){
            double nextArrTime;
            if (Double.isNaN(s.CLA)) {
                nextArrTime = Double.POSITIVE_INFINITY;
            }
            else {
                nextArrTime = s.CLA;
            }

            double nextDepTime;
            if (Double.isNaN(s.CL4)) {
                nextDepTime = Double.POSITIVE_INFINITY;
            }
            else {
                nextDepTime = s.CL4;
            }

            if (nextArrTime == Double.POSITIVE_INFINITY && nextDepTime == Double.POSITIVE_INFINITY) {
                break;
            }

            if (nextDepTime <= nextArrTime) {
                s.MC = nextDepTime;
                handleDeparture(s);
            } else {
                s.MC = nextArrTime;
                handleArrival(s);
            }

            printRow(s);
        }

    }

    static void handleArrival(State s) {
        s.CLA = s.MC + expRate(lambda);     //posisson arrivals

        if (!s.repairman) {
            s.repairman = true;
            s.CL4 = s.MC + expRate(mu);  //exponential service
        } else {
            s.Q.addLast(1);
        }
    }

    static void handleDeparture(State s) {
        s.departed++;
        if (!s.Q.isEmpty()) {
            s.Q.removeFirst();
            s.CL4 = s.MC + expRate(mu);
            s.repairman = true;
        } else {
            s.repairman = false;
            s.CL4 = Double.NaN;
        }
    }


    static void printRow(State s) {
        String cla = Double.isNaN(s.CLA) ? "-" : trim(s.CLA);
        String cl4 = Double.isNaN(s.CL4) ? "-" : trim(s.CL4);
        String R = s.repairman ? "busy" : "idle";
        System.out.printf("%s\t%s\t%s\t%d\t%s%n",
                trim(s.MC), cla, cl4, s.Q.size(), R);
    }


    static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format(Locale.US, "%.4f", v);
    }


}



