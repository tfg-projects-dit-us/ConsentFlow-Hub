# GestorConsentimientos
Aplicación web para la gestión de consentimientos médicos, creada utilizando el kit de desarrollo JBPM, el cual permite la construcción de aplicaciones que funcionan bajo el paradigma de trabajo BPM.


## Ejecución
Para ejecutar la aplicación basta con ejecutar el script "launch.sh" del proyecto gestorconsentimientos-service:

```shell
./launch.sh clean install
```
Se puede comprobar el funcionamiento de la aplicación desde la url "http://localhost:8090/".

### EndPoints
Los recursos disponibles a los que atiende la aplicación son los siguientes.
- `/business-application/index.html`: fichero estático de inicio que venía por defecto al generarse el proyecto.

- `/`: bienvenida general, público
- `/menu`: al acceder a este recurso, se tiene que haber iniciado sesión si o si, para lo que se tendrá en la sesión información sobre si el usuario es paciente o facultativo, para realizar la redirección adecuada al menú de rol, redirigiendo a `/facultativo` o `/paciente`.

- `/facultativo`: menú
- `/facultativo/solicitar`: creación de la solicitud de consentimiento
- `/facultativo/solicitudes`: listado de las solicitudes de consentimiento
- `/facultativo/solicitud?id`: visualización de una solicitud de consentimiento concreta
- `/facultativo/consentimientos`: listado de los consentimientos
- `/facultativo/consentimiento?id`: visualización de un consentimiento concreto

- `/paciente`: menú
- `/paciente/solicitudes`: listado de las solicitudes de consentimiento
- `/paciente/solicitud?id`: visualización de una solicitud de consentimiento concreta
- `/paciente/consentimientos`: listado de los consentimientos
- `/paciente/consentimiento?id`: visualización de un consentimiento concreto

- `/management/deployedUnits`: se muestran en terminal todas las unidades de despliegue disponibles en el motor de procesos.
- `/management/processInstances?detailed`: se muestran en terminal todas las instancias de proceso disponibles en el servidor, y en caso de indicar con "true" el parámetro "detailed", se mostrarán las tareas, variables y workItems de cada una de las instancias de proceso.
- `/management/workItems?processInstanceId`: se muestran en terminal todos los workItems de una instancia de proceso.
- `/management/tasks?processInstanceId`: se muestran en terminal todas las tareas de una instancia de proceso.
- `/management/vars?processInstanceId`: se muestran en terminal todas las variables de una instancia de proceso.


## Procesos de negocio

### Solicitud de Consentimiento - "RequestConsent"
En este proceso, el facultativo médico que inicia el proceso, pretende realizar una solicitud para obtener consentimiento sobre un asunto determinado, a uno o varios pacientes.

Para elaborar esa solicitud de consentimiento, el facultativo va a rellenar un cuestionario (recurso fhir) en el que va a introducir todos los datos necesarios para la solicitud, como:
+ pacientes a los que va dirigida
+ datos a los que se quiere acceso
+ periodo de acceso a los datos solicitados
+ facultativo que los va a utilizar
+ el uso que se le va a dar a los datos solicitados
+ ...

Lo que va a ocurrir a partir de la respuesta que el facultativo médico da a ese cuestionario, es que se va a generar una solicitud de consentimiento, con los datos e información indicados, que va destinada a los distintos pacientes seleccionados, los cuales tendrán que aceptar o rechazar la petición, generándose así el correspondiente consentimiento.

El fin del proceso se producirá cuando cada uno de los pacientes haya ofrecido una respuesta a la solicitud de consentimiento que se ha generado para el.

El proceso cuenta con las siguientes variables:

| Tipo      | Nombre                         | Descripción                                                                                                                                             |
| --------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| String    | practitioner                   | usuario facultativo                                                                                                                                     |
| String    | fhirServer                     | servidor fhir del que obtener el recurso Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento         |
| String    | requestQuestionnaireId         | id del recurso fhir de tipo Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento                      |
| String    | requestQuestionnaireResponseId | id del recurso fhir de tipo QuestionnarieResponse que representa la respuesta al cuestionario que asiste la creación de la solicitud del consentimiento |
| ArrayList | patientList                    | lista de pacientes a los que va dirigida la solicitud de consentimiento                                                                                 |
| ArrayList | reviewList                     | lista de respuestas de los pacientes a su solicitud de consentimiento                                                                                   |

#### Nodos del workflow del proceso

##### Inicio
El facultativo médico comienza el proceso al querer crear una solicitud de consentimiento haciendo una operación GET al recurso "/facultativo/solicitud", para lo que el controlador de la Business-Application utilizará la API del motor de procesos para crear una instancia del proceso, comenzando la ejecución del workflow, el cual empezará a partir del inicio.

##### Tarea personalizada - "ConsentRequestConfig"
Esta tarea de tipo personalizado se encarga de recoger tanto el servidor fhir (fhirServer) en el que se encuentra el recurso fhir de tipo Questionnarie, el cual se utilizará para crear la solicitud de consentimiento, como el id (requestQuestionnaireId) que lo identifica.

