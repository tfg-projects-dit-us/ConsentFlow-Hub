package us.dit.gestorconsentimientos.service.services.kie.process;

import java.util.HashMap;
import java.util.Map;

import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.kie.api.runtime.process.WorkItem;
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
        String requestQuestionnaireResponseId = null;
        Map <String, Object> vars = new HashMap<String,Object>();
        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        //TODO ¿Sería más correcto obtener las variables de la tarea (Son variables de entrada), y no del proceso?

        fhirServer = (String) processService.getProcessInstanceVariable(processInstanceId, "fhirServer");
        requestQuestionnaireResponseId = (String) processService.getProcessInstanceVariable(processInstanceId, "requestQuestionnaireResponseId");
        vars.put("fhirServer",fhirServer);
        vars.put("requestQuestionnaireResponseId",requestQuestionnaireResponseId);

        workItemInstance = processService.getWorkItemByProcessInstance(processInstanceId).get(0);
        userTaskInstanceDesc = runtimeDataService.getTaskByWorkItemId(workItemInstance.getId());

        // Se ha asignado por defecto que la tarea tiene que ser ejecutada por el usuario 
        // defaultPotentialOwner, y por tanto este tiene que cederla al usuario que la va a llevar a cabo.
        // Ese usuario podrá ser uno genérico para la BA, o el usuario real de la BA que la va
        // a ejecutar.
        userTaskService.delegate(processInstanceId, defaultPotentialOwner, baDefaultUser);
        
        userTaskService.start(userTaskInstanceDesc.getDeploymentId(),userTaskInstanceDesc.getTaskId(), baDefaultUser);

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
