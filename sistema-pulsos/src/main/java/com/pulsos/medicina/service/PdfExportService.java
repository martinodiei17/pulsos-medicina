package com.pulsos.medicina.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.pulsos.medicina.model.Consulta;
import com.pulsos.medicina.model.DocumentoAdjunto;
import com.pulsos.medicina.model.Paciente;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    private static final Color COLOR_VERDE_PULSOS = new Color(72, 90, 72);
    private static final Color COLOR_ARENA_LINEA = new Color(227, 221, 216);
    private static final Color COLOR_FONDO_SECCION = new Color(250, 247, 245);
    private static final Color COLOR_TEXTO_OSCURO = new Color(45, 52, 45);
    private static final Color COLOR_TEXTO_GRIS = new Color(108, 117, 108);
    private static final Color COLOR_ALERTA = new Color(184, 80, 80);

    public byte[] generarHistoriaClinicaPdf(Paciente paciente) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            HeaderFooter footer = new HeaderFooter(new Phrase("PULSOS - Medicina & Bienestar  |  Página ", 
                    FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXTO_GRIS)), new Phrase(". Documento Clínico Confidencial."));
            footer.setBorder(Rectangle.NO_BORDER);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.setFooter(footer);

            document.open();

            // Membrete
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65f, 35f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            
            Font logoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COLOR_VERDE_PULSOS);
            Paragraph logoP = new Paragraph("P U L S O S", logoFont);
            logoP.setSpacingAfter(2f);
            leftCell.addElement(logoP);

            Font subLogoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXTO_GRIS);
            leftCell.addElement(new Paragraph("MEDICINA & BIENESTAR INTEGRAL", subLogoFont));
            headerTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            
            Font titleDocFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_VERDE_PULSOS);
            Paragraph docTitle = new Paragraph("HISTORIA CLÍNICA", titleDocFont);
            docTitle.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(docTitle);

            Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXTO_GRIS);
            Paragraph fechaP = new Paragraph("Emisión: " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), fechaFont);
            fechaP.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(fechaP);
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Línea divisoria
            document.add(new Paragraph(" "));
            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(COLOR_VERDE_PULSOS);
            lineCell.setFixedHeight(2f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            lineTable.addCell(lineCell);
            document.add(lineTable);
            document.add(new Paragraph(" "));

            // Datos del Paciente
            Font secFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_VERDE_PULSOS);
            document.add(new Paragraph("INFORMACIÓN DEL PACIENTE", secFont));
            document.add(new Paragraph(" "));

            PdfPTable infoTable = new PdfPTable(3);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{40f, 30f, 30f});

            Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_TEXTO_GRIS);
            Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXTO_OSCURO);

            addPatientCell(infoTable, "NOMBRE COMPLETO", paciente.getNombreCompleto(), lblFont, valFont);
            addPatientCell(infoTable, "DNI", paciente.getDni(), lblFont, valFont);
            addPatientCell(infoTable, "EDAD / NACIMIENTO", (paciente.getEdad() != null ? paciente.getEdad() + " años" : "-") + 
                    (paciente.getFechaNacimiento() != null ? " (" + paciente.getFechaNacimiento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")" : ""), lblFont, valFont);

            addPatientCell(infoTable, "COBERTURA MÉDICA", (paciente.getObraSocial() != null && !paciente.getObraSocial().isEmpty() ? paciente.getObraSocial() : "Particular") + 
                    (paciente.getNumeroAfiliado() != null ? " (N° " + paciente.getNumeroAfiliado() + ")" : ""), lblFont, valFont);
            addPatientCell(infoTable, "TELÉFONO", paciente.getTelefono() != null ? paciente.getTelefono() : "-", lblFont, valFont);
            addPatientCell(infoTable, "GRUPO SANGUÍNEO", paciente.getGrupoSanguineo() != null ? paciente.getGrupoSanguineo() : "-", lblFont, valFont);

            document.add(infoTable);

            // Alergias y Antecedentes
            document.add(new Paragraph(" "));
            PdfPTable alertTable = new PdfPTable(2);
            alertTable.setWidthPercentage(100);
            alertTable.setWidths(new float[]{50f, 50f});

            PdfPCell cAlergia = new PdfPCell();
            cAlergia.setBackgroundColor(COLOR_FONDO_SECCION);
            cAlergia.setBorderColor(COLOR_ARENA_LINEA);
            cAlergia.setPadding(8f);
            cAlergia.addElement(new Paragraph("ALERGIAS O CONTRAINDICACIONES", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_ALERTA)));
            cAlergia.addElement(new Paragraph(paciente.getAlergias() != null && !paciente.getAlergias().isEmpty() ? paciente.getAlergias() : "Sin alergias declaradas", valFont));
            alertTable.addCell(cAlergia);

            PdfPCell cAntecedentes = new PdfPCell();
            cAntecedentes.setBackgroundColor(COLOR_FONDO_SECCION);
            cAntecedentes.setBorderColor(COLOR_ARENA_LINEA);
            cAntecedentes.setPadding(8f);
            cAntecedentes.addElement(new Paragraph("ANTECEDENTES CLÍNICOS / QUIRÚRGICOS", lblFont));
            cAntecedentes.addElement(new Paragraph(paciente.getAntecedentesMedicos() != null && !paciente.getAntecedentesMedicos().isEmpty() ? paciente.getAntecedentesMedicos() : "Sin antecedentes registrados", valFont));
            alertTable.addCell(cAntecedentes);

            document.add(alertTable);

            // Evoluciones Médicas
            document.add(new Paragraph(" "));
            document.add(new Paragraph("EVOLUCIONES MÉDICAS CRONOLÓGICAS", secFont));
            document.add(new Paragraph(" "));

            if (paciente.getConsultas() == null || paciente.getConsultas().isEmpty()) {
                document.add(new Paragraph("No se registran evoluciones médicas previas.", valFont));
            } else {
                for (Consulta c : paciente.getConsultas()) {
                    PdfPTable conTable = new PdfPTable(1);
                    conTable.setWidthPercentage(100);

                    PdfPCell cell = new PdfPCell();
                    cell.setBorderColor(COLOR_ARENA_LINEA);
                    cell.setBackgroundColor(COLOR_FONDO_SECCION);
                    cell.setPadding(10f);

                    String fechaStr = c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    Paragraph headP = new Paragraph(fechaStr + "  -  " + c.getMedicoTratante() + (c.getMatriculaMedico() != null ? " (" + c.getMatriculaMedico() + ")" : ""),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_VERDE_PULSOS));
                    cell.addElement(headP);
                    cell.addElement(new Paragraph("Motivo: " + c.getMotivo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXTO_OSCURO)));

                    if (c.getDiagnostico() != null && !c.getDiagnostico().isEmpty()) {
                        cell.addElement(new Paragraph("Diagnóstico: " + c.getDiagnostico(), valFont));
                    }
                    if (c.getTratamiento() != null && !c.getTratamiento().isEmpty()) {
                        cell.addElement(new Paragraph("Indicaciones / Tratamiento: " + c.getTratamiento(), valFont));
                    }

                    conTable.addCell(cell);
                    document.add(conTable);
                    document.add(new Paragraph(" "));
                }
            }

            // Registro de Estudios
            if (paciente.getAdjuntos() != null && !paciente.getAdjuntos().isEmpty()) {
                document.add(new Paragraph("ESTUDIOS DE IMÁGENES Y RECETAS REGISTRADAS", secFont));
                document.add(new Paragraph(" "));

                PdfPTable adjTable = new PdfPTable(3);
                adjTable.setWidthPercentage(100);
                adjTable.setWidths(new float[]{25f, 50f, 25f});

                addTableHeader(adjTable, "TIPO", lblFont);
                addTableHeader(adjTable, "DESCRIPCIÓN / ARCHIVO", lblFont);
                addTableHeader(adjTable, "FECHA DE CARGA", lblFont);

                for (DocumentoAdjunto doc : paciente.getAdjuntos()) {
                    addTableData(adjTable, doc.getTipoDocumento(), valFont);
                    addTableData(adjTable, (doc.getDescripcion() != null ? doc.getDescripcion() + " - " : "") + doc.getNombreArchivoOriginal(), valFont);
                    addTableData(adjTable, doc.getFechaCarga().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), valFont);
                }
                document.add(adjTable);
            }

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addPatientCell(PdfPTable table, String label, String value, Font lblFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COLOR_ARENA_LINEA);
        cell.setPadding(6f);
        cell.addElement(new Paragraph(label, lblFont));
        cell.addElement(new Paragraph(value != null ? value : "-", valFont));
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String header, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(header, font));
        cell.setBackgroundColor(COLOR_FONDO_SECCION);
        cell.setBorderColor(COLOR_ARENA_LINEA);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addTableData(PdfPTable table, String data, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(data != null ? data : "-", font));
        cell.setBorderColor(COLOR_ARENA_LINEA);
        cell.setPadding(6f);
        table.addCell(cell);
    }
}
