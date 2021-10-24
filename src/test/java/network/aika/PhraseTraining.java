package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.platform.commons.util.StringUtils.isBlank;

public class PhraseTraining {


    private String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    @Test
    public void trainPhrases() throws IOException {
        TextModel m = new TextModel();

        //    m.setN(912);

        for (int round = 0; round < 2; round++) {
            process(m, round);
        }
    }


    private void process(TextModel m, int round) throws IOException {
/*        Step[] countingOnlyFilters = new Step[]{
                ActivationStep.TEMPLATE_CLOSE_LOOP_OUTPUT,
                ActivationStep.TEMPLATE_PROPAGATE_OUTPUT,
                ActivationStep.TEMPLATE_PROPAGATE_INPUT,
                ActivationStep.ENTROPY_GRADIENT,
                ActivationStep.PROPAGATE_GRADIENTS_NET,
                ActivationStep.PROPAGATE_GRADIENTS_SUM,
                ActivationStep.INDUCTION,
                LinkStep.TEMPLATE_INPUT,
                LinkStep.TEMPLATE_OUTPUT,
                LinkStep.INDUCTION,
                LinkStep.INFORMATION_GAIN_GRADIENT
        };*/
        Util.loadExamplePhrases("phrases.txt")
                .stream()
                .filter(p -> !isBlank(p))
                .forEach(p -> {
                            System.out.println(p);
                            Document doc = new Document(p);
                            doc.setConfig(
                                    Util.getTestConfig()
                                    .setAlpha(0.99)
                                    .setLearnRate(-0.1)
                                    .setEnableTraining(true)
                            );

                            int i = 0;
                            TokenActivation lastToken = null;
                            for (String t : doc.getContent().split(" ")) {
                                if (!isBlank(t)) {
                                    int j = i + t.length();
                                    TokenActivation currentToken = doc.addToken(m, t, i, j);
                                    TokenActivation.addRelation(lastToken, currentToken);
                                    lastToken = currentToken;
                                    i = j + 1;
                                }
                            }

                            if (round == 0) {
      //                          doc.addFilters(countingOnlyFilters);
                            } else {
                                AikaDebugger.createAndShowGUI(doc, m);
                            }

                            doc.process(m);
                        }
                );
    }

}
