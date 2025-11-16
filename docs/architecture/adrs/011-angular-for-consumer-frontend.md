---
adr_number: 011
title: "Angular 用於 Consumer Frontend"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [009, 010]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "accessibility", "performance"]
---

# ADR-011: Angular 用於 Consumer Frontend

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要consumer-facing web application that:

- 提供s 優秀的 用戶體驗 用於 customers
- 處理s 複雜的 state management (shopping cart, checkout flow)
- 支援s progressive web app (PWA) capabilities
- 啟用s offline functionality
- 提供s strong type safety 和 structure
- 支援s 大型的-scale application development
- 啟用s code reusability 和 維持ability
- 支援s internationalization 用於 global users

### 業務上下文

**業務驅動因素**：

- 需要 scalable consumer web application
- Requirement 用於 複雜的 user workflows (product browsing, cart, checkout)
- 預期的 high traffic (10K+ concurrent users)
- 支援 用於 multiple languages 和 regions
- PWA capabilities 用於 mobile-like experience
- Long-term 維持ability (5+ 年)

**限制條件**：

- 必須 integrate 與 REST API (ADR-009)
- 預算: No additional frontend infrastructure costs
- Timeline: 6 個月 to production
- 必須 支援 modern browsers 和 mobile devices
- Team 將 grow from 3 to 10+ frontend developers

### 技術上下文

**目前狀態**：

- RESTful API 與 OpenAPI 3.0 (ADR-009)
- Next.js 用於 CMC (ADR-010)
- Spring Boot backend
- AWS infrastructure

**需求**：

- 複雜的 state management
- Form handling 和 validation
- Real-time updates (cart, inventory)
- Responsive design
- PWA 支援
- SEO optimization
- Performance optimization
- Accessibility (WCAG 2.1)

## 決策驅動因素

1. **Enterprise Scale**: 支援 大型的, 複雜的 application
2. **Type Safety**: Strong TypeScript integration
3. **結構**： Opinionated framework for consistency
4. **Team Scalability**: 支援 growing team
5. **Long-term 維持ability**: 5+ 年 lifespan
6. **PWA 支援**: Native PWA capabilities
7. **Tooling**: Comprehensive CLI 和 tooling
8. **Performance**: Optimized 用於 production

## 考慮的選項

### 選項 1： Angular 18

**描述**： Comprehensive TypeScript framework with full-featured tooling

**優點**：

- ✅ Comprehensive framework (routing, forms, HTTP, etc.)
- ✅ Strong TypeScript 支援 (built 與 TypeScript)
- ✅ Opinionated structure (consistency 跨 team)
- ✅ 優秀的 CLI tooling
- ✅ Built-in dependency injection
- ✅ RxJS 用於 reactive programming
- ✅ Native PWA 支援
- ✅ Angular Material 用於 UI components
- ✅ Signals 用於 reactive state management
- ✅ Standalone components (modern approach)

**缺點**：

- ⚠️ Steeper learning curve
- ⚠️ 大型的r bundle size than React
- ⚠️ More verbose than React

**成本**： $0 (open source)

**風險**： **Low** - Mature, enterprise-proven

### 選項 2： React with Next.js (Same as CMC)

**描述**： Use Next.js for both CMC and consumer app

**優點**：

- ✅ Single framework 用於 both apps
- ✅ Team already learning Next.js
- ✅ Flexible 和 lightweight
- ✅ 大型的 ecosystem

**缺點**：

- ❌ Less structure (need to choose state management, forms, etc.)
- ❌ More decisions to make
- ❌ Less opinionated
- ❌ 更難維持 consistency 跨 大型的 team

**成本**： $0

**風險**： **Medium** - May lack structure for complex app

### 選項 3： Vue.js 3

**描述**： Progressive JavaScript framework

**優點**：

- ✅ 容易learn
- ✅ 良好的 performance
- ✅ Composition API

**缺點**：

