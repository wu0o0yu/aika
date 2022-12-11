/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.neuron;

import network.aika.Model;
import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.fields.QueueSumField;
import network.aika.fields.SumField;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Element;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Timestamp;
import network.aika.neuron.conjunctive.Scope;
import network.aika.neuron.visitor.linking.LinkingOperator;
import network.aika.utils.Utils;
import network.aika.utils.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import static network.aika.direction.Direction.INPUT;
import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.activation.Timestamp.MAX;
import static network.aika.neuron.activation.Timestamp.MIN;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<S extends Synapse, I extends Neuron, O extends Neuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation<?>, OA extends Activation> implements Element, Writable {

    protected NeuronProvider input;
    protected NeuronProvider output;

    boolean isInputLinked;
    boolean isOutputLinked;

    protected S template;

    public static int dbgCounter = 0;
    public int dbgId = dbgCounter++;

    protected SumField weight = (SumField) new QueueSumField(this, "weight", true)
            .addListener(() ->
                    setModified()
            );

    protected boolean allowTraining = true;

    protected Scope scope;

    public Synapse() {
        this.scope = null;
    }

    public Synapse(Scope scope) {
        this.scope = scope;
    }

    public abstract double getSumOfLowerWeights();

    public boolean checkLinkingEvent(Activation act) {
        return isTrue(act.getIsFired());
    }

    public Scope getScope() {
        return scope;
    }

    public void propagate(IA iAct) {
        if(propagateLinkExists(iAct))
            return;

        OA oAct = getOutput().createActivation(iAct.getThought());
        oAct.init(this, iAct);

        createLink(iAct, oAct);
    }

    protected void warmUpRelatedInputNeurons(IA bs) {
    }

    public double getPropagatePreNetUB(IA iAct) {
        if (getOutput().isCallActivationCheckCallback() &&
                !getModel().getActivationCheckCallBack().check(iAct)) {
            return -1000.0;
        }

        return getOutput().getBias().getCurrentValue() +
                getWeight().getCurrentValue();
    }

    public static double getLatentLinkingPreNetUB(Synapse synA, Synapse synB) {
        double preUB = synA.getWeight().getCurrentValue();

        if(synB != null) {
            preUB += synB.getWeight().getCurrentValue() +
                    Math.min(
                            synA.getSumOfLowerWeights(),
                            synB.getSumOfLowerWeights()
                    );
        } else
            preUB += synA.getSumOfLowerWeights();

        return preUB;
    }

    public static boolean latentActivationExists(Synapse synA, Synapse synB, Activation iActA, Activation iActB) {
        Stream<Link> linksA = iActA.getOutputLinks(synA);
        return linksA.map(lA -> lA.getOutput())
                .map(oAct -> oAct.getInputLink(synB))
                .filter(Objects::nonNull)
                .map(lB -> lB.getInput())
                .anyMatch(iAct -> iAct == iActB);
    }

    public boolean linkExists(IA iAct, OA oAct) {
        assert iAct.getNeuron() == getInput();

        Link existingLink = oAct.getInputLink(iAct.getNeuron());
        if(existingLink == null)
            return false;

        assert existingLink.getSynapse() == this;
        assert existingLink.getInput() == iAct;

        return true;
    }

    public boolean linkExists(OA oAct) {
        return oAct.getInputLink(getInput()) != null;
    }

    public boolean propagateLinkExists(IA iAct) {
        return iAct.getOutputLinks(this)
                        .findAny()
                        .isPresent();
    }

    public abstract void startVisitor(LinkingOperator c, Activation bs);

    public void linkAndPropagateOut(IA bs) {
        getOutput()
                .linkOutgoing(this, bs);

        getOutput()
                .latentLinkOutgoing(this, bs);

        if (getPropagatePreNetUB(bs) > 0.0)
            propagate(bs);
        else if(getStoredAt() == INPUT)
            warmUpRelatedInputNeurons(bs);
    }

    public void setModified() {
        getStoredAt()
                .getNeuron(this)
                .setModified();
    }

    public void count(L l) {
    }

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public S instantiateTemplate(I input, O output) {
        S s;
        try {
            s = (S) getClass().getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        s.init(input, output, this, weight.getCurrentValue());
        return s;
    }

    public S init(Neuron input, Neuron output, double initialWeight) {
        return init(input, output, null, initialWeight);
    }

    public S init(Neuron input, Neuron output, S templateSyn, double initialWeight) {
        setInput((I) input);
        setOutput((O) output);
        template = templateSyn;

        link();

        weight.setValue(initialWeight);

        return (S) this;
    }

    protected void link() {
        input.linkInput(this, true);
        output.linkOutput(this, true);
    }

    public abstract L createLink(IA input, OA output);

    public void setWeight(double w) {
        weight.setValue(w);
    }

    public boolean isAllowTraining() {
        return allowTraining;
    }

    public void setAllowTraining(boolean allowTraining) {
        this.allowTraining = allowTraining;
    }

    public S getTemplate() {
        return template;
    }

    public boolean isOfTemplate(Synapse templateSynapse) {
        if(template == templateSynapse)
            return true;

        if(template == null)
            return false;

        return template.isOfTemplate(templateSynapse);
    }

    public Direction getStoredAt() {
        return INPUT;
    }

    public boolean isInputLinked() {
        return isInputLinked;
    }

    public boolean isOutputLinked() {
        return isOutputLinked;
    }

    public NeuronProvider getPInput() {
        return input;
    }

    public NeuronProvider getPOutput() {
        return output;
    }

    public I getInput() {
        return (I) input.getNeuron();
    }

    public O getOutput() {
        return (O) output.getNeuron();
    }

    public Model getModel() {
        return output != null ?
                output.getModel() :
                null;
    }


    public SumField getWeight() {
        return weight;
    }

    public boolean isZero() {
        return Utils.belowTolerance(weight.getCurrentValue());
    }

    public boolean isNegative() {
        return weight.getCurrentValue() < 0.0;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeUTF(getClass().getName());

        out.writeLong(input.getId());
        out.writeLong(output.getId());

        out.writeBoolean(isInputLinked);
        out.writeBoolean(isOutputLinked);

        weight.write(out);
    }

    public static Synapse read(DataInput in, Model m) throws IOException {
        String synClazz = in.readUTF();

        Synapse s = (Synapse) m.modelClass(synClazz);
        s.readFields(in, m);
        return s;
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        input = m.lookupNeuron(in.readLong());
        output = m.lookupNeuron(in.readLong());

        isInputLinked = in.readBoolean();
        isOutputLinked = in.readBoolean();

        weight.readFields(in, m);
    }

    @Override
    public Timestamp getCreated() {
        return MIN;
    }

    @Override
    public Timestamp getFired() {
        return MAX;
    }

    @Override
    public Thought getThought() {
        Model m = getModel();
        return m != null ?
                m.getCurrentThought() :
                null;
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + (input != null ? input.toKeyString() : "--")  + "](" + (isInputLinked ? "+" : "-") + ") " +
                getArrow() +
                " out:[" + (output != null ? output.toKeyString() : "--") + "](" + (isOutputLinked ? "+" : "-") + ")";
    }

    private String getArrow() {
        return "-->";
    }
}
