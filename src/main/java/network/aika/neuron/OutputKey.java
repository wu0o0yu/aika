package network.aika.neuron;

import java.util.Comparator;

public interface OutputKey {

    Comparator<OutputKey> OUTPUT_COMP = Comparator
            .<OutputKey, Neuron>comparing(s -> s.getPOutput())
            .thenComparing(s -> s.getPatternScope());

    Neuron getPOutput();

    PatternScope getPatternScope();
}
