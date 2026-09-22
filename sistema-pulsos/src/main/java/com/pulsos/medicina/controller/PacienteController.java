package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Consulta;
import com.pulsos.medicina.model.DocumentoAdjunto;
import com.pulsos.medicina.model.Paciente;
import com.pulsos.medicina.service.PacienteService;
import com.pulsos.medicina.service.PdfExportService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;
    private final PdfExportService pdfExportService;

    public PacienteController(PacienteService pacienteService, PdfExportService pdfExportService) {
        this.pacienteService = pacienteService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("pacientes", pacienteService.listarTodos(q));
        model.addAttribute("busqueda", q);
        return "pacientes/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        model.addAttribute("paciente", new Paciente());
        model.addAttribute("esEdicion", false);
        return "pacientes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Paciente paciente = pacienteService.buscarPorId(id);
        model.addAttribute("paciente", paciente);
        model.addAttribute("esEdicion", true);
        return "pacientes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("paciente") Paciente paciente) {
        pacienteService.guardar(paciente);
        return "redirect:/pacientes/" + paciente.getId();
    }

    @GetMapping("/{id}")
    public String verHistoriaClinica(@PathVariable Long id, Model model) {
        Paciente paciente = pacienteService.buscarPorId(id);
        model.addAttribute("paciente", paciente);
        model.addAttribute("nuevaConsulta", new Consulta());
        return "pacientes/historia-clinica";
    }

    @PostMapping("/{id}/consultas")
    public String agregarConsulta(@PathVariable Long id, @ModelAttribute Consulta nuevaConsulta, Authentication auth) {
        if (nuevaConsulta.getMedicoTratante() == null || nuevaConsulta.getMedicoTratante().isEmpty()) {
            nuevaConsulta.setMedicoTratante(auth != null ? auth.getName() : "Médico");
        }
        pacienteService.agregarConsulta(id, nuevaConsulta);
        return "redirect:/pacientes/" + id;
    }

    @PostMapping("/{id}/adjuntos")
    public String subirDocumento(@PathVariable Long id,
                                 @RequestParam("archivo") MultipartFile archivo,
                                 @RequestParam("tipo") String tipo,
                                 @RequestParam(value = "descripcion", required = false) String descripcion,
                                 Authentication auth) {
        try {
            String usuario = auth != null ? auth.getName() : "Médico";
            pacienteService.adjuntarArchivo(id, archivo, tipo, descripcion, usuario);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/pacientes/" + id;
    }

    @GetMapping("/adjuntos/{docId}/eliminar")
    public String eliminarDocumento(@PathVariable Long docId, @RequestParam("pacienteId") Long pacienteId) {
        pacienteService.eliminarDocumento(docId);
        return "redirect:/pacientes/" + pacienteId;
    }

    @GetMapping("/archivos/{docId}")
    @ResponseBody
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long docId) {
        DocumentoAdjunto doc = pacienteService.buscarDocumentoPorId(docId);
        Resource file = pacienteService.cargarArchivoComoRecurso(doc.getNombreArchivoAlmacenado());
        String mediaType = doc.getContentType() != null ? doc.getContentType() : "application/octet-stream";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNombreArchivoOriginal() + "\"")
                .body(file);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarHistoriaClinicaPdf(@PathVariable Long id) {
        Paciente paciente = pacienteService.buscarPorId(id);
        byte[] pdfBytes = pdfExportService.generarHistoriaClinicaPdf(paciente);
        String nombrePdf = "Historia_Clinica_" + paciente.getDni() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombrePdf + "\"")
                .body(pdfBytes);
    }
}
