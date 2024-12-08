package com.example.attendancemanagement.repository;

import com.example.attendancemanagement.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacherId(Long teacherId); // Find all courses assigned to a teacher

    Optional<Course> findByName(String name);

    List<Course> findAllByTeacherId(Long teacherId);
}
