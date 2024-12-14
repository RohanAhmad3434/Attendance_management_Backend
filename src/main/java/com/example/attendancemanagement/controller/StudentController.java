package com.example.attendancemanagement.controller;

import com.example.attendancemanagement.entity.Attendance;
import com.example.attendancemanagement.entity.Course;
import com.example.attendancemanagement.entity.User;
import com.example.attendancemanagement.repository.AttendanceRepository;
import com.example.attendancemanagement.repository.CourseRepository;
import com.example.attendancemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;


    @GetMapping("/attendance/{studentId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> viewAttendance(@PathVariable Long studentId) {
        // Validate user existence and role
        User user = userRepository.findById(studentId).orElse(null);
        if (user == null || user.getRole() != User.Role.Student) {
            return ResponseEntity.badRequest().body("Invalid student ID or user is not a student.");
        }

        // Retrieve attendance records
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentId(studentId);
        if (attendanceRecords.isEmpty()) {
            return ResponseEntity.ok("No attendance records found for this student.");
        }

        return ResponseEntity.ok(attendanceRecords);
    }


    @GetMapping("/checkRunningAttendance/{studentId}/{courseId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> checkRunningAttendance(@PathVariable Long studentId, @PathVariable Long courseId) {
        // Validate student existence and role
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getRole() != User.Role.Student) {
            return ResponseEntity.badRequest().body("Invalid student ID or user is not a student.");
        }

        // Validate course existence
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.badRequest().body("Invalid course ID.");
        }

        // Fetch attendance records for the student in the course
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentId(studentId)
                .stream()
                .filter(attendance -> attendance.getCourse().getId().equals(courseId))
                .toList();

        // Calculate running attendance
        long totalConducted = attendanceRecords.size(); // Total attendance days conducted
        long daysPresent = attendanceRecords.stream()
                .filter(attendance -> "Present".equalsIgnoreCase(attendance.getStatus()))
                .count(); // Total days the student was present

        if (totalConducted == 0) {
            return ResponseEntity.ok("No attendance conducted for this course yet.");
        }

        double runningAttendance = ((double) daysPresent / totalConducted) * 100;

        // Return the response
        return ResponseEntity.ok(
                String.format("Running attendance for student: %-20s | Course: %-20s | Attendance: %-6.2f%%",
                        student.getUsername(), course.getName(), runningAttendance)
//                String.format("Running attendance for student    :   %s   in course   :   %s :        %.2f%%", student.getUsername(), course.getName(), runningAttendance)
        );
    }


    @GetMapping("/attendanceGroupedByDate/{studentId}")
    @CrossOrigin(origins = "http://127.0.0.1:5500")
    public ResponseEntity<?> getAttendanceGroupedByDate(@PathVariable Long studentId) {
        // Validate student existence and role
        User student = userRepository.findById(studentId).orElse(null);
        if (student == null || student.getRole() != User.Role.Student) {
            return ResponseEntity.badRequest().body("Invalid student ID or user is not a student.");
        }

        // Fetch all attendance records for the student
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentId(studentId);
        if (attendanceRecords.isEmpty()) {
            return ResponseEntity.ok("No attendance records found for this student.");
        }

        // Group attendance records by date and map the data to include teacher name and course name
        Map<String, List<Map<String, Object>>> groupedByDate = attendanceRecords.stream()
                .collect(Collectors.groupingBy(
                        attendance -> attendance.getDate().toString(),
                        Collectors.mapping(attendance -> {
                            Map<String, Object> record = new HashMap<>();
                            record.put("courseName", attendance.getCourse().getName());
                            record.put("teacherName", getTeacherName(attendance.getCourse().getTeacherId()));
                           // record.put("date", attendance.getDate().toString());
                            record.put("status", attendance.getStatus());
                            return record;
                        }, Collectors.toList())
                ));

        // Sort the grouped records by date in descending order
        Map<String, List<Map<String, Object>>> sortedGroupedByDate = new TreeMap<>(Collections.reverseOrder());
        sortedGroupedByDate.putAll(groupedByDate);

        // Return the grouped records
        return ResponseEntity.ok(sortedGroupedByDate);
    }

    // Helper method to get the teacher's name from the teacherId
    private String getTeacherName(Long teacherId) {
        User teacher = userRepository.findById(teacherId).orElse(null);
        if (teacher != null && teacher.getRole() == User.Role.Teacher) {
            return teacher.getUsername();  // Assuming teacher's name is stored in the 'username' field
        }
        return "Unknown Teacher";  // Default if no teacher is found
    }

}
