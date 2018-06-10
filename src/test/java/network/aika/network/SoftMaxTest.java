package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Range;
import network.aika.neuron.activation.SearchNode;
import org.junit.Test;

import java.util.Collections;

import static network.aika.neuron.activation.Range.Relation.EQUALS;

public class SoftMaxTest {

    @Test
    public void testSoftMax() {
        SearchNode.COMPUTE_SOFT_MAX = true;

        SearchNode.OPTIMIZE_SEARCH = false;
        double[][] r = initModel(new double[][] {{1.0, 1.0}, {0.5, 5.0}});

        System.out.println(r);

        SearchNode.OPTIMIZE_SEARCH = true;
        r = initModel(new double[][] {{1.0, 1.0}, {0.5, 5.0}});

        System.out.println(r);

    }


    public double[][] initModel(double[][] x) {
        Model m = new Model();

        Neuron inhib = m.createNeuron("INHIBITORY");
        Neuron.init(inhib, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        Neuron[] inputs = new Neuron[]{
                m.createNeuron("INPUT A"),
                m.createNeuron("INPUT B")
        };

        Neuron[][] output = new Neuron[2][2];

        int j = 0;
        for (double[] y : x) {
            int i = 0;

            for (double a : y) {
                Neuron n = Neuron.init(m.createNeuron(j + "-" + i), a, ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT, INeuron.Type.EXCITATORY,
                        new Synapse.Builder()
                                .setSynapseId(0)
                                .setNeuron(inputs[j])
                                .setWeight(10.0)
                                .setBias(-10.0)
                                .setRangeOutput(true),
                        new Synapse.Builder()
                                .setSynapseId(1)
                                .setNeuron(inhib)
                                .setWeight(-100.0)
                                .setBias(0.0)
                                .setRecurrent(true)
                                .addRangeRelation(EQUALS, 0)
                                .setRangeOutput(false)
                );
                output[j][i] = n;

                inhib.addSynapse(
                        new Synapse.Builder()
                                .setNeuron(n)
                                .setWeight(1.0)
                                .setBias(0.0)
                                .setRangeOutput(true)
                );
                i++;
            }
            j++;
        }

        Document doc = m.createDocument("A B ");
        inputs[0].addInput(doc, 0, 1);
        inputs[1].addInput(doc, 2, 3);

        doc.process();

        System.out.println(doc.activationsToString(true, true, true));

        double[][] results = new double[2][2];
        for(j = 0; j < 2; j++) {
            for(int i = 0; i < 2; i++) {
                Neuron n = output[j][i];

                Activation act = n.getActivation(doc, j == 0 ? new Range(0, 1) : new Range(2, 3), false);
                results[j][i] = act.avgState.p;
            }
        }

        return results;
    }
}
