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


import org.aika.neuron.*;
import org.aika.Document;
import org.aika.neuron.activation.Range;
import org.aika.neuron.activation.Range.Relation;
import org.aika.lattice.InputNode;
import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Selector;

/**
 *
 * @author Lukas Molzberger
 */
public class TestHelper {


    public static InputNode addOutputNode(Document doc, Neuron n, Integer relativeRid, Integer absoluteRid, boolean ro) {
        return addOutputNode(doc, n, relativeRid, absoluteRid, Relation.EQUALS, ro ? Range.Output.DIRECT : Range.Output.NONE);
    }


    public static InputNode addOutputNode(Document doc, Neuron n, Integer relativeRid, Integer absoluteRid, Relation rangeMatch, Range.Output rangeOutput) {
        return InputNode.add(doc.model,
                new Synapse.Key(
                        false,
                        relativeRid,
                        absoluteRid,
                        rangeMatch,
                        rangeOutput
                ),
                n.get()
        );
    }

    public static Activation get(Document doc, INeuron n, Range r) {
        return Selector.get(doc, n, null, r, Relation.CONTAINS);
    }
}
