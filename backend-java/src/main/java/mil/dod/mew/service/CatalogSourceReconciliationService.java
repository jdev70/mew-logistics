package mil.dod.mew.service;

import mil.dod.mew.domain.entity.CatalogSource;
import mil.dod.mew.repository.CatalogSourceRepository;
import mil.dod.mew.service.exception.BusinessValidationException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reconciles the versioned, reviewed catalog-source manifest with the source registry.
 * This workflow deliberately never deletes registry records, because existing catalog
 * associations may depend on them. Operators should run a dry run before applying.
 */
@Service
public class CatalogSourceReconciliationService {
    public static final String MANIFEST_RESOURCE = "catalog-sources/manifest.csv";
    private static final String HEADER = "source_id,title,source_url,retrieved_on,source_version,license_notes,reviewer,review_status";
    private static final Set<String> REVIEW_STATUSES = Set.of("DRAFT", "VERIFIED", "STALE", "RETIRED");

    private final CatalogSourceRepository catalogSources;

    public CatalogSourceReconciliationService(CatalogSourceRepository catalogSources) {
        this.catalogSources = catalogSources;
    }

    @Transactional(readOnly = true)
    public ReconciliationResult previewBundledManifest() {
        return reconcileBundledManifest(false);
    }

    @Transactional
    public ReconciliationResult applyBundledManifest() {
        return reconcileBundledManifest(true);
    }

    @Transactional(readOnly = true)
    public ReconciliationResult preview(String manifest) {
        return reconcile(manifest, false);
    }

    @Transactional
    public ReconciliationResult apply(String manifest) {
        return reconcile(manifest, true);
    }

    private ReconciliationResult reconcileBundledManifest(boolean apply) {
        ClassPathResource resource = new ClassPathResource(MANIFEST_RESOURCE);
        try (Reader reader = new BufferedReader(new java.io.InputStreamReader(resource.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            return reconcile(readAll(reader), apply);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read bundled catalog-source manifest.", exception);
        }
    }

    private ReconciliationResult reconcile(String manifest, boolean apply) {
        List<ManifestEntry> entries = parse(manifest);
        Map<String, CatalogSource> existingById = new HashMap<>();
        for (CatalogSource source : catalogSources.findAll()) {
            existingById.put(source.getSourceId(), source);
        }

        int added = 0;
        int updated = 0;
        int unchanged = 0;
        Set<String> manifestIds = new HashSet<>();
        List<CatalogSource> toSave = new ArrayList<>();
        for (ManifestEntry entry : entries) {
            manifestIds.add(entry.sourceId());
            CatalogSource source = existingById.get(entry.sourceId());
            if (source == null) {
                added++;
                if (apply) {
                    source = new CatalogSource();
                    copy(entry, source);
                    toSave.add(source);
                }
            } else if (matches(entry, source)) {
                unchanged++;
            } else {
                updated++;
                if (apply) {
                    copy(entry, source);
                    toSave.add(source);
                }
            }
        }
        if (apply && !toSave.isEmpty()) {
            catalogSources.saveAll(toSave);
        }

        List<String> unlistedRegistryIds = existingById.keySet().stream()
                .filter(id -> !manifestIds.contains(id)).sorted().toList();
        return new ReconciliationResult(apply, entries.size(), added, updated, unchanged, unlistedRegistryIds);
    }

    private List<ManifestEntry> parse(String manifest) {
        if (manifest == null || manifest.isBlank()) {
            throw new BusinessValidationException("Catalog-source manifest cannot be empty.");
        }
        try (BufferedReader reader = new BufferedReader(new StringReader(manifest))) {
            String header = reader.readLine();
            if (!HEADER.equals(header)) {
                throw new BusinessValidationException("Catalog-source manifest header is invalid.");
            }
            List<ManifestEntry> entries = new ArrayList<>();
            Set<String> sourceIds = new LinkedHashSet<>();
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                ManifestEntry entry = parseRow(line, row);
                if (!sourceIds.add(entry.sourceId())) {
                    throw new BusinessValidationException("Duplicate source_id in manifest: " + entry.sourceId());
                }
                entries.add(entry);
            }
            if (entries.isEmpty()) {
                throw new BusinessValidationException("Catalog-source manifest must contain at least one source.");
            }
            return entries;
        } catch (IOException impossible) {
            throw new IllegalStateException("Unable to read catalog-source manifest.", impossible);
        }
    }

    private ManifestEntry parseRow(String line, int row) {
        String[] values = line.split(",", -1);
        if (values.length != 8) {
            throw new BusinessValidationException("Manifest row " + row + " must contain exactly 8 comma-separated values.");
        }
        for (String value : values) {
            if (value.isBlank()) {
                throw new BusinessValidationException("Manifest row " + row + " contains a blank required value.");
            }
        }
        if (!values[0].matches("[A-Z0-9-]{3,60}")) {
            throw new BusinessValidationException("Manifest row " + row + " has an invalid source_id.");
        }
        try {
            URI uri = URI.create(values[2]);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new BusinessValidationException("Manifest row " + row + " must use an absolute HTTPS source_url.");
            }
            LocalDate retrievedOn = LocalDate.parse(values[3]);
            if (!REVIEW_STATUSES.contains(values[7])) {
                throw new BusinessValidationException("Manifest row " + row + " has an unsupported review_status.");
            }
            return new ManifestEntry(values[0], values[1], values[2], retrievedOn, values[4], values[5], values[6], values[7]);
        } catch (DateTimeParseException exception) {
            throw new BusinessValidationException("Manifest row " + row + " has an invalid retrieved_on date.");
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Manifest row " + row + " has an invalid source_url.");
        }
    }

    private boolean matches(ManifestEntry entry, CatalogSource source) {
        return entry.title().equals(source.getTitle())
                && entry.sourceUrl().equals(source.getSourceUrl())
                && entry.retrievedOn().equals(source.getRetrievedOn())
                && entry.sourceVersion().equals(source.getSourceVersion())
                && entry.licenseNotes().equals(source.getLicenseNotes())
                && entry.reviewer().equals(source.getReviewer())
                && entry.reviewStatus().equals(source.getReviewStatus());
    }

    private void copy(ManifestEntry entry, CatalogSource source) {
        source.setSourceId(entry.sourceId());
        source.setTitle(entry.title());
        source.setSourceUrl(entry.sourceUrl());
        source.setRetrievedOn(entry.retrievedOn());
        source.setSourceVersion(entry.sourceVersion());
        source.setLicenseNotes(entry.licenseNotes());
        source.setReviewer(entry.reviewer());
        source.setReviewStatus(entry.reviewStatus());
        source.setLastReviewedOn(entry.retrievedOn());
    }

    private String readAll(Reader reader) throws IOException {
        StringBuilder contents = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) contents.append(buffer, 0, read);
        return contents.toString();
    }

    private record ManifestEntry(String sourceId, String title, String sourceUrl, LocalDate retrievedOn,
                                 String sourceVersion, String licenseNotes, String reviewer,
                                 String reviewStatus) { }

    public record ReconciliationResult(boolean applied, int manifestEntries, int added, int updated,
                                       int unchanged, List<String> unlistedRegistryIds) { }
}
