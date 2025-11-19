package com.nuzip.nuzip;

import com.nuzip.nuzip.domain.NewsCategory;
import com.nuzip.nuzip.domain.User;
import com.nuzip.nuzip.domain.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@SpringBootApplication
public class NuzipApplication {

	public static void main(String[] args) {
		SpringApplication.run(NuzipApplication.class, args);
	}

	@Bean
	CommandLineRunner runner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// 이미 더미유저가 있다면 중복 저장 방지
			if (!userRepository.existsByUserId("kim1")) {

				User user1 = User.builder()
						.userId("kim1")
						.password(passwordEncoder.encode("1234"))
						.username("김일")
						.newsCategory(Set.of(
								NewsCategory.POLITICS,
								NewsCategory.ECONOMY,
								NewsCategory.IT_SCIENCE
						))
						.birthDate(LocalDate.of(1990, 1, 1))
						.phone("01011110000")
						.build();

				userRepository.save(user1);
			}

			if (!userRepository.existsByUserId("lee2")) {
				User user2 = User.builder()
						.userId("lee2")
						.password(passwordEncoder.encode("1234"))
						.username("이이")
						.newsCategory(Set.of(
								NewsCategory.SOCIETY,
								NewsCategory.LIFE_CULTURE,
								NewsCategory.SPORTS
						))
						.phone("01022220000")
						.build();

				userRepository.save(user2);
			}

			System.out.println("✅ 더미 유저 데이터 등록 완료!");
		};
	}
}
