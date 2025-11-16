---
adr_number: 023
title: "API Rate Limiting Strategy (Token Bucket vs Leaky Bucket)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [004, 009, 022, 050]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["performance", "security", "availability"]
---

# ADR-023: API Rate Limiting Strategy (Token Bucket vs Leaky Bucket)

## 狀態

**Accepted** - 2025-10-25

## 上下文

### 問題陳述

企業電子商務平台需要速率限制來：

- Protect backend services from overload 和 abuse
- Ensure fair resource allocation among users
- Prevent DDoS attacks 和 API abuse
- 維持 system stability 期間 流量尖峰
- 支援 different rate limits 用於 different user tiers (free, premium, enterprise)
- 提供 graceful degradation under high load
- 啟用 burst traffic handling 用於 legitimate 使用案例s

### 業務上下文

**業務驅動因素**：

- Protect system from malicious traffic (DDoS, scraping)
- Ensure fair usage 跨 all customers
- 支援 tiered pricing model (different limits per tier)
- 維持 99.9% availability 期間 流量尖峰
- Prevent resource exhaustion from single user
- 啟用 burst capacity 用於 legitimate peak usage

**Business Constraints**:

- 必須 not impact legitimate 用戶體驗
- Different limits 用於 different API endpoints
- 支援 用於 API key-based 速率限制
- Real-time rate limit feedback to clients
- Cost-effective implementation

### 技術上下文

**目前狀態**：

- RESTful API design (ADR-009)
- Redis 分散式快取 (ADR-004)
- Distributed locking (ADR-022)
- API security strategy (ADR-050)
- Multiple 應用程式實例 (horizontal scaling)

**需求**：

- Multi-level 速率限制 (global, per-user, per-IP, per-endpoint)
- 支援 用於 burst traffic (allow temporary spikes)
- Distributed 速率限制 跨 instances
- Low latency (< 5ms overhead)
- Real-time limit tracking 和 feedback
- Configurable limits per user tier
- Rate limit headers in responses

## 決策驅動因素

1. **Burst Handling**: Allow legitimate burst traffic
2. **Fairness**: Prevent single user from monopolizing resources
3. **Performance**: < 5ms overhead per request
4. **Flexibility**: Different limits 用於 different endpoints/users
5. **Distributed**: Work 跨 multiple instances
6. **User Experience**: Clear feedback on rate limits
7. **成本**： Leverage existing infrastructure
8. **Security**: Protect against DDoS 和 abuse

## 考慮的選項

### 選項 1： Token Bucket Algorithm (Recommended)

**描述**： Users accumulate tokens at a fixed rate, consume tokens per request, allows burst traffic

**優點**：

- ✅ Allows burst traffic (accumulated tokens)
- ✅ Flexible rate control (different token costs)
- ✅ 更好的 用戶體驗 (smooth traffic)
- ✅ Industry standard (AWS, Google, Stripe)
- ✅ 簡單implement 與 Redis
- ✅ 支援s different user tiers easily
- ✅ Predictable behavior
- ✅ Low latency (< 5ms)

**缺點**：

- ⚠️ 可以 allow 大型的 bursts if bucket is full
- ⚠️ Requires token refill logic
- ⚠️ More 複雜的 than fixed window

**成本**：

- Development: 5 person-days
- Infrastructure: $0 (uses existing Redis)
- Maintenance: Low

**風險**： **Low** - Well-understood algorithm

**Implementation 複雜的ity**: Medium

### 選項 2： Leaky Bucket Algorithm

**描述**： Requests enter a queue, processed at fixed rate, excess requests overflow

**優點**：

- ✅ Smooth, constant output rate
- ✅ Prevents burst traffic completely
- ✅ 簡單的 conceptual model
- ✅ Predictable resource usage

**缺點**：

- ❌ No burst traffic 支援 (poor UX)
- ❌ Requests queued (increased latency)
- ❌ Queue management 複雜的ity
- ❌ Memory overhead 用於 queue
- ❌ 更難implement distributed
- ❌ Less flexible 用於 different endpoints

**成本**：

- Development: 8 person-days
- Infrastructure: $0 (uses existing Redis)
- Maintenance: Medium

