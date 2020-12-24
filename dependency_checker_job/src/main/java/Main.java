import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

public class Main {
    public static void main(String[] args) {

        CtClass l = Launcher.parseClass("public class testClass {\n" +
                "    testClass(int jemoeder,double jevader){\n" +
                "        int x;\n" +
                "    }\n" +
                "    \n" +
                "    static void A-method(){\n" +
                "        \n" +
                "    }\n" +
                "    \n" +
                "    class classB{\n" +
                "        int kill = 0;\n" +
                "        \n" +
                "        classB(){\n" +
                "            System.out.println(\"Hello\");\n" +
                "        }\n" +
                "        \n" +
                "        void die(){\n" +
                "            kill=1;\n" +
                "        }\n" +
                "    }\n" +
                "}");
        System.out.println(l.getMethods());
        System.out.println(l.getConstructors());
    }
}
