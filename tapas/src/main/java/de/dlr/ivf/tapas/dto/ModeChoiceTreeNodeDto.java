package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ModeChoiceTreeNodeDto represents a data transfer object for the mode choice tree node.
 */
@NoArgsConstructor
@Getter
public class ModeChoiceTreeNodeDto {

    /**
     * The name of the mode choice tree.
     */
    @Column("name")
    private String name;

    /**
     * Represents the id of a node in the mode choice tree.
     */
    @Column("node_id")
    private int nodeId;

    /**
     * The ID of the parent node in the mode choice tree.
     */
    @Column("parent_node_id")
    private int parentNodeId;

    @Column("attribute_values")
    private int[] attributeValues;

    @Column("split_variable")
    private String splitVariable;

    @Column("distribution")
    private double[] distribution;

    @Column("distribution_json")
    private String distributionJson;
}
