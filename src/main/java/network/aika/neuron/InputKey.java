package network.aika.neuron;

import java.util.Comparator;

public interface InputKey {

    Comparator<InputKey> INPUT_COMP = Comparator
            .<InputKey, Neuron>comparing(s -> s.getPInput())
            .thenComparing(s -> s.getPatternScope());

    Neuron getPInput();

    PatternScope getPatternScope();

    class PureInputKey implements InputKey {
        private Neuron input;
        private PatternScope ps;

        public PureInputKey(Neuron input, PatternScope ps) {
            this.input = input;
            this.ps = ps;
        }

        @Override
        public Neuron getPInput() {
            return input;
        }

        @Override
        public PatternScope getPatternScope() {
            return ps;
        }
    }
}
