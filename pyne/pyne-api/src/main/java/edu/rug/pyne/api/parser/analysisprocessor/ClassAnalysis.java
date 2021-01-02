package edu.rug.pyne.api.parser.analysisprocessor;

import com.syncleus.ferma.FramedGraph;
import edu.rug.pyne.api.parser.Parser;
import edu.rug.pyne.api.structure.VertexClass;
import edu.rug.pyne.api.structure.VertexPackage;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;


/**
 * This a analysis processor. It takes the source code class and analyzes the
 * dependencies it has.
 *
 * @author Patrick Beuks (s2288842) <code@beuks.net>
 */
public class ClassAnalysis extends AbstractProcessor<CtClass<?>> {

    // The graph with the vertex classes
    private final FramedGraph framedGraph;

    // The parser containing additional information
    private final Parser parser;

    // Create a logger
    private static final Logger LOGGER
            = LogManager.getLogger(ClassAnalysis.class);

    private long counter = 0;

    /**
     * A consumer for annotations, used to get the type and add the declaration
     * of it
     */
    private class AnnotationConsumer
            implements Consumer<CtAnnotation<? extends Annotation>> {

        private final List<CtTypeReference> dependences;

        /**
         * A consumer for annotations, used to get the type and add the
         * declaration of it
         *
         * @param dependences The list to add the found type to
         */
        public AnnotationConsumer(List<CtTypeReference> dependences) {
            this.dependences = dependences;
        }

        @Override
        public void accept(CtAnnotation<? extends Annotation> annotation) {
            dependences.add(annotation.getAnnotationType());
        }

    }

    /**
     * This class processor implements a spoon processor to analyze source code
     * classes
     *
     * @param parser The parser to use
     * @param framedGraph The graph with the vertex classes
     */
    public ClassAnalysis(Parser parser, FramedGraph framedGraph) {
        this.framedGraph = framedGraph;
        this.parser = parser;
    }

    /**
     * Processes a single source code class
     *
     * @param clazz The class to process
     */
    @Override
    public void process(CtClass<?> clazz) {
        this.processClass(clazz);
    }

    /**
     * Processes a single source code class or interface
     *
     * @param clazz The class or interface to process
     */
    public void processClass(CtType<?> clazz) {

        VertexClass vertex = VertexClass
                .getVertexClassByName(framedGraph, clazz.getQualifiedName());

        if (vertex == null) {
            return;
        }

        // Check if added files is set, and if so only analyze those classes
        if (parser.getAddedFiles() != null) {
            File file = clazz.getPosition().getFile();
            if (!parser.getAddedFiles().contains(file)) {
                return;
            }
        }

        processClassDependencies(clazz, vertex);
        processClassReferences(clazz, vertex);

    }

    /**
     * Checks if the given class has a superclass or implements interfaces and
     * if so adds the corresponding edges to the vertex.
     *
     * @param clazz The class being processed
     * @param vertexClass The corresponding vertex
     */
    private void processClassDependencies(
            CtType clazz, VertexClass vertexClass
    ) {

        if (clazz.getSuperclass() != null) {
            VertexClass superClass
                    = getOrCreateVertexClass(clazz.getSuperclass());
            vertexClass.addChildOfClass(superClass);
        }

        for (CtTypeReference<?> superInterface : clazz.getSuperInterfaces()) {
            if (superInterface == null)
                continue;
            VertexClass superInterfaceClass
                    = getOrCreateVertexClass(superInterface);
            vertexClass.addImplematationOfClass(superInterfaceClass);
        }

    }

    /**
     * Goes over all class references for the given class and adds the
     * corresponding edges.
     *
     * @param clazz The class being processed
     * @param vertexClass The corresponding vertex
     */
    private void processClassReferences(CtType clazz, VertexClass vertexClass) {

        for (CtTypeReference referencedClass : getClassReferences(clazz)) {

            if (referencedClass == null) {
                continue;
            }

            VertexClass referencedClassVertex = getOrCreateVertexClass(referencedClass);
            vertexClass.addDependOnClass(referencedClassVertex);
        }
    }

