package com.example.attendancemanagement.repository;

import com.example.attendancemanagement.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Enrollment findByStudent_IdAndCourse_Id(Long studentId, Long courseId);
    @Transactional
    void deleteByCourseId(Long courseId);

    // Corrected method to fetch enrollments by student ID
    List<Enrollment> findByStudent_Id(Long studentId);

    //@Transactional // Ensure transactional consistency
    default void deleteByStudentId(Long studentId) {
        List<Enrollment> enrollments = findByStudent_Id(studentId); // Corrected query method
        deleteAll(enrollments); // Delete all fetched enrollments
    }

    List<Enrollment> findAllByCourseId(Long courseId);

    List<Enrollment> findByCourse_Id(Long courseId);
}
