---
adr_number: 028
title: "File Storage Strategy 與 S3"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 011, 029]
affected_viewpoints: ["deployment", "information", "operational"]
affected_perspectives: ["performance", "scalability", "cost", "security"]
---

# ADR-028: File Storage Strategy 與 S3

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要file storage for:

- Product images (thumbnails, full-size, zoom images)
- User-generated content (product reviews 與 photos, profile pictures)
- Order documents (invoices, receipts, shipping labels)
- Marketing assets (banners, promotional images, videos)
- Static assets (CSS, JavaScript, fonts)
- File uploads from admin panel (bulk imports, reports)
- Backup files (database backups, log archives)

### 業務上下文

**業務驅動因素**：

- 支援 high-quality product images (critical 用於 conversion)
- 啟用 user-generated content (reviews 與 photos increase trust)
- Store order documents 用於 compliance (7-year retention)
- Deliver static assets globally 與 low latency
- 處理 file uploads from multiple sources (web, mobile, admin)
- Scale to millions of files (100K+ products, 1M+ orders)
- Cost-effective storage 與 automatic lifecycle management

**Business Constraints**:

- 預算: $500/month 用於 file storage
- Image delivery latency < 200ms globally
- 支援 用於 multiple file types (images, PDFs, videos)
- Secure access control (public, private, signed URLs)
- Compliance 與 data retention policies
- High availability (99.99% uptime)

### 技術上下文

**目前狀態**：

- PostgreSQL 主要資料庫 (ADR-001)
- AWS 雲端基礎設施 (ADR-011)
- Spring Boot 3.4.5 application
- Multiple 應用程式實例 (horizontal scaling)
- No dedicated file storage solution

**需求**：

- Scalable storage (petabytes if needed)
- High availability 和 durability (99.999999999%)
- Global content delivery (CDN integration)
- Secure access control (IAM, signed URLs)
- Automatic image optimization 和 resizing
- Lifecycle management (archival, deletion)
- Versioning 用於 critical files
- Integration 與 Spring Boot
- Cost-effective storage tiers

## 決策驅動因素

1. **Scalability**: 處理 millions of files, petabytes of data
2. **Performance**: Fast upload/download, global delivery
3. **成本**： Within $500/month budget, pay-as-you-go
4. **Durability**: 99.999999999% (11 nines)
5. **Availability**: 99.99% uptime
6. **Security**: Access control, encryption, signed URLs
7. **Integration**: Spring Boot, AWS ecosystem
8. **Features**: Versioning, lifecycle, CDN, image processing

## 考慮的選項

### 選項 1： Amazon S3 with CloudFront CDN (Recommended)

**描述**： Use S3 for storage with CloudFront for global content delivery

**優點**：

- ✅ Unlimited scalability (petabytes)
- ✅ 99.999999999% durability
- ✅ 99.99% availability
- ✅ Multiple storage tiers (Standard, IA, Glacier)
- ✅ Automatic lifecycle management
- ✅ Versioning 支援
- ✅ CloudFront CDN integration
- ✅ Image optimization (Lambda@Edge)
- ✅ Signed URLs 用於 secure access
- ✅ Event notifications (S3 → Lambda)
- ✅ 優秀的Spring Boot整合
- ✅ Pay-as-you-go pricing
- ✅ AWS native integration

**缺點**：

- ⚠️ Data transfer costs (egress)
- ⚠️ CloudFront costs 用於 high traffic
- ⚠️ Learning curve 用於 optimization

**成本**：

- S3 Storage: $23/TB/month (Standard)
- S3 Requests: $0.005/1000 PUT, $0.0004/1000 GET
- CloudFront: $0.085/GB (first 10TB)
- Total Estimate: $300-500/month (100TB storage, 10TB transfer)

**風險**： **Low** - Industry-standard solution

**Performance**:

- Upload: 100-500ms
- Download (CDN): 50-200ms globally
- Throughput: 3,500 PUT/s, 5,500 GET/s per prefix

### 選項 2： Local File System Storage

**描述**： Store files on application server file system

**優點**：

