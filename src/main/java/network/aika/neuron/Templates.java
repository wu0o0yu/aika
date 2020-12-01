package network.aika.neuron;

import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;

public class Templates {

    public static final PatternPartNeuron PATTERN_PART_TEMPLATE = init(new PatternPartNeuron(), -1, "Template Patter Part Neuron");
    public static final PatternNeuron INPUT_PATTERN_TEMPLATE = init(new PatternNeuron(), -2, "Input Template Patter Neuron");
    public static final PatternNeuron SAME_PATTERN_TEMPLATE = init(new PatternNeuron(), -3, "Same Template Patter Neuron");
    public static final InhibitoryNeuron INHIBITORY_TEMPLATE = init(new InhibitoryNeuron(), -4, "Template Inhibitory Neuron");

    static {
        INPUT_PATTERN_TEMPLATE.getTemplates().add(SAME_PATTERN_TEMPLATE);
        SAME_PATTERN_TEMPLATE.getTemplates().add(INPUT_PATTERN_TEMPLATE);
    }

    public static final PatternPartSynapse PRIMARY_INPUT_SYNAPSE_TEMPLATE =
            init(
                    new PatternPartSynapse(INPUT_PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false),
                    true,
                    true
            );

    public static final PatternPartSynapse RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE =
            init(
                    new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false),
                    true,
                    true
            );

    public static final PatternPartSynapse RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE =
            init(
                    new PatternPartSynapse(INHIBITORY_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false),
                    true,
                    true
            );

    public static final PatternPartSynapse SAME_PATTERN_SYNAPSE_TEMPLATE =
            init(
                    new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, false, true),
                    true,
                    true
            );

    public static final PatternPartSynapse RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE =
            init(
                    new PatternPartSynapse(SAME_PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, true, false, true),
                    true,
                    true
            );

    public static final PatternPartSynapse NEGATIVE_SYNAPSE_TEMPLATE =
            init(
                    new PatternPartSynapse(INHIBITORY_TEMPLATE, PATTERN_PART_TEMPLATE, null, true, true, false, false),
                    false,
                    true
            );

    public static final PatternSynapse PATTERN_SYNAPSE_TEMPLATE =
            init(
                    new PatternSynapse(PATTERN_PART_TEMPLATE, SAME_PATTERN_TEMPLATE, null),
                    true,
                    true
            );

    public static final PrimaryInhibitorySynapse PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE =
            init(
                    new PrimaryInhibitorySynapse(INPUT_PATTERN_TEMPLATE, INHIBITORY_TEMPLATE, null),
                    true,
                    true
            );

    public static final InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE =
            init(
                    new InhibitorySynapse(PATTERN_PART_TEMPLATE, INHIBITORY_TEMPLATE, null),
                    false,
                    true
            );


    private static <N extends Neuron> N init(N n, int id, String label) {
        NeuronProvider np = new NeuronProvider(id);
        np.setNeuron(n);
        n.setProvider(np);
        n.setLabel(label);
        return n;
    }

    private static <S extends Synapse> S init(S ts, boolean linkInput, boolean linkOutput) {
        if(linkInput) {
            ts.linkInput();
        }
        if(linkOutput) {
            ts.linkOutput();
        }
        return ts;
    }
}
