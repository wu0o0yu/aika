package network.aika.training.metaneurons;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.search.SearchNode;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.inhibitory.InhibitorySynapse;
import network.aika.neuron.relation.MultiRelation;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.relation.WeightedRelation;
import network.aika.training.utils.Dictionary;
import network.aika.training.utils.Parser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.Relation.*;

public class MetaNeuronInductionExperiment {

    MetaModel model = new MetaModel();

    Dictionary dict = new Dictionary(model);

    String[] txt = new String[] {
            "die Katze",
//                "die schwarze Katze",
            "die Straße",
            "eine Straße",
//                "eine lange Straße",
//                "die kurvige Straße",
            "das Auto",
//                "das schnelle Auto",
            "ein Auto",
//                "ein schnelles Auto",
//                "das blaue Auto",
            //               "ein blaues Auto",
            "die Maus",
            "eine Maus",
//                "eine graue Maus",
            "die Kamera",
            "das Boot",
            "der Hund",
//                "der braune Hund",
            "das Hochseil",
//                "ein langes Hochseil",
//                "das lange Hochseil",
            "der Koch",
            "ein Koch",
            "der Monitor",
            "ein Monitor",
//                "der hochauflösende Monitor",
            "die Küche",
//                "die luxuriöse Küche",
//                "eine große Küche",
            "das Pferd",
            "ein Pferd",
//                "ein schnelles Pferd",
            "ein Bier",
//                "ein kaltes Bier",
            "das Motorrad",
            "die Dose",
            "eine Dose",
//                "eine warme Dose",
            "der Supermarkt",
//                "der kleine Supermarkt",
            "das Essen",
//                "ein leckeres Essen",
//                "das leckere Essen",
            "der Esel",
            "die Bank",
            "eine Bank",
//                "eine große Bank",
    };


    @Test
    public void testMetaNeuronInduction()  {
        SearchNode.COMPUTE_SOFT_MAX = true;
        SearchNode.OPTIMIZE_SEARCH = false;

        generatePhraseTargets();

        for(int round = 0; round < 40; round++) {
            for (String t : txt) {
                System.out.println("\nTrain: " + t);
                TDocument doc = Parser.parse(model, t, (d, w) -> dict.lookupWord(w));

                System.out.println(doc.activationsToString());

                doc.count();
/*                doc.train(
                        new TDocument.Config()
                                .setLearnRate(0.6)
                );
*/
                doc.clearActivations();
            }
        }
        System.out.println();

        MetaNeuron.induce(model, 0);

        model.dumpModel();
    }


    @Test
    public void testTrainTargetNeuronWithRecurrentInput() {
        SearchNode.COMPUTE_SOFT_MAX = true;
        SearchNode.OPTIMIZE_SEARCH = false;

        List<ExcitatoryNeuron> targetNeurons = generatePhraseTargets();

        Map<Neuron, InhibitoryNeuron> in1Neurons = new TreeMap<>();
        Map<Neuron, InhibitoryNeuron> in2Neurons = new TreeMap<>();

        for(ExcitatoryNeuron tn: targetNeurons) {
            Neuron key = tn.getProvider().getSynapseById(0).getInput();
            InhibitoryNeuron in1 = getInhibitoryNeuron(in1Neurons, key, "1");

            int synId1 = in1.getNewSynapseId();

            Neuron.init(in1.getProvider(), 0.0,
                    new InhibitorySynapse.Builder()
                            .setSynapseId(synId1)
                            .setNeuron(tn)
                            .setWeight(1.0),
                    new Relation.Builder()
                            .setFrom(synId1)
                            .setTo(OUTPUT)
                            .setRelation(
                                    new MultiRelation(
                                            new PositionRelation.Equals(0, BEGIN),
                                            new PositionRelation.Equals(1, END)
                                    )
                            )
            );

            InhibitoryNeuron in2 = getInhibitoryNeuron(in2Neurons, key, "2");

            int synId2 = in2.getNewSynapseId();

            Neuron.init(in2.getProvider(), 0.0,
                    new InhibitorySynapse.Builder()
                            .setSynapseId(synId2)
                            .setNeuron(tn)
                            .setWeight(1.0),
                    new Relation.Builder()
                            .setFrom(synId2)
                            .setTo(OUTPUT)
                            .setRelation(
                                    new MultiRelation(
                                            new PositionRelation.Equals(2, BEGIN),
                                            new PositionRelation.Equals(3, END)
                                    )
                            )
            );
        }


        for(int i = 0; i < 100; i++) {
            System.out.println("Round " + i);

            for (String t : txt) {
                TDocument doc = Parser.parse(model, t, (d, w) -> dict.lookupWord(w));

                doc.process();

                System.out.println(doc.activationsToString());

                doc.train(
                        new TDocument.Config()
                                .setLearnRate(0.1)
                );

                doc.clearActivations();
            }
        }
        System.out.println();

        model.dumpModel();
    }


    public InhibitoryNeuron getInhibitoryNeuron(Map<Neuron, InhibitoryNeuron> inNeurons, Neuron key, String label) {
        InhibitoryNeuron in = inNeurons.get(key);

        if(in == null) {
            in = new InhibitoryNeuron(model, "I-Phrase-" + key.getLabel().substring(2) + "-" + label);
            inNeurons.put(key, in);
        }
        return in;
    }


    public List<ExcitatoryNeuron> generatePhraseTargets() {
        ArrayList<ExcitatoryNeuron> targetNeurons = new ArrayList<>();
        for(String e: txt) {
            String[] w = e.split(" ");

            ExcitatoryNeuron phrase = new ExcitatoryNeuron(model, e);

            Neuron.init(phrase.getProvider(), 2.0,
                    new ExcitatorySynapse.Builder()
                            .setSynapseId(0)
                            .setNeuron(dict.lookupWord(w[0]))
                            .setWeight(5.0),
                    new ExcitatorySynapse.Builder()
                            .setSynapseId(1)
                            .setNeuron(dict.lookupWord(w[1]))
                            .setWeight(5.0),
                    new Relation.Builder()
                            .setRelation(new WeightedRelation(END_TO_BEGIN_EQUALS, 1.0))
                            .setFrom(0)
                            .setTo(1),
                    new Relation.Builder()
                            .setRelation(
                                    new MultiRelation(
                                            new WeightedRelation(new PositionRelation.Equals(BEGIN, 0), 1.0),
                                            new WeightedRelation(new PositionRelation.Equals(END, 1), 1.0)
                                    )
                            )
                            .setFrom(0)
                            .setTo(OUTPUT),
                    new Builder()
                            .setRelation(
                                    new MultiRelation(
                                            new WeightedRelation(new PositionRelation.Equals(BEGIN, 2), 1.0),
                                            new WeightedRelation(new PositionRelation.Equals(END, 3), 1.0)
                                    )
                            )
                            .setFrom(1)
                            .setTo(OUTPUT)
            );

            targetNeurons.add(phrase);
        }
        return targetNeurons;
    }
}
