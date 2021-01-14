package analysis;

public class Pkg {

    private final String name;
    private final Boolean internal;

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
}
