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
package org.aika.network;


import org.aika.Activation;
import org.aika.corpus.Document;
import org.aika.corpus.InterprNode;
import org.aika.corpus.Range;
import org.aika.corpus.Range.Operator;
import org.aika.corpus.Range.Mapping;
import org.aika.lattice.InputNode;
import org.aika.lattice.Node;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;

import java.util.Collections;
import java.util.Set;

import static org.aika.corpus.Range.Operator.*;
import static org.aika.corpus.Range.Mapping.END;
import static org.aika.corpus.Range.Mapping.START;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {


    public static Activation addActivation(InputNode in, Document doc, Activation inputAct) {
        in.addActivation(doc, inputAct);
        doc.propagate();
        if(in instanceof InputNode) {
            return Activation.get(doc, in, inputAct.key.rid, inputAct.key.r, LESS_THAN, GREATER_THAN, inputAct.key.o, InterprNode.Relation.EQUALS);
        }
        return null;
    }


    public static InputNode addOutputNode(Document doc, Neuron n, Integer relativeRid, Integer absoluteRid) {
        return addOutputNode(doc, n, relativeRid, absoluteRid, EQUALS, START, true, EQUALS, END, true);
    }


    public static InputNode addOutputNode(Document doc, Neuron n, Integer relativeRid, Integer absoluteRid, Operator startRangeMatch, Mapping startMapping, boolean startRangeOutput, Operator endRangeMatch, Mapping endMapping, boolean endRangeOutput) {
        return InputNode.add(doc.m, new Synapse.Key(false, false, relativeRid, absoluteRid, startRangeMatch, startMapping, startRangeOutput, endRangeMatch, endMapping, endRangeOutput), n);
    }

    public static <T extends Node, A extends Activation<T>> A get(Document doc, T n, Range r, InterprNode o) {
        return Activation.get(doc, n, null, r, LESS_THAN, GREATER_THAN, o, InterprNode.Relation.EQUALS);
    }
}
