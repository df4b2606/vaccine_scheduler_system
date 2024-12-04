package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.util.Arrays;

public class Reservation {
    private final int appointment_id;
    private final String vaccines_name;
    private final String patient_name;
    private final String caregiver_name;
    private final Date Time;


    private Reservation(ReservationBuilder builder) {
        this.appointment_id = builder.appointment_id;
        this.vaccines_name = builder.vaccines_name;
        this.patient_name = builder.patient_name;
        this.caregiver_name = builder.caregiver_name;
        this.Time = builder.Time;
    }

    private Reservation(ReservationGetter getter) {
        this.appointment_id = getter.appointment_id;
        this.vaccines_name = getter.vaccines_name;
        this.patient_name = getter.patient_name;
        this.caregiver_name = getter.caregiver_name;
        this.Time = getter.Time;
    }

    // Getters
    public String getVaccinename() {
        return vaccines_name;
    }
    public String getCaregivername() {
        return caregiver_name;
    }
    public String getPatientname() {return patient_name;}
    public Date getTime() {return Time;}
    public int getAppointment_id() {return appointment_id;}


    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Reservations VALUES (? , ? , ? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setInt(1, this.appointment_id);
            statement.setString(2, this.vaccines_name);
            statement.setString(3, this.patient_name);
            statement.setString(4, this.caregiver_name);
            statement.setDate(5, this.Time);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }


    public static class ReservationBuilder {
        private final int appointment_id;
        private final String vaccines_name;
        private final String patient_name;
        private final String caregiver_name;
        private final Date Time;

        public ReservationBuilder(String c_name, String p_name, String v_name, int a_id, Date d) {
            this.appointment_id = a_id;
            this.vaccines_name = v_name;
            this.patient_name = p_name;
            this.caregiver_name = c_name;
            this.Time = d;
        }

        public Reservation build() {
            return new Reservation(this);
        }
    }
    public static class ReservationGetter {
        private final int appointment_id;
        private String vaccines_name;
        private String patient_name;
        private String caregiver_name;
        private Date Time;

        // Constructor
        public ReservationGetter(int appointment_id) {
            this.appointment_id = appointment_id;
        }

        // Get specific information from the database
        public Reservation get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getReservation = "SELECT appointment_id, vaccines_name, patient_name, caregiver_name, Time " +
                    "FROM Reservations WHERE appointment_id = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getReservation);
                statement.setInt(1, this.appointment_id);
                ResultSet resultSet = statement.executeQuery();


                if (resultSet.next()) {
                    int a_id = resultSet.getInt("appointment_id");
                    String v_name = resultSet.getString("vaccines_name");
                    String p_name = resultSet.getString("patient_name");
                    String c_name = resultSet.getString("caregiver_name");
                    Date time = resultSet.getDate("Time");
                    return new Reservation.ReservationBuilder(c_name, p_name, v_name, a_id, time).build();
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException("Error retrieving reservation with ID " + appointment_id, e);
            } finally {
                cm.closeConnection();
            }
        }
    }

}