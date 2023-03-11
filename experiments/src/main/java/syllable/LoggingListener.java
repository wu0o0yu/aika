package syllable;

import network.aika.callbacks.EventListener;
import network.aika.callbacks.EventType;
import network.aika.elements.Element;
import network.aika.steps.FieldStep;
import network.aika.steps.Step;
import network.aika.steps.thought.AnnealStep;

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
        System.out.println("" + as.getElement().getAnnealing().getCurrentValue());

        as.getElement().getAnnealing().getReceivers().forEach(fl ->
                System.out.println("     " + fl)
        );
        System.out.println();
    }

    @Override
    public void onElementEvent(EventType et, Element e) {
    }
}
