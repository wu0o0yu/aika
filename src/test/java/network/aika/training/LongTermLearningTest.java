package network.aika.training;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;
import network.aika.training.LongTermLearning.Config;
import org.junit.Test;

public class LongTermLearningTest {


    @Test
    public void testLongTermPotentiationAndLongTermDepression() {
        Model m = new Model();

        Neuron nA = m.createNeuron("A");
        Neuron nB = m.createNeuron("B");

        Document doc = m.createDocument("Bla ");

        nA.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 3)
                        .setValue(1.0)
                        .setTargetValue(null)
                        .setFired(0)
        );
        nB.addInput(doc,
                new Activation.Builder()
                        .setRange(0, 3)
                        .setValue(0.5)
                        .setTargetValue(null)
                        .setFired(1)
        );

        LongTermLearning.train(doc,
                new Config()
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
                                    SynapseEvaluation.DeleteMode.DELETE_IF_SIGN_CHANGES
                            );
                        })
        );
    }

}
