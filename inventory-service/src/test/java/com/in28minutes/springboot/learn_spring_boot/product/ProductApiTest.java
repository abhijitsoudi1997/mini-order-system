package com.in28minutes.springboot.learn_spring_boot.product;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

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

@SpringBootTest
@ActiveProfiles("test")
class ProductApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void getProducts_returnsSeededProducts() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sku", hasItem("SKU-1001")));
    }
    
    @Test
    void getProducts_withSize_limitsThePage() throws Exception {
        mockMvc.perform(get("/products").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getProduct_whenMissing_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id 99999"));
    }
    
    @Test
    void getProductBySku_whenExists_returns200() throws Exception {
        mockMvc.perform(get("/products/sku/SKU-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getProductBySku_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/products/sku/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with sku NOPE"));
    }

    @Test
    void createProduct_withValidBody_returns201AndLocation() throws Exception {
        String body = """
                {"sku":"SKU-TEST-1","name":"Pen","category":"Stationery","price":20,"quantity":100}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sku").value("SKU-TEST-1"));
    }

    @Test
    void createProduct_withInvalidBody_returns400WithDetails() throws Exception {
        String body = """
                {"sku":"","name":"","category":"Stationery","price":0,"quantity":-1}
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isNotEmpty());
    }
    
    @Test
    void deleteProduct_isSoft_andProductDisappearsFromApi() throws Exception {
        String body = """
                {"sku":"SKU-TEST-2","name":"Bin","category":"Home","price":99.50,"quantity":3}
                """;
        String json = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$.id");

        mockMvc.perform(delete("/products/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/products/" + id)).andExpect(status().isNotFound());
    }
}