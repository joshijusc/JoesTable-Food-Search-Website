package BusinessData;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import org.json.JSONObject;
import org.json.simple.JSONArray;

public class BusinessData extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public BusinessData() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		 // Retrieve businesses data from the session or database
        List<JSONObject> businesses = (List<JSONObject>) request.getSession().getAttribute("businesses");
        
        // Convert businesses to JSON array
        JSONArray businessesArray = new JSONArray();
        for (JSONObject business : businesses) {
            businessesArray.add(business);
        }

        // Set the content type and write JSON to the response
        response.setContentType("application/json");
        response.getWriter().write(businessesArray.toString());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
