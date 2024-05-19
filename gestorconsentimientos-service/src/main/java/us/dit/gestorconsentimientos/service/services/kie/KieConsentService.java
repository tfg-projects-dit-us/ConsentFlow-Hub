package us.dit.gestorconsentimientos.service.services.kie;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jbpm.services.api.AdvanceRuntimeDataService;
import org.jbpm.services.api.model.ProcessInstanceWithVarsDesc;
import org.jbpm.services.api.query.QueryResultMapper;
import org.jbpm.services.api.query.model.QueryParam;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.Status;
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
    
    @Autowired
	private AdvanceRuntimeDataService advancedRuntimeDataService;   

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
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentReviewId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_NAME, reviewHumanTaskName));
                        add(QueryParam.in(AdvanceRuntimeDataService.TASK_ATTR_STATUS, new ArrayList<String>(){{
                            add( Status.Created.toString());
                            add( Status.Ready.toString());
                            add( Status.Reserved.toString());
                            add( Status.InProgress.toString());
                        }}));
                        }},
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo("patient", patient));
                        }},
                null,
                (QueryParam) null,
                new QueryContext());
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = processInstanceDesc.getVariables();            
            
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
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentReviewId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_NAME, reviewHumanTaskName));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, Status.Completed.toString()));
                        }},
                new ArrayList<QueryParam>(){{ 
                    add(QueryParam.equalsTo("patient", patient));
                        }},
                null,
                (QueryParam) null,
                new QueryContext());
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = processInstanceDesc.getVariables();            
            
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
                (Boolean) processInstanceVars.get("review")));
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

        List<ProcessInstanceWithVarsDesc> processInstanceParentDescList = null;
        final List<String> processInstanceIdRequestList = new ArrayList<String>();;
        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <RequestedConsent> consentsList = new ArrayList<RequestedConsent>();
 
        // Lista con los ID de instancias de procesos de "ConsentRequest" iniciados por <practitioner>       
        processInstanceParentDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentRequestId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, requestHumanTaskName));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, Status.Completed.toString()));
                        }},
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo("practitioner", practitioner));
                        }},
                null,
                (QueryParam) null,
                new QueryContext());
        
        for (ProcessInstanceWithVarsDesc pi:processInstanceParentDescList){
            processInstanceIdRequestList.add(pi.getId().toString());
        }
        
        // Lista de instancias de procesos "ConsentReview", cuyos padres fueron iniciados
        // por <practitioner>, y que tienen su tarea humana sin completar
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.in(QueryResultMapper.COLUMN_PARENTPROCESSINSTANCEID, processInstanceIdRequestList));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentReviewId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_NAME, reviewHumanTaskName));
                        add(QueryParam.in(AdvanceRuntimeDataService.TASK_ATTR_STATUS, new ArrayList<String>(){{
                            add( Status.Created.toString());
                            add( Status.Ready.toString());
                            add( Status.Reserved.toString());
                            add( Status.InProgress.toString());
                        }}));
                        }},
                null,
                null,
                (QueryParam) null,
                new QueryContext());
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = processInstanceDesc.getVariables();            
            
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
     * Método que devuelve una lista con los consentimientos (solicitudes revisadas,
     * aceptadas o rechazadas), solicitados por un facultativo médico determinado.
     * 
     * @param practitioner facultativo que solicitó los consentimientos que se quieren obtener
     */
    public List <ReviewedConsent> getConsentsByPractitioner(String practitioner){

        List<ProcessInstanceWithVarsDesc> processInstanceParentDescList = null;
        final List<String> processInstanceIdRequestList = new ArrayList<String>();;
        List<ProcessInstanceWithVarsDesc> processInstanceDescList = null;
        Map<String,Object> processInstanceVars = null;
        List <ReviewedConsent> consentsList = new ArrayList<ReviewedConsent>();
 
        // Lista con los ID de instancias de procesos de "ConsentRequest" iniciados por <practitioner>       
        processInstanceParentDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentRequestId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, requestHumanTaskName));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, Status.Completed.toString()));
                        }},
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.equalsTo("practitioner", practitioner));
                        }},
                null,
                (QueryParam) null,
                new QueryContext());
        
        for (ProcessInstanceWithVarsDesc pi:processInstanceParentDescList){
            processInstanceIdRequestList.add(pi.getId().toString());
        }
        
        // Lista de instancias de procesos "ConsentReview", cuyos padres fueron iniciados
        // por <practitioner>
        processInstanceDescList = advancedRuntimeDataService.queryProcessByVariablesAndTask(
                new ArrayList<QueryParam>(){{ 
                        add(QueryParam.in(QueryResultMapper.COLUMN_PARENTPROCESSINSTANCEID, processInstanceIdRequestList));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.PROCESS_ATTR_DEFINITION_ID, processConsentReviewId));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_NAME, reviewHumanTaskName));
                        add(QueryParam.equalsTo(AdvanceRuntimeDataService.TASK_ATTR_STATUS, Status.Completed.toString()));
                        }},
                null,
                null,
                (QueryParam) null,
                new QueryContext());
        
        // Obtención de las variables que forman parte de cada consentimiento
        for (ProcessInstanceWithVarsDesc processInstanceDesc: processInstanceDescList){
            
            processInstanceVars = processInstanceDesc.getVariables();            
            
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
                (Boolean) processInstanceVars.get("review")));
        }

        return consentsList;
    }

}