- ✅ 簡單implement
- ✅ No additional cost
- ✅ Fast local access
- ✅ No network latency

**缺點**：

- ❌ Not scalable (limited disk space)
- ❌ No redundancy (single point of failure)
- ❌ 難以share 跨 instances
- ❌ No CDN integration
- ❌ Manual backup required
- ❌ No lifecycle management
- ❌ Doesn't work 與 horizontal scaling
- ❌ No global delivery

**成本**： $0 (included in server cost)

**風險**： **High** - Insufficient for production

**Performance**: Fast locally, slow globally

### 選項 3： Database BLOB Storage (PostgreSQL)

**描述**： Store files as BLOBs in PostgreSQL database

**優點**：

- ✅ ACID transactions 與 files
- ✅ 簡單implement
- ✅ No additional infrastructure
- ✅ Backup included 與 database

**缺點**：

- ❌ Poor performance 用於 大型的 files
- ❌ Increases database size 和 cost
- ❌ Slow queries 與 大型的 BLOBs
- ❌ No CDN integration
- ❌ Limited to database size
- ❌ Expensive storage ($0.115/GB vs $0.023/GB)
- ❌ No image optimization
- ❌ Increases backup time

**成本**： $115/TB/month (5x more than S3)

**風險**： **High** - Performance and cost issues

**Performance**: 500-2000ms 用於 大型的 files

### 選項 4： Self-Hosted Object Storage (MinIO)

**描述**： Deploy MinIO on EC2 instances for S3-compatible storage

**優點**：

- ✅ S3-compatible API
- ✅ Open-source
- ✅ Full control
- ✅ No data transfer costs

**缺點**：

- ❌ Operational overhead (manage servers)
- ❌ Need to 處理 redundancy
- ❌ No built-in CDN
- ❌ Manual scaling
- ❌ Higher total cost (EC2 + EBS)
- ❌ Need to manage backups
- ❌ Lower durability than S3

**成本**： $400-600/month (EC2 + EBS + bandwidth)

**風險**： **Medium** - Operational complexity

**Performance**: Similar to S3, 但 no CDN

## 決策結果

**選擇的選項**： **Amazon S3 with CloudFront CDN**

### 理由

S3 與 CloudFront被選擇的原因如下：

1. **Scalability**: Unlimited storage, 處理s millions of files
2. **Durability**: 99.999999999% (11 nines) - industry-leading
3. **Availability**: 99.99% uptime 與 自動容錯移轉
4. **Cost-Effective**: Pay-as-you-go, multiple storage tiers
5. **Performance**: CloudFront CDN 用於 global delivery (< 200ms)
6. **Features**: Versioning, lifecycle, events, image optimization
7. **Security**: IAM, encryption, signed URLs, bucket policies
8. **Integration**: 優秀的 Spring Boot 支援, AWS ecosystem
9. **Managed Service**: No 營運開銷
10. **Proven**: Used 透過 Netflix, Airbnb, 和 millions of applications

**Storage Strategy**:

**S3 Bucket Organization**:

- `ecommerce-products-{env}`: Product images (public via CloudFront)
- `ecommerce-user-content-{env}`: User uploads (private, signed URLs)
- `ecommerce-documents-{env}`: Order documents (private, encrypted)
- `ecommerce-static-{env}`: Static assets (public via CloudFront)
- `ecommerce-backups-{env}`: Backups 和 archives (Glacier)

**Storage Tiers**:

- **S3 Standard**: Active files (product images, static assets)
- **S3 Intelligent-Tiering**: User content (automatic optimization)
- **S3 Glacier**: Archives (old orders, backups)

**CloudFront Distribution**:

- **Products**: Global edge caching, image optimization
- **Static Assets**: Global edge caching, compression
- **User Content**: Signed URLs, regional caching

**Image Optimization**:

- Lambda@Edge 用於 on-the-fly resizing
- WebP format conversion 用於 modern browsers
- Lazy loading 和 progressive images

**為何不選 Local File System**： Not scalable, no redundancy, doesn't work 與 horizontal scaling.

**為何不選 Database BLOBs**： Poor performance, expensive, increases 資料庫負載.