- ❌ Team lacks Vue experience
- ❌ Smaller ecosystem than React/Angular
- ❌ Less enterprise adoption

**成本**： $0

**風險**： **Medium** - Team learning curve

### 選項 4： Svelte/SvelteKit

**描述**： Compiler-based framework

**優點**：

- ✅ 優秀的 performance
- ✅ Small bundle size
- ✅ 簡單的 syntax

**缺點**：

- ❌ Smaller ecosystem
- ❌ Less enterprise adoption
- ❌ Team lacks experience
- ❌ Fewer component libraries

**成本**： $0

**風險**： **High** - Less proven for enterprise

## 決策結果

**選擇的選項**： **Angular 18**

### 理由

Angular was selected 用於 the consumer frontend 用於 the following reasons:

1. **Enterprise Scale**: Designed 用於 大型的, 複雜的 applications
2. **Strong Structure**: Opinionated framework ensures consistency
3. **TypeScript First**: Built 與 TypeScript, 優秀的 type safety
4. **Comprehensive**: Includes routing, forms, HTTP, state management
5. **Team Scalability**: Clear patterns help 大型的 teams collaborate
6. **Long-term 支援**: Google-backed 與 LTS releases
7. **PWA 支援**: Built-in PWA capabilities
8. **Tooling**: 優秀的 CLI 和 development tools
9. **Modern Features**: Signals, standalone components, 改善d DX

**實作策略**：

**架構**：

```text
Angular App (Consumer)
├── Standalone Components
├── Signals for State
├── Angular Material
├── RxJS for Async
└── TypeScript
```

**Key Features**:

- Standalone components (no NgModules)
- Signals 用於 reactive state management
- Angular Material 用於 UI components
- RxJS 用於 複雜的 async operations
- Service Workers 用於 PWA
- Lazy loading 用於 performance

**為何不選 React/Next.js**： While Next.js works well 用於 CMC, Angular's structure 和 comprehensiveness are 更好的 suited 用於 the 複雜的 consumer application 與 a 大型的 team.

**為何不選 Vue/Svelte**： Team lacks experience 和 these frameworks have smaller ecosystems 用於 enterprise applications.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Frontend Developers | High | Need to learn Angular | Training, documentation, examples |
| Backend Developers | Low | API integration unchanged | API documentation |
| End Users | Positive | 更好的 用戶體驗 | User testing, feedback |
| QA Team | Medium | New testing framework | Testing guides, tools |

### 影響半徑

**選擇的影響半徑**： **Bounded Context**

影響：

- Consumer frontend application
- Deployment infrastructure
- Development workflow
- Testing strategy

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Angular learning curve | High | Medium | Training, examples, pair programming |
| Bundle size concerns | Medium | Medium | Lazy loading, tree shaking, optimization |
| Performance issues | Low | High | Performance monitoring, optimization |
| Team adoption resistance | Medium | Medium | Demonstrate benefits, 提供 支援 |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Project Setup （第 1 週）

- [ ] Create Angular project

  ```bash
  npm install -g @angular/cli
  ng new consumer-app --standalone --routing --style=scss
  ```

- [ ] Configure TypeScript

  ```json
  {
    "compilerOptions": {
      "target": "ES2022",
      "module": "ES2022",
      "lib": ["ES2022", "DOM"],
      "strict": true,
      "esModuleInterop": true,
      "skipLibCheck": true,
      "forceConsistentCasingInFileNames": true
    }
  }
  ```

- [ ] Set up project structure

  ```
  consumer-app/
  ├── src/
  │   ├── app/
  │   │   ├── core/
  │   │   │   ├── services/
  │   │   │   ├── guards/
  │   │   │   └── interceptors/
  │   │   ├── shared/
  │   │   │   ├── components/
  │   │   │   ├── directives/
  │   │   │   └── pipes/
  │   │   ├── features/
  │   │   │   ├── products/
  │   │   │   ├── cart/
  │   │   │   ├── checkout/
  │   │   │   └── account/
  │   │   └── app.component.ts
  │   ├── assets/
  │   └── environments/
  └── angular.json
  ```

