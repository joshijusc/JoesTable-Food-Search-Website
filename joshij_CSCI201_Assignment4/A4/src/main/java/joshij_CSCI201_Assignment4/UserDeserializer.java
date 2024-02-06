package joshij_CSCI201_Assignment4;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class UserDeserializer implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, java.lang.reflect.Type typeOfT, com.google.gson.JsonDeserializationContext context)
            throws JsonParseException {
//    	 User userInstance = new User();

         // Implement the logic to populate userInstance from the JSON representation
//         JsonObject jsonObject = json.getAsJsonObject();

        return null; //userInstance;
    }
}
