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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ID_USER = "idUser";

	public LoginServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		HttpSession session = request.getSession(true);
		session.setMaxInactiveInterval(30 * 60); // Set session timeout to 30 minutes
		Integer idUser = (Integer) session.getAttribute("idUser");

		if ("true".equals(request.getParameter("logout"))) {
			session.invalidate();
			response.sendRedirect("home.html");
			return;
		}

		JsonObject jsonResponse = new JsonObject();
		jsonResponse.addProperty("idUser", idUser);

		String username = getUsernameById(idUser);
		jsonResponse.addProperty("username", username);

		response.setContentType("application/json");
		try (PrintWriter out = response.getWriter()) {
			out.print(jsonResponse.toString());
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// read login page form fields
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		if (username == null || username.isBlank() || password == null || password.isBlank()) {
			response.sendRedirect("login.html");
		}
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
//				System.out.println("Database connection closed.");
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
			// Handles invalid login
	        // Set content type
	        response.setContentType("text/html");

	        // Get the PrintWriter
	        PrintWriter out = response.getWriter();

	        // Generate HTML with JavaScript
	        out.println("<html>");
	        out.println("<head>");
	        out.println("<title>Login Alert</title>");
	        out.println("<script>");
	        out.println("alert('Invalid username or password');");
	        out.println("</script>");
	        out.println("</head>");
	        out.println("<body>");
	        out.println("<h1>Invalid username or password</h1>");
	        out.println("</body>");
	        out.println("</html>");
			
			
//			response.sendRedirect("login.html");
		}
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
//			if (conn != null) {
//				System.out.println("Connected to the database.");
//			}
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

	public String getUsernameById(Integer idUser) {
		String sql = "";
		String username = null;

		Connection conn = connectToDatabase("none", "none");

		sql = "SELECT `username` FROM `reservation`.`users` WHERE `iduser` = ?";
		if (idUser != null && idUser > 0) {
			try (PreparedStatement statement = conn.prepareStatement(sql)) {
				statement.setInt(1, idUser);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						username = resultSet.getString("username");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			username = "none";
		}
		return username;
	}

	void tests() {
		/*
		 * username password blank blank exists exists
		 * 
		 */
	}

}
