package dev.ragassistant.web;

import dev.ragassistant.rag.Answer;
import dev.ragassistant.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public Answer ask(@Valid @RequestBody AskRequest request) {
        return ragService.answer(request.question().strip());
    }
}
