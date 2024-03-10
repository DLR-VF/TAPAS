package de.dlr.ivf.tapas.choice;

import java.util.Collection;

public sealed interface Tree {
    record EmptyNode() implements Tree { }

    record MultiNode(double value, Collection<Tree> children) implements Tree { }
}
