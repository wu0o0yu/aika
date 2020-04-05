package network.aika.neuron.excitatory.patternpart;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.pattern.PatternNeuron;

public class PatternPartSynapse<I extends TNeuron> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    private PatternScope patternScope;
    private boolean isRecurrent;
    private boolean isNegative;


    public PatternPartSynapse() {
    }

    public PatternPartSynapse(Neuron input, Neuron output, PatternScope patternScope, boolean isRecurrent, boolean isNegative) {
        super(input, output);
        this.patternScope = patternScope;
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
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

    @Override
    public boolean isPropagate() {
        return input.get() instanceof PatternNeuron;
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
        return false;
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

        public Synapse getSynapse(Neuron outputNeuron) {
            PatternPartSynapse s = (PatternPartSynapse) super.getSynapse(outputNeuron);

            return s;
        }

        protected SynapseFactory getSynapseFactory() {
            return (input, output) -> new PatternPartSynapse(input, output, patternScope, isRecurrent, isNegative);
        }
    }
}
