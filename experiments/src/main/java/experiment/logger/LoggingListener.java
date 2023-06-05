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
package experiment.logger;

import network.aika.callbacks.EventListener;
import network.aika.callbacks.EventType;
import network.aika.elements.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Step;
import network.aika.steps.thought.AnnealStep;

import static java.lang.Integer.MAX_VALUE;

/**
 * @author Lukas Molzberger
 */
public class LoggingListener implements EventListener {

    @Override
    public void onQueueEvent(EventType et, Step s) {
        if(s instanceof FieldStep<?>) {
            log((FieldStep) s);
        } else if(s instanceof AnnealStep) {
            log((AnnealStep) s);
        }
    }

    private static void log(FieldStep fs) {
        System.out.println("" + fs);

        fs.getField().getReceivers().forEach(fl ->
                        System.out.println("     " + fl)
                );
        System.out.println();
    }

    private static void log(AnnealStep as) {
        System.out.println("" + as.getElement().getAnnealing().getValue(MAX_VALUE));

        as.getElement().getAnnealing().getReceivers().forEach(fl ->
                System.out.println("     " + fl)
        );
        System.out.println();
    }

    @Override
    public void onElementEvent(EventType et, Element e) {
    }
}
