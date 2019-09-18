# Steps for Running LDA Demo

Install minikube and helm using homebrew (cmd: brew install)

Create new minikube (By default it will start with 3CPU and 4096 memory)

```
$ minikube start 
$ minikube dashboard
```

Initialize Helm - this will deploy the Tiller Pod in the cluster

```
$ helm init  
```

Add the repository that contains the spark-operator Helm chart to local Helm repository 

```
$ helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
```



Deploy  the Spark Operator into the cluster. Need to use the --set parameter because the default version in helm chart is not the latest. 
Need to enable the WebHook for mounting the volumes and applying Spark Config value

```
$ helm install incubator/sparkoperator --namespace spark-operator \
--set operatorVersion=v2.4.0-v1beta1-latest \
--set enableWebhook=true
```

Create serviceAccount for our spark applications, grant the account a ClusterRole with permissions that Spark Driver pod needs to  control Executor pods. (Located in ⁨spark-on-k8s-operator-master⁩/manifest)

```
$ kubectl apply -f spark-rbac.yaml
```

Create directory on Minikube for saving Spark Events for History Server

```
$ minikube ssh
$ sudo mkdir /mnt/data
$ exit
```

Create Persistent Volume and Persistent Volume Claim for Spark Events 

```
$ kubectl apply -f pv-volume.yaml
$ kubectl apply -f pvc-claim.yaml 
```

Run the LDA Demo that reads and writes to the persistent volume on minikube.

```
$ kubectl apply -f lda-demo-local.yaml
```

```  

Use kubectl port-forward to access the Spark UI

```
$ kubectl port-forward <driver-pod-name> 4040:4040
```


Install History Server, which will read events from /mnt/data

```
$ helm install stable/spark-history-server --namespace default \
--set pvc.existingClaimName=task-pv-claim \
--set pvc.eventsDir=
```

Open a browser to view the Spark History Server

```
$ minikube service <animal name>-spark-history-server
```  

Use kubectl port-forward to access the Spark UI

```
$ kubectl port-forward lda-demo-local-driver 4040:4040
```

To list SparkApplications, run:

```
$ kubectl get sparkapplications 
```

To check events for the SparkApplication object, run:

```
$ kubectl describe sparkapplication lda-demo-local
```

Before re-rerunning the application, delete it from the cluster:

```
$ kubectl delete sparkapplication lda-demo-local
```

