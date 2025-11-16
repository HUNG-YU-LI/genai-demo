---
adr_number: 029
title: "Background Job Processing Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [005, 022, 028]
affected_viewpoints: ["concurrency", "deployment", "operational"]
affected_perspectives: ["performance", "scalability", "availability"]
---

# ADR-029: Background Job Processing Strategy

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要background job processing for:

- **Asynchronous Operations**: Email sending, notifications, report generation
- **Scheduled Tasks**: Daily inventory sync, order status updates, abandoned cart reminders
- **Long-Running Operations**: Bulk imports, data exports, image processing
- **Retry Logic**: Failed payment retries, external API call retries
- **Batch Processing**: End-of-day reconciliation, analytics aggregation
- **Event Processing**: Domain event 處理rs that don't need immediate execution
- **Resource-Intensive Tasks**: PDF generation, video transcoding, data analysis

### 業務上下文

**業務驅動因素**：

- 改善 用戶體驗 (don't block on slow operations)
- 處理 peak loads (queue jobs 期間 流量尖峰)
- Ensure reliability (retry failed operations automatically)
- 支援 scheduled operations (daily reports, reminders)
- 啟用 scalability (process jobs independently)
- 維持 system responsiveness (offload heavy tasks)

**Business Constraints**:

- 預算: $300/month 用於 job processing infrastructure
- Job processing latency < 5 minutes 用於 high-priority jobs
- 支援 用於 job priorities (critical, high, normal, low)
- Job retry 與 exponential backoff
- Job monitoring 和 alerting
- Dead letter queue 用於 failed jobs

### 技術上下文

**目前狀態**：

- Spring Boot 3.4.5 application
- Kafka 用於 event streaming (ADR-005)
- Redis 用於 分散式快取 (ADR-022)
- AWS 雲端基礎設施
- Multiple 應用程式實例 (horizontal scaling)

**需求**：

- Distributed job processing (multiple workers)
- Job scheduling (cron-like)
- Job priorities 和 queues
- Retry mechanism 與 exponential backoff
- Dead letter queue 用於 failed jobs
- Job monitoring 和 metrics
- Horizontal scalability
- At-least-once delivery guarantee
- Job persistence (survive restarts)

## 決策驅動因素

1. **Reliability**: At-least-once delivery, automatic retries
2. **Scalability**: 處理 10,000+ jobs/hour
3. **Performance**: Low latency 用於 high-priority jobs
4. **Features**: Scheduling, priorities, retries, DLQ
5. **Integration**: Spring Boot, AWS ecosystem
6. **成本**： Within $300/month budget
7. **Operations**: 容易monitor 和 troubleshoot
8. **Flexibility**: 支援 various job types

## 考慮的選項

### 選項 1： Spring Boot @Async with Database Queue

**描述**： Use Spring's @Async with database-backed job queue

**優點**：

- ✅ 簡單implement
- ✅ No additional infrastructure
- ✅ ACID transactions 與 jobs
- ✅ Built into Spring Boot
- ✅ 容易debug

**缺點**：

- ❌ Poor scalability (database bottleneck)
- ❌ No built-in scheduling
- ❌ Limited retry logic
- ❌ No priority queues
- ❌ Polling overhead
- ❌ Doesn't survive application restarts well
- ❌ No distributed coordination

**成本**： $0 (included in database)

**風險**： **High** - Insufficient for requirements

**Performance**: 100-500 jobs/minute

### 選項 2： AWS SQS + Spring Cloud AWS

**描述**： Use AWS SQS for job queues with Spring Cloud AWS integration

**優點**：

- ✅ Fully 託管服務
- ✅ Highly scalable (unlimited throughput)
- ✅ At-least-once delivery
- ✅ Dead letter queue 支援
- ✅ Message visibility timeout (retry)
- ✅ FIFO queues available
- ✅ 優秀的Spring Boot整合
- ✅ Pay-as-you-go pricing
- ✅ AWS native integration
- ✅ No 營運開銷

**缺點**：

- ⚠️ No built-in scheduling (need EventBridge)
- ⚠️ Limited message size (256KB)
- ⚠️ Eventual consistency
- ⚠️ No priority within queue (need multiple queues)

**成本**：

- SQS: $0.40/million requests (first 1M free)
- EventBridge: $1.00/million events
- Total: $50-100/month (10M jobs/month)

**風險**： **Low** - AWS managed service

**Performance**: 10,000+ jobs/second

### 選項 3： Redis-Based Queue (Spring Data Redis)

**描述**： Use Redis as job queue with Spring Data Redis

**優點**：

- ✅ Uses existing Redis infrastructure
- ✅ Fast (記憶體內)
- ✅ 簡單implement
- ✅ 良好的Spring Boot整合
- ✅ 支援s priorities (sorted sets)
- ✅ Pub/sub 用於 notifications

**缺點**：

- ❌ No built-in scheduling
- ❌ Limited persistence (AOF/RDB)
- ❌ Manual retry logic
- ❌ No dead letter queue (need to implement)
- ❌ Memory constraints
- ❌ 複雜的 distributed coordination

**成本**： $0 (uses existing Redis)

**風險**： **Medium** - Need to implement features

**Performance**: 5,000-10,000 jobs/second

### 選項 4： Quartz Scheduler + Database

**描述**： Use Quartz Scheduler for job scheduling and execution

**優點**：

- ✅ Mature scheduling library
- ✅ Cron-like scheduling
- ✅ Clustered mode (distributed)
- ✅ Job persistence
- ✅ Spring Boot integration
- ✅ Misfire handling

**缺點**：

- ❌ Database-backed (scalability limits)
- ❌ 複雜的 configuration
- ❌ Not designed 用於 high-throughput queues
- ❌ Limited retry logic
- ❌ No dead letter queue
- ❌ Polling overhead

**成本**： $0 (uses existing database)

**風險**： **Medium** - Scalability concerns

**Performance**: 100-1,000 jobs/minute

### 選項 5： Apache Kafka (Existing Infrastructure)

**描述**： Use existing Kafka infrastructure for job processing

**優點**：

- ✅ Already deployed (ADR-005)
- ✅ High throughput
- ✅ Durable (replicated)
- ✅ Ordered processing
- ✅ Exactly-once semantics
- ✅ 良好的 用於 event-driven jobs

**缺點**：

- ❌ No built-in scheduling
- ❌ 複雜的 retry logic
- ❌ No priority queues
- ❌ Overkill 用於 簡單的 jobs
- ❌ Higher operational 複雜的ity

**成本**： $0 (existing infrastructure)

**風險**： **Medium** - Complex for simple jobs

**Performance**: 100,000+ messages/second

## 決策結果

**選擇的選項**： **AWS SQS + Spring Cloud AWS + EventBridge Scheduler**

### 理由

AWS SQS 與 EventBridge被選擇的原因如下：

1. **Fully Managed**: No 營運開銷, AWS 處理s scaling
2. **Reliability**: At-least-once delivery, automatic retries
3. **Scalability**: Unlimited throughput, 處理s 10,000+ jobs/second
4. **Cost-Effective**: Pay-as-you-go, within 預算 ($50-100/month)
5. **Features**: Dead letter queue, visibility timeout, FIFO queues
6. **Integration**: 優秀的 Spring Boot 支援 via Spring Cloud AWS
7. **Scheduling**: EventBridge 用於 cron-like scheduling
8. **AWS Native**: Seamless integration 與 other AWS services
9. **Proven**: Used 透過 millions of applications worldwide

**Job Processing Architecture**:

**Queue Structure**:

- `ecommerce-jobs-critical`: Critical jobs (payment processing, order confirmation)
- `ecommerce-jobs-high`: High-priority jobs (email sending, notifications)
- `ecommerce-jobs-normal`: Normal jobs (report generation, data sync)
- `ecommerce-jobs-low`: Low-priority jobs (analytics, cleanup)
- `ecommerce-jobs-dlq`: Dead letter queue 用於 failed jobs

**Job Types**:

- **Immediate Jobs**: Processed as soon as possible (email, notifications)
- **Scheduled Jobs**: Triggered 透過 EventBridge (daily reports, reminders)
- **Delayed Jobs**: Delayed execution (abandoned cart after 1 hour)
- **Recurring Jobs**: Periodic execution (inventory sync every 15 minutes)

**Retry Strategy**:

- **Automatic Retries**: SQS visibility timeout (exponential backoff)
- **Max Retries**: 3 attempts before moving to DLQ
- **Backoff**: 1 minute, 5 minutes, 15 minutes
- **DLQ Processing**: Manual review 和 reprocessing

**EventBridge Scheduling**:

- Daily reports: 8:00 AM UTC
- Inventory sync: Every 15 minutes
- Abandoned cart reminders: Every hour
- Order status updates: Every 5 minutes
- Analytics aggregation: Midnight UTC

**為何不選 @Async**： Poor scalability, limited features, doesn't survive restarts.

**為何不選 Redis Queue**： Need to implement many features, memory constraints.

**為何不選 Quartz**： Database bottleneck, not designed 用於 high-throughput queues.

**為何不選 Kafka**： Overkill 用於 簡單的 jobs, 複雜的 retry logic.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Learn SQS 和 EventBridge | Training, code examples, documentation |
| Operations Team | Low | Monitor SQS 和 EventBridge | AWS 託管服務, CloudWatch dashboards |
| End Users | Positive | Faster 回應時間, 更好的 UX | N/A |
| Business | Positive | Reliable job processing, scalability | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All bounded contexts (job processing)
- Application services (job submission)
- Infrastructure layer (SQS client, EventBridge)
- Notification service (email, SMS)
- Report generation service
- Data synchronization services

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| SQS unavailability | Very Low | High | Fallback to database queue, circuit breaker |
| Job processing delays | Low | Medium | Monitor queue depth, scale workers |
| DLQ overflow | Low | Medium | Alerting, automated reprocessing |
| Cost overrun | Low | Low | Monitor usage, set alarms |
| Message loss | Very Low | High | At-least-once delivery, DLQ |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Setup （第 1 週）

- [x] Create SQS queues (critical, high, normal, low, DLQ)
- [x] Configure queue policies 和 IAM roles
- [x] Set up EventBridge scheduler
- [x] Configure CloudWatch alarms
- [x] Create monitoring dashboards

### 第 2 階段： Integration （第 2-3 週）

- [x] Integrate Spring Cloud AWS
- [x] Implement job submission service
- [x] Implement job processing workers
- [x] Add retry logic 和 error handling
- [x] Implement DLQ processing

### 第 3 階段： Job Types （第 4-5 週）

- [x] Implement email sending jobs
- [x] Implement notification jobs
- [x] Implement report generation jobs
- [x] Implement data sync jobs
- [x] Implement scheduled jobs (EventBridge)

### 第 4 階段： Testing & Optimization （第 6 週）

- [x] Load testing (10,000+ jobs/hour)
- [x] Failure testing (retry, DLQ)
- [x] Performance optimization
- [x] Documentation 和 training

### 回滾策略

**觸發條件**：

- Job processing failure rate > 5%
- Queue depth > 10,000 messages
- SQS errors > 1%
- Cost exceeds 預算 透過 > 50%

**回滾步驟**：

1. Disable job submission to SQS
2. Fall back to synchronous processing
3. Investigate 和 fix issues
4. Re-啟用 SQS gradually
5. Monitor performance

**回滾時間**： < 1 hour

## 監控和成功標準

### 成功指標

- ✅ Job processing success rate > 99%
- ✅ Job processing latency < 5 minutes (high-priority)
- ✅ Queue depth < 1,000 messages
- ✅ DLQ messages < 1% of total
- ✅ Cost within 預算 ($100/month)
- ✅ Zero job loss incidents

### 監控計畫

**CloudWatch Metrics**:

- SQS Queue Depth (per queue)
- SQS Messages Sent/Received
- SQS Message Age
- SQS DLQ Messages
- EventBridge Invocations
- EventBridge Failed Invocations
- Lambda Duration (for EventBridge targets)

**Application Metrics**:

```java
@Component
public class JobProcessingMetrics {
    private final Timer jobProcessingTime;
    private final Counter jobsProcessed;
    private final Counter jobsFailed;
    private final Gauge queueDepth;
    
    // Track job processing performance
}
```

**告警**：

- Queue depth > 5,000 messages
- Message age > 30 minutes
- DLQ messages > 100
- Job failure rate > 5%
- EventBridge failures > 1%
- Cost > $150/month

**審查時程**：

- Daily: Check job processing metrics
- Weekly: Review failed jobs 和 DLQ
- Monthly: Optimize job processing
- Quarterly: Job processing strategy review

## 後果

### 正面後果

- ✅ **Reliability**: At-least-once delivery, automatic retries
- ✅ **Scalability**: Unlimited throughput, 處理s peak loads
- ✅ **Performance**: Fast job processing, low latency
- ✅ **Cost-Effective**: Pay-as-you-go, within 預算
- ✅ **Managed Service**: No 營運開銷
- ✅ **Flexibility**: 支援 various job types 和 priorities
- ✅ **Monitoring**: Comprehensive metrics 和 alerting

### 負面後果

- ⚠️ **複雜的ity**: Additional AWS services to manage
- ⚠️ **Eventual Consistency**: Jobs processed asynchronously
- ⚠️ **Debugging**: 更難trace job execution
- ⚠️ **成本**： Additional AWS service costs
- ⚠️ **Learning Curve**: Team needs to learn SQS 和 EventBridge

### 技術債務

**已識別債務**：

1. Manual DLQ processing (可以 automate 與 Lambda)
2. 簡單的 retry logic (可以 add exponential backoff 與 jitter)
3. No job prioritization within queue (future enhancement)

**債務償還計畫**：

- **Q2 2026**: Implement automated DLQ processing
- **Q3 2026**: Add advanced retry strategies
- **Q4 2026**: Implement job prioritization 和 scheduling optimization

## 相關決策

- [ADR-005: Use Apache Kafka 用於 Event Streaming](005-use-kafka-for-event-streaming.md) - Kafka 用於 event-driven jobs
- [ADR-022: Distributed Locking 與 Redis](022-distributed-locking-with-redis.md) - Locking 用於 job coordination
- [ADR-028: File Storage Strategy 與 S3](028-file-storage-strategy-with-s3.md) - S3 用於 job artifacts

## 備註

### SQS Queue Configuration

```yaml
# SQS Queue: ecommerce-jobs-critical
Visibility Timeout: 30 seconds
Message Retention: 4 days
Receive Wait Time: 20 seconds (long polling)
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3
Encryption: SSE-SQS

# SQS Queue: ecommerce-jobs-high
Visibility Timeout: 60 seconds
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-normal
Visibility Timeout: 300 seconds (5 minutes)
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-low
Visibility Timeout: 600 seconds (10 minutes)
Message Retention: 4 days
Receive Wait Time: 20 seconds
Dead Letter Queue: ecommerce-jobs-dlq
Max Receive Count: 3

# SQS Queue: ecommerce-jobs-dlq
Message Retention: 14 days
Alarm: CloudWatch alarm when messages > 100
```

### Spring Boot SQS Integration

```java
@Configuration
public class SqsConfiguration {
    
    @Value("${aws.region}")
    private String region;
    
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        return SqsAsyncClient.builder()
            .region(Region.of(region))
            .build();
    }
    
    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
            .sqsAsyncClient(sqsAsyncClient)
            .build();
    }
}

@Service
public class JobSubmissionService {
    
    @Autowired
    private SqsTemplate sqsTemplate;
    
    @Value("${aws.sqs.queue.critical}")
    private String criticalQueue;
    
    @Value("${aws.sqs.queue.high}")
    private String highQueue;
    
    @Value("${aws.sqs.queue.normal}")
    private String normalQueue;
    
    public void submitJob(Job job, JobPriority priority) {
        String queueUrl = getQueueUrl(priority);
        
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(serializeJob(job))
            .messageAttributes(Map.of(
                "JobType", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(job.getType())
                    .build(),
                "Priority", MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(priority.name())
                    .build()
            ))
            .build();
        
        sqsTemplate.send(request);
    }
    
    public void submitDelayedJob(Job job, Duration delay) {
        String queueUrl = getQueueUrl(job.getPriority());
        
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(serializeJob(job))
            .delaySeconds((int) delay.getSeconds())
            .build();
        
        sqsTemplate.send(request);
    }
    
    private String getQueueUrl(JobPriority priority) {
        return switch (priority) {
            case CRITICAL -> criticalQueue;
            case HIGH -> highQueue;
            case NORMAL -> normalQueue;
            case LOW -> normalQueue;
        };
    }
}
```

### Job Processing Worker

```java
@Component
public class JobProcessingWorker {
    
    @Autowired
    private JobExecutor jobExecutor;
    
    @SqsListener(value = "${aws.sqs.queue.critical}", deletionPolicy = ON_SUCCESS)
    public void processCriticalJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.CRITICAL, acknowledgment);
    }
    
    @SqsListener(value = "${aws.sqs.queue.high}", deletionPolicy = ON_SUCCESS)
    public void processHighPriorityJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.HIGH, acknowledgment);
    }
    
    @SqsListener(value = "${aws.sqs.queue.normal}", deletionPolicy = ON_SUCCESS)
    public void processNormalJob(String message, Acknowledgment acknowledgment) {
        processJob(message, JobPriority.NORMAL, acknowledgment);
    }
    
    private void processJob(String message, JobPriority priority, Acknowledgment acknowledgment) {
        try {
            Job job = deserializeJob(message);
            
            // Execute job
            jobExecutor.execute(job);
            
            // Acknowledge successful processing
            acknowledgment.acknowledge();
            
            // Record metrics
            recordJobSuccess(job, priority);
            
        } catch (Exception e) {
            // Log error
            logger.error("Job processing failed", e);
            
            // Record metrics
            recordJobFailure(job, priority, e);
            
            // Don't acknowledge - message will be retried
            // After max retries, will move to DLQ
        }
    }
}

@Service
public class JobExecutor {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ReportService reportService;
    
    public void execute(Job job) {
        switch (job.getType()) {
            case "SEND_EMAIL" -> executeSendEmail(job);
            case "SEND_NOTIFICATION" -> executeSendNotification(job);
            case "GENERATE_REPORT" -> executeGenerateReport(job);
            case "SYNC_INVENTORY" -> executeSyncInventory(job);
            default -> throw new UnsupportedJobTypeException(job.getType());
        }
    }
    
    private void executeSendEmail(Job job) {
        EmailJobData data = job.getData(EmailJobData.class);
        emailService.sendEmail(data.getTo(), data.getSubject(), data.getBody());
    }
    
    private void executeSendNotification(Job job) {
        NotificationJobData data = job.getData(NotificationJobData.class);
        notificationService.sendNotification(data.getUserId(), data.getMessage());
    }
    
    private void executeGenerateReport(Job job) {
        ReportJobData data = job.getData(ReportJobData.class);
        reportService.generateReport(data.getReportType(), data.getParameters());
    }
}
```

### EventBridge Scheduler Configuration

```yaml
# EventBridge Rule: Daily Reports
Name: ecommerce-daily-reports
Schedule: cron(0 8 * * ? *)  # 8:00 AM UTC daily
Target: Lambda function to submit SQS job
Input:
  jobType: GENERATE_REPORT
  reportType: DAILY_SALES
  priority: NORMAL

# EventBridge Rule: Inventory Sync
Name: ecommerce-inventory-sync
Schedule: rate(15 minutes)
Target: Lambda function to submit SQS job
Input:
  jobType: SYNC_INVENTORY
  priority: HIGH

# EventBridge Rule: Abandoned Cart Reminders
Name: ecommerce-abandoned-cart
Schedule: rate(1 hour)
Target: Lambda function to submit SQS job
Input:
  jobType: SEND_ABANDONED_CART_REMINDER
  priority: NORMAL

# EventBridge Rule: Order Status Updates
Name: ecommerce-order-status
Schedule: rate(5 minutes)
Target: Lambda function to submit SQS job
Input:
  jobType: UPDATE_ORDER_STATUS
  priority: HIGH
```

### Lambda Function 用於 EventBridge

```javascript
// Lambda function to submit jobs to SQS from EventBridge
const AWS = require('aws-sdk');
const sqs = new AWS.SQS();

exports.handler = async (event) => {
    const queueUrl = process.env.SQS_QUEUE_URL;
    
    const params = {
        QueueUrl: queueUrl,
        MessageBody: JSON.stringify(event),
        MessageAttributes: {
            'JobType': {
                DataType: 'String',
                StringValue: event.jobType
            },
            'Priority': {
                DataType: 'String',
                StringValue: event.priority
            },
            'Source': {
                DataType: 'String',
                StringValue: 'EventBridge'
            }
        }
    };
    
    try {
        await sqs.sendMessage(params).promise();
        return { statusCode: 200, body: 'Job submitted successfully' };
    } catch (error) {
        console.error('Error submitting job:', error);
        throw error;
    }
};
```

### DLQ Processing

```java
@Component
public class DlqProcessor {
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processDlq() {
        // Receive messages from DLQ
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(dlqUrl)
            .maxNumberOfMessages(10)
            .build();
        
        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        
        for (Message message : response.messages()) {
            try {
                // Log failed job
                logFailedJob(message);
                
                // Optionally: Attempt reprocessing with manual intervention
                // Or: Send alert to operations team
                
                // Delete from DLQ after logging
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
                    
            } catch (Exception e) {
                logger.error("Error processing DLQ message", e);
            }
        }
    }
}
```

### Job Model

```java
@Data
public class Job {
    private String id;
    private String type;
    private JobPriority priority;
    private Map<String, Object> data;
    private Instant createdAt;
    private Instant scheduledAt;
    private int retryCount;
    private String errorMessage;
    
    public <T> T getData(Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(data, clazz);
    }
}

public enum JobPriority {
    CRITICAL,  // Process immediately
    HIGH,      // Process within 1 minute
    NORMAL,    // Process within 5 minutes
    LOW        // Process within 15 minutes
}

@Data
public class EmailJobData {
    private String to;
    private String subject;
    private String body;
    private List<String> attachments;
}

@Data
public class NotificationJobData {
    private String userId;
    private String message;
    private NotificationType type;
}

@Data
public class ReportJobData {
    private String reportType;
    private Map<String, Object> parameters;
    private String outputFormat;
}
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
