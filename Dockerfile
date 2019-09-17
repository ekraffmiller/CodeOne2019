FROM gcr.io/spark-operator/spark:v2.4.0
RUN mkdir -p /opt/spark/jars
RUN mkdir -p /opt/spark/data
COPY target/2019SparkK8SDemo-1.0-SNAPSHOT.jar /opt/spark/jars
COPY k8s/jars/hadoop-azure-2.7.3.jar /opt/spark/jars
COPY k8s/jars/azure-storage-2.0.0.jar /opt/spark/jars
COPY data/wine_reviews500.csv /opt/spark/data