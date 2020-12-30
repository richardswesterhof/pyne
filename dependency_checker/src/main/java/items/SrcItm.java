package items;

import functionality.Comparator;

/**
 * represents any item that will be looked for that is present in the source code
 * i.e. classes, packages
 */
public abstract class SrcItm extends Itm {

    protected Boolean isInternal;

    public SrcItm(String name, Boolean isInternal, Comparator.TOOL_NAME found) {
        super(name, found);
        this.isInternal = isInternal;
    }

    public Boolean isInternal() {
        return isInternal;
    }

    public void setInternal(Boolean internal) {
        if(internal != null) isInternal = internal;
    }
}
