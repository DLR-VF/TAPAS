package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ModeChoiceTreeNodeDto {

    @Column("name")
    private String name;

    @Column("node_id")
    private int nodeId;

    @Column("parent_node_id")
    private int parentNodeId;

    @Column("attribute_values")
    private int[] attributeValues;

    @Column("split_variable")
    private String splitVariable;

    @Column("distribution")
    private double[] distribution;


}
