package us.dit.gestorconsentimientos.service.services.kie.process;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.UserTaskService;
import org.jbpm.services.api.model.DeploymentUnit;
import org.jbpm.kie.services.impl.KModuleDeploymentUnit;
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
    private DeploymentService deploymentService;

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

    private String deploymentUnitId;

    private void deployBusinessAssests(){

        DeploymentUnit deploymentUnit = null;
        
        if (this.deploymentService.getDeployedUnits().iterator().hasNext()){
        
            deploymentUnitId = this.deploymentService.getDeployedUnits().iterator().next().getDeploymentUnit().getIdentifier();

            //TODO ¿Se crean más unidades de despliegue? ¿Es la forma correcta de utilizar siempre la misma?

        }else{
            // Cración de la unidad de despliegue que contiene todos los activos de negocio
            deploymentUnit = new KModuleDeploymentUnit(deploymentGroupId, deploymentArtifactId, deploymentVersion);
            deploymentUnitId = deploymentUnit.getIdentifier();

            // Despliegue de la unidad de depligue que contiene todos los activos de negocio
            deploymentService.deploy(deploymentUnit);
            deploymentService.activate(deploymentUnitId);
            logger.info("> Desplegada unidad con ID: " + deploymentUnitId);
            //logger.info("¿Está desplegada la unidad con ID " +deploymentUnitId+ "?: " + deploymentService.isDeployed(deploymentUnitId));
            //Collection<DeployedUnit> deployedUnits = deploymentService.getDeployedUnits();
            //logger.info("Deployment Units: ");
            //logger.info(deployedUnits);
        }
    }

    /**
     * Método que va a crear una instancia del proceso. 
     * Para ello va a crear una unidad de despliegue.
     * 
     * @param params parámetros de entrada para el proceso
     * @return (Long) processInstanceId
     */
    public Long createProcessInstance(Map<String,Object> params){
        
        Long processInstanceId = null;

        // Despliegue de los activos de negocio
        deployBusinessAssests();

        // Instanciación del proceso
        processInstanceId = processService.startProcess(deploymentUnitId, processId, params);
        logger.info("> Desplegada instancia de proceso ConsentRequest con ID: " + processInstanceId);
        logger.info(processService.getProcessInstance(deploymentUnitId, processInstanceId));
        logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).toString());
        logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).getInitiator());
        logger.info(runtimeDataService.getProcessInstanceById(processInstanceId).getState());
        
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
    public Map <String, Object> initRequestTask(Long processInstanceId){
        
        String fhirServer = null;
        Long requestQuestionnaireId = null;
        Map <String, Object> vars = new HashMap<String,Object>();
        WorkItem workItemInstance = null;
        UserTaskInstanceDesc userTaskInstanceDesc = null;

        //TODO ¿Sería más correcto obtener las variables de la tarea (Son variables de entrada), y no del proceso?

        fhirServer = (String) processService.getProcessInstanceVariable(processInstanceId, "fhirServer");
        requestQuestionnaireId = (Long) processService.getProcessInstanceVariable(processInstanceId, "requestQuestionnaireId");
        vars.put("fhirServer",fhirServer);
        vars.put("requestQuestionnaireId",requestQuestionnaireId);

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
     * Método que va completar la tarea humana de creación de solicitud de consentimiento.
     * 
     * @param processInstanceId ID de la instancia del proceso para la que se va a 
     *                          completar la tarea de solicitud de consentimiento.
     * @param results variables que se han generado al realizar la tarea.
     */
    public void completeRequestTask(Long processInstanceId, Map <String, Object> results){

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
