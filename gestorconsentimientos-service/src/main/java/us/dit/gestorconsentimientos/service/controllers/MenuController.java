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
