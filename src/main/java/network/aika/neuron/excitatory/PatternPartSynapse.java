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
package network.aika.neuron.excitatory;

import network.aika.Model;
import network.aika.neuron.*;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import network.aika.neuron.activation.Scope;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static network.aika.neuron.activation.Fired.NOT_FIRED;
import static network.aika.neuron.activation.Scope.*;
import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternPartSynapse<I extends Neuron<?>> extends ExcitatorySynapse<I, PatternPartNeuron> {

    public static byte type;

    public boolean isNegative;

    public boolean isRecurrent;
    public boolean inputScope;
    public boolean isSamePattern;

    public PatternPartSynapse() {
        super();
    }

    public PatternPartSynapse(I input, PatternPartNeuron output, Synapse template) {
        super(input, output, template);
    }

    public PatternPartSynapse(I input, PatternPartNeuron output, Synapse template, boolean isNegative, boolean isRecurrent, boolean inputScope, boolean isSamePattern) {
        super(input, output, template);
        this.isNegative = isNegative;
        this.isRecurrent = isRecurrent;
        this.inputScope = inputScope;
        this.isSamePattern = isSamePattern;
    }

    @Override
    public void updateReference(Link l) {
        if(isNegative)
            return;

        Reference iRef = l.getInput().getReference();
        if(iRef == null)
            return;

        // TODO: find a better solution.
        Synapse ts = l.getSynapse().getTemplate();
        Templates t = getModel().getTemplates();
        if(ts != t.RELATED_INPUT_SYNAPSE_FROM_INHIBITORY_TEMPLATE && ts != t.RELATED_INPUT_SYNAPSE_FROM_PP_TEMPLATE) {
            l.getOutput().propagateReference(iRef);
        }
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if (v.startDir == INPUT) {
            return !act.getNeuron().isInputNeuron() && this == act.getModel().getTemplates().RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE;
        } else {
            return act.getNeuron() instanceof PatternNeuron || !isInputScope();
        }
    }

    @Override
    protected boolean checkCausality(Activation fromAct, Activation toAct, Visitor v) {
        if(!isRecurrent) {
            return fromAct.getFired() != NOT_FIRED && fromAct.getFired().compareTo(toAct.getFired()) <= 0;
        } else {
            return v.getSelfRef();
        }
    }

    @Override
    public PatternPartSynapse instantiateTemplate(I input, PatternPartNeuron output) {
        if(!input.getTemplates().contains(getInput()))
            return null;

        return new PatternPartSynapse(input, output, this, isNegative, isRecurrent, inputScope, isSamePattern);
    }

    @Override
    public Activation branchIfNecessary(Activation oAct, Visitor v) {
        if (getOutput().isInputNeuron())
            return null;

        if(isRecurrent() && !v.getSelfRef())
            return null;

        if (isNegative() && !v.getSelfRef()) {
            oAct = oAct.createBranch(this);
        }
        return oAct;
    }

    @Override
    public Set<ScopeEntry> transition(ScopeEntry s, Direction dir, Direction startDir, boolean checkFinalRequirement) {
        if(inputScope) {
            if(dir == INPUT) {
                if(checkFinalRequirement && s.getScope() != PP_SAME) {
                    return Collections.emptySet();
                }

                switch (s.getScope()) {
                    case PP_SAME:
                        return s.nextSet(PP_INPUT);
                    case PP_RELATED_SAME:
                    case PP_INPUT:
                        return s.nextSet(PP_RELATED_INPUT);
                    case I_SAME:
                        return s.nextSet(I_INPUT);
                }
            } else {
                if(checkFinalRequirement && s.getScope() != PP_INPUT) {
                    return Collections.emptySet();
                }

                switch (s.getScope()) {
                    case PP_INPUT:
                        if(startDir == INPUT) {
                            return s.nextSet(PP_SAME, PP_RELATED_INPUT);
                        } else if(startDir == OUTPUT) {
                            return s.nextSet(PP_SAME);
                        }
                    case PP_RELATED_INPUT:
                        return s.nextSet(PP_RELATED_SAME);
                    case I_INPUT:
                        return s.nextSet(I_SAME);
                }
            }
        }

        if(isSamePattern) {
            if(checkFinalRequirement) {
                if(dir == INPUT && s.getScope() != PP_SAME) {
                    return Collections.emptySet();
                }
                if(dir == OUTPUT && s.getScope() != PP_RELATED_SAME && s.getScope() != PP_SAME) {
                    return Collections.emptySet();
                }
            }

            switch (s.getScope()) {
                case PP_SAME:
                    return s.nextSet(PP_RELATED_SAME, PP_SAME);
                case PP_RELATED_SAME:
                    return s.nextSet(PP_SAME);
                case PP_RELATED_INPUT:
                case PP_INPUT:
                case P_SAME:
                    return Collections.singleton(s);
            }
        }

        return Collections.emptySet();
    }

    @Override
    public Reference getReference(Link l) {
        return (
                isRecurrent ?
                        l.getOutput() :
                        l.getInput()
        ).getReference();
    }

    @Override
    public byte getType() {
        return type;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public boolean isRecurrent() {
        return isRecurrent;
    }

    public boolean isInputScope() {
        return inputScope;
    }

    public boolean isSamePattern() {
        return isSamePattern;
    }

    public void setWeight(double weight) {
        super.setWeight(weight);
        output.getNeuron().setModified(true);
    }

    public void addWeight(double weightDelta) {
        super.addWeight(weightDelta);
        output.getNeuron().setModified(true);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(isNegative);
        out.writeBoolean(isRecurrent);
        out.writeBoolean(inputScope);
        out.writeBoolean(isSamePattern);
    }

    @Override
    public void readFields(DataInput in, Model m) throws IOException {
        super.readFields(in, m);

        isNegative = in.readBoolean();
        isRecurrent = in.readBoolean();
        inputScope = in.readBoolean();
        isSamePattern = in.readBoolean();
    }
}