**為何不選 MinIO**： Operational overhead, higher total cost, no built-in CDN.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Learn S3 SDK 和 best practices | Training, code examples, documentation |
| Operations Team | Low | Monitor S3 和 CloudFront | AWS 託管服務, CloudWatch dashboards |
| End Users | Positive | Faster image loading, 更好的 UX | N/A |
| Business | Positive | Cost-effective, scalable storage | N/A |
| Security Team | Low | Review access policies | IAM policies, encryption |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- Product bounded context (product images)
- Order bounded context (invoices, receipts)
- Customer bounded context (profile pictures)
- Review bounded context (review photos)
- Application services (file upload/download)
- Infrastructure layer (S3 client, CloudFront)
- Frontend applications (image URLs, CDN)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| S3 unavailability | Very Low | High | Multi-region replication, fallback URLs |
| Cost overrun | Low | Medium | Lifecycle policies, monitoring, alerts |
| Data loss | Very Low | High | Versioning, cross-region replication |
| Slow uploads | Low | Medium | Multipart upload, transfer acceleration |
| CDN cache issues | Low | Low | Cache invalidation, versioned URLs |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup （第 1 週）

- [x] Create S3 buckets 與 proper naming
- [x] Configure bucket policies 和 IAM roles
- [x] 啟用 versioning 用於 critical buckets
- [x] Set up lifecycle policies
- [x] Configure encryption (SSE-S3)
- [x] Create CloudFront distributions

### 第 2 階段： Integration （第 2-3 週）

- [x] Integrate Spring Boot 與 AWS S3 SDK
- [x] Implement file upload service
- [x] Implement file download service
- [x] Add signed URL generation
- [x] Implement multipart upload 用於 大型的 files
- [x] Add file metadata tracking

### 第 3 階段： Image Optimization （第 4 週）

- [x] Set up Lambda@Edge 用於 image resizing
- [x] Implement WebP conversion
- [x] Add responsive image 支援
- [x] Configure CloudFront caching
- [x] Implement lazy loading

### 第 4 階段： Migration & Testing （第 5-6 週）

- [x] Migrate existing files to S3
- [x] Update application to use S3 URLs
- [x] Load testing (1000+ concurrent uploads)
- [x] Performance testing (CDN latency)
- [x] Security testing (access control)

### 回滾策略

**觸發條件**：

- S3 availability < 99%
- Upload failure rate > 5%
- Cost exceeds 預算 透過 > 50%
- Performance degradation > 20%

**回滾步驟**：

1. Disable S3 uploads
2. Fall back to local storage temporarily
3. Investigate 和 fix issues
4. Re-啟用 S3 gradually
5. Monitor performance

**回滾時間**： < 2 hours

## 監控和成功標準

### 成功指標

- ✅ File upload success rate > 99%
- ✅ File download latency < 200ms (95th percentile)
- ✅ S3 availability > 99.99%
- ✅ CloudFront cache hit rate > 80%
- ✅ Cost within 預算 ($500/month)
- ✅ Zero data loss incidents

### 監控計畫

**CloudWatch Metrics**:

- S3 Bucket Size
- S3 Request Count (PUT, GET, DELETE)
- S3 4xx/5xx Errors
- CloudFront Request Count
- CloudFront Cache Hit Rate
- CloudFront Error Rate
- Data Transfer (in/out)

**Application Metrics**:

```java
@Component
public class FileStorageMetrics {
    private final Timer uploadTime;
    private final Timer downloadTime;
    private final Counter uploadSuccess;
    private final Counter uploadFailure;
    private final Gauge storageUsed;
    
    // Track file storage performance
}
```

**告警**：

- S3 error rate > 1%
- Upload failure rate > 5%
- CloudFront error rate > 1%
- Cache hit rate < 70%
- Storage cost > $600/month
- Data transfer > 20TB/month

**審查時程**：

- Daily: Check storage metrics
- Weekly: Review cost 和 usage
- Monthly: Optimize storage 和 CDN
- Quarterly: Storage strategy review

## 後果

### 正面後果

