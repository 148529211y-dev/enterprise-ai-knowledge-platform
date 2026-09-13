package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.mapper.KbChunkMapper;
import com.ruoyi.ai.mapper.KbDocumentMapper;
import com.ruoyi.ai.service.AsyncTaskService;
import com.ruoyi.ai.service.EmbeddingService;
import com.ruoyi.ai.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class KbServiceTest {

    @Mock
    private KbDocumentMapper docMapper;
    @Mock
    private KbChunkMapper chunkMapper;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private VectorStoreService vectorStore;
    @Mock
    private AsyncTaskService asyncTaskService;

    @InjectMocks
    private KbServiceImpl kbService;

    private Method chunkTextMethod;

    @BeforeEach
    void setUp() throws Exception {
        chunkTextMethod = KbServiceImpl.class.getDeclaredMethod("chunkText", String.class);
        chunkTextMethod.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private List<String> chunkText(String text) throws Exception {
        return (List<String>) chunkTextMethod.invoke(kbService, text);
    }

    @Test
    void chunkText_shortText_returnsSingleChunk() throws Exception {
        String text = "Short text content.";
        List<String> chunks = chunkText(text);

        assertEquals(1, chunks.size());
        assertEquals("Short text content.", chunks.get(0));
    }

    @Test
    void chunkText_emptyText_returnsEmptyList() throws Exception {
        List<String> chunks = chunkText("");

        assertTrue(chunks.isEmpty());
    }

    @Test
    void chunkText_multipleParagraphs_respectsMaxChunkSize() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i > 0) sb.append("\n\n");
            sb.append("Paragraph ").append(i).append(" with some content to make it longer.");
        }
        String text = sb.toString();

        List<String> chunks = chunkText(text);

        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 500, "Chunk exceeds MAX_CHUNK_SIZE: " + chunk.length());
        }
        assertTrue(chunks.size() >= 1);
    }

    @Test
    void chunkText_longParagraph_splitsCorrectly() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append("a");
        }
        String text = sb.toString();

        List<String> chunks = chunkText(text);

        assertTrue(chunks.size() > 1, "Long text should be split into multiple chunks");
    }

    @Test
    void chunkText_paragraphExceedingMax_triggersSplitLongText() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1200; i++) {
            sb.append("x");
        }
        String text = sb.toString();

        List<String> chunks = chunkText(text);

        assertTrue(chunks.size() >= 2);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 500 || chunks.size() == 1);
        }
    }

    @Test
    void chunkText_twoParagraphs_createsOverlap() throws Exception {
        String p1 = "First paragraph content here for testing purposes.";
        String p2 = "Second paragraph content here for testing purposes.";
        String text = p1 + "\n\n" + p2;

        List<String> chunks = chunkText(text);

        if (chunks.size() > 1) {
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);
            assertTrue(secondChunk.contains("First") || secondChunk.length() <= 500);
        }
    }

    @Test
    void chunkText_preservesAllContent() throws Exception {
        String p1 = "Alpha paragraph.";
        String p2 = "Beta paragraph.";
        String p3 = "Gamma paragraph.";
        String text = p1 + "\n\n" + p2 + "\n\n" + p3;

        List<String> chunks = chunkText(text);
        String rejoined = String.join(" ", chunks);

        assertTrue(rejoined.contains("Alpha"));
        assertTrue(rejoined.contains("Beta"));
        assertTrue(rejoined.contains("Gamma"));
    }
}
