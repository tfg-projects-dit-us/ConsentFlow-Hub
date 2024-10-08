![GitHub top Language](https://img.shields.io/github/languages/top/tfg-projects-dit-us/ConsentFlow-Hub)
![GitHub forks](https://img.shields.io/github/forks/tfg-projects-dit-us/ConsentFlow-Hub?style=social)
![GitHub contributors](https://img.shields.io/github/contributors/tfg-projects-dit-us/ConsentFlow-Hub)
![GitHub Repo stars](https://img.shields.io/github/stars/tfg-projects-dit-us/ConsentFlow-Hub?style=social)
![GitHub repo size](https://img.shields.io/github/repo-size/tfg-projects-dit-us/ConsentFlow-Hub)
![GitHub watchers](https://img.shields.io/github/watchers/tfg-projects-dit-us/ConsentFlow-Hub)
![GitHub](https://img.shields.io/github/license/tfg-projects-dit-us/ConsentFlow-Hub)

<img src="https://github.com/tfg-projects-dit-us/ConsentFlow-Hub/blob/master/Resources/img/ConsentFlowIcon.png" width="200" />

ConsentFlow Hub
=====================================

Plataforma para la gestión de consentimientos médicos, creada utilizando el kit de desarrollo JBPM, que facilita el desarrollo de soluciones conformes al paradigma BPM (Business Process Management)

Actualmente es una versión beta en la que se han incluido sólo las capacidades más elementales

Está desarrollado en el Departamento de Ingeniería Telemática de la Universidad de Sevilla

## Licencia

Este proyecto está licenciado bajo los términos de la [Licencia Pública General de GNU (GPL) versión 3](https://www.gnu.org/licenses/gpl-3.0.html).

## License

This project is licensed under the terms of the [GNU General Public License (GPL) version 3](https://www.gnu.org/licenses/gpl-3.0.html).

## Reconocimientos

Este proyecto es el resultado del trabajo desarrollado por los alumnos que a continuación se mencionan, bajo la supervisión de la profesora Isabel Román Martínez.

**Autores:**
- [Jose Antonio García Linares](https://github.com/josgarlin): desarrolla en su TFG la primera versión del proyecto
- [Marco Antonio Madonado Orozco](https://github.com/mamaldonado97): desarrolla en su TFG plantillas para la presentación de cuestionarios FHIR
- [Francisco Javier Ros Raposo](https://github.com/J4V1R6): desarrolla en su TFM la versión actual del proyecto

**Supervisora:**
- [Isabel Román Martínez](https://github.com/Isabel-Roman), Profesora del Departamento de Ingeniería Telemática de la Universidad de Sevilla

La supervisión incluye la generación de ideas, la corrección, el desarrollo de algunos componentes y la orientación técnica durante todo el proceso de desarrollo.

**Graphical design**
- Sticker created by Stickers -Flaticon https://www.flaticon.es/stickers-gratis/consentimiento

<a href="https://www.flaticon.es/stickers-gratis/consentimiento" title="consentimiento stickers">Sticker de la imagen creado por Stickers - Flaticon</a>

## Instrucciones de despliegue y ejecución
Para ejecutar la aplicación empresarial basta con ejecutar el script "launch" del proyecto gestorconsentimientos-service (".sh" para linux, ".bat" para windows):

```shell
./launch.sh clean install -Ppostgres
```
La aplicación, está configurada para tratar con un servidor Hapi Fhir en la url "http://localhost:8888/fhir/", para lo que se puede desplegar utilizando el fichero "docker-compose.yaml" que hay en la carpeta "fhirServer" del repositorio. Otra opción es modificar el fichero de configuración y utilizar un servidor hapi fhir público o que tenga otra dirección.

La aplicación también tiene configurada en el fichero de configuración "application.properties" el uso de una BBDD postgresql en la dirección "localhost" y en el puerto "5432". Es posible desplegar el servidor postgresql utilizando una aplicación de escritorio o terminal, o por el contrario utilizando un contenedor docker, para lo que se puede utilizar el fichero "docker-compose.yaml" de la carpeta postgresql.Es posible modificar la dirección y puerto de la BBDD postgresql, o incluso modificar la BBDD que se utiliza, para lo que es necesario indicar el perfil maven adecuado "-P<perfil>", para que el proyecto maven cuente con las dependencias necesarias.

Es posible arrancar todo de manera conjunta desde un script (start.sh) para linux que hay en el directorio raíz del proyecto, o por el contrario ejecutar cada componente por separado:

```bash
# Acceso al directorio raíz del repositorio
cd ConsentFlow-Hub/

# Ejecución del servidor Fhir (la opción '-d' lo ejecuta en segundo plano)
cd ./Resources/fhirServer/
sudo docker-compose up -d

# Publicación del recurso fhir de tipo Questionnaire que utiliza la aplicación empresarial (al ser el primero tiene id=1, lo que se utiliza para obtenerlo desde la aplicación)
# Esperar a que el contenedor haya arrancado
./fhir_save_questionnaire.sh

# Ejecución del servidor postgresql
cd ./Resources/postgresql
sudo docker-compose up -d 

```
Una vez se haya arrancado todo, se puede proceder a probar el funcionamiento de la aplicación desde la url "http://localhost:8090/".

En el servidor "http://localhost:8888/" será posible visualizar los recursos FHIR que se hayan ido publicando.

Además, junto con la BBDD se levanta el servicio adminer, en la dirección "http:localhost:3000", a traves del cual es posible acceder de manera muy sencilla y visual a la BBDD postgresql, indicando el tipo de BBDD, la dirección (que al estar dentro del mismo docker compose, se puede utilizar le nombre del contenedor "jbpm_db", con el puerto que tiene abierto), la BBDD, el usuario y la contraseña configuradas en la configuración del contenedor de postgresql.

Algunos de los usuarios activos, disposnibles para poder probar la aplicación, fijados en el fichero "WebSecurityConfig.java" son:
- user: paciente y facultativo
- paciente: paciente
- facultativo: facultativo

Estos tienen como contraseña su mismo usuario.

### EndPoints
Los recursos disponibles a los que atiende la aplicación son los siguientes.
- `/business-application/index.html`: fichero estático de inicio que venía por defecto al generarse el proyecto.

- `/`: bienvenida general, público
- `/menu`: al acceder a este recurso, se tiene que haber iniciado sesión si o si, para lo que se tendrá en la sesión información sobre si el usuario es paciente o facultativo, para realizar la redirección adecuada al menú de rol, redirigiendo a `/facultativo` o `/paciente`.

- `/facultativo`: menú
- `/facultativo/solicitar`: creación de la solicitud de consentimiento
- `/facultativo/solicitudes`: listado de las solicitudes de consentimiento
- `/facultativo/solicitudes/id`: visualización de una solicitud de consentimiento concreta
- `/facultativo/consentimientos`: listado de los consentimientos
- `/facultativo/consentimientos/id`: visualización de un consentimiento concreto

- `/paciente`: menú
- `/paciente/solicitudes`: listado de las solicitudes de consentimiento
- `/paciente/solicitudes/id`: visualización de una solicitud de consentimiento concreta
- `/paciente/consentimientos`: listado de los consentimientos
- `/paciente/consentimientos/id`: visualización de un consentimiento concreto

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
| Long    | requestQuestionnaireId         | id del recurso fhir de tipo Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento                      |
| Long    | requestQuestionnaireResponseId | id del recurso fhir de tipo QuestionnarieResponse que representa la respuesta al cuestionario que asiste la creación de la solicitud del consentimiento |
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
| Long  | requestQuestionnaireId         | id del recurso fhir de tipo Questionnaire que representa el cuestionario que asiste la creación de la solicitud del consentimiento                                            |
| Long  | requestQuestionnaireResponseId | id del recurso fhir de tipo Questionnarie Response que representa la respuesta al cuestionario que asiste la creación de la solicitud del consentimiento                      |
| String  | patient                        | paciente que tiene que contestar a la solicitud de consentimiento                                                                                                             |
| Long  | reviewQuestionnaireId          | id del recurso fhir de tipo Questionnarie que respresenta el cuestionario mostrado al paciente para que este acepte o rechace la solicitud de consentimiento                  |
| Long  | reviewQuestionnaireResponseId  | id del recurso fhir de tipo Questionnarie Respose que representa la respuesta del paciente al cuestionario en el que acepta o rechaza la solicitud de consentimiento recibida |
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

Cómo se va a trabajar con Hapi Fhir, se van a definir e incluir las dependencias necesarias al proyecto, las cuales van a trabajar en la versión" 6.4.0", la versión más actual a la que no existen problemas de compatibilidad, puesto que el procesamiento XML de FHIR utilizará la implementación StAX "Woodstox" versión "6.4.0", e utilizar versiones superiores para estas dependencias genera un fallo al trabajar con ellos.
```xml
<!-- Hapi Fhir -->
<dependency>
  <groupId>ca.uhn.hapi.fhir</groupId>
  <artifactId>hapi-fhir-structures-r5</artifactId>
  <version>${hapifhir.version}</version>
</dependency>
<dependency>
  <groupId>ca.uhn.hapi.fhir</groupId>
  <artifactId>hapi-fhir-base</artifactId>
  <version>${hapifhir.version}</version>
</dependency>
<dependency>
  <groupId>ca.uhn.hapi.fhir</groupId>
  <artifactId>hapi-fhir-client</artifactId>
  <version>${hapifhir.version}</version>
</dependency>
```

Otra dependencia que es necesaria para poder trabajar con las plantillas thymeleaf:
```xml
<!-- HTML Templates -->
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Para poder trabajar con los activos de negocio que va a contener el proyecto "kjar", desde business-central (jbpm), es necesario crear un repositorio de control de versiones git, el cual va a permitir llevar a cabo modificaciones en el proyecto desde la herramienta.

Para que el contenido del proyecto gestorconsentimientos-model esté disponible tanto para el proyecto kjar como para el proyecto service, es necesario incluirlo en sus dependencias, lo cual se realiza en los ficheros pom.xml correspondientes a cada proyecto.

```xml
		<dependency>
			<groupId>us.dit</groupId>
			<artifactId>gestorconsentimientos-model</artifactId>
			<version>1.0</version>
		</dependency>
```

## Gestión del proyecto KJAR (definición de los recursos BPM como procesos)
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

## Instalación de la herramienta JBPM

La versión del servidor JBPM empleado es la 7.74.1, la cual requiere para funcionar java 8, y la aplicación empresarial requiere de la 17.


### Instalación de Java
Es necesario instalar JDK puesto que la aplicación web desarrollada, así como la herramienta Business-Central están escritas en el lenguaje de programación Java.

El puerto por defecto en el que se arranca el servidor es el 8080, de forma que para acceder a el una vez iniciado se deberá acceder a la dirección "http://localhost:8080".

Para comprobar que versión está instalada de JDK y JRE:
```bash
java --version
```

Se van a instalar dos versiones JDK y JRE, una para la ejecución de las Business Application creada utilizando el framework JBPM, y otra para la herramienta Business-Central. Para ello, utilizando el gestor de paquetes apt:
```bash
sudo apt-get update
sudo apt-get upgrade

# Para BA
sudo apt install openjdk-17-jdk
sudo apt install openjdk-17-jre

# Para la aplicación Business Central
sudo apt install openjdk-8-jdk
sudo apt install openjdk-8-jre
```

Para comprobar que ambas versiones están disponibles:
```bash
update-alternatives --list java
update-alternatives --list javac
```

Para comprobar cual es la versión instalada que se va a utilizar por defecto (queremos la 17):
```bash
java --version
```

En caso de estar instalado y experimentar problemas, estos pueden estar relacionados con las variables de entorno definidas por java, y que contienen las rutas de instalación de los diferentes complementos.
