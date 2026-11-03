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

import com.jayway.jsonpath.JsonPath;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OrderCancelApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String orderJson(String sku, int quantity) {
        return """
            {
              "customerName": "Cancel Tester",
              "customerEmail": "cancel-test@example.com",
              "customerPhone": "9876543210",
              "shippingAddress": { "line": "12 MG Road", "city": "Mysore", "state": "Karnataka", "pincode": "570001" },
              "items": [ { "sku": "%s", "quantity": %d } ]
            }
            """.formatted(sku, quantity);
    }

    // places an order and returns its id
    private int placeOrder(String sku, int quantity, int expectedStatus) throws Exception {
        String body = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(sku, quantity)))
            .andExpect(status().is(expectedStatus))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void confirmedOrderCanBeCancelled() throws Exception {
        int id = placeOrder("SKU-1005", 1, 201);

        mockMvc.perform(post("/orders/" + id + "/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancellingTwiceReturns409() throws Exception {
        int id = placeOrder("SKU-1005", 1, 201);

        mockMvc.perform(post("/orders/" + id + "/cancel")).andExpect(status().isOk());
        mockMvc.perform(post("/orders/" + id + "/cancel")).andExpect(status().isConflict());
    }

    @Test
    void failedOrderCannotBeCancelled() throws Exception {
        int id = placeOrder("SKU-1004", 1, 409);     // out of stock, saved as FAILED

        mockMvc.perform(post("/orders/" + id + "/cancel"))
            .andExpect(status().isConflict());
    }

    @Test
    void cancellingUnknownOrderReturns404() throws Exception {
        mockMvc.perform(post("/orders/999999/cancel"))
            .andExpect(status().isNotFound());
    }
}
