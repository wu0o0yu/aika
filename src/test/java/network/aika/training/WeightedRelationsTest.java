package network.aika.training;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.input.InputNeuron;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.relation.WeightedRelation;
import org.junit.Test;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.*;

public class WeightedRelationsTest {

    @Test
    public void testDerHund() {
        MetaModel m = new MetaModel();

        InputNeuron inDer = new InputNeuron(m, "der");
        InputNeuron inHund = new InputNeuron(m, "Hund");

        ExcitatoryNeuron phraseDerHund = new ExcitatoryNeuron(m, "P-der Hund", null);

        Neuron.init(phraseDerHund.getProvider(), 2.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inDer)
                        .setWeight(5.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inHund)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new WeightedRelation(new PositionRelation.Equals(1, 0), 1.0)),
                new WeightedRelation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(new WeightedRelation(BEGIN_EQUALS, 1.0)),
                new WeightedRelation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(new WeightedRelation(END_EQUALS, 1.0))
        );

        TDocument doc = new TDocument(m, "der Hund ");

        inDer.getProvider().addInput(doc, 0, 4);
        inHund.getProvider().addInput(doc, 4, 9);

        doc.process();

        System.out.println(doc.activationsToString());
    }
}
