
/**
*  This file is part of ConsentFlow Hub: a flexible solution for the eficiente management of consents in healthcare systems.
*  Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
*
*  ConsentFlow Hub is free software: you can redistribute it and/or
*  modify it under the terms of the GNU General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or (at
*  your option) any later version.
*
*  ConsentFlow Hub is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
*  Public License for more details.
*
*  You should have received a copy of the GNU General Public License along
*  with ConsentFlow Hub. If not, see <https://www.gnu.org/licenses/>.
**/
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
