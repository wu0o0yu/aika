package network.aika.training.metaneurons;

import network.aika.neuron.Neuron;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.input.InputNeuron;
import network.aika.training.meta.MetaNeuronInduction;
import network.aika.neuron.relation.MultiRelation;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.relation.WeightedRelation;
import org.junit.Test;

import java.util.Collections;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.Relation.END_TO_BEGIN_EQUALS;

public class MetaNeuronSuppressionExperiment {



    @Test
    public void testMetaNeuronSuppression() {
        MetaModel model = new MetaModel();

        InputNeuron inDer = new InputNeuron(model, "W-der");
        InputNeuron inHund = new InputNeuron(model, "W-Hund");

        ExcitatoryNeuron phraseDerHund = new ExcitatoryNeuron(model, "P-der Hund");

        Neuron.init(phraseDerHund.getProvider(), 2.0,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inDer)
                        .setWeight(5.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inHund)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(new WeightedRelation(END_TO_BEGIN_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(
                                new MultiRelation(
                                        new WeightedRelation(new PositionRelation.Equals(BEGIN, 0), 1.0),
                                        new WeightedRelation(new PositionRelation.Equals(END, 1), 1.0)
                                )
                        ),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(
                                new MultiRelation(
                                        new WeightedRelation(new PositionRelation.Equals(BEGIN, 2), 1.0),
                                        new WeightedRelation(new PositionRelation.Equals(END, 3), 1.0)
                                )
                        )
        );


        MetaNeuronInduction mni = new MetaNeuronInduction(model);
        mni.createNewMetaNeuron(
                0,
                inDer.getProvider(),
                Collections.singletonList((ExcitatorySynapse) phraseDerHund.getProvider().getSynapseById(0))
        );
        


        for(int i = 0; i < 10; i++) {
            TDocument doc = new TDocument(model, "der Hund");

            inDer.getProvider().addInput(doc, 0, 4);
            inHund.getProvider().addInput(doc, 4, 8);

            doc.process();

            System.out.println(doc.activationsToString());

            doc.generateSynapses();

            doc.count();

            doc.trainLTL(
                    new TDocument.Config()
                            .setLearnRate(0.1)
            );

            doc.clearActivations();
        }

        model.dumpModel();
    }

}
