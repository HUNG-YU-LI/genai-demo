#!/usr/bin/env python3
"""
高效批量翻译 Markdown 文件为繁体中文
规则：技术名词保留英文，只翻译通用内容
"""

import os
import re
from pathlib import Path
import sys

# 完整的英译中映射表
TRANSLATIONS = {
    # 文档标题和章节
    'Guide': '指南',
    'Overview': '概述',
    'Introduction': '簡介',
    'Summary': '摘要',
    'Description': '說明',
    'Purpose': '目的',
    'Objective': '目標',
    'Goals': '目標',

    # 特性和功能
    'Features': '功能特性',
    'Characteristics': '特性',
    'Advantages': '優點',
    'Benefits': '優勢',
    'Strengths': '優點',
    'Weaknesses': '缺點',
    'Limitations': '限制',

    # 文档结构
    'Prerequisites': '先決條件',
    'Requirements': '需求',
    'Installation': '安裝',
    'Usage': '使用方式',
    'Examples': '範例',
    'Tutorial': '教學',
    'References': '參考資料',
    'Documentation': '文件',
    'Notes': '注意事項',
    'Tips': '提示',
    'Best Practices': '最佳實踐',
    'Common Issues': '常見問題',
    'Troubleshooting': 'Troubleshooting',
    'FAQ': '常見問答',

    # 流程和步骤
    'Workflow': 'Workflow',
    'Process': '流程',
    'Procedure': '程序',
    'Steps': '步驟',
    'Lifecycle': '生命週期',
    'Pipeline': 'Pipeline',

    # 开发相关
    'Development': 'Development',
    'Implementation': '實作',
    'Code': '程式碼',
    'Function': '函數',
    'Method': '方法',
    'Class': '類別',
    'Interface': 'Interface',
    'Module': '模組',
    'Package': '套件',
    'Library': '函式庫',
    'Framework': 'Framework',

    # 测试相关
    'Testing': 'Testing',
    'Test': 'Test',
    'Unit Test': 'Unit Test',
    'Integration Test': 'Integration Test',
    'Test Case': 'Test Case',
    'Test Coverage': 'Test Coverage',

    # 部署和运维
    'Deployment': 'Deployment',
    'Release': 'Release',
    'Build': 'Build',
    'Environment': 'Environment',
    'Configuration': 'Configuration',
    'Setup': 'Setup',
    'Installation': '安裝',

    # 架构相关
    'Architecture': 'Architecture',
    'Design': '設計',
    'Pattern': 'Pattern',
    'Model': 'Model',
    'Structure': '結構',
    'Component': 'Component',
    'Service': 'Service',

    # 安全和权限
    'Security': 'Security',
    'Authentication': 'Authentication',
    'Authorization': 'Authorization',
    'Permission': '權限',
    'Access Control': 'Access Control',

    # 性能和优化
    'Performance': 'Performance',
    'Optimization': '優化',
    'Scalability': 'Scalability',
    'Efficiency': '效率',
    'Speed': '速度',

    # 文档质量
    'Quality': '品質',
    'Standard': '標準',
    'Convention': '慣例',
    'Guideline': '指導方針',
    'Policy': '政策',
    'Rule': '規則',

    # 状态和操作
    'Status': '狀態',
    'State': '狀態',
    'Action': '操作',
    'Operation': '操作',
    'Task': '任務',
    'Job': '作業',

    # 结果和输出
    'Result': '結果',
    'Output': '輸出',
    'Input': '輸入',
    'Response': '回應',
    'Request': '請求',

    # 错误和问题
    'Error': '錯誤',
    'Exception': 'Exception',
    'Bug': 'Bug',
    'Issue': '問題',
    'Problem': '問題',
    'Failure': '失敗',
    'Warning': '警告',

    # 改进和更新
    'Improvement': '改進',
    'Enhancement': '增強',
    'Update': '更新',
    'Upgrade': '升級',
    'Migration': '遷移',
    'Refactoring': 'Refactoring',

    # 评估和审查
    'Review': '審查',
    'Evaluation': '評估',
    'Assessment': '評估',
    'Inspection': '檢查',
    'Validation': '驗證',
    'Verification': '驗證',

    # 完整性和准确性
    'Complete': '完整',
    'Incomplete': '不完整',
    'Comprehensive': '全面',
    'Partial': '部分',
    'Full': '完整',
    'Detailed': '詳細',

    # 清晰度
    'Clear': '清楚',
    'Unclear': '不清楚',
    'Ambiguous': '模糊',
    'Vague': '含糊',
    'Specific': '具體',
    'Precise': '精確',

    # 程度和级别
    'Sufficient': '充足',
    'Insufficient': '不足',
    'Adequate': '足夠',
    'Inadequate': '不足',
    'Excellent': '優秀',
    'Good': '良好',
    'Fair': '尚可',
    'Poor': '差',

    # 准确性
    'Accurate': '準確',
    'Inaccurate': '不準確',
    'Correct': '正確',
    'Incorrect': '錯誤',
    'Valid': '有效',
    'Invalid': '無效',

    # 文档状态
    'Approved': '已批准',
    'Pending': '待處理',
    'Rejected': '已拒絕',
    'Draft': '草稿',
    'Final': '最終版',
    'Published': '已發布',

    # 人员和角色
    'Team': '團隊',
    'Member': '成員',
    'Lead': '負責人',
    'Manager': '經理',
    'Developer': '開發者',
    'Engineer': '工程師',
    'Architect': 'Architect',
    'Designer': '設計師',
    'Tester': '測試人員',
    'User': '使用者',
    'Customer': '客戶',
    'Stakeholder': '利害關係人',

    # 时间相关
    'Deadline': '截止日期',
    'Timeline': '時程',
    'Schedule': '時程表',
    'Duration': '期間',
    'Frequency': '頻率',

    # 其他常用词
    'Version': '版本',
    'Type': '類型',
    'Category': '類別',
    'Level': '等級',
    'Priority': '優先級',
    'Scope': '範圍',
    'Context': 'Context',
    'Boundary': '邊界',
    'Constraint': '限制',
    'Dependency': 'Dependency',
    'Resource': '資源',
    'Tool': '工具',
    'Utility': '工具',
    'Helper': '輔助工具',

    # 通用动词
    'Create': '建立',
    'Build': '建置',
    'Generate': '產生',
    'Develop': '開發',
    'Implement': '實作',
    'Deploy': '部署',
    'Install': '安裝',
    'Configure': '配置',
    'Setup': '設定',
    'Initialize': '初始化',
    'Execute': '執行',
    'Run': '執行',
    'Start': '啟動',
    'Stop': '停止',
    'Pause': '暫停',
    'Resume': '恢復',
    'Restart': '重啟',
    'Delete': '刪除',
    'Remove': '移除',
    'Clean': '清理',
    'Update': '更新',
    'Modify': '修改',
    'Change': '變更',
    'Edit': '編輯',
    'Add': '新增',
    'Insert': '插入',
    'Append': '附加',
    'Merge': '合併',
    'Split': '分割',
    'Search': '搜尋',
    'Find': '尋找',
    'Filter': '篩選',
    'Sort': '排序',
    'Group': '分組',
    'Aggregate': '彙總',
    'Calculate': '計算',
    'Compute': '運算',
    'Process': '處理',
    'Transform': '轉換',
    'Convert': '轉換',
    'Parse': '解析',
    'Validate': '驗證',
    'Verify': '驗證',
    'Check': '檢查',
    'Test': '測試',
    'Monitor': '監控',
    'Track': '追蹤',
    'Log': '記錄',
    'Report': '報告',
    'Export': '匯出',
    'Import': '匯入',
    'Upload': '上傳',
    'Download': '下載',
    'Share': '分享',
    'Publish': '發布',
    'Subscribe': '訂閱',
    'Notify': '通知',
    'Alert': '警報',
    'Send': '發送',
    'Receive': '接收',
    'Request': '請求',
    'Response': '回應',
    'Query': '查詢',
    'Fetch': '擷取',
    'Load': '載入',
    'Save': '儲存',
    'Store': '儲存',
    'Cache': '快取',
    'Backup': '備份',
    'Restore': '還原',
    'Recover': '恢復',
    'Migrate': '遷移',
    'Upgrade': '升級',
    'Downgrade': '降級',
    'Rollback': 'Rollback',
    'Commit': 'Commit',
    'Push': 'Push',
    'Pull': 'Pull',
    'Clone': 'Clone',
    'Fork': 'Fork',
    'Branch': 'Branch',
    'Tag': 'Tag',
    'Checkout': 'Checkout',
    'Switch': '切換',
    'Rebase': 'Rebase',
    'Cherry-pick': 'Cherry-pick',
    'Stash': 'Stash',
    'Apply': '應用',
    'Discard': '捨棄',
    'Reset': '重置',
    'Revert': '還原',
    'Squash': 'Squash',
    'Amend': 'Amend',

    # 通用形容词
    'New': '新的',
    'Old': '舊的',
    'Latest': '最新',
    'Current': '目前',
    'Previous': '先前',
    'Next': '下一個',
    'First': '第一個',
    'Last': '最後',
    'Main': '主要',
    'Primary': '主要',
    'Secondary': '次要',
    'Optional': '選用',
    'Required': '必要',
    'Mandatory': '強制',
    'Recommended': '建議',
    'Suggested': '建議',
    'Available': '可用',
    'Unavailable': '不可用',
    'Enabled': '已啟用',
    'Disabled': '已停用',
    'Active': '啟用中',
    'Inactive': '未啟用',
    'Open': '開放',
    'Closed': '關閉',
    'Public': '公開',
    'Private': '私有',
    'Internal': '內部',
    'External': '外部',
    'Global': '全域',
    'Local': '本地',
    'Remote': '遠端',
    'Distributed': '分散式',
    'Centralized': '集中式',
    'Synchronous': '同步',
    'Asynchronous': '非同步',
    'Sequential': '循序',
    'Parallel': '平行',
    'Concurrent': '並行',
    'Real-time': '即時',
    'Offline': '離線',
    'Online': '線上',
    'Manual': '手動',
    'Automatic': '自動',
    'Static': '靜態',
    'Dynamic': '動態',
    'Fixed': '固定',
    'Variable': '變數',
    'Constant': '常數',
    'Temporary': '臨時',
    'Permanent': '永久',
    'Volatile': '易失',
    'Persistent': '持久',
    'Transient': '暫態',
    'Immutable': '不可變',
    'Mutable': '可變',
    'Read-only': '唯讀',
    'Write-only': '唯寫',
    'Read-write': '讀寫',
    'Single': '單一',
    'Multiple': '多個',
    'Unique': '唯一',
    'Duplicate': '重複',
    'Original': '原始',
    'Copy': '副本',
    'Source': '來源',
    'Target': '目標',
    'Destination': '目的地',
    'Origin': '起源',
    'Default': '預設',
    'Custom': '自訂',
    'Standard': '標準',
    'Extended': '擴展',
    'Basic': '基本',
    'Advanced': '進階',
    'Simple': '簡單',
    'Complex': '複雜',
    'Easy': '容易',
    'Difficult': '困難',
    'Fast': '快速',
    'Slow': '緩慢',
    'High': '高',
    'Low': '低',
    'Medium': '中',
    'Large': '大',
    'Small': '小',
    'Maximum': '最大',
    'Minimum': '最小',
    'Average': '平均',
    'Total': '總計',
    'Partial': '部分',
    'Complete': '完成',
    'Success': '成功',
    'Failure': '失敗',
    'Successful': '成功',
    'Failed': '失敗',
    'Passed': '通過',
    'Pending': '待處理',
    'Running': '執行中',
    'Stopped': '已停止',
    'Completed': '已完成',
    'Cancelled': '已取消',
    'Aborted': '已中止',
    'Timeout': '逾時',
    'Expired': '已過期',
    'Valid': '有效',
    'Invalid': '無效',
    'Enabled': '已啟用',
    'Disabled': '已停用',

    # 连接词和介词
    'and': '和',
    'or': '或',
    'with': '與',
    'without': '不含',
    'for': '用於',
    'from': '從',
    'to': '到',
    'in': '在',
    'on': '於',
    'at': '在',
    'by': '由',
    'via': '透過',
    'using': '使用',
    'through': '經由',
    'before': '之前',
    'after': '之後',
    'during': '期間',
    'within': '內',
    'between': '之間',
    'among': '之間',
    'across': '跨',
    'over': '超過',
    'under': '低於',
    'above': '高於',
    'below': '低於',

    # 疑问词
    'What': '什麼',
    'When': '何時',
    'Where': '哪裡',
    'Who': '誰',
    'Why': '為什麼',
    'How': '如何',
    'Which': '哪個',

    # 是否问题
    'Yes': '是',
    'No': '否',
    'True': '真',
    'False': '假',
    'Enabled': '啟用',
    'Disabled': '停用',
    'On': '開',
    'Off': '關',

    # 时间单位
    'Second': '秒',
    'Minute': '分鐘',
    'Hour': '小時',
    'Day': '天',
    'Week': '週',
    'Month': '月',
    'Year': '年',

    # 数量单位
    'Item': '項目',
    'Record': '記錄',
    'Entry': '條目',
    'Row': '列',
    'Column': '欄',
    'Field': '欄位',
    'Value': '值',
    'Count': '計數',
    'Number': '數量',
    'Amount': '數量',
    'Size': '大小',
    'Length': '長度',
    'Width': '寬度',
    'Height': '高度',
    'Depth': '深度',
    'Weight': '重量',
    'Volume': '容量',
    'Capacity': '容量',
    'Limit': '限制',
    'Threshold': '閾值',
    'Range': '範圍',
    'Interval': '間隔',
    'Period': '期間',

    # 文档类型
    'Document': '文件',
    'File': '檔案',
    'Folder': '資料夾',
    'Directory': '目錄',
    'Path': '路徑',
    'Location': '位置',
    'Address': '位址',
    'Link': '連結',
    'URL': 'URL',
    'Endpoint': 'Endpoint',
    'Page': '頁面',
    'Section': '章節',
    'Chapter': '章',
    'Article': '文章',
    'Post': '貼文',
    'Comment': '留言',
    'Note': '註記',
    'Annotation': '註解',
    'Remark': '備註',
    'Label': '標籤',
    'Tag': 'Tag',
    'Bookmark': '書籤',
    'Attachment': '附件',
    'Image': '圖片',
    'Picture': '圖片',
    'Photo': '照片',
    'Video': '影片',
    'Audio': '音訊',
    'Media': '媒體',
    'Content': '內容',
    'Data': '資料',
    'Information': '資訊',
    'Message': '訊息',
    'Notification': '通知',
    'Email': 'Email',
    'Subject': '主旨',
    'Body': '內文',
    'Header': '標頭',
    'Footer': '頁尾',
    'Title': '標題',
    'Name': '名稱',
    'Description': '說明',
    'Details': '詳細資訊',
    'Properties': '屬性',
    'Attributes': '屬性',
    'Settings': '設定',
    'Options': '選項',
    'Preferences': '偏好設定',
    'Parameters': '參數',
    'Arguments': '引數',
    'Variables': '變數',
    'Constants': '常數',
    'Values': '值',
    'Items': '項目',
    'Elements': '元素',
    'Objects': '物件',
    'Entities': '實體',
    'Records': '記錄',
    'Rows': '列',
    'Columns': '欄',
    'Fields': '欄位',
    'Keys': '鍵',
    'Indexes': '索引',
    'References': '參考',
    'Relations': '關聯',
    'Associations': '關聯',
    'Links': '連結',
    'Connections': '連線',
    'Mappings': '對應',
    'Bindings': '綁定',
}

