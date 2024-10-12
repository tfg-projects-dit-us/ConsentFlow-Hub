

# Arranca el servidor Fhir local que se utiliza (R5)
echo "Arrancando el contenedor docker con el servidor Fhir local"
echo "Arrancando el contenedor docker con la BBDD Postgresql"
echo "En caso de tener ya un servidor postgresql no será necesario y no arrancará por no estar disponible el puerto 5432"
echo "La aplicación está configurada para el usuario y contraseña jbpm y una DB consentimientos"

# Se elminan los contenedores parados para eliminar los datos de ejecución de estos.
# En caso de querer conservar los datos, se puede comentar.
sudo docker stop hapi jbpm_db adminer
sudo docker rm hapi jbpm_db adminer

cd ./postgresql
sudo docker compose up -d
cd ../

cd ./fhirServer
sudo docker compose up -d

# Se da un tiempo a que arranque
echo "Esperando 50 segundos a que arranque el servidor"
echo 0
sleep 10

echo 10
sleep 10

echo 20
sleep 10

echo 30
sleep 10

echo 40
sleep 10

# Sube el cuestionario que permite realizar la solicitud de consentimientos
echo "Publicando el recurso Questionnaire en el servidor Fhir local"
./fhir_save_questionnarie.sh

# Arranca la aplicación Gestor de Consentimientos
echo "Arrancando aplicación Gestor de Consentimientos"
cd ../../gestorconsentimientos-service/
./launch.sh clean install -Ppostgres
