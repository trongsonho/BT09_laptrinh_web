package vn.iotstar;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.Role;
import vn.iotstar.entity.User;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.RoleRepository;
import vn.iotstar.repository.UserRepository;

import java.math.BigDecimal;

@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository,
                                      UserRepository userRepository,
                                      ProductRepository productRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));

            User admin = userRepository.findByUsername("admin").orElseGet(() -> {
                User u = User.builder()
                        .username("admin")
                        .email("admin@gmail.com")
                        .fullName("Administrator")
                        .password(passwordEncoder.encode("123456"))
                        .role(adminRole)
                        .enabled(true)
                        .build();
                return userRepository.save(u);
            });

            User user1 = userRepository.findByUsername("trungnh").orElseGet(() -> {
                User u = User.builder()
                        .username("trungnh")
                        .email("trungnh@hcmute.edu.vn")
                        .fullName("Nguyễn Hữu Trung")
                        .password(passwordEncoder.encode("123456"))
                        .role(userRole)
                        .enabled(true)
                        .build();
                return userRepository.save(u);
            });

            User user2 = userRepository.findByUsername("user02").orElseGet(() -> {
                User u = User.builder()
                        .username("user02")
                        .email("user02@gmail.com")
                        .fullName("Người Dùng 02")
                        .password(passwordEncoder.encode("123456"))
                        .role(userRole)
                        .enabled(true)
                        .build();
                return userRepository.save(u);
            });

            if (productRepository.count() == 0) {
                productRepository.save(Product.builder()
                        .name("Điện thoại Oppo A95")
                        .description("Điện thoại Oppo A95 chính hãng")
                        .price(new BigDecimal("6500000.00"))
                        .user(user1)
                        .build());

                productRepository.save(Product.builder()
                        .name("Điện thoại Oppo A6")
                        .description("Điện thoại tầm trung Oppo A6")
                        .price(new BigDecimal("4200000.00"))
                        .user(user1)
                        .build());

                productRepository.save(Product.builder()
                        .name("Laptop Asus Zenbook")
                        .description("Laptop mỏng nhẹ văn phòng")
                        .price(new BigDecimal("18500000.00"))
                        .user(user2)
                        .build());
            }
        };
    }
}
