package mil.dod.mew;

import mil.dod.mew.domain.entity.CatalogSource;
import mil.dod.mew.repository.CatalogSourceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {"mew.synthetic.scenario=demo", "mew.synthetic.history-days=7"})
@ActiveProfiles("synthetic-data")
class CatalogSourceManifestTest {
    private static final String MANIFEST_RESOURCE = "/catalog-sources/manifest.csv";

    @Autowired
    private CatalogSourceRepository catalogSources;

    @Test
    void everySeededCatalogSourceMatchesTheVersionedManifest() throws Exception {
        Map<String, ManifestEntry> manifest = readManifest();

        for (CatalogSource source : catalogSources.findAll()) {
            ManifestEntry entry = manifest.get(source.getSourceId());
            assertNotNull(entry, () -> "Missing manifest entry for " + source.getSourceId());
            assertEquals(entry.title(), source.getTitle());
            assertEquals(entry.sourceUrl(), source.getSourceUrl());
            assertEquals(entry.retrievedOn(), source.getRetrievedOn());
            assertEquals(entry.sourceVersion(), source.getSourceVersion());
            assertEquals(entry.licenseNotes(), source.getLicenseNotes());
            assertEquals(entry.reviewer(), source.getReviewer());
            assertEquals(entry.reviewStatus(), source.getReviewStatus());
        }
        assertEquals(manifest.size(), catalogSources.count(),
                "The manifest and seeded source registry must contain the same sources.");
    }

    private Map<String, ManifestEntry> readManifest() throws Exception {
        try (var stream = getClass().getResourceAsStream(MANIFEST_RESOURCE)) {
            assertNotNull(stream, "Catalog-source manifest must be packaged with the application.");
            try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                reader.readLine();
                return reader.lines().filter(line -> !line.isBlank()).map(this::parse)
                        .collect(Collectors.toMap(ManifestEntry::sourceId, entry -> entry));
            }
        }
    }

    private ManifestEntry parse(String line) {
        String[] values = line.split(",", -1);
        if (values.length != 8) {
            throw new IllegalArgumentException("Invalid catalog-source manifest row: " + line);
        }
        return new ManifestEntry(values[0], values[1], values[2], LocalDate.parse(values[3]), values[4],
                values[5], values[6], values[7]);
    }

    private record ManifestEntry(String sourceId, String title, String sourceUrl, LocalDate retrievedOn,
                                 String sourceVersion, String licenseNotes, String reviewer,
                                 String reviewStatus) { }
}
