package com.orm.manager;

import com.orm.annotations.Column;
import com.orm.annotations.PrimaryKey;
import com.orm.annotations.Table;
import com.orm.exceptions.DataException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {

    private Connection connection;
    private PreparedStatement writePreparedStatement;
    private PreparedStatement readPreparedStatement;
    private Map<String, String> configMap;
    private static final DataManager INSTANCE = new DataManager();
    public void init() {}

    public DataManager() {
        try {
            final String rootDirectory = System.getProperty("user.dir");
            final StringBuilder jsonString = new StringBuilder();
            final BufferedReader reader = new BufferedReader(new FileReader(rootDirectory+"/src/main/java/com/orm/config/conf.json"));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();
            final JSONObject jsonConfig = new JSONObject(jsonString.toString());
            configMap = new HashMap<>();
            configMap.put("jdbcDriver", jsonConfig.getString("driver"));
            configMap.put("jdbcUrl", jsonConfig.getString("connection_url"));
            configMap.put("username", jsonConfig.getString("username"));
            configMap.put("password", jsonConfig.getString("password"));
        } catch (Exception e) {}
    }

    public void begin() throws DataException {
        try {
            Class.forName(configMap.get("jdbcDriver"));
            this.connection = DriverManager.getConnection(configMap.get("jdbcUrl"), configMap.get("username"), configMap.get("password"));
        } catch (Exception exception) {
           throw new DataException(exception.getMessage());
        }
    }

    public int save(Object entity) throws DataException {
        if (entity == null) {
            throw new DataException("Entity must not be null");
        }
        Class<?> associatedClass = entity.getClass();
        if (!associatedClass.isAnnotationPresent(Table.class)) {
            throw new DataException("Entity class must be annotated with @Table");
        }
        if (connection == null) {
            throw new DataException("Transaction has not begun");
        }
        final Table tableAnnotation = associatedClass.getAnnotation(Table.class);
        final String insertQuery = insertQueryForSave(associatedClass, tableAnnotation.value());
        try {
            int parameterIndex = 1;
            Object primaryKeyValue = null;
            writePreparedStatement = connection.prepareStatement(insertQuery);
            Field[] declaredFields = associatedClass.getDeclaredFields();
            for (Field field : declaredFields) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    writePreparedStatement.setObject(parameterIndex, field.get(entity));
                    parameterIndex += 1;
                }
                if (field.isAnnotationPresent(PrimaryKey.class)) {
                    primaryKeyValue = field.get(entity);
                }
            }
            if (isPrimaryKeyExists(associatedClass, tableAnnotation, primaryKeyValue)) {
                throw new DataException(String.format("Primary Key %s exists", primaryKeyValue));
            }
            int rowsAffected = writePreparedStatement.executeUpdate();
            return rowsAffected;
        } catch (Exception e) {
            throw new DataException(e.getMessage());
        }
    }

    public <T> List<T> fetch(Class<T> type) throws DataException {
        if (connection == null) {
            throw new DataException("Transaction has not begun");
        }
        List<T> results = new ArrayList<>();
        try {
            if (type.isAnnotationPresent(Table.class)) {
                String tableName = type.getAnnotation(Table.class).value();
                String selectStatement = String.format("select * from %s", tableName);
                if (readPreparedStatement == null) {
                    readPreparedStatement = connection.prepareStatement(selectStatement);
                    ResultSet resultSet = readPreparedStatement.executeQuery();
                    Map<String, Method> columnToSetterMap = new HashMap<>();
                    for (Field field : type.getDeclaredFields()) {
                        if (field.isAnnotationPresent(Column.class)) {
                            String setterMethodUppercaseName = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                            Method setterMethod = type.getMethod("set" + setterMethodUppercaseName, field.getType());
                            columnToSetterMap.put(field.getAnnotation(Column.class).name(), setterMethod);
                        }
                    }
                    while (resultSet.next()) {
                        final T newInstance = type.getDeclaredConstructor().newInstance();
                        for (Map.Entry<String, Method> entry : columnToSetterMap.entrySet()) {
                            Object column = resultSet.getObject(entry.getKey());
                            entry.getValue().invoke(newInstance, column);
                        }
                        results.add(newInstance);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new DataException(e.getMessage());
        }
    }

    private boolean isPrimaryKeyExists(Class<?> associatedClass, Table tableAnnotation, Object primaryKeyValue) throws DataException {
        try {
            String selectByPrimaryKeyQuery = selectQueryByPrimaryKey(associatedClass, tableAnnotation.value());
            readPreparedStatement = connection.prepareStatement(selectByPrimaryKeyQuery);
            readPreparedStatement.setObject(1, primaryKeyValue);
            ResultSet resultSet = readPreparedStatement.executeQuery();
            return resultSet.next();
        } catch (Exception e) {
            throw new DataException(e.getMessage());
        }
    }
    private String selectQueryByPrimaryKey(Class<?> associatedClass, String tableName) {
        Field[] declaredFields = associatedClass.getDeclaredFields();
        String primaryKeyColumn = null;
        for (Field field : declaredFields) {
            if (field.isAnnotationPresent(Column.class) && field.isAnnotationPresent(PrimaryKey.class)) {
                field.setAccessible(true);
                primaryKeyColumn = field.getAnnotation(Column.class).name();
                break;
            }
        }
        String selectQuery = null;
        if (primaryKeyColumn != null) {
            selectQuery = String.format("select * from %s where %s=?", tableName, primaryKeyColumn);
        }
        return selectQuery;
    }
    private String insertQueryForSave(Class<?> associatedClass, String tableName) {
        String nameString = getNameString(associatedClass);
        String questionMarkString = getQuestionMarkString(associatedClass);
        String statement = "insert into " + tableName+"("+nameString.substring(0, nameString.length() - 1)+")" + " values("+questionMarkString.substring(0, questionMarkString.length() - 1)+")";
        return statement;
    }

    private String getQuestionMarkString(Class<?> associatedClass) {
        StringBuilder questionMarksStringBuilder = new StringBuilder();
        Field[] fields = associatedClass.getDeclaredFields();
        for (final Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Column.class)) {
                questionMarksStringBuilder.append("?,");
            }
        }
        return questionMarksStringBuilder.toString();
    }

    private String getNameString(final Class<?> associatedClass) {
        StringBuilder nameStringBuilder = new StringBuilder();
        Field[] fields = associatedClass.getDeclaredFields();
        for (final Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(Column.class)) {
                String columnName = field.getAnnotation(Column.class).name();
                nameStringBuilder.append(columnName + ",");
            }
        }
        return nameStringBuilder.toString();
    }

    public void end() throws DataException {
        try {
            if (connection != null) {
                connection.close();;
            }
            if (writePreparedStatement != null) {
                writePreparedStatement.close();
            }
            if (readPreparedStatement != null) {
                readPreparedStatement.close();
            }
            configMap = null;
        } catch (Exception e) {
            throw new DataException(e.getMessage());
        }
    }

    public static DataManager getDataManager() {
        return INSTANCE;
    }
}