### 第 2 階段： Core Services （第 1-2 週）

- [ ] Create API service

  ```typescript
  // src/app/core/services/api.service.ts
  import { Injectable, inject } from '@angular/core';
  import { HttpClient, HttpHeaders } from '@angular/common/http';
  import { Observable } from 'rxjs';
  import { environment } from '../../../environments/environment';
  
  @Injectable({
    providedIn: 'root'
  })
  export class ApiService {
    private http = inject(HttpClient);
    private baseUrl = environment.apiUrl;
    
    get<T>(endpoint: string): Observable<T> {
      return this.http.get<T>(`${this.baseUrl}${endpoint}`);
    }
    
    post<T>(endpoint: string, data: any): Observable<T> {
      return this.http.post<T>(`${this.baseUrl}${endpoint}`, data);
    }
    
    put<T>(endpoint: string, data: any): Observable<T> {
      return this.http.put<T>(`${this.baseUrl}${endpoint}`, data);
    }
    
    delete<T>(endpoint: string): Observable<T> {
      return this.http.delete<T>(`${this.baseUrl}${endpoint}`);
    }
  }
  ```

- [ ] Create authentication service

  ```typescript
  // src/app/core/services/auth.service.ts
  import { Injectable, inject, signal } from '@angular/core';
  import { Router } from '@angular/router';
  import { ApiService } from './api.service';
  import { tap } from 'rxjs/operators';
  
  @Injectable({
    providedIn: 'root'
  })
  export class AuthService {
    private api = inject(ApiService);
    private router = inject(Router);
    
    // Signals for reactive state
    isAuthenticated = signal(false);
    currentUser = signal<User | null>(null);
    
    login(credentials: LoginCredentials) {
      return this.api.post<AuthResponse>('/api/v1/auth/login', credentials)
        .pipe(
          tap(response => {
            localStorage.setItem('token', response.token);
            this.isAuthenticated.set(true);
            this.currentUser.set(response.user);
          })
        );
    }
    
    logout() {
      localStorage.removeItem('token');
      this.isAuthenticated.set(false);
      this.currentUser.set(null);
      this.router.navigate(['/login']);
    }
  }
  ```

- [ ] Create HTTP interceptor

  ```typescript
  // src/app/core/interceptors/auth.interceptor.ts
  import { HttpInterceptorFn } from '@angular/common/http';
  
  export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const token = localStorage.getItem('token');
    
    if (token) {
      const cloned = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });
      return next(cloned);
    }
    
    return next(req);
  };
  ```

### 第 3 階段： State Management （第 2-3 週）

- [ ] Create cart service 與 signals

  ```typescript
  // src/app/core/services/cart.service.ts
  import { Injectable, inject, computed, signal } from '@angular/core';
  import { ApiService } from './api.service';
  
  @Injectable({
    providedIn: 'root'
  })
  export class CartService {
    private api = inject(ApiService);
    
    // Signals for reactive state
    private cartItems = signal<CartItem[]>([]);
    
    // Computed values
    itemCount = computed(() => 
      this.cartItems().reduce((sum, item) => sum + item.quantity, 0)
    );
    
    totalAmount = computed(() =>
      this.cartItems().reduce((sum, item) => 
        sum + (item.price * item.quantity), 0
      )
    );
    
    // Read-only signal
    items = this.cartItems.asReadonly();
    
    addItem(product: Product, quantity: number = 1) {
      const currentItems = this.cartItems();
      const existingItem = currentItems.find(item => item.productId === product.id);
      
      if (existingItem) {
        this.updateQuantity(product.id, existingItem.quantity + quantity);
      } else {
        this.cartItems.set([
          ...currentItems,
          {
            productId: product.id,
            name: product.name,
            price: product.price,
            quantity
          }
        ]);
      }
      
      this.syncWithBackend();
    }
    
    removeItem(productId: string) {
      this.cartItems.update(items => 
        items.filter(item => item.productId !== productId)
      );
      this.syncWithBackend();
    }
    
    updateQuantity(productId: string, quantity: number) {
      this.cartItems.update(items =>
        items.map(item =>
          item.productId === productId
            ? { ...item, quantity }
            : item
        )
      );
      this.syncWithBackend();
    }
    
    private syncWithBackend() {
      this.api.post('/api/v1/cart', this.cartItems()).subscribe();
    }
  }
  ```

