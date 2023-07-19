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
package network.aika.elements;

import network.aika.elements.neurons.NeuronProvider;

import java.util.Comparator;

/**
 *
 * @author Lukas Molzberger
 */
public class OutputKey {

    public static final Comparator<OutputKey> COMPARATOR = Comparator
            .<OutputKey, NeuronProvider>comparing(ok -> ok.n)
            .thenComparingInt(ok -> ok.actId);

    private final NeuronProvider n;
    private final Integer actId;

    public OutputKey(NeuronProvider n, Integer actId) {
        this.n = n;
        this.actId = actId;
    }

    public String toString() {
        return "[" + n.getId() + "]:" + actId;
    }
}
