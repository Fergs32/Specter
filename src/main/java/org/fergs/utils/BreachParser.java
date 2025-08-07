package org.fergs.utils;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fergs.objects.Breach;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class BreachParser {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<Breach> parse(final String json) throws Exception {
        final JsonNode root = MAPPER.readTree(json).path("breaches");
        final List<Breach> results = new ArrayList<>();
        final DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;
        for (final Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            final String key = it.next();
            final JsonNode node = root.get(key);
            final Breach b = new Breach(
                    node.path("breachId").asInt(),
                    node.path("site").asText(),
                    node.path("recordsCount").asLong(),
                    node.path("description").asText(),
                    LocalDate.parse(node.path("publishDate").asText(), fmt)
            );
            results.add(b);
        }
        return results;
    }
}
