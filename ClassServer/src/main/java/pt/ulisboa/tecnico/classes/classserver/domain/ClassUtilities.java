package pt.ulisboa.tecnico.classes.classserver.domain;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ClassUtilities {

    public static List<Student> classStudentsToGrpc(Collection<ClassStudent> students) {
        return students.stream().map(ClassUtilities::classStudentToGrpc).collect(Collectors.toList());
    }

    public static Student classStudentToGrpc(ClassStudent student) {
        return Student.newBuilder().setStudentName(student.getName()).setStudentId(student.getId()).build();
    }

    public static List<ClassStudent> studentsToDomain(List<Student> students) {
        return students.stream().map(ClassUtilities::studentToDomain).collect(Collectors.toList());
    }

    public static ClassStudent studentToDomain(Student student) {
        return new ClassStudent(student.getStudentId(), student.getStudentName());
    }

}
