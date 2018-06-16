package network.aika.nlp;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.SearchNode;
import network.aika.training.LongTermLearning;
import network.aika.training.MetaNetwork;
import network.aika.training.MetaSynapse;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static network.aika.neuron.activation.Range.Relation.*;

public class ExtractSyllables {

    public static String LETTERS = "abcdefghijklmnopqrstuvwxyzäöüß";

    public Model model;

    public Neuron documentN;
    public Neuron letterInhib;
    public Neuron syllableInhibAll;

    public Map<Character, Neuron> inputLetters = new HashMap<>();
    public Map<Integer, Neuron> syllableInhibPerLevel = new HashMap<>();


    @Before
    public void init() {
        model = new Model();
        documentN = model.createNeuron("DOCUMENT");

        letterInhib = model.createNeuron("S-LETTER");
        syllableInhibAll = model.createNeuron("S-SYLLABLE-ALL");
        Neuron.init(syllableInhibAll, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        for(int i = 2; i < 4; i++) {
            Neuron syllableInhib = model.createNeuron("S-SYLLABLE-" + i);
            Neuron syllableMetaN = model.createNeuron("M-SYLLABLE-" + i);
            syllableInhibPerLevel.put(i, syllableInhib);

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
                                .setMetaWeight(3.1)
                                .setMetaBias(-3.1)
                                .setSynapseId(j + 1)
                                .setNeuron(letterInhib)
                                .setWeight(0.31)
                                .setBias(-0.31)
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
                            .setNeuron(syllableInhibAll)
                            .setWeight(-100.0)
                            .setBias(0.0)
                            .setRecurrent(true)
                            .addRangeRelation(OVERLAPS, Synapse.Builder.OUTPUT)
            );

            if(i > 2) {
                inputs.add(
                        new MetaSynapse.Builder() // First letter of the pattern
                                .setMetaWeight(0.5)
                                .setMetaBias(-0.5)
                                .setSynapseId(i + 1)
                                .setNeuron(syllableInhibPerLevel.get(i - 1))
                                .setWeight(0.1)
                                .setBias(-0.1)
                                .addRangeRelation(BEGIN_EQUALS, 0)
                                .setRangeOutput(false, false)
                );

                inputs.add(
                        new MetaSynapse.Builder() // First letter of the pattern
                                .setMetaWeight(0.5)
                                .setMetaBias(-0.5)
                                .setSynapseId(i + 2)
                                .setNeuron(syllableInhibPerLevel.get(i - 1))
                                .setWeight(0.1)
                                .setBias(-0.1)
                                .addRangeRelation(END_EQUALS, i - 1)
                                .setRangeOutput(false, false)
                );
            }

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
    public void extractSyllables() throws IOException {
        SearchNode.OPTIMIZE_SEARCH = false;
        SearchNode.COMPUTE_SOFT_MAX = true;


        ArrayList<String> inputs = new ArrayList<>();
        inputs.add("kuchen ");
        inputs.add("kirsche ");
        inputs.add("küche ");
        inputs.add("hoch ");
        inputs.add("schon ");
        inputs.add("ich ");
        inputs.add("muschel ");
        inputs.add("chinesisch ");
        inputs.add("mich ");
        inputs.add("schule ");
        inputs.add("kochen ");
        inputs.add("fluch ");
        inputs.add("chef ");



        String[] files = new String[]{
                "Aschenputtel"/*,
                "BruederchenUndSchwesterchen",
                "DasTapfereSchneiderlein",
                "DerFroschkoenig",
                "DerGestiefelteKater",
                "DerGoldeneSchluessel",
                "DerSuesseBrei",
                "DerTeufelMitDenDreiGoldenenHaaren",
                "DerWolfUndDieSiebenJungenGeisslein",
                "DieBremerStadtmusikanten",
                "DieDreiFedern",
                "DieSterntaler",
                "DieWeisseSchlange",
                "DieZwoelfBrueder",
                "Dornroeschen",
                "FrauHolle",
                "HaenselUndGretel",
                "HansImGlueck",
                "JorindeUndJoringel",
                "KatzeUndMausInGesellschaft",
                "MaerchenVonEinemDerAuszogDasFuerchtenZuLernen",
                "Marienkind",
                "Rapunzel",
                "Rotkaeppchen",
                "Rumpelstilzchen",
                "SchneeweisschenUndRosenrot",
                "Schneewitchen",
                "TischleinDeckDich",
                "VonDemFischerUndSeinerFrau"*/
        };

/*
        ArrayList<String> inputs = new ArrayList<>();
        for (String fn : files) {
            File f = new File("/Users/lukas.molzberger/aika-ws/aika-syllables/src/main/resources/text/maerchen/" + fn + ".txt");
            InputStream is = new FileInputStream(f);
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String txt = writer.toString();

            txt = txt.replace('.', ' ');
            txt = txt.replace(',', ' ');
            txt = txt.replace('?', ' ');
            txt = txt.replace('!', ' ');
            txt = txt.replace('"', ' ');
            txt = txt.replace('-', ' ');
            txt = txt.replace(':', ' ');
            txt = txt.replace(';', ' ');
            txt = txt.replace('\n', ' ');
            txt = txt.replace("  ", " ");
            txt = txt.replace("  ", " ");

            for(String word: txt.split(" ")) {
                inputs.add(word);
            }
        }
*/

        for (String txt : inputs) {
            System.out.println(txt);
            {
                Document doc = model.createDocument(txt);

                processCharacters(doc);

                doc.process();

                System.out.println(doc.activationsToString(true, true, true));

                MetaNetwork.train(doc, 0.1);

                doc.clearActivations();
            }
            {
                Document doc = model.createDocument(txt);

                processCharacters(doc);

                doc.process();

                LongTermLearning.train(doc,
                        new LongTermLearning.Config()
                                .setPatternLearnRate(0.5)
                                .setStrengthLearnRate(0.2)
                                .setStrengthOffset(0.4)
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
                    System.out.println("            " + ps.input.getLabel() + " w:" + ps.weight + " b:" + ps.bias);
                }
            }
        }
    }
}
