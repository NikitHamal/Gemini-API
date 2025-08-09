package com.gemini.api.client.models;

import java.util.HashMap;
import java.util.stream.Collectors;

public class GemJar extends HashMap<String, Gem> {

    /**
     * Retrieves a gem by its ID and/or name.
     * @param id The unique identifier of the gem. Can be null if searching by name.
     * @param name The user-friendly name of the gem. Can be null if searching by id.
     * @return The matching Gem, or null if not found.
     */
    public Gem get(String id, String name) {
        if (id != null) {
            Gem gem = super.get(id);
            if (gem != null) {
                // If name is also provided, it must match.
                if (name == null || name.equals(gem.getName())) {
                    return gem;
                }
            }
        } else if (name != null) {
            // If only name is provided, search through the values.
            for (Gem gem : values()) {
                if (name.equals(gem.getName())) {
                    return gem;
                }
            }
        }
        return null;
    }

    /**
     * Filters the gems based on criteria.
     * @param predefined Filter by whether the gem is predefined. Can be null to ignore this filter.
     * @param name Filter by the gem's name. Can be null to ignore this filter.
     * @return A new GemJar containing the filtered gems.
     */
    public GemJar filter(Boolean predefined, String name) {
        return values().stream()
                .filter(gem -> (predefined == null || gem.isPredefined() == predefined))
                .filter(gem -> (name == null || name.equals(gem.getName())))
                .collect(Collectors.toMap(Gem::getId, gem -> gem, (a, b) -> b, GemJar::new));
    }
}
