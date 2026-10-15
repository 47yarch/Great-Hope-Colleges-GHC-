package com.ghc.model;
public class Course { public int id,units; public String code,name,description; public Course(int id,String code,String name,int units,String description){this.id=id;this.code=code;this.name=name;this.units=units;this.description=description;} public String toString(){return code+" — "+name;} }
