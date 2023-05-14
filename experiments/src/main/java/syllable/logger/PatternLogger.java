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
package syllable.logger;

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.Link;
import network.aika.elements.links.PatternLink;
import network.aika.text.Document;
import network.aika.utils.Utils;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static syllable.logger.ExperimentLogger.CSV_FORMAT;

/**
 * @author Lukas Molzberger
 */
public class PatternLogger {

    public static final double PRECISION = 10000.0;

    CSVPrinter printer;

    public PatternLogger() {

    }

    public PatternLogger(File path, PatternActivation act) {
        open(new File(path, "pattern-" + act.getNeuron().getId() + ".csv"));
    }


    public void open(File f)  {
        try {
            if(f.exists())
                f.delete();

            List<String> headerLabels = new ArrayList<>();
            headerLabels.addAll(
                    List.of("DocId", "Content", "pLabel", "pAct-Id", "PN-Id", "net", "gradient", "bias")
            );

            for(int i = 0; i < 5; i++) {
                headerLabels.addAll(
                    List.of(i + "-bLabel", i + "-bAct Id", i + "-BN-Id", i + "-net", i + "-gradient", i + "-bias", i + "-weight-pl")
                );
            }

            FileWriter fw = new FileWriter(f);
            printer = new CSVPrinter(
                    fw,
                    CSV_FORMAT.withHeader(headerLabels.toArray(new String[0]))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(PatternActivation pAct) {
        try {
            List entry = new ArrayList();

            entry.addAll(
                    List.of(
                            pAct.getThought().getId(),
                            ((Document) pAct.getThought()).getContent(),
                            pAct.getLabel(),
                            pAct.getId(),
                            pAct.getNeuron().getId() + (pAct.getNeuron().isAbstract() ? "-abstr" : ""),
                            Utils.round(pAct.getNet().getCurrentValue(), PRECISION),
                            Utils.round(pAct.getGradient().getCurrentValue(), PRECISION),
                            Utils.round(pAct.getNeuron().getBias().getCurrentValue(), PRECISION))
            );

            List<Link> inputLinks = pAct.getInputLinks()
                    .filter(l -> l instanceof PatternLink)
                    .collect(Collectors.toList());

            for(int i = 0; i < Math.min(5, inputLinks.size()); i++) {
                Link il = inputLinks.get(i);
                BindingActivation iAct = (BindingActivation) il.getInput();

                if(iAct == null)
                    continue;

                entry.addAll(
                        List.of(
                                iAct.getLabel(),
                                iAct.getId(),
                                iAct.getNeuron().getId() + (iAct.getNeuron().isAbstract() ? "-abstr" : ""),
                                Utils.round(iAct.getNet().getCurrentValue(), PRECISION),
                                Utils.round(iAct.getGradient().getCurrentValue(), PRECISION),
                                Utils.round(iAct.getNeuron().getBias().getCurrentValue(), PRECISION),
                                Utils.round(il.getSynapse().getWeight().getCurrentValue(), PRECISION)
                        )
                );
            }

            printer.printRecord(entry.toArray());
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
