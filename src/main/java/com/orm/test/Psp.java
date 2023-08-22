package com.orm.test;

import com.orm.exceptions.DataException;
import com.orm.manager.DataManager;

import java.util.List;

public class Psp {
    public static void main(String[] gg) throws DataException {
        DataManager dataManager = DataManager.getDataManager();
        dataManager.begin();

        List<Student> students = dataManager.fetch(Student.class);
        for (Student student : students) {
            System.out.println(String.format("Student rollNo %s , name %s", student.getRollNumber(), student.getName()));
        }
        dataManager.end();
    }
}
