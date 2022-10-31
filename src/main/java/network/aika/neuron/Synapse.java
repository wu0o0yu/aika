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
import network.aika.fields.QueueField;
import network.aika.neuron.activation.*;
import network.aika.fields.Field;
import network.aika.neuron.conjunctive.Scope;
import network.aika.neuron.visitor.ActLinkingOperator;
import network.aika.neuron.visitor.linking.LinkingOperator;
import network.aika.sign.Sign;
import network.aika.utils.Bound;
import network.aika.utils.Utils;
import network.aika.utils.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import static network.aika.fields.Fields.isTrue;
import static network.aika.neuron.activation.Timestamp.MAX;
import static network.aika.neuron.activation.Timestamp.MIN;
import static network.aika.sign.Sign.NEG;
import static network.aika.sign.Sign.POS;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Synapse<S extends Synapse, I extends Neuron, O extends Neuron<?, OA>, L extends Link<S, IA, OA>, IA extends Activation<?>, OA extends Activation> implements Element, Writable {

    private static final Logger log = LoggerFactory.getLogger(Synapse.class);

    protected NeuronProvider input;
    protected NeuronProvider output;

    private boolean isInputLinked;
    private boolean isOutputLinked;

    protected S template;

    protected QueueField weight = new QueueField(this, "weight", () -> {
        setModified();
    });


    protected boolean allowTraining = true;

    protected Scope scope;

    public Synapse() {
        this.scope = null;
    }

    public Synapse(Scope scope) {
        this.scope = scope;
    }

    public abstract double getSumOfLowerWeights();

    public boolean propagateCheck(IA iAct) {
        return true;
    }

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
/*
    protected boolean checkCausal(IA iAct, OA oAct) {
        return Link.isCausal(iAct, oAct);
    }
*/
    public boolean isPropagate() {
        double tsWeight = getWeight().getCurrentValue();
        double tnBias = getOutput().getBias().getCurrentValue();
        return tsWeight + tnBias > 0.0;
    }

    public boolean isLatentLinking() {
        return getWeight().getCurrentValue() + getSumOfLowerWeights() > 0.0;
    }

    public static boolean isLatentLinking(Synapse synA, Synapse synB) {
        double sumOfLowerWeights = Math.min(synA.getSumOfLowerWeights(), synB.getSumOfLowerWeights());
        return synA.getWeight().getCurrentValue() + synB.getWeight().getCurrentValue() + sumOfLowerWeights > 0.0;
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

    protected void linkStepB(Activation bsA, Synapse synA, Link linkA) {
        startVisitor(
                new ActLinkingOperator(bsA, synA, linkA, this),
                bsA
        );
    }

    public void linkAndPropagateOut(IA bs) {
        getOutput()
                .linkOutgoing(this, bs);

        getOutput()
                .latentLinkOutgoing(this, bs);

        if (isPropagate())
            propagate(bs);
    }

    public abstract void setModified();

    public void setInput(I input) {
        this.input = input.getProvider();
    }

    public void setOutput(O output) {
        this.output = output.getProvider();
    }

    public S instantiateTemplate(L l, I input, O output) {
        S s = instantiateTemplate(input, output);

        if(l != null)
            s.weight.setValue(weight.getCurrentValue());
 //           s.weight.setValue(computeInitialWeight(l.getInput()));

        return s;
    }

    /*
    protected double computeInitialWeight(IA iAct) {
        double initialWeight = weight.getCurrentValue();
        initialWeight -= iAct.getConfig().getLearnRate() *
                iAct.getNeuron().getSurprisal(POS, iAct.getAbsoluteRange(), true);
        return initialWeight;
    }
*/

    public S instantiateTemplate(I input, O output) {
        S s = instantiateTemplate();

        s.input = input.getProvider();
        s.output = output.getProvider();

        return s;
    }

    public S instantiateTemplate() {
        S s;
        try {
            s = (S) getClass().getConstructor().newInstance();
            initNewInstance(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    protected void initNewInstance(S s) {
        s.template = this;
        s.weight.setValue(weight.getCurrentValue());
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

    public boolean isInputLinked() {
        return isInputLinked;
    }

    public void linkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = true;
        in.addOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public void unlinkInput() {
        Neuron in = getInput();
        in.getLock().acquireWriteLock();
        isInputLinked = false;
        in.removeOutputSynapse(this);
        in.getLock().releaseWriteLock();
    }

    public boolean isOutputLinked() {
        return isOutputLinked;
    }

    public void linkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = true;
        out.addInputSynapse(this);
        out.getLock().releaseWriteLock();
    }

    public void unlinkOutput() {
        Neuron out = output.getNeuron();

        out.getLock().acquireWriteLock();
        isOutputLinked = false;
        out.removeInputSynapse(this);
        out.getLock().releaseWriteLock();
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


    public Field getWeight() {
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

    public S init(Neuron input, Neuron output, double initialWeight) {
        setInput((I) input);
        setOutput((O) output);
        getWeight().setValue(initialWeight);
        linkInput();
        linkOutput();

        return (S) this;
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + (input != null ? input.getNeuron().toKeyString() : "--")  + "](" + (isInputLinked ? "+" : "-") + ") " +
                getArrow() +
                " out:[" + (output != null ? output.getNeuron().toKeyString() : "--") + "](" + (isOutputLinked ? "+" : "-") + ")";
    }

    private String getArrow() {
        if(isPropagate())
            return "==>";
        if(isLatentLinking())
            return "-=>";
        return "-->";
    }
}
