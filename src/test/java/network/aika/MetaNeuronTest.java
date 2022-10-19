package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.conjunctive.text.TokenPositionRelationNeuron;
import network.aika.neuron.disjunctive.BindingCategoryNeuron;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.neuron.disjunctive.PatternCategoryNeuron;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestUtils.*;

public class MetaNeuronTest {


    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{0.0, 0.0});
        coords.put(1, new double[]{0.716, 0.002});
        coords.put(2, new double[]{-0.001, 0.298});
        coords.put(3, new double[]{0.002, 0.623});
        coords.put(4, new double[]{0.244, 0.826});
        coords.put(5, new double[]{0.487, 1.03});
        coords.put(6, new double[]{0.713, 0.257});
        coords.put(7, new double[]{0.71, 0.604});
        coords.put(8, new double[]{1.053, 0.804});
        coords.put(9, new double[]{0.715, 0.83});
        coords.put(10, new double[]{0.299, 0.024});
        coords.put(11, new double[]{0.71, 1.004});
        coords.put(12, new double[]{0.998, 1.147});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{3.653, 5.614});
        coords.put(2l, new double[]{-3.127, -0.2});
        coords.put(3l, new double[]{-3.06, 5.322});
        coords.put(4l, new double[]{-2.841, 15.017});
        coords.put(5l, new double[]{-10.958, 5.141});
        coords.put(6l, new double[]{7.238, 5.682});
        coords.put(7l, new double[]{-11.176, 12.639});
        coords.put(8l, new double[]{3.592, 12.772});
        coords.put(9l, new double[]{-2.603, 20.359});
        coords.put(10l, new double[]{-8.632, -6.736});
        coords.put(11l, new double[]{0.792, -6.933});


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


    public void setupMetaNeuronTest(AIKADebugger debugger) {
        Model m = new Model();

        LatentRelationNeuron relPT = TokenPositionRelationNeuron.lookupRelation(m, -1, -1);

        // Abstract
        CategoryNeuron letterCategory = new PatternCategoryNeuron()
                .init(m, "PC-letter");

        TokenNeuron letterPN = new TokenNeuron()
                .init(m, "Abstract Letter");


        new PatternCategoryInputSynapse()
                .init(letterCategory, letterPN, 1.0);

        CategoryNeuron syllableCategory = new PatternCategoryNeuron()
                .init(m, "PC-syllable");

        CategoryNeuron sylBeginCategory = new BindingCategoryNeuron()
                .init(m, "Syl. Begin");

        CategoryNeuron sylContinueRightCategory = new BindingCategoryNeuron()
                .init(m, "Cont. Right Begin");

        BindingNeuron sylBeginBN = new BindingNeuron()
                .init(m, "Abstract Syl. Begin");

        BindingNeuron sylContinueRightBN = new BindingNeuron()
                .init(m, "Abstract Syl. Cont. Right");

        new RelationInputSynapse()
                .init(relPT, sylContinueRightBN, 5.0)
                .adjustBias();

        new SamePatternSynapse()
                .init(sylBeginBN, sylContinueRightBN, 10.0)
                .adjustBias();

        PatternNeuron syllablePN = new PatternNeuron()
                .init(m, "Syllable");

        new PatternSynapse()
                .init(sylBeginBN, syllablePN, 10.0)
                .adjustBias();

        new PositiveFeedbackSynapse()
                .init(syllablePN, sylBeginBN, 10.0);

        new PatternSynapse()
                .init(sylContinueRightBN, syllablePN, 6.0)
                .adjustBias();

        new PositiveFeedbackSynapse()
                .init(syllablePN, sylContinueRightBN, 10.0);

        new PatternCategoryInputSynapse()
                .init(syllableCategory, syllablePN, 1.0);

        new InputPatternSynapse()
                .init(letterPN, sylBeginBN, 10.0)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .init(sylBeginCategory, sylBeginBN, 1.0);

        new InputPatternSynapse()
                .init(letterPN, sylContinueRightBN, 10.0)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .init(sylContinueRightCategory, sylContinueRightBN, 1.0);

        // Concrete
        TokenNeuron letterS = letterPN.instantiateTemplate(true)
                .init(m, "L-s", true);

        TokenNeuron letterC = letterPN.instantiateTemplate(true)
                .init(m, "L-c", true);

        letterPN.updateBias(3.0);
        sylBeginBN.updateBias(3.0);
        sylContinueRightBN.updateBias(3.0);
        syllablePN.updateBias(3.0);


        Document doc = new Document(m, "s c h");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(1.35);
        camera.setViewCenter(0.267, 0.367, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(2.3);
        camera.setViewCenter(0.0, 0.0, 0);

        TokenActivation letterSAct = doc.addToken(letterS, 0, 0, 2);
        TokenActivation letterCAct = doc.addToken(letterC, 1, 2, 4);
        process(doc, List.of(letterSAct, letterCAct));

        doc.postProcessing();
        doc.updateModel();
    }
}
