package us.dit.gestorconsentimientos.service.services.kie.process;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.task.model.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
	@Value("${kie.user}")
	private String baDefaultUser;
	
    @Value("${kie.task.potentialOwner}")
	private String defaultPotentialOwner;

    /**
     * Método que para una instancia de proceso va a iniciar la tarea humana de revisión
     * de solicitudes de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          iniciar la tarea de revisión de solicitud de consentimiento.
     * @return (Map <String, Object>) vars - variables necesarias para la ejecución de la tarea humana
     */
    public Map <String, Object> initReviewTask(Long processInstanceId){
        
        String fhirServer = null;
        Long requestQuestionnaireResponseId = null;
        Map <String, Object> vars = new HashMap<String,Object>();
        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        // Las variables que indican el servidor fhir y el id que referencian al recurso FHIR Questionnaire
        // que se utiliza para generar la solicitud de consentimiento, se obtienen actualmente de las variables
        // del proceso, sin embargo se podría ahcer de otras maneras, por ejemplo a través del WorkItem, o de
        // la tarea, de sus variables de entrada. Hay que tener claro que cada enfoque puede tener sus pros y 
        // contras, y que implicará distintos caminos e información previa
        fhirServer = (String) processService.getProcessInstanceVariable(processInstanceId, "fhirServer");
        requestQuestionnaireResponseId = (Long) processService.getProcessInstanceVariable(processInstanceId, "requestQuestionnaireResponseId");
        vars.put("fhirServer",fhirServer);
        vars.put("requestQuestionnaireResponseId",requestQuestionnaireResponseId);

        // FIXME Tratar de obtener WI mediante un identificador y no por posición
        // La instancia de Work Item que se está obteniendo es la primera de las múltiples posibles tareas
        // activas que puede haber en el proceso, por cómo está diseñado, solo puede haber una única tarea
        // o nodo activo de manera simultánea, y por tanto un solo WI en esa lista.
        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());

        // Se ha asignado por defecto que la tarea tiene que ser ejecutada por el usuario 
        // defaultPotentialOwner, y por tanto este tiene que cederla al usuario que la va a llevar a cabo.
        // Ese usuario podrá ser uno genérico para la BA, o el usuario real de la BA que la va
        // a ejecutar.
        
        // En la primera vez que se inicia la tarea, el usuario al que está asignada es el configurado por defecto, 
        // pero el resto de veces que se acceda a este método tratando de iniciar la tarea, el usuario al que le 
        // corresponde llevarla a cabo será el paciente.
        if (userTaskInstanceDesc.getActualOwner().equals(defaultPotentialOwner)){
            userTaskService.delegate(userTaskInstanceDesc.getTaskId(), defaultPotentialOwner, baDefaultUser);
        }
        
        // Unicamente se modifica el estado de la tarea en caso de acceder a este método por primera vez, cuando el estado no es Inprogress
        if (!userTaskInstanceDesc.getStatus().equals(Status.InProgress.toString())){
            userTaskService.start(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), baDefaultUser);
        }

        return vars;
    }

    /**
     * Método que va completar la tarea humana de revisión de solicitud de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          completar la tarea de revisión de solicitud de consentimiento.
     * @param results variables que se han generado al realizar la tarea.
     */
    public void completeReviewTask(Long processInstanceId, Map <String, Object> results){

        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);

        // Al completar el workItem de una tarea humana, la tarea humana no se ve completada, 
        // por lo que es necesario completar la tarea y no el workItem.
        //processService.completeWorkItem(workItemInstance.getId(), results);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());
        
        userTaskService.complete(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), baDefaultUser, results);

    }

}
