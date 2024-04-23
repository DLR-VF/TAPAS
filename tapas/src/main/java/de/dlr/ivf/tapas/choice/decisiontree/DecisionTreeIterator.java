package de.dlr.ivf.tapas.choice.decisiontree;

import java.util.Set;

public class DecisionTreeIterator<T> {

    private final DecisionTree<T> rootNode;

    public DecisionTreeIterator(DecisionTree<T> rootNode){
        this.rootNode = rootNode;
    }

    public T findValue(Set<SplitVariable<?>> decisions){
        return walkTree(rootNode, decisions).value();
    }

    private DecisionLeaf<T> walkTree(DecisionTree<T> decisionTree, Set<SplitVariable<?>> decisions){
        return switch (decisionTree){
            case DecisionLeaf<T> leaf -> leaf;
            case DecisionNode<T> decisionNode -> findNodeBySplitVariable(decisionNode, decisions);
        };
    }

    private DecisionLeaf<T> findNodeBySplitVariable(DecisionNode<T> decisionNode, Set<SplitVariable<?>> decisions){

        DecisionTree<T> resultingNode = null;

        for(SplitVariable<?> decision: decisionNode.children().keySet()){
            if(decisions.contains(decision)){
                resultingNode = walkTree(decisionNode.childBySplitVariable(decision), decisions);
                break;
            }
        }

        return resultingNode == null ? decisionNode.asLeaf() : walkTree(resultingNode, decisions);
    }
}
