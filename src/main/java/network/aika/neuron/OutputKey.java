package network.aika.neuron;

import java.util.Comparator;

public interface OutputKey {

    Comparator<OutputKey> OUTPUT_COMP = Comparator
            .<OutputKey, NeuronProvider>comparing(s -> s.getPOutput())
            .thenComparing(s -> s.getPatternScope());

    NeuronProvider getPOutput();

    PatternScope getPatternScope();

    class PureOutputKey implements OutputKey {
        private NeuronProvider output;
        private PatternScope ps;

        public PureOutputKey(NeuronProvider output, PatternScope ps) {
            this.output = output;
            this.ps = ps;
        }

        @Override
        public NeuronProvider getPOutput() {
            return output;
        }

        @Override
        public PatternScope getPatternScope() {
            return ps;
        }
    }
}
