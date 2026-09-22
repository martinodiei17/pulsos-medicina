package com.pulsos.medicina;

import com.pulsos.medicina.model.*;
import com.pulsos.medicina.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@SpringBootApplication
public class PulsosApplication {

	public static void main(String[] args) {
		SpringApplication.run(PulsosApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(UsuarioRepository userRepo, PacienteRepository pacRepo, 
							 TurnoRepository turnoRepo, PasswordEncoder encoder) {
		return args -> {
			// Forzar creación o actualización del usuario admin con contraseña encriptada correctamente
			if (userRepo.findByUsername("admin").isEmpty()) {
				Usuario admin = new Usuario();
				admin.setUsername("admin");
				admin.setPassword(encoder.encode("admin123"));
				admin.setNombreCompleto("Dirección Médica Pulsos");
				admin.setEmail("admin@pulsosmedicina.com.ar");
				admin.setTelefono("+54 9 11 5231-0001");
				admin.setRoles(Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO, Rol.ROLE_RECEPCION));
				userRepo.save(admin);
				System.out.println("--> Usuario admin creado exitosamente.");
			} else {
				userRepo.findByUsername("admin").ifPresent(admin -> {
					admin.setPassword(encoder.encode("admin123"));
					admin.setRoles(Set.of(Rol.ROLE_ADMIN, Rol.ROLE_MEDICO, Rol.ROLE_RECEPCION));
					userRepo.save(admin);
					System.out.println("--> Contraseña del usuario admin actualizada correctamente.");
				});
			}

			// Demás usuarios si no existen
			if (userRepo.findByUsername("medico").isEmpty()) {
				Usuario doc1 = new Usuario();
				doc1.setUsername("medico");
				doc1.setPassword(encoder.encode("medico123"));
				doc1.setNombreCompleto("Dra. Florencia Silva");
				doc1.setEmail("florencia.silva@pulsosmedicina.com.ar");
				doc1.setTelefono("+54 9 11 4802-9912");
				doc1.setEspecialidad("Medicina Familiar & Bienestar");
				doc1.setMatricula("MN 145.892");
				doc1.setTokenCalendario(UUID.randomUUID().toString());
				doc1.setRoles(Set.of(Rol.ROLE_MEDICO));
				userRepo.save(doc1);
			}

			if (userRepo.findByUsername("dr.santiago").isEmpty()) {
				Usuario doc2 = new Usuario();
				doc2.setUsername("dr.santiago");
				doc2.setPassword(encoder.encode("pulsos2026"));
				doc2.setNombreCompleto("Dr. Santiago Albarracín");
				doc2.setEmail("santiago.albarracin@pulsosmedicina.com.ar");
				doc2.setTelefono("+54 9 11 3912-4451");
				doc2.setEspecialidad("Cardiología & Deportología");
				doc2.setMatricula("MN 122.450");
				doc2.setTokenCalendario(UUID.randomUUID().toString());
				doc2.setRoles(Set.of(Rol.ROLE_MEDICO));
				userRepo.save(doc2);
			}

			if (userRepo.findByUsername("recepcion").isEmpty()) {
				Usuario recep = new Usuario();
				recep.setUsername("recepcion");
				recep.setPassword(encoder.encode("recepcion123"));
				recep.setNombreCompleto("Secretaría de Admisión Pulsos");
				recep.setEmail("admision@pulsosmedicina.com.ar");
				recep.setTelefono("+54 9 11 5231-0000");
				recep.setRoles(Set.of(Rol.ROLE_RECEPCION));
				userRepo.save(recep);
			}

			// Datos de pacientes y turnos de prueba opcionales si está vacía la base
			if (pacRepo.count() == 0) {
				Paciente p1 = new Paciente();
				p1.setNombreCompleto("Valentina Rossi");
				p1.setDni("38.452.198");
				p1.setFechaNacimiento(LocalDate.of(1994, 6, 15));
				p1.setTelefono("+54 9 11 4829-1123");
				p1.setEmail("valentina.rossi@email.com");
				p1.setObraSocial("OSDE");
				p1.setNumeroAfiliado("310-984729-01");
				p1.setAlergias("Penicilina, AINEs");
				p1.setAntecedentesMedicos("Hipotiroidismo subclínico diagnosticado en 2021.");
				p1.setGrupoSanguineo("A+");
				pacRepo.save(p1);

				Paciente p2 = new Paciente();
				p2.setNombreCompleto("Mateo Benítez");
				p2.setDni("41.205.890");
				p2.setFechaNacimiento(LocalDate.of(1998, 11, 3));
				p2.setTelefono("+54 9 11 5590-7712");
				p2.setEmail("mbenitez@email.com");
				p2.setObraSocial("Swiss Medical");
				p2.setNumeroAfiliado("SM-8849102");
				p2.setAlergias("Ninguna declarada");
				p2.setAntecedentesMedicos("Artroscopía de rodilla derecha (2022).");
				p2.setGrupoSanguineo("0+");
				pacRepo.save(p2);
			}
		};
	}
}
