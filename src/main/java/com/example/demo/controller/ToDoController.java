package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.naming.NoPermissionException;
import javax.validation.Valid;

import com.example.demo.dto.ToDoResponse;
import com.example.demo.dto.ToDoSaveRequest;
import com.example.demo.exception.ToDoNotFoundException;
import com.example.demo.service.ToDoService;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class ToDoController {

	@Autowired
	ToDoService toDoService;
	
	@ExceptionHandler({ ToDoNotFoundException.class, NoPermissionException.class })
	public String handleException(Exception ex) {
		return ex.getMessage();
	}
	
	@GetMapping("/todos")
	@Valid List<ToDoResponse> getAll() {
		return toDoService.getAll();
	}

	@PostMapping("/todos")
	@Valid ToDoResponse save(@Valid @RequestBody ToDoSaveRequest todoSaveRequest, @RequestParam Boolean isAdmin)
			throws ToDoNotFoundException, NoPermissionException {
		return toDoService.upsert(todoSaveRequest, isAdmin);
	}

	@PutMapping("/todos/{id}/complete")
	@Valid ToDoResponse save(@PathVariable Long id, @RequestParam Boolean isAdmin)
			throws ToDoNotFoundException, NoPermissionException {
		return toDoService.completeToDo(id, isAdmin);
	}

	@GetMapping("/todos/{id}")
	@Valid ToDoResponse getOne(@PathVariable Long id) throws ToDoNotFoundException {
		return toDoService.getOne(id);
	}

	@DeleteMapping("/todos/{id}")
	void delete(@PathVariable Long id, @RequestParam Boolean isAdmin) throws NoPermissionException {
		toDoService.deleteOne(id, isAdmin);
	}

}