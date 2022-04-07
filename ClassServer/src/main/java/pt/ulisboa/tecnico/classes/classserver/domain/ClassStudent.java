package pt.ulisboa.tecnico.classes.classserver.domain;

public class ClassStudent {

    private String id;
    private String name;

    public ClassStudent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Id getter
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * Name getter
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Id setter
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Name setter
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Check if the given id is a valid student ID
     *  - format : alunoXXXX where X are positive integers
     * @param id
     * @return booolean
     */
    public static boolean isValidStudentId(String id) {
        return id.matches("^aluno\\d{4}$");
    }

    /**
     * Check if the given name is a valid name
     *  - format : a string with length between 3 and 30
     * @param name
     * @return
     */
    public static boolean isValidStudentName(String name) {
        return name.matches("^.{3,30}$");
    }

}
