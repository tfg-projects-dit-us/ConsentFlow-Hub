package us.dit.gestorconsentimientos.service.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


/**
 * Controlador que va a atender las operaciones sobre el recurso "/menu".
 * 
 * @author Javier
 */
@Controller
public class MenuController {

	private static final Logger logger = LogManager.getLogger();

    /**
     * Procesa la petición GET al recurso "/menu", redirigiendo a un recurso u otro según
     * si el usuario es un facultativo o un paciente.
     * 
     * @return "redirect:/facultativo" ó "redirect:/paciente"
     */    
    @GetMapping("/menu")
    public String getIndex() {
        
        logger.info("IN --- /menu");

        Authentication auth = null;
        UserDetails userDetails = null;

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();
        logger.info(" + user: " + userDetails.getUsername());

        //TODO: Averiguar si el usuario es facultativo o no

        //TODO: Redirigir a un recurso u otro según si el usuario es facultativo o no

        logger.info("OUT --- /menu");
        return "redirect:/facultativo";
    }
    
}
