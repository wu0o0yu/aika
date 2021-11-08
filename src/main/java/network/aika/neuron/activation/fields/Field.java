package network.aika.neuron.activation.fields;

import network.aika.Model;
import network.aika.neuron.Synapse;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.function.Consumer;

public class Field implements FieldInput, FieldOutput, Writable {

    private double accumulated = 0.0;
    private double update = 0.0;

    private FieldUpdateEvent fieldListener;

    public Field() {
    }

    public Field(FieldUpdateEvent fieldListener) {
        this.fieldListener = fieldListener;
    }

    public Field(double x) {
        this.accumulated = x;
    }

    public Field(double x, FieldUpdateEvent fieldListener) {
        this.accumulated = x;
        this.fieldListener = fieldListener;
    }

    public double getAccumulated() {
        return accumulated;
    }

    public void set(double g) {
        update = g - accumulated;

        if(fieldListener == null || Utils.belowTolerance(update))
            return;

        fieldListener.updated();
    }

    public void add(double g) {
        update += g;

        if(fieldListener == null || Utils.belowTolerance(update))
            return;

        fieldListener.updated();
    }

    public void propagateUpdate(FieldInput to) {
        propagateUpdate(u -> to.add(u));
    }

    public void propagateUpdate(Consumer<Double> to) {
        Double u = propagateUpdate();
        if(u == null)
            return;

        to.accept(u);
    }

    private Double propagateUpdate() {
        if(Utils.belowTolerance(update))
            return null;

        double result = update;
        accumulated += update;
        update = 0.0;
        return result;
    }

    @Override
    public void write(DataOutput out) throws IOException {

    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {

    }
}
