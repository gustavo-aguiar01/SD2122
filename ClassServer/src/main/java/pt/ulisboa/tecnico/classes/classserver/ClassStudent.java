package pt.ulisboa.tecnico.classes.classserver;

public class ClassStudent {

    private String id;
    private String name;

    public ClassStudent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static boolean isValidStudentId(String id) {
        return id.matches("^aluno\\d{4}$");
    }

    public static boolean isValidStudentName(String name) {
        return name.matches("^.{3,30}$");
    }

}
