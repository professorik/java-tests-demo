package com.example.demo.controller;

import com.example.demo.dto.ToDoSaveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.Arrays;
import java.util.Optional;

import com.example.demo.model.ToDoEntity;
import com.example.demo.repository.ToDoRepository;
import com.example.demo.service.ToDoService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ToDoController.class)
@ActiveProfiles(profiles = "test")
@Import(ToDoService.class)
class ToDoControllerWithServiceIT {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ToDoRepository toDoRepository;

	@Test
	void whenGetOne_thenReturnCorrectOne() throws Exception {
		var todo = new ToDoEntity(0l, "Test 1");
		when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(todo));

		this.mockMvc
				.perform(get("/todos/" + todo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(todo.getId()))
				.andExpect(jsonPath("$.text").value(todo.getText()));
	}

	@Test
	void whenSaveValid_thenReturnSameResponse() throws Exception {
		var todoSaveReq = new ToDoSaveRequest();
		todoSaveReq.text = "My to do text";
		var todoEntity = new ToDoEntity(todoSaveReq.text);
		when(toDoRepository.save(ArgumentMatchers.any(ToDoEntity.class))).thenReturn(todoEntity);
		mockMvc.perform(post("/todos")
				.param("isAdmin", "true")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper()
						.writer()
						.withDefaultPrettyPrinter()
						.writeValueAsString(todoSaveReq)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.text").value(todoEntity.getText()))
				.andExpect(jsonPath("$.completedAt").doesNotExist());
	}

	@Test
	void whenGetAll_thenReturnValidResponse() throws Exception {
		String testText = "My to do text";
		when(toDoRepository.findAll()).thenReturn(
				Arrays.asList(new ToDoEntity(1l, testText))
		);
		this.mockMvc
				.perform(get("/todos"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].text").value(testText))
				.andExpect(jsonPath("$[0].id").isNumber())
				.andExpect(jsonPath("$[0].completedAt").doesNotExist());
	}

	@Test
	void whenGetOneNotFind_thenReturnToDoNotFound() throws Exception {
		mockMvc.perform(get("/todos/" + -1l))
				.andExpect(jsonPath("$").value("Can not find todo with id " + -1l));
	}
}