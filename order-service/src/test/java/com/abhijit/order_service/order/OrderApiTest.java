package com.abhijit.order_service.order;



import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
public class OrderApiTest {
	
	@Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createOrder_withValidBody_returns201WithTotal() throws Exception {
        String body = """
                {"customerName":"Ravi Kumar","customerEmail":"ravi@example.com",
                 "shippingAddress":{"line":"12 MG Road","city":"Mysore","state":"Karnataka","pincode":"570001"},
                 "items":[{"sku":"SKU-1001","quantity":2},{"sku":"SKU-1002","quantity":1}]}
                """;

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(110700.0))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void createOrder_withInvalidBody_returns400() throws Exception {
        String body = """
                {"customerName":"","customerEmail":"not-an-email",
                 "shippingAddress":{"line":"12 MG Road","city":"Mysore","state":"Karnataka","pincode":"12"},
                 "items":[]}
                """;

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

}
