package com.orm.test;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class Main {

    public static void main(String[] args) {
        try {
            final String rootDirectory = System.getProperty("user.dir");
            StringBuilder jsonString = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(rootDirectory+"/src/main/java/com/orm/config/conf.json"));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            JSONObject jsonConfig = new JSONObject(jsonString.toString());
            // Extract connection details from JSON
            String jdbcDriver = jsonConfig.getString("driver");
            String jdbcUrl = jsonConfig.getString("connection_url");
            String username = jsonConfig.getString("username");
            String password = jsonConfig.getString("password");

            // Step 2: Load the MySQL JDBC driver
            Class.forName(jdbcDriver);
            Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
            String sqlQuery = "SELECT * FROM Students";
            PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next())
            {
                int id = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                int age = resultSet.getInt("age");
                String grade = resultSet.getString("grade");
                System.out.println("ID: " + id);
                System.out.println("First Name: " + firstName);
                System.out.println("Last Name: " + lastName);
                System.out.println("Age: " + age);
                System.out.println("Grade: " + grade);
                System.out.println("------------------------");
            }
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            // Print column names and types
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                String columnType = metaData.getColumnTypeName(i);
                int columnSize = metaData.getColumnDisplaySize(i);
                System.out.println("Column " + i + ":");
                System.out.println("   Name: " + columnName);
                System.out.println("   Type: " + columnType);
                System.out.println("   Size: " + columnSize);
                System.out.println("------------------------");
            }
            resultSet.close();
            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}