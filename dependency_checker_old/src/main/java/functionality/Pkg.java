package functionality;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Pkg {

    private String name;
    private Boolean internal;
    private Map<Comparator.TOOL_NAME, Integer> idMap = new HashMap<>();
    private Set<Comparator.TOOL_NAME> foundBy = new HashSet<>();

    public Pkg(String name, boolean internal, Comparator.TOOL_NAME found) {
        this.name = name;
        this.internal = internal;
        foundBy.add(Comparator.TOOL_NAME.IDEAL);
        foundBy.add(found);
    }

    public void addFoundBy(Comparator.TOOL_NAME tool) {
        foundBy.add(tool);
    }

    public boolean wasFoundBy(Comparator.TOOL_NAME tool) {
        return foundBy.contains(tool);
    }

    public String getName() {
        return name;
    }

    public Boolean isInternal() {
        return internal;
    }

    public String getBasicInfoString() {
        return name + " (" + (internal ? "in" : "ex") + "ternal)";
    }

    public void setToolId(Comparator.TOOL_NAME tool, Integer id) {
        idMap.put(tool, id);
    }

    public int getToolId(Comparator.TOOL_NAME tool) {
        return idMap.get(tool);
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * method for quickly getting the id assigned by this program.
     * since different tools use different ids for each package,
     * we decided to add to make it even more confusing
     * by giving each package another id that we assign to it :)
     * (actually it's because this way we have the same id convention for all packages)
     * @return the id assigned by this program
     */
    public int getId() {
        return idMap.get(Comparator.TOOL_NAME.IDEAL);
    }

}
