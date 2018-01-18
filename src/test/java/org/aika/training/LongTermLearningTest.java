package org.aika.training;

import org.aika.neuron.INeuron;
import org.junit.Test;

public class LongTermLearningTest {


    @Test
    public void testLongTermPotentiation() {

    }


    @Test
    public void testLongTermDepression() {

    }


    public static double sig(INeuron in, INeuron on) {
        if(on.type == INeuron.Type.INHIBITORY) {
            return 1.0;
        }

        int iFreq = Math.max(1, ((NeuronStatistic) in.statistic).frequency);
        int oFreq = Math.max(1, ((NeuronStatistic) on.statistic).frequency);
        return Math.pow(iFreq * oFreq, -0.2);
    }

}
