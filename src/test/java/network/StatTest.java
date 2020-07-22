package network;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.junit.jupiter.api.Test;

public class StatTest {

    @Test
    public void testStandardDeviation() {
        experimentStandardDeviation(10);
        experimentStandardDeviation(100);
        experimentStandardDeviation(1000);
        experimentStandardDeviation(10000);
        experimentStandardDeviation(100000);
    }

    public void experimentStandardDeviation(double N) {
        compute(0.001, N);
        compute(0.01, N);
        compute(0.1, N);
        compute(0.5, N);
        compute(0.9, N);
        compute(0.99, N);
        compute(0.999, N);
        System.out.println();
    }

    private void compute(double p, double N) {
        double f = p * N;
        BetaDistribution dist = new BetaDistribution(f, N - f);

        double exp = f / N;
        double var = dist.getNumericalVariance();
        double sd = Math.sqrt(var);

        System.out.println("f:" + f + " N:" + N + " exp:" + exp + " var:" + var + " sd:" + sd);
    }
}