**風險**： **Medium** - Queue management complexity

**Implementation 複雜的ity**: High

### 選項 3： Fixed Window Counter

**描述**： Count requests in fixed time windows (e.g., per minute)

**優點**：

- ✅ Very 簡單的 to implement
- ✅ Low memory usage
- ✅ Fast (< 2ms)
- ✅ 容易understand

**缺點**：

- ❌ Burst at window boundaries (2x limit possible)
- ❌ Unfair (early requests get priority)
- ❌ Poor 用戶體驗
- ❌ No burst handling

**成本**：

- Development: 2 person-days
- Infrastructure: $0
- Maintenance: Low

**風險**： **Medium** - Boundary burst problem

**Implementation 複雜的ity**: Low

### 選項 4： Sliding Window Log

**描述**： Track timestamp of each request, count requests in sliding window

**優點**：

- ✅ Accurate 速率限制
- ✅ No boundary burst problem
- ✅ Fair distribution

**缺點**：

- ❌ High memory usage (store all timestamps)
- ❌ Expensive to compute (s可以 all timestamps)
- ❌ Doesn't scale well
- ❌ No burst 支援

**成本**：

- Development: 6 person-days
- Infrastructure: Higher Redis memory
- Maintenance: High

**風險**： **High** - Scalability concerns

**Implementation 複雜的ity**: High

## 決策結果

**選擇的選項**： **Token Bucket Algorithm**

### 理由

Token Bucket被選擇的原因如下：

1. **Burst 支援**: Allows legitimate burst traffic (accumulated tokens)
2. **User Experience**: Smooth traffic handling, 更好的 than strict limits
3. **Industry Standard**: Used 透過 AWS, Google Cloud, Stripe, GitHub
4. **Flexibility**: 容易configure different limits per endpoint/user
5. **Performance**: Low latency (< 5ms) 與 Redis implementation
6. **Distributed**: Works well 跨 multiple instances
7. **Cost-Effective**: Uses existing Redis infrastructure
8. **Proven**: Battle-tested in production systems

**Rate Limiting Strategy**:

**Multi-Level Limits**:

- **Global**: 10,000 requests/minute (system-wide)
- **Per-User**: 100 requests/minute (authenticated users)
- **Per-IP**: 1,000 requests/minute (anonymous users)
- **Per-Endpoint Sensitive**: 10 requests/minute (payment, admin)

**User Tiers**:

- **Free**: 100 req/min, burst 150
- **Premium**: 500 req/min, burst 750
- **Enterprise**: 2,000 req/min, burst 3,000

**Token Bucket Parameters**:

- **Bucket Capacity**: 1.5x rate limit (allows 50% burst)
- **Refill Rate**: Rate limit per minute
- **Token Cost**: 1 token per request (可以 vary 透過 endpoint)

**為何不選 Leaky Bucket**： No burst 支援, poor 用戶體驗, higher 複雜的ity.

**為何不選 Fixed Window**： Boundary burst problem, unfair distribution.

**為何不選 Sliding Window Log**： High memory usage, doesn't scale well.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Implement 速率限制 logic | Code examples, library 支援 |
| Operations Team | Low | Monitor rate limit metrics | Dashboards 和 alerts |
| End Users | Low | May see 429 errors if exceeded | Clear error messages, retry-after headers |
| API Consumers | Medium | Need to 處理 rate limits | Documentation, SDKs 與 retry logic |
| Business | Positive | Protected from abuse, fair usage | N/A |
| Security Team | Positive | DDoS protection | N/A |

### 影響半徑

**選擇的影響半徑**： **System**

影響：

- All API endpoints (速率限制 middleware)
- Authentication layer (user tier identification)
- Infrastructure layer (Redis rate limit storage)
- API Gateway (rate limit enforcement)
- Monitoring 和 alerting (rate limit metrics)

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positives | Medium | Medium | Generous burst capacity, monitoring |
| Redis unavailability | Low | High | Fallback to 記憶體內, circuit breaker |
| Distributed clock skew | Low | Low | Use Redis time, not local time |
| Rate limit bypass | Low | High | Multiple enforcement layers |
| Performance impact | Low | Medium | Optimize Redis operations, caching |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Core Implementation （第 1-2 週）

