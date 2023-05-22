package network.aika.debugger.activations;

import javax.swing.*;

import static network.aika.debugger.activations.ActivationPanelMode.CURRENT;
import static network.aika.debugger.activations.ActivationPanelMode.SELECTED;

public class ActivationPanelContextMenu extends JPopupMenu {

    ActivationPanel actPanel;

    JMenuItem currentVsSelectedItem;

    public ActivationPanelContextMenu(ActivationPanel actPanel) {
        this.actPanel = actPanel;

        currentVsSelectedItem = new JMenuItem(actPanel.getMode().getTxt());
        currentVsSelectedItem.addActionListener(l ->
            actPanel.setMode(
                    actPanel.getMode() == CURRENT ?
                            SELECTED :
                            CURRENT
                    )
        );
        add(currentVsSelectedItem);
    }
}