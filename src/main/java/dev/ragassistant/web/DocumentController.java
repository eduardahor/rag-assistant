package dev.ragassistant.web;

import dev.ragassistant.error.InvalidDocumentException;
import dev.ragassistant.ingestion.DocumentService;
import dev.ragassistant.ingestion.IngestionResult;
import dev.ragassistant.store.DocumentInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".md", ".markdown", ".txt");

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionResult> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || SUPPORTED_EXTENSIONS.stream().noneMatch(filename.toLowerCase(Locale.ROOT)::endsWith)) {
            throw new InvalidDocumentException("Formato não suportado. Envie arquivos " + SUPPORTED_EXTENSIONS + ".");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.ingest(filename, content));
    }

    @GetMapping
    public List<DocumentInfo> list() {
        return documentService.list();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return documentService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
