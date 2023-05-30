package network.aika.debugger.activations;


import javax.swing.*;

import static network.aika.debugger.activations.ActivationPanelMode.CURRENT;
import static network.aika.debugger.activations.ActivationPanelMode.SELECTED;

public class ActivationPanelContextMenu extends JPopupMenu {

    ActivationConsoleManager actPanel;

    JMenuItem currentVsSelectedItem;

    public ActivationPanelContextMenu(ActivationConsoleManager actPanel) {
        this.actPanel = actPanel;

        currentVsSelectedItem = new JMenuItem(actPanel.getMode().getTxt());
        currentVsSelectedItem.addActionListener(l ->
            actPanel.setMode(actPanel.getMode().getInverted())
        );
        add(currentVsSelectedItem);
    }
}