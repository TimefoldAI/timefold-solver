package ai.timefold.solver.examples.curriculumcourse.score;

import static ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ONE_HARD;
import static ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofHard;
import static ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore.ofSoft;
import static ai.timefold.solver.core.api.score.stream.ConstraintCollectors.countDistinct;
import static ai.timefold.solver.core.api.score.stream.Joiners.equal;
import static ai.timefold.solver.core.api.score.stream.Joiners.filtering;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.examples.curriculumcourse.domain.Curriculum;
import ai.timefold.solver.examples.curriculumcourse.domain.Lecture;
import ai.timefold.solver.examples.curriculumcourse.domain.UnavailablePeriodPenalty;
import ai.timefold.solver.examples.curriculumcourse.domain.solver.CourseConflict;

public class CurriculumCourseConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                conflictingLecturesDifferentCourseInSamePeriod(factory),
                conflictingLecturesSameCourseInSamePeriod(factory),
                roomOccupancy(factory),
                unavailablePeriodPenalty(factory),
                roomCapacity(factory),
                minimumWorkingDays(factory),
                curriculumCompactness(factory),
                roomStability(factory)
        };
    }

    // ************************************************************************
    // Hard constraints
    // ************************************************************************

    Constraint conflictingLecturesDifferentCourseInSamePeriod(ConstraintFactory factory) {
        return factory.forEach(CourseConflict.class)
                .join(Lecture.class,
                        equal(CourseConflict::getLeftCourse, Lecture::getCourse))
                .join(Lecture.class,
                        equal((courseConflict, lecture1) -> courseConflict.getRightCourse(), Lecture::getCourse),
                        equal((courseConflict, lecture1) -> lecture1.getPeriod(), Lecture::getPeriod))
                .filter(((courseConflict, lecture1, lecture2) -> lecture1 != lecture2))
                .penalize(ONE_HARD,
                        (courseConflict, lecture1, lecture2) -> courseConflict.getConflictCount())
                .asConstraint("conflictingLecturesDifferentCourseInSamePeriod");
    }

    Constraint conflictingLecturesSameCourseInSamePeriod(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lecture.class,
                equal(Lecture::getPeriod),
                equal(Lecture::getCourse))
                .penalize(ONE_HARD,
                        (lecture1, lecture2) -> 1 + lecture1.getCurriculumSet().size())
                .asConstraint("conflictingLecturesSameCourseInSamePeriod");
    }

    Constraint roomOccupancy(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lecture.class,
                equal(Lecture::getRoom),
                equal(Lecture::getPeriod))
                .penalize(ONE_HARD)
                .asConstraint("roomOccupancy");
    }

    Constraint unavailablePeriodPenalty(ConstraintFactory factory) {
        return factory.forEach(UnavailablePeriodPenalty.class)
                .join(Lecture.class,
                        equal(UnavailablePeriodPenalty::getCourse, Lecture::getCourse),
                        equal(UnavailablePeriodPenalty::getPeriod, Lecture::getPeriod))
                .penalize(ofHard(10))
                .asConstraint("unavailablePeriodPenalty");
    }

    // ************************************************************************
    // Soft constraints
    // ************************************************************************

    Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lecture.class)
                .filter(lecture -> lecture.getStudentSize() > lecture.getRoom().getCapacity())
                .penalize(ofSoft(1),
                        lecture -> lecture.getStudentSize() - lecture.getRoom().getCapacity())
                .asConstraint("roomCapacity");
    }

    Constraint minimumWorkingDays(ConstraintFactory factory) {
        return factory.forEach(Lecture.class)
                .groupBy(Lecture::getCourse, countDistinct(Lecture::getDay))
                .filter((course, dayCount) -> course.getMinWorkingDaySize() > dayCount)
                .penalize(ofSoft(5),
                        (course, dayCount) -> course.getMinWorkingDaySize() - dayCount)
                .asConstraint("minimumWorkingDays");
    }

    Constraint curriculumCompactness(ConstraintFactory factory) {
        return factory.forEach(Curriculum.class)
                .join(Lecture.class,
                        filtering((curriculum, lecture) -> lecture.getCurriculumSet().contains(curriculum)))
                .ifNotExists(Lecture.class,
                        equal((curriculum, lecture) -> lecture.getDay(), Lecture::getDay),
                        equal((curriculum, lecture) -> lecture.getTimeslotIndex(), lecture -> lecture.getTimeslotIndex() + 1),
                        filtering((curriculum, lectureA, lectureB) -> lectureB.getCurriculumSet().contains(curriculum)))
                .ifNotExists(Lecture.class,
                        equal((curriculum, lecture) -> lecture.getDay(), Lecture::getDay),
                        equal((curriculum, lecture) -> lecture.getTimeslotIndex(), lecture -> lecture.getTimeslotIndex() - 1),
                        filtering((curriculum, lectureA, lectureB) -> lectureB.getCurriculumSet().contains(curriculum)))
                .penalize(ofSoft(2))
                .asConstraint("curriculumCompactness");
    }

    Constraint roomStability(ConstraintFactory factory) {
        return factory.forEach(Lecture.class)
                .groupBy(Lecture::getCourse, countDistinct(Lecture::getRoom))
                .filter((course, roomCount) -> roomCount > 1)
                .penalize(HardSoftScore.ONE_SOFT,
                        (course, roomCount) -> roomCount - 1)
                .asConstraint("roomStability");
    }

}
