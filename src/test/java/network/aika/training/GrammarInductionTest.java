package network.aika.training;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Range;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.activation.Range.Relation.*;
import static network.aika.neuron.activation.Range.Relation.OVERLAPS;

public class GrammarInductionTest {

    Model model;

    Neuron wordInhib;
    Neuron bigramInhib;
    Neuron bigramMetaN;

    Map<String, Neuron> dictionary = new TreeMap<>();


    @Before
    public void init() {
        model = new Model();

        wordInhib = model.createNeuron("S-WORD");
        bigramInhib = model.createNeuron("S-PHRASE");


        Neuron.init(wordInhib, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        Neuron.init(bigramInhib, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);


        bigramMetaN = model.createNeuron("M-PHRASE");


        MetaNetwork.initMetaNeuron(bigramMetaN, 4.0, 6.0,
                new MetaSynapse.Builder() // First word of the phrase
                        .setMetaWeight(2.0)
                        .setMetaBias(-2.0)
                        .setSynapseId(0)
                        .setNeuron(wordInhib)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .setRangeOutput(true, false),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setMetaWeight(2.0)
                        .setMetaBias(-2.0)
                        .setSynapseId(1)
                        .setNeuron(wordInhib)
                        .setWeight(2.0)
                        .setBias(-2.0)
                        .addRangeRelation(BEGIN_TO_END_EQUALS, 0)
                        .setRangeOutput(false, true),
                new MetaSynapse.Builder()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setSynapseId(2)
                        .setNeuron(bigramInhib)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(OVERLAPS, Synapse.Builder.OUTPUT)
        );

        bigramInhib.addSynapse(
                new MetaSynapse.Builder()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setSynapseId(0)
                        .setNeuron(bigramMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );
    }


    public Document parse(String txt) {
        Document doc = model.createDocument(txt);
        txt = txt + ' ';

        char lc = ' ';
        int begin = 0;
        int end = 0;
        for(int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);

            if(isSeparator(lc) && !isSeparator(c)) {
                if(begin != end) {
                    String word = txt.substring(begin, end);
                    Neuron wn = dictionary.get(word);
                    if (wn == null) {
                        wn = model.createNeuron("W-" + word);
                        dictionary.put(word, wn);

                        wordInhib.addSynapse(
                                new Synapse.Builder()
                                    .setNeuron(wn)
                                    .setWeight(1.0)
                                    .setBias(0.0)
                                    .setRangeOutput(true)
                        );
                    }

                    wn.addInput(doc, begin, i);
                }
                begin = i;
            } else if(!isSeparator(lc) && isSeparator(c)) {
                end = i;
            }

            lc = c;
        }

        doc.process();

        return doc;
    }


    private static boolean isSeparator(char c) {
        return c == ' ' || c == '(' || c == ')' || c == '/' || c == ',' || c == ';' || c == ':' || c == '.' || c == '!' || c == '?' || c == '-' || c == '\n';
    }


    @Ignore
    @Test
    public void testGrammarInduction() throws IOException {
        File f = new File("./src/test/resources/text/ADogsLife.txt");
        InputStream is = new FileInputStream(f);
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, "UTF-8");
        String txt = writer.toString();

        Document doc = parse(txt);

        System.out.println(doc.activationsToString(true, true, true));
    }
}
