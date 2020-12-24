package functionality;

import java.util.HashSet;
import java.util.Set;

public class ToolPerformance {

    private Comparator.TOOL_NAME name;
    private Set<String> internalHits;
    private Set<String> externalHits;
    private int internalMissCount = 0;
    private int externalMissCount = 0;

    public ToolPerformance(Comparator.TOOL_NAME name) {
        this.name = name;
        internalHits = new HashSet<>();
        externalHits = new HashSet<>();
    }

    public void addInternalRecognizedPackage(String pkg) {
        internalHits.add(pkg);
    }

    public void addExternalRecognizedPackage(String pkg) {
        externalHits.add(pkg);
    }

    public void addPkg(Pkg pkg) {
        if(pkg.isInternal()) addInternalRecognizedPackage(pkg.getName());
        else addExternalRecognizedPackage(pkg.getName());
    }

    public Comparator.TOOL_NAME getName() {
        return name;
    }

    public Set<String> getHits() {
        Set<String> totalHits = new HashSet<>();
        totalHits.addAll(internalHits);
        totalHits.addAll(externalHits);
        return totalHits;
    }

    public Set<String> getInternalHits() {
        return internalHits;
    }

    public Set<String> getExternalHits() {
        return externalHits;
    }

    public int getHitCount() {
        return internalHits.size() + externalHits.size();
    }

    public int getInternalHitCount() {
        return internalHits.size();
    }

    public int getExternalHitCount() {
        return externalHits.size();
    }
}
