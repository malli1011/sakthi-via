package com.practice.sakthi_via.controller;

import com.practice.sakthi_via.exception.ResourceNotFoundException;
import com.practice.sakthi_via.model.Employee;
import com.practice.sakthi_via.repository.EmployeeRepository;
import com.practice.sakthi_via.service.EmployeeService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.contains;

@Controller
@RequestMapping("/api/v1")
@Api("Employee Management System")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeService employeeService;

    @ApiOperation("Retrieve all the employees")
    @GetMapping("/employees")
    public ResponseEntity<List> getEmployee() {
        List<Employee> employeeList = employeeRepository.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(employeeList);
    }

    @ApiOperation("Create Employee")
    @PostMapping("/employees")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Employee created successfully"),
            @ApiResponse(code = 403, message = "Username not available")
    })
    public ResponseEntity<Employee> createEmployee(
            @ApiParam(value = "Employee details", required = true) @RequestBody Employee employee) {
        if(employeeService.checkUsername(employee.getUsername()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(employee);
        employeeRepository.save(employee);
        Employee newEmployee = employeeRepository.findById(employee.getId()).get();
        return ResponseEntity.status(HttpStatus.OK).body(newEmployee);
    }

    @ApiOperation("Get Employee details by Id")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Successfully retrieved employee details"),
            @ApiResponse(code = 404, message = "Employee Id not found")
    })
    @GetMapping(value = "/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(
            @ApiParam(value = "Id to retrieve Employee Details", required = true) @PathVariable(value = "id") Long id)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee ID " + id + " not found"));
        return ResponseEntity.status(HttpStatus.OK).body(employee);
    }

    @ApiOperation("Delete Employee by Id")
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<String> deleteEmployeeById(
            @ApiParam(value = "Id to delete Employee details", required = true) @PathVariable(value = "id") Long id)
            throws ResourceNotFoundException {
        Employee employee = employeeRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee ID " + id + " not found"));
        employeeRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.OK).body("Success");
    }

    @ApiOperation("Get Employee Details By Email")
    @GetMapping("/employeesByEmail/{email}")
    public ResponseEntity<List> getEmployeeByEmail(
            @ApiParam(value = "Email to retrieve Employee Details", required = true)
            @PathVariable(value = "email") String email)
            throws ResourceNotFoundException {
        List<Employee> employeeList = employeeRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Email " + email + " not found"));
        return ResponseEntity.status(HttpStatus.OK).body(employeeList);
    }

    @ApiOperation("Get Employee Details either by Email or Username")
    @GetMapping("/employeesByUsernameOrEmail")
    public ResponseEntity<List> getEmployeeByUsernameOrEmail(
            @ApiParam(value = "Email to retrieve Employee Details", required = false) @RequestParam String username,
            @ApiParam(value = "Username to retrieve Employee Details", required = false) @RequestParam String email) {
        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setEmail(email);
        ExampleMatcher exampleMatcher = ExampleMatcher.matchingAny()
                .withMatcher("email", contains().ignoreCase())
                .withMatcher("username", contains().ignoreCase());
        Example<Employee> example = Example.of(employee, exampleMatcher);

        List<Employee> employeeList = employeeRepository.findAll(example);
        return ResponseEntity.status(HttpStatus.OK).body(employeeList);
    }

    @ApiOperation(value = "Update Employee Details")
    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(
            @ApiParam(value = "User Id to update employee object", required = true) @PathVariable(value = "id") Long id,
            @ApiParam(value = "Update employee object", required = true) @Valid @RequestBody Employee employeeDetails)
            throws ResourceNotFoundException {
        Employee updatedEmployee = employeeRepository.findById(id)
                .map(employee -> {
                    if (employeeDetails.getEmail() != null)
                        employee.setEmail(employeeDetails.getEmail());
                    if(employeeDetails.getAge() != 0)
                        employee.setAge(employeeDetails.getAge());
                    if(employeeDetails.getName() != null)
                        employee.setName(employeeDetails.getName());
                    if(employeeDetails.getUsername() != null)
                        employee.setUsername(employeeDetails.getUsername());
                    return employeeRepository.save(employee);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Employee ID " + id + " not found"));
        return ResponseEntity.status(HttpStatus.OK).body(updatedEmployee);
    }

}
