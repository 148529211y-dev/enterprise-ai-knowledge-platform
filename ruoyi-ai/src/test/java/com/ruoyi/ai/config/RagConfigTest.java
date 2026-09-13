package com.ruoyi.ai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagConfigTest {

    @Test
    void defaultTopK_isThree() {
        RagConfig config = new RagConfig();
        assertEquals(3, config.getTopK());
    }

    @Test
    void defaultTemperature_isZeroPointThree() {
        RagConfig config = new RagConfig();
        assertEquals(0.3, config.getTemperature(), 1e-9);
    }

    @Test
    void defaultSimilarityThreshold_isZeroPointThree() {
        RagConfig config = new RagConfig();
        assertEquals(0.3, config.getSimilarityThreshold(), 1e-9);
    }

    @Test
    void defaultMaxTokens_is2048() {
        RagConfig config = new RagConfig();
        assertEquals(2048, config.getMaxTokens());
    }

    @Test
    void defaultChunkStrategy_isParagraph() {
        RagConfig config = new RagConfig();
        assertEquals(ChunkStrategy.PARAGRAPH, config.getChunkStrategy());
    }

    @Test
    void setTopK_updatesValue() {
        RagConfig config = new RagConfig();
        config.setTopK(5);
        assertEquals(5, config.getTopK());
    }

    @Test
    void setTemperature_updatesValue() {
        RagConfig config = new RagConfig();
        config.setTemperature(0.7);
        assertEquals(0.7, config.getTemperature(), 1e-9);
    }

    @Test
    void setSimilarityThreshold_updatesValue() {
        RagConfig config = new RagConfig();
        config.setSimilarityThreshold(0.5);
        assertEquals(0.5, config.getSimilarityThreshold(), 1e-9);
    }

    @Test
    void setMaxTokens_updatesValue() {
        RagConfig config = new RagConfig();
        config.setMaxTokens(4096);
        assertEquals(4096, config.getMaxTokens());
    }

    @Test
    void setChunkStrategy_updatesValue() {
        RagConfig config = new RagConfig();
        config.setChunkStrategy(ChunkStrategy.FIXED_SIZE);
        assertEquals(ChunkStrategy.FIXED_SIZE, config.getChunkStrategy());
    }
}
