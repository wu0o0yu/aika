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
        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();
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
        camera.setViewPercent(2.050);
        camera.setViewCenter(0.00309, 0.56119, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.0);
        camera.setViewCenter(2.013, 0.458, 0);

        doc.processTokens(List.of("Jackson", "Cook"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();
    }
}
