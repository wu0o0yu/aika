package network.aika.neuron;

import java.util.Comparator;

public interface InputKey {

    Comparator<InputKey> INPUT_COMP = Comparator
            .<InputKey, Neuron>comparing(s -> s.getPInput())
            .thenComparing(s -> s.getPatternScope());

    Neuron getPInput();

    PatternScope getPatternScope();
}
