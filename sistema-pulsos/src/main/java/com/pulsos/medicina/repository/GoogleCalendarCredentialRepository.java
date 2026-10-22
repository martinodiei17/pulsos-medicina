package com.pulsos.medicina.repository;
import com.pulsos.medicina.model.GoogleCalendarCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface GoogleCalendarCredentialRepository extends JpaRepository<GoogleCalendarCredential,Long> {
 Optional<GoogleCalendarCredential> findByUsuarioId(Long usuarioId);
 @Modifying @Query("DELETE FROM GoogleCalendarCredential c WHERE c.usuario.id = :id")
 int deleteByUsuarioId(@Param("id") Long id);
}
