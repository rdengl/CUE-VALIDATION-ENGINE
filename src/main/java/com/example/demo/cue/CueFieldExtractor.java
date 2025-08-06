package com.example.demo.cue;
import java.util.*;

public class CueFieldExtractor {

    public static Map<String, String> extractCueFieldsWithoutRegex(String cueInput) {
        Map<String, String> fieldMap = new LinkedHashMap<>();

        // Step 1: Trim to get content inside Request { ... }
        int start = cueInput.indexOf('{');
        int end = cueInput.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) return fieldMap;

        String content = cueInput.substring(start + 1, end).trim();

        int i = 0;
        int length = content.length();
        while (i < length) {
            // Skip whitespace
            while (i < length && Character.isWhitespace(content.charAt(i))) i++;

            // Extract key
            StringBuilder keyBuilder = new StringBuilder();
            while (i < length && content.charAt(i) != ':' && content.charAt(i) != '\n') {
                keyBuilder.append(content.charAt(i++));
            }

            String key = keyBuilder.toString().trim();
            if (i >= length || content.charAt(i) != ':') break;
            i++; // skip ':'

            // Extract value
            StringBuilder valueBuilder = new StringBuilder();
            int brace = 0, bracket = 0;
            boolean inString = false;

            while (i < length) {
                char c = content.charAt(i);

                if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                    inString = !inString;
                }

                if (!inString) {
                    if (c == '{') brace++;
                    if (c == '}') brace--;
                    if (c == '[') bracket++;
                    if (c == ']') bracket--;
                }

                // Break when next key is likely starting (on new line and outside of block/string)
                if (!inString && brace == 0 && bracket == 0 && c == '\n') {
                    // Peek ahead to see if next line starts with a key
                    int temp = i + 1;
                    while (temp < length && Character.isWhitespace(content.charAt(temp))) temp++;
                    if (temp < length && Character.isLetter(content.charAt(temp))) {
                        break;
                    }
                }

                valueBuilder.append(c);
                i++;
            }

            if (!key.isEmpty()) {
                fieldMap.put(key, valueBuilder.toString().trim());
            }
        }

        return fieldMap;
    }

    // Test
    public static void main(String[] args) {
        String cueSchema = """
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

        Map<String, String> result = extractCueFieldsWithoutRegex(cueSchema);
        result.forEach((k, v) -> System.out.println(k + " -> " + v));
    }
}
