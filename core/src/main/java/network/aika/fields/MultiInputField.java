package network.aika.fields;

import java.util.ArrayList;
import java.util.Collection;

public class MultiInputField extends Field {

    private Collection<FieldLink> inputs;

    public MultiInputField(FieldObject reference, String label, Double tolerance) {
        super(reference, label, tolerance);
    }

    @Override
    protected void initIO() {
        super.initIO();

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
}
