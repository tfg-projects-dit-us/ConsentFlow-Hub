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
package us.dit.gestorconsentimientos.service.services.kie;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jbpm.services.api.DeploymentService;
import org.jbpm.services.api.ProcessService;
import org.jbpm.services.api.RuntimeDataService;
import org.jbpm.services.api.model.DeployedUnit;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.model.VariableDesc;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.query.QueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio que se encarga de comunicarse con el servidor de procesos, para obtener 
 * información y monitorizar distintos elementos. 
 *  
 * @author Javier
 */
@Service
public class KieManagementService {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private RuntimeDataService runtimeDataService;

    @Autowired
    private ProcessService processService;

    /**
     * Método que devuelve una lista con todas las unidades de despliegue existentes en 
     * el servidor kie.
     */    
    public void getDeployedUnits(){
        Collection<DeployedUnit> deployedUnitCollection= null;
        
        deployedUnitCollection = deploymentService.getDeployedUnits();

        logger.info("Lista de unidades de depliegue disponibles en el servidor: ");
        if (deployedUnitCollection!=null){
            for (DeployedUnit deployedUnit:deployedUnitCollection){
                logger.info(" - " + deployedUnit.toString());
            }
        }
    }

    /**
     * Método que devuelve una lista con las descripciones de las instancias de proceso
     * existentes en la unidad de despliegue.
     * 
     * @param detailed bandera que indica al método si extender información sobre las instancias de proceso o no 
     */
    public void getProcessInstances(boolean detailed){

        Collection<ProcessInstanceDesc> processInstanceDescList = null;
            
            processInstanceDescList = runtimeDataService.getProcessInstances(new QueryContext());
            
            if (detailed != true){
                logger.info("Lista de todas las instancias de proceso de la unidad de despliegue: ");
                if (processInstanceDescList!=null){
                    for(ProcessInstanceDesc processInstanceDesc:processInstanceDescList){
                        logger.info(" - " + processInstanceDesc.toString());
                    }
                }
            }else{
                logger.info("Lista de todas las instancias de proceso de la unidad de despliegue: ");
                for(ProcessInstanceDesc processInstanceDesc:processInstanceDescList){
                    logger.info(" - " + processInstanceDesc.toString());
                    getTasksByProcessInstanceId(processInstanceDesc.getId());
                    getVarsByProcessInstanceId(processInstanceDesc.getId());
                    getWorkItemsByProcessInstanceId(processInstanceDesc.getId());
                }
            }     
    }

    /**
     * Método que devuelve una lista con los WorkItems de una instancia de proceso.
     *  
     * NOTA: Funciona cuando los procesos no han finalizado, en primer lugar, debido a 
     * que los workitem desaparecen al ser un elemento que tiene sentido unicamente en 
     * ejecución, un medio de comunicación entre JBPM y la aplicación. 
     * En segundo lugar, cualquier método en el que se utilice la unidad de depliegue 
     * como apoyo para consultar datos no va a funcionar correctamente con elementos ya 
     * completados, puesto que estos dejan de estar "desplegados" dentro de la unidad
     * de depliegue una vez terminados.
     * 
     * @param processInstanceId Id de la instancia de proceso de la que se obtienen los WorkItems
     */
    public void getWorkItemsByProcessInstanceId(Long processInstanceId){

        try {

            String deploymentUnitId = null;
            List<WorkItem> workItemList = null;
    
            // Obtención de la unidad de despliegue en la que se están desplegando las 
            // instancias de los procesos que se quieren obtener.
            deploymentUnitId = this.deploymentService.getDeployedUnits().iterator().next().getDeploymentUnit().getIdentifier();
    
            // Obtención de la Lista de Work Items de una instancia de proceso
            workItemList = processService.getWorkItemByProcessInstance(deploymentUnitId, processInstanceId);
    
            logger.info("  - Lista de los WorkItem de la Instancia de Proceso <" + processInstanceId.toString() + "> :");
            if (workItemList!=null){            
                for (WorkItem workItem:workItemList){
                    logger.info("   +" + workItem.toString());
                }
            }
            
        } catch (Exception e) {
            // handle exception
        }

    }

    /**
     * Método que devuelve una lista con las tareas de una instancia de proceso.
     * 
     * @param processInstanceId Id de la instancia de proceso de la que se obtienen las tareas
     */    
    public void getTasksByProcessInstanceId(Long processInstanceId){

        List<Long> taskIdList = null;
        
        // Obtención de la Lista de tareas de una instancia de proceso
        taskIdList = runtimeDataService.getTasksByProcessInstanceId(processInstanceId);
        
        logger.info("  - Lista de tareas pertenecientes a la instancia de proceso <" + processInstanceId.toString() + "> :");
        if (taskIdList!=null){
            for(Long taskId:taskIdList){
                logger.info("   +" + runtimeDataService.getTaskById(taskId).toString());
            }
        }

    }

    /**
     * Método que devuelve una lista con las variables de una instancia de proceso.
     * 
     * @param processInstanceId Id de la instancia de proceso de la que se obtienen las tareas
     */    
    public void getVarsByProcessInstanceId(Long processInstanceId){

        Collection<VariableDesc> varsCollection = null;
        Iterator<VariableDesc> iterator = null;

        // Obtención de la Lista de tareas de una instancia de proceso
        varsCollection = runtimeDataService.getVariablesCurrentState(processInstanceId);

        logger.info("  - Lista de Variables pertenecientes a la instancia de proceso <" + processInstanceId.toString() + "> :");
        
        iterator = varsCollection.iterator();        
        while (iterator.hasNext()){
            logger.info("   +" + iterator.next().toString());
        }


    }

}
