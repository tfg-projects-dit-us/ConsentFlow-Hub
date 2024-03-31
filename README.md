# GestorConsentimientos
Aplicación web para la gestión de consentimientos médicos, creada utilizando el kit de desarrollo JBPM, el cual permite la construcción de aplicaciones que funcionan bajo el paradigma de trabajo BPM.

La creación de la aplicación parte por su generación a través de un arquetipo maven:

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

Para que la aplicación generada funcione correctamente, es necesario modificar la versión de spring que utiliza la aplicación a las 2.6.15. Para ello se va a utilizar en el fichero pom.xml del proyecto gestorconsentimientos-service un padre que define la versión para cualquier dependencia y plugin spring a la 2.6.15.

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
