package items;

import functionality.Comparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * represents any item that will be looked for
 * i.e. classes, packages, dependencies
 */
public abstract class Itm {

    protected String name;
    protected Map<Comparator.TOOL_NAME, Integer> idMap = new HashMap<>();
    protected Set<Comparator.TOOL_NAME> foundBy = new HashSet<>();


    public Itm(String name, Comparator.TOOL_NAME found) {
        this.name = name;
        foundBy.add(Comparator.TOOL_NAME.IDEAL);
        foundBy.add(found);
    }

    public static Cls createClass(String name, Boolean internal, Comparator.TOOL_NAME found) {
        return new Cls(name, internal, found);
    }

    public static Pkg createPackage(String name, Boolean internal, Comparator.TOOL_NAME found) {
        return new Pkg(name, internal, found);
    }

    public static Dep createDependency(SrcItm from, SrcItm to, int amount, Comparator.TOOL_NAME found) {
        return new Dep(from, to, amount, found);
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
     * since different tools use different ids for each class,
     * we decided to add to make it even more confusing
     * by giving each class another id that we assign to it :)
     * (actually it's because this way we have the same id convention for all classes)
     * @return the id assigned by this program
     */
    public int getId() {
        return idMap.get(Comparator.TOOL_NAME.IDEAL);
    }
}
