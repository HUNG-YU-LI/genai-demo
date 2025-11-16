# Availability & Resilience Perspective

> **Status**: 📝 待文檔化
> **Last Updated**: 2025-01-17
> **Owner**: SRE Lead

## Overview

Availability & Resilience Perspective 確保系統保持運作並從故障中快速恢復。

## 關鍵關注點

- High availability (99.9% 正常運行時間)
- 容錯模式
- 災難恢復
- 優雅降級

## Quality Attribute Scenarios

### Scenario 1: Database Failover

- **Source**: 資料庫伺服器
- **Stimulus**: 主資料庫故障
- **Environment**: 營業時間的生產環境
- **Artifact**: 客戶資料服務
- **Response**: 系統故障轉移至次要資料庫
- **Response Measure**: RTO ≤ 5 分鐘，RPO ≤ 1 分鐘

## Availability 目標

- **Uptime**: 99.9% (每年 8.76 小時停機時間)
- **RTO**: 5 分鐘
- **RPO**: 1 分鐘

## 影響的視角

- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Multi-AZ deployment
- [Operational Viewpoint](../../viewpoints/operational/README.md) - 監控和恢復
- [Concurrency Viewpoint](../../viewpoints/concurrency/README.md) - 重試機制

## 快速連結

- [回到所有 Perspectives](../README.md)
- [主文檔](../../README.md)
