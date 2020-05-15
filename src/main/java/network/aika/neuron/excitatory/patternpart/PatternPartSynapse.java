package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.NeuronProvider;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.Neuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;

public class PatternPartSynapse<I extends Neuron> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    private PatternScope patternScope;
    private boolean isRecurrent;
    private boolean isNegative;

    public PatternPartSynapse() {
    }

    public PatternPartSynapse(NeuronProvider input, NeuronProvider output, PatternScope patternScope, boolean isRecurrent, boolean isNegative, boolean propagate) {
        super(input, output);
        this.patternScope = patternScope;
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
        this.propagate = propagate;
    }

    @Override
    public void init(PatternScope patternScope, Boolean isRecurrent, Boolean isNegative, boolean propagate) {
        this.patternScope = patternScope;
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
        this.propagate = propagate;
    }

    @Override
    public byte getType() {
        return type;
    }

    public PatternScope getPatternScope() {
        return patternScope;
    }

    public void setPatternScope(PatternScope patternScope) {
        this.patternScope = patternScope;
    }

    public void setRecurrent(boolean recurrent) {
        isRecurrent = recurrent;
    }

    @Override
    public boolean isRecurrent() {
        return isRecurrent;
    }

    @Override
    public boolean isNegative() {
        return isNegative;
    }


    public static class Builder extends Synapse.Builder {
        private PatternScope patternScope;
        private boolean isRecurrent;
        private boolean isNegative;

        public Builder setPatternScope(PatternScope ps) {
            patternScope = ps;
            return this;
        }

        public Builder setRecurrent(boolean recurrent) {
            isRecurrent = recurrent;
            return this;
        }

        public Builder setNegative(boolean negative) {
            isNegative = negative;
            return this;
        }

        public Synapse getSynapse(NeuronProvider outputNeuron) {
            return new PatternPartSynapse(inputNeuron, outputNeuron, patternScope, isRecurrent, isNegative, propagate);
        }
    }
}
