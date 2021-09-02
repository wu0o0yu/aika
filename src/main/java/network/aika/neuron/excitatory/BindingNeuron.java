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
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.steps.tasks.AlternateBranchTask;
import network.aika.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

/**
 * @author Lukas Molzberger
 */
public class BindingNeuron extends ExcitatoryNeuron<BindingNeuronSynapse> {
    private static final Logger log = LoggerFactory.getLogger(BindingNeuron.class);

    public BindingNeuron() {
        super();
    }

    public BindingNeuron(Model model, boolean addProvider) {
        super(model, addProvider);
    }


    public void transition(ActVisitor v, Activation act) {
        if(v.getCurrentDir() == OUTPUT && enteringAlternateBranch(v))
            return;

        act.followLinks(v);
    }

    @Override
    public void alternateBranchTransition(ActVisitor v, Activation act) {
        AlternateBranchTask abTask = (AlternateBranchTask) v.getVisitorTask();

        abTask.checkBranch(act);

        if(!abTask.isAlternateBranch())
            act.followLinks(v);
    }

    public boolean enteringAlternateBranch(ActVisitor v) {
        Activation act = v.getActivation();

        AlternateBranchTask abTask = new AlternateBranchTask();

        act.getAllBranches().forEach(bAct ->
                abTask.neuronTransition(
                        new ActVisitor(
                                v,
                                abTask,
                                bAct,
                                v.getScope(),
                                INPUT,
                                INPUT
                        ),
                        bAct
                )
        );

        return abTask.isAlternateBranch();
    }

    @Override
    public boolean allowTemplatePropagate(Activation act) {
        Neuron n = act.getNeuron();

        if(n.isInputNeuron())
            return false;

        Utils.checkTolerance(act, act.getOutputGradientSum());

        return true;
    }

    @Override
    public BindingNeuron instantiateTemplate(boolean addProvider) {
        BindingNeuron n = new BindingNeuron(getModel(), addProvider);
        initFromTemplate(n);

        return n;
    }
}
