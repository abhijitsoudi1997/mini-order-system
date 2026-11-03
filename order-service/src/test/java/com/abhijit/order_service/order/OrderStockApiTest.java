package com.abhijit.order_service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class OrderStockApiTest {
	
	// Stub stock (test profile): SKU-1003 = 5, SKU-1004 = 0, SKU-1005 = 200
    private static final String EMAIL = "stock-test@example.com";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // Builds a valid order JSON; only the SKU and quantity change between tests
    private String orderJson(String sku, int quantity) {
        return """
            {
              "customerName": "Stock Tester",
              "customerEmail": "%s",
              "customerPhone": "9876543210",
              "shippingAddress": {
                "line": "12 MG Road",
                "city": "Mysore",
                "state": "Karnataka",
                "pincode": "570001"
              },
              "items": [ { "sku": "%s", "quantity": %d } ]
            }
            """.formatted(EMAIL, sku, quantity);
    }

    @Test
    void outOfStockProductReturns409() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("SKU-1004", 1)))          // stock is 0
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.failureReason").value(containsString("Insufficient stock for SKU-1004")));
    }

    @Test
    void quantityMoreThanStockReturns409() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("SKU-1003", 6)))          // stock is 5
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.failureReason").value(containsString("requested 6, available 5")));
    }

    @Test
    void quantityEqualToStockIsAccepted() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("SKU-1005", 200)))        // exactly the stock: boundary case
            .andExpect(status().isCreated());
    }

    @Test
    void quantityOneMoreThanStockIsRejected() throws Exception {
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson("SKU-1005", 201)))        // one above the stock
            .andExpect(status().isConflict());
    }

    @Test
    void rejectedOrderIsSavedAsFailed() throws Exception {
        String uniqueEmail = "failed-order@example.com";
        String body = orderJson("SKU-1004", 1).replace(EMAIL, uniqueEmail);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("FAILED"));

        mockMvc.perform(get("/orders").param("customerEmail", uniqueEmail))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

}
