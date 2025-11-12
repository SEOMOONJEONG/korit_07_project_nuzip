package com.nuzip.nuzip.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

// JPA 리포지토리 (existsByUserId, findByUserId)
// Spring Data REST에서 노출되지 않도록 했습니다.
@RepositoryRestResource(exported = false)
public interface UserRepository extends JpaRepository<User, Long> {

    // UserDatailsService에서 사용할 수 있도록 미리 추상 메서드 하나 정의해두겠습니다.
    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

}