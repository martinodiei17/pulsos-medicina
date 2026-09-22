package com.pulsos.medicina.controller;

import com.pulsos.medicina.model.Rol;
import com.pulsos.medicina.model.Usuario;
import com.pulsos.medicina.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos(null));
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("todosLosRoles", Rol.values());
        model.addAttribute("esEdicion", false);
        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Long id, Model model) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id).orElse(null);
            if (usuario == null) {
                return "redirect:/usuarios";
            }
            model.addAttribute("usuario", usuario);
            model.addAttribute("todosLosRoles", Rol.values());
            model.addAttribute("esEdicion", true);
            return "usuarios/formulario";
        } catch (Exception e) {
            return "redirect:/usuarios";
        }
    }

    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        // Llamada limpia al método oficial de tu servicio
        usuarioService.guardar(usuario);
        return "redirect:/usuarios";
    }
}
