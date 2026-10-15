package com.ghc.model;
public class Student {
 public int id; public String studentNo,firstName,lastName,gender,birthDate,email,phone,address,yearLevel,program,status;
 public Student(int id,String studentNo,String firstName,String lastName,String gender,String birthDate,String email,String phone,String address,String yearLevel,String program,String status){this.id=id;this.studentNo=studentNo;this.firstName=firstName;this.lastName=lastName;this.gender=gender;this.birthDate=birthDate;this.email=email;this.phone=phone;this.address=address;this.yearLevel=yearLevel;this.program=program;this.status=status;}
 public String toString(){return studentNo+" — "+lastName+", "+firstName;}
}
