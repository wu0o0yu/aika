package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.Templates;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.disjunctive.CategoryNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.utils.TestUtils.*;

public class JacksonCookTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();

        coords.put(0, new double[]{-0.084, -0.003});
        coords.put(1, new double[]{0.171, 0.197});
        coords.put(2, new double[]{-0.455, 0.2});
        coords.put(3, new double[]{0.252, 0.377});
        coords.put(4, new double[]{0.25, 0.612});
        coords.put(5, new double[]{-0.345, 0.609});
        coords.put(6, new double[]{0.403, 0.812});
        coords.put(7, new double[]{0.174, 0.845});
        coords.put(8, new double[]{-0.173, 0.612});
        coords.put(9, new double[]{-0.433, 0.847});
        coords.put(10, new double[]{-0.263, 0.84});
        coords.put(11, new double[]{0.403, 1.012});
        coords.put(12, new double[]{-0.403, 1.047});
        coords.put(13, new double[]{0.103, 0.612});
        coords.put(14, new double[]{0.797, -0.023});
        coords.put(15, new double[]{0.817, 0.177});
        coords.put(16, new double[]{0.583, 0.403});
        coords.put(17, new double[]{1.142, 0.187});
        coords.put(18, new double[]{0.695, 0.585});
        coords.put(19, new double[]{1.144, 0.587});
        coords.put(20, new double[]{0.704, 0.973});
        coords.put(21, new double[]{0.79, 0.823});
        coords.put(22, new double[]{0.989, 0.582});
        coords.put(23, new double[]{1.287, 0.948});
        coords.put(24, new double[]{1.041, 0.835});
        coords.put(25, new double[]{0.704, 1.173});
        coords.put(26, new double[]{1.287, 1.148});
        coords.put(27, new double[]{0.84, 0.585});
        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();

        coords.put(1l, new double[]{1.98, -1.765});
        coords.put(2l, new double[]{0.552, -2.548});
        coords.put(3l, new double[]{-0.895, -0.87});
        coords.put(4l, new double[]{1.237, -1.15});
        coords.put(5l, new double[]{3.294, -2.543});
        coords.put(6l, new double[]{2.182, -1.068});
        coords.put(7l, new double[]{4.835, -0.398});
        coords.put(8l, new double[]{0.493, -0.03});
        coords.put(9l, new double[]{0.902, 1.03});
        coords.put(10l, new double[]{0.902, 2.164});
        coords.put(11l, new double[]{-0.683, -0.055});
        coords.put(12l, new double[]{-1.0, 1.03});
        coords.put(13l, new double[]{-1.019, 2.096});
        coords.put(14l, new double[]{3.15, -0.113});
        coords.put(15l, new double[]{3.207, 1.251});
        coords.put(16l, new double[]{3.198, 2.115});
        coords.put(17l, new double[]{4.35, -0.094});
        coords.put(18l, new double[]{4.936, 1.289});
        coords.put(19l, new double[]{4.936, 2.485});
        coords.put(20l, new double[]{-0.068, 1.635});
        coords.put(21l, new double[]{4.052, 1.309});
        coords.put(22l, new double[]{0.908, 2.931});
        coords.put(23l, new double[]{3.044, 2.922});
        coords.put(24l, new double[]{2.143, 3.792});

        return coords;
    }

    @Test
    public void testJacksonCook()  {
        AIKADebugger debugger = AIKADebugger.createAndShowGUI();

        debugger.setCurrentTestCase(() ->
                setupJacksonCookTest(debugger)
        );

        debugger.run();
    }

    public void setupJacksonCookTest(AIKADebugger debugger) {

        TextModel m = new TextModel();

        m.init();
        Templates t = m.getTemplates();

        PatternNeuron jacksonIN = m.lookupToken("Jackson");

        PatternNeuron cookIN = m.lookupToken("Cook");
        BindingNeuron cookPTRelBN = TextModel.getPreviousTokenRelationBindingNeuron(cookIN);


        BindingNeuron jacksonForenameBN = createNeuron(t.BINDING_TEMPLATE, "jackson (forename)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonForenameBN, 10.0);
        PatternNeuron jacksonForenameEntity = initPatternLoop(t, "Entity: jackson (forename)", jacksonForenameBN);
        updateBias(jacksonForenameEntity, 3.0);
        CategoryNeuron forenameCN = createNeuron(t.CATEGORY_TEMPLATE, "forename");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonForenameEntity, forenameCN, 10.0);

        BindingNeuron jacksonCityBN = createNeuron(t.BINDING_TEMPLATE, "jackson (city)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, jacksonIN, jacksonCityBN, 10.0);
        PatternNeuron jacksonCityEntity = initPatternLoop(t, "Entity: jackson (city)", jacksonCityBN);
        updateBias(jacksonCityEntity, 3.0);
        CategoryNeuron cityCN = createNeuron(t.CATEGORY_TEMPLATE, "city");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, jacksonCityEntity, cityCN, 10.0);

        BindingNeuron cookSurnameBN = createNeuron(t.BINDING_TEMPLATE, "cook (surname)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookSurnameBN, 10.0);
        PatternNeuron cookSurnameEntity = initPatternLoop(t, "Entity: cook (surname)", cookSurnameBN);
        updateBias(cookSurnameEntity, 3.0);
        CategoryNeuron surnameCN = createNeuron(t.CATEGORY_TEMPLATE, "surname");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookSurnameEntity, surnameCN, 10.0);

        BindingNeuron cookProfessionBN = createNeuron(t.BINDING_TEMPLATE, "cook (profession)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, cookIN, cookProfessionBN, 10.0);
        PatternNeuron cookProfessionEntity = initPatternLoop(t, "Entity: cook (profession)", cookProfessionBN);
        updateBias(cookProfessionEntity, 3.0);
        CategoryNeuron professionCN = createNeuron(t.CATEGORY_TEMPLATE, "profession");
        createSynapse(t.CATEGORY_SYNAPSE_TEMPLATE, cookProfessionEntity, professionCN, 10.0);

        initInhibitoryLoop(t, "jackson", jacksonForenameBN, jacksonCityBN);
        initInhibitoryLoop(t, "cook", cookSurnameBN, cookProfessionBN);

        updateBias(jacksonForenameBN, 2.0);
        updateBias(jacksonCityBN, 3.0);
        updateBias(cookSurnameBN, 2.0);
        updateBias(cookProfessionBN, 3.0);

        BindingNeuron forenameBN = createNeuron(t.BINDING_TEMPLATE, "forename (person name)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, forenameCN, forenameBN, 10.0);
        BindingNeuron surnameBN = createNeuron(t.BINDING_TEMPLATE, "surname (person name)");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, surnameCN, surnameBN, 10.0);
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, cookPTRelBN, surnameBN, 10.0);

        updateBias(forenameBN, 2.0);
        updateBias(surnameBN, 2.0);

        PatternNeuron personNamePattern = initPatternLoop(t, "person name", forenameBN, surnameBN);
        updateBias(personNamePattern, 3.0);



        Document doc = new Document(m, "Jackson Cook");
        debugger.setDocument(doc);
        debugger.setModel(m);

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true)
                .setTemplatesEnabled(true);
        doc.setConfig(c);


        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(2.4);
        camera.setViewCenter(0.443, 0.844, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.45);
        camera.setViewCenter(1.829, 0.92, 0);

        doc.processTokens(List.of("Jackson", "Cook"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
