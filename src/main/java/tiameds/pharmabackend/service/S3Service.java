package tiameds.pharmabackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(String key, MultipartFile file) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return buildS3Url(key);
    }

    public InputStreamResource getFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return new InputStreamResource(s3Client.getObject(getObjectRequest));
    }

    public String copyFile(String sourceKey, String targetKey) {
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(targetKey)
                .build());

        return buildS3Url(targetKey);
    }

    public String copyFromExternalUrl(String sourceUrl, String targetKey) {
        URI uri = URI.create(sourceUrl.trim());
        String host = uri.getHost();

        if (host == null || !host.endsWith(".amazonaws.com")) {
            throw new IllegalArgumentException("Invalid S3 URL format: " + sourceUrl);
        }

        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Failed to download file from " + sourceUrl + ", HTTP status: " + response.statusCode());
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("application/octet-stream");

            s3Client.putObject(PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(targetKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(response.body()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download file from " + sourceUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Download interrupted for " + sourceUrl, e);
        }

        return buildS3Url(targetKey);
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }

    public String extractKeyFromUrl(String s3Url) {
        int idx = s3Url.indexOf(".amazonaws.com/");
        if (idx == -1) {
            throw new IllegalArgumentException("Invalid S3 URL format: " + s3Url);
        }
        return s3Url.substring(idx + ".amazonaws.com/".length());
    }

    private String buildS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }
}
