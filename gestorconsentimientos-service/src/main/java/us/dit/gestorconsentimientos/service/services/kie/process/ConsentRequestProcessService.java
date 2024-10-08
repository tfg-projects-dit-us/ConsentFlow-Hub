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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.kie.api.runtime.process.WorkItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio que se encarga de comunicarse con el servidor de procesos, para gestionar el 
 * proceso de Solicitud de Consentimientos.
 * 
 * @author Javier
 */
@Service
public class ConsentRequestProcessService {

	private static final Logger logger = LogManager.getLogger();

    @Autowired
    private ProcessService processService;
    
    @Autowired
    private UserTaskService userTaskService;

    @Autowired
    private RuntimeDataService runtimeDataService;

    @Value("${kie.deployment.groupid}")
    private String deploymentGroupId;
    
    @Value("${kie.deployment.artifactid}")
    private String deploymentArtifactId;
    
    @Value("${kie.deployment.version}")
    private String deploymentVersion;
    
    @Value("${kie.process.ConsentRequest.id}")
	private String processId;

	@Value("${kie.user}")
	private String baDefaultUser;
	
    @Value("${kie.task.potentialOwner}")
	private String defaultPotentialOwner;

    /**
     * Método que va a crear una instancia del proceso. 
     * Para ello va a crear una unidad de despliegue.
     *       
     * @param params parámetros de entrada para el proceso 
     * @return (Long) processInstanceId
     */
    public Long createProcessInstance(Map<String,Object> params){
     
        logger.info("Entrando en el metodo para crear una instancia de proceso");
        Long processInstanceId = null;

        // Instanciación del proceso
        //si la invocación se hace desde PractitionerController sólo contiene "practitioner", el nombre del usuario
        processInstanceId = processService.startProcess("gestorconsentimientos-kjar", processId, params);
        logger.info("> Desplegada instancia de proceso ConsentRequest con ID: " + processInstanceId);
        
        /**
         * No pongas código en los logs, si da error ese código en lugar de ayudar a descubrir errores los introducen...
         */
        //logger.info(processService.getProcessInstance(deploymentUnitId, processInstanceId));
        //logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).toString());
        //logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).getInitiator());
        //logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).getState());
        
        return processInstanceId;
    }

    /**
     * Método que va a iniciar la tarea humana para la creación de la solicitud de 
     * consentimiento, en una instancia del proceso concreta.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          iniciar la tarea de solicitud de consentimiento.
     * @return (Map <String, Object>) vars - variables necesarias para la ejecución de la tarea humana
     */
    public Map <String, Object> initRequestTask(Long processInstanceId,String practitioner){
        logger.info("INICIANDO tarea en el proceso "+processInstanceId);
        String fhirServer = null;
        Long requestQuestionnaireId = null;
        Map <String, Object> vars = new HashMap<String,Object>();
        List<WorkItem> workItemInstanceList = null;
        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        //FIXME ¿Sería más correcto obtener las variables de la tarea (Son variables de entrada), y no del proceso?

        // Las variables que indican el servidor fhir y el id que referencian al recurso FHIR Questionnaire
        // que se utiliza para generar la solicitud de consentimiento, se obtienen actualmente de las variables
        // del proceso, sin embargo se podría hacer de otras maneras, por ejemplo a través del WorkItem, o de
        // la tarea, de sus variables de entrada. Hay que tener claro que cada enfoque puede tener sus pros y 
        // contras, y que implicará distintos caminos e información previa
        fhirServer = (String) processService.getProcessInstanceVariable(processInstanceId, "fhirServer");
        logger.info("Obteniendo la variable fhirServer del proceso "+fhirServer);
        requestQuestionnaireId = (Long) processService.getProcessInstanceVariable(processInstanceId, "requestQuestionnaireId");
        logger.info("Obteniendo la variable id del cuestionario del proceso "+requestQuestionnaireId);
        vars.put("fhirServer",fhirServer);
        vars.put("requestQuestionnaireId",requestQuestionnaireId);

        
        // La instancia de Work Item que se está obteniendo es la primera de las múltiples posibles tareas
        // activas que puede haber en el proceso, por cómo está diseñado, solo puede haber una única tarea
        // o nodo activo de manera simultánea, y por tanto un solo WI en esa lista. Además se comprueba que 
        // esos WI pertenezcan a la tarea 'ConsentRequestGeneration'

        workItemInstanceList = processService.getWorkItemByProcessInstance(processInstanceId);
        workItemInstance = workItemInstanceList.stream().filter(wi->wi.getParameter("TaskName").equals("ConsentRequestGeneration")).collect(Collectors.toList()).get(0);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());
        
        
        userTaskService.start(userTaskInstanceDesc.getTaskId(),practitioner);
        
        return vars;
    }

    /**
     * Método que va completar la tarea humana de creación de solicitud de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          completar la tarea de solicitud de consentimiento.
     * @param results variables que se han generado al realizar la tarea.
     */
    public void completeRequestTask(Long processInstanceId, Map <String, Object> results, String practitioner){

        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);

        // Al completar el workItem de una tarea humana, la tarea humana no se ve completada, 
        // por lo que es necesario completar la tarea y no el workItem.
        //processService.completeWorkItem(workItemInstance.getId(), results);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());
        
        userTaskService.complete(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), practitioner, results);
    }

}
