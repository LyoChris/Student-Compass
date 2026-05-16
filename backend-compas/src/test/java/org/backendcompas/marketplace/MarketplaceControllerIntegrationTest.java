package org.backendcompas.marketplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.backendcompas.core.security.JwtUtil;
import org.backendcompas.modules.account.model.User;
import org.backendcompas.modules.account.model.UserRole;
import org.backendcompas.modules.account.model.UserStatus;
import org.backendcompas.modules.account.repository.UserRepository;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.model.MarketplaceItem;
import org.backendcompas.modules.marketplace.repository.MarketplaceItemRepository;
import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class MarketplaceControllerIntegrationTest {

    private static final UUID IASI_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FII_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @Autowired private UserRepository userRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private FacultyRepository facultyRepository;
    @Autowired private MarketplaceItemRepository itemRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String userToken;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        City city = cityRepository.findById(IASI_ID).orElseThrow();
        Faculty faculty = facultyRepository.findById(FII_ID).orElseThrow();

        User user = new User();
        user.setFirstName("Market");
        user.setLastName("Tester");
        user.setEmail("market@test.com");
        user.setPasswordHash(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCity(city);
        user.setFaculty(faculty);
        user = userRepository.save(user);

        sellerId = user.getId();
        userToken = jwtUtil.generateAccessToken(user);
    }

    @Test
    void createItemReturns201() throws Exception {
        CreateItemRequestDto dto = CreateItemRequestDto.builder()
                .sellerId(sellerId)
                .title("Math Notes")
                .description("Great math notes for year 1")
                .price(BigDecimal.valueOf(35))
                .category(ItemCategory.BOOKS_NOTES)
                .itemCondition(ItemCondition.GOOD)
                .tags(List.of("math", "year-1"))
                .imageUrls(List.of("https://example.com/img.jpg"))
                .build();

        mockMvc.perform(post("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Math Notes"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isBoosted").value(false));
    }

    @Test
    void createItemWithoutAuthReturns401() throws Exception {
        CreateItemRequestDto dto = CreateItemRequestDto.builder()
                .sellerId(sellerId).title("X").description("Y")
                .price(BigDecimal.ONE).category(ItemCategory.OTHER).itemCondition(ItemCondition.FAIR)
                .build();

        mockMvc.perform(post("/api/v1/marketplace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createItemMissingTitleReturns400() throws Exception {
        String payload = """
                {"sellerId":"%s","description":"D","price":10,"category":"OTHER","itemCondition":"NEW"}
                """.formatted(sellerId);

        mockMvc.perform(post("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsAllActiveItems() throws Exception {
        createItem("Book A", "D", BigDecimal.valueOf(10), ItemCategory.BOOKS_NOTES, ItemCondition.NEW);
        createItem("Laptop", "D", BigDecimal.valueOf(800), ItemCategory.ELECTRONICS, ItemCondition.GOOD);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void searchByTextMatchesTitle() throws Exception {
        createItem("Calculus Guide", "Math", BigDecimal.valueOf(20), ItemCategory.BOOKS_NOTES, ItemCondition.LIKE_NEW);
        createItem("Laptop", "Electronics", BigDecimal.valueOf(500), ItemCategory.ELECTRONICS, ItemCondition.GOOD);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("search", "calculus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchByCategory() throws Exception {
        createItem("Book", "D", BigDecimal.valueOf(10), ItemCategory.BOOKS_NOTES, ItemCondition.NEW);
        createItem("Laptop", "D", BigDecimal.valueOf(500), ItemCategory.ELECTRONICS, ItemCondition.GOOD);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("category", "ELECTRONICS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchByCondition() throws Exception {
        createItem("Book", "D", BigDecimal.valueOf(15), ItemCategory.BOOKS_NOTES, ItemCondition.NEW);
        createItem("Jacket", "D", BigDecimal.valueOf(30), ItemCategory.CLOTHING, ItemCondition.FAIR);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("condition", "FAIR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchByMaxPrice() throws Exception {
        createItem("Cheap", "D", BigDecimal.valueOf(5), ItemCategory.OTHER, ItemCondition.FAIR);
        createItem("Expensive", "D", BigDecimal.valueOf(500), ItemCategory.ELECTRONICS, ItemCondition.NEW);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchByMinPrice() throws Exception {
        createItem("Cheap", "D", BigDecimal.valueOf(5), ItemCategory.OTHER, ItemCondition.FAIR);
        createItem("Expensive", "D", BigDecimal.valueOf(500), ItemCategory.ELECTRONICS, ItemCondition.NEW);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("minPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchWithNegativeMinPriceReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("minPrice", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchWithMinGreaterThanMaxReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("minPrice", "100")
                        .param("maxPrice", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchWithSearchTermTooLongReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("search", "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchWithInvalidSortPropertyReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("sort", "invalidField,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchWithValidSortProperty() throws Exception {
        createItem("A Item", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);
        createItem("B Item", "D", BigDecimal.valueOf(20), ItemCategory.OTHER, ItemCondition.NEW);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("sort", "price,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("A Item"));
    }

    @Test
    void boostedItemsAppearFirst() throws Exception {
        createItem("Regular", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);
        MarketplaceItem boosted = createItem("Boosted", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);
        boosted.setIsBoosted(true);
        itemRepository.save(boosted);

        mockMvc.perform(get("/api/v1/marketplace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Boosted"));
    }

    @Test
    void getItemByIdReturns200() throws Exception {
        MarketplaceItem item = createItem("Get Me", "Desc", BigDecimal.valueOf(25), ItemCategory.OTHER, ItemCondition.NEW);

        mockMvc.perform(get("/api/v1/marketplace/" + item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Get Me"));
    }

    @Test
    void getItemByUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItemReturnsUpdatedFields() throws Exception {
        MarketplaceItem item = createItem("Old Title", "Old desc", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);

        UpdateItemRequestDto dto = UpdateItemRequestDto.builder()
                .title("New Title").price(BigDecimal.valueOf(99)).build();

        mockMvc.perform(put("/api/v1/marketplace/" + item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.price").value(99));
    }

    @Test
    void updateNonExistentItemReturns404() throws Exception {
        UpdateItemRequestDto dto = UpdateItemRequestDto.builder().title("New").build();

        mockMvc.perform(put("/api/v1/marketplace/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItemWithNullFieldsPreservesOriginal() throws Exception {
        MarketplaceItem item = createItem("Preserve Me", "Keep", BigDecimal.valueOf(10), ItemCategory.BOOKS_NOTES, ItemCondition.GOOD);

        UpdateItemRequestDto dto = UpdateItemRequestDto.builder().description("Updated desc").build();

        mockMvc.perform(put("/api/v1/marketplace/" + item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Preserve Me"))
                .andExpect(jsonPath("$.description").value("Updated desc"));
    }

    @Test
    void updateItemTagsAndImages() throws Exception {
        MarketplaceItem item = createItem("Tagged", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);

        UpdateItemRequestDto dto = UpdateItemRequestDto.builder()
                .tags(List.of("tag1", "tag2"))
                .imageUrls(List.of("https://example.com/a.jpg"))
                .build();

        mockMvc.perform(put("/api/v1/marketplace/" + item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value("tag1"));
    }

    @Test
    void changeStatusToReserved() throws Exception {
        MarketplaceItem item = createItem("Status Item", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);

        mockMvc.perform(patch("/api/v1/marketplace/" + item.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("status", "RESERVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    void changeStatusForUnknownItemReturns404() throws Exception {
        mockMvc.perform(patch("/api/v1/marketplace/" + UUID.randomUUID() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .param("status", "SOLD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void boostItemSetsBoostedTrue() throws Exception {
        MarketplaceItem item = createItem("Boost Me", "D", BigDecimal.valueOf(10), ItemCategory.OTHER, ItemCondition.NEW);

        mockMvc.perform(patch("/api/v1/marketplace/" + item.getId() + "/boost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isBoosted").value(true));
    }

    @Test
    void boostNonExistentItemReturns404() throws Exception {
        mockMvc.perform(patch("/api/v1/marketplace/" + UUID.randomUUID() + "/boost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    private MarketplaceItem createItem(String title, String description, BigDecimal price,
                                       ItemCategory category, ItemCondition condition) {
        return itemRepository.save(MarketplaceItem.builder()
                .sellerId(sellerId)
                .title(title)
                .description(description)
                .price(price)
                .category(category)
                .itemCondition(condition)
                .status(ItemStatus.ACTIVE)
                .isBoosted(false)
                .build());
    }
}
