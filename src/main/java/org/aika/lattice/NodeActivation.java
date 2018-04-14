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
package org.aika.lattice;


import org.aika.Utils;
import org.aika.Document;
import org.aika.neuron.activation.Range;
import org.aika.lattice.AndNode.Refinement;

import java.util.*;


public class NodeActivation<T extends Node> {

    public final int id;

    public final T node;

    public final Document doc;

    public long visited = -1;
    public Long repropagateV;

    public TreeMap<Integer, AndNode.Link> outputsToAndNode = new TreeMap<>();
    public TreeMap<Integer, OrNode.Link> outputsToOrNode = new TreeMap<>();
    public InputNode.Link outputToInputNode;


    public NodeActivation(int id, Document doc, T node) {
        this.id = id;
        this.doc = doc;
        this.node = node;
    }
}
