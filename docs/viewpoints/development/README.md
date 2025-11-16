# Development Viewpoint

> **狀態**：📝 待撰寫
> **最後更新**：2025-01-17
> **負責人**：技術主管

## 概述

Development Viewpoint 描述程式碼組織、模組結構、建置流程和開發環境。

## 目的

此 viewpoint 回答：

- 程式碼如何組織？
- 模組相依性是什麼？
- 系統如何建置和測試？
- 開發人員需要哪些工具？

## 利害關係人

- **主要**：開發人員、建置工程師
- **次要**：DevOps、架構師

## 內容

### 📄 文件

- [Overview](overview.md) - 程式碼組織方法
- [Module Organization](module-organization.md) - 套件結構和 bounded contexts
- [Dependency Rules](dependency-rules.md) - Hexagonal architecture 約束
- [Build Process](build-process.md) - Gradle 建置和測試執行

### 📊 圖表

- 套件結構圖
- 相依性圖
- 建置管線圖

## 關鍵概念

### 程式碼組織

```text
app/src/main/java/solid/humank/genaidemo/
├── domain/              # Domain 層（無外部相依性）
│   ├── customer/       # Customer bounded context
│   ├── order/          # Order bounded context
│   └── ...
├── application/        # Application services（使用案例）
├── infrastructure/     # Infrastructure adapters
└── interfaces/         # API controllers、事件處理器
```

### 相依性規則

- Domain 層：不相依其他層
- Application 層：僅相依 domain
- Infrastructure 層：相依 domain（透過介面）
- Interface 層：相依 application

### 建置工具

- **建置系統**：Gradle 8.x
- **Java 版本**：Java 21
- **測試**：JUnit 5、Mockito、Cucumber
- **程式碼品質**：ArchUnit、JaCoCo

## 相關文件

### 相關 Viewpoints

- [Functional Viewpoint](../functional/README.md) - Bounded contexts
- [Deployment Viewpoint](../deployment/README.md) - 建置產物

### 相關 Perspectives

- [Evolution Perspective](../../perspectives/evolution/README.md) - 程式碼可維護性

### 相關指南

- [Development Guide](../../development/README.md) - 詳細開發指示
- [Coding Standards](../../development/coding-standards/README.md)

## 快速連結

- [返回所有 Viewpoints](../README.md)
- [主文件](../../README.md)
