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
package us.dit.gestorconsentimientos.service.services.kie.process;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.kie.api.runtime.process.WorkItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Servicio que se encarga de comunicarse con el servidor de procesos, para gestionar el 
 * proceso de Revisión de Solicitudes de Consentimientos.
 * 
 * @author Javier
 */
@Service
public class ConsentReviewProcessService {

    @Autowired
    private RuntimeDataService runtimeDataService;
    // https://docs.jbpm.org/7.74.1.Final/jbpm-docs/html_single/#service-runtime-data-con_jbpm-doc

    @Autowired
    private ProcessService processService;

    @Autowired
    private UserTaskService userTaskService;    

    /**
     * Método que para una instancia de proceso va a iniciar la tarea humana de revisión
     * de solicitudes de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          iniciar la tarea de revisión de solicitud de consentimiento.
     * @return (Map <String, Object>) vars - variables necesarias para la ejecución de la tarea humana
     */
    public Map <String, Object> initReviewTask(Long processInstanceId, String patient){
        
        String fhirServer = null;
        Long requestQuestionnaireResponseId = null;
        Map <String, Object> vars = new HashMap<String,Object>();
        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        // Las variables que indican el servidor fhir y el id que referencian al recurso FHIR Questionnaire
        // que se utiliza para generar la solicitud de consentimiento, se obtienen actualmente de las variables
        // del proceso, sin embargo se podría hacer de otras maneras, por ejemplo a través del WorkItem, o de
        // la tarea, de sus variables de entrada. Hay que tener claro que cada enfoque puede tener sus pros y 
        // contras, y que implicará distintos caminos e información previa
        fhirServer = (String) processService.getProcessInstanceVariable(processInstanceId, "fhirServer");
        requestQuestionnaireResponseId = (Long) processService.getProcessInstanceVariable(processInstanceId, "requestQuestionnaireResponseId");
        vars.put("fhirServer",fhirServer);
        vars.put("requestQuestionnaireResponseId",requestQuestionnaireResponseId);

        // La instancia de Work Item que se está obteniendo es la primera de las múltiples posibles tareas
        // activas que puede haber en el proceso, por cómo está diseñado, solo puede haber una única tarea
        // o nodo activo de manera simultánea, y por tanto un solo WI en esa lista.
        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());

      
        // Unicamente se modifica el estado de la tarea en caso de acceder a este método por primera vez, cuando el estado no es Inprogress
        userTaskService.start(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), patient);

        return vars;
    }

    /**
     * Método que va completar la tarea humana de revisión de solicitud de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          completar la tarea de revisión de solicitud de consentimiento.
     * @param results variables que se han generado al realizar la tarea.
     */
    public void completeReviewTask(Long processInstanceId, Map <String, Object> results, String patient){

        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);

        // Al completar el workItem de una tarea humana, la tarea humana no se ve completada, 
        // por lo que es necesario completar la tarea y no el workItem.
        //processService.completeWorkItem(workItemInstance.getId(), results);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());
        
        userTaskService.complete(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), patient, results);

    }

}
