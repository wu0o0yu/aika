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
package network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Lukas Molzberger
 */
public class SyllableExperiment {

    CharBasedTraining charBasedTrainings = new CharBasedTraining();

    @BeforeEach
    public void init() {
        charBasedTrainings.init();
    }

    private void train(String word) {
        charBasedTrainings.train(word);
    }

    @Test
    public void testTraining() throws IOException {
        //"/Users/lukas.molzberger/aika-ws/maerchen"
        // "C:\\ws\\aika-syllables\\src\\main\\resources\\text\\maerchen"
        // "/Users/lukas/IdeaProjects/aika-bitbucket/test-data"
        for(String word: Util.loadExamplesAsWords(new File("C:\\ws\\aika-syllables\\src\\main\\resources\\text\\maerchen"))) {
            train( word + " ");
        }

        System.out.println();
    }
}
