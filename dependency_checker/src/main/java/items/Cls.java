package items;

import java.util.Arrays;
import java.util.List;

/**
 * class to represent a class found by a tool
 */
public class Cls extends SrcItm {

    public Cls(String name, Boolean internal, analysis.Comparator.TOOL_NAME found) {
        super(name, internal, found);
    }

    public String getCleanName() {
        List<String> split = Arrays.asList(name.split("[._]"));
        return split.get(split.size() - 1);
    }
}
