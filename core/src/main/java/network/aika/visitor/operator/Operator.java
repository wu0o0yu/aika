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
package network.aika.visitor.operator;

import network.aika.direction.Direction;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.Scope;
import network.aika.visitor.LinkingVisitor;
import network.aika.visitor.Visitor;

/**
 * @author Lukas Molzberger
 */
public interface Operator {

    Direction getRelationDir(Scope fromScope);

    void check(LinkingVisitor v, Link lastLink, Activation act);

}
