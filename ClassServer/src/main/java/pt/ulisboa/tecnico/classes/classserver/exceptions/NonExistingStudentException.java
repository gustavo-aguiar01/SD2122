package pt.ulisboa.tecnico.classes.classserver.exceptions;

public class NonExistingStudentException extends Exception {

    public NonExistingStudentException() {}

    public String getMessage() {
        return "Student with given id is not enrolled";
    }
}
