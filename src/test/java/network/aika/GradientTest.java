package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

public class GradientTest {


    @Test
    public void gradientAndInduction2() {
        TextModel m = new TextModel();
        m.setConfig(Util.getTestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setEnableTraining(true)
        );

        m.setN(912);

        Document doc = new Document("A B ");

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

            i = j + 1;
        }

        m.getTemplates().SAME_BINDING_TEMPLATE.setDirectConjunctiveBias(-0.32);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPos(899);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setLastPos(739);


        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println();
    }


    @Test
    public void gradientAndInduction3() {
        TextModel m = new TextModel();
        m.setConfig(Util.getTestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setEnableTraining(true)
        );

        m.setN(912);

        Document doc = new Document("A B C ");

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

            i = j + 1;
        }

        m.getTemplates().SAME_BINDING_TEMPLATE.setDirectConjunctiveBias(-0.32);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPos(899);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setLastPos(739);


        Neuron nC = m.getNeuron("C");
        nC.setFrequency(30.0);
        nC.getSampleSpace().setN(234);
        nC.getSampleSpace().setLastPos(867);


        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println();
    }


    @Test
    public void gradientAndInduction2With2Docs() {
        TextModel m = new TextModel();
        m.setConfig(Util.getTestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setEnableTraining(true)
        );

        m.setN(912);

        Document doc = new Document("A B ");

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

            i = j + 1;
        }

        m.getTemplates().SAME_BINDING_TEMPLATE.setDirectConjunctiveBias(-0.32);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPos(899);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setLastPos(739);


        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println();
    }
}
