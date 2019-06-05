package edu.rug.pyne.parser.analysisprocessor;

import com.syncleus.ferma.FramedGraph;
import edu.rug.pyne.parser.PostProcess;
import edu.rug.pyne.structure.VertexClass;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

/**
 *
 * @author Patrick Beuks (s2288842) <code@beuks.net>
 */
public class ClassPostProcess implements PostProcess {

    @Override
    public void postProcess(FramedGraph framedGraph) {
        
        List<? extends VertexClass> systemClasses = framedGraph.traverse((g) -> g.V().hasLabel(VertexClass.LABEL).has("ClassType", VertexClass.ClassType.SystemClass.name())).toList(VertexClass.class);

        framedGraph.traverse((g) -> {
            return g.V().hasLabel(VertexClass.LABEL).where(
                    __.both("dependsOn", "isChildOf", "isImplementationOf").count().is(0)
            );
        }).toList(VertexClass.class).forEach((orphanNode) -> orphanNode.remove());

        for (VertexClass systemClass : systemClasses) {
            List<? extends VertexClass> coupleVertexes = framedGraph.traverse((g) -> systemClass.getRawTraversal().out("dependsOn", "isChildOf", "isImplementationOf")).toList(VertexClass.class);
            
            for (VertexClass coupleVertex : coupleVertexes) {
                if (systemClass.getBelongsToPackage().equals(coupleVertex.getBelongsToPackage())) {
                    continue;
                }

                if (!systemClass.getAfferentOfPackages().contains(coupleVertex.getBelongsToPackage())) {
                    systemClass.addAfferentOf(coupleVertex.getBelongsToPackage());
                }

                if (!coupleVertex.getEfferentOfPackages().contains(systemClass.getBelongsToPackage())) {
                    coupleVertex.addEfferentOf(systemClass.getBelongsToPackage());
                }
            }
            
        }
    }
    
}
