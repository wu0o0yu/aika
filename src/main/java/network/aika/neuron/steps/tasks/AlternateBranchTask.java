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
package network.aika.neuron.steps.tasks;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.visitor.ActVisitor;
import network.aika.neuron.activation.visitor.VisitorTask;

/**
 *
 * @author Lukas Molzberger
 */
public class AlternateBranchTask implements VisitorTask {

    private boolean isAlternateBranch;

    public AlternateBranchTask() {
    }

    public void checkBranch(Activation act) {
        if(act.isMarked())
            isAlternateBranch = true;
    }

    @Override
    public void processTask(ActVisitor v) {

    }

    @Override
    public void neuronTransition(ActVisitor v, Activation act) {
        act.getNeuron()
                .alternateBranchTransition(v, act);
    }

    @Override
    public void synapseTransition(ActVisitor v, Synapse s, Link l) {
        s.alternateBranchTransition(v, s, l);
    }

    public boolean isAlternateBranch() {
        return isAlternateBranch;
    }

    public String toString() {
        return "AlternateBranchTask";
    }

    public String getTaskDescription() {
        return toString();
    }
}
