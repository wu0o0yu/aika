package network.aika.neuron.activation.fields;

public class InvertedField implements FieldOutput {

    FieldOutput input;

    public InvertedField(FieldOutput in) {
        this.input = in;
    }

    @Override
    public double getCurrentValue() {
        return 1.0 - input.getCurrentValue();
    }

    @Override
    public double getNewValue(boolean ack) {
        return 1.0 - input.getNewValue(ack);
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public boolean updateAvailable(int updateArg) {
        return input.updateAvailable(updateArg);
    }

    @Override
    public double getUpdate(int updateArg, boolean ack) {
        return getNewValue(ack) - getCurrentValue();
    }

    @Override
    public void acknowledgePropagated() {
        input.acknowledgePropagated();
    }
}