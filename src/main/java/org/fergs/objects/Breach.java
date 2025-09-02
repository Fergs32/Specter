package org.fergs.objects;

import java.time.LocalDate;


public record Breach(int breachId, String site, long recordsCount, String description, LocalDate publishDate) { }
