import java.util.HashMap;
import java.util.Map;

public class Pkg {

    private String name;
    private Boolean internal;
    private Map<Main.TOOL_NAME, Boolean> recognizedBy;

    public Pkg(String name, Boolean internal) {
        this.name = name;
        this.internal = internal;
        this.recognizedBy = new HashMap<>();
        for(Main.TOOL_NAME possibleName : Main.TOOL_NAME.values()) {
            recognizedBy.put(possibleName, false);
        }
    }

    public Pkg(String name, Boolean internal, Main.TOOL_NAME foundBy) {
        this(name, internal);
        recognizedBy.put(foundBy, true);
    }

    public String getName() {
        return name;
    }

    public Boolean isInternal() {
        return internal;
    }

    public Map<Main.TOOL_NAME, Boolean> recognizedBy() {
        return recognizedBy;
    }

    public void addRecognizedBy(Main.TOOL_NAME foundBy) {
        recognizedBy.put(foundBy, true);
    }
}
