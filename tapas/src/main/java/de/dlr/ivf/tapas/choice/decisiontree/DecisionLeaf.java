package de.dlr.ivf.tapas.choice.decisiontree;

public record DecisionLeaf<T>(T value) implements DecisionTree<T> {}
