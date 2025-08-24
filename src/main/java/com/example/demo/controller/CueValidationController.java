package com.example.demo.controller;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.CueValidationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;



@RestController
@RequestMapping("/validate-api")
public class CueValidationController {

	@PostMapping("/validate")
	public Map<String,String> validate(@RequestBody CueValidationRequest request) {


		String schema4 = "Request: {\r\n"
				+ "  name:  string & !=\"\" @message(\"Name must not be empty\") @success(\"Name is valid\")\r\n"
				+ "  email: string & =~\"^.+@.+\\\\..+$\" @message(\"Invalid email format\") @success(\"Email is valid\")\r\n"
				+ "  age:   int & >=18 & <=60 @message(\"Age must be between 18 and 60\") @success(\"Age is valid\")\r\n"
				+ "}\r\n"
				+ "";

		String json4 = "{\r\n"
				+ "  \"Request\": {\r\n"
				+ "    \"name\": \"John Doe\",\r\n"
				+ "     \"email\": \"john.doe@example.com\",\r\n"
				+ "	 \"age\": 3\r\n"
				+ "  }\r\n"
				+ "}\r\n"
				+ "";



		String schema5 = "Request: {\r\n"
				+ "    name: string & !=\"\" @message(\"Name must not be empty\")\r\n"
				+ "\r\n"
				+ "    age: int & >=18 & <=60 @message(\"Age must be between 18 and 60\")\r\n"
				+ "  email: string & =~\"^.+@.+\\\\..+$\"   @message(\"Invalid email format\")\r\n"
				+ "    dob: string & =~\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\" @message(\"DOB must be in YYYY-MM-DD format\")\r\n"
				+ "\r\n"
				+ "    isActive?: bool\r\n"
				+ "}";

		String json5 = "{\r\n"
				+ "  \"Request\": {\r\n"
				+ "    \"name\": \"\",\r\n"
				+ "    \"age\": 19,\r\n"
				+ "    \"email\": \"ramgmail.com\",\r\n"
				+ "    \"dob\": \"200-06-20\",\r\n"
				+ "    \"isActive\": true\r\n"
				+ "  }\r\n"
				+ "}\r\n"
				+ "";



	// all valid cases added

		String schema8 = """
				Request: {
				      age: int & >= 18 & <= 60 @tag(message="Age must be between 18 and 60")
				      name: string & !="" @tag(message="Name must not be empty")
				      salary: int @tag(message="Salary must be an integer and not null")
				      dobFormat1: string & =~"^\\\\d{4}-\\\\d{2}-\\\\d{2} \\\\d{2}:\\\\d{2}:\\\\d{2}$" @tag(message="Date must be in yyyy-MM-dd HH:mm:ss format", date="yyyy-MM-dd HH:mm:ss")
				      dobFormat2: string & =~"^\\\\d{4}-\\\\d{2}-\\\\d{2}$" @tag(message="Date must be in yyyy-MM-dd format", date="yyyy-MM-dd")
				      dobFormat3: string & =~"^\\\\d{2}-\\\\d{2}-\\\\d{4}$" @tag(message="Date must be in MM-dd-yyyy format", date="MM-dd-yyyy")
				       parentLevelDecimal: number & >=0.00 & <=9999999999.99 @tag(message="Invalid amount", decimal="2")
				      children: [...{
				        id: string & !="" @tag(message="Child ID must not be empty")
				        address: string & !="" @tag(message="Child address must not be empty")
				        status: "active" @tag(message="Status must be 'active'")
				    	status_or_operator :"active" | "inactive" | "pending" @tag(message="Constant validation for Status must be 'active' | 'inactive' 'pending'")
				    	childLevelDecimal: number & >=0.00 & <=9999999999.99 @tag(message="Invalid amount", decimal="2")
				      }]
				    }
				""";
		String json8 = """
				{
				   "Request": {
				     "age": 30,
				     "name": "Alice",
				     "salary": 50000,
				     "dobFormat1": "2006-01-02 15:04:05",
				 	"dobFormat2": "1990-05-12",
				 	"dobFormat3": "01-02-2006",
				 	"parentLevelDecimal": 77.89,
				     "children": [
				       {
				         "id": "001",
				         "address": "123 Street",
				         "status": "active",
				 		"status_or_operator": "pending",
				 		"childLevelDecimal":76.90
				       }
				     ]
				   }
				 }
				""";



		System.out.println("json: "+request.getJson());
		System.out.println("Schema: "+request.getSchema());

		CueValidatorLoader.CueValidatorLibrary instance = CueValidatorLoader.getInstance();
		String result = instance.ValidateJSONWithCue(request.getSchema(),request.getJson());


		System.out.println(result);
		// Convert JSON array string to List<String>
		Map<String, String> errorMap = null;
		try {
			errorMap = new ObjectMapper().readValue(result, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Display each error
		System.out.println("Map"+ errorMap);
		return errorMap;
	}
}
