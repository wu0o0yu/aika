package org.aika.training;


import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

public class SynapseSignificance {


    public static double sig(Synapse s) {
        return sig(s.input.get(), s.output.get());
    }


    public static double sig(Activation iAct, Activation oAct) {
        return sig(iAct.key.node.neuron.get(), oAct.key.node.neuron.get());
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