### 第 4 階段： UI Components （第 3-5 週）

- [ ] Install Angular Material

  ```bash
  ng add @angular/material
  ```

- [ ] Create product list component

  ```typescript
  // src/app/features/products/product-list.component.ts
  import { Component, inject, OnInit, signal } from '@angular/core';
  import { CommonModule } from '@angular/common';
  import { MatCardModule } from '@angular/material/card';
  import { MatButtonModule } from '@angular/material/button';
  import { ProductService } from '../../core/services/product.service';
  import { CartService } from '../../core/services/cart.service';
  
  @Component({
    selector: 'app-product-list',
    standalone: true,
    imports: [CommonModule, MatCardModule, MatButtonModule],
    template: `
      <div class="product-grid">
        @for (product of products(); track product.id) {
          <mat-card>
            <img mat-card-image [src]="product.imageUrl" [alt]="product.name">
            <mat-card-header>
              <mat-card-title>{{ product.name }}</mat-card-title>
              <mat-card-subtitle>\${{ product.price }}</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <p>{{ product.description }}</p>
            </mat-card-content>
            <mat-card-actions>
              <button mat-raised-button color="primary" 
                      (click)="addToCart(product)">
                Add to Cart
              </button>
            </mat-card-actions>
          </mat-card>
        }
      </div>
    `,
    styles: [`
      .product-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
        gap: 1rem;
        padding: 1rem;
      }
    `]
  })
  export class ProductListComponent implements OnInit {
    private productService = inject(ProductService);
    private cartService = inject(CartService);
    
    products = signal<Product[]>([]);
    
    ngOnInit() {
      this.productService.getProducts().subscribe(
        products => this.products.set(products)
      );
    }
    
    addToCart(product: Product) {
      this.cartService.addItem(product);
    }
  }
  ```

- [ ] Create shopping cart component
- [ ] Create checkout flow components
- [ ] Create user account components

### Phase 5: PWA Setup （第 5-6 週）

- [ ] Add PWA 支援

  ```bash
  ng add @angular/pwa
  ```

- [ ] Configure service worker

  ```json
  {
    "index": "/index.html",
    "assetGroups": [
      {
        "name": "app",
        "installMode": "prefetch",
        "resources": {
          "files": [
            "/favicon.ico",
            "/index.html",
            "/manifest.webmanifest",
            "/*.css",
            "/*.js"
          ]
        }
      },
      {
        "name": "assets",
        "installMode": "lazy",
        "updateMode": "prefetch",
        "resources": {
          "files": [
            "/assets/**",
            "/*.(svg|cur|jpg|jpeg|png|apng|webp|avif|gif|otf|ttf|woff|woff2)"
          ]
        }
      }
    ],
    "dataGroups": [
      {
        "name": "api",
        "urls": ["/api/**"],
        "cacheConfig": {
          "maxSize": 100,
          "maxAge": "1h",
          "strategy": "freshness"
        }
      }
    ]
  }
  ```

### Phase 6: Testing （第 6-8 週）

