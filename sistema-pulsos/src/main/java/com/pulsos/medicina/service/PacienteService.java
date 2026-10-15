        Consulta consulta = buscarConsultaPorId(consultaId);

        if (!consulta.getPaciente()
                .getId()
                .equals(pacienteId)) {

            throw new RuntimeException(
                    "La historia clínica no pertenece al paciente indicado"
            );
        }

        consulta.setMedicoTratante(datos.getMedicoTratante());
        consulta.setMatriculaMedico(datos.getMatriculaMedico());
        consulta.setMotivo(datos.getMotivo());
        consulta.setDiagnostico(datos.getDiagnostico());
        consulta.setTratamiento(datos.getTratamiento());

        consultaRepo.save(consulta);
    }

    public void eliminarConsulta(
            Long pacienteId,
            Long consultaId) {

        Consulta consulta = buscarConsultaPorId(consultaId);

        if (!consulta.getPaciente()
                .getId()
                .equals(pacienteId)) {

            throw new RuntimeException(
                    "La historia clínica no pertenece al paciente indicado"
            );
        }

        consultaRepo.delete(consulta);
    }

    public void adjuntarArchivo(
            Long pacienteId,
            MultipartFile file,
            String tipo,
            String descripcion,
            String usuario) throws IOException {

        if (file.isEmpty()) {
            return;
        }

        Paciente paciente = buscarPorId(pacienteId);

        String originalName = file.getOriginalFilename();
        String storedName = storage.subir(file);

        DocumentoAdjunto adjunto =
                new DocumentoAdjunto();

        adjunto.setNombreArchivoOriginal(originalName);
        adjunto.setNombreArchivoAlmacenado(storedName);
        adjunto.setTipoDocumento(tipo);

        adjunto.setDescripcion(
                descripcion != null &&
                !descripcion.trim().isEmpty()
                        ? descripcion.trim()
                        : originalName
        );

        adjunto.setContentType(file.getContentType());
        adjunto.setTamanio(file.getSize());

        adjunto.setSubidoPor(
                usuario != null
                        ? usuario
                        : "Sistema"
        );

        adjunto.setPaciente(paciente);

        try {
            docRepo.saveAndFlush(adjunto);
        } catch (RuntimeException e) {
            storage.eliminar(storedName);
            throw e;
        }
    }

    public Resource cargarArchivoComoRecurso(String storedName) {
        return new ByteArrayResource(storage.descargar(storedName));
    }

    public DocumentoAdjunto buscarDocumentoPorId(
            Long id) {

        return docRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Documento no encontrado"
                        ));
    }

    @Transactional
    public void eliminarDocumento(Long docId) {
        DocumentoAdjunto doc = buscarDocumentoPorId(docId);
        String nombre = doc.getNombreArchivoAlmacenado();
        docRepo.delete(doc);
        docRepo.flush();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    storage.eliminar(nombre);
                } catch (Exception e) {
                    System.err.println("No se pudo eliminar archivo R2: " + nombre);
                }
            }
        });
    }
}
