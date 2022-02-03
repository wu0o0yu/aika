package network.aika.fields;

public class ConstantField implements FieldOutput, MultiSourceFieldOutput {

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
    public double getNewValue() {
        return value;
    }

    @Override
    public boolean updateAvailable() {
        return !initialized;
    }

    @Override
    public boolean updateAvailable(int updateArg) {
        return updateAvailable();
    }
    @Override
    public double getUpdate() {
        acknowledgePropagated();
        return !initialized ? value : 0.0;
    }

    @Override
    public double getUpdate(int updateArg) {
        return getUpdate();
    }

    @Override
    public void acknowledgePropagated() {
        initialized = true;
    }

    @Override
    public void acknowledgePropagated(int updateArg) {
        acknowledgePropagated();
    }
}
