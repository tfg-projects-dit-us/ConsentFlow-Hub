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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import us.dit.gestorconsentimientos.service.services.kie.KieConsentService;
import us.dit.gestorconsentimientos.service.services.kie.KieManagementService;


/**
 * Controlador que va a atender las operaciones sobre el recurso "/management/processInstances".
 * 
 * @author Javier
 */
@Controller
@ResponseBody
@RequestMapping("/management")
public class ManagementController {

	private static final Logger logger = LogManager.getLogger();

    @Autowired
    KieConsentService kieConsentService;

    @Autowired
    private KieManagementService kieManagementService;

    /**
     * Procesa la petición GET al recurso "/management/deployedUnits", solicitando
     * al servicio gestión del servidor Kie que imprima todas las unidades de despliegue disponibles.
     * 
     * @return "Texto"
     */    
    @GetMapping("/deployedUnits")
    public String getDeployedUnits() {
        // http://localhost:8090/management/deployedUnits
        logger.info("IN --- /management/deployedUnits");

        // Obtención de la lista de unidades de despliegue
        kieManagementService.getDeployedUnits();

        logger.info("OUT --- /management/deployedUnits");
        return "Terminal Output";
    }

    /**
     * Procesa la petición GET al recurso "/management/processInstances", solicitando
     * al servicio de gestión del servidor Kie que imprima todas las instancias de 
     * proceso existentes en la unidad de despliegue.
     * 
     * @param detailed Bandera que indica si se detallan o no las instancias de proceso
     * @return "Texto"
     */    
    @GetMapping("/processInstances")
    public String getProcessInstances(@RequestParam boolean detailed) {
        // http://localhost:8090/management/processInstances?detailed=true
        logger.info("IN --- /management/processInstances");

        // Obtención de la lista de procesos
        kieManagementService.getProcessInstances(detailed);

        logger.info("OUT --- /management/processInstances");
        return "Terminal Output";
    }

    /**
     * Procesa la petición GET al recurso "/management/workItems", solicitando
     * al servicio de gestión del servidor Kie que imprima todos los workItems de una
     * instancia de proceso.
     * 
     * @param processInstanceId Bandera que indica si se detallan o no las instancias de proceso
     * @return "Texto"
     */    
    @GetMapping("/workItems")
    public String getWorkItemsByProcessInstanceId(@RequestParam Long processInstanceId) {
        
        logger.info("IN --- /management/workItems");

        // Obtención de la lista de WorkItems
        kieManagementService.getWorkItemsByProcessInstanceId(processInstanceId);

        logger.info("OUT --- /management/workItems");
        return "Terminal Output";
    }

    /**
     * Procesa la petición GET al recurso "/management/tasks", solicitando
     * al servicio de gestión del servidor Kie que imprima todas las tareas de una
     * instancia de proceso.
     * 
     * @param processInstanceId Bandera que indica si se detallan o no las instancias de proceso
     * @return "Texto"
     */    
    @GetMapping("/tasks")
    public String getTasksByProcessInstanceId(@RequestParam Long processInstanceId) {
        
        logger.info("IN --- /management/tasks");

        // Obtención de la lista de tareas
        kieManagementService.getTasksByProcessInstanceId(processInstanceId);

        logger.info("OUT --- /management/tasks");
        return "Terminal Output";
    }

    /**
     * Procesa la petición GET al recurso "/management/vars", solicitando
     * al servicio de gestión del servidor Kie que imprima todas las variables de una
     * instancia de proceso.
     * 
     * @param processInstanceId Bandera que indica si se detallan o no las instancias de proceso
     * @return "Texto"
     */    
    @GetMapping("/vars")
    public String getVarssByProcessInstanceId(@RequestParam Long processInstanceId) {
        
        logger.info("IN --- /management/vars");

        // Obtención de la lista de tareas
        kieManagementService.getVarsByProcessInstanceId(processInstanceId);

        logger.info("OUT --- /management/vars");
        return "Terminal Output";
    }

}
