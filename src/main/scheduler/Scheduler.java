package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.model.Reservation;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create Patient Failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create Patient Failed.");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if(currentPatient!=null||currentCaregiver!=null){
            System.out.println("User already logged in, try again.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed.");

            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try{
            patient=new Patient.PatientGetter(username, password).get();
        }
        catch (SQLException e) {
            System.out.println("Login patient failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed.");
        } else {
            // if login was successful, set the current patient to the patient that was just logged in
            System.out.println("Logged in as: " + username);
            currentPatient=patient;
        }

    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        String dateString = tokens[1]; // 输入的日期字符串
        Date sqlDate = Date.valueOf(dateString); // 转换为 java.sql.Date
        //check1: Check if someone's already logged-in
        if(currentCaregiver==null&&currentPatient==null){
          System.out.println("Please login first");
          return;
        }
        else {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            try {

                String availableCaregiver = "SELECT Username FROM Availabilities WHERE Time=? ORDER BY Username";
                String availableVaccine = "SELECT Name,Doses FROM Vaccines WHERE Doses>0";
                PreparedStatement statement1 = con.prepareStatement(availableCaregiver);
                statement1.setDate(1, sqlDate);
                ResultSet resultSet1 = statement1.executeQuery();
                while (resultSet1.next()) {
                    String username = resultSet1.getString("Username");
                    System.out.println(username);
                }
                PreparedStatement statement2 = con.prepareStatement(availableVaccine);

                ResultSet resultSet2 = statement2.executeQuery();
                while (resultSet2.next()) {
                    String name = resultSet2.getString("Name");
                    int doses = resultSet2.getInt("Doses");
                    //Output
                    System.out.print(name+" "+doses);

                }


            } catch (SQLException e) {
                System.out.println("Please try again");
            } finally {
                cm.closeConnection();
            }
        }

    }

    private static void reserve(String[] tokens) {
        String dateString = tokens[1]; // The input date
        String vaccineName=tokens[2];// The input vaccine name
        Date sqlDate = Date.valueOf(dateString); // turn into java.sql.Date
        // check1: check if someone's already logged-in

        if (currentCaregiver==null&&currentPatient==null){
        System.out.println("Please login first");
        return;

        }

        //check 2: check if the current logged-in user is a caregiver
      else if(currentPatient==null&&currentCaregiver!=null){
        System.out.println("Please login as a patient");
        return;
      }
      else if(currentPatient!=null&&currentCaregiver==null){
          ConnectionManager cm=new ConnectionManager();
          Connection con=cm.createConnection();
          String availableCaregiver="SELECT Username FROM Availabilities WHERE Time=? ORDER BY Username";
          String availableVaccine="SELECT Name,Doses FROM Vaccines WHERE Name=?";
          String lastReservation="SELECT MAX(appointment_id) AS max_id FROM Reservations ";

         try {
              PreparedStatement statement1 = con.prepareStatement(availableCaregiver);
              statement1.setDate(1, sqlDate);
              ResultSet resultset1= statement1.executeQuery();
              PreparedStatement statement2 = con.prepareStatement(availableVaccine);
              statement2.setString(1,vaccineName);
              ResultSet resultset2= statement2.executeQuery();
              PreparedStatement statement3 = con.prepareStatement(lastReservation);
              ResultSet resultset3= statement3.executeQuery();
              if(!resultset1.next()){
                  System.out.println("No caregiver is available");
                  return;
              }
              if(!resultset2.next()){
                  System.out.println("Not enough available doses");
                  return;
              }

              int newAppointmentId;
              if(!resultset3.next()){
                  newAppointmentId=1;
              }
              else{
                  int lastAppointmentId=resultset3.getInt("max_id");
                  newAppointmentId=lastAppointmentId+1;
              }
             String caregiverName=resultset1.getString("Username");
              //create a new reservation and save it to the database
              Reservation reservation = new Reservation.ReservationBuilder(caregiverName,currentPatient.getUsername(),vaccineName,newAppointmentId,sqlDate).build();
              reservation.saveToDB();
              Caregiver.deleteAvailability(caregiverName,sqlDate);

                 int vaccineDoses=resultset2.getInt("Doses");

             Vaccine vaccine=new Vaccine.VaccineBuilder(vaccineName,vaccineDoses).build();
             vaccine.decreaseAvailableDoses(1);
              System.out.println("Appointment ID "+newAppointmentId+", "+"Caregiver username "+caregiverName);
        }
          catch(SQLException e){
              System.out.println("Please try again");
          }
          finally {
              cm.closeConnection();
          }
      }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            String caregiverName = currentCaregiver.getUsername(); // caregiver_name
            currentCaregiver.uploadAvailability(caregiverName,d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // cancel <appointment_id>
        //check 1: check if someone's already logged-in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        int appointment_id = Integer.parseInt(tokens[1]);
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String find = "SELECT caregiver_name, patient_name, Time, vaccines_name FROM Reservations WHERE appointment_id=?";
        String delete = "DELETE FROM Reservations WHERE appointment_id=?";
        String searchVaccine="SELECT Doses FROM Vaccines WHERE Name=?";

        try {
            PreparedStatement statement1 = con.prepareStatement(find);
            statement1.setInt(1, appointment_id);
            ResultSet resultset1 = statement1.executeQuery();
            PreparedStatement statement2 = con.prepareStatement(delete);
            statement2.setInt(1, appointment_id);

            if (resultset1.next()) {
                String patientName = resultset1.getString("patient_name");
                String caregiverName = resultset1.getString("caregiver_name");
                Date appointmentTime = resultset1.getDate("Time");
                String vaccineName = resultset1.getString("vaccines_name");
                PreparedStatement statement3 = con.prepareStatement(searchVaccine);
                statement3.setString(1,vaccineName);
                ResultSet resultset3 = statement3.executeQuery();
                int vaccineDoses=0;
                if (resultset3.next()) {
                    vaccineDoses=resultset3.getInt("Doses");
                }
                if (currentPatient != null) {
                    //check 2: check if the current logged-in user is the patient in reservation
                    if (currentPatient.getUsername().equals(patientName)) {
                        statement2.executeUpdate();
                        Caregiver.uploadAvailability(caregiverName, appointmentTime);
                         Vaccine vaccine=new Vaccine.VaccineBuilder(vaccineName,vaccineDoses).build();
                         vaccine.increaseAvailableDoses(1);
                        System.out.println("Reservation canceled");
                    } else {
                        System.out.println("You are not the patient of this reservation");
                    }
                } else if (currentCaregiver != null) {
                    //check 3: check if the current logged-in user is the caregiver in reservation
                    if (currentCaregiver.getUsername().equals(caregiverName)) {
                        statement2.executeUpdate();
                        Caregiver.uploadAvailability(caregiverName, appointmentTime);
                        System.out.println("Reservation canceled");
                    } else {
                        System.out.println("You are not authorized to cancel this reservation");
                    }
                }
            } else {
                System.out.println("Reservation not found");
            }

        } catch (SQLException e) {
            System.out.println("Error occurred when canceling");
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
       String searchForCaregiver="SELECT * FROM Reservations WHERE caregiver_name=?";
       String searchForPatient="SELECT * FROM Reservations WHERE patient_name=?";
       // check 1: check if someone's already logged-in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        //If the current login user is a caregiver, output the specific information
        else if (currentCaregiver != null && currentPatient == null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            try {
                PreparedStatement statement1 = con.prepareStatement(searchForCaregiver);
                statement1.setString(1, currentCaregiver.getUsername());
                ResultSet resultset1= statement1.executeQuery();
                while (resultset1.next()){
                    String patientName=resultset1.getString("patient_name");
                    Date date=resultset1.getDate("Time");
                    int appointmentId=resultset1.getInt("appointment_id");
                    String vaccinesName=resultset1.getString("vaccines_name");
                    System.out.println(appointmentId+" "+vaccinesName+" "+date+" "+patientName);
                }


            }
            catch (SQLException e) {
                System.out.println("Please try again");
                e.printStackTrace();
                System.out.println("SQL Error Code: " + e.getErrorCode());
                System.out.println("SQL State: " + e.getSQLState());
                System.out.println("Error Message: " + e.getMessage());
            }
            finally {
                cm.closeConnection();
            }
        }

        //If the current login user is a patient, output the specific information
        else if(currentPatient!=null && currentCaregiver==null) {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();
            try {
                PreparedStatement statement2 = con.prepareStatement(searchForPatient);
                statement2.setString(1, currentPatient.getUsername());
                ResultSet resultset2= statement2.executeQuery();
                while (resultset2.next()){
                    String patientName=resultset2.getString("caregiver_name");
                    Date date=resultset2.getDate("Time");
                    int appointmentId=resultset2.getInt("appointment_id");
                    String vaccinesName=resultset2.getString("vaccines_name");
                    System.out.println(appointmentId+" "+vaccinesName+" "+date+" "+patientName);
                }
            }
            catch (SQLException e) {
                System.out.println("Please try again");
            }
            finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        try {
            // check 1: check if someone's already logged-in
            if (currentPatient == null && currentCaregiver == null) {
                System.out.println("Please login first");
            }
            //if a patient logged in
            else if (currentPatient != null && currentCaregiver == null) {
                currentPatient = null;
                System.out.println("Successfully Logged out");
            }
            //if a caregiver logged in
            else if (currentCaregiver != null && currentPatient == null) {
                currentCaregiver = null;
                System.out.println("Successfully Logged out");
            }
        }
        catch(Exception e){
            System.out.println("Please try again");
        }


    }
}
