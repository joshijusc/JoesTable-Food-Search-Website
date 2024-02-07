import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/RestaurantSearchServlet")
public class RestaurantSearchServlet extends HttpServlet {
	private static final String ID_USER = "idUser";
	private static final String RESTAURANT_NAME = "restaurantName";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String SORT = "sort";
	private static final String BUSINESSESRAW = "businessesraw";
	private static final String BUSINESSES = "businesses";
	private static final String BUSINESSESJSON = "businessesJson";

	
	public static double latitudeDouble = 34.02116;
	public static double longitudeDouble = -118.287132;
	public String latitudeString = "34.02116";
	public String longitudeString = "-118.287132";
	public static String restaurantName = "Ramen";
	public static String sort = "best_match";
	private static final long serialVersionUID = 1L;

	public RestaurantSearchServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
	    // Create or retrieve the user session
	    HttpSession session = request.getSession(true);

	    if (session != null) {
	        // Get the businesses from the session
	        List<JSONObject> businesses = (List<JSONObject>) session.getAttribute(BUSINESSES);
	        restaurantName = (String) session.getAttribute(RESTAURANT_NAME);


	        if (businesses != null && !businesses.isEmpty()) {
	            // Convert businesses to JSON array
	            JSONArray businessesArray = new JSONArray();
	            for (JSONObject business : businesses) {
	                businessesArray.add(business);
	            }

	            // Set the JSON array as the response content
	            response.setContentType("application/json");
	            response.setCharacterEncoding("UTF-8");
	            
	            // Create a JSON object to hold both restaurant name and businesses
	            JSONObject responseData = new JSONObject();
	            responseData.put(RESTAURANT_NAME, restaurantName);
	            responseData.put(BUSINESSES, businessesArray);

	            // Write the JSON object as the response
	            response.getWriter().write(responseData.toJSONString());
	            
	            // response.getWriter().write(businessesArray.toJSONString());
	        } else {
	            // If no businesses found in the session, return an empty JSON array
	            response.setContentType("application/json");
	            response.setCharacterEncoding("UTF-8");
	            response.getWriter().write("[]");
	        }
	    } else {
	        // If the session is not available, return an empty JSON array
	        response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");
	        response.getWriter().write("[]");
	    }
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// read form fields
		restaurantName = request.getParameter("restaurantName");
		restaurantName = URLEncoder.encode((restaurantName), "UTF-8");
		System.out.println("restaurantName: " + restaurantName);
		latitudeString = request.getParameter("latitude");
		longitudeString = request.getParameter("longitude");
		sort = request.getParameter("sort");
		
		Integer idUser = 0;

		// Create or retrieve the user session
		HttpSession session = request.getSession(true);
		session.setMaxInactiveInterval(30 * 60); // Set session timeout to 30 minutes
		idUser = (Integer) session.getAttribute("idUser");
		
		session.setAttribute(RESTAURANT_NAME, restaurantName); 
		session.setAttribute(LATITUDE, latitudeString); 
		session.setAttribute(LONGITUDE, longitudeString); 
		session.setAttribute(SORT, sort); 
		
	       // Check if the API results are already in the session
        List<JSONObject> businesses = (List<JSONObject>) session.getAttribute(BUSINESSES);

            try {
                businesses = getRestaurantFromYelpAPI(latitudeString, longitudeString, restaurantName, sort);
        
                //sSystem.out.println("businesses: " + businesses);

                // Iterate over businesses to concatenate the display address
                for (JSONObject business : businesses) {
                    JSONObject location = (JSONObject) business.get("location");
                    if (location != null) {
                        Object displayAddressObject = location.get("display_address");

                        if (displayAddressObject != null && displayAddressObject instanceof JSONArray) {
                            JSONArray displayAddress = (JSONArray) displayAddressObject;

                            if (!displayAddress.isEmpty()) {
                                // Concatenate the display address and add it to the business object
                                business.put("display_address", String.join(", ", displayAddress));
                            } else {
                                System.out.println("Display address is empty");
                            }
                        } else {
                            System.out.println("Unexpected or null value for display_address: " + displayAddressObject);
                        }
                    } else {
                        System.out.println("Location information is missing");
                    }
                }

                // Store businesses in the session
                session.setAttribute(BUSINESSES, businesses);

                // Convert businesses to JSON array
                JSONArray businessesArray = new JSONArray();
                businessesArray.addAll(businesses);

                // Set the JSON array as a string in the request attribute
                request.setAttribute("BUSINESSESJSON", businessesArray.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        

        response.sendRedirect("businesses.html");
    }
		
		
	private static List<JSONObject> getRestaurantFromYelpAPI(String latitude, String longitude, String searchTerm,
			String sort) throws ParseException {
		List<JSONObject> apiResults = new ArrayList<>();
		
		yelpAPIKey = ""; // ******************** INSERT API KEY HERE ***********************************
		String apiURL = "https://api.yelp.com/v3/businesses/search?latitude=" + latitude + "&longitude=" + longitude
				+ "&radius=10000" + "&term=" + searchTerm + "&sort_by=" + sort + "&limit=10";

		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiURL)).header("accept", "application/json")
				.header("Authorization",
						"Bearer " + yelpAPIKey)
				.method("GET", HttpRequest.BodyPublishers.noBody()).build();
		HttpResponse<String> response = null;
		try {
			response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

			// Parse JSON response using JSON-Simple
			JSONParser parser = new JSONParser();
			JSONObject jsonResponse = (JSONObject) parser.parse(response.body());
//			System.out.println("jsonResponse: " + jsonResponse.toString());
			JSONArray businesses = (JSONArray) jsonResponse.get("businesses");
//			System.out.println("businesses: " + businesses.toString());

			// Loop through each business and add to the results list
			for (Object businessObj : businesses) {
				JSONObject business = (JSONObject) businessObj;
				apiResults.add(business);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

		return apiResults;
	}

	private static String getCuisineFromJson(JSONArray categoriesArray) {
		StringBuilder cuisineBuilder = new StringBuilder();

		for (int i = 0; i < categoriesArray.size(); i++) {
			JSONObject categoryObject = (JSONObject) categoriesArray.get(i);
			if (categoryObject.containsKey("title")) {
				String title = (String) categoryObject.get("title");
				cuisineBuilder.append(title);
				if (i < categoriesArray.size() - 1) {
					cuisineBuilder.append(", ");
				}
			}
		}

		String cuisine = cuisineBuilder.toString();

		return cuisine;
	}
}