- [ ] Set up testing

  ```typescript
  // src/app/features/products/product-list.component.spec.ts
  import { ComponentFixture, TestBed } from '@angular/core/testing';
  import { ProductListComponent } from './product-list.component';
  import { ProductService } from '../../core/services/product.service';
  import { of } from 'rxjs';
  
  describe('ProductListComponent', () => {
    let component: ProductListComponent;
    let fixture: ComponentFixture<ProductListComponent>;
    let productService: jasmine.SpyObj<ProductService>;
    
    beforeEach(async () => {
      const productServiceSpy = jasmine.createSpyObj('ProductService', ['getProducts']);
      
      await TestBed.configureTestingModule({
        imports: [ProductListComponent],
        providers: [
          { provide: ProductService, useValue: productServiceSpy }
        ]
      }).compileComponents();
      
      productService = TestBed.inject(ProductService) as jasmine.SpyObj<ProductService>;
      fixture = TestBed.createComponent(ProductListComponent);
      component = fixture.componentInstance;
    });
    
    it('should load products on init', () => {
      const mockProducts = [{ id: '1', name: 'Product 1', price: 10 }];
      productService.getProducts.and.returnValue(of(mockProducts));
      
      component.ngOnInit();
      
      expect(component.products()).toEqual(mockProducts);
    });
  });
  ```

- [ ] Write unit tests 用於 services
- [ ] Write component tests
- [ ] Set up E2E tests 與 Playwright

### 回滾策略

**觸發條件**：

- Team unable to adopt Angular
- Performance issues
- Development velocity decreases > 30%
- Bundle size too 大型的

**回滾步驟**：

1. Migrate to Next.js (same as CMC)
2. Reuse components where possible
3. Simplify state management
4. Re-evaluate after addressing issues

**回滾時間**： 4 weeks

## 監控和成功標準

### 成功指標

- ✅ Page load time < 3 seconds
- ✅ Lighthouse score > 85
- ✅ PWA score > 90
- ✅ Zero runtime errors in production
- ✅ Developer satisfaction > 4/5
- ✅ Bundle size < 1MB (initial load)

### 監控計畫

**Performance Metrics**:

- Core Web Vitals
- Bundle size
- API 回應時間
- Error rates

**審查時程**：

- Weekly: Performance review
- Monthly: Dependency updates
- Quarterly: Architecture review

## 後果

### 正面後果

- ✅ **Enterprise Scale**: 處理s 複雜的 application
- ✅ **Strong Structure**: Consistent patterns 跨 team
- ✅ **Type Safety**: 優秀的 TypeScript 支援
- ✅ **Comprehensive**: All tools included
- ✅ **PWA 支援**: Native PWA capabilities
- ✅ **Long-term 支援**: Google-backed LTS
- ✅ **Modern Features**: Signals, standalone components

### 負面後果

- ⚠️ **Learning Curve**: Steeper than React
- ⚠️ **Bundle Size**: 大型的r than React
- ⚠️ **Verbosity**: More code than React

### 技術債務

**已識別債務**：

1. No E2E tests initially (acceptable 用於 MVP)
2. Limited accessibility testing (future enhancement)
3. No internationalization yet (future requirement)

**債務償還計畫**：

- **Q1 2026**: Implement comprehensive E2E tests
- **Q2 2026**: Add accessibility testing
- **Q3 2026**: Implement internationalization
- **Q4 2026**: Optimize bundle size

## 相關決策

- [ADR-009: RESTful API Design 與 OpenAPI](009-restful-api-design-with-openapi.md) - API integration
- [ADR-010: Next.js 用於 CMC Frontend](010-nextjs-for-cmc-frontend.md) - CMC frontend

## 備註

### Key Dependencies

```json
{
  "dependencies": {
    "@angular/core": "^18.0.0",
    "@angular/common": "^18.0.0",
    "@angular/router": "^18.0.0",
    "@angular/forms": "^18.0.0",
    "@angular/material": "^18.0.0",
    "@angular/pwa": "^18.0.0",
    "rxjs": "^7.8.0",
    "typescript": "~5.4.0"
  }
}
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
