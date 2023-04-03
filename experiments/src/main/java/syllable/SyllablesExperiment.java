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
package syllable;

import network.aika.Config;
import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.CategoryActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.activations.TokenActivation;
import network.aika.elements.links.CategoryLink;
import network.aika.elements.links.Link;
import network.aika.sign.Sign;
import network.aika.steps.Phase;
import network.aika.text.Document;
import org.apache.commons.io.IOUtils;
import syllable.logger.ExperimentLogger;
import syllable.logger.LoggingListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Lukas Molzberger
 */
public class SyllablesExperiment {

    public static void processTokens(SyllableTemplateModel m, Document doc, Iterable<String> tokens, int separatorLength) {
        int i = 0;
        int pos = 0;

        List<TokenActivation> tokenActs = new ArrayList<>();
        for(String t: tokens) {
            int j = i + t.length();

            tokenActs.add(
                    doc.addToken(
                            m.lookupInputToken(t),
                            pos,
                            i,
                            j
                    )
            );

            pos++;

            i = j + separatorLength;
        }

        PatternActivation maxSurprisalAct = tokenActs.stream().filter(PatternActivation.class::isInstance)
                .map(PatternActivation.class::cast)
                .max(Comparator.comparingDouble(act ->
                        act.getNeuron().getSurprisal(
                                Sign.POS,
                                act.getAbsoluteRange(),
                                true
                        )
                )).orElse(null);

        doc.setActivationCheckCallback(act -> {
            if (act == null)
                return true;

            Link l = act.getInputLink(m.letterCategory);
            if(l == null)
                return false;

            CategoryActivation cAct = (CategoryActivation) l.getInput();
            if (cAct == null)
                return false;

            CategoryLink catLink = (CategoryLink) cAct.getInputLinks().findAny().orElse(null);
            if(catLink == null)
                return false;

            return maxSurprisalAct == catLink.getInput();
        });

        for(TokenActivation tAct: tokenActs) {
            tAct.setNet(10.0);
        }
    }


    public static Config getConfig() {
        return new Config() {
            public String getLabel(Activation act) {
                Document doc = (Document) act.getThought();
                return doc.getTextSegment(act.getRange());
            }
        };
    }

    public static void main(String[] args) throws IOException {
        new SyllablesExperiment()
                .testTraining();
    }

    public void testTraining() throws IOException {
        List<String> inputs = new ArrayList<>();
        inputs.add("der");
        inputs.add("der");

        inputs.addAll(getInputs());
        train(inputs);
    }

    private void train(List<String> inputs) {
        Model model = new Model();

        SyllableTemplateModel syllableModel = new SyllableTemplateModel(model);

        model.setN(0);

        // Counting letters loop
        inputs.forEach(w -> {
            Document doc = initDocument(model, w);
            doc.getConfig()
                    .setTrainingEnabled(false)
                    .setMetaInstantiationEnabled(false)
                    .setCountingEnabled(true);

//            AIKADebugger.createAndShowGUI(doc);

            processTokens(
                    syllableModel,
                    doc,
                    convertToCharTokens(w),
                    0
            );

            doc.process(Phase.ANNEAL);

            doc.postProcessing();
            doc.updateModel();
            doc.disconnect();
        });

        syllableModel.initMeta();

        int[] counter = new int[1];

        ExperimentLogger el = new ExperimentLogger(43, 31, 24, 70);

        inputs.forEach(w -> {
            Document doc = initDocument(model, w);
            doc.getConfig()
                    .setTrainingEnabled(true)
                    .setMetaInstantiationEnabled(true)
                    .setCountingEnabled(true);

            AIKADebugger debugger = null;
            System.out.println(counter[0] + " " + w);
            if(counter[0] >= 11) {// 3, 6
                debugger = AIKADebugger.createAndShowGUI(doc);
            }

            processTokens(
                    syllableModel,
                    doc,
                    convertToCharTokens(w),
                    0
            );

            waitForClick(debugger);

            doc.process(Phase.INFERENCE);

            LoggingListener logger = null;

            if(debugger != null) {
                logger = new LoggingListener();
                doc.addEventListener(logger);
            }

            doc.close();

            el.annealingLogInit(doc);

            doc.anneal();

            waitForClick(debugger);

            doc.instantiateTemplates();

            waitForClick(debugger);

            doc.close();

            doc.train();

            el.log(doc);

            waitForClick(debugger);

            if(logger != null)
                doc.removeEventListener(logger);

            doc.postProcessing();
            doc.updateModel();
            doc.disconnect();

            counter[0]++;
        });

        el.close();
    }

    private static void waitForClick(AIKADebugger debugger) {
        if(debugger != null)
            debugger.getStepManager().waitForClick();
    }

    public List<String> convertToCharTokens(String w) {
        ArrayList<String> result = new ArrayList<>();
        for(char c: w.toCharArray()) {
            result.add("" + c);
        }
        return result;
    }

    private Document initDocument(Model m, String txt) {
        Document doc = new Document(m, txt);
        doc.setConfig(
                getConfig()
                        .setAlpha(null)
                        .setLearnRate(-0.01)
                        .setTrainingEnabled(false)
        );

        return doc;
    }

    private List<String> getInputs() throws IOException {
        String[] files = new String[]{
                "Aschenputtel",
                "BruederchenUndSchwesterchen",
                "DasTapfereSchneiderlein",
                "DerFroschkoenig",
                "DerGestiefelteKater",
                "DerGoldeneSchluessel",
                "DerSuesseBrei",
                "DerTeufelMitDenDreiGoldenenHaaren",
                "DerWolfUndDieSiebenJungenGeisslein",
                "DieBremerStadtmusikanten",
                "DieDreiFedern",
                "DieSterntaler",
                "DieWeisseSchlange",
                "DieZwoelfBrueder",
                "Dornroeschen",
                "FrauHolle",
                "HaenselUndGretel",
                "HansImGlueck",
                "JorindeUndJoringel",
                "KatzeUndMausInGesellschaft",
                "MaerchenVonEinemDerAuszogDasFuerchtenZuLernen",
                "Marienkind",
                "Rapunzel",
                "Rotkaeppchen",
                "Rumpelstilzchen",
                "SchneeweisschenUndRosenrot",
                "Schneewitchen",
                "TischleinDeckDich",
                "VonDemFischerUndSeinerFrau"
        };

        ArrayList<String> inputs = new ArrayList<>();

        for (String fn : files) {
            InputStream is = getClass().getResourceAsStream("../corpora/public-domain-txt/" + fn + ".txt");
            assert is != null;

            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            String txt = writer.toString();

            txt = txt.replace('.', ' ');
            txt = txt.replace(',', ' ');
            txt = txt.replace('?', ' ');
            txt = txt.replace('!', ' ');
            txt = txt.replace('"', ' ');
            txt = txt.replace('-', ' ');
            txt = txt.replace(':', ' ');
            txt = txt.replace(';', ' ');
            txt = txt.replace('\n', ' ');
            txt = txt.replace("  ", " ");
            txt = txt.replace("  ", " ");

            for(String word: txt.split(" ")) {
                String w = word.toLowerCase().trim();
                if(w.isBlank())
                    continue;

                inputs.add(w);
            }
        }
        return inputs;
    }
}
