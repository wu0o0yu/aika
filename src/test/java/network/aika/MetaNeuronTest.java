package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.LatentRelationNeuron;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.neurons.CategoryNeuron;
import network.aika.elements.neurons.InhibitoryNeuron;
import network.aika.elements.synapses.*;
import network.aika.elements.neurons.TokenNeuron;
import network.aika.elements.neurons.TokenPositionRelationNeuron;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestUtils.*;
import static network.aika.elements.synapses.Scope.INPUT;

public class MetaNeuronTest {


    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{0.0, 0.0});
        coords.put(1, new double[]{1.082, 0.004});
        coords.put(2, new double[]{-0.001, 0.298});
        coords.put(3, new double[]{0.002, 0.623});
        coords.put(4, new double[]{0.244, 0.826});
        coords.put(5, new double[]{0.047, 0.926});
        coords.put(6, new double[]{0.507, 1.03});
        coords.put(7, new double[]{1.082, 0.296});
        coords.put(8, new double[]{1.061, 0.574});
        coords.put(9, new double[]{1.243, 0.813});
        coords.put(10, new double[]{0.632, 0.83});
        coords.put(11, new double[]{0.472, 0.009});
        coords.put(12, new double[]{0.998, 1.147});
        coords.put(13, new double[]{1.243, 1.013});
        coords.put(14, new double[]{0.272, 0.287});
        coords.put(15, new double[]{0.272, 0.487});
        coords.put(16, new double[]{0.664, 0.289});
        coords.put(17, new double[]{0.655, 0.513});
        coords.put(18, new double[]{0.44, 0.525});
        coords.put(19, new double[]{0.44, 0.725});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{0.077, -15.728});
        coords.put(2l, new double[]{-3.301, 6.852});
        coords.put(3l, new double[]{-3.236, 12.599});
        coords.put(4l, new double[]{9.227, 38.369});
        coords.put(5l, new double[]{-5.69, 38.367});
        coords.put(6l, new double[]{2.188, 12.729});
        coords.put(7l, new double[]{-15.7, 8.66});
        coords.put(8l, new double[]{-7.929, 21.224});
        coords.put(9l, new double[]{14.846, 7.498});
        coords.put(10l, new double[]{14.858, 21.224});
        coords.put(11l, new double[]{-11.775, -16.114});
        coords.put(12l, new double[]{11.394, -15.651});
        coords.put(13l, new double[]{-12.148, -3.61});
        coords.put(14l, new double[]{14.523, -3.094});
        coords.put(15l, new double[]{2.156, 6.443});

        return coords;
    }

    @Test
    public void testMetaNeuron()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.addBreakpoints(
        );

        debugger.setCurrentTestCase(() ->
                setupMetaNeuronTest(debugger)
        );
        debugger.run();
    }

    private BindingNeuron createInitialSyllableBindingNeuron(
            Model m,
            TokenNeuron letterPN,
            InhibitoryNeuron inhib,
            PatternNeuron syllablePN
    ) {
        CategoryNeuron sylBeginCategory = new CategoryNeuron()
                .init(m, "Syl. Cat. Pos-0");

        BindingNeuron sylBeginBN = new BindingNeuron()
                .init(m, "Abstract Syl. Pos-0");

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(sylBeginBN, inhib);

        new NegativeFeedbackSynapse()
                .setWeight(-20.0)
                .init(inhib, sylBeginBN)
                .adjustBias();

        new PatternSynapse()
                .setWeight(10.0)
                .init(sylBeginBN, syllablePN)
                .adjustBias();

        new PositiveFeedbackSynapse()
                .setWeight(10.0)
                .init(syllablePN, sylBeginBN)
                .adjustBias();

        new InputPatternFromPatternSynapse()
                .setWeight(10.0)
                .init(letterPN, sylBeginBN)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .setWeight(1.0)
                .init(sylBeginCategory, sylBeginBN);

//        sylBeginBN.updateBias(3.0);

        return sylBeginBN;
    }


    private BindingNeuron createContinueRightBindingNeuron(
            Model m,
            int pos,
            TokenNeuron letterPN,
            LatentRelationNeuron relPT,
            BindingNeuron lastBN,
            InhibitoryNeuron inhib,
            PatternNeuron syllablePN
    ) {

        CategoryNeuron sylContinueRightCategory = new CategoryNeuron()
                .init(m, "Syl. Cat. Pos-R" + pos);

        BindingNeuron sylContinueRightBN = new BindingNeuron()
                .init(m, "Abstract Syl. Pos-R" + pos);

        new InhibitorySynapse(INPUT)
                .setWeight(1.0)
                .init(sylContinueRightBN, inhib);

        new NegativeFeedbackSynapse()
                .setWeight(-20.0)
                .init(inhib, sylContinueRightBN)
                .adjustBias();

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(relPT, sylContinueRightBN)
                .adjustBias();

        new SamePatternSynapse()
                .setWeight(10.0)
                .init(lastBN, sylContinueRightBN)
                .adjustBias();

        new PatternSynapse()
                .setWeight(6.0)
                .init(sylContinueRightBN, syllablePN)
                .adjustBias();

        new PositiveFeedbackSynapse()
                .setWeight(10.0)
                .init(syllablePN, sylContinueRightBN);


        new InputPatternFromPatternSynapse()
                .setWeight(10.0)
                .init(letterPN, sylContinueRightBN)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .setWeight(1.0)
                .init(sylContinueRightCategory, sylContinueRightBN);

        sylContinueRightBN.setBias(3.0);
        return sylContinueRightBN;
    }


    public void setupMetaNeuronTest(AIKADebugger debugger) {
        Model m = new Model();

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, -1, -1);

        // Abstract
        CategoryNeuron letterCategory = new CategoryNeuron()
                .init(m, "PC-letter");

        TokenNeuron letterPN = new TokenNeuron()
                .init(m, "Abstract Letter");

        PatternNeuron syllablePN = new PatternNeuron()
                .init(m, "Syllable");

        InhibitoryNeuron inhib =new InhibitoryNeuron()
                .init(m, "I");


        new PatternCategoryInputSynapse()
                .setWeight(1.0)
                .init(letterCategory, letterPN);

        CategoryNeuron syllableCategory = new CategoryNeuron()
                .init(m, "Syllable Category");

        BindingNeuron sylBeginBN = createInitialSyllableBindingNeuron(
                m,
                letterPN,
                inhib,
                syllablePN
        );

        BindingNeuron sylRightPos1BN = createContinueRightBindingNeuron(
                m,
                1,
                letterPN,
                relPT,
                sylBeginBN,
                inhib,
                syllablePN
        );

        new PatternCategoryInputSynapse()
                .setWeight(1.0)
                .init(syllableCategory, syllablePN);

        // Concrete
        TokenNeuron letterS = letterPN.instantiateTemplate()
                .init(m, "L-s");

        TokenNeuron letterC = letterPN.instantiateTemplate()
                .init(m, "L-c");

        letterPN.setBias(3.0);
        syllablePN.setBias(3.0);


        Document doc = new Document(m, "s c h");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setTrainingEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.65);
        camera.setViewCenter(0.557, 0.473, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.1);
        camera.setViewCenter(-0.782, 10.985, 0);

        TokenActivation letterSAct = doc.addToken(letterS, 0, 0, 2);
        TokenActivation letterCAct = doc.addToken(letterC, 1, 2, 4);
        process(doc, List.of(letterSAct, letterCAct));

        doc.postProcessing();
        doc.updateModel();
    }
}
