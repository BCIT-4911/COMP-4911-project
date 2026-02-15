package com.corejsf.employee;

public class Employee implements Serializable {

    private static final long serialVersionUID = 11L;

    /** The employee's name. */
    private String name;

    /** The employee's employee number. */
    private int empNumber;




    /**
     * The no-argument constructor. Used to create new employees from within the
     * application.
     */
    public Employee() {
    }

    /**
     * The argument-containing constructor. Used to create the initial employees
     * who have access as well as the administrator.
     *
     * @param empName the name of the employee.
     * @param number the empNumber of the user.
     *
     */
    public Employee(final String empName, final int number) {
        name = empName;
        empNumber = number;
    }


    /**
     * name getter.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * name setter.
     * @param empName the name to set
     */
    public void setName(final String empName) {
        name = empName;
    }

    /**
     * empNumber getter.
     * @return the empNumber
     */
    public int getEmpNumber() {
        return empNumber;
    }

    /**
     * empNumber setter.
     * @param number the empNumber to set
     */
    public void setEmpNumber(final int number) {
        empNumber = number;
    }



    @Override
    public String toString() {
        return name + '\t' + empNumber;
    }



}
