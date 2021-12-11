package network.aika.neuron.activation.fields;

public class ConstantField implements FieldOutput {

    public static final ConstantField ZERO = new ConstantField(0.0);
    public static final ConstantField ONE = new ConstantField(1.0);

    private final double value;
    private boolean initialized = false;

    public ConstantField(double value) {
        this.value = value;
    }

    @Override
    public double getCurrentValue() {
        return value;
    }

    @Override
    public double getNewValue(boolean ack) {
        return value;
    }

    @Override
    public boolean updateAvailable() {
        return updateAvailable(1);
    }

    @Override
    public boolean updateAvailable(int updateArg) {
        return !initialized;
    }

    @Override
    public double getUpdate(int updateArg, boolean ack) {
        if(ack)
            acknowledgePropagated();
        return !initialized ? value : 0.0;
    }

    @Override
    public void acknowledgePropagated() {
        initialized = true;
    }
}
