package com.pulsos.medicina.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
@Service
public class GoogleTokenEncryptionService {
 private final SecretKeySpec key;
 private final SecureRandom random=new SecureRandom();
 public GoogleTokenEncryptionService(@Value("${GOOGLE_TOKEN_ENCRYPTION_KEY}") String secret){
  byte[] raw=Base64.getDecoder().decode(secret);
  if(raw.length!=32) throw new IllegalArgumentException("GOOGLE_TOKEN_ENCRYPTION_KEY debe ser Base64 de 32 bytes");
  key=new SecretKeySpec(raw,"AES");
 }
 public String encrypt(String text){try{
  byte[] iv=new byte[12];random.nextBytes(iv);
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));
  byte[] encrypted=c.doFinal(text.getBytes(StandardCharsets.UTF_8));
  byte[] result=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,result,0,iv.length);System.arraycopy(encrypted,0,result,iv.length,encrypted.length);
  return Base64.getEncoder().encodeToString(result);
 }catch(Exception e){throw new IllegalStateException("Error cifrando token",e);}}
 public String decrypt(String text){try{
  byte[] raw=Base64.getDecoder().decode(text);if(raw.length<29)throw new IllegalArgumentException("Token inválido");
  byte[] iv=java.util.Arrays.copyOfRange(raw,0,12);byte[] encrypted=java.util.Arrays.copyOfRange(raw,12,raw.length);
  Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
  return new String(c.doFinal(encrypted),StandardCharsets.UTF_8);
 }catch(Exception e){throw new IllegalStateException("Error descifrando token",e);}}
}
