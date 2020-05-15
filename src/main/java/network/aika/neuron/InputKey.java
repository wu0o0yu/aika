package network.aika.neuron;

import java.util.Comparator;

public interface InputKey {

    Comparator<InputKey> INPUT_COMP = Comparator
            .<InputKey, NeuronProvider>comparing(s -> s.getPInput())
            .thenComparing(s -> s.getPatternScope());

    NeuronProvider getPInput();

    PatternScope getPatternScope();

    class PureInputKey implements InputKey {
        private NeuronProvider input;
        private PatternScope ps;

        public PureInputKey(NeuronProvider input, PatternScope ps) {
            this.input = input;
            this.ps = ps;
        }

        @Override
        public NeuronProvider getPInput() {
            return input;
        }

        @Override
        public PatternScope getPatternScope() {
            return ps;
        }
    }
}
