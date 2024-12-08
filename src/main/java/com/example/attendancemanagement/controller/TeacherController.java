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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @GetMapping("/courses/{teacherId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> listCoursesWithIds(@PathVariable Long teacherId) {
        try {
            List<Course> courses = courseRepository.findAllByTeacherId(teacherId);

            if (courses.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No courses found for this teacher.");
            }

            List<Map<String, Object>> courseData = courses.stream().map(course -> {
                Map<String, Object> data = new HashMap<>();
                data.put("courseId", course.getId());
                data.put("courseName", course.getName());
                return data;
            }).toList();

            return ResponseEntity.ok(courseData);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error fetching courses: " + ex.getMessage());
        }
    }



    @PostMapping("/markAttendance")
    @Transactional
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<Map<String, String>> markAttendance(@RequestBody List<Map<String, Object>> attendanceDataList) {
        Map<String, String> response = new HashMap<>();

        for (Map<String, Object> attendanceData : attendanceDataList) {
            Long teacherId = Long.valueOf((Integer) attendanceData.get("teacherId"));
            Long courseId = Long.valueOf((Integer) attendanceData.get("courseId"));
            Long studentId = Long.valueOf((Integer) attendanceData.get("studentId"));
            String status = (String) attendanceData.get("status");

            // Perform validations and save attendance for each entry
            Course course = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("Invalid course ID."));
            if (!course.getTeacherId().equals(teacherId)) {
                response.put("message", "You are not authorized to mark attendance for this course.");
                return ResponseEntity.status(403).body(response);  // 403 Forbidden
            }

            Enrollment enrollment = enrollmentRepository.findByStudent_IdAndCourse_Id(studentId, courseId);
            if (enrollment == null) {
                response.put("message", "Student is not enrolled in this course.");
                return ResponseEntity.status(400).body(response);  // 400 Bad Request
            }

            Optional<Attendance> existingAttendance = attendanceRepository.findByCourseIdAndStudentIdAndDate(courseId, studentId, LocalDate.now());
            if (existingAttendance.isPresent()) {
                response.put("message", "Attendance has already been marked for this student today.");
                return ResponseEntity.status(400).body(response);  // 400 Bad Request
            }

            Attendance attendance = new Attendance();
            attendance.setCourse(course);
            attendance.setStudent(userRepository.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Invalid student ID.")));
            attendance.setDate(LocalDate.now());
            attendance.setStatus(status);

            attendanceRepository.save(attendance);
        }

        response.put("message", "Attendance marked successfully.");
        return ResponseEntity.ok(response);  // 200 OK
    }

    @GetMapping("/courses/{courseId}/students")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> listEnrolledStudents(@PathVariable Long courseId) {
        try {
            List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);

            if (enrollments.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "No students enrolled in this course.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            List<Map<String, Object>> studentData = enrollments.stream().map(enrollment -> {
                Map<String, Object> data = new HashMap<>();
                data.put("studentId", enrollment.getStudent().getId());
                data.put("studentName", enrollment.getStudent().getUsername());
                return data;
            }).toList();

            return ResponseEntity.ok(studentData);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error fetching students: " + ex.getMessage());
        }
    }


    @GetMapping("/courses/{courseId}/studentsNotMarkedToday")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> listStudentsNotMarkedToday(@PathVariable Long courseId) {
        try {
            List<Enrollment> enrollments = enrollmentRepository.findAllByCourseId(courseId);

            if (enrollments.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "No students enrolled in this course.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            List<Map<String, Object>> studentData = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                Map<String, Object> data = new HashMap<>();
                data.put("studentId", enrollment.getStudent().getId());
                data.put("studentName", enrollment.getStudent().getUsername());

                // Check if attendance is already marked for today
                Optional<Attendance> attendance = attendanceRepository.findByCourseIdAndStudentIdAndDate(courseId, enrollment.getStudent().getId(), LocalDate.now());

                if (!attendance.isPresent()) {
                    studentData.add(data); // Add student to list if attendance not marked
                }
            }

            return ResponseEntity.ok(studentData);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error fetching students: " + ex.getMessage());
        }
    }






}