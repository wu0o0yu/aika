package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.Templates;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.graphstream.ui.view.camera.Camera;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.utils.TestUtils.*;

public class ABCDTest {

    public Map<Integer, double[]> getActCoordinateMap() {
        Map<Integer, double[]> coords = new TreeMap<>();
        coords.put(0, new double[]{-0.683, -0.476});
        coords.put(1, new double[]{-0.595, -0.266});
        coords.put(2, new double[]{-0.896, -0.025});
        coords.put(3, new double[]{-0.415, -0.033});
        coords.put(4, new double[]{-0.665, 0.463});
        coords.put(5, new double[]{-0.261, 0.465});
        coords.put(6, new double[]{-0.525, 0.967});
        coords.put(7, new double[]{0.227, -0.577});
        coords.put(8, new double[]{0.074, -0.296});
        coords.put(9, new double[]{-0.065, -0.025});
        coords.put(10, new double[]{0.483, -0.04});
        coords.put(11, new double[]{0.558, 0.462});
        coords.put(12, new double[]{0.909, 0.475});
        coords.put(13, new double[]{0.257, 0.937});
        coords.put(14, new double[]{0.718, 0.958});
        coords.put(15, new double[]{0.004, 0.937});
        coords.put(16, new double[]{-0.006, 0.463});
        coords.put(17, new double[]{0.348, 0.467});
        coords.put(18, new double[]{0.962, -0.64});
        coords.put(19, new double[]{0.978, -0.269});
        coords.put(20, new double[]{0.747, -0.036});
        coords.put(21, new double[]{1.107, 0.001});
        coords.put(22, new double[]{0.899, 1.293});
        coords.put(23, new double[]{1.753, 1.296});
        coords.put(24, new double[]{1.237, 1.822});
        coords.put(25, new double[]{1.708, -0.606});
        coords.put(26, new double[]{1.689, -0.266});
        coords.put(27, new double[]{1.53, -0.051});
        coords.put(28, new double[]{1.84, 0.023});

        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();
        coords.put(1l, new double[]{1.942, -2.738});
        coords.put(2l, new double[]{4.879, -1.981});
        coords.put(3l, new double[]{1.739, -1.882});
        coords.put(4l, new double[]{-1.274, -3.501});
        coords.put(5l, new double[]{0.802, -3.547});
        coords.put(6l, new double[]{3.915, -3.588});
        coords.put(7l, new double[]{5.852, -3.531});
        coords.put(8l, new double[]{-1.257, -0.365});
        coords.put(9l, new double[]{-0.034, -0.312});
        coords.put(10l, new double[]{-0.661, 1.384});
        coords.put(11l, new double[]{1.565, -0.24});
        coords.put(12l, new double[]{3.894, -0.249});
        coords.put(13l, new double[]{0.868, 1.283});
        coords.put(14l, new double[]{2.593, 1.36});
        coords.put(15l, new double[]{3.865, 2.807});
        coords.put(16l, new double[]{6.174, 2.821});
        coords.put(17l, new double[]{4.95, 4.607});

        return coords;
    }

    /**
     *
     */
    @Test
    public void testABCD() throws InterruptedException {
        TextModel m = new TextModel();
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron a_IN = m.lookupToken("a");

        PatternNeuron b_IN = m.lookupToken("b");
        PatternNeuron c_IN = m.lookupToken("c");
        PatternNeuron d_IN = m.lookupToken("d");

        // Pattern ab
        BindingNeuron a_abBN = createNeuron(t.BINDING_TEMPLATE, "a (ab)");
        BindingNeuron b_abBN = createNeuron(t.BINDING_TEMPLATE, "b (ab)");


        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, m.getPreviousTokenRelationBindingNeuron(), b_abBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, a_abBN, b_abBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, a_IN, a_abBN, 10.0);
        updateBias(a_abBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_abBN, 10.0);

        PatternNeuron abPattern = initPatternLoop(t, "ab", a_abBN, b_abBN);
        updateBias(abPattern, 3.0);

        // Pattern bc
        BindingNeuron b_bcBN = createNeuron(t.BINDING_TEMPLATE, "b (bc)");
        BindingNeuron c_bcBN = createNeuron(t.BINDING_TEMPLATE, "c (bc)");

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, m.getPreviousTokenRelationBindingNeuron(), c_bcBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, b_bcBN, c_bcBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_bcBN, 10.0);
        initInhibitoryLoop(t, "b", b_abBN, b_bcBN);
        updateBias(b_abBN, 3.0);
        updateBias(b_bcBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, c_IN, c_bcBN, 10.0);
        updateBias(c_bcBN, 3.0);

        PatternNeuron bcPattern = initPatternLoop(t, "bc", b_bcBN, c_bcBN);
        updateBias(bcPattern, 3.0);

        // Pattern bcd
        BindingNeuron bc_bcdBN = createNeuron(t.BINDING_TEMPLATE, "bc (bcd)");
        BindingNeuron d_bcdBN = createNeuron(t.BINDING_TEMPLATE, "d (bcd)");
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, c_bcBN, bc_bcdBN, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, m.getPreviousTokenRelationBindingNeuron(), d_bcdBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, bc_bcdBN, d_bcdBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, bcPattern, bc_bcdBN, 10.0);
        updateBias(bc_bcdBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, d_IN, d_bcdBN, 10.0);
        updateBias(d_bcdBN, 3.0);

        PatternNeuron bcdPattern = initPatternLoop(t, "bcd", bc_bcdBN, d_bcdBN);
        updateBias(bcdPattern, 3.0);


        Document doc = new Document(m, "abcd");

        Config c = getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setInductionThreshold(0.1)
                .setTrainingEnabled(true)
                .setTemplatesEnabled(true);
        doc.setConfig(c);

        AIKADebugger debugger = AIKADebugger.createAndShowGUI(doc);
        Map<Integer, double[]> actCoords = getActCoordinateMap();
        Map<Long, double[]> neuronCoords = getNeuronCoordinateMap();
        debugger.getActivationViewManager().setCoordinateListener(act -> actCoords.get(act.getId()));

        Camera camera = debugger.getActivationViewManager().getCamera();
        camera.setViewPercent(2.95);
        camera.setViewCenter(0.451, 0.579, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.2);
        camera.setViewCenter(1.978, 0.47, 0);

        doc.processTokens(List.of("a", "b", "c", "d"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        Thread.sleep(100);
    }
}
