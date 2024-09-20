package us.dit.gestorconsentimientos.service.services.kie;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.api.AdvanceRuntimeDataService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceWithVarsDesc;
import org.jbpm.services.api.model.VariableDesc;
import org.jbpm.services.api.query.QueryResultMapper;
import org.jbpm.services.api.query.model.QueryParam;
import org.kie.api.runtime.query.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import us.dit.gestorconsentimientos.model.RequestedConsent;
import us.dit.gestorconsentimientos.model.ReviewedConsent;


/**
 * Servicio que se encarga de comunicarse con el servidor de procesos, para obtener 
 * información sobre los consentimientos existentes, a base de consultar información 
 * sobre procesos y tareas.
 * 
 * @author Javier
 */
@Service
public class KieConsentService {

    // https://docs.jbpm.org/7.74.1.Final/jbpm-docs/html_single/#service-runtime-data-con_jbpm-doc
	private static final Logger logger = LogManager.getLogger();

    @Autowired
	private AdvanceRuntimeDataService advancedRuntimeDataService;

    @Autowired
	private RuntimeDataService runtimeDataService;

    @Value("${kie.task.ConsentReviewGeneration.name}")
    private String reviewHumanTaskName;

    @Value("${kie.task.ConsentRequestGeneration.name}")
    private String requestHumanTaskName;

    @Value("${kie.process.ConsentReview.id}")
	private String processConsentReviewId;

    @Value("${kie.process.ConsentRequest.id}")
    private String processConsentRequestId;

    @Value("${spring.datasource.url}")
    private String dataSource;


