package com.example.attendancemanagement.repository;


import com.example.attendancemanagement.entity.Attendance;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentId(Long studentId);
    List<Attendance> findByCourseId(Long courseId);
    @Transactional
    void deleteByCourseId(Long courseId);

    Optional<Attendance> findByCourseIdAndStudentIdAndDate(Long courseId, Long studentId, LocalDate date);

    Collection<Object> findByCourseIdAndDate(Long courseId, LocalDate today);
    List<Long> findStudentIdsByCourseIdAndDate(Long courseId, LocalDate today);

    List<Attendance> findAllByCourseIdIn(List<Long> courseIds);
}