La tarea cuenta con un workItem personalizado, el cual se ejecutará en la Business Application para llevar a cabo su objetivo.

Las variables que recibe:
- "RequestConsent" (nombre del proceso)
Las variables que rellena:
- fhirServer
- requestQuestionnaireId

A su entrada realiza lo siguiente:
```java
System.out.println("JBPM -- Entro en la tarea 'Consent Request Config' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```

A su salida realiza lo siguiente:
```java
System.out.println("JBPM -- Salgo de la tarea 'Consent Request Config' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```

##### Tarea humana - "Consent Request Generation"
En esta tarea, se utilizan el servidor fhir y el id de cuestionario para presentar el cuestionario seleccionado al facultativo. Cuando este lo rellena, lo entrega con una operación POST al recurso "/facultativo/solicitud", y se almacena la petición, y se genera para cada uno de los pacientes una solicitud de consentimiento.

Las variables que recibe:
- practitioner
- fhirServer
- requestQuestionnaireId
Las variables que rellena:
- requestQuestionnaireResponseId
- patientList
- reviewList

A su entrada realiza lo siguiente:
```java
System.out.println("JBPM -- Entro en la tarea 'Consent Request Generation' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```

A su salida realiza lo siguiente:
```java
System.out.println("JBPM -- Salgo de la tarea 'Consent Request Generation' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```

##### Subproceso - "Multiple Consent Review"
Para cada uno de los pacientes listados en el cuestionario, se genera un subproceso "ReviewConsent" en el que tienen que leer la solicitud de consentimiento y aceptarla o rechazarla.

Su workflow cuenta con los siguientes nodos:
+ Inicio
+ Tarea humana - "Consent Review Generation"
+ Fin

Las variables que recibe:
- practitioner
- fhirServer
- requestQuestionnaireId
- requestQuestionnaireResponseId
- patientList como patient
Las variables que rellena:
- reviewList como review


A su entrada realiza lo siguiente:
```java
System.out.println("JBPM -- Entro en la tarea 'Multiple Consent Review' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```

A su salida realiza lo siguiente:
```java
System.out.println("JBPM -- Salgo de la tarea 'Multiple Consent Review' del proceso 'ConsentRequest'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patientList: " + kcontext.getVariable("patientList").toString());
System.out.println("reviewList: " + kcontext.getVariable("reviewList").toString());
```
##### Fin
El proceso finaliza cuando cada uno de los pacientes listados ha respondido a su solicitud de consentimiento, terminando por consecuencia el subproceso que le correspondía.


### Revisión de Consentimiento - "ReviewConsent"
El proceso es iniciado por el proceso "Solicitud de consentimiento" cuando el facultativo genera la solicitud de consentimiento para una serie de pacientes.

El proceso tiene las siguientes variables:

| Tipo    | Nombre                         | Descripción                                                                                                                                                                   |
| ------- | ------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| String  | practitioner                   | usuario facultativo                                                                                                                                                           |
| String  | fhirServer                     | servidor fhir del que obtener el recurso Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento                               |
| String  | requestQuestionnaireId         | id del recurso fhir de tipo Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento                                            |
| String  | requestQuestionnaireResponseId | id del recurso fhir de tipo Questionnarie Response que representa la respuesta al cuestionario que asiste la creación de la solicitud del consentimiento                      |
| String  | patient                        | paciente que tiene que contestar a la solicitud de consentimiento                                                                                                             |
| String  | reviewQuestionnaireId          | id del recurso fhir de tipo Questionnarie que respresenta el cuestionario mostrado al paciente para que este acepte o rechace la solicitud de consentimiento                  |
| String  | reviewQuestionnaireResponseId  | id del recurso fhir de tipo Questionnarie Respose que representa la respuesta del paciente al cuestionario en el que acepta o rechaza la solicitud de consentimiento recibida |
| Boolean | review                         | respuesta del paciente a la solicitud de consentimiento                                                                                                                       |

#### Nodos del workflow del proceso
##### Tarea humana - "Consent Review Generation"
En esta tarea, se utilizan el servidor fhir y el id de la respuesta del cuestionario que identifica la solicitud de consentimiento generada por el facultativo para presentar al paciente el consentimiento que tiene que aceptar o rechazar. Cuando este lo rellena, lo entrega con una operación POST al recurso "/facultativo/consentimiento".

Las variables que recibe:
- fhirServer
- requestQuestionnaireResponseId
Las variables que rellena:
- reviewQuestionnaireId
- reviewQuestionnaireResponseId
- review

A su entrada realiza lo siguiente:
```java
System.out.println("JBPM -- Entro en la tarea 'Consent Review Generation' del proceso 'ConsentReview'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patient: " + kcontext.getVariable("patient").toString());
System.out.println("reviewQuestionnaireId: " + kcontext.getVariable("reviewQuestionnaireId").toString());
System.out.println("reviewQuestionnaireResponseId: " + kcontext.getVariable("reviewQuestionnaireResponseId").toString());
System.out.println("review: " + kcontext.getVariable("review").toString());
```

