package de.dlr.ivf.tapas.dto;

import de.dlr.ivf.api.io.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class EpisodeDto {

    @Column("scheme_id")
    private int schemeId;

    @Column("start")
    private int start;

    @Column("duration")
    private int duration;

    @Column("act_code_zbe")
    private int actCodeZbe;

    @Column("home")
    private boolean isHome;

    @Column("tournumber")
    private int tourNumber;

    @Column("workchain")
    private boolean isWorkChain;

    @Column("key")
    private String key;
}
