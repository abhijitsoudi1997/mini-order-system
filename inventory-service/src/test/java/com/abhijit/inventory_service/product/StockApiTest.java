package com.abhijit.inventory_service.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
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
public class StockApiTest {
	
	@Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ReservationRepository reservationRepository;

    private MockMvc mockMvc;
    private final List<Product> created = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
    	reservationRepository.deleteAll();
        productRepository.deleteAll(created);   // only delete what this test created
        created.clear();
    }

    private Product saveProduct(String sku, int quantity) {
        Product p = new Product();
        p.setSku(sku);
        p.setName("Test " + sku);
        p.setCategory("Test");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(quantity);
        Product saved = productRepository.save(p);
        created.add(saved);
        return saved;
    }

    private String stockJson(String orderNumber, String... skuAndQty) {
        // skuAndQty: "SKU-A:2", "SKU-B:1"
        String items = Arrays.stream(skuAndQty)
                .map(s -> {
                    String[] parts = s.split(":");
                    return "{\"sku\":\"" + parts[0] + "\",\"quantity\":" + parts[1] + "}";
                })
                .collect(Collectors.joining(","));
        return "{\"orderNumber\":\"" + orderNumber + "\",\"items\":[" + items + "]}";
    }

    @Test
    void reserveReducesAvailableStock() throws Exception {
        saveProduct("SKU-STK-1", 5);

        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-1", "SKU-STK-1:2")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESERVED"))
            .andExpect(jsonPath("$.orderNumber").value("ORD-T-1"));

        mockMvc.perform(get("/products/sku/SKU-STK-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.reservedQuantity").value(2))
            .andExpect(jsonPath("$.availableQuantity").value(3));
    }

    @Test
    void reserveMoreThanAvailableReturns409() throws Exception {
        saveProduct("SKU-STK-2", 5);

        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-2", "SKU-STK-2:6")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(containsString("Insufficient stock for SKU-STK-2")));
    }

    @Test
    void reserveUnknownSkuReturns404() throws Exception {
        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-3", "SKU-DOES-NOT-EXIST:1")))
            .andExpect(status().isNotFound());
    }

    @Test
    void multiItemReservationIsAllOrNothing() throws Exception {
        saveProduct("SKU-STK-4A", 5);
        saveProduct("SKU-STK-4B", 1);

        // first item is fine, second item is too many: the whole call must fail
        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-4", "SKU-STK-4A:1", "SKU-STK-4B:99")))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/products/sku/SKU-STK-4A"))
            .andExpect(jsonPath("$.reservedQuantity").value(0));   // first item was rolled back
    }

    @Test
    void releaseGivesBackTheWholeReservation() throws Exception {
        saveProduct("SKU-STK-5", 5);

        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-5", "SKU-STK-5:3")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-5", "SKU-STK-5:3")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RELEASED"));

        mockMvc.perform(get("/products/sku/SKU-STK-5"))
            .andExpect(jsonPath("$.reservedQuantity").value(0))
            .andExpect(jsonPath("$.availableQuantity").value(5));
    }

    @Test
    void releaseOfUnknownOrderReturns404() throws Exception {
        mockMvc.perform(post("/inventory/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-6", "SKU-STK-6:1")))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderNumber\":\"\",\"items\":[]}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/inventory/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-T-7", "SKU-STK-7:0")))   // quantity must be > 0
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void reservingTheSameOrderTwiceReservesOnlyOnce() throws Exception {
        saveProduct("SKU-STK-8", 5);
        String body = stockJson("ORD-IDEM-1", "SKU-STK-8:2");

        mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());                                    // the repeat is accepted

        mockMvc.perform(get("/products/sku/SKU-STK-8"))
            .andExpect(jsonPath("$.reservedQuantity").value(2));            // but only 2 are held, not 4
    }

    @Test
    void sameOrderNumberWithDifferentItemsReturns409() throws Exception {
        saveProduct("SKU-STK-9", 5);

        mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-IDEM-2", "SKU-STK-9:2")))
            .andExpect(status().isOk());

        mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON)
                .content(stockJson("ORD-IDEM-2", "SKU-STK-9:3")))
            .andExpect(status().isConflict());
    }

    @Test
    void releasingTwiceIsHarmless() throws Exception {
        saveProduct("SKU-STK-10", 5);
        String reserve = stockJson("ORD-IDEM-3", "SKU-STK-10:2");

        mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON).content(reserve))
            .andExpect(status().isOk());
        mockMvc.perform(post("/inventory/release").contentType(MediaType.APPLICATION_JSON).content(reserve))
            .andExpect(status().isOk());
        mockMvc.perform(post("/inventory/release").contentType(MediaType.APPLICATION_JSON).content(reserve))
            .andExpect(status().isOk());                                    // second release: no error

        mockMvc.perform(get("/products/sku/SKU-STK-10"))
            .andExpect(jsonPath("$.reservedQuantity").value(0));            // and not -2
    }

}
