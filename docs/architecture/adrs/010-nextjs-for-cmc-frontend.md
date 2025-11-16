---
adr_number: 010
title: "Next.js 用於 CMC Management Frontend"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [009, 011]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "accessibility"]
---

# ADR-010: Next.js 用於 CMC Management Frontend

## 狀態

**Accepted** - 2025-10-24

## 上下文

### 問題陳述

The Enterprise E-Commerce Platform 需要Content Management Console (CMC) 用於 internal users (administrators, content managers, operations team) that:

- 提供s intuitive UI 用於 managing products, orders, customers
- 支援s server-side rendering 用於 performance
- 啟用s rapid development 與 modern tooling
- Integrates seamlessly 與 REST API (ADR-009)
- 支援s authentication 和 authorization
- 提供s responsive design 用於 desktop 和 tablet
- 啟用s real-time updates 用於 operational dashboards
- 支援s internationalization

### 業務上下文

**業務驅動因素**：

- 需要 efficient internal operations management
- Requirement 用於 fast 頁面載入 和 SEO (for help documentation)
- Team growth requiring scalable frontend architecture
- 需要 rapid feature development
- 支援 用於 multiple languages (English, Chinese)

**限制條件**：

- Team has React experience
- 必須 integrate 與 existing REST API
- 預算: No additional frontend infrastructure costs
- Timeline: 3 個月 to MVP
- 必須 支援 modern browsers (Chrome, Firefox, Safari, Edge)

### 技術上下文

**目前狀態**：

- RESTful API 與 OpenAPI 3.0 (ADR-009)
- Spring Boot backend
- AWS infrastructure (ADR-007)
- TypeScript preference 用於 type safety

**需求**：

- Server-side rendering (SSR) 用於 performance
- Static site generation (SSG) 用於 documentation
- API routes 用於 BFF pattern
- TypeScript 用於 type safety
- Component library 用於 consistency
- State management
- Form handling 和 validation
- Real-time updates

## 決策驅動因素

1. **Performance**: Fast 頁面載入 與 SSR/SSG
2. **Developer Experience**: Modern tooling 和 hot reload
3. **Team Skills**: Leverage React knowledge
4. **Type Safety**: TypeScript 支援
5. **SEO**: Server-side rendering 用於 help docs
6. **Flexibility**: 支援 SSR, SSG, 和 CSR
7. **Ecosystem**: 豐富的 component libraries
8. **成本**： Free and open source

## 考慮的選項

### 選項 1： Next.js 14 with React 18

**描述**： React framework with SSR, SSG, and API routes

**優點**：

- ✅ 優秀的 performance (SSR, SSG, ISR)
- ✅ Built-in TypeScript 支援
- ✅ API routes 用於 BFF pattern
- ✅ File-based routing
- ✅ Image optimization
- ✅ Team has React experience
- ✅ 大型的 ecosystem (shadcn/ui, Radix UI)
- ✅ 優秀的 developer experience
- ✅ Vercel deployment (optional)
- ✅ App Router 與 React Server Components

**缺點**：

- ⚠️ Learning curve 用於 App Router
- ⚠️ Server infrastructure needed 用於 SSR

**成本**： $0 (open source)

**風險**： **Low** - Mature, widely adopted

### 選項 2： Create React App (CRA)

**描述**： Client-side React application

**優點**：

- ✅ 簡單的 setup
- ✅ Team knows React
- ✅ No server needed

**缺點**：

- ❌ No SSR (poor initial load)
- ❌ No built-in routing
- ❌ No API routes
- ❌ Limited optimization
- ❌ CRA is deprecated

**成本**： $0

**風險**： **High** - CRA is no longer maintained

### 選項 3： Vue.js with Nuxt

**描述**： Vue framework with SSR

**優點**：

- ✅ 良好的 performance
- ✅ SSR 支援
- ✅ 良好的 developer experience

**缺點**：

- ❌ Team lacks Vue experience
- ❌ Smaller ecosystem than React
- ❌ Learning curve

**成本**： $0

**風險**： **Medium** - Team learning curve

### 選項 4： Angular (Same as Consumer App)

**描述**： Use Angular for both CMC and consumer app

**優點**：

- ✅ Single framework 用於 both apps
- ✅ Strong TypeScript 支援
- ✅ Comprehensive framework

**缺點**：

- ❌ Heavier than needed 用於 CMC
- ❌ Steeper learning curve
- ❌ Less flexible than Next.js
- ❌ Overkill 用於 internal tool

**成本**： $0

**風險**： **Low** - But not optimal for CMC

## 決策結果

**選擇的選項**： **Next.js 14 with React 18**