- [x] Implement Token Bucket algorithm 與 Redis
- [x] Create rate limiter service
- [x] Add rate limit middleware
- [x] Implement multi-level 速率限制
- [x] Add rate limit headers to responses

### 第 2 階段： User Tier Support （第 3 週）

- [x] Implement user tier identification
- [x] Configure limits per tier
- [x] Add API key-based 速率限制
- [x] Implement endpoint-specific limits
- [x] Add rate limit bypass 用於 internal services

### 第 3 階段： Monitoring & Feedback （第 4 週）

- [x] Add rate limit metrics
- [x] Create rate limit dashboards
- [x] Implement rate limit alerts
- [x] Add rate limit logging
- [x] Create rate limit documentation

### 第 4 階段： Optimization （第 5 週）

- [x] Performance testing 和 tuning
- [x] Optimize Redis operations
- [x] Add rate limit caching
- [x] Implement graceful degradation
- [x] Load testing 與 rate limits

### 回滾策略

**觸發條件**：

- Rate limiter latency > 10ms
- False positive rate > 5%
- Redis errors > 1%
- User complaints > threshold

**回滾步驟**：

1. Disable 速率限制 middleware
2. Allow all requests through
3. Investigate 和 fix issues
4. Re-啟用 與 higher limits
5. Gradually tighten limits

**回滾時間**： < 15 minutes

## 監控和成功標準

### 成功指標

- ✅ Rate limiter latency < 5ms (95th percentile)
- ✅ False positive rate < 1%
- ✅ DDoS attack mitigation > 99%
- ✅ System availability 維持ed at 99.9%
- ✅ User complaints < 0.1% of requests
- ✅ Rate limit bypass attempts detected

### 監控計畫

**Application Metrics**:

```java
@Component
public class RateLimitMetrics {
    private final Counter rateLimitExceeded;
    private final Counter rateLimitAllowed;
    private final Timer rateLimitCheckTime;
    private final Gauge activeRateLimits;
    
    // Track rate limit performance
}
```

**CloudWatch Metrics**:

- Rate limit checks per second
- Rate limit exceeded count
- Rate limit check latency
- Active rate limits 透過 user/IP
- Burst usage percentage
- Token bucket fill rate

**告警**：

- Rate limit check latency > 10ms
- Rate limit exceeded spike (> 100/min)
- Redis rate limit errors > 1%
- Burst capacity exhausted > 80%
- Suspicious rate limit patterns

**審查時程**：

- Daily: Check rate limit metrics
- Weekly: Review rate limit patterns
- Monthly: Adjust limits based on usage
- Quarterly: Rate limit strategy review

## 後果

### 正面後果

- ✅ **Protection**: Prevents DDoS 和 API abuse
- ✅ **Fairness**: Ensures fair resource allocation
- ✅ **Stability**: 維持s system stability under load
- ✅ **Flexibility**: 支援s different user tiers
- ✅ **User Experience**: Allows burst traffic
- ✅ **Cost Control**: Prevents resource exhaustion
- ✅ **Security**: Additional security layer

### 負面後果

- ⚠️ **複雜的ity**: Additional middleware layer
- ⚠️ **Latency**: 2-5ms overhead per request
- ⚠️ **False Positives**: Legitimate users may be limited
- ⚠️ **Monitoring**: Need to track rate limit metrics
- ⚠️ **Documentation**: Users need to understand limits

### 技術債務

**已識別債務**：

1. Static rate limits (可以 be dynamic based on load)
2. 簡單的 token cost (可以 vary 透過 endpoint 複雜的ity)
3. No automatic limit adjustment (future enhancement)

**債務償還計畫**：

- **Q2 2026**: Implement dynamic rate limits based on system load
- **Q3 2026**: Add endpoint-specific token costs
- **Q4 2026**: Implement ML-based anomaly detection

## 相關決策

