package com.shop.shopping.pagerealm.repository;

import com.shop.shopping.pagerealm.entity.AppRole;
import com.shop.shopping.pagerealm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {

    Optional<Role> findByRoleName(AppRole appRole);
}
