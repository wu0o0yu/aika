package network.aika.neuron;

import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;

public class Templates {

 //   private static Templates templates = new Templates();

    public static final PatternPartNeuron PATTERN_PART_TEMPLATE = init(new PatternPartNeuron(), -1);
    public static final PatternNeuron PATTERN_TEMPLATE = init(new PatternNeuron(), -2);
    public static final InhibitoryNeuron INHIBITORY_TEMPLATE = init(new InhibitoryNeuron(), -3);

    public static final PatternPartSynapse PRIMARY_INPUT_SYNAPSE_TEMPLATE = init(new PatternPartSynapse(PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false));
    public static final PatternPartSynapse RELATED_INPUT_SYNAPSE_TEMPLATE = init(new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false));
    public static final PatternPartSynapse SAME_PATTERN_SYNAPSE_TEMPLATE = init(new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, false, true));
    public static final PatternPartSynapse RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE = init(new PatternPartSynapse(PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, true, false, true));
    public static final PatternPartSynapse NEGATIVE_SYNAPSE_TEMPLATE = init(new PatternPartSynapse(INHIBITORY_TEMPLATE, PATTERN_PART_TEMPLATE, null, true, true, false, false));

    public static final PatternSynapse PATTERN_SYNAPSE_TEMPLATE = init(new PatternSynapse(PATTERN_PART_TEMPLATE, PATTERN_TEMPLATE, null));

    public static final PrimaryInhibitorySynapse PRIMARY_INHIBITORY_SYNAPSE_TEMPLATE = init(new PrimaryInhibitorySynapse(PATTERN_TEMPLATE, INHIBITORY_TEMPLATE, null));
    public static final InhibitorySynapse INHIBITORY_SYNAPSE_TEMPLATE = init(new InhibitorySynapse(PATTERN_PART_TEMPLATE, INHIBITORY_TEMPLATE, null));

    private static <N extends Neuron> N init(N n, int id) {
        NeuronProvider np = new NeuronProvider(id);
        np.setNeuron(n);
        n.setProvider(np);
        return n;
    }

    private static <S extends Synapse> S init(S ts) {
        ts.linkInput();
        ts.linkOutput();
        return ts;
    }
}
