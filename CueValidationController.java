package com.example.demo.controller;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dao.CueValidationRequest;

@RestController
@RequestMapping("/validate-api")
public class CueValidationController {

    @PostMapping("/validate")
    public List<String> validate(@RequestBody CueValidationRequest request) {
    	// db call to get schema
    	String schema1 ="package Request\r\n"
    			+ "#Request: {\r\n"
    			+ "    name:  string\r\n"
    			+ "    age:   int & >=18 & <=60\r\n"
    			+ "    email: string & =~\"^.+@.+\\\\..+$\"\r\n"
    			+ "}\r\n"
    			+ "#Request\r\n";
    	
    	
    	// take project request json
    	String json1 ="{\r\n"
    			+ "  \"name\": 1,\r\n"
    			+ "  \"age\": 388,\r\n"
    			+ "  \"email\": \"ram@example.com\"\r\n"
    			+ "}\r\n"
    			;
    	
    	
    	
    	 String json = "{\r\n"
    	 		+ "    		          \"Request\": {\r\n"
    	 		+ "    		            \"name\": {\r\n"
    	 		+ "    		              \"first\": \"\",\r\n"
    	 		+ "    		              \"last\": \"Doe\"\r\n"
    	 		+ "    		            },\r\n"
    	 		+ "    		            \"age\": 10,\r\n"
    	 		+ "    		            \"email\": \"bademail\"\r\n"
    	 		+ "    		          }\r\n"
    	 		+ "    		        }";

    		        String schema = "Request: {\r\n"
    		        		+ "    		          name: {\r\n"
    		        		+ "    		            first: string & !=\"\"\r\n"
    		        		+ "    		            last: string\r\n"
    		        		+ "    		          }\r\n"
    		        		+ "    		          age:   int & >=18 & <=60\r\n"
    		        		+ "    		          email: string & =~\"^.+@.+\\\\..+$\"\r\n"
    		        		+ "    		        }";
    	
        // Call Go DLL
    		        
    		        
    		        String json2 = """
    		                {
    		                  "Request": {
    		                    "name": "",
    		                    "age": 1,
    		                    "email": "ramgmail.com"
    		                  }
    		                }""";

    		                String schema2 = """
    		                package validation
    		                Request: {
    		                    name: string
    		                    age: int 
    		                    email: string
    		                    errors: [...string]
    		                    errors: [
    		                        if name == "" {
    		                            "name: Name must not be empty"
    		                        }
    		                        ,
    		                        if !(age >= 18 && age <= 60) {
    		                            "Age: Age must be between 18 and 60"
    		                        },
    		                        if !(email =~ "^.+@.+\\\\..+$") {
    		                            "email: Invalid email format"
    		                        },
    		                    ]
    		                }""";
    		        
    		                
    		               
    		                
    		                //-------------------------
    		                
    		                
    		                
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
    		                        
    		                        
    		                        String schema6 = "Request: {\r\n"
    		                        		+ "  user: {\r\n"
    		                        		+ "    name: string & !=\"\" @message(\"Name must not be empty\")\r\n"
    		                        		+ "    age:  int & >=18 & <=60 @message(\"Age must be between 18 and 60\")\r\n"
    		                        		+ "  }\r\n"
    		                        		+ "}";
    		                        
    		                        String json6 = "{\r\n"
    		                        		+ "  \"Request\": {\r\n"
    		                        		+ "    \"user\": {\r\n"
    		                        		+ "      \"name\": \"\",\r\n"
    		                        		+ "      \"age\": 70\r\n"
    		                        		+ "    }\r\n"
    		                        		+ "  }\r\n"
    		                        		+ "}\r\n"
    		                        		+ "";
    		                        

    		                		
        String result = CueValidatorLibrary.INSTANCE.ValidateJSONWithCue(json5,schema5);
       // String result = CueValidatorLibrary.INSTANCE.ValidateJSON(schema5,json5);
        
        System.out.println(result);
     // Convert JSON array string to List<String>
        JSONArray arr = new JSONArray(result);
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            errors.add(arr.getString(i));
        }

        // Display each error
        for (String err : errors) {
            System.out.println(err);
        }
        return errors;
    }
}
