package network.aika.fields;

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
    public double getNewValue() {
        return 1.0 - input.getNewValue();
    }

    @Override
    public boolean updateAvailable() {
        return input.updateAvailable();
    }

    @Override
    public double getUpdate() {
        return getNewValue() - getCurrentValue();
    }

    @Override
    public void acknowledgePropagated() {
        input.acknowledgePropagated();
    }
}