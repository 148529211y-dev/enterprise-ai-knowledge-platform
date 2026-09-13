package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.AsyncTask;
import com.ruoyi.ai.mapper.AsyncTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncTaskServiceTest {

    @Mock
    private AsyncTaskMapper asyncTaskMapper;

    @InjectMocks
    private AsyncTaskServiceImpl asyncTaskService;

    @Test
    void submitDocProcessTask_savesWaitingStatus() {
        when(asyncTaskMapper.insert(any(AsyncTask.class))).thenReturn(1);
        when(asyncTaskMapper.updateStatus(anyString(), anyString(), anyString(), any())).thenReturn(1);

        String taskId = asyncTaskService.submitDocProcessTask(100L, 1L, docId -> {});

        assertNotNull(taskId);
        assertFalse(taskId.isEmpty());

        ArgumentCaptor<AsyncTask> captor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskMapper).insert(captor.capture());
        AsyncTask saved = captor.getValue();
        assertEquals("WAITING", saved.getStatus());
        assertEquals("doc_process", saved.getTaskType());
        assertEquals(100L, saved.getBizId());
        assertEquals(taskId, saved.getTaskId());
    }

    @Test
    void submitDocProcessTask_returnsNonNullTaskId() {
        when(asyncTaskMapper.insert(any(AsyncTask.class))).thenReturn(1);
        when(asyncTaskMapper.updateStatus(anyString(), anyString(), anyString(), any())).thenReturn(1);

        String taskId = asyncTaskService.submitDocProcessTask(200L, null, docId -> {});

        assertNotNull(taskId);
        assertEquals(32, taskId.length());
    }

    @Test
    void getTaskStatus_delegatesToMapper() {
        AsyncTask expected = new AsyncTask();
        expected.setTaskId("abc123");
        expected.setStatus("SUCCESS");
        when(asyncTaskMapper.selectByTaskId("abc123")).thenReturn(expected);

        AsyncTask result = asyncTaskService.getTaskStatus("abc123");

        assertNotNull(result);
        assertEquals("abc123", result.getTaskId());
        assertEquals("SUCCESS", result.getStatus());
        verify(asyncTaskMapper).selectByTaskId("abc123");
    }

    @Test
    void getTaskStatus_unknownTask_returnsNull() {
        when(asyncTaskMapper.selectByTaskId("unknown")).thenReturn(null);

        AsyncTask result = asyncTaskService.getTaskStatus("unknown");

        assertNull(result);
    }

    @Test
    void submitDocProcessTask_setsProgressDescription() {
        when(asyncTaskMapper.insert(any(AsyncTask.class))).thenReturn(1);
        when(asyncTaskMapper.updateStatus(anyString(), anyString(), anyString(), any())).thenReturn(1);

        asyncTaskService.submitDocProcessTask(300L, 2L, docId -> {});

        ArgumentCaptor<AsyncTask> captor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getProgress());
    }
}
