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
        String redirect = "redirect:/paciente";

        // Obtención del usuario que opera sobre el recurso para toda la sesión
        auth = SecurityContextHolder.getContext().getAuthentication();
        userDetails = (UserDetails) auth.getPrincipal();

        logger.info(" + user: " + userDetails.getUsername());
        logger.info(" + Roles: " + auth.getAuthorities().toString());        

        // Los usuarios que tengan el rol de facultativo serán redirigidos a su pestaña
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_FACULTATIVO"))) {
            redirect = "redirect:/facultativo";
            logger.info(" + El usuario es un facultativo");
        }        

        logger.info("OUT --- /menu");
        return redirect;
    }
    
}
