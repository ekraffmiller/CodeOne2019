Install minikube and helm using homebrew (cmd: brew install)

Delete existing minikube cluster if necessary

```
$ minikube stop
$ minikube delete
```

Create new minikube (minikube start - by default it will start with 3CPU and 4096 memory)
>minikube start  (will take a few minutes)
>helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
Only need to do this once. Add the repository  that contains the spark-operator Helm chart. 


Begin Demo
>helm init  (wait for tiller Pod to deploy)
This installs Helm service Tiller in the minikube  cluster.   

>helm install incubator/sparkoperator --namespace spark-operator --set operatorVersion=v2.4.0-v1beta1-latest --set enableWebhook=true

Deploy  the sparkoperator into the cluster. Need to use the --set parameter because the default version in helm chart is not the latest.  


>kubectl apply -f spark-rbac.yaml
Create serviceAccount for spark application, grant the account a ClusterRole with permissions that Spark Driver pod needs to  control Executor pods. (Located in ⁨spark-on-k8s-operator-master⁩/manifest)

>kubectl apply -f spark-pi.yaml  
Can only run this once!  Next  time, it will say “completed” and job won’t run. To run again, change application name.


>minikube ssh
$ sudo mkdir /mnt/data
$ exit
Creates directory on Kubernetes cluster node for sharing

>kubectl apply -f pv-volume.yaml


>kubectl apply -f task-pvc-claim.yaml 
Create PVC claim for sharing a volume to write spark events for Spark History


>helm install stable/spark-history-server --namespace default --set pvc.existingClaimName=task-pv-claim --set pvc.eventsDir=
Deploy the Spark History Server, which will read from /mnt/data


>kubectl apply -f spark-pi-history.yaml
Run the Spark Application that writes its events to the persistent volume
 TODO - show the Spark UI with port forwarding
 >minikube service <animal name>-spark-history-server  
Opens a browser to the Spark History Server!


