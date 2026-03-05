package com.corejsf.DTO;

public class OptionDTO {

    private int id;
    private String name;

    public OptionDTO() { }

    public OptionDTO(final int id, final String name) {
        this.id = id;
        this.name = name;
    }

    // getters/setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
