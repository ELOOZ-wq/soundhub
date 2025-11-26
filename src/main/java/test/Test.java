package test;

import utils.DBConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Test {
    public static void main(String[] args) {
        try (Connection con = DBConnection.getConnection()) {
            if (con != null && !con.isClosed()) {
                System.out.println("Connexion à la DB réussie !");
            }
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
            e.printStackTrace();
        }

    }
}
