package de.dlr.ivf.tapas.choice.decisiontree;

import java.util.Map;

public record DecisionNode<T>(Map<SplitVariable<?>, DecisionTree<T>> children, DecisionLeaf<T> asLeaf) implements DecisionTree<T> {

    public DecisionTree<T> childBySplitVariable(SplitVariable<?> splitVariable){
        return children.get(splitVariable);
    }
}
