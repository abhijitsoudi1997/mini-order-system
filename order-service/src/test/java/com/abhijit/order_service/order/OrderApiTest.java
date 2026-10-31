package com.abhijit.order_service.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

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
				.andExpect(status().isCreated()).andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.status").value("CREATED")).andExpect(jsonPath("$.totalAmount").value(110700.0))
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	void createOrder_withInvalidBody_returns400WithFieldDetails() throws Exception {
		String body = """
				{"customerName":"","customerEmail":"not-an-email",
				 "shippingAddress":{"line":"12 MG Road","city":"Mysore","state":"Karnataka","pincode":"12"},
				 "items":[]}
				""";

		mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"))
				.andExpect(jsonPath("$.path").value("/orders"))
				.andExpect(jsonPath("$.details", hasItem("shippingAddress.pincode: pincode must be 6 digits")))
				.andExpect(jsonPath("$.details", hasItem("items: at least one item is required")));
	}

	@Test
	void createOrder_withUnknownSku_returns400AndSavesNothing() throws Exception {
		String body = """
				{"customerName":"Atomic Test","customerEmail":"atomic@example.com",
				 "shippingAddress":{"line":"12 MG Road","city":"Mysore","state":"Karnataka","pincode":"570001"},
				 "items":[{"sku":"SKU-1001","quantity":1},{"sku":"NOPE","quantity":1}]}
				""";

		mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Unknown product sku NOPE"));

		mockMvc.perform(get("/orders").param("customerEmail", "atomic@example.com")).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void getOrder_whenMissing_returns404WithMessage() throws Exception {
		mockMvc.perform(get("/orders/99999")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Order not found with id 99999"))
				.andExpect(jsonPath("$.path").value("/orders/99999"));
	}

	@Test
	void getOrderByNumber_whenMissing_returns404() throws Exception {
		mockMvc.perform(get("/orders/number/NOPE")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Order not found with order number NOPE"));
	}

	@Test
	void listOrders_withoutEmail_returns400() throws Exception {
		mockMvc.perform(get("/orders")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Missing request parameter: customerEmail"));
	}

	@Test
	void getOrder_withNonNumericId_returns400() throws Exception {
		mockMvc.perform(get("/orders/abc")).andExpect(status().isBadRequest());
	}

	@Test
	void unknownUrl_returns404() throws Exception {
		mockMvc.perform(get("/nope")).andExpect(status().isNotFound());
	}

	@Test
	void getOrder_byIdAndByNumber_andListByEmail() throws Exception {
		String body = """
				{"customerName":"Round Four","customerEmail":"round4@example.com",
				 "shippingAddress":{"line":"12 MG Road","city":"Mysore","state":"Karnataka","pincode":"570001"},
				 "items":[{"sku":"SKU-1003","quantity":1}]}
				""";
		String json = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		Number id = JsonPath.read(json, "$.id");
		String number = JsonPath.read(json, "$.orderNumber");

		mockMvc.perform(get("/orders/" + id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.orderNumber").value(number));

		mockMvc.perform(get("/orders/number/" + number)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1));

		mockMvc.perform(get("/orders").param("customerEmail", "round4@example.com")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.page.totalElements").value(1));
	}

}
