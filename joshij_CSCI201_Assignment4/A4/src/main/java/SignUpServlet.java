import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.catalina.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

@WebServlet("/SignUpServlet")
public class SignUpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ID_USER = "idUser";

    public SignUpServlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Jason Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmpassword = request.getParameter("confirmpassword");
        String email = request.getParameter("email");
        String tos = request.getParameter("TOS");				// Checked = "on"; unchecked = null

        
        
		// If inputs entered then check if key items already in use
		int inputsValid = 0;
		try {
			inputsValid = validateRegistrationInputs(username, password, email, tos);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		
		
		Gson gson = new Gson();
		PrintWriter print2user = response.getWriter();
		
		// Display to user results of validity check
		if (inputsValid == 1) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			String error = "Username is already in use";
			print2user.write(gson.toJson(error));
			print2user.flush();
		}
		else if (inputsValid == 2) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			String error = "Email is already in use or malformed.";
			print2user.write(gson.toJson(error));
			print2user.flush();
		}
		else if (inputsValid == 3) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			String error = "Username and Email are already in use";
			print2user.write(gson.toJson(error));
			print2user.flush();
		}
		else if (inputsValid == 4) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			String error = "User info is missing";
			print2user.write(gson.toJson(error));
			print2user.flush();
		}
		else {
			response.setStatus(HttpServletResponse.SC_OK);
			
			
			Boolean userAdded = addUser2Database(username, password, email);
			if (userAdded) {
//				print2user.println("User added successfully.");
//				print2user.write(gson.toJson("User added successfully."));
//		        // Build HTML code to be sent to user
//		        String htmlRespone = "<html>";
//		        htmlRespone += "<h2>"; 
//		        htmlRespone += "Your email is: " + email + "<br/>";      
//		        htmlRespone += "Your username is: " + username + "<br/>";      
//		        htmlRespone += "Your password is: " + password + "<br/>";
//		        htmlRespone += "Confirm Password: " + confirmpassword + "<br/>";      
//		        htmlRespone += "TOS: " + tos + "<br/>";      
//		        htmlRespone += "</h2>";
//
//		        htmlRespone += "</html>";
//		         
//		        // Show response to user web page 
//		        print2user.println(htmlRespone);
//		        print2user.flush();
		        
				// Create or retrieve the user session
				HttpSession session = request.getSession(true);
				session.setMaxInactiveInterval(30 * 60); // Set session timeout to 30 minutes
				
				Connection conn = connectToDatabase(username, password);


				// Authenticate user and set session attribute
				Integer idUser = authenticateUser(conn, username, password);

				// Close the database
				if (conn != null) {
					try {
						conn.close();
//						System.out.println("Database connection closed.");
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

				if (idUser > 0) {
					session.setAttribute(ID_USER, idUser); // Set idUser in the session attribute

					// Create a JSON object with the authentication status
					JsonObject jsonResponse = new JsonObject();
					jsonResponse.addProperty("idUser", idUser);

					// Set response type to JSON
					response.setContentType("application/json");

					response.sendRedirect("home.html");
				} else {
					response.sendRedirect("login.html");
				}
			} else {
				print2user.println("User not added.");
				print2user.write(gson.toJson("User not added."));	
			}
	        // Close the writer
	        print2user.close();
	        

		}
        
	}
	
	protected int validateRegistrationInputs(String username, String password, String email, String tos) throws ClassNotFoundException, SQLException{
        int inValid = 0;
        String sql = "";

        // Error check user inputs
 		if (username == null || username.isBlank()
 				|| password == null || password.isBlank()
 				|| email == null || email.isBlank()
 				|| tos == null || tos.isBlank()) {
 			inValid = 4;
 		} else {
	        // Open the database
			Connection conn = connectToDatabase(username, password);
	        
	        // Check if user name exists
	    	sql = "SELECT Count(iduser) as found FROM reservation.users WHERE username = ?";
	    	
	        try {
	            try (PreparedStatement statement = conn.prepareStatement(sql)) {
	                statement.setString(1, username);
	                try (ResultSet resultSet = statement.executeQuery()) {
	                    if (resultSet.next()) {
	                        if (resultSet.getInt("found") == 1) {
	                                inValid += 1;
	                        }
	                    }
	                System.out.println("Username found: " + inValid);
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        
	        
	        // Check email existence
	    	sql = "SELECT Count(iduser) as found FROM reservation.users WHERE email = ?";
	    	
	        try {
	            try (PreparedStatement statement = conn.prepareStatement(sql)) {
	                statement.setString(1, email);
	                try (ResultSet resultSet = statement.executeQuery()) {
	                    if (resultSet.next()) {
	                        if (resultSet.getInt("found") > 0 || !email.contains("@") || email.split("@").length < 2) {
	                        	// TODO use regex to validate email
	                        	inValid += 2;
	                        }
	                    }
	                System.out.println("Email found: " + inValid);
	                }
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	        
	        
	        if (conn != null) {
	            try {
	                conn.close();
	                System.out.println("Database connection closed.");
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	        }       
        }
		return inValid;
	}


	protected Boolean addUser2Database(String username, String password, String email) {
		Boolean userAdded = false;
		String sql = "";
        
        // Open the database
		Connection conn = connectToDatabase(username, password);
        
        // Check if user name exists
    	sql = "INSERT IGNORE INTO `reservation`.`users` (`username`, `password`, `email`) VALUES (?, ?, ?)";

    	System.out.println("User name: " + username);
		System.out.println("Password: " + password);
		System.out.println("Email: " + email);

	    // If all checks pass, proceed with the insertion
	    try (PreparedStatement statement = conn.prepareStatement(sql)) {
	        statement.setString(1, username);
	        statement.setString(2, password);
	        statement.setString(3, email);

	        int rowsInserted = statement.executeUpdate();

	        if (rowsInserted > 0) {
	        	userAdded = true;
	            System.out.println("User inserted successfully!");
	        } else {
	        	userAdded = false;
	        	System.out.println("User not inserted successfully!");
	        }
	        
    	} catch (SQLException e) {
    	    e.printStackTrace();
    	}


    	
    	
        
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }       

    	
		return userAdded;
	}
	

    public Connection connectToDatabase(String username, String password) {
    	Connection conn = null;
    	try {
            // Explicitly load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish the database connection
            String DB_URL = "jdbc:mysql://localhost:3306/reservation";
            String DB_USER = "root";
            String DB_PASSWORD = "root";

            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            if (conn != null) {
                System.out.println("Connected to the database.");
            } 
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

	public int authenticateUser(Connection conn, String username, String password) {
		int idUser = 0;

		// Error check user inputs
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			idUser = 0;
		} else {
			String sql = "SELECT iduser FROM reservation.users WHERE username = ? AND password = ?";
			// TODO account for null if not found
			// TODO check individually username in correct password incorrect

			try {
				try (PreparedStatement statement = conn.prepareStatement(sql)) {
					statement.setString(1, username);
					statement.setString(2, password);
					try (ResultSet resultSet = statement.executeQuery()) {
						if (resultSet.next()) {
//	                    	System.out.println("before if: " + resultSet.getInt("iduser"));
							if (resultSet.getInt("iduser") > 0) {
								idUser = resultSet.getInt("iduser");
							} else {
								idUser = 0;
//	        	                System.out.println("In authenticate User method: " + idUser);
							}
						}
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return idUser;
	}

    
	// Check if the email is in use

    

	void tests() {
		/*
		username	password	email					Confirm password	
		blank		blank		blank					blank
		exists
								exists
								3
								3@
								3@
								to do regex validity 
														TODO match
		*/
	}


}
