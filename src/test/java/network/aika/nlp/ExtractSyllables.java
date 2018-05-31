package network.aika.nlp;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.LongTermLearning;
import network.aika.training.MetaNetwork;
import network.aika.training.MetaSynapse;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static network.aika.neuron.activation.Range.Relation.BEGIN_TO_END_EQUALS;
import static network.aika.neuron.activation.Range.Relation.OVERLAPS;

public class ExtractSyllables {

    public static String LETTERS = "abcdefghijklmnopqrstuvwxyzäöüß";

    public Model model;

    public Neuron documentN;
    public Neuron letterInhib;
    public Neuron syllableInhibAll;

    public Map<Character, Neuron> inputLetters = new HashMap<>();


    @Before
    public void init() {
        model = new Model();
        documentN = model.createNeuron("DOCUMENT");

        letterInhib = model.createNeuron("S-LETTER");
        syllableInhibAll = model.createNeuron("S-SYLLABLE-ALL");
        Neuron.init(syllableInhibAll, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        for(int i = 2; i < 3; i++) {
            Neuron syllableInhib = model.createNeuron("S-SYLLABLE-" + i);
            Neuron syllableMetaN = model.createNeuron("M-SYLLABLE-" + i);

            ArrayList<Synapse.Builder> inputs = new ArrayList<>();
            inputs.add(
                    new MetaSynapse.Builder() // First letter of the pattern
                            .setMetaWeight(4.0)
                            .setMetaBias(-4.0)
                            .setSynapseId(0)
                            .setNeuron(letterInhib)
                            .setWeight(0.4)
                            .setBias(-0.4)
                            .setRangeOutput(true, false)
            );
            for (int j = 0; j < i - 2; j++) {
                inputs.add(
                        new MetaSynapse.Builder() // Middle letter of the pattern
                                .setMetaWeight(2.0)
                                .setMetaBias(-2.0)
                                .setSynapseId(j + 1)
                                .setNeuron(letterInhib)
                                .setWeight(0.2)
                                .setBias(-0.2)
                                .addRangeRelation(BEGIN_TO_END_EQUALS, j)
                                .setRangeOutput(false)
                );
            }
            inputs.add(
                    new MetaSynapse.Builder() // Last letter of the pattern
                            .setMetaWeight(4.0)
                            .setMetaBias(-4.0)
                            .setSynapseId(i - 1)
                            .setNeuron(letterInhib)
                            .setWeight(0.4)
                            .setBias(-0.4)
                            .addRangeRelation(BEGIN_TO_END_EQUALS, i - 2)
                            .setRangeOutput(false, true)
            );
            inputs.add(
                    new MetaSynapse.Builder()
                            .setMetaWeight(-100.0)
                            .setMetaBias(0.0)
                            .setSynapseId(i)
                            .setNeuron(syllableInhib)
                            .setWeight(-100.0)
                            .setBias(0.0)
                            .setRecurrent(true)
                            .addRangeRelation(OVERLAPS, Synapse.Builder.OUTPUT)
            );

            MetaNetwork.initMetaNeuron(syllableMetaN, 0.3, 3.0, inputs.toArray(new Synapse.Builder[inputs.size()]));

            Neuron.init(syllableInhib, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY,
                    new MetaSynapse.Builder()
                    .setMetaWeight(1.0)
                    .setMetaBias(0.0)
                    .setNeuron(syllableMetaN)
                    .setWeight(1.0)
                    .setBias(0.0)
                    .setRangeOutput(true)
            );

            syllableInhibAll.addSynapse(
                    new MetaSynapse.Builder()
                            .setMetaWeight(1.0)
                            .setMetaBias(0.0)
                            .setNeuron(syllableInhib)
                            .setWeight(1.0)
                            .setBias(0.0)
                            .setRangeOutput(true)
            );
        }

        Neuron.init(letterInhib, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);


        for (char c: LETTERS.toCharArray()) {
            Neuron letterN = model.createNeuron("L-" + c);
            inputLetters.put(c, letterN);

            letterInhib.addSynapse(
                    new Synapse.Builder()
                            .setNeuron(letterN)
                            .setWeight(1.0)
                            .setBias(0.0)
                            .setRangeOutput(true)
            );
        }
    }


    public void processCharacters(Document doc) {
        for(int pos = 0; pos < doc.length(); pos++) {
            char c = doc.getContent().charAt(pos);

            Neuron inputLetter = inputLetters.get(Character.toLowerCase(c));
            if(inputLetter != null) {
                inputLetter.addInput(doc, pos, pos + 1);
            }
        }
    }


    @Test
    public void extractSyllables() {
        ArrayList<String> inputs = new ArrayList<>();
        inputs.add("kuchen ");
        inputs.add("küche ");
        inputs.add("hoch ");
        inputs.add("ich ");
        inputs.add("chinesisch ");
        inputs.add("mich ");
        inputs.add("kochen ");
        inputs.add("fluch ");
        inputs.add("chef ");


        for (String txt : inputs) {
            System.out.println(txt);
            {
                Document doc = model.createDocument(txt);

                processCharacters(doc);

                doc.process();

                MetaNetwork.train(doc, 0.1);

                doc.clearActivations();
            }
            {
                Document doc = model.createDocument(txt);

                processCharacters(doc);

                doc.process();

                LongTermLearning.train(doc,
                        new LongTermLearning.Config()
                                .setLTPLearnRate(1.0)
                                .setLTDLearnRate(1.0)
                );

                doc.clearActivations();
            }
        }

        dumpResults();
    }


    private void dumpResults() {
        System.out.println("Results: ");
        for(Synapse inhibSyn : syllableInhibAll.inMemoryInputSynapses.values()) {
            System.out.println("    " + inhibSyn.input.getLabel() + " Bias:" + inhibSyn.input.get().biasSum);

            for (Synapse s : inhibSyn.input.inMemoryInputSynapses.values()) {
                Neuron pattern = s.input;
                System.out.println("        " + pattern.getLabel() + " os.weight:" + s.weight + " bias:" + pattern.get().bias);

                for (Synapse ps : pattern.inMemoryInputSynapses.values()) {
                    System.out.println("            " + ps.input.getLabel() + " ps.weight:" + ps.weight);
                }
            }
        }
    }
}