### 理由

Next.js was selected 用於 the CMC frontend 用於 the following reasons:

1. **Performance**: SSR 和 SSG 提供 fast 頁面載入
2. **Team Skills**: Team already knows React
3. **TypeScript**: Built-in TypeScript 支援
4. **Flexibility**: 支援s SSR, SSG, 和 CSR as needed
5. **API Routes**: BFF pattern 用於 backend integration
6. **Developer Experience**: 優秀的 tooling 和 hot reload
7. **Ecosystem**: 豐富的 component libraries (shadcn/ui, Radix UI)
8. **Modern Features**: App Router 與 React Server Components

**實作策略**：

**架構**：

```text
Next.js App (CMC)
├── App Router
├── React Server Components
├── API Routes (BFF)
├── shadcn/ui Components
└── TypeScript
```

**Key Features**:

- Server-side rendering 用於 dashboard pages
- Static generation 用於 help documentation
- API routes 用於 authentication 和 data aggregation
- shadcn/ui 用於 consistent UI components
- React Query 用於 data fetching 和 caching

**為何不選 CRA**： CRA is deprecated 和 lacks SSR capabilities needed 用於 performance.

**為何不選 Vue/Nuxt**： Team lacks Vue experience. React knowledge 可以 be leveraged immediately.

**為何不選 Angular 用於 CMC**： Angular is 更好的 suited 用於 the consumer app's 複雜的ity. Next.js is more appropriate 用於 the internal CMC tool.

## 影響分析

### 利害關係人影響

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Frontend Developers | High | Need to learn Next.js App Router | Training, documentation, examples |
| Backend Developers | Low | API integration unchanged | API documentation |
| Operations Team | High | Primary users of CMC | User training, intuitive UI |
| DevOps Team | Medium | Need to deploy Next.js app | Deployment guides, Docker setup |

### 影響半徑

**選擇的影響半徑**： **Bounded Context**

影響：

- CMC frontend application
- Deployment infrastructure
- Development workflow
- Testing strategy

### 風險評估

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| App Router learning curve | High | Medium | Training, examples, pair programming |
| SSR infrastructure 複雜的ity | Medium | Medium | Use Vercel 或 containerize 與 Docker |
| Performance issues | Low | High | Implement caching, optimize images |
| State management 複雜的ity | Medium | Medium | Use React Query, minimize global state |

**整體風險等級**： **Low**

## 實作計畫

### 第 1 階段： Project Setup （第 1 週）

- [ ] Create Next.js project

  ```bash
  npx create-next-app@latest cmc-frontend --typescript --tailwind --app
  ```

- [ ] Configure TypeScript

  ```json
  {
    "compilerOptions": {
      "target": "ES2020",
      "lib": ["ES2020", "DOM"],
      "jsx": "preserve",
      "module": "ESNext",
      "moduleResolution": "bundler",
      "strict": true,
      "paths": {
        "@/*": ["./src/*"]
      }
    }
  }
  ```

- [ ] Set up project structure

  ```
  cmc-frontend/
  ├── src/
  │   ├── app/
  │   │   ├── (auth)/
  │   │   │   ├── login/
  │   │   │   └── layout.tsx
  │   │   ├── (dashboard)/
  │   │   │   ├── customers/
  │   │   │   ├── orders/
  │   │   │   ├── products/
  │   │   │   └── layout.tsx
  │   │   ├── api/
  │   │   │   └── auth/
  │   │   └── layout.tsx
  │   ├── components/
  │   │   ├── ui/
  │   │   └── features/
  │   ├── lib/
  │   │   ├── api/
  │   │   └── utils/
  │   └── types/
  └── public/
  ```

### 第 2 階段： UI Component Library （第 1-2 週）

- [ ] Install shadcn/ui

  ```bash
  npx shadcn-ui@latest init
  ```

- [ ] Add core components

  ```bash
  npx shadcn-ui@latest add button
  npx shadcn-ui@latest add form
  npx shadcn-ui@latest add table
  npx shadcn-ui@latest add dialog
  npx shadcn-ui@latest add dropdown-menu
  ```

- [ ] Create custom components

  ```typescript
  // src/components/ui/data-table.tsx
  export function DataTable<TData, TValue>({
    columns,
    data,
  }: DataTableProps<TData, TValue>) {
    const table = useReactTable({
      data,
      columns,
      getCoreRowModel: getCoreRowModel(),
      getPaginationRowModel: getPaginationRowModel(),
      getSortedRowModel: getSortedRowModel(),
      getFilteredRowModel: getFilteredRowModel(),
    });
    
    return (
      <div className="rounded-md border">
        <Table>
          {/* Table implementation */}
        </Table>
      </div>
    );
  }
  ```

