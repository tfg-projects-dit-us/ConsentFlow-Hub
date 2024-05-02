package us.dit.gestorconsentimientos.service.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;


/**
 * Controlador que va a atender las operaciones sobre el recurso "/".
 * 
 * @author Javier
 */
@Controller
public class IndexController {

	private static final Logger logger = LogManager.getLogger();

    /**
     * Procesa la petición GET al recurso "/", indicando la plantilla thymeleaf que se 
     * tiene que seguir para generar el contenido html que se debe mostrar como respuesta.
     * 
     * @param model contendrá los atributos que necesita la plantilla thymeleaf
     * @return "index" plantilla thymeleaf
     */
    @GetMapping("/")
    public String getIndex(Model model) {

        logger.info("IN --- /");

        logger.info("OUT --- /");

        return "index";
    }
    
}
