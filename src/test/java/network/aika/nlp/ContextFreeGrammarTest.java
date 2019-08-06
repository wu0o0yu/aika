package network.aika.nlp;



//http://ccl.pku.edu.cn/doubtfire/NLP/Parsing/Introduction/Grammars%20and%20Parsing.htm


import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.ActivationFunction.LIMITED_RECTIFIED_LINEAR_UNIT;
import static network.aika.ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT;
import static network.aika.neuron.INeuron.Type.*;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.relation.Relation.*;

public class ContextFreeGrammarTest {


    Model m;

    Neuron I;

    Neuron S;
    Neuron NP;
    Neuron VP;

    Neuron NP_ART_ADJ_N;
    Neuron NP_ART_N;
    Neuron NP_ADJ_N;

    Neuron VP_AUX_V_NP;
    Neuron VP_V_NP;

    Neuron ART;
    Neuron N;
    Neuron ADJ;
    Neuron V;
    Neuron AUX;

    Map<String, Neuron> dictionary = new TreeMap<>();


    @Before
    public void init() {
        m = new Model();

        I = m.createNeuron("Inhibitory", INHIBITORY, LIMITED_RECTIFIED_LINEAR_UNIT);

        S = m.createNeuron("Sentence", EXCITATORY);
        NP = m.createNeuron("Noun Phrase", INHIBITORY, LIMITED_RECTIFIED_LINEAR_UNIT);
        VP = m.createNeuron("Verb Phrase", INHIBITORY, LIMITED_RECTIFIED_LINEAR_UNIT);

        NP_ART_ADJ_N = m.createNeuron("NP <- ART ADJ N", EXCITATORY);
        NP_ART_N = m.createNeuron("NP <- ART N", EXCITATORY);
        NP_ADJ_N = m.createNeuron("NP <- ADJ N", EXCITATORY);

        VP_AUX_V_NP = m.createNeuron("VP <- AUX V NP", EXCITATORY);
        VP_V_NP = m.createNeuron("VP <- V NP", EXCITATORY);

        ART = m.createNeuron("Article", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);
        N = m.createNeuron("Noun", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);
        ADJ = m.createNeuron("Adjective", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);
        V = m.createNeuron("Verb", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);
        AUX = m.createNeuron("Auxiliary", INHIBITORY, RECTIFIED_HYPERBOLIC_TANGENT);


        for(Neuron n: new Neuron[] {I, NP, VP}) {
            Neuron.init(n, 0.0);
        }


        initOrNeuron(I, NP, VP, ART, N, ADJ, V, AUX);
        initOrNeuron(NP, NP_ART_ADJ_N, NP_ART_N, NP_ADJ_N);
        initOrNeuron(VP, VP_AUX_V_NP, VP_V_NP);


        initAndNeuron(S, 6.0, NP, VP);

        initAndNeuron(NP_ART_ADJ_N, 9.0, ART, ADJ, N);
        initAndNeuron(NP_ART_N, 6.0, ART, N);
        initAndNeuron(NP_ADJ_N, 6.0, ADJ, N);

        initAndNeuron(VP_AUX_V_NP, 9.0, AUX, V, NP);
        initAndNeuron(VP_V_NP, 6.0, V, NP);


        initWord("the", ART);
        initWord("large", ADJ);
        initWord("can", AUX, N, V);
        initWord("hold", N, V);
        initWord("water", N, V);

    }


    private void initWord(String word, Neuron... wordTypes) {
        Neuron wordN = m.createNeuron("W-" + word, INPUT);

        dictionary.put(word, wordN);

        for (Neuron wordType : wordTypes) {
            Neuron entity = m.createNeuron("E-" + word + "(" + wordType.getLabel() + ")", EXCITATORY);

            Neuron.init(entity, 2.0,
                    new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wordN)
                        .setWeight(10)
                        .setRecurrent(false),
                    new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(I)
                        .setWeight(-50.0)
                        .setRecurrent(true),
                    new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(OVERLAPS),
                    new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
            );

            int wtSynId = wordType.getNewSynapseId();
            Neuron.init(wordType,
                    new Synapse.Builder()
                            .setSynapseId(wtSynId)
                            .setNeuron(entity)
                            .setWeight(3.0),
                    new Relation.Builder()
                            .setFrom(wtSynId)
                            .setTo(OUTPUT)
                            .setRelation(EQUALS)
            );
        }
    }


    private void initOrNeuron(Neuron orN, Neuron... inputs) {
        for (Neuron n : inputs) {
            int synId = orN.getNewSynapseId();
            Neuron.init(orN,
                    new Synapse.Builder()
                            .setSynapseId(synId)
                            .setNeuron(n)
                            .setWeight(1.0),
                    new Relation.Builder()
                            .setFrom(synId)
                            .setTo(OUTPUT)
                            .setRelation(EQUALS)
            );
        }
    }


    private void initAndNeuron(Neuron andN, double weight, Neuron... inputs) {
        List<Neuron.Builder> in = new ArrayList<>();

        for(int i = 0; i < inputs.length; i++) {
            boolean begin = i == 0;
            boolean end = i + 1 == inputs.length;

            in.add(new Synapse.Builder()
                        .setSynapseId(i)
                        .setNeuron(inputs[i])
                        .setWeight(10.0)
                        .setIdentity(true)
            );

            if(begin) {
                in.add(
                        new Relation.Builder()
                                .setFrom(i)
                                .setTo(OUTPUT)
                                .setRelation(BEGIN_EQUALS)
                );
            }
            if(end) {
                in.add(
                        new Relation.Builder()
                                .setFrom(i)
                                .setTo(OUTPUT)
                                .setRelation(END_EQUALS)
                );
            }

            if(!begin) {
                in.add(new Relation.Builder()
                        .setFrom(i)
                        .setTo(i - 1)
                        .setRelation(BEGIN_TO_END_EQUALS)
                );
            }
        }

        in.add(new Synapse.Builder()
                .setSynapseId(inputs.length)
                .setNeuron(I)
                .setWeight(-100.0)
                .setRecurrent(true)
        );
        in.add(new Relation.Builder()
                .setFrom(inputs.length)
                .setTo(Synapse.OUTPUT)
                .setRelation(OVERLAPS)
        );

        Neuron.init(andN, weight, in.toArray(new Neuron.Builder[in.size()]));
    }


    public Document parse(String txt) {
        Document doc = new Document(m, txt);

        int i = 0;
        for(String word: txt.split(" ")) {
            int j = i + word.length();
            Neuron wn = dictionary.get(word);
            if(wn != null) {
                wn.addInput(doc, i, j + 1);
            }

            i = j + 1;
        }

        doc.process();

        return doc;
    }


    @Test
    public void parseSentence() {
        Document doc = parse("the large can can hold the water ");

        System.out.println(doc.activationsToString());

        Assert.assertNotNull(S.getActivation(doc, 0, 33, true));
    }
}