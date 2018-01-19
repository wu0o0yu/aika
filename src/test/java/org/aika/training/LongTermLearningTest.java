package org.aika.training;

import org.aika.Model;
import org.aika.Neuron;
import org.aika.corpus.Document;
import org.aika.corpus.Range;
import org.aika.neuron.INeuron;
import org.aika.neuron.Synapse;
import org.aika.training.LongTermLearning.TrainConfig;
import org.junit.Test;

public class LongTermLearningTest {


    @Test
    public void testLongTermPotentiationAndLongTermDepression() {
        Model m = new Model();

        Neuron nA = m.createNeuron("A");
        Neuron nB = m.createNeuron("B");

        Document doc = m.createDocument("Bla ");

        nA.addInput(doc, 0, 3, 1.0, null, 0);
        nB.addInput(doc, 0, 3, 0.5, null, 1);

        LongTermLearning.train(doc,
                new TrainConfig()
                        .setLTPLearnRate(0.5)
                        .setLTDLearnRate(0.5)
                        .setBeta(0.5)
                        .setSynapseEvaluation((s, iAct, oAct) -> {
                            return new SynapseEvaluation.Result(
                                    new Synapse.Key(
                                            iAct.rounds.getLast().fired >= oAct.rounds.getLast().fired,
                                            null,
                                            null,
                                            Range.Relation.NONE,
                                            Range.Output.DIRECT
                                    ),
                                    1.0,
                                    true
                            );
                        })
        );
    }

}
