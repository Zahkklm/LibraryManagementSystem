package com.librarysystem.borrowservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test") // Use application-test.yml for H2 config
public class BorrowServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}