A su salida realiza lo siguiente:
```java
System.out.println("JBPM -- Salgo de la tarea 'Consent Review Generation' del proceso 'ConsentReview'.");

System.out.println("fhirServer: " + kcontext.getVariable("fhirServer").toString());
System.out.println("requestQuestionnaireId: " + kcontext.getVariable("requestQuestionnaireId").toString());
System.out.println("requestQuestionnaireResponseId: " + kcontext.getVariable("requestQuestionnaireResponseId").toString());
System.out.println("practitioner: " + kcontext.getVariable("practitioner").toString());
System.out.println("patient: " + kcontext.getVariable("patient").toString());
System.out.println("reviewQuestionnaireId: " + kcontext.getVariable("reviewQuestionnaireId").toString());
System.out.println("reviewQuestionnaireResponseId: " + kcontext.getVariable("reviewQuestionnaireResponseId").toString());
System.out.println("review: " + kcontext.getVariable("review").toString());
```

## Generación
Tal y como queda documentado en la [Documentación Oficial de JBPM](https://docs.jbpm.org/7.74.1.Final/jbpm-docs/html_single/#_sect_BA_create_application), la creación de la aplicación parte por su generación a través de un arquetipo maven:

```shell
mvn archetype:generate -B \
	-DarchetypeGroupId=org.kie \
	-DarchetypeArtifactId=kie-model-archetype \
	-DarchetypeVersion=7.74.1.Final \
	-DgroupId=us.dit \
	-DartifactId=gestorconsentimientos-model \
	-Dversion=1.0 \
	-Dpackage=us.dit.gestorconsentimientos.model
```

```shell
mvn archetype:generate -B \
	-DarchetypeGroupId=org.kie \
	-DarchetypeArtifactId=kie-kjar-archetype \
	-DarchetypeVersion=7.74.1.Final \
	-DgroupId=us.dit \
	-DartifactId=gestorconsentimientos-kjar \
	-Dversion=1.0 \
	-Dpackage=us.dit.gestorconsentimientos
```

```shell
mvn archetype:generate -B \
	-DarchetypeGroupId=org.kie \
	-DarchetypeArtifactId=kie-service-spring-boot-archetype \
	-DarchetypeVersion=7.74.1.Final \
	-DgroupId=us.dit \
	-DartifactId=gestorconsentimientos-service \
	-Dversion=1.0 \
	-Dpackage=us.dit.gestorconsentimientos.service \
	-DappType=bpm
```

Para que la aplicación generada funcione correctamente, es necesario modificar la versión de spring que utiliza la aplicación a las 2.6.15. Para ello se va a utilizar en el fichero pom.xml del proyecto gestorconsentimientos-service un padre que define la versión para cualquier dependencia y plugin spring a la 2.6.15. Para que la versión de los componentes (dependencias y plugins) Spring ya existentes en el fichero queden con la misma versión, es necesario retirar la versión específica de todas esas dependencias.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.6.15</version>
    <!-- Referencia de la versión 2.6.15: https://docs.spring.io/spring-boot/docs/2.6.15/reference/htmlsingle/ -->
    <!-- Versión 2.6.15 es la más actual con la que todo funciona, con fecha 6/10/23 -->
    <!-- Versiones 2.7.0, 2.7.15 y 2.7.16 dan un error al persistir-->
</parent>
```

Para que el contenido del proyecto gestorconsentimientos-model esté disponible tanto para el proyecto kjar como para el proyecto service, es necesario incluirlo en sus dependencias, lo cual se realiza en los ficheros pom.xml correspondientes a cada proyecto.

```xml
		<dependency>
			<groupId>us.dit</groupId>
			<artifactId>gestorconsentimientos-model</artifactId>
			<version>1.0</version>
		</dependency>
```

## Gestión del proyecto KJAR
Para poder trabajar con los activos de negocio que va a contener el proyecto "kjar", desde business-central (jbpm), es necesario crear un repositorio de control de versiones git, el cual va a permitir llevar a cabo modificaciones en el proyecto desde la herramienta.

En primer lugar es necesario inicializar el repositorio git:
```shell
cd GestorConsentimientos/gestorconsentimientos-kjar/
git init
git add -A
git commit -m "version inicial"

# Desde business-central importar el proyecto
# file:///home/javi/Proyectos/GestorConsentimientos/gestorconsentimientos-kjar/

git remote add origin ssh://wbadmin@localhost:8001/MySpace/gestorconsentimientos-kjar
```

Una vez generados activos de negocio desde jbpm, es necesario actualizar la versión del proyecto "KJAR" que emplea la BA, puesto que todo lo realizado en "business-central", se encuentra en nuevas versiones del repositorio, y que es necesario descargar al proyecto "KJAR".
```shell
git pull origin master
```
