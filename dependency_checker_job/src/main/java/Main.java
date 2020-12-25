import spoon.Launcher;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.ArrayList;
import java.util.Set;

public class Main {
    public static void main(String[] args) {

        CtClass l = Launcher.parseClass("public class testClass {\n" +
                "    testClass(int jemoeder,double jevader){\n" +
                "        int x;\n" +
                "    }\n" +
                "\n" +
                "    static void A(int argument){\n" +
                "        float h = 2;\n" +
                "    }\n" +
                "\n" +
                "    class classB{\n" +
                "        int kill = 0;\n" +
                "\n" +
                "        classB(){\n" +
                "            System.out.println(\"Hello\");\n" +
                "        }\n" +
                "\n" +
                "        void die(){\n" +
                "            kill=1;\n" +
                "        }\n" +
                "    }\n" +
                "}");

        System.out.println(l.getMethods());
        Set<CtExecutable<?>>  x = (Set<CtExecutable<?>>) l.getMethods();
        Set<CtExecutable<?>>  y = (Set<CtExecutable<?>>) l.getConstructors();
        ArrayList<CtExecutable<?>> arr = new ArrayList<>();
        arr.addAll(x);
        arr.addAll(y);
        System.out.println(arr);
    }
}
