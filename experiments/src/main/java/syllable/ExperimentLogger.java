package syllable;

import network.aika.elements.activations.PatternActivation;
import network.aika.text.Document;

import java.util.TreeMap;

public class ExperimentLogger {

    TreeMap<Long, PatternLogger> patternLogger = new TreeMap<>();

    public ExperimentLogger(long... patternNeuronIds) {
        for(long id: patternNeuronIds)
            patternLogger.put(id, new PatternLogger("pattern-" + id + ".csv"));
    }

    public void log(Document doc) {
        doc.getActivations()
                .stream()
                .filter(act -> act instanceof PatternActivation)
                .map(act -> (PatternActivation)act)
                .forEach(act -> {
                    PatternLogger pl = patternLogger.get(act.getNeuron().getId());
                    if(pl != null)
                        pl.log(act);
                });
    }

    public void close() {
        patternLogger.values()
                .forEach(pl -> pl.close());
    }
}
