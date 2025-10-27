import java.util.*;


public class DD1 {


    static double uptime = 10;
    static double serviceTime = 5;

    public static class State{
        int m = 3;
        double MC;
        double[] CL;
        double CL4;
        double n;
        boolean repairman;
        Integer inService = null;
        Deque<Integer> Q = new ArrayDeque<>();
    }

    public static void main(String[] args) {


        State s = new State();
        s.MC = 0;
        s.CL = new double[s.m];
        s.CL[0] = 1; s.CL[1] = 4; s.CL[2] = 9;
        s.CL4 = Double.NaN;
        s.n = 0;
        s.repairman = false;


        for(int i =0; i<=7; i++){
            int nextArrIndex = MinCl(s.CL);
            double nextArrTime = (nextArrIndex == -1) ? Double.POSITIVE_INFINITY : s.CL[nextArrIndex];
            double nextDepTime = Double.isNaN(s.CL4) ? Double.POSITIVE_INFINITY : s.CL4;

            if (nextArrTime == Double.POSITIVE_INFINITY && nextDepTime == Double.POSITIVE_INFINITY) {
                break;
            }

            if(nextDepTime<=nextArrTime){  //DEPARTURE FIRST always
                s.MC = nextDepTime;
                handleDeparture(s, uptime, serviceTime);
            }
            else{
                s.MC = nextArrTime;
                handleArrival(s, uptime, serviceTime, nextArrIndex);
            }

            printRow(s);
        }

    }

    static int MinCl(double[] CL) {
        double best = Double.POSITIVE_INFINITY;
        int idx = -1;
        for (int i = 0; i < CL.length; i++) {
            double v = CL[i];
            if (!Double.isNaN(v) && v < best) {
                best = v;
                idx = i;
            }
        }
        return idx;
    }


    static void handleDeparture(State s,double uptime,double serviceTime){
        s.CL[s.inService] = s.MC + uptime;
        s.n = s.n - 1;

        if(!s.Q.isEmpty()){
            s.inService = s.Q.removeFirst();
            s.CL4 = s.MC + serviceTime;
            s.repairman = true;
        }
        else{
            s.inService = null;
            s.CL4 = Double.NaN;
            s.repairman = false;
        }
    }

    static void handleArrival(State s, double uptime,double serviceTime, int index){
        s.CL[index] = Double.NaN;
        s.n = s.n + 1;

        if(!s.repairman){
            s.repairman = true;
            s.inService = index;
            s.CL4 = s.MC + serviceTime;
        }
        else {
            s.Q.addLast(index);
        }
    }

    static void printRow(State s) {
        StringBuilder sb = new StringBuilder();
        sb.append(trim(s.MC)).append('\t');

        for (int i = 0; i < s.CL.length; i++) {
            sb.append(Double.isNaN(s.CL[i]) ? "-" : trim(s.CL[i]));
            sb.append('\t');
        }

        String cl4 = Double.isNaN(s.CL4) ? "-" : trim(s.CL4);
        String R = s.repairman ? "busy" : "idle";
        sb.append(cl4).append('\t').append(R);

        System.out.println(sb.toString());
    }

    static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long)Math.rint(v));
        return String.format(Locale.US, "%.4f", v);
    }

}



