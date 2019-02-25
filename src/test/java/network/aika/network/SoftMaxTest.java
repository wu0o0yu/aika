package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.Utils;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.SearchNode;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

public class SoftMaxTest {

    @Test
    public void testSoftMax() {
        SearchNode.COMPUTE_SOFT_MAX = true;

        SearchNode.OPTIMIZE_SEARCH = false;
        double[][] ra = initModel(new double[][]{{1.0, 1.0}, {0.5, 5.0}});

        SearchNode.OPTIMIZE_SEARCH = true;
        double[][] rb = initModel(new double[][]{{1.0, 1.0}, {0.5, 5.0}});

        SearchNode.COMPUTE_SOFT_MAX = false;
    }


    public double[][] initModel(double[][] x) {
        Model m = new Model();

        Neuron inhib = m.createNeuron("INHIBITORY");
        Neuron.init(inhib, 0.0, ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

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
                                .setBias(-10.0),
                        new Synapse.Builder()
                                .setSynapseId(1)
                                .setNeuron(inhib)
                                .setWeight(-100.0)
                                .setBias(0.0)
                                .setRecurrent(true),
                        new Relation.Builder()
                                .setFrom(1)
                                .setTo(0)
                                .setRelation(EQUALS),
                        new Relation.Builder()
                                .setFrom(0)
                                .setTo(OUTPUT)
                                .setRelation(EQUALS)
                );
                output[j][i] = n;

                int inhibSynId = inhib.getNewSynapseId();
                Neuron.init(inhib,
                        new Synapse.Builder()
                                .setSynapseId(inhibSynId)
                                .setNeuron(n)
                                .setWeight(1.0)
                                .setBias(0.0),
                        new Relation.Builder()
                                .setFrom(inhibSynId)
                                .setTo(OUTPUT)
                                .setRelation(EQUALS)
                );
                i++;
            }
            j++;
        }

        Document doc = m.createDocument("A B ");
        inputs[0].addInput(doc, 0, 1);
        inputs[1].addInput(doc, 2, 3);

        doc.process();

        System.out.println(doc.activationsToString());

        System.out.println();

        double[][] results = new double[2][2];
        for(j = 0; j < 2; j++) {
            for(int i = 0; i < 2; i++) {
                Neuron n = output[j][i];

                Activation act = j == 0 ?
                        n.getActivation(doc, 0, 1, false) :
                        n.getActivation(doc, 2, 3, false);
            }
        }

        System.out.println();
        System.out.println();

        return results;
    }
}
