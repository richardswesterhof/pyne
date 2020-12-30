package items;

import functionality.Comparator;

public class Dep extends Itm {

    private SrcItm from;
    private SrcItm to;
    private int amount;

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
