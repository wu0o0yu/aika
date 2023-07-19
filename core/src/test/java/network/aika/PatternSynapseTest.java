package network.aika;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.enums.sign.Sign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternSynapseTest {


    @Test
    public void testUpdateFrequencyForIandO_bothActive_InitialState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.POS,Sign.POS,0.0);
        patternSynapse.updateFrequencyForIandO(true,true);
        assertEquals(1, (int) patternSynapse.getFrequency(Sign.POS,Sign.POS,0.0));
    }
    @Test
    public void testUpdateFrequencyForIandO_bothActive_UpdatedState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.POS,Sign.POS,12.0);
        patternSynapse.updateFrequencyForIandO(true,true);
        assertEquals(13, (int) patternSynapse.getFrequency(Sign.POS,Sign.POS,0.0));
    }

    @Test
    public void testUpdateFrequencyForIandO_onlyIActive_InitialState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.POS,Sign.NEG,0.0);
        patternSynapse.updateFrequencyForIandO(true,false);
        assertEquals(1, (int) patternSynapse.getFrequency(Sign.POS,Sign.NEG,0.0));
    }

    @Test
    public void testUpdateFrequencyForIandO_onlyIActive_UpdatedState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.POS,Sign.NEG,8.0);
        patternSynapse.updateFrequencyForIandO(true,false);
        assertEquals(9, (int) patternSynapse.getFrequency(Sign.POS,Sign.NEG,0.0));
    }

    @Test
    public void testUpdateFrequencyForIandO_onlyOActive_InitialState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.NEG,Sign.POS,0.0);
        patternSynapse.updateFrequencyForIandO(false,true);
        assertEquals(1, (int) patternSynapse.getFrequency(Sign.NEG,Sign.POS,0.0));
    }
    @Test
    public void testUpdateFrequencyForIandO_onlyOActive_UpdatedState() {
        PatternSynapse patternSynapse = new PatternSynapse();
        patternSynapse.setFrequency(Sign.NEG,Sign.POS,6.0);
        patternSynapse.updateFrequencyForIandO(false,true);
        assertEquals(7, (int) patternSynapse.getFrequency(Sign.NEG,Sign.POS,0.0));
    }
}