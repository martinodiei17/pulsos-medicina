package com.pulsos.medicina.service;

import com.pulsos.medicina.model.Consulta;
import com.pulsos.medicina.model.DocumentoAdjunto;
import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.repository.ConsultaRepository;
import com.pulsos.medicina.repository.DocumentoRepository;
import com.pulsos.medicina.repository.PacienteRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepo;
    private final DocumentoRepository docRepo;
    private final ConsultaRepository consultaRepo;
    private final Path uploadDir = Paths.get("uploads");

    public PacienteService(PacienteRepository pacienteRepo, DocumentoRepository docRepo, ConsultaRepository consultaRepo) {
        this.pacienteRepo = pacienteRepo;
        this.docRepo = docRepo;
        this.consultaRepo = consultaRepo;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de adjuntos", e);
        }
    }

    public List<Paciente> listarTodos(String busqueda) {
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            return pacienteRepo.findByNombreCompletoContainingIgnoreCaseOrDniContaining(busqueda.trim(), busqueda.trim());
        }
        return pacienteRepo.findAll();
    }

    public Paciente buscarPorId(Long id) {
        return pacienteRepo.findById(id).orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
    }

    public Paciente guardar(Paciente paciente) {
        return pacienteRepo.save(paciente);
    }

    public void agregarConsulta(Long pacienteId, Consulta consulta) {
        Paciente paciente = buscarPorId(pacienteId);
        consulta.setPaciente(paciente);
        consultaRepo.save(consulta);
    }

    public void adjuntarArchivo(Long pacienteId, MultipartFile file, String tipo, String descripcion, String usuario) throws IOException {
        if (file.isEmpty()) return;

        Paciente paciente = buscarPorId(pacienteId);
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;
        Path targetPath = this.uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        DocumentoAdjunto adjunto = new DocumentoAdjunto();
        adjunto.setNombreArchivoOriginal(originalName);
        adjunto.setNombreArchivoAlmacenado(storedName);
        adjunto.setTipoDocumento(tipo);
        adjunto.setDescripcion(descripcion != null && !descripcion.trim().isEmpty() ? descripcion.trim() : originalName);
        adjunto.setContentType(file.getContentType());
        adjunto.setTamanio(file.getSize());
        adjunto.setSubidoPor(usuario != null ? usuario : "Sistema");
        adjunto.setPaciente(paciente);

        docRepo.save(adjunto);
    }

    public Resource cargarArchivoComoRecurso(String storedName) {
        try {
            Path filePath = this.uploadDir.resolve(storedName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Archivo no legible");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Ruta no válida", e);
        }
    }

    public DocumentoAdjunto buscarDocumentoPorId(Long id) {
        return docRepo.findById(id).orElseThrow(() -> new RuntimeException("Documento no encontrado"));
    }

    public void eliminarDocumento(Long docId) {
        DocumentoAdjunto doc = buscarDocumentoPorId(docId);
        try {
            Path file = this.uploadDir.resolve(doc.getNombreArchivoAlmacenado());
            Files.deleteIfExists(file);
        } catch (Exception ignored) {}
        docRepo.delete(doc);
    }
}
