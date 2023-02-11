package network.aika.fields;

import network.aika.FieldObject;

import java.util.ArrayList;
import java.util.Collection;

public class SumField extends Field {

    private Collection<FieldLink> inputs;

    public SumField(FieldObject reference, String label, Double tolerance) {
        super(reference, label, tolerance);
    }

    public SumField(FieldObject reference, String label, Double tolerance, boolean weakRefs) {
        super(reference, label, tolerance, weakRefs);
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
}