- ✅ **Scalability**: Unlimited storage capacity
- ✅ **Durability**: 99.999999999% (11 nines)
- ✅ **Performance**: Fast global delivery via CDN
- ✅ **Cost-Effective**: Pay-as-you-go, multiple tiers
- ✅ **Reliability**: 99.99% availability
- ✅ **Security**: Encryption, access control, signed URLs
- ✅ **Features**: Versioning, lifecycle, events
- ✅ **Managed Service**: No 營運開銷

### 負面後果

- ⚠️ **複雜的ity**: Additional AWS service to manage
- ⚠️ **成本**： Data transfer costs can add up
- ⚠️ **Latency**: Network latency 用於 uploads
- ⚠️ **Dependency**: Relies on AWS availability
- ⚠️ **Learning Curve**: Team needs to learn S3 best practices

### 技術債務

**已識別債務**：

1. Manual image optimization (可以 automate 與 Lambda)
2. 簡單的 lifecycle policies (可以 optimize based on usage)
3. No automatic cost optimization (future enhancement)

**債務償還計畫**：

- **Q2 2026**: Implement automated image optimization pipeline
- **Q3 2026**: Add ML-based lifecycle optimization
- **Q4 2026**: Implement cost optimization recommendations

## 相關決策

- [ADR-001: Use PostgreSQL 用於 Primary Database](001-use-postgresql-for-primary-database.md) - Database stores file metadata
- [ADR-011: AWS Cloud Infrastructure](011-aws-cloud-infrastructure.md) - S3 is part of AWS ecosystem
- [ADR-029: Background Job Processing Strategy](029-background-job-processing-strategy.md) - Background jobs 用於 file processing

## 備註

### S3 Bucket Configuration

```yaml
# S3 Bucket: ecommerce-products-prod
Versioning: Enabled
Encryption: SSE-S3 (AES-256)
Public Access: Blocked (access via CloudFront only)
Lifecycle Rules:

  - Transition to Intelligent-Tiering after 30 days
  - Delete old versions after 90 days

CORS: Enabled for web uploads
Event Notifications: Lambda for image processing

# S3 Bucket: ecommerce-user-content-prod
Versioning: Enabled
Encryption: SSE-S3 (AES-256)
Public Access: Blocked (signed URLs only)
Lifecycle Rules:

  - Transition to Intelligent-Tiering after 90 days
  - Archive to Glacier after 1 year

Object Lock: Enabled for compliance

# S3 Bucket: ecommerce-documents-prod
Versioning: Enabled
Encryption: SSE-KMS (customer managed key)
Public Access: Blocked
Lifecycle Rules:

  - Archive to Glacier after 90 days
  - Retain for 7 years (compliance)

Object Lock: Enabled (compliance mode)

# S3 Bucket: ecommerce-backups-prod
Versioning: Disabled
Encryption: SSE-S3
Public Access: Blocked
Lifecycle Rules:

  - Transition to Glacier immediately
  - Delete after 30 days

```

### Spring Boot S3 Integration

```java
@Configuration
public class S3Configuration {
    
    @Value("${aws.region}")
    private String region;
    
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .build();
    }
    
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
            .region(Region.of(region))
            .build();
    }
}

@Service
public class FileStorageService {
    
    @Autowired
    private S3Client s3Client;
    
    @Autowired
    private S3Presigner s3Presigner;
    
    @Value("${aws.s3.bucket.products}")
    private String productsBucket;
    
    public String uploadFile(String key, InputStream inputStream, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .contentType(contentType)
            .build();
        
        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
        
        return getPublicUrl(key);
    }
    
    public String uploadLargeFile(String key, File file) {
        // Use multipart upload for files > 5MB
        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .build();
        
        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();
        
        // Upload parts (5MB each)
        List<CompletedPart> completedParts = new ArrayList<>();
        long partSize = 5 * 1024 * 1024; // 5MB
        long fileSize = file.length();
        int partNumber = 1;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            for (long position = 0; position < fileSize; position += partSize) {
                long currentPartSize = Math.min(partSize, fileSize - position);
                
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(productsBucket)
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();
                
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                    uploadPartRequest,
                    RequestBody.fromInputStream(fis, currentPartSize)
                );
                
                completedParts.add(CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(uploadPartResponse.eTag())
                    .build());
                
                partNumber++;
            }
        }
        
        // Complete multipart upload
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .uploadId(uploadId)
            .multipartUpload(CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build())
            .build();
        
        s3Client.completeMultipartUpload(completeRequest);
        
        return getPublicUrl(key);
    }
    
    public String generateSignedUrl(String key, Duration expiration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .build();
        
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getObjectRequest)
            .build();
        
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        
        return presignedRequest.url().toString();
    }
    
    public InputStream downloadFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .build();
        
        return s3Client.getObject(request);
    }
    
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(productsBucket)
            .key(key)
            .build();
        
        s3Client.deleteObject(request);
    }
    
    private String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", 
            productsBucket, region, key);
    }
}
```

