package mil.dod.mew;

import mil.dod.mew.domain.entity.CatalogSource;
import mil.dod.mew.repository.CatalogSourceRepository;
import mil.dod.mew.service.CatalogSourceReconciliationService;
import mil.dod.mew.service.exception.BusinessValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"mew.synthetic.scenario=demo", "mew.synthetic.history-days=7"})
@ActiveProfiles("synthetic-data")
@Transactional
class CatalogSourceReconciliationServiceTest {
    private static final String HEADER = "source_id,title,source_url,retrieved_on,source_version,license_notes,reviewer,review_status\n";

    @Autowired private CatalogSourceReconciliationService reconciliation;
    @Autowired private CatalogSourceRepository catalogSources;

    @Test
    void previewReportsChangesWithoutWritingAndApplyUpsertsTheSource() {
        String manifest = HEADER + "TEST-CURATED-SOURCE,Curated test source,https://example.test/catalog,2026-11-17,v1,Public reference,Test reviewer,VERIFIED\n";

        CatalogSourceReconciliationService.ReconciliationResult preview = reconciliation.preview(manifest);
        assertFalse(preview.applied());
        assertEquals(1, preview.added());
        assertEquals(0, preview.updated());
        assertFalse(catalogSources.findBySourceId("TEST-CURATED-SOURCE").isPresent());

        CatalogSourceReconciliationService.ReconciliationResult applied = reconciliation.apply(manifest);
        assertTrue(applied.applied());
        assertEquals(1, applied.added());
        CatalogSource saved = catalogSources.findBySourceId("TEST-CURATED-SOURCE").orElseThrow();
        assertEquals("Curated test source", saved.getTitle());
        assertEquals("VERIFIED", saved.getReviewStatus());
    }

    @Test
    void rejectsUnsafeOrAmbiguousManifestRowsBeforeWriting() {
        String duplicate = HEADER
                + "DUPLICATE-SOURCE,One,https://example.test/one,2026-11-17,v1,Public reference,Reviewer,VERIFIED\n"
                + "DUPLICATE-SOURCE,Two,https://example.test/two,2026-11-17,v1,Public reference,Reviewer,VERIFIED\n";

        assertThrows(BusinessValidationException.class, () -> reconciliation.apply(duplicate));
        assertFalse(catalogSources.findBySourceId("DUPLICATE-SOURCE").isPresent());
    }
}