### 第 3 階段： Authentication （第 2-3 週）

- [ ] Implement NextAuth.js

  ```typescript
  // src/app/api/auth/[...nextauth]/route.ts
  import NextAuth from "next-auth";
  import CredentialsProvider from "next-auth/providers/credentials";
  
  export const authOptions = {
    providers: [
      CredentialsProvider({
        name: "Credentials",
        credentials: {
          email: { label: "Email", type: "email" },
          password: { label: "Password", type: "password" }
        },
        async authorize(credentials) {
          const res = await fetch(`${process.env.API_URL}/api/v1/auth/login`, {
            method: "POST",
            body: JSON.stringify(credentials),
            headers: { "Content-Type": "application/json" }
          });
          
          const user = await res.json();
          
          if (res.ok && user) {
            return user;
          }
          return null;
        }
      })
    ],
    pages: {
      signIn: "/login",
    },
    callbacks: {
      async jwt({ token, user }) {
        if (user) {
          token.accessToken = user.accessToken;
        }
        return token;
      },
      async session({ session, token }) {
        session.accessToken = token.accessToken;
        return session;
      }
    }
  };
  
  const handler = NextAuth(authOptions);
  export { handler as GET, handler as POST };
  ```

- [ ] Create protected routes

  ```typescript
  // src/app/(dashboard)/layout.tsx
  import { getServerSession } from "next-auth";
  import { redirect } from "next/navigation";
  
  export default async function DashboardLayout({
    children,
  }: {
    children: React.ReactNode;
  }) {
    const session = await getServerSession(authOptions);
    
    if (!session) {
      redirect("/login");
    }
    
    return (
      <div className="flex min-h-screen">
        <Sidebar />
        <main className="flex-1">{children}</main>
      </div>
    );
  }
  ```

### 第 4 階段： API Integration （第 3-4 週）

- [ ] Set up React Query

  ```typescript
  // src/app/providers.tsx
  'use client';
  
  import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
  import { useState } from 'react';
  
  export function Providers({ children }: { children: React.ReactNode }) {
    const [queryClient] = useState(() => new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 60 * 1000,
          refetchOnWindowFocus: false,
        },
      },
    }));
    
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    );
  }
  ```