    /**
     * Finds all dependencies the given class has.
     *
     * @param clazz The class being processed
     */
    private List<CtTypeReference> getClassReferences(CtType clazz) {
        List<CtTypeReference> references = new ArrayList<>();

        // Sets up the consumers that will add the references.
        AnnotationConsumer annotationConsumer
                = new AnnotationConsumer(references);

        //Creates a list of methods and constructors
        ArrayList<CtExecutable<?>> executables = new ArrayList<>();
        executables.addAll((Set<CtExecutable<?>>) clazz.getMethods());
        if (clazz instanceof CtClass) {
            executables.addAll((Set<CtExecutable<?>>) ((CtClass) clazz).getConstructors());
        }

        //retrieve the dependencies out of all the methods and constructors
        for (CtExecutable<?> ctExecutable : executables) {

            //add return value of method
            references.add(ctExecutable.getType());

            // Get binaryOperators used in the method, so we can check if they 
            // are instanceof elements and add the dependency if so.
            List<CtBinaryOperator<?>> BinaryElements = ctExecutable
                    .getElements(new TypeFilter<>(CtBinaryOperator.class));

            for (CtBinaryOperator<?> element : BinaryElements) {
                if (element.getKind().equals(BinaryOperatorKind.INSTANCEOF)) {
                    references.add(element.getRightHandOperand().getType());
                }
            }

            // Add all paramater references and annotations
            ctExecutable.getAnnotations().forEach(annotationConsumer);
            for (CtParameter<?> parameter : ctExecutable.getParameters()) {
                parameter.getAnnotations().forEach(annotationConsumer);
                references.add(parameter.getType());
            }

            // Get the body if the method has one
            CtBlock<?> body = ctExecutable.getBody();
            if (body == null) {
                continue;
            }

            // Get all constructors in the method
            List<CtConstructorCall<?>> constructorElements = body
                    .getElements(new TypeFilter<>(CtConstructorCall.class));

            //add all references for the constructor calls in the method
            for(CtConstructorCall<?> c : constructorElements){
                references.add(c.getType());
            }

            // Get all invocations in the method
            List<CtInvocation<?>> invocationElements = body
                    .getElements(new TypeFilter<>(CtInvocation.class));

            // Retrieve the dependencies of all invocations
            for(CtInvocation<?> c : invocationElements){
                if(c.getExecutable().getDeclaringType() == null){
                    LOGGER.warn("Spoon cannot find the declaration of " + c);
                    counter++;
                    LOGGER.warn("instance count " + counter);
                }else {
                    references.add(c.getExecutable().getDeclaringType());
                }
            }
        }

        // Get all annotations the class uses and add them
        clazz.getAnnotations().forEach(annotationConsumer);

        // add all the fields types and annotations
        for (CtField<?> field : (List<CtField<?>>) clazz.getFields()) {
            field.getAnnotations().forEach(annotationConsumer);
            references.add(field.getType());
        }

        return references;
    }


    /**
     * Gets the vertex class by the reference. If it does not exists a new
     * vertex class, with SystemType set to RetrievedClass, is created and
     * returned.
     *
     * @param clazz The class to find in the graph
     * @return The found vertex, or a newly created one if it does not exists
     */
    private VertexClass getOrCreateVertexClass(CtTypeReference clazz) {

        // Find the vertex class by name
        VertexClass vertexClass = VertexClass
                .getVertexClassByName(framedGraph, clazz.getQualifiedName());

        // If found we are done and it can be returned
        if (vertexClass != null) {
            return vertexClass;
        }

        // A new vertex class is created.
        vertexClass = VertexClass
                .createRetrievedClass(framedGraph, clazz.getQualifiedName());

        // An inner class does not have a package. So we need to go outside
        // until we find the parent class that does have a package.
        CtTypeReference cur = clazz;
        while (!cur.isPrimitive() && cur.getPackage() == null) {
            var tmp = cur.getDeclaringType();
            if (tmp == null || tmp.getPackage() == null)
                break;
            cur = tmp;
        }
        
        VertexPackage packageVertex = null;
        // If the type is a primative (like int or byte) it does not have a
        // package, So we set it to java.lang
        if (cur.isPrimitive()) {
            packageVertex = VertexPackage
                    .getVertexPackageByName(framedGraph, "java.lang");
            if (packageVertex == null) {
                packageVertex = VertexPackage.createRetrievedPackage(
                        framedGraph, "java.lang"
                );
            }
        } else {
            // Get or create the package by name.
            CtPackageReference ctPackage = cur.getPackage();
            if (ctPackage != null) {
                packageVertex = VertexPackage.getVertexPackageByName(
                        framedGraph, ctPackage.getQualifiedName()
                );
                if (packageVertex == null) {
                    packageVertex = VertexPackage.createVertexPackage(
                            framedGraph, ctPackage
                    );
                }
            }
        }

        // Set the belongsTo edge.
        if (packageVertex != null) {
            vertexClass.setBelongsTo(packageVertex);
        }

        return vertexClass;
    }

}
