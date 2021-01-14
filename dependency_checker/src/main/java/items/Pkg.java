package items;

import analysis.Comparator;

/**
 * class to represent a package found by a tool
 */
public class Pkg extends SrcItm {

    public Pkg(String name, Boolean isInternal, Comparator.TOOL_NAME found) {
        super(name, isInternal, found);
    }
}
