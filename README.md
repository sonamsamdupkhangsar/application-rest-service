# application-rest-service
This projects stores application information and the users associated with the application.  This `application-rest-service` contains the application for a client.  It can save application with association to clientId and organizationId.  It can also save user role with groupNames.
 This service requires a jwt-token.  Once the user is authenticated the user can setup application with user access and groupNames.


## Run locally
```
mvn spring-boot:run  -Dspring-boot.run.arguments="--POSTGRES_USERNAME=dummy \
                      --POSTGRES_PASSWORD=dummy \
                      --POSTGRES_DBNAME=account \
                      --POSTGRES_SERVICE=localhost:5432"
```
 
 
## Build Docker image
Build docker image using included Dockerfile.
`docker build -t imageregistry/application-rest-service:1.0 .` 

## Push Docker image to repository
`docker push imageregistry/application-rest-service:1.0`

## Deploy Docker image locally
`docker run -e POSTGRES_USERNAME=dummy \
 -e POSTGRES_PASSWORD=dummy -e POSTGRES_DBNAME=account \
  -e POSTGRES_SERVICE=localhost:5432 \
 --publish 8080:8080 imageregistry/application-rest-service:1.0`


## Installation on Kubernetes
Use my Helm chart here @ [sonam-helm-chart](https://github.com/sonamsamdupkhangsar/sonam-helm-chart):

```
helm install project-api sonam/mychart -f values.yaml --version 0.1.21 --namespace=yournamespace
```

##Instruction for port-forwarding database pod
```
export PGMASTER=$(kubectl get pods -o jsonpath={.items..metadata.name} -l application=spilo,cluster-name=project-minimal-cluster,spilo-role=master -n yournamesapce); 
echo $PGMASTER;
kubectl port-forward $PGMASTER 6432:5432 -n backend;
```

###Login to database instruction
```
export PGPASSWORD=$(kubectl get secret <SECRET_NAME> -o 'jsonpath={.data.password}' -n backend | base64 -d);
echo $PGPASSWORD;
export PGSSLMODE=require;
psql -U <USER> -d projectdb -h localhost -p 6432

```