- [ADR-004: Use Redis 用於 Distributed Caching](004-use-redis-for-distributed-caching.md) - Redis stores rate limit state
- [ADR-009: RESTful API Design 與 OpenAPI 3.0](009-restful-api-design-with-openapi.md) - Rate limits in API design
- [ADR-022: Distributed Locking 與 Redis](022-distributed-locking-with-redis.md) - Redisson 用於 distributed operations
- [ADR-050: API Security 和 Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Overall API security

## 備註

### Token Bucket Implementation

```java
@Service
public class TokenBucketRateLimiter {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public boolean tryConsume(String key, int tokens, int capacity, int refillRate) {
        RBucket<TokenBucket> bucket = redissonClient.getBucket("rate:limit:" + key);
        
        TokenBucket tokenBucket = bucket.get();
        if (tokenBucket == null) {
            tokenBucket = new TokenBucket(capacity, refillRate);
        }
        
        // Refill tokens based on time elapsed
        long now = System.currentTimeMillis();
        long timePassed = now - tokenBucket.getLastRefillTime();
        int tokensToAdd = (int) (timePassed * refillRate / 60000); // per minute
        
        tokenBucket.refill(tokensToAdd, capacity);
        tokenBucket.setLastRefillTime(now);
        
        // Try to consume tokens
        boolean allowed = tokenBucket.tryConsume(tokens);
        
        // Save updated bucket
        bucket.set(tokenBucket, 1, TimeUnit.HOURS);
        
        return allowed;
    }
}

@Data
public class TokenBucket {
    private int tokens;
    private int capacity;
    private int refillRate;
    private long lastRefillTime;
    
    public TokenBucket(int capacity, int refillRate) {
        this.tokens = capacity;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.lastRefillTime = System.currentTimeMillis();
    }
    
    public void refill(int tokensToAdd, int capacity) {
        this.tokens = Math.min(this.tokens + tokensToAdd, capacity);
    }
    
    public boolean tryConsume(int tokens) {
        if (this.tokens >= tokens) {
            this.tokens -= tokens;
            return true;
        }
        return false;
    }
}
```

### Rate Limit Middleware

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Autowired
    private TokenBucketRateLimiter rateLimiter;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String userId = extractUserId(request);
        String endpoint = request.getRequestURI();
        
        // Get rate limit configuration
        RateLimitConfig config = getRateLimitConfig(userId, endpoint);
        
        // Check rate limit
        String key = String.format("%s:%s", userId, endpoint);
        boolean allowed = rateLimiter.tryConsume(
            key, 
            1, // tokens to consume
            config.getCapacity(), 
            config.getRefillRate()
        );
        
        if (allowed) {
            // Add rate limit headers
            response.addHeader("X-RateLimit-Limit", String.valueOf(config.getRefillRate()));
            response.addHeader("X-RateLimit-Remaining", String.valueOf(getRemainingTokens(key)));
            response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime(key)));
            
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            response.setStatus(429); // Too Many Requests
            response.addHeader("Retry-After", String.valueOf(getRetryAfter(key)));
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
        }
    }
}
```

### Rate Limit Configuration

```yaml
# application.yml
rate-limit:
  global:
    capacity: 15000  # 1.5x of 10000 req/min
    refill-rate: 10000  # 10000 req/min
  
  tiers:
    free:
      capacity: 150  # 1.5x of 100 req/min
      refill-rate: 100
    premium:
      capacity: 750  # 1.5x of 500 req/min
      refill-rate: 500
    enterprise:
      capacity: 3000  # 1.5x of 2000 req/min
      refill-rate: 2000
  
  endpoints:
    /api/v1/orders:
      capacity: 150
      refill-rate: 100
    /api/v1/payments:
      capacity: 15  # 1.5x of 10 req/min (sensitive)
      refill-rate: 10
    /api/v1/admin:
      capacity: 15
      refill-rate: 10
```

### Rate Limit Response Headers

```text
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 75
X-RateLimit-Reset: 1640000000

HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640000060
Retry-After: 60
```

### Rate Limit Key Strategy

```text
Pattern: {type}:{identifier}:{endpoint}

Examples:

- user:CUST-123:/api/v1/orders
- ip:192.168.1.1:/api/v1/products
- apikey:key-abc123:/api/v1/data
- global:system:/api/v1/*

```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-25  
**下次審查**： 2026-01-25 （每季）
