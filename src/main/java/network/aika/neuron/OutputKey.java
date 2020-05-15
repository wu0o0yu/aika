package network.aika.neuron;

import java.util.Comparator;

public interface OutputKey {

    Comparator<OutputKey> OUTPUT_COMP = Comparator
            .<OutputKey, Neuron>comparing(s -> s.getPOutput())
            .thenComparing(s -> s.getPatternScope());

    Neuron getPOutput();

    PatternScope getPatternScope();

    class PureOutputKey implements OutputKey {
        private Neuron output;
        private PatternScope ps;

        public PureOutputKey(Neuron output, PatternScope ps) {
            this.output = output;
            this.ps = ps;
        }

        @Override
        public Neuron getPOutput() {
            return output;
        }

        @Override
        public PatternScope getPatternScope() {
            return ps;
        }
    }
}
