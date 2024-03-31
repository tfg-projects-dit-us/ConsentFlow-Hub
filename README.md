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
