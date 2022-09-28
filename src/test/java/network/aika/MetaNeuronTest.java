package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.BCategoryInputSynapse;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PCategoryInputSynapse;
import network.aika.neuron.conjunctive.PrimaryInputSynapse;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.disjunctive.BindingCategoryNeuron;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.text.Document;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.TestUtils.*;
import static network.aika.TestUtils.processTokens;

public class MetaNeuronTest {



    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

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
        CategoryNeuron letterCategory = new CategoryNeuron();
        letterCategory.addProvider(m);
        letterCategory.setLabel("C-letter");

        TokenNeuron letterPN = new TokenNeuron();
        letterPN.addProvider(m);

        letterPN.setLabel("Abstract Letter");
        letterPN.setTemplate(true);
        letterPN.getBias().setValue(3.0);

        Synapse.init(new PCategoryInputSynapse(), letterCategory, letterPN, 1.0);

        BindingCategoryNeuron letterBindingCategory = new BindingCategoryNeuron();
        letterBindingCategory.addProvider(m);
        letterBindingCategory.setLabel("BC-letter");

        BindingNeuron letterBN = new BindingNeuron();
        letterBN.addProvider(m);
        letterBN.setLabel("Abstract BN-letter");
        letterBN.setTemplate(true);
        letterBN.getBias().setValue(3.0);

        Synapse.init(new PrimaryInputSynapse(), letterPN, letterBN, 10.0);
        Synapse.init(new BCategoryInputSynapse(), letterBindingCategory, letterBN, 1.0);

        // Concrete
        TokenNeuron letterS = letterPN.instantiateTemplate(true);
        letterS.setLabel("L-s");
        letterS.setNetworkInput(true);




    //    TokenNeuron letterSIN = TOKEN_TEMPLATE.lookupToken("Letter-s");

        /*
        BindingNeuron forenameBN = createNeuron(t.BINDING_TEMPLATE, "forename (person name)");


        BindingNeuron jacksonForenameBN = createNeuron(forenameBN, "jackson (forename)");
        BindingNeuron jacksonJCBN = createNeuron(jacksonForenameBN, "jackson (jackson cook)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonJCBN, 10.0);
        BindingCategoryNeuron jacksonForenameCN = createNeuron(t.BINDING_CATEGORY_TEMPLATE, "jackson (forename)");
        addInhibitoryLoop(t, createNeuron(t.INHIBITORY_TEMPLATE, "I-cook"), false, cookSurnameBN, cookProfessionBN);

        updateBias(jacksonJCBN, 2.0);
*/


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
  //      camera.setViewPercent(4.7);
  //      camera.setViewCenter(1.293, 1.279, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
   //     camera.setViewPercent(1.5);
   //     camera.setViewCenter(1.702, 2.272, 0);

        TokenActivation letterSAct = doc.addToken(letterS, 0, 0, 1);
        process(doc, List.of(letterSAct));

        doc.postProcessing();
        doc.updateModel();
    }
}
