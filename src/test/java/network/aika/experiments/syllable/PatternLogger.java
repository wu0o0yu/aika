package network.aika.experiments.syllable;

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.Link;
import network.aika.elements.links.PatternLink;
import network.aika.text.Document;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PatternLogger {

    CSVPrinter printer;

    public PatternLogger(String filename)  {
        try {
            File f = new File("src/test/resources/experiments/", filename);
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
            printer = new CSVPrinter(fw, CSVFormat.DEFAULT
                    .withHeader(headerLabels.toArray(new String[0])));
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
                            pAct.getNeuron().getId(),
                            pAct.getNet().getCurrentValue(),
                            pAct.getGradient().getCurrentValue(),
                            pAct.getNeuron().getBias().getCurrentValue())
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
                                iAct.getNeuron().getId(),
                                iAct.getNet().getCurrentValue(),
                                iAct.getGradient().getCurrentValue(),
                                iAct.getNeuron().getBias().getCurrentValue(),
                                il.getSynapse().getWeight().getCurrentValue()
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
