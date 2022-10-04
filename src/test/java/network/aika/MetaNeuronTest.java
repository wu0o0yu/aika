package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
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
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.BINDING;
import static network.aika.neuron.conjunctive.ConjunctiveNeuronType.PATTERN;

public class MetaNeuronTest {


    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{0.0, 0.0});
        coords.put(1, new double[]{-0.001, 0.201});
        coords.put(2, new double[]{0.0, 0.406});
        coords.put(3, new double[]{0.242, 0.623});
        coords.put(4, new double[]{0.464, 0.823});
        coords.put(5, new double[]{0.249, 0.201});
        coords.put(6, new double[]{0.245, 0.401});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{-0.347, 0.137});
        coords.put(2l, new double[]{-0.33, 0.75});
        coords.put(3l, new double[]{1.082, 0.098});
        coords.put(4l, new double[]{0.417, 0.123});
        coords.put(5l, new double[]{0.421, 0.741});
        coords.put(6l, new double[]{1.092, 0.741});
        coords.put(7l, new double[]{-0.353, -0.728});
        coords.put(8l, new double[]{0.415, -0.724});

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

        // Abstract
        CategoryNeuron letterCategory = new PatternCategoryNeuron();
        letterCategory.addProvider(m);
        letterCategory.setLabel("PC-letter");

        TokenNeuron letterPN = new TokenNeuron();
        letterPN.addProvider(m);

        letterPN.setLabel("Abstract Letter");
        letterPN.getBias().setValue(3.0);

        Synapse.init(new CategoryInputSynapse(PATTERN), letterCategory, letterPN, 1.0);

        CategoryNeuron syllableCategory = new PatternCategoryNeuron();
        syllableCategory.addProvider(m);
        syllableCategory.setLabel("PC-syllable");


        CategoryNeuron letterBindingCategory = new BindingCategoryNeuron();
        letterBindingCategory.addProvider(m);
        letterBindingCategory.setLabel("BC-letter");

        BindingNeuron letterBN = new BindingNeuron();
        letterBN.addProvider(m);
        letterBN.setLabel("Abstract BN-letter");
        letterBN.getBias().setValue(3.0);

        PatternNeuron syllable = new PatternNeuron();
        syllable.addProvider(m);
        syllable.setLabel("Syllable");

        Synapse.init(new PatternSynapse(), letterBN, syllable, 10.0);
        Synapse.init(new CategoryInputSynapse(PATTERN), syllableCategory, syllable, 1.0);

        Synapse.init(new PrimaryInputSynapse(), letterPN, letterBN, 10.0);
        Synapse.init(new CategoryInputSynapse(BINDING), letterBindingCategory, letterBN, 1.0);

        // Concrete
        TokenNeuron letterS = letterPN.instantiateTemplate(true);
        letterS.setLabel("L-s");
        letterS.setNetworkInput(true);



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

        TokenActivation letterSAct = doc.addToken(letterS, 0, 0, 1);
        process(doc, List.of(letterSAct));

        doc.postProcessing();
        doc.updateModel();
    }
}
