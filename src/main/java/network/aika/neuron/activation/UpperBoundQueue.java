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

import network.aika.neuron.activation.Activation.Link;

import java.util.ArrayDeque;


/**
 *
 * @author Lukas Molzberger
 */
public class UpperBoundQueue {
    private final ArrayDeque<Activation> queue = new ArrayDeque<>();


    public void add(Link l) {
        if(!l.isRecurrent()) {
            add(l.getOutput());
        }
    }


    public void add(Activation act) {
        if(!act.ubQueued && act.getInputState() == null) {
            act.ubQueued = true;
            queue.addLast(act);
        }
    }


    public boolean process() {
        boolean flag = false;
        while(!queue.isEmpty()) {
            flag = true;
            Activation act = queue.pollFirst();
            act.ubQueued = false;

            act.processBounds();
        }
        return flag;
    }
}