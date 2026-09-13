package com.ruoyi.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {1.0f, 2.0f, 3.0f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(1.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.0f, 1.0f, 0.0f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_oppositeVectors_returnsNegativeOne() {
        float[] a = {1.0f, 2.0f, 3.0f};
        float[] b = {-1.0f, -2.0f, -3.0f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(-1.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_differentLengthVectors_returnsZero() {
        float[] a = {1.0f, 2.0f};
        float[] b = {1.0f, 2.0f, 3.0f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_nullFirstVector_returnsZero() {
        float[] b = {1.0f, 2.0f, 3.0f};

        double result = EmbeddingService.cosineSimilarity(null, b);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_nullSecondVector_returnsZero() {
        float[] a = {1.0f, 2.0f, 3.0f};

        double result = EmbeddingService.cosineSimilarity(a, null);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_bothNull_returnsZero() {
        double result = EmbeddingService.cosineSimilarity(null, null);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_zeroVectors_returnsZero() {
        float[] a = {0.0f, 0.0f, 0.0f};
        float[] b = {1.0f, 2.0f, 3.0f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(0.0, result, 1e-9);
    }

    @Test
    void cosineSimilarity_knownVectors_returnsExpected() {
        float[] a = {1.0f, 0.0f};
        float[] b = {0.7071f, 0.7071f};

        double result = EmbeddingService.cosineSimilarity(a, b);

        assertEquals(0.7071, result, 1e-3);
    }
}