def translate_line(line):
    """翻译单行，保留代码块和技术术语"""
    # 跳过代码块
    if line.strip().startswith('```') or line.strip().startswith('`'):
        return line

    # 跳过URL
    if 'http://' in line or 'https://' in line or 'www.' in line:
        return line

    # 跳过已包含大量中文的行
    chinese_ratio = len(re.findall(r'[\u4e00-\u9fff]', line)) / max(len(line), 1)
    if chinese_ratio > 0.3:  # 如果超过30%是中文，可能已翻译
        return line

    # 应用翻译，使用单词边界匹配
    translated = line
    for en, zh in sorted(TRANSLATIONS.items(), key=lambda x: -len(x[0])):
        # 使用单词边界确保只翻译完整单词
        pattern = r'\b' + re.escape(en) + r'\b'
        translated = re.sub(pattern, zh, translated, flags=re.IGNORECASE)

    return translated

def translate_file(filepath):
    """翻译整个文件"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        translated_lines = []
        in_code_block = False

        for line in lines:
            # 检测代码块
            if line.strip().startswith('```'):
                in_code_block = not in_code_block
                translated_lines.append(line)
                continue

            # 在代码块内，不翻译
            if in_code_block:
                translated_lines.append(line)
                continue

            # 翻译行
            translated_lines.append(translate_line(line))

        # 写回文件
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(translated_lines)

        return True
    except Exception as e:
        print(f"Error translating {filepath}: {e}", file=sys.stderr)
        return False

def main():
    if len(sys.argv) > 1:
        # 翻译指定文件或目录
        target = sys.argv[1]
        if os.path.isfile(target):
            files = [Path(target)]
        elif os.path.isdir(target):
            files = list(Path(target).rglob('*.md'))
        else:
            print(f"Invalid path: {target}")
            return 1
    else:
        # 默认翻译docs目录
        docs_dir = Path('/home/user/genai-demo/docs')
        files = list(docs_dir.rglob('*.md'))

    print(f"Found {len(files)} markdown files to translate")

    success_count = 0
    fail_count = 0

    for filepath in files:
        print(f"Translating: {filepath}")
        if translate_file(filepath):
            success_count += 1
        else:
            fail_count += 1

    print(f"\nTranslation completed:")
    print(f"  Success: {success_count}")
    print(f"  Failed: {fail_count}")

    return 0 if fail_count == 0 else 1

if __name__ == '__main__':
    sys.exit(main())