    public Long getReviewProcessInstanceIdByRequestQuestionnaireResponseId(Long requestQuestionnaireResponseId){

        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;

        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
            new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                    //add(QueryParam.equalsTo(QueryResultMapper.COLUMN_PARENTID parentProcessInstanceId));
                    //add(QueryParam.equalsTo(QueryResultMapper.COLUMN_PARENTPROCESSINSTANCEID parentProcessInstanceId));
                    }},
            new ArrayList<QueryParam>(){{ 
                add(QueryParam.equalsTo("requestQuestionnaireResponseId", (String) requestQuestionnaireResponseId.toString()));
                    }},
            new QueryContext());

        return processInstanceDescList.get(0).getId();
        
    }

    public RequestedConsent getRequestedConsentByConsentReviewInstanceId(Long processInstanceId){
        
        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        ProcessInstanceWithVarsDesc processInstanceDesc = null;
        Map<String,Object> processInstanceVars = null;
        RequestedConsent requestedConsent = null;

        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
            new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                    add(QueryParam.equalsTo(QueryResultMapper.COLUMN_PROCESSINSTANCEID, processInstanceId));
                    }},
            null,
            new QueryContext());
        
        if (processInstanceDescList.size() != 0) {
        	logger.info("Se han encontrado "+processInstanceDescList.size()+" consentimientos");
            processInstanceDesc = processInstanceDescList.get(0);
            
            processInstanceVars = processInstanceDesc.getVariables();
    
            requestedConsent = new RequestedConsent(
                (Long) processInstanceDesc.getId(),
                (String) processInstanceVars.get("fhirServer"),
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")), 
                (Date) processInstanceDesc.getDataTimeStamp(),
                (String) processInstanceVars.get("practitioner"), 
                (String) processInstanceVars.get("patient"));
        }
        
        return requestedConsent;
    }


    public ReviewedConsent getReviewedConsentByConsentReviewInstanceId(Long processInstanceId){
        
        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        ProcessInstanceWithVarsDesc processInstanceDesc = null;
        Map<String,Object> processInstanceVars = null;
        ReviewedConsent reviewedConsent = null;

        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
            new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                    add(QueryParam.equalsTo(QueryResultMapper.COLUMN_PROCESSINSTANCEID, processInstanceId));
                    }},
            null,
            new QueryContext());
        
        if (processInstanceDescList.size() != 0) {
        	logger.info("Se han encontrado "+processInstanceDescList.size()+" revisiones");
            processInstanceDesc = processInstanceDescList.get(0);
            
            processInstanceVars = processInstanceDesc.getVariables();

            reviewedConsent = new ReviewedConsent(
                (Long) processInstanceDesc.getId(),
                (String) processInstanceVars.get("fhirServer"),
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")), 
                (Date) processInstanceDesc.getDataTimeStamp(),
                (String) processInstanceVars.get("practitioner"), 
                (String) processInstanceVars.get("patient"),
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireId")),
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireResponseId")),
                (Boolean) Boolean.parseBoolean( (String) processInstanceVars.get("review")),
                (Long) Long.parseLong("0")); //TODO extraer del proceso el ID del consentimiento generado a partir de reviewQuestionnaireResponseId
        }
        
        return reviewedConsent;
    }    


    /**
     * Método que devuelve una lista con las solicitudes de consentimiento, pendientes 
     * de ser revisadas, dirigidas para un paciente.
     * 
     * @param patient paciente para el que se obtienen las solicitudes pendientes
     */
    public List <RequestedConsent> getRequestedConsentsByPatient(String patient){

        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <RequestedConsent> consentsList = new ArrayList<RequestedConsent>();
        
        // Lista de instancias de procesos "ConsentReview", cuyos padres fueron iniciados
        // por <practitioner>, y que tienen su tarea humana sin completar
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                        add(QueryParam.equalsTo("status", 1));
                        }},
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo("patient", (String) patient));
                        }},
                new QueryContext()
                );
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = runtimeDataService.getVariablesCurrentState(processInstanceDesc.getId()).stream().collect(Collectors.toMap((VariableDesc::getVariableId), VariableDesc::getNewValue));
            
            consentsList.add(new RequestedConsent(
                (Long) processInstanceDesc.getId(),
                (String) processInstanceVars.get("fhirServer"),
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")), 
                (Date) processInstanceDesc.getDataTimeStamp(),
                (String) processInstanceVars.get("practitioner"), 
                (String) processInstanceVars.get("patient")));
        }

        return consentsList;
    }

    /**
     * Método que devuelve una lista con los consentimientos (solicitudes revisadas)
     * otorgados por un paciente.
     * 
     * @param patient paciente para el que se obtienen los consentimientos
     */
    public List <ReviewedConsent> getConsentsByPatient(String patient){

        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <ReviewedConsent> consentsList = new ArrayList<ReviewedConsent>();
        
        // Lista de instancias de procesos "ConsentReview", cuyos pacientes son <patient>
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                        add(QueryParam.equalsTo("status", 2));
                        }},
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo("patient", (String) patient));
                        }},
                new QueryContext());
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = runtimeDataService.getVariablesCurrentState(processInstanceDesc.getId()).stream().collect(Collectors.toMap((VariableDesc::getVariableId), VariableDesc::getNewValue));
            
            consentsList.add(new ReviewedConsent(
                (Long) processInstanceDesc.getId(),
                (String) processInstanceVars.get("fhirServer"),
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")),
                (Date) processInstanceDesc.getDataTimeStamp(),
                (String) processInstanceVars.get("practitioner"), 
                (String) processInstanceVars.get("patient"), 
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireResponseId")),
                (Boolean) Boolean.parseBoolean( (String) processInstanceVars.get("review")),
                (Long) Long.parseLong("0"))); //TODO extraer del proceso el ID del consentimiento generado a partir de reviewQuestionnaireResponseId
        }

        return consentsList;
    }

    /**
     * Método que devuelve una lista con las solicitudes de consentimiento emitidas, por
     * un facultativo médico determinado y que están pendientes de revisión. 
     * 
     * @param practitioner facultativo que emitió las solicitudes de consentimiento, aún
     *                     pendientes de revisar, que se quieren obtener
     */
    public List <RequestedConsent> getRequestedConsentsByPractitioner(String practitioner){
    	logger.info("Buscando los consentimientos pendientes de "+practitioner);

        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <RequestedConsent> consentsList = new ArrayList<RequestedConsent>();
 
        // Lista de instancias de procesos "ConsentReview", cuyos padres fueron iniciados
        // por <practitioner>, y que tienen su tarea humana sin completar
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                    add(QueryParam.equalsTo("status", 1));
                        }},
                    new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo("practitioner", (String) practitioner));
                        }},
                new QueryContext());

        if (!processInstanceDescList.isEmpty()){
            // Obtención de las variables que forman parte de cada consentimiento
            for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){

                processInstanceVars = runtimeDataService.getVariablesCurrentState(processInstanceDesc.getId()).stream().collect(Collectors.toMap((VariableDesc::getVariableId), VariableDesc::getNewValue));


                if (processInstanceVars != null){
                    consentsList.add(new RequestedConsent(
                        (Long) processInstanceDesc.getId(),
                        (String) processInstanceVars.get("fhirServer"),
                        (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                        (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")),
                        (Date) processInstanceDesc.getDataTimeStamp(),
                        (String) processInstanceVars.get("practitioner"), 
                        (String) processInstanceVars.get("patient")));
                }
            }
        }
        return consentsList;
    }

    /**
     * Método que devuelve una lista con los consentimientos (solicitudes revisadas,
     * aceptadas o rechazadas), solicitados por un facultativo médico determinado.
     * 
     * @param practitioner facultativo que solicitó los consentimientos que se quieren obtener
     */
    public List <ReviewedConsent> getConsentsByPractitioner(String practitioner){
    	logger.info("Obteniendo los consentimientos pendientes de "+practitioner);
    	
        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <ReviewedConsent> consentsList = new ArrayList<ReviewedConsent>();

        // Lista de instancias de procesos "ConsentReview", cuyos padres fueron iniciados
        // por <practitioner>

        //runtimeDataService.getProcessInstancesByVariable(practitioner, null, null);
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariables(
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, (String) processConsentReviewId));
                    add(QueryParam.equalsTo("status", 2));
                        }},
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo("practitioner", (String) practitioner));
                        }},
                new QueryContext());

        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = runtimeDataService.getVariablesCurrentState(processInstanceDesc.getId()).stream().collect(Collectors.toMap((VariableDesc::getVariableId), VariableDesc::getNewValue));
            
            consentsList.add(new ReviewedConsent(
                (Long) processInstanceDesc.getId(),
                (String) processInstanceVars.get("fhirServer"),
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("requestQuestionnaireResponseId")), 
                (Date) processInstanceDesc.getDataTimeStamp(),
                (String) processInstanceVars.get("practitioner"), 
                (String) processInstanceVars.get("patient"), 
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireId")), 
                (Long) Long.parseLong( (String) processInstanceVars.get("reviewQuestionnaireResponseId")),
                (Boolean) Boolean.parseBoolean( (String) processInstanceVars.get("review")),
                (Long) Long.parseLong("0"))); //TODO Extraer del proceso el ID del consentimiento generado a partir de reviewQuestionnaireResponseId
        }

        return consentsList;
    }

}
