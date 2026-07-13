package org.main.builds.extract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.main.builds.model.ItemPath;
import org.main.builds.source.ItemCatalog;

class ItemSequenceExtractorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void reconstructsItemsInRawArrayOrderAtTheInclusiveCutoff() throws IOException {
        JsonNode timeline = mapper.readTree(getClass().getResourceAsStream(
                "/builds/timeline-item-sequence.json"));
        ItemCatalog catalog = new ItemCatalog() {
            @Override
            public boolean isStartingItem(int id) {
                return Set.of(2003, 1055).contains(id);
            }

            @Override
            public boolean isCompletedBoot(int id) {
                return id == 3006;
            }

            @Override
            public boolean isCompletedCoreItem(int id) {
                return Set.of(6672, 3031, 3094).contains(id);
            }
        };

        ItemPath path = new ItemSequenceExtractor(Duration.ofMinutes(2)).extract(
                timeline, 1, Set.of(3006, 6672, 3031, 3094, 1038), catalog);

        assertThat(path.startingItems()).containsExactly(2003, 1055);
        assertThat(path.boots()).isEqualTo(3006);
        assertThat(path.coreItems()).containsExactly(6672, 3031, 3094);
    }
}
