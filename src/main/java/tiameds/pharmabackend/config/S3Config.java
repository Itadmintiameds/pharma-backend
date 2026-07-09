package tiameds.pharmabackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.s3.access-key}")
    private String accessKeyId;

    @Value("${aws.s3.secret-key}")
    private String secretAccessKey;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (accessKeyId == null || accessKeyId.isBlank() || "dummy".equals(accessKeyId)) {
            throw new IllegalStateException(
                    "AWS credentials are not configured (aws.s3.access-key resolved to '" + accessKeyId + "'). "
                            + "The .env file was not loaded — reload the Maven project so the spring-dotenv "
                            + "dependency is on the classpath, and make sure the app runs from the project root.");
        }
        log.info("S3 client starting with access key {}**** in region {}",
                accessKeyId.substring(0, Math.min(8, accessKeyId.length())), region);
        return S3Client.builder()
                .region(Region.of(region))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .build();
    }
}
