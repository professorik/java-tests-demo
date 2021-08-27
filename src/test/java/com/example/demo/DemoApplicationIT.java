package com.example.demo;

import com.example.demo.controller.ToDoController;
import com.example.demo.dto.ToDoSaveRequest;
import com.example.demo.model.ToDoEntity;
import com.example.demo.repository.ToDoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@AutoConfigureMockMvc
class DemoApplicationIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ToDoRepository toDoRepository;

	@Autowired
	private ToDoController toDoController;

	@Test
	void whenValidSave_thenReturnSameResponse() throws Exception {
		var todoSaveReq = new ToDoSaveRequest();
		todoSaveReq.id = 2L;
		todoSaveReq.text = "My to do text";

		var id = toDoRepository.save(new ToDoEntity(todoSaveReq.id, todoSaveReq.text)).getId();

		todoSaveReq.id = id;

		mockMvc.perform(post("/todos")
				.param("isAdmin", "true")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper()
						.writer()
						.withDefaultPrettyPrinter()
						.writeValueAsString(todoSaveReq)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.text").value(todoSaveReq.text));
		toDoRepository.deleteAll();
	}

	@Test
	void whenRequestCompleteValidId_thenReturnCompleted() throws Exception {
		var todoSaveReq = new ToDoSaveRequest();
		todoSaveReq.text = "My to do text";

		var id = toDoRepository.save(new ToDoEntity(todoSaveReq.text)).getId();
		mockMvc.perform(put("/todos/" + id + "/complete")
				.param("isAdmin", "true"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.completedAt").exists());
		toDoRepository.deleteAll();
	}

	@Test
	void whenDeleteWithoutAdmin_thenReturnForbidden() throws Exception {
		var id = toDoRepository.save(new ToDoEntity("Some text")).getId();

		mockMvc.perform(delete("/todos/" + id)
				.param("isAdmin", "false"))
				.andExpect(jsonPath("$").value("Only admin can do this"));
		toDoRepository.deleteAll();
	}

	@Test
	void contextLoads() throws Exception {
		if (toDoController == null) {
			throw new Exception("ToDoController is null");
		}
	}
}
