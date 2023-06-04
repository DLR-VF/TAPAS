package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class IncomeDto {

    @Column("id")
    private int id;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_income")
    private String nameIncome;

    @Column("code_income")
    private int codeIncome;

    @Column("max")
    private int max;
}