### File Upload Controller

```java
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") FileType type) {
        
        // Validate file
        validateFile(file, type);
        
        // Generate unique key
        String key = generateFileKey(file.getOriginalFilename(), type);
        
        // Upload to S3
        String url = fileStorageService.uploadFile(
            key,
            file.getInputStream(),
            file.getContentType()
        );
        
        // Save metadata to database
        FileMetadata metadata = saveFileMetadata(key, file, url);
        
        return ResponseEntity.ok(FileUploadResponse.from(metadata));
    }
    
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException(fileId));
        
        // Generate signed URL for private files
        if (metadata.isPrivate()) {
            String signedUrl = fileStorageService.generateSignedUrl(
                metadata.getKey(),
                Duration.ofMinutes(15)
            );
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(signedUrl))
                .build();
        }
        
        // Redirect to public URL
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(metadata.getUrl()))
            .build();
    }
}
```

### CloudFront Configuration

```yaml
# CloudFront Distribution: Products
Origin: ecommerce-products-prod.s3.amazonaws.com
Origin Access Identity: Enabled (restrict S3 access)
Cache Behavior:

  - Path: /images/*
  - TTL: 86400 (1 day)
  - Compress: Enabled
  - Viewer Protocol: Redirect HTTP to HTTPS

Lambda@Edge:

  - Viewer Request: Image optimization
  - Origin Response: WebP conversion

Price Class: All Edge Locations
SSL Certificate: ACM certificate
Custom Domain: cdn.ecommerce.com
```

### Image Optimization Lambda@Edge

```javascript
// Lambda@Edge function for image optimization
exports.handler = async (event) => {
    const request = event.Records[0].cf.request;
    const uri = request.uri;
    
    // Parse query parameters for image transformations
    const params = new URLSearchParams(request.querystring);
    const width = params.get('w');
    const height = params.get('h');
    const quality = params.get('q') || '80';
    const format = params.get('f') || 'auto';
    
    // Modify URI to include transformations
    if (width || height) {
        const transformations = [];
        if (width) transformations.push(`w_${width}`);
        if (height) transformations.push(`h_${height}`);
        transformations.push(`q_${quality}`);
        
        // Insert transformations into URI
        const parts = uri.split('/');
        parts.splice(1, 0, transformations.join(','));
        request.uri = parts.join('/');
    }
    
    // WebP support
    const headers = request.headers;
    if (format === 'auto' && headers.accept) {
        const accept = headers.accept[0].value;
        if (accept.includes('image/webp')) {
            request.uri = request.uri.replace(/\.(jpg|jpeg|png)$/, '.webp');
        }
    }
    
    return request;
};
```

### File Naming Convention

```text
Pattern: {context}/{type}/{date}/{uuid}.{extension}

Examples:

- products/images/2025/10/25/550e8400-e29b-41d4-a716-446655440000.jpg
- users/avatars/2025/10/25/550e8400-e29b-41d4-a716-446655440001.png
- orders/invoices/2025/10/25/550e8400-e29b-41d4-a716-446655440002.pdf
- reviews/photos/2025/10/25/550e8400-e29b-41d4-a716-446655440003.jpg

```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
