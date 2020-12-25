package functionality;

public class IDProvider {

    private int id;

    public IDProvider() {
        this(0);
    }

    public IDProvider(int i) {
        id = i;
    }

    public int getNextId() {
        return id++;
    }

    public int peekNextId() {
        return id;
    }
}
