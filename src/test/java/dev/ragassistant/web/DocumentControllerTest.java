package dev.ragassistant.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.ragassistant.ingestion.DocumentService;
import dev.ragassistant.ingestion.IngestionResult;
import dev.ragassistant.store.DocumentInfo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean DocumentService documentService;

    @Test
    void uploadIndexesMarkdownFile() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.ingest(eq("api.md"), anyString())).thenReturn(new IngestionResult(id, "api.md", 3));
        MockMultipartFile file = new MockMultipartFile("file", "api.md", "text/markdown", "# API".getBytes());

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("api.md"))
                .andExpect(jsonPath("$.chunks").value(3));
    }

    @Test
    void uploadRejectsUnsupportedExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", new byte[] {1});

        mvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(documentService);
    }

    @Test
    void listReturnsDocuments() throws Exception {
        when(documentService.list()).thenReturn(List.of(new DocumentInfo(UUID.randomUUID(), "api.md", Instant.now(), 3)));

        mvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filename").value("api.md"));
    }

    @Test
    void deleteReturns204WhenFoundAnd404Otherwise() throws Exception {
        UUID found = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(documentService.delete(found)).thenReturn(true);
        when(documentService.delete(missing)).thenReturn(false);

        mvc.perform(delete("/api/documents/" + found)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/documents/" + missing)).andExpect(status().isNotFound());
    }
}
