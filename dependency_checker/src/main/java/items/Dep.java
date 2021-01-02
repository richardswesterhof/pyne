package items;

import analysis.Comparator;

/**
 * class to represent a dependency found by a tool
 */
public class Dep extends Itm {

    private final SrcItm from;
    private final SrcItm to;
    private final int amount;

    public Dep(SrcItm from, SrcItm to, int amount, Comparator.TOOL_NAME found) {
        super(null, found);
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    public SrcItm getFrom() {
        return from;
    }

    public SrcItm getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }
}
