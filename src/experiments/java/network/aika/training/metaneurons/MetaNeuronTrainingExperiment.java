package network.aika.training.metaneurons;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.search.SearchNode;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.neuron.relation.Relation;
import network.aika.training.relation.WeightedRelation;
import network.aika.training.utils.Dictionary;
import network.aika.training.utils.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.*;
import static network.aika.neuron.relation.Relation.OVERLAPS;


public class MetaNeuronTrainingExperiment {

    MetaModel model;
    Dictionary dict;

    InhibitoryNeuron phraseInhib;
    MetaNeuron word;
    MetaNeuron bigram;
    MetaNeuron trigram;
//    Neuron[] generalizedBigram;
//    Neuron[] generalizedTrigram;
    MetaNeuron wordType;



    @Before
    public void init() {
        model = new MetaModel();
        dict = new Dictionary(model);

        phraseInhib = new InhibitoryNeuron(model, "I-PHRASE");
        Neuron.init(phraseInhib.getProvider(), 0.0);


        word = model.createMetaNeuron("Word");
        wordType = model.createMetaNeuron("WordType");
        bigram = model.createMetaNeuron("BiGram");
        trigram = model.createMetaNeuron("TriGram");


        model.initMetaNeuron(word,
                0.0,
                -10.0,
                EQUALS
        );

        model.initMetaNeuron(wordType,
                0.0,
                -10.5,
                EQUALS,
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setIdentity(true)
                        .setSynapseId(0)
                        .setNeuron(word.getInhibitoryNeuron())
                        .setWeight(10.0),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setIdentity(true)
                        .setSynapseId(1)
                        .setNeuron(word.getInhibitoryNeuron())
                        .setWeight(0.5),
/*                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setRecurrent(true)
                        .setIdentity(false)
                        .setSynapseId(2)
                        .setNeuron(phraseInhib)
                        .setWeight(0.0)
                        .setBias(0.0)
                        .setRangeInput(VARIABLE),*/
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(CONTAINS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );


        model.initMetaNeuron(bigram,
                8.0,
                -6.5,
                EQUALS,
                new MetaSynapse.Builder() // First word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(0)
                        .setNeuron(wordType.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setLimit(1.0)
                        .setIdentity(true),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(1)
                        .setNeuron(wordType.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setLimit(1.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setSynapseId(2)
                        .setNeuron(phraseInhib)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(BEGIN_TO_END_EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(OVERLAPS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(END_EQUALS)
        );


        model.initMetaNeuron(trigram,
                8.0,
                -6.5,
                EQUALS,
                new MetaSynapse.Builder() // First word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(0)
                        .setNeuron(wordType.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(1)
                        .setNeuron(wordType.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(2)
                        .setNeuron(wordType.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setNeuron(phraseInhib)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(BEGIN_TO_END_EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(1)
                        .setRelation(BEGIN_TO_END_EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(OVERLAPS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(END_EQUALS)
        );


        for(InhibitoryNeuron in: new InhibitoryNeuron[] {bigram.getInhibitoryNeuron(), trigram.getInhibitoryNeuron()}) {
            int synId = phraseInhib.getNewSynapseId();
            Neuron.init(phraseInhib.getProvider(),
                    new Synapse.Builder()
                            .setSynapseId(synId)
                            .setNeuron(in)
                            .setWeight(1.0)
                            .setRecurrent(false)
                            .setIdentity(true),
                    new Relation.Builder()
                            .setFrom(synId)
                            .setTo(Synapse.OUTPUT)
                            .setRelation(EQUALS)
            );
        }
    }





    private void dumpResults() {
        for(Synapse inhibSyn : bigram.getInhibitoryNeuron().getProvider().getActiveInputSynapses()) {
            System.out.println("    " + inhibSyn.getInput().getLabel());

            for (Synapse s : inhibSyn.getInput().getActiveInputSynapses()) {
                System.out.println("        " + s.getInput().getLabel() + " " + s.getWeight());
            }
        }
    }


    @Test
    public void testGrammarInduction() throws IOException {
        SearchNode.COMPUTE_SOFT_MAX = true;
        SearchNode.OPTIMIZE_SEARCH = false;

        String[] txt = new String[] {
                "der"
/*                "die Katze",
                "die Straße",
                "die kurvige Straße",
                "das Auto",
                "das schnelle Auto",
                "ein Auto",
                "ein schnelles Auto",
                "die Maus",
                "eine Maus",
                "eine graue Maus",
                "der braune Hund",
                "die schwarze Katze",
                "die Kamera",
                "das Boot",
                "der Hund",
                "das lange Hochseil",
                "der Koch",
                "ein Koch",
                "das blaue Auto",
                "ein blaues Auto",
                "der hochauflösende Monitor",
                "eine Straße",
                "die luxuriöse Küche",
                "das Hochseil",
                "ein langes Hochseil",
                "das Pferd",
                "ein Pferd",
                "ein kaltes Bier",
                "ein schnelles Pferd",
                "der Monitor",
                "ein Monitor",
                "die Küche",
                "eine große Küche",
                "eine lange Straße",
                "das Motorrad",
                "die Dose",
                "eine Dose",
                "eine warme Dose",
                "der kleine Supermarkt",
                "das Essen",
                "ein leckeres Essen",
                "das leckere Essen",
                "der Esel",
                "die Bank",
                "eine große Bank",
                "der Supermarkt"*/
        };


        for(int round = 0; round < 40; round++) {
            for (String t : txt) {
                System.out.println("\nTrain: " + t);
                TDocument doc = Parser.parse(model, t, (d, w) -> dict.lookupWord(w));

                System.out.println(doc.activationsToString());

                doc.train(
                        new TDocument.Config()
                                .setLearnRate(0.6)
                );

                doc.clearActivations();
            }
        }
        System.out.println();
    }
}
