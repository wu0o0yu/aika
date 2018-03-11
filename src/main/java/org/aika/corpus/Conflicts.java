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
package org.aika.corpus;


import org.aika.lattice.NodeActivation;
import org.aika.neuron.Activation;
import org.aika.neuron.Linker.Direction;

import java.util.*;

import static org.aika.neuron.Linker.Direction.INPUT;

/**
 * The class {@code Conflicts} handles between different interpretation options for a given text.
 *
 * @author Lukas Molzberger
 */
public class Conflicts {

    public SortedSet<InterpretationNode> primary = new TreeSet<>();
    public Set<InterpretationNode> secondary = new TreeSet<>();


    public static boolean isConflicting(Activation a, Activation b) {
        if(a.key.interpretation.conflicts.secondary.contains(b.key.interpretation)) return true;
        if(b.key.interpretation.conflicts.secondary.contains(a.key.interpretation)) return true;

        return false;
    }


    public static void linkConflicts(Activation act, long v, Direction dir) {
        for (Activation.SynapseActivation sa : (dir == INPUT ? act.neuronInputs : act.neuronOutputs)) {
            if(sa.synapse.isNegative() && sa.synapse.key.isRecurrent) {
                Activation oAct = (dir == INPUT ? act : sa.output);
                Activation iAct = (dir == INPUT ? sa.input : act);

                markConflicts(iAct, oAct, v);

                addConflict(oAct, iAct, v);
            }
        }
    }


    private static void addConflict(Activation oAct, Activation iAct, long v) {
        if (iAct.key.interpretation.markedConflict == v) {
            if (iAct != oAct) {
                add(oAct.key.interpretation, iAct.key.interpretation);
            }
        } else {
            for (Activation.SynapseActivation sa : iAct.neuronInputs) {
                addConflict(oAct, sa.input, v);
            }
        }
    }


    private static void markConflicts(Activation iAct, Activation oAct, long v) {
        oAct.key.interpretation.markedConflict = v;
        for (Activation.SynapseActivation sa : iAct.neuronOutputs) {
            if (sa.synapse.key.isRecurrent && sa.synapse.isNegative()) {
                sa.output.key.interpretation.markedConflict = v;
            }
        }
    }


    public static Collection<InterpretationNode> getConflicting(InterpretationNode n) {
        ArrayList<InterpretationNode> conflicts = new ArrayList<>();
        Conflicts.collectConflicting(conflicts, n);
        return conflicts;
    }


    private static void collectConflicting(Collection<InterpretationNode> results, InterpretationNode n) {
        assert n.primId >= 0;
        results.addAll(n.conflicts.primary);
        results.addAll(n.conflicts.secondary);
    }


    public static void add(InterpretationNode primary, InterpretationNode secondary) {
        primary.conflicts.primary.add(secondary);
        secondary.conflicts.secondary.add(primary);
    }


    public boolean hasConflicts() {
        return !primary.isEmpty() || !secondary.isEmpty();
    }
}
