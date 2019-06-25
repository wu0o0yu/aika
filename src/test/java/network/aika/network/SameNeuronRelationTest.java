package network.aika.network;

import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.EQUALS;

public class SameNeuronRelationTest {

    @Test
    public void testSameNeuronRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("IN-A", INeuron.Type.INPUT);
        Neuron inB = m.createNeuron("IN-B", INeuron.Type.INPUT);
        Neuron inC = m.createNeuron("IN-C", INeuron.Type.INPUT);


        Neuron middle = m.createNeuron("MIDDLE", INeuron.Type.INHIBITORY);

        Neuron out = m.createNeuron("OUT", INeuron.Type.EXCITATORY);


        Neuron.init(middle, 0.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(out, 2.0,
                new Synapse.Builder()
                    .setSynapseId(0)
                    .setNeuron(middle)
                    .setWeight(5.0)

        );
    }
}
