package org.fergs.objects;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter @Setter
public class Breach {
    public final int breachId;
    public final String site;
    public final long recordsCount;
    public final String description;
    public final LocalDate publishDate;

    public Breach(int breachId, String site, long recordsCount, String description, LocalDate publishDate) {
        this.breachId = breachId;
        this.site = site;
        this.recordsCount = recordsCount;
        this.description = description;
        this.publishDate = publishDate;
    }
}
