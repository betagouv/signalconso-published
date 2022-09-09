package services

import akka.Done
import akka.NotUsed
import akka.stream.IOResult
import akka.stream.Materializer
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.util.ByteString

import java.nio.file.Path
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import config.BucketConfiguration
import controllers.error.AppError.BucketFileNotFound

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class S3Service(implicit
    val materializer: Materializer,
    val executionContext: ExecutionContext,
    val bucketConfiguration: BucketConfiguration
) extends S3ServiceInterface {
  private[this] val bucketName = bucketConfiguration.amazonBucketName

  private val alpakkaS3Client = S3
  private val awsS3Client = AmazonS3ClientBuilder
    .standard()
    .withEndpointConfiguration(
      new EndpointConfiguration("https://cellar-c2.services.clever-cloud.com", "us-east-1")
    )
    .withCredentials(
      new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(
          bucketConfiguration.keyId,
          bucketConfiguration.secretKey
        )
      )
    )
    .build()

  override def upload(bucketKey: String): Sink[ByteString, Future[MultipartUploadResult]] =
    alpakkaS3Client.multipartUpload(bucketName, bucketKey)

  override def download(bucketKey: String): Future[ByteString] =
    downloadFromBucket(bucketKey)
      .flatMap(a => a.runWith(Sink.reduce((a: ByteString, b: ByteString) => a ++ b)))

  override def downloadOnCurrentHost(bucketKey: String, filePath: String): Future[IOResult] =
    downloadFromBucket(bucketKey).flatMap(a => a.runWith(FileIO.toPath(Path.of(filePath))))

  private def downloadFromBucket(bucketKey: String): Future[Source[ByteString, NotUsed]] =
    alpakkaS3Client
      .download(bucketName, bucketKey)
      .runWith(Sink.head)
      .map {
        case Some((byteStringSource, _)) => byteStringSource
        case None                        => throw BucketFileNotFound(bucketName, bucketKey)
      }

  override def delete(bucketKey: String): Future[Done] =
    alpakkaS3Client.deleteObject(bucketName, bucketKey).runWith(Sink.head)

  override def getSignedUrl(bucketKey: String, method: HttpMethod = HttpMethod.GET): String = {
    // See https://docs.aws.amazon.com/AmazonS3/latest/dev/ShareObjectPreSignedURLJavaSDK.html
    val expiration = new java.util.Date
    expiration.setTime(expiration.getTime + 1000 * 60 * 60)
    val generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, bucketKey)
      .withMethod(method)
      .withExpiration(expiration)
    awsS3Client.generatePresignedUrl(generatePresignedUrlRequest).toString
  }
}
