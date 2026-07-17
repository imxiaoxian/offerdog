import type { CategoryTreeNode } from '@/services/category'

/** 与产品约定：全部 + 后端 / 技术管理 / 前端 / 算法 / DevOps */
export type JobBucketKey = 'all' | 'backend' | 'mgmt' | 'frontend' | 'algo' | 'devops'

export const JOB_BUCKET_OPTIONS: { key: JobBucketKey; label: string }[] = [
  { key: 'all', label: '全部' },
  { key: 'backend', label: '后端' },
  { key: 'mgmt', label: '技术管理' },
  { key: 'frontend', label: '前端' },
  { key: 'algo', label: '算法' },
  { key: 'devops', label: 'DevOps' },
]

/**
 * 根据岗位/分类名称归入大类（用于分类树二级节点、与模板 category 文案规则一致）
 */
export function bucketForJobName(name: string): JobBucketKey | null {
  const s = (name || '').trim()
  if (!s) return null

  if (/DevOps|devops|运维|SRE|CI\/CD|持续集成/i.test(s)) return 'devops'
  if (/算法|机器学习|深度学习|\bML\b|数据挖掘|人工智能|数据科学/i.test(s)) return 'algo'
  // 先匹配前端栈，避免「JavaScript」被误判为 Java 后端
  if (/JavaScript|TypeScript|前端|Web|React|Vue|iOS|Android|移动|小程序|Flutter|H5|全栈|全端/i.test(s)) return 'frontend'
  if (/后端|服务端|中间件|分布式|微服务|Java|Go|Rust|Python|C\+\+/.test(s)) return 'backend'
  if (/技术管理|技术总监|技术负责人|CTO|架构师|团队负责人|Engineering Manager|Team Lead/i.test(s)) return 'mgmt'
  if (/管理|总监|负责人|Lead/i.test(s)) return 'mgmt'

  return null
}

/**
 * 面试模板 category 字段（如「前端工程师」「DevOps」）映射到大类；无法识别时返回 null（仅「全部」下展示）
 */
export function bucketForTemplateCategory(category: string): JobBucketKey | null {
  const s = (category || '').trim()
  if (!s) return null
  return bucketForJobName(s)
}

/** 从分类树收集某大类下的全部二级岗位 ID */
export function collectLevel2IdsForBucket(tree: CategoryTreeNode[], bucket: JobBucketKey): number[] {
  if (bucket === 'all') return []
  const ids: number[] = []
  const walk = (nodes: CategoryTreeNode[]) => {
    for (const n of nodes) {
      if (n.level === 2 && typeof n.id === 'number') {
        const b = bucketForJobName(n.name)
        if (b === bucket) ids.push(n.id)
      }
      if (n.children?.length) walk(n.children)
    }
  }
  walk(tree)
  return ids
}
