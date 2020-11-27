package network.aika.neuron;

import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;

public class Templates {

    private static Templates templates = new Templates();

    public final PatternPartNeuron PATTERN_PART_TEMPLATE = new PatternPartNeuron();
    public final PatternNeuron PATTERN_TEMPLATE = new PatternNeuron();
    public final InhibitoryNeuron INHIBITORY_TEMPLATE = new InhibitoryNeuron();


    public static Templates getTemplates() {
        return templates;
    }

    public Templates() {
        Neuron[] templateNeurons = new Neuron[]{
                PATTERN_PART_TEMPLATE,
                PATTERN_TEMPLATE,
                INHIBITORY_TEMPLATE
        };

        int idCounter = -1;
        for(Neuron n: templateNeurons) {
            NeuronProvider np = new NeuronProvider(idCounter--);
            np.setNeuron(n);
            n.setProvider(np);
        }

        Synapse[] templateSynapses = new Synapse[] {
                new PatternPartSynapse(PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false),
                new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, true, false),
                new PatternPartSynapse(PATTERN_PART_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, false, false, true),
                new PatternPartSynapse(PATTERN_TEMPLATE, PATTERN_PART_TEMPLATE, null, false, true, false, true),
                new PatternPartSynapse(INHIBITORY_TEMPLATE, PATTERN_PART_TEMPLATE, null, true, true, false, false),
                new PatternSynapse(PATTERN_PART_TEMPLATE, PATTERN_TEMPLATE, null),
                new InhibitorySynapse(PATTERN_TEMPLATE, INHIBITORY_TEMPLATE, null),
                new InhibitorySynapse(PATTERN_PART_TEMPLATE, INHIBITORY_TEMPLATE, null)
        };

        for(Synapse ts: templateSynapses) {
            ts.linkInput();
            ts.linkOutput();
        }
    }
}
