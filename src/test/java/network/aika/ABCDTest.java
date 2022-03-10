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
        coords.put(0, new double[]{-0.592, -0.127});
        coords.put(1, new double[]{-0.378, 0.139});
        coords.put(2, new double[]{-0.782, 0.202});
        coords.put(3, new double[]{-0.488, 0.346});
        coords.put(4, new double[]{-0.595, 0.755});
        coords.put(5, new double[]{-0.194, 0.762});
        coords.put(6, new double[]{-0.302, 1.183});
        coords.put(7, new double[]{0.138, -0.12});
        coords.put(8, new double[]{0.141, 0.146});
        coords.put(9, new double[]{-0.156, 0.339});
        coords.put(10, new double[]{0.49, 0.374});
        coords.put(11, new double[]{0.546, 0.782});
        coords.put(12, new double[]{0.923, 0.783});
        coords.put(13, new double[]{0.411, 1.215});
        coords.put(14, new double[]{0.757, 1.187});
        coords.put(15, new double[]{-0.001, 1.204});
        coords.put(16, new double[]{-0.021, 0.855});
        coords.put(17, new double[]{0.355, 0.852});
        coords.put(18, new double[]{0.919, -0.117});
        coords.put(19, new double[]{0.974, 0.174});
        coords.put(20, new double[]{0.767, 0.381});
        coords.put(21, new double[]{1.206, 0.399});
        coords.put(22, new double[]{0.936, 1.834});
        coords.put(23, new double[]{1.842, 1.834});
        coords.put(24, new double[]{1.351, 2.065});
        coords.put(25, new double[]{1.835, -0.127});
        coords.put(26, new double[]{1.579, 0.153});
        coords.put(27, new double[]{1.687, 0.374});
        coords.put(28, new double[]{1.991, 0.361});
        return coords;
    }

    public Map<Long, double[]> getNeuronCoordinateMap() {
        Map<Long, double[]> coords = new TreeMap<>();
        coords.put(1l, new double[]{1.9, -0.993});
        coords.put(2l, new double[]{0.016, -1.65});
        coords.put(3l, new double[]{-0.246, -0.029});
        coords.put(4l, new double[]{0.236, -0.011});
        coords.put(5l, new double[]{1.216, -1.708});
        coords.put(6l, new double[]{0.671, -0.023});
        coords.put(7l, new double[]{1.688, -0.035});
        coords.put(8l, new double[]{2.713, -1.802});
        coords.put(9l, new double[]{2.34, -0.048});
        coords.put(10l, new double[]{3.069, -0.048});
        coords.put(11l, new double[]{3.991, -1.725});
        coords.put(12l, new double[]{3.652, -0.082});
        coords.put(13l, new double[]{4.261, -0.089});
        coords.put(14l, new double[]{0.026, 0.602});
        coords.put(15l, new double[]{0.934, 0.602});
        coords.put(16l, new double[]{0.395, 1.376});
        coords.put(17l, new double[]{1.463, 0.592});
        coords.put(18l, new double[]{2.717, 0.586});
        coords.put(19l, new double[]{1.212, 1.359});
        coords.put(20l, new double[]{2.047, 1.353});
        coords.put(21l, new double[]{2.675, 2.039});
        coords.put(22l, new double[]{4.041, 2.065});
        coords.put(23l, new double[]{3.305, 2.822});
        return coords;
    }
    /**
     * Dieser Testcase dient dazu, die gegenseitige Unterdrückung mehrerer Pattern zu testen:
     * 'ab' steht im Konflikt mit 'bc' und 'bcd'. 'bc' ist aber in 'bcd' eingebettet und soll daher nicht unterdrückt werden.
     *
     * Testet also Pattern, Mutual Exclusion und Relationen
     */

    @Test
    public void testABCD() throws InterruptedException {
        TextModel m = new TextModel();
        m.init();
        Templates t = m.getTemplates();

        PatternNeuron a_IN = m.lookupToken("a");

        PatternNeuron b_IN = m.lookupToken("b");
        BindingNeuron b_PTRelBN = TextModel.getPreviousTokenRelationBindingNeuron(b_IN);

        PatternNeuron c_IN = m.lookupToken("c");
        BindingNeuron c_PTRelBN = TextModel.getPreviousTokenRelationBindingNeuron(c_IN);

        PatternNeuron d_IN = m.lookupToken("d");
        BindingNeuron d_PTRelBN = TextModel.getPreviousTokenRelationBindingNeuron(d_IN);

        // Pattern ab

        BindingNeuron a_abBN = createNeuron(t.BINDING_TEMPLATE, "a (ab)");
        BindingNeuron b_abBN = createNeuron(t.BINDING_TEMPLATE, "b (ab)");


        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, b_PTRelBN, b_abBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, a_abBN, b_abBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, a_IN, a_abBN, 10.0);
    //    initInhibitoryLoop(t, "a", a_abBN);
        updateBias(a_abBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_abBN, 10.0);

        PatternNeuron abPattern = initPatternLoop(t, "ab", a_abBN, b_abBN);
        updateBias(abPattern, 3.0);

        // Pattern bc
        BindingNeuron b_bcBN = createNeuron(t.BINDING_TEMPLATE, "b (bc)");
        BindingNeuron c_bcBN = createNeuron(t.BINDING_TEMPLATE, "c (bc)");

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, c_PTRelBN, c_bcBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, b_bcBN, c_bcBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, b_IN, b_bcBN, 10.0);
        initInhibitoryLoop(t, "b", b_abBN, b_bcBN);
        updateBias(b_abBN, 3.0);
        updateBias(b_bcBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, c_IN, c_bcBN, 10.0);
     //   initInhibitoryLoop(t, "c", c_bcBN);
        updateBias(c_bcBN, 3.0);

        PatternNeuron bcPattern = initPatternLoop(t, "bc", b_bcBN, c_bcBN);
        updateBias(bcPattern, 3.0);

        // Pattern bcd

        BindingNeuron bc_bcdBN = createNeuron(t.BINDING_TEMPLATE, "bc (bcd)");
        BindingNeuron d_bcdBN = createNeuron(t.BINDING_TEMPLATE, "d (bcd)");
        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, c_bcBN, bc_bcdBN, 10.0);

        createSynapse(t.RELATED_INPUT_SYNAPSE_TEMPLATE, d_PTRelBN, d_bcdBN, 10.0);
        createSynapse(t.SAME_PATTERN_SYNAPSE_TEMPLATE, bc_bcdBN, d_bcdBN, 10.0);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, bcPattern, bc_bcdBN, 10.0);
    //    initInhibitoryLoop(t, "bc", bc_bcdBN);
        updateBias(bc_bcdBN, 2.5);

        createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, d_IN, d_bcdBN, 10.0);
    //    initInhibitoryLoop(t, "d", d_bcdBN);
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
        camera.setViewPercent(2.050);
        camera.setViewCenter(0.00309, 0.56119, 0);

        debugger.getNeuronViewManager().setCoordinateListener(n -> neuronCoords.get(n.getId()));
        camera = debugger.getNeuronViewManager().getCamera();
        camera.setViewPercent(1.0);
        camera.setViewCenter(2.013, 0.458, 0);

        doc.processTokens(List.of("a", "b", "c", "d"));

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        Thread.sleep(100);
    }
}
