package network.aika.meta;

import network.aika.parser.Context;

public class TestContext implements Context {


    String headlineTargetString;

    String headlineTargetLabel;

    public TestContext(String headlineTargetString, String headlineTargetLabel) {
        this.headlineTargetString = headlineTargetString;
        this.headlineTargetLabel = headlineTargetLabel;
    }

    public String getHeadlineTargetString() {
        return headlineTargetString;
    }

    public String getHeadlineTargetLabel() {
        return headlineTargetLabel;
    }

    public boolean isHeadlineTarget() {
        return headlineTargetLabel != null;
    }
}
