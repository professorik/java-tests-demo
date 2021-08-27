package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.ToDoSaveRequest;
import com.example.demo.dto.mapper.ToDoEntityToResponseMapper;
import com.example.demo.exception.ToDoNotFoundException;
import com.example.demo.model.ToDoEntity;
import com.example.demo.repository.ToDoRepository;

import javax.naming.NoPermissionException;

class ToDoServiceTest {

	private ToDoRepository toDoRepository;

	private ToDoService toDoService;

	//executes before each test defined below
	@BeforeEach
	void setUp() {
		this.toDoRepository = mock(ToDoRepository.class);
		toDoService = new ToDoService(toDoRepository);
	}

	@Test
	void whenGetAll_thenReturnAll() {
		var testToDos = new ArrayList<ToDoEntity>();
		testToDos.add(new ToDoEntity(0l, "Test 1"));
		var toDo = new ToDoEntity(1l, "Test 2");
		toDo.completeNow();
		testToDos.add(toDo);
		when(toDoRepository.findAll()).thenReturn(testToDos);
		var todos = toDoService.getAll();
		assertTrue(todos.size() == testToDos.size());
		for (int i = 0; i < todos.size(); i++) {
			assertThat(todos.get(i), samePropertyValuesAs(
				ToDoEntityToResponseMapper.map(testToDos.get(i))
			));
		}
	}

	@Test
	void whenUpsertWithId_thenReturnUpdated() throws ToDoNotFoundException, NoPermissionException {
		var expectedToDo = new ToDoEntity(0l, "New Item");
		when(toDoRepository.findById(anyLong())).thenAnswer(i -> {
			Long id = i.getArgument(0, Long.class);
			if (id.equals(expectedToDo.getId())) {
				return Optional.of(expectedToDo);
			} else {
				return Optional.empty();
			}
		});
		when(toDoRepository.save(ArgumentMatchers.any(ToDoEntity.class))).thenAnswer(i -> {
			ToDoEntity arg = i.getArgument(0, ToDoEntity.class);
			Long id = arg.getId();
			if (id != null) {
				if (!id.equals(expectedToDo.getId()))
					return new ToDoEntity(id, arg.getText());
				expectedToDo.setText(arg.getText());
				return expectedToDo; //return valid result only if we get valid id
			} else {
				return new ToDoEntity(40158l, arg.getText());
			}
		});
		var toDoSaveRequest = new ToDoSaveRequest();
		toDoSaveRequest.id = expectedToDo.getId();
		toDoSaveRequest.text = "Updated Item";
		var todo = toDoService.upsert(toDoSaveRequest, true);
		assertTrue(todo.id == toDoSaveRequest.id);
		assertTrue(todo.text.equals(toDoSaveRequest.text));
	}
	
	@Test
	void whenUpsertNoId_thenReturnNew() throws ToDoNotFoundException, NoPermissionException {
		var newId = 0l;
		when(toDoRepository.findById(anyLong())).thenAnswer(i -> {
			Long id = i.getArgument(0, Long.class);
			if (id == newId) {
				return Optional.empty();
			} else {
				return Optional.of(new ToDoEntity(newId, "Wrong ToDo"));
			}
		});
		when(toDoRepository.save(ArgumentMatchers.any(ToDoEntity.class))).thenAnswer(i -> {
			ToDoEntity arg = i.getArgument(0, ToDoEntity.class);
			Long id = arg.getId();
			if (id == null)
				return new ToDoEntity(newId, arg.getText());
			else 
				return new ToDoEntity();
		});
		var toDoDto = new ToDoSaveRequest();
		toDoDto.text = "Created Item";
		var result = toDoService.upsert(toDoDto, true);
		assertTrue(result.id == newId);
		assertTrue(result.text.equals(toDoDto.text));
	}

	@Test
	void whenUpsertWithIdNotFound_thenThrowToDoNotFoundException() {
		var expectedToDo = new ToDoEntity(3l, "New Item");
		when(toDoRepository.findById(anyLong())).thenReturn(Optional.empty());
		var toDoSaveRequest = new ToDoSaveRequest();
		toDoSaveRequest.id = expectedToDo.getId();
		toDoSaveRequest.text = "Updated Item";
		assertThrows(ToDoNotFoundException.class, () -> toDoService.upsert(toDoSaveRequest, true));
	}

	@Test
	void whenComplete_thenReturnWithCompletedAt() throws ToDoNotFoundException, NoPermissionException {
		var startTime = ZonedDateTime.now(ZoneOffset.UTC);
		var todo = new ToDoEntity(0l, "Test 1");
		when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(todo));
		when(toDoRepository.save(ArgumentMatchers.any(ToDoEntity.class))).thenAnswer(i -> {
			ToDoEntity arg = i.getArgument(0, ToDoEntity.class);
			Long id = arg.getId();
			if (id.equals(todo.getId())) {
				return todo;
			} else {
				return new ToDoEntity();
			}
		});
		var result = toDoService.completeToDo(todo.getId(), true);
		assertTrue(result.id == todo.getId());
		assertTrue(result.text.equals(todo.getText()));
		assertTrue(result.completedAt.isAfter(startTime));
	}

	@Test
	void whenCompleteNotFound_thenThrowToDoNotFoundException() {
		var todo = new ToDoEntity(0l, "Test 1");
		when(toDoRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(ToDoNotFoundException.class, () -> toDoService.completeToDo(todo.getId(), true));
	}

	@Test
	void whenGetOne_thenReturnCorrectOne() throws ToDoNotFoundException {
		var todo = new ToDoEntity(0l, "Test 1");
		when(toDoRepository.findById(anyLong())).thenReturn(Optional.of(todo));
		var result = toDoService.getOne(0l);
		assertThat(result, samePropertyValuesAs(
			ToDoEntityToResponseMapper.map(todo)
		));
	}

	@Test
	void whenDeleteOne_thenRepositoryDeleteCalled() throws NoPermissionException {
		var id = 0l;
		toDoService.deleteOne(id, true);
		verify(toDoRepository, times(1)).deleteById(id);
	}

	@Test
	void whenIdNotFound_thenThrowNotFoundException() {
		assertThrows(ToDoNotFoundException.class, () -> toDoService.getOne(1l));
	}

	@Test
	void whenDeleteNotAdmin_thenThrowNoPermissionException() {
		assertThrows(NoPermissionException.class, () -> toDoService.deleteOne(1l, false));
	}

	@Test
	void whenUpsertNotAdmin_thenThrowNoPermissionException() {
		var toDoSaveRequest = new ToDoSaveRequest();
		toDoSaveRequest.id = 0l;
		toDoSaveRequest.text = "Updated Item";
		assertThrows(NoPermissionException.class, () -> toDoService.upsert(toDoSaveRequest, false));
	}

	@Test
	void whenCompleteNotAdmin_thenThrowNoPermissionException() {
		assertThrows(NoPermissionException.class, () -> toDoService.completeToDo(1l, false));
	}
}
