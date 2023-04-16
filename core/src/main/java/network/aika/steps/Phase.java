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
package network.aika.steps;

/**
 *
 * @author Lukas Molzberger
 */
public enum Phase {
    INSTANTIATION_NODES("IA"),
    INSTANTIATION_EDGES("IB"),
    INPUT_LINKING("IL"),
    OUTPUT_LINKING("OL"),
    INFERENCE("I"),
    CLOSE("C"),
    POST_CLOSE("PC"),
    ANNEAL("A"),
    TRAINING("T"),
    COUNTING("C"),
    SAVE("S");

    String label;

    Phase(String l) {
        label = l;
    }

    public String getLabel() {
        return label;
    }
}
