package com.sleepwell.sleepwell_backend.rag;

import com.sleepwell.sleepwell_backend.rag.dto.RagQueryRequest;
import com.sleepwell.sleepwell_backend.rag.infra.EmbeddingClient;
import com.sleepwell.sleepwell_backend.rag.infra.PromptBuilder;
import com.sleepwell.sleepwell_backend.rag.infra.RagChatClient;
import com.sleepwell.sleepwell_backend.rag.infra.SparseStore;
import com.sleepwell.sleepwell_backend.rag.infra.adapter.DenseAdapter;
import com.sleepwell.sleepwell_backend.rag.infra.impl.HybridRetriever;
import com.sleepwell.sleepwell_backend.rag.infra.impl.MultiQueryExpander;
import com.sleepwell.sleepwell_backend.rag.model.ScoredDoc;
import com.sleepwell.sleepwell_backend.rag.service.RagIndexService;
import com.sleepwell.sleepwell_backend.rag.service.RagQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 2026 regression/contract tests. Uses doubles only, never starts Spring or external clients. */
class PortfolioRagContractTest {
    @Test void denseUsesTheVectorStoreSearchContractAndPreservesMetadata() {
        VectorStore store = mock(VectorStore.class);
        Document doc = Document.builder().id("doc-1").text("Synthetic sleep document")
                .metadata(Map.of("source", "https://example.org/synthetic", "namespace", "fixture"))
                .score(0.8).build();
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
        var result = new DenseAdapter(store).search("sleep", 3);
        assertEquals(1, result.size());
        assertEquals("doc-1", result.get(0).id());
        assertEquals(doc.getText(), result.get(0).content());
        assertEquals(0.8, result.get(0).score());
        assertEquals("fixture", result.get(0).meta().get("namespace"));
        verify(store).similaritySearch(argThat((SearchRequest req) -> req.getQuery().equals("sleep") && req.getTopK() == 3));
    }

    @Test void denseRejectsInvalidInputsWithoutCallingTheStore() {
        VectorStore store = mock(VectorStore.class);
        var adapter = new DenseAdapter(store);
        assertThrows(IllegalArgumentException.class, () -> adapter.search(" ", 5));
        assertThrows(IllegalArgumentException.class, () -> adapter.search(null, 5));
        assertThrows(IllegalArgumentException.class, () -> adapter.search("sleep", 0));
        verifyNoInteractions(store);
    }

    @Test void denseDoesNotSilentlySwallowStoreFailuresOrNullResults() {
        VectorStore store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("synthetic failure"));
        var adapter = new DenseAdapter(store);
        assertThrows(IllegalStateException.class, () -> adapter.search("sleep", 1));
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> adapter.search("sleep", 1));
    }

    @Test void chunkingTerminatesAndPreservesCharactersAtBoundaries() {
        var index = new RagIndexService(mock(EmbeddingClient.class), mock(VectorStore.class),
                mock(SparseStore.class), mock(Environment.class));
        for (int size = 1; size <= 16; size++) {
            for (int overlap = 0; overlap <= 18; overlap++) {
                String text = "가나다라마바사아자차카타파하ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                List<String> chunks = ReflectionTestUtils.invokeMethod(index, "simpleChunks", text, size, overlap);
                int safeOverlap = Math.min(overlap, size - 1);
                StringBuilder reconstructed = new StringBuilder(chunks.get(0));
                for (int i = 1; i < chunks.size(); i++) reconstructed.append(chunks.get(i).substring(safeOverlap));
                assertEquals(text, reconstructed.toString());
                assertTrue(chunks.size() <= text.length());
            }
        }
        assertEquals(List.of(), ReflectionTestUtils.invokeMethod(index, "simpleChunks", "   ", 8, 2));
        assertEquals(List.of("short"), ReflectionTestUtils.invokeMethod(index, "simpleChunks", "short", 1200, 200));
    }

    @Test void serviceRejectsInvalidRequestsBeforeAnyRetrievalOrGeneration() {
        var prompt = mock(PromptBuilder.class); var chat = mock(RagChatClient.class);
        var hybrid = mock(HybridRetriever.class); var expansion = mock(MultiQueryExpander.class);
        var service = new RagQueryService(prompt, chat, hybrid, expansion, Optional.empty(), Optional.empty());
        for (RagQueryRequest req : Arrays.asList(null, RagQueryRequest.simple(null, 5),
                RagQueryRequest.simple(" ", 5), RagQueryRequest.simple("sleep", 0),
                RagQueryRequest.simple("sleep", 51), RagQueryRequest.simple("x".repeat(4001), 5))) {
            assertThrows(IllegalArgumentException.class, () -> service.answer(req));
        }
        verifyNoInteractions(prompt, chat, hybrid, expansion);
    }

    @Test void queryBuildsCitationsAndPromptWithSyntheticDoubles() throws Exception {
        var prompt = mock(PromptBuilder.class); var chat = mock(RagChatClient.class);
        var hybrid = mock(HybridRetriever.class); var expansion = mock(MultiQueryExpander.class);
        var service = new RagQueryService(prompt, chat, hybrid, expansion, Optional.empty(), Optional.empty());
        ReflectionTestUtils.setField(service, "denseTopK", 30);
        ReflectionTestUtils.setField(service, "sparseTopK", 60);
        ReflectionTestUtils.setField(service, "rerankTopK", 20);
        when(expansion.expand("sleep")).thenReturn(List.of());
        when(chat.chat(anyString(), startsWith("Query:"))).thenReturn("[]");
        var doc = new ScoredDoc("synthetic-1", "sleep fixture", 0.8, "https://example.org/fixture",
                Map.of("title", "Synthetic fixture", "source", "https://example.org/fixture"));
        when(hybrid.search(anyString(), anyInt(), anyInt(), anyBoolean(), anyInt(), anyDouble())).thenReturn(List.of(doc));
        when(prompt.build(eq("sleep"), anyList(), anyMap())).thenAnswer(call ->
                new PromptBuilder.Built("fixture-system", "fixture-user", call.getArgument(1)));
        when(chat.chat("fixture-system", "fixture-user")).thenReturn("Synthetic answer. Second sentence.");
        var response = service.answer(RagQueryRequest.simple("sleep", 5));
        assertEquals("Synthetic answer. Second sentence.", response.get("message_detailed"));
        List<?> citations = (List<?>) response.get("citations");
        assertEquals(1, citations.size());
        assertEquals("https://example.org/fixture", ((PromptBuilder.Cite) citations.get(0)).url());
        verify(prompt).build(eq("sleep"), anyList(), anyMap());
    }
}
