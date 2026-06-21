package org.uniplan.domain

import org.springframework.data.jpa.repository.JpaRepository

interface UniversityRepository : JpaRepository<University, Long>
interface InstituteRepository : JpaRepository<Institute, Long>
interface DepartmentRepository : JpaRepository<Department, Long>
interface TermRepository : JpaRepository<Term, Long>
interface BlockRepository : JpaRepository<Block, Long>
interface DatePatternRepository : JpaRepository<DatePattern, Long>
interface CourseRepository : JpaRepository<Course, Long>
interface CourseOfferingRepository : JpaRepository<CourseOffering, Long>
interface GroupRepository : JpaRepository<Group, Long>
interface SectionRepository : JpaRepository<Section, Long>
interface LocationRepository : JpaRepository<Location, Long>
interface InstructorRepository : JpaRepository<Instructor, Long>
interface InstructorAvailabilityRepository : JpaRepository<InstructorAvailability, Long>
interface DisciplineAssignmentRepository : JpaRepository<DisciplineAssignment, Long>
interface RotationAssignmentRepository : JpaRepository<RotationAssignment, Long>
interface ScheduleVersionRepository : JpaRepository<ScheduleVersion, Long>
interface AssignmentRepository : JpaRepository<Assignment, Long>
