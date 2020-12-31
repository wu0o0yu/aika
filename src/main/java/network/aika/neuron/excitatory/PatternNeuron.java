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
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import network.aika.neuron.activation.direction.Direction;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.activation.Visitor.Transition.ACT;
import static network.aika.neuron.activation.Visitor.Transition.LINK;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternNeuron extends ExcitatoryNeuron<PatternSynapse> {

    private static final Logger log = LoggerFactory.getLogger(PatternNeuron.class);

    public static byte type;

    private String tokenLabel;

    public PatternNeuron() {
        super();
    }

    public PatternNeuron(NeuronProvider p) {
        super(p);
    }

    private PatternNeuron(Model model) {
        super(model);
    }

    @Override
    public void updateAttributes(Node node) {

    }


    @Override
    public Scope[] getInitialScopes(Direction dir) {
        return dir == Direction.INPUT ?
                new Scope[]{ Scope.P_SAME } :
                new Scope[]{ Scope.PP_SAME, Scope.PP_INPUT, Scope.I_INPUT };
    }

    @Override
    public boolean checkGradientThreshold(Activation act) {
        Neuron n = act.getNeuron();

        if(n.getSurprisal(POS) < 1.4)
            return false;

        return true;
    }

    @Override
    public PatternNeuron instantiateTemplate() {
        PatternNeuron n = new PatternNeuron(getModel());
        n.getTemplates().add(this);
        n.getTemplates().addAll(getTemplates());
        return n;
    }

    @Override
    public void transition(Visitor v, Activation act) {
        if (v.downUpDir == OUTPUT)
            return;

        Visitor nv = v.prepareNextStep(act, null, v.getScopes(), ACT);

        if(nv == null)
            return;

        nv.downUpDir = OUTPUT;

        act.followLinks(nv);
    }

    @Override
    public byte getType() {
        return type;
    }

    public void setTokenLabel(String tokenLabel) {
        this.tokenLabel = tokenLabel;
    }

    public String getTokenLabel() {
        return tokenLabel;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);

        out.writeBoolean(tokenLabel != null);
        if(tokenLabel != null) {
            out.writeUTF(tokenLabel);
        }
    }

    @Override
    public void readFields(DataInput in, Model m) throws Exception {
        super.readFields(in, m);

        if(in.readBoolean()) {
            tokenLabel = in.readUTF();
        }
    }
}
