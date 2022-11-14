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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.fields.*;
import network.aika.neuron.Synapse;
import network.aika.neuron.visitor.DownVisitor;
import network.aika.neuron.visitor.UpVisitor;
import network.aika.neuron.visitor.selfref.SelfRefDownVisitor;
import network.aika.steps.link.LinkingIn;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;
import static network.aika.fields.ThresholdOperator.Type.ABOVE;
import static network.aika.neuron.activation.Timestamp.FIRED_COMPARATOR;

/**
 *
 * @author Lukas Molzberger
 */
public abstract class Link<S extends Synapse, I extends Activation<?>, O extends Activation> implements Element {

    protected S synapse;

    protected final I input;
    protected O output;

    protected AbstractFunction weightedInputUB;
    protected AbstractFunction weightedInputLB;
    protected AbstractFunction backwardsGradient;

    protected ThresholdOperator onTransparent;

    public Link(S s, I input, O output) {
        this.synapse = s;
        this.input = input;
        this.output = output;

        init();

        if(input != null && output != null) {
            initOnTransparent();
            initWeightInputUB();
            initWeightInputLB();

            if (getConfig().isTrainingEnabled() && getSynapse().isAllowTraining())
                output.isFinal.addEventListener(() -> {
                            connectGradientFields();
                            connectWeightUpdate();
                        }
                );
        }

        getThought().onLinkCreationEvent(this);

        addInputLinkingStep();
    }

    protected void addInputLinkingStep() {
        LinkingIn.add(this);
    }

    public void selfRefVisitDown(SelfRefDownVisitor v) {
        v.next(this);
    }

    public void bindingVisitDown(DownVisitor v) {
        v.next(this);
    }

    public void bindingVisitUp(UpVisitor v) {
        v.next(this);
    }

    public void patternVisitDown(DownVisitor v) {
    }

    public void patternVisitUp(UpVisitor v) {
    }

    public void rangeVisitDown(DownVisitor v) {
    }

    public void inhibVisitDown(DownVisitor v) {
        v.next(this);
    }

    public void inhibVisitUp(UpVisitor v) {
        v.next(this);
    }


    protected void initOnTransparent() {
        onTransparent = threshold(
                this,
                "onTransparent",
                0.0,
                ABOVE,
                mul(
                        this,
                        "isFired * weight",
                        synapse.getWeight(),
                        input.isFired
                )
        );
    }

    protected void connectGradientFields() {
    }

    public void instantiateTemplate( Activation iAct, Activation oAct) {
        S instSyn = (S) synapse
                .instantiateTemplate(
                        this,
                        iAct.getNeuron(),
                        oAct.getNeuron()
                );
        instSyn.linkOutput();

        instSyn.createLink(iAct, oAct);
    }

    public abstract void connectWeightUpdate();

    protected void initWeightInputUB() {
        weightedInputUB = initWeightedInput(true);
        connect(weightedInputUB, getOutput().getNet(true));
    }

    protected void initWeightInputLB() {
        weightedInputLB = initWeightedInput(false);
        connect(weightedInputLB, getOutput().getNet(false));
    }

    protected Multiplication initWeightedInput(boolean upperBound) {
        return mul(
                this,
                "iAct(id:" + getInput().getId() + ").value" + (upperBound ? "UB" : "LB") + " * s.weight",
                getInputValue(upperBound),
                synapse.getWeight()
        );
    }

    public void init() {
        if(getInput() != null)
            linkInput();

        if(getOutput() != null)
            linkOutput();
    }

    public FieldOutput getOnTransparent() {
        return onTransparent;
    }

    public AbstractFunction getWeightedInput(boolean upperBound) {
        return upperBound ? weightedInputUB : weightedInputLB;
    }

    public AbstractFunction getWeightedInputUB() {
        return weightedInputUB;
    }

    public AbstractFunction getWeightedInputLB() {
        return weightedInputLB;
    }

    public FieldOutput getBackwardsGradient() {
        return backwardsGradient;
    }

    @Override
    public Timestamp getFired() {
        return isCausal() ? input.getFired() : output.getFired();
    }

    @Override
    public Timestamp getCreated() {
        return isCausal() ? input.getCreated() : output.getCreated();
    }

    public FieldOutput getInputValue(boolean upperBound) {
        return input.getValue(upperBound);
    }

    public S getSynapse() {
        return synapse;
    }

    public void setSynapse(S synapse) {
        this.synapse = synapse;
    }

    public I getInput() {
        return input;
    }

    public O getOutput() {
        return output;
    }

    public boolean isCausal() {
        return input == null || isCausal(input, output);
    }

    public static boolean isCausal(Activation iAct, Activation oAct) {
        return FIRED_COMPARATOR.compare(iAct.getFired(), oAct.getFired()) < 0;
    }

    public void linkInput() {
        if(input == null)
            return;

        Link el = input.outputLinks.put(
                new OutputKey(output.getNeuronProvider(), output.getId()),
                this
        );

        assert el == null;
    }

    public void linkOutput() {
        Link el = (Link) output.inputLinks.put(
                input != null ? input.getNeuronProvider() : synapse.getPInput(),
                this
        );
        assert el == null;
    }

    public void unlinkInput() {
        OutputKey ok = output.getOutputKey();
        input.outputLinks.remove(ok, this);
    }

    public void unlinkOutput() {
        output.inputLinks.remove(input.getNeuronProvider(), this);
    }

    public boolean isNegative() {
        return synapse.isNegative();
    }

    @Override
    public Thought getThought() {
        return output.getThought();
    }

    private String getInputKeyString() {
        return (input != null ? input.toKeyString() : "id:X n:[" + synapse.getInput() + "]");
    }

    private String getOutputKeyString() {
        return (output != null ? output.toKeyString() : "id:X n:[" + synapse.getOutput() + "]");
    }

    public String toString() {
        return getClass().getSimpleName() +
                " in:[" + getInputKeyString() + "] " +
                "--> " +
                "out:[" + getOutputKeyString() + "]";
    }
}
