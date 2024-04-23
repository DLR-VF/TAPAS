package de.dlr.ivf.tapas.choice.decisiontree;

public sealed interface DecisionTree<T> permits DecisionNode, DecisionLeaf{}
