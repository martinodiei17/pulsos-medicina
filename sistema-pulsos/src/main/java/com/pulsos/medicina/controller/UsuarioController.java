package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String index(@RequestParam(value = "q", required = false) String q, Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos(q));
        model.addAttribute("busqueda", q);
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String formularioNuevo(Model model) {
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        model.addAttribute("usuario", usuario);
        model.addAttribute("todosLosRoles", Rol.values());
        model.addAttribute("esEdicion", false);
        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String formularioEditar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        model.addAttribute("usuario", usuario);
        model.addAttribute("todosLosRoles", Rol.values());
        model.addAttribute("esEdicion", true);
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario, @RequestParam(value = "passwordPlana", required = false) String passwordPlana) {
        usuarioService.guardarOActualizar(usuario, passwordPlana);
        return "redirect:/usuarios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam("activo") boolean activo) {
        usuarioService.cambiarEstado(id, activo);
        return "redirect:/usuarios";
    }
}