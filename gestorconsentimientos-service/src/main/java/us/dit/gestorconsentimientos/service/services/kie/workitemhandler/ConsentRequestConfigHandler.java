package us.dit.gestorconsentimientos.service.services.kie.workitemhandler;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Work Item Handler que permite la ejecución de código automático, personalizado.
 * Cuando en el workflow de ejecución de tareas toca una tarea la cual se deba procesar
 * con este WorkItem Handler, este manejador de código va a recibir un WorkItem de la 
 * tarea, y se va a ejecutar con la información que contenga esa carga de trabajo.
 * 
 * @author Isabel 
 */
@Component("ConsentRequestConfig")
public class ConsentRequestConfigHandler implements WorkItemHandler {
	private static final Logger logger = LogManager.getLogger();

	@Value("${fhirserver.location}")
	private String fhirServer;

	@Value("${fhir.questionnaire.request.id}")
	private String requestQuestionnaireId;

	@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		logger.info("IN --- WORK ITEM HANDLER: ConsentRequestConfigHandler");
		logger.info("+ WI gestionado: " + workItem.toString());
		
		Map<String,Object> params = null;
		Map<String,Object> results = new HashMap<String, Object>();
		
		params = workItem.getParameters();		
		logger.info("+ Nombre proceso Entrada: "+(String)params.get("processName"));

		results.put("fhirServer",fhirServer);
		results.put("requestQuestionnaireId",requestQuestionnaireId);
		
		manager.completeWorkItem(workItem.getId(), results);

		logger.info("OUT --- WORK ITEM HANDLER: ConsentRequestConfigHandler");
	}

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {

	}

}
