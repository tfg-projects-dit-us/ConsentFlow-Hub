#!/bin/bash
cd ..
MVN_ARG_LINE=()

for arg in "$@"
do
    case "$arg" in
        *)
            MVN_ARG_LINE+=("$arg")
        ;;
    esac
done

startDateTime=`date +%s`

# check that Maven args are non empty
if [ "$MVN_ARG_LINE" != "" ] ; then
    mvnBin="mvn"
    if [ -a $M3_HOME/bin/mvn ] ; then
       mvnBin="$M3_HOME/bin/mvn"
    fi
    echo
    echo "Running maven build on available projects (using Maven binary '$mvnBin')"

    "$mvnBin" -v
    echo
    projects=( "*-model" "*-kjar" "gestorconsentimientos-service")

    for suffix in "${projects[@]}"; do

        for repository in $suffix;  do
        echo
            if [ -d "$repository" ]; then
                echo "==============================================================================="
                echo "$repository"
                echo "==============================================================================="

                cd $repository

                "$mvnBin" "${MVN_ARG_LINE[@]}"
                returnCode=$?

                if [ $returnCode != 0 ] ; then
                    exit $returnCode
                fi

                cd ..
                fi

        done;
    done;
    endDateTime=`date +%s`
    spentSeconds=`expr $endDateTime - $startDateTime`
    echo
    echo "Total build time: ${spentSeconds}s"

else
    echo "No Maven arguments skipping maven build"
        
fi


if [[ "$@" =~ "docker" ]]; then
    echo "Launching the application as docker container..."
    
    docker run -d -p 8090:8090 --name gestorconsentimientos-service apps/gestorconsentimientos-service:1.0
elif [[ "$@" =~ "openshift" ]]; then
    echo "Launching the application on OpenShift..."
    
    oc new-app gestorconsentimientos-service:1.0
    oc expose svc/gestorconsentimientos-service
else

	echo "Launching the application locally..."
	pattern="gestorconsentimientos-service"
	files=( $pattern )
	cd ${files[0]}
	executable="$(ls  *target/*.jar | tail -n1)"
	java -Dorg.kie.server.bypass.auth.user=true -jar "$executable"
fi