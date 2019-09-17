package edu.harvard.codeone2019.sparkk8sdemo;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import org.apache.spark.ml.clustering.LDAModel;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.clustering.LDA;
import org.apache.spark.ml.feature.CountVectorizer;
import org.apache.spark.ml.feature.CountVectorizerModel;
import org.apache.spark.ml.feature.RegexTokenizer;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.collection.mutable.WrappedArray;

/**
 * Simple Machine Learning example, reads CSV from Azure Blob Storage, runs LDA
 * Topic Model, and saves the results to Azure. Topic modeling is a type of
 * statistical modeling for discovering the abstract “topics” that occur in a
 * collection of documents. Latent Dirichlet Allocation (LDA) is an example of
 * topic model and is used to classify text in a document to a particular topic.
 * LDA is included in the SparkML library.
 *
 * @author ellenk
 */
public class SimpleSparkApp {

    public static void main(String args[]) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        SparkSession session = SparkSession.builder().appName("LDA-demo").getOrCreate();
        String azureStorageKey = args[0];
        //
        // Convert raw text into feature vectors and vocabulary
        //
        Dataset<Row> textRows = loadText(session);
        Dataset<Row> tokenized =  new RegexTokenizer().setPattern("\\W").setInputCol("text")
                .setOutputCol("words").transform(textRows);
        Dataset<Row> filtered = new StopWordsRemover().setInputCol("words")
                .setOutputCol("filtered").transform(tokenized);
        CountVectorizerModel cvModel = new CountVectorizer()
                .setInputCol("filtered")
                .setOutputCol("features").setMinDF(10).fit(filtered);
        Dataset<Row> features = cvModel.transform(filtered);
        String[] vocab = cvModel.vocabulary();

        //
        // Use feature vectors to generate LDA model
        //
        LDA lda = new LDA().setK(10).setMaxIter(50);
        LDAModel model = lda.fit(features);

        // Extract topics and save results
        Dataset<Row> topics = model.describeTopics(10);
        List<Row> topicsList = topics.collectAsList();
        saveResults(azureStorageKey, topicsList, vocab);
    }

    /**
     * Read CSV file from Azure blob storage into a Spark Dataframe
     *
     * @param session
     * @return
     */
    private static Dataset<Row> loadText(SparkSession session) {
        String fromURI = session.sparkContext().getConf().get("spark.codeOne.demo.readFileURI");
        return session.read().format("csv")
                .option("inferSchema", "true")
                .option("header", "true")
                .option("delimiter", ",")
                .load(fromURI);

    }

    /**
     * Replace LDA topic termIndices with term values, for more readablility
     * Write Results to Azure Blob Storage
     *
     * @param topicsList
     * @param vocab
     * @param writeFileURI
     */
    private static void saveResults(String azureStorageKey, List<Row> topicsList, String[] vocab) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        StringBuilder sb = new StringBuilder();

        topicsList.forEach(row -> {
            Integer[] indices = (Integer[]) ((WrappedArray<Integer>) row.getAs("termIndices")).array();
            String[] terms = new String[indices.length];
            for (int i = 0; i < indices.length; i++) {
                terms[i] = vocab[indices[i]];
            }
            sb.append(Arrays.toString(terms)).append(System.lineSeparator());
        });

        saveToCloudStorage(azureStorageKey, sb.toString());

    }

    /**
     * Make connection to Azure Storage account, and upload the text results to
     * Blob storage, in LDAResults.txt
     *
     * @param session
     * @param results
     * @throws URISyntaxException
     * @throws InvalidKeyException
     * @throws StorageException
     * @throws IOException
     */
    private static void saveToCloudStorage(String blobAccountKey,String results)
            throws URISyntaxException, InvalidKeyException, StorageException, IOException {

        String storageConnectionString
                = "DefaultEndpointsProtocol=https;"
                + "AccountName=" + "consilience2" + ";"
                + "AccountKey=" + blobAccountKey + ";EndpointSuffix=core.windows.net";
        CloudStorageAccount.parse(storageConnectionString);

        CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        CloudBlobContainer container = blobClient.getContainerReference("code-one-2019");
        container.createIfNotExists();
        CloudBlockBlob blob = container.getBlockBlobReference("LDAResults.txt");
        blob.uploadText(results);
    }

}
