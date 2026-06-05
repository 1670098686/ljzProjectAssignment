package com.campus.trade.repository;

import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.AccountStatus;
import com.campus.trade.model.enums.UserRole;
import com.campus.trade.repository.projection.SchoolCountView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByRoleAndStatus(UserRole role, AccountStatus status);

    List<User> findByStatus(AccountStatus status);

    List<User> findByDeleteRequestedTrueAndDeleteScheduleTimeBefore(LocalDateTime time);

    long countByCreateTimeAfter(LocalDateTime time);

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(AccountStatus status);

    long countByEmailVerifiedTrue();

    long countByDeleteRequestedTrue();

    long countByLastLoginAfter(LocalDateTime time);

    long countByRole(UserRole role);

    @Query("select u.school as school, count(u) as total from User u where u.school is not null " +
            "group by u.school order by total desc")
    List<SchoolCountView> topSchools(Pageable pageable);
}
