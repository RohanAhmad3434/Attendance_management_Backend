package com.example.attendancemanagement.controller;

import com.example.attendancemanagement.entity.Attendance;
import com.example.attendancemanagement.entity.Course;
import com.example.attendancemanagement.entity.Enrollment;
import com.example.attendancemanagement.entity.User;
import com.example.attendancemanagement.repository.AttendanceRepository;
import com.example.attendancemanagement.repository.CourseRepository;
import com.example.attendancemanagement.repository.EnrollmentRepository;
import com.example.attendancemanagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @PostMapping("/users")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        // Check if all required fields are provided
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return ResponseEntity.badRequest().body("Username, password, and role are required.");
        }

        // Check if the username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username is already taken.");
        }

        try {
            // Save the user
            User savedUser = userRepository.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving the user.");
        }
    }

    @DeleteMapping("/users/{id}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @Transactional  // Ensure this method is executed within a transaction context
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (!user.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found."));
        }

        User.Role role = user.get().getRole();

        // Handle deletion based on user role
        if (role == User.Role.Student) {
            // Remove all enrollments for the student
            enrollmentRepository.deleteByStudentId(id);  // Ensure this is also within a transaction
        } else if (role == User.Role.Teacher) {
            // Unassign teacher from all courses
            List<Course> courses = courseRepository.findByTeacherId(id);
            for (Course course : courses) {
                course.setTeacherId(null);
                courseRepository.save(course);
            }
        }

        // Delete the user
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    @PutMapping("/users/{id}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        // Check if the user exists
        Optional<User> existingUser = userRepository.findById(id);
        if (!existingUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // Validate required fields
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return ResponseEntity.badRequest().body("Username, password, and role are required.");
        }

        // Check for unique username
        Optional<User> userWithSameUsername = userRepository.findByUsername(user.getUsername());
        if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username is already taken.");
        }

        // Update the user details
        User updatedUser = existingUser.get();
        updatedUser.setUsername(user.getUsername());
        updatedUser.setPassword(user.getPassword());
        updatedUser.setRole(user.getRole());

        try {
            User savedUser = userRepository.save(updatedUser);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating the user.");
        }
    }


    @GetMapping("/users")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public List<User> viewUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/courses")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> createCourse(@RequestBody Map<String, String> courseDetails) {
        String courseName = courseDetails.get("name");
        String teacherName = courseDetails.get("teacherName");

        // Validate course name
        if (courseName == null || courseName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Course name is required.");
        }

        // Check if the course name already exists
        if (courseRepository.findByName(courseName).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Course name is already taken.");
        }

        Long teacherId = null;
        if (teacherName != null && !teacherName.trim().isEmpty()) {
            // Validate teacher name
            Optional<User> optionalTeacher = userRepository.findByUsername(teacherName);
            if (!optionalTeacher.isPresent()) {
                return ResponseEntity.badRequest().body("Teacher with the given name does not exist.");
            }

            User teacher = optionalTeacher.get();
            if (teacher.getRole() != User.Role.Teacher) {
                return ResponseEntity.badRequest().body("The specified user is not a teacher.");
            }

            teacherId = teacher.getId();
        }

        // Create and save the course
        Course course = new Course();
        course.setName(courseName);
        course.setTeacherId(teacherId);

        try {
            Course newCourse = courseRepository.save(course);
            return ResponseEntity.status(HttpStatus.CREATED).body(newCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating the course.");
        }
    }



    @DeleteMapping("/courses/{id}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        Optional<Course> course = courseRepository.findById(id);
        if (!course.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }

        // Remove all enrollments for the course
        enrollmentRepository.deleteByCourseId(id);

        // Delete the course
        courseRepository.deleteById(id);
        return ResponseEntity.ok("Course deleted successfully.");
    }

    @PutMapping("/courses/{id}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Map<String, String> courseDetails) {
        String courseName = courseDetails.get("name");
        String teacherName = courseDetails.get("teacherName");

        // Check if the course exists
        Optional<Course> optionalCourse = courseRepository.findById(id);
        if (!optionalCourse.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }

        // Fetch the existing course
        Course existingCourse = optionalCourse.get();

        // Update course name
        if (courseName != null && !courseName.trim().isEmpty()) {
            existingCourse.setName(courseName);
        }

        // Validate and set the teacher ID
        if (teacherName != null && !teacherName.trim().isEmpty()) {
            Optional<User> optionalTeacher = userRepository.findByUsername(teacherName);
            if (!optionalTeacher.isPresent()) {
                return ResponseEntity.badRequest().body("Teacher with the given name does not exist.");
            }

            User teacher = optionalTeacher.get();
            if (teacher.getRole() != User.Role.Teacher) {
                return ResponseEntity.badRequest().body("The specified user is not a teacher.");
            }

            existingCourse.setTeacherId(teacher.getId());
        } else {
            existingCourse.setTeacherId(null); // Allow unassigning the teacher
        }

        // Save and return the updated course
        try {
            Course updatedCourse = courseRepository.save(existingCourse);
            return ResponseEntity.ok(updatedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating the course.");
        }
    }



    // Enroll student in course
    @PostMapping("/enroll")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<Map<String, Object>> enrollStudentInCourse(@RequestBody Map<String, Long> enrollmentData) {
        Long studentId = enrollmentData.get("studentId");
        Long courseId = enrollmentData.get("courseId");

        Map<String, Object> response = new HashMap<>();

        // Check if student and course exist
        if (!userRepository.existsById(studentId) || !courseRepository.existsById(courseId)) {
            response.put("success", false);
            response.put("message", "Invalid student or course ID.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            // Retrieve the student from the database
            User student = userRepository.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Student not found."));

            // Validate that the user is a Student
            if (student.getRole() != User.Role.Student) {
                response.put("success", false);
                response.put("message", "The provided ID does not belong to a Student.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Retrieve the course from the database
            Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Course not found."));

            // Create and save new enrollment
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(course);

            enrollmentRepository.save(enrollment);

            response.put("success", true);
            response.put("message", "Student successfully enrolled.");
            response.put("enrollment", Map.of(
                    "student", Map.of("id", student.getId(), "username", student.getUsername()),
                    "course", Map.of("id", course.getId(), "name", course.getName())
            ));
            return ResponseEntity.ok(response);
        } catch (DataIntegrityViolationException e) {
            response.put("success", false);
            response.put("message", "Student is already enrolled in the course.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }


    @GetMapping("/courses_database")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public List<Course> viewCourseTable() {
        return courseRepository.findAll();

    }



    @GetMapping("/courses")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public List<Map<String, Object>> viewCourses() {
        List<Map<String, Object>> coursesWithTeachers = new ArrayList<>();

        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            Map<String, Object> courseData = new HashMap<>();
            courseData.put("id", course.getId());
            courseData.put("name", course.getName());

            if (course.getTeacherId() != null) {
                Optional<User> optionalTeacher = userRepository.findById(course.getTeacherId());
                courseData.put("teacherName", optionalTeacher.map(User::getUsername).orElse("None"));
            } else {
                courseData.put("teacherName", "None");
            }

            coursesWithTeachers.add(courseData);
        }

        return coursesWithTeachers;
    }





    // Assign Teacher to a Course
    @PutMapping("/assign-teacher/{courseId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> assignTeacherToCourse(@PathVariable Long courseId, @RequestBody Long teacherId) {
        // Validate if the course exists
        Optional<Course> optionalCourse = courseRepository.findById(courseId);
        if (!optionalCourse.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found.");
        }

        // Validate if the teacher exists and has the role 'Teacher'
        Optional<User> optionalTeacher = userRepository.findById(teacherId);
        if (!optionalTeacher.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found.");
        }

        User teacher = optionalTeacher.get();
        if (teacher.getRole() != User.Role.Teacher) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The provided user is not a Teacher.");
        }

        // Assign teacher to course
        Course course = optionalCourse.get();
        course.setTeacherId(teacherId);

        // Save the updated course
        courseRepository.save(course);

        return ResponseEntity.ok("Teacher assigned to course successfully.");
    }


    // View all enrollments for students
    @GetMapping("/enrollments")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public List<Enrollment> getEnrollments() {
        return enrollmentRepository.findAll();
    }




    // Fetch teacher details based on teacherId
    @GetMapping("/teachers/{teacherId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> getTeacherById(@PathVariable Long teacherId) {
        Optional<User> teacherOptional = userRepository.findById(teacherId);

        if (teacherOptional.isPresent()) {
            User teacher = teacherOptional.get();
            if (teacher.getRole() == User.Role.Teacher) {
                // Return teacher details
                Map<String, Object> teacherDetails = new HashMap<>();
                teacherDetails.put("id", teacher.getId());
                teacherDetails.put("username", teacher.getUsername());
                teacherDetails.put("role", teacher.getRole().toString());
                return ResponseEntity.ok(teacherDetails);
            } else {
                // User exists but is not a teacher
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The user is not a teacher.");
            }
        } else {
            // Teacher not found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Teacher not found.");
        }
    }



    @GetMapping("/attendanceGroupedByDate")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> getAttendanceGroupedByDate() {
        try {
            // Fetch all attendance records

            List<Attendance> attendanceRecords = attendanceRepository.findAll();

            if (attendanceRecords.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No attendance records found.");
            }

            // Group attendance records by date
            Map<LocalDate, Map<String, Object>> groupedByDate = attendanceRecords.stream()
                    .collect(Collectors.groupingBy(
                            Attendance::getDate,
                            Collectors.collectingAndThen(Collectors.toList(), recordsByDate -> {
                                // Further group by course name
                                Map<String, Object> groupedByCourse = new HashMap<>();

                                Map<String, List<Map<String, String>>> courses = recordsByDate.stream()
                                        .collect(Collectors.groupingBy(
                                                record -> record.getCourse().getName(),
                                                Collectors.mapping(record -> {
                                                    Map<String, String> attendanceDetails = new HashMap<>();
                                                    attendanceDetails.put("studentName", record.getStudent().getUsername());
                                                    Long teacherId = record.getCourse().getTeacherId();
                                                    String teacherName = teacherId != null ? userRepository.findById(teacherId).map(User::getUsername).orElse("Unknown") : "None";
                                                    attendanceDetails.put("teacherName", teacherName);
                                                    attendanceDetails.put("status", record.getStatus());
                                                    return attendanceDetails;
                                                }, Collectors.toList())
                                        ));

                                // Count total present and absent for the date
                                long totalPresent = recordsByDate.stream()
                                        .filter(record -> "Present".equalsIgnoreCase(record.getStatus()))
                                        .count();
                                long totalAbsent = recordsByDate.stream()
                                        .filter(record -> "Absent".equalsIgnoreCase(record.getStatus()))
                                        .count();

                                groupedByCourse.put("courses", courses);
                                groupedByCourse.put("totalPresent", totalPresent);
                                groupedByCourse.put("totalAbsent", totalAbsent);

                                return groupedByCourse;
                            })
                    ));

            return ResponseEntity.ok(groupedByDate);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error fetching grouped attendance: " + ex.getMessage());
        }
    }



}
