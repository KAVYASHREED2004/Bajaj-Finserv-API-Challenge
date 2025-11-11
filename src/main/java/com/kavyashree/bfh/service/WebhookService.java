package com.kavyashree.bfh.service;

import com.kavyashree.bfh.dto.GenerateWebhookRequest;
import com.kavyashree.bfh.dto.GenerateWebhookResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WebhookService {

    private final RestTemplate rest = new RestTemplate();

    public void executeWorkflow() {
        try {
            String genUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            GenerateWebhookRequest request = new GenerateWebhookRequest(
                    "Kavyashree D",
                    "U25UV22T043046",
                    "kavya.shreed@campusuvce.in"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(request, headers);

            System.out.println("Calling generateWebhook...");
            ResponseEntity<GenerateWebhookResponse> resp =
                    rest.postForEntity(genUrl, entity, GenerateWebhookResponse.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                System.err.println("generateWebhook failed: " + resp.getStatusCode());
                return;
            }

            GenerateWebhookResponse body = resp.getBody();
            String webhookUrl = body.getWebhook();
            String accessToken = body.getAccessToken();

            System.out.println("Received webhook: " + webhookUrl);
            System.out.println("Received accessToken: " + (accessToken != null ? "[REDACTED]" : "null"));

            int lastTwo = extractLastTwoDigits(request.getRegNo());
            boolean isOdd = (lastTwo % 2) == 1;
            System.out.println("Last two digits: " + lastTwo + " -> " + (isOdd ? "ODD (Question 1)" : "EVEN (Question 2)"));

            String finalQuery;
            if (isOdd) {
                finalQuery = buildFinalQueryForQuestion1();
            } else {
                finalQuery = buildFinalQueryForQuestion2();
            }

            HttpHeaders postHeaders = new HttpHeaders();
            postHeaders.setContentType(MediaType.APPLICATION_JSON);
            postHeaders.set("Authorization", "Bearer " + accessToken);

            Map<String, String> payload = Map.of("finalQuery", finalQuery);
            HttpEntity<Map<String, String>> postEntity = new HttpEntity<>(payload, postHeaders);

            System.out.println("Submitting finalQuery to webhook...");
            ResponseEntity<String> postResp = rest.postForEntity(webhookUrl, postEntity, String.class);
            System.out.println("Submit status: " + postResp.getStatusCode());
            System.out.println("Submit response: " + postResp.getBody());

        } catch (Exception e) {
            System.err.println("Error in workflow:");
            e.printStackTrace();
        }
    }

    private int extractLastTwoDigits(String regNo) {
        if (regNo == null) return 0;
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.length() == 0) return 0;
        String lastTwo = digits.length() == 1 ? digits : digits.substring(digits.length() - 2);
        return Integer.parseInt(lastTwo);
    }

    private String buildFinalQueryForQuestion1() {
        return "SELECT e.FIRST_NAME || ' ' || e.LAST_NAME AS NAME, "
             + "p.AMOUNT AS SALARY, "
             + "FLOOR(DATEDIFF(CURDATE(), e.DOB)/365) AS AGE, "
             + "d.DEPARTMENT_NAME "
             + "FROM PAYMENTS p "
             + "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID "
             + "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID "
             + "WHERE DAY(p.PAYMENT_TIME) <> 1 "
             + "ORDER BY p.AMOUNT DESC LIMIT 1;";
    }

    private String buildFinalQueryForQuestion2() {
        return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, "
             + "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT "
             + "FROM EMPLOYEE e1 "
             + "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID "
             + "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT "
             + "AND e2.DOB > e1.DOB "
             + "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME "
             + "ORDER BY e1.EMP_ID DESC;";
    }
}
