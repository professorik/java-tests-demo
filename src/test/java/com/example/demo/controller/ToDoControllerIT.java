package com.example.demo.controller;

import com.example.demo.dto.ToDoSaveRequest;
import com.example.demo.dto.mapper.ToDoEntityToResponseMapper;
import com.example.demo.exception.ToDoNotFoundException;
import com.example.demo.model.ToDoEntity;
import com.example.demo.service.ToDoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToDoController.class)
@ActiveProfiles(profiles = "test")
class ToDoControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToDoService toDoService;

    @Test
    void whenDeleteOneWithRightCredentials_returnNoContent() throws Exception {
        this.mockMvc
                .perform(delete("/todos/0")
                        .param("isAdmin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void whenCompleteWrongId_thenThrowToDoNotFoundExceptionStatus404() throws Exception {
        when(toDoService.completeToDo(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean()))
                .thenThrow(new ToDoNotFoundException(0l));
        this.mockMvc
                .perform(put("/todos/0/complete")
                        .param("isAdmin", "true"))
                .andExpect(jsonPath("$").value("Can not find todo with id " + 0l));
    }

    @Test
    void whenSave_thenReturnSameButResponse() throws Exception {
        String testText = "My to do text";
        Long testId = 1l;
        var todoSaveReq = new ToDoSaveRequest();
        todoSaveReq.id = testId;
        todoSaveReq.text = testText;
        when(toDoService.upsert(ArgumentMatchers.any(ToDoSaveRequest.class), ArgumentMatchers.anyBoolean()))
                .thenReturn(ToDoEntityToResponseMapper.map(new ToDoEntity(testId, testText)));
        this.mockMvc
                .perform(post("/todos")
                        .param("isAdmin", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper()
                                .writer()
                                .withDefaultPrettyPrinter()
                                .writeValueAsString(todoSaveReq)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value(testText))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.id").value(testId))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
    }

    @Test
    void whenGetAll_thenReturnValidResponse() throws Exception {
        String testText = "My to do text";
        Long testId = 1l;
        when(toDoService.getAll()).thenReturn(
                Arrays.asList(
                        ToDoEntityToResponseMapper.map(new ToDoEntity(testId, testText))
                )
        );
        this.mockMvc
                .perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text").value(testText))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].id").value(testId))
                .andExpect(jsonPath("$[0].completedAt").doesNotExist());
    }
}