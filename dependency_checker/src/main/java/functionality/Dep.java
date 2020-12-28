package functionality;

import java.util.HashSet;
import java.util.Set;

public class Dep {

    private Cls from;
    private Cls to;
    private int amount;
    private Set<Comparator.TOOL_NAME> foundBy = new HashSet<>();


    public Dep(Cls from, Cls to, int amount, Comparator.TOOL_NAME found) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        foundBy.add(Comparator.TOOL_NAME.IDEAL);
        foundBy.add(found);
    }

    public void addFoundBy(Comparator.TOOL_NAME tool) {
        foundBy.add(tool);
    }

    public boolean wasFoundBy(Comparator.TOOL_NAME tool) {
        return foundBy.contains(tool);
    }

    public Cls getFrom() {
        return from;
    }

    public Cls getTo() {
        return to;
    }

    public int getAmount() {
        return amount;
    }
}