- [ ] Create API client

  ```typescript
  // src/lib/api/client.ts
  import { getSession } from 'next-auth/react';
  
  class ApiClient {
    private baseUrl: string;
    
    constructor() {
      this.baseUrl = process.env.NEXT_PUBLIC_API_URL || '';
    }
    
    async request<T>(
      endpoint: string,
      options?: RequestInit
    ): Promise<T> {
      const session = await getSession();
      
      const response = await fetch(`${this.baseUrl}${endpoint}`, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${session?.accessToken}`,
          ...options?.headers,
        },
      });
      
      if (!response.ok) {
        throw new Error(`API Error: ${response.statusText}`);
      }
      
      return response.json();
    }
    
    get<T>(endpoint: string) {
      return this.request<T>(endpoint);
    }
    
    post<T>(endpoint: string, data: unknown) {
      return this.request<T>(endpoint, {
        method: 'POST',
        body: JSON.stringify(data),
      });
    }
    
    put<T>(endpoint: string, data: unknown) {
      return this.request<T>(endpoint, {
        method: 'PUT',
        body: JSON.stringify(data),
      });
    }
    
    delete<T>(endpoint: string) {
      return this.request<T>(endpoint, {
        method: 'DELETE',
      });
    }
  }
  
  export const apiClient = new ApiClient();
  ```

- [ ] Create API hooks

  ```typescript
  // src/lib/api/hooks/useCustomers.ts
  import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
  import { apiClient } from '../client';
  
  export function useCustomers(page = 0, size = 20) {
    return useQuery({
      queryKey: ['customers', page, size],
      queryFn: () => apiClient.get(`/api/v1/customers?page=${page}&size=${size}`),
    });
  }
  
  export function useCustomer(id: string) {
    return useQuery({
      queryKey: ['customer', id],
      queryFn: () => apiClient.get(`/api/v1/customers/${id}`),
      enabled: !!id,
    });
  }
  
  export function useCreateCustomer() {
    const queryClient = useQueryClient();
    
    return useMutation({
      mutationFn: (data: CreateCustomerRequest) =>
        apiClient.post('/api/v1/customers', data),
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: ['customers'] });
      },
    });
  }
  ```

### Phase 5: Feature Implementation （第 4-8 週）

- [ ] Implement Customer Management

  ```typescript
  // src/app/(dashboard)/customers/page.tsx
  import { DataTable } from '@/components/ui/data-table';
  import { columns } from './columns';
  import { useCustomers } from '@/lib/api/hooks/useCustomers';
  
  export default function CustomersPage() {
    const { data, isLoading } = useCustomers();
    
    if (isLoading) return <div>Loading...</div>;
    
    return (
      <div className="container mx-auto py-10">
        <h1 className="text-3xl font-bold mb-6">Customers</h1>
        <DataTable columns={columns} data={data?.content || []} />
      </div>
    );
  }
  ```

- [ ] Implement Order Management
- [ ] Implement Product Management
- [ ] Implement Dashboard 與 metrics

### Phase 6: Testing and Optimization （第 8-9 週）

- [ ] Set up testing

  ```bash
  npm install -D @testing-library/react @testing-library/jest-dom jest
  ```

- [ ] Write component tests
- [ ] Implement E2E tests 與 Playwright
- [ ] Optimize images 和 performance
- [ ] Add error boundaries

### 回滾策略

**觸發條件**：

- Performance issues 與 SSR
- Team unable to adopt Next.js
- Deployment 複雜的ity too high
- Development velocity decreases > 30%

**回滾步驟**：

1. Migrate to Vite + React
2. Use client-side rendering only
3. Simplify deployment
4. Re-evaluate after addressing issues

**回滾時間**： 2 weeks

## 監控和成功標準

### 成功指標

- ✅ Page load time < 2 seconds
- ✅ Lighthouse score > 90
- ✅ Zero runtime errors in production
- ✅ Developer satisfaction > 4/5
- ✅ Feature delivery velocity 維持ed
- ✅ Bundle size < 500KB

### 監控計畫

**Performance Metrics**:

- Core Web Vitals (LCP, FID, CLS)
- Page load times
- API 回應時間
- Bundle size

**審查時程**：

- Weekly: Performance review
- Monthly: Dependency updates
- Quarterly: Architecture review

## 後果

### 正面後果

- ✅ **優秀的 Performance**: SSR 提供s fast initial loads
- ✅ **Developer Experience**: Modern tooling 和 hot reload
- ✅ **Type Safety**: TypeScript throughout
- ✅ **Flexibility**: SSR, SSG, 和 CSR as needed
- ✅ **豐富的 Ecosystem**: shadcn/ui, Radix UI, React Query
- ✅ **SEO**: Server-side rendering 用於 help docs
- ✅ **API Routes**: BFF pattern 用於 backend integration

### 負面後果

- ⚠️ **Learning Curve**: App Router is new paradigm
- ⚠️ **Server Infrastructure**: Need to run Node.js server
- ⚠️ **複雜的ity**: More 複雜的 than pure client-side

### 技術債務

**已識別債務**：

1. No E2E tests initially (acceptable 用於 MVP)
2. Limited accessibility testing (future enhancement)
3. No internationalization yet (future requirement)

**債務償還計畫**：

- **Q1 2026**: Implement comprehensive E2E tests
- **Q2 2026**: Add accessibility testing 和 改善ments
- **Q3 2026**: Implement internationalization
- **Q4 2026**: Optimize bundle size 和 performance

## 相關決策

- [ADR-009: RESTful API Design 與 OpenAPI](009-restful-api-design-with-openapi.md) - API integration
- [ADR-011: Angular 用於 Consumer Frontend](011-angular-for-consumer-frontend.md) - Consumer app frontend

## 備註

### Next.js App Router Structure

```text
app/
├── (auth)/
│   ├── login/
│   │   └── page.tsx
│   └── layout.tsx
├── (dashboard)/
│   ├── customers/
│   │   ├── [id]/
│   │   │   └── page.tsx
│   │   ├── page.tsx
│   │   └── columns.tsx
│   ├── orders/
│   ├── products/
│   └── layout.tsx
├── api/
│   └── auth/
│       └── [...nextauth]/
│           └── route.ts
├── layout.tsx
└── page.tsx
```

### Key Dependencies

```json
{
  "dependencies": {
    "next": "14.0.0",
    "react": "18.2.0",
    "react-dom": "18.2.0",
    "typescript": "5.3.0",
    "@tanstack/react-query": "^5.0.0",
    "next-auth": "^4.24.0",
    "@radix-ui/react-*": "latest",
    "tailwindcss": "^3.4.0",
    "zod": "^3.22.0"
  }
}
```

---

**文檔狀態**： ✅ Accepted  
**上次審查**： 2025-10-24  
**下次審查**： 2026-01-24 （每季）
