package org.aika.training;


import org.aika.corpus.Document;
import org.aika.neuron.Activation;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class LongTermPotAndDep {

    public static double LTP_LEARN_RATE = 0.1;
    public static double LTD_LEARN_RATE = 0.1;

    public static double BETA = 0.5;


    public static void train(Document doc) {
        for(INeuron n: doc.finallyActivatedNeurons) {
            for(Activation act: n.getFinalActivations(doc)) {
                longTermPotentiation(doc, n, act);
                longTermDepression(doc, n, act, false);
                longTermDepression(doc, n, act, true);
            }
        }
    }


    private static void longTermPotentiation(Document doc, INeuron n, Activation act) {
        double iaSum = 0.0;
        for(Activation.SynapseActivation sa: act.getFinalInputActivations()) {
            if(!sa.synapse.isNegative()) {
                iaSum += sa.input.getFinalState().value * sa.synapse.weight;
            }
        }

        // n.posDirSum wird bei Oder-Neuronen sehr groß.
        double x = LTP_LEARN_RATE * (1.0 - act.getFinalState().value) * (iaSum / (n.posDirSum + n.posRecSum));

        act.getFinalInputActivations().forEach(sa -> {
            double sDelta = sa.input.getFinalState().value * x * SynapseSignificance.sig(sa.synapse);

            sa.synapse.weightDelta += (float) sDelta;
            n.bias -= BETA * sDelta;
            assert !Double.isNaN(n.bias);
        });

        doc.notifyWeightsModified(n, n.inputSynapses.values());
    }


    private static void longTermDepression(Document doc, INeuron n, Activation act, boolean dir) {
        Set<Synapse> actSyns = new TreeSet<>(dir ? Synapse.OUTPUT_SYNAPSE_COMP : Synapse.INPUT_SYNAPSE_COMP);
        (dir ? act.getFinalOutputActivations() : act.getFinalInputActivations()).forEach(sa -> actSyns.add(sa.synapse));

        (dir ? n.outputSynapses : n.inputSynapses).values().stream()
                .filter(s -> !s.isNegative() && !actSyns.contains(s))
                .forEach(s -> {
                    s.weightDelta -= (float) (LTD_LEARN_RATE * act.getFinalState().value * SynapseSignificance.sig(s));

                    if (dir) {
                        doc.notifyWeightsModified(s.output.get(), Collections.singletonList(s));
                    }
                });

        if(!dir) {
            doc.notifyWeightsModified(n, n.inputSynapses.values());
        }
    }
}
