package network.aika.fields;

import java.util.ArrayList;
import java.util.Collection;

public class MultiInputField extends Field {

    private Collection<FieldLink> inputs;

    public MultiInputField(FieldObject reference, String label, Double tolerance) {
        super(reference, label, tolerance);
    }

    public MultiInputField(FieldObject reference, String label, int maxRounds, Double tolerance, boolean weakRefs) {
        super(reference, label, maxRounds, tolerance, weakRefs);
    }

    @Override
    protected void initIO(boolean weakRefs) {
        super.initIO(weakRefs);

        // TODO: implement weakRefs
        inputs = new ArrayList<>();
    }

    @Override
    public int getNextArg() {
        return inputs.size();
    }

    @Override
    public void addInput(FieldLink l) {
        inputs.add(l);
    }

    @Override
    public void removeInput(FieldLink l) {
        inputs.remove(l);
    }

    @Override
    public Collection<FieldLink> getInputs() {
        return inputs;
    }

    public double verifySum() {
        return inputs.stream()
                .mapToDouble(i -> i.input.getValue(lastRound, 0.0))
                .sum();
    }
}
