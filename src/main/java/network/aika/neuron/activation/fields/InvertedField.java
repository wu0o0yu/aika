package network.aika.neuron.activation.fields;

import java.util.function.Function;

public class InvertedField implements FieldOutput {

    FieldOutput input;

    public InvertedField(FieldOutput in) {
        this.input = in;
    }

    @Override
    public double getOldValue() {
        return 1.0 - input.getOldValue();
    }

    @Override
    public double getNewValue() {
        return 1.0 - input.getNewValue();
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public double getUpdate() {
        return getNewValue() - getOldValue();
    }

    @Override
    public void acknowledgePropagated() {
        input.acknowledgePropagated();
    }
}