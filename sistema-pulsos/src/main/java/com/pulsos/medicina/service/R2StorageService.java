package com.pulsos.medicina.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public class R2StorageService {
    private final S3Client s3;
    private final String bucket;

    public R2StorageService(
            @Value("${r2.endpoint}") String endpoint,
            @Value("${r2.access-key-id}") String accessKey,
            @Value("${r2.secret-access-key}") String secretKey,
            @Value("${r2.bucket}") String bucket) {
        this.bucket = bucket;
        this.s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public String subir(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IOException("Archivo vacío");
        // Límite inicial de 10 MiB para evitar descargas excesivas en memoria.
        if (file.getSize() > 10L * 1024 * 1024)
            throw new IOException("El archivo supera los 10 MB");

        String original = file.getOriginalFilename();
        String extension = "";
        if (original != null && original.lastIndexOf('.') >= 0) {
            extension = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        if (!extension.matches("\\.(pdf|png|jpg|jpeg|webp)"))
            throw new IOException("Solo se permiten PDF e imágenes JPG, PNG o WEBP");

        String key = UUID.randomUUID() + extension;
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes()));
        return key;
    }

    public byte[] descargar(String key) {
        validarClave(key);
        return s3.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket).key(key).build()).asByteArray();
    }

    public void eliminar(String key) {
        validarClave(key);
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket).key(key).build());
    }

    private void validarClave(String key) {
        if (key == null || !key.matches("[a-fA-F0-9-]{36}\\.(pdf|png|jpg|jpeg|webp)"))
            throw new IllegalArgumentException("Nombre de archivo no válido");
    }
}
