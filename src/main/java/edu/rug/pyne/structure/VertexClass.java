package edu.rug.pyne.structure;

import com.syncleus.ferma.AbstractVertexFrame;
import com.syncleus.ferma.annotations.Adjacency;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.annotations.Incidence;
import com.syncleus.ferma.annotations.Property;
import java.util.Iterator;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

/**
 *
 * 
 * @author Patrick Beuks (s2288842) <code@beuks.net>
 */
@GraphElement
public abstract class VertexClass extends AbstractVertexFrame {
    
    @Property("name")
    public abstract String getName();
    
    @Property("name")
    public abstract void setName(String name);
    
    @Incidence(label = "belongsTo", direction = Direction.OUT)
    public abstract EdgeBelongsTo getBelongsTo();
    
    @Adjacency(label = "belongsTo", direction = Direction.OUT)
    public abstract VertexPackage getBelongsToPackage();
    
    public EdgeBelongsTo setBelongsTo(VertexPackage vertexPackage) {
        Iterator<Edge> edges = getElement().edges(Direction.OUT, "belongsTo");
        if (edges.hasNext()) {
            edges.next().remove();
        }
        return addFramedEdge("belongsTo", vertexPackage, EdgeBelongsTo.class);
    }
    
}
