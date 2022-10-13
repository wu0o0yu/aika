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
        coords.put(5, new double[]{0.487, 1.026});
        coords.put(6, new double[]{0.25, 0.299});
        coords.put(7, new double[]{0.249, 0.619});
        coords.put(8, new double[]{0.71, 0.296});
        coords.put(9, new double[]{0.71, 0.496});
        coords.put(10, new double[]{0.71, 0.696});
        coords.put(11, new double[]{0.71, 0.896});
        coords.put(12, new double[]{0.716, 0.202});
        coords.put(13, new double[]{0.716, 0.402});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{1.973, 2.034});
        coords.put(2l, new double[]{-0.191, 1.951});
        coords.put(3l, new double[]{-0.199, 4.045});
        coords.put(4l, new double[]{4.766, 6.952});
        coords.put(5l, new double[]{4.189, 2.034});
        coords.put(6l, new double[]{-1.7, 6.769});
        coords.put(7l, new double[]{1.911, 6.853});
        coords.put(8l, new double[]{-0.033, 10.715});
        coords.put(9l, new double[]{-1.762, -2.218});
        coords.put(10l, new double[]{1.062, -2.155});


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


        CategoryNeuron letterBindingCategory = new BindingCategoryNeuron()
                .init(m, "BC-letter");

        BindingNeuron sylBeginBN = new BindingNeuron()
                .init(m, "Abstract Syl. Begin");

        BindingNeuron sylContinueRightBN = new BindingNeuron()
                .init(m, "Abstract Syl. Cont. Right");

        new RelatedInputSynapse()
                .init(relPT, sylContinueRightBN, 5.0)
                .adjustBias();

        new SamePatternSynapse()
                .init(sylBeginBN, sylContinueRightBN, 10.0)
                .adjustBias();

        PatternNeuron syllable = new PatternNeuron()
                .init(m, "Syllable");

        new PatternSynapse()
                .init(sylBeginBN, syllable, 10.0)
                .adjustBias();

        new PatternSynapse()
                .init(sylContinueRightBN, syllable, 6.0)
                .adjustBias();

        new PatternCategoryInputSynapse()
                .init(syllableCategory, syllable, 1.0)
                .adjustBias();

        new PrimaryInputSynapse()
                .init(letterPN, sylBeginBN, 10.0)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .init(letterBindingCategory, sylBeginBN, 1.0)
                .adjustBias();

        new PrimaryInputSynapse()
                .init(letterPN, sylContinueRightBN, 10.0)
                .adjustBias();

        new BindingCategoryInputSynapse()
                .init(letterBindingCategory, sylContinueRightBN, 1.0)
                .adjustBias();

        // Concrete
        TokenNeuron letterS = letterPN.instantiateTemplate(true)
                .init(m, "L-s", true);

        TokenNeuron letterC = letterPN.instantiateTemplate(true)
                .init(m, "L-c", true);

        letterPN.updateBias(3.0);
        sylBeginBN.updateBias(3.0);
        sylContinueRightBN.updateBias(3.0);

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
