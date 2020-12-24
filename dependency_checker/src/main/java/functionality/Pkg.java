package functionality;

import java.util.HashMap;
import java.util.Map;

public class Pkg {

    private String name;
    private Boolean internal;
    private Map<Comparator.TOOL_NAME, Integer> idMap = new HashMap<>();

    public Pkg(String name, Boolean internal) {
        this.name = name;
        this.internal = internal;
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

    public Integer getToolId(Comparator.TOOL_NAME tool) {
        return idMap.get(tool);
    }
}
