package items;

import functionality.Comparator;

import java.util.*;

public class Cls extends SrcItm {

    private Boolean internal;

    public Cls(String name, Boolean internal, functionality.Comparator.TOOL_NAME found) {
        super(name, internal, found);
    }

    public String getCleanName() {
        List<String> split = Arrays.asList(name.split("[._]"));
        return split.get(split.size() - 1);
    }

    public Boolean isInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        if(internal != null) {
            this.internal = internal;
        }
    }
}
