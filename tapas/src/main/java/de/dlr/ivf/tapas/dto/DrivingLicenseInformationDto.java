package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.annotation.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DrivingLicenseInformationDto {

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @Column("class")
    private String className;

    @Column("name_dli")
    private String drivingLicenseInfoName;

    @Column("code_dli")
    private int drivingLicenseInfoCode;
